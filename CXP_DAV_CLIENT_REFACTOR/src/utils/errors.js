import Swal from "sweetalert2";

/**
 * Mensaje del backend (ApiResponse.message) o, si el error no vino de una respuesta HTTP
 * (validaciones propias que lanzan Error), su propio mensaje.
 */
export const apiErrorMessage = (error, fallback) => {
  if (error?.response) return error.response.data?.message || fallback;
  if (error && !error.isAxiosError && error.message) return error.message;
  return fallback;
};

/** Con responseType "blob" los errores JSON del backend también llegan como Blob. */
export const readApiErrorMessage = async (error, fallback) => {
  const data = error?.response?.data;
  if (data instanceof Blob) {
    try {
      return JSON.parse(await data.text())?.message || fallback;
    } catch {
      return fallback;
    }
  }
  return apiErrorMessage(error, fallback);
};

export const showLoading = (text = "Guardando los cambios, por favor espere.", title = "Procesando...") =>
  Swal.fire({
    title,
    text,
    allowOutsideClick: false,
    didOpen: () => Swal.showLoading(),
  });

export const showSuccess = (text, title = "¡Éxito!") =>
  Swal.fire({ icon: "success", title, text, timer: 2000, showConfirmButton: false });

export const showError = (text, title = "Error") =>
  Swal.fire({ icon: "error", title, text, confirmButtonColor: "#dc2626" });

export const showApiError = (error, fallback, title) => showError(apiErrorMessage(error, fallback), title);
