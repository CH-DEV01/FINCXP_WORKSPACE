/** Un 401 significa cookie ausente o vencida; el resto es fallo transitorio de /me. */
export const shouldClearSessionOnProfileError = (error) => error?.response?.status === 401;
