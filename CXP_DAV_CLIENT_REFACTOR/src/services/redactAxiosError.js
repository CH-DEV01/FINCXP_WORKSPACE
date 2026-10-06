const REDACTED = "[REDACTED]";

/**
 * Interceptor de errores: borra el cuerpo enviado (contraseñas, DUI) y el token del
 * AxiosError antes de que llegue a los catch, que lo imprimen completo en consola.
 */
export const redactAxiosError = (error) => {
  const config = error?.config;

  if (config) {
    if (config.data !== undefined) {
      config.data = REDACTED;
    }
    if (config.headers) {
      if (typeof config.headers.set === "function") {
        if (config.headers.has?.("Authorization")) {
          config.headers.set("Authorization", REDACTED);
        }
      } else if (config.headers.Authorization) {
        config.headers.Authorization = REDACTED;
      }
    }
  }

  return Promise.reject(error);
};
