import axios from "axios";
import { API_URL } from "../constants/apiConstants";
import { redactAxiosError } from "./redactAxiosError";

// El login usa la instancia global de axios.
axios.interceptors.response.use((response) => response, redactAxiosError);

// El JWT viaja en una cookie HttpOnly que arma la API: JavaScript no lo lee ni lo envía.
const api = axios.create({
  baseURL: API_URL,
  withCredentials: true,
});

export const SESSION_EXPIRED_EVENT = "auth:session-expired";

let sessionEnded = false;
let sessionActive = false;

/** Desde aquí los 401 ya no llegan a las pantallas: la sesión terminó y la app va al portal. */
export const markSessionEnded = () => {
  sessionEnded = true;
};

/** Con sesión abierta, un 401 significa que expiró o fue revocada. */
export const markSessionActive = () => {
  sessionActive = true;
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
