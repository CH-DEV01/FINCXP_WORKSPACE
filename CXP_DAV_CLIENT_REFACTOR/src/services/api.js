import axios from "axios";
import { API_URL } from "../constants/apiConstants";
import { redactAxiosError } from "./redactAxiosError";

// El login usa la instancia global de axios.
axios.interceptors.response.use((response) => response, redactAxiosError);

const api = axios.create({
  baseURL: API_URL,
  withCredentials: true,
});

// Interceptor de Peticiones
api.interceptors.request.use(
  (config) => {
    // 1. Extraer el token del localStorage (asegúrate de que el nombre de la llave coincida con el tuyo)
    const token = localStorage.getItem("jwt_token");

    // 2. Si existe el token, inyectarlo en el header 'Authorization'
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => {
    // Manejo de errores antes de que se envíe la petición
    return Promise.reject(error);
  },
);

export const SESSION_EXPIRED_EVENT = "auth:session-expired";

let sessionEnded = false;

/** Desde aquí los 401 ya no llegan a las pantallas: la sesión terminó y la app va al portal. */
export const markSessionEnded = () => {
  sessionEnded = true;
};

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error?.response?.status === 401 && (sessionEnded || localStorage.getItem("jwt_token"))) {
      if (!sessionEnded) {
        sessionEnded = true;
        window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
      }
      // Sin propagar: el catch de la pantalla abriría su propio Swal y reemplazaría el aviso de sesión.
      return new Promise(() => {});
    }
    return redactAxiosError(error);
  },
);

export default api;
