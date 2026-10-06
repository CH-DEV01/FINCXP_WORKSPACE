import React, { createContext, useContext, useState, useEffect, useMemo, useCallback, useRef } from "react";
import axios from "axios";
import Swal from "sweetalert2";
import api, { SESSION_EXPIRED_EVENT, markSessionEnded } from "../services/api"; // Asegúrate de que este 'api' inyecte el JWT en los headers
import { API_BASE_URL, PORTAL_URL } from "../constants/apiConstants";

const TOKEN_KEY = "jwt_token";
// Compartida entre pestañas: trabajar en una mantiene viva la sesión de las demás.
const LAST_ACTIVITY_KEY = "last_activity";
const DEFAULT_IDLE_TIMEOUT_MINUTES = 15;
const IDLE_CHECK_INTERVAL_MS = 15_000;
const ACTIVITY_WRITE_INTERVAL_MS = 5_000;
const ACTIVITY_EVENTS = ["mousedown", "mousemove", "keydown", "scroll", "touchstart", "wheel"];

const SESSION_END_MESSAGES = {
  expired: "Su sesión expiró. Ingrese nuevamente desde el portal.",
  idle: "Su sesión se cerró por inactividad.",
  otherTab: "La sesión se cerró en otra pestaña.",
};

/** Momento de expiración del JWT en milisegundos, o null si no se puede leer. */
const tokenExpiresAt = (token) => {
  try {
    const payload = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    const { exp } = JSON.parse(atob(payload));
    return typeof exp === "number" ? exp * 1000 : null;
  } catch {
    return null;
  }
};

const clearStoredSession = () => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(LAST_ACTIVITY_KEY);
  sessionStorage.clear();
};

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const endingRef = useRef(false);

  const fetchUserProfile = useCallback(async (token) => {
    setIsLoading(true);
    try {
      const response = await api.get("/v1/auth/me", {
        headers: { Authorization: `Bearer ${token}` },
      });
      setUser(response.data?.data);
    } catch (error) {
      console.error("Error validando sesión:", error);
      setUser(null);
      localStorage.removeItem(TOKEN_KEY);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    const initAuth = async () => {
      const token = localStorage.getItem(TOKEN_KEY);
      const expiresAt = token ? tokenExpiresAt(token) : null;

      if (token && (expiresAt === null || expiresAt > Date.now())) {
        await fetchUserProfile(token);
      } else {
        if (token) clearStoredSession();
        setIsLoading(false);
      }
    };
    initAuth();
  }, [fetchUserProfile]);

  // El usuario se mantiene en el estado hasta redirigir: sin él, las rutas protegidas
  // saltarían al portal antes de mostrar el aviso.
  const endSession = useCallback(async (reason) => {
    if (endingRef.current) return;
    endingRef.current = true;
    markSessionEnded();
    clearStoredSession();

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

  useEffect(() => {
    if (!user) return undefined;

    const expiresAt = tokenExpiresAt(localStorage.getItem(TOKEN_KEY) || "");
    if (expiresAt === null) return undefined;

    const timer = setTimeout(() => endSession("expired"), Math.max(0, expiresAt - Date.now()));
    return () => clearTimeout(timer);
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
      if ((event.key === TOKEN_KEY || event.key === null) && !localStorage.getItem(TOKEN_KEY)) {
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
          baseURL: API_BASE_URL,
        });

        // Extraemos el JWT del response de Spring Boot
        const jwt = response.data.data;
        localStorage.setItem(TOKEN_KEY, jwt);

        // Una vez tenemos el token, traemos el perfil completo (ruta, menús, etc)
        await fetchUserProfile(jwt);
        return response;
      } finally {
        setIsLoading(false);
      }
    },
    [fetchUserProfile],
  );

  const logout = useCallback(() => {
    endingRef.current = true;
    markSessionEnded();
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
