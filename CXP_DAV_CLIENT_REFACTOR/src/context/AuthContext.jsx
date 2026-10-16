import React, { createContext, useContext, useState, useEffect, useMemo, useCallback, useRef } from "react";
import axios from "axios";
import Swal from "sweetalert2";
import api, {
  SESSION_EXPIRED_EVENT,
  SESSION_REQUEST_CONFIG,
  markSessionActive,
  markSessionEnded,
} from "../services/api";
import { API_BASE_URL, API_URL } from "../constants/apiConstants";
import { goToPortal, portalUrl } from "../services/portal";
import { applyServerTime, nowMs } from "../utils/sessionClock";
import { shouldClearSessionOnProfileError } from "../utils/sessionProfileError";

// El JWT está en una cookie HttpOnly que JavaScript no puede leer. Aquí se guarda solo
// su vigencia ({ issuedAt, expiresAt } en milisegundos), compartida entre pestañas.
const SESSION_KEY = "session_window";
// Compartida entre pestañas: trabajar en una mantiene viva la sesión de las demás.
const LAST_ACTIVITY_KEY = "last_activity";
// Versiones anteriores guardaban aquí la vigencia y el JWT.
const LEGACY_SESSION_KEY = "session";
const LEGACY_TOKEN_KEY = "jwt_token";
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

/** Vigencia guardada de la sesión, o null si no hay o no se puede leer. */
const readSession = () => {
  try {
    const { issuedAt, expiresAt } = JSON.parse(localStorage.getItem(SESSION_KEY));
    return typeof issuedAt === "number" && typeof expiresAt === "number" ? { issuedAt, expiresAt } : null;
  } catch {
    return null;
  }
};

const writeSession = (session) => {
  if (!session) return;
  applyServerTime(session.serverTime);
  localStorage.setItem(
    SESSION_KEY,
    JSON.stringify({ issuedAt: session.issuedAt, expiresAt: session.expiresAt }),
  );
};

/** Momento en que conviene renovar el token, antes de que venza. */
const refreshAt = ({ issuedAt, expiresAt }) =>
  expiresAt - Math.min(REFRESH_MARGIN_MS, (expiresAt - issuedAt) / 4);

const clearStoredSession = () => {
  localStorage.removeItem(SESSION_KEY);
  localStorage.removeItem(LAST_ACTIVITY_KEY);
  localStorage.removeItem(LEGACY_SESSION_KEY);
  localStorage.removeItem(LEGACY_TOKEN_KEY);
  sessionStorage.clear();
};

// Revoca en la API todos los tokens del usuario y borra la cookie. Usa axios directo:
// el interceptor de `api` no resuelve las respuestas 401.
const revokeSession = async () => {
  try {
    await axios.post(`${API_URL}/v1/auth/logout`, null, {
      ...SESSION_REQUEST_CONFIG,
      timeout: LOGOUT_TIMEOUT_MS,
    });
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
      writeSession(profile?.session);
      markSessionActive();
      setUser(profile);
    } catch (error) {
      // Solo un 401 confirma que no hay sesión. Timeout, 5xx o red no deben borrar
      // session_window: las demás pestañas lo interpretarían como cierre (SES-06).
      setUser(null);
      if (shouldClearSessionOnProfileError(error)) {
        clearStoredSession();
      } else {
        console.error("Error validando sesión:", error);
      }
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Solo la API puede saber si la cookie existe y sigue vigente.
  useEffect(() => {
    localStorage.removeItem(LEGACY_SESSION_KEY);
    localStorage.removeItem(LEGACY_TOKEN_KEY);
    // Se pide desde el inicio: al cerrar la sesión la URL ya está lista.
    portalUrl();
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
    await goToPortal();
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
      // at e issuedAt/expiresAt están en hora de servidor; nowMs() corrige el reloj local.
      timer = setTimeout(action, Math.max(0, at - nowMs()));
    };

    const check = async () => {
      if (cancelled) return;
      const session = readSession();
      if (!session) return;

      const now = nowMs();
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
        writeSession(response.data.data);
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
      const now = nowMs();
      if (now - lastWrite >= ACTIVITY_WRITE_INTERVAL_MS) {
        lastWrite = now;
        localStorage.setItem(LAST_ACTIVITY_KEY, String(now));
      }
    };

    recordActivity();
    ACTIVITY_EVENTS.forEach((event) => window.addEventListener(event, recordActivity, { passive: true }));

    const interval = setInterval(() => {
      const lastActivity = Number(localStorage.getItem(LAST_ACTIVITY_KEY));
      if (lastActivity && nowMs() - lastActivity >= idleMs) {
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

  const login = useCallback(
    async (payload) => {
      setIsLoading(true);
      try {
        const response = await axios.post("/api/v1/auth/sso-login", payload, {
          ...SESSION_REQUEST_CONFIG,
          baseURL: API_BASE_URL,
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
    clearStoredSession();
    await revokeSession();
    await goToPortal({ replace: true });
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
