import React, { createContext, useContext, useState, useEffect, useMemo, useCallback, useRef } from "react";
import axios from "axios";
import Swal from "sweetalert2";
import api, { SESSION_EXPIRED_EVENT, markSessionActive, markSessionEnded } from "../services/api";
import { API_BASE_URL, API_URL, PORTAL_URL } from "../constants/apiConstants";

// Solo la vigencia del token ({ issuedAt, expiresAt }); el JWT está en una cookie HttpOnly.
// Compartida entre pestañas: si desaparece, la sesión se cerró en otra.
const SESSION_KEY = "session";
// Versiones anteriores guardaban aquí el JWT.
const LEGACY_TOKEN_KEY = "jwt_token";
// Compartida entre pestañas: trabajar en una mantiene viva la sesión de las demás.
const LAST_ACTIVITY_KEY = "last_activity";
const DEFAULT_IDLE_TIMEOUT_MINUTES = 15;
const IDLE_CHECK_INTERVAL_MS = 15_000;
const ACTIVITY_WRITE_INTERVAL_MS = 5_000;
const ACTIVITY_EVENTS = ["mousedown", "mousemove", "keydown", "scroll", "touchstart", "wheel"];
const REFRESH_MARGIN_MS = 60_000;
const LOGOUT_TIMEOUT_MS = 3_000;

const SESSION_END_MESSAGES = {
  expired: "Su sesión expiró. Ingrese nuevamente desde el portal.",
  idle: "Su sesión se cerró por inactividad.",
  otherTab: "La sesión se cerró en otra pestaña.",
};

const readSession = () => {
  try {
    const session = JSON.parse(localStorage.getItem(SESSION_KEY));
    return typeof session?.issuedAt === "number" && typeof session?.expiresAt === "number" ? session : null;
  } catch {
    return null;
  }
};

const storeSession = ({ issuedAt, expiresAt }) =>
  localStorage.setItem(SESSION_KEY, JSON.stringify({ issuedAt, expiresAt }));

/** Momento en que conviene renovar el token, antes de que venza. */
const refreshAt = ({ issuedAt, expiresAt }) =>
  expiresAt - Math.min(REFRESH_MARGIN_MS, (expiresAt - issuedAt) / 4);

const clearStoredSession = () => {
  localStorage.removeItem(SESSION_KEY);
  localStorage.removeItem(LAST_ACTIVITY_KEY);
  localStorage.removeItem(LEGACY_TOKEN_KEY);
  sessionStorage.clear();
};

// Revoca la sesión en la API y borra la cookie. Usa axios directo: el interceptor de
// `api` no resuelve las respuestas 401.
const revokeSession = async () => {
  try {
    await axios.post(`${API_URL}/v1/auth/logout`, null, { withCredentials: true, timeout: LOGOUT_TIMEOUT_MS });
  } catch {
    // La sesión se cierra en el navegador aunque la API no responda.
  }
};

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const endingRef = useRef(false);

  const fetchUserProfile = useCallback(async () => {
    setIsLoading(true);
    try {
      const response = await api.get("/v1/auth/me");
      const profile = response.data?.data;
      storeSession(profile.session);
      markSessionActive();
      setUser(profile);
    } catch (error) {
      setUser(null);
      if (error?.response?.status === 401) {
        localStorage.removeItem(SESSION_KEY);
      } else {
        console.error("Error validando sesión:", error);
      }
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    localStorage.removeItem(LEGACY_TOKEN_KEY);
    fetchUserProfile();
  }, [fetchUserProfile]);

  // El usuario se mantiene en el estado hasta redirigir: sin él, las rutas protegidas
  // saltarían al portal antes de mostrar el aviso.
  const endSession = useCallback(async (reason) => {
    if (endingRef.current) return;
    endingRef.current = true;
    markSessionEnded();
    clearStoredSession();
    // La pestaña que cerró la sesión ya la revocó.
    if (reason !== "otherTab") await revokeSession();

    await Swal.fire({
      icon: "info",
      title: "Sesión finalizada",
      text: SESSION_END_MESSAGES[reason],
      confirmButtonText: "Ir al portal",
      confirmButtonColor: "#dc2626",
      allowOutsideClick: false,
      allowEscapeKey: false,
    });
    window.location.href = PORTAL_URL;
  }, []);

  useEffect(() => {
    const onSessionExpired = () => endSession("expired");
    window.addEventListener(SESSION_EXPIRED_EVENT, onSessionExpired);
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, onSessionExpired);
  }, [endSession]);

  // El token dura lo que la inactividad permitida: se renueva antes de vencer solo si
  // hubo actividad desde que se emitió. La API rechaza la renovación al llegar a la
  // duración máxima de la sesión, y el interceptor de 401 cierra la sesión.
  useEffect(() => {
    if (!user) return undefined;

    let timer;
    let cancelled = false;

    const schedule = (at, action) => {
      clearTimeout(timer);
      timer = setTimeout(action, Math.max(0, at - Date.now()));
    };

    const check = async () => {
      if (cancelled) return;
      const session = readSession();
      if (!session) return;

      const now = Date.now();
      if (session.expiresAt <= now) {
        endSession("expired");
        return;
      }
      // Aún no toca renovar, o ya lo renovó otra pestaña.
      if (refreshAt(session) > now) {
        schedule(refreshAt(session), check);
        return;
      }
      const lastActivity = Number(localStorage.getItem(LAST_ACTIVITY_KEY));
      if (!(lastActivity > session.issuedAt)) {
        schedule(session.expiresAt, () => endSession("idle"));
        return;
      }

      try {
        const response = await api.post("/v1/auth/refresh");
        if (cancelled) return;
        storeSession(response.data.data);
        check();
      } catch {
        if (!cancelled) schedule(session.expiresAt, () => endSession("expired"));
      }
    };

    check();
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [user, endSession]);

  useEffect(() => {
    if (!user) return undefined;

    const idleMs = (Number(user.idleTimeoutMinutes) || DEFAULT_IDLE_TIMEOUT_MINUTES) * 60_000;
    let lastWrite = 0;

    const recordActivity = () => {
      const now = Date.now();
      if (now - lastWrite >= ACTIVITY_WRITE_INTERVAL_MS) {
        lastWrite = now;
        localStorage.setItem(LAST_ACTIVITY_KEY, String(now));
      }
    };

    recordActivity();
    ACTIVITY_EVENTS.forEach((event) => window.addEventListener(event, recordActivity, { passive: true }));

    const interval = setInterval(() => {
      const lastActivity = Number(localStorage.getItem(LAST_ACTIVITY_KEY));
      if (lastActivity && Date.now() - lastActivity >= idleMs) {
        endSession("idle");
      }
    }, IDLE_CHECK_INTERVAL_MS);

    return () => {
      clearInterval(interval);
      ACTIVITY_EVENTS.forEach((event) => window.removeEventListener(event, recordActivity));
    };
  }, [user, endSession]);

  useEffect(() => {
    if (!user) return undefined;

    const onStorage = (event) => {
      if ((event.key === SESSION_KEY || event.key === null) && !localStorage.getItem(SESSION_KEY)) {
        endSession("otherTab");
      }
    };
    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
  }, [user, endSession]);

  // Ingreso de desarrollo: la API responde con la cookie de sesión.
  const login = useCallback(
    async (payload) => {
      setIsLoading(true);
      try {
        const response = await axios.post("/api/v1/auth/sso-login", payload, {
          baseURL: API_BASE_URL,
          withCredentials: true,
        });
        await fetchUserProfile();
        return response;
      } finally {
        setIsLoading(false);
      }
    },
    [fetchUserProfile],
  );

  const logout = useCallback(async () => {
    endingRef.current = true;
    markSessionEnded();
    await revokeSession();
    clearStoredSession();
    window.location.replace(PORTAL_URL);
  }, []);

  const userData = useMemo(
    () =>
      user
        ? {
            ...user,
            name: [user.firstName, user.lastName].filter(Boolean).join(" ") || user.dui,
          }
        : null,
    [user],
  );

  const value = useMemo(
    () => ({ user, userData, login, logout, isLoading }),
    [user, userData, login, logout, isLoading],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => useContext(AuthContext);
