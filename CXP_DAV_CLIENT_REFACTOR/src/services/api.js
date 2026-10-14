import axios from "axios";
import { API_URL } from "../constants/apiConstants";
import { redactAxiosError } from "./redactAxiosError";

// El JWT viaja en una cookie HttpOnly del dominio común; el navegador solo la envía a
// la API con withCredentials, y la API exige este encabezado en las escrituras.
export const SESSION_REQUEST_CONFIG = {
  withCredentials: true,
  headers: { "X-Requested-With": "XMLHttpRequest" },
};

// El login usa la instancia global de axios.
axios.interceptors.response.use((response) => response, redactAxiosError);

const api = axios.create({
  baseURL: API_URL,
  ...SESSION_REQUEST_CONFIG,
});

export const SESSION_EXPIRED_EVENT = "auth:session-expired";

let sessionActive = false;
let sessionEnded = false;

/** La API confirmó la sesión: desde aquí un 401 significa que expiró. */
export const markSessionActive = () => {
  sessionActive = true;
};

/** Desde aquí los 401 ya no llegan a las pantallas: la sesión terminó y la app va al portal. */
export const markSessionEnded = () => {
  sessionEnded = true;
};

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error?.response?.status === 401 && (sessionEnded || sessionActive)) {
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
