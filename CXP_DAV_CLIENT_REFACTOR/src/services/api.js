import axios from "axios";
import { API_URL } from "../constants/apiConstants";
import { redactAxiosError } from "./redactAxiosError";

export const TOKEN_KEY = "jwt_token";

// El login usa la instancia global de axios.
axios.interceptors.response.use((response) => response, redactAxiosError);

const api = axios.create({
  baseURL: API_URL,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const SESSION_EXPIRED_EVENT = "auth:session-expired";

let sessionEnded = false;

/** Desde aquí los 401 ya no llegan a las pantallas: la sesión terminó y la app va al portal. */
export const markSessionEnded = () => {
  sessionEnded = true;
};

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error?.response?.status === 401 && (sessionEnded || localStorage.getItem(TOKEN_KEY))) {
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
