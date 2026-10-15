import axios from "axios";
import { API_URL, PORTAL_URL } from "../constants/apiConstants";

const CONFIG_TIMEOUT_MS = 3_000;
const HTTP_URL = /^https?:\/\/\S+$/i;

let portalUrlRequest;

// Sin respuesta de la API o sin APP_LOGIN_URL configurada se usa PORTAL_URL, para no
// dejar al usuario sin salida.
const fetchPortalUrl = async () => {
  try {
    const response = await axios.get(`${API_URL}/v1/public/config`, { timeout: CONFIG_TIMEOUT_MS });
    const url = response.data?.data?.portalUrl;
    return typeof url === "string" && HTTP_URL.test(url) ? url : PORTAL_URL;
  } catch {
    return PORTAL_URL;
  }
};

/** URL de salida al portal (parámetro APP_LOGIN_URL); se consulta una sola vez. */
export const portalUrl = () => {
  portalUrlRequest ??= fetchPortalUrl();
  return portalUrlRequest;
};

/** Sale de la aplicación hacia el portal; con replace no deja la página en el historial. */
export const goToPortal = async ({ replace = false } = {}) => {
  const url = await portalUrl();
  if (replace) window.location.replace(url);
  else window.location.href = url;
};
