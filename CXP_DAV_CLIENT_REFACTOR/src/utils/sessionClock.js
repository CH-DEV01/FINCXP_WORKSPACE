/**
 * Corrige el desfase entre el reloj local y el del servidor.
 * issuedAt/expiresAt vienen en hora de servidor; sin este ajuste, un equipo
 * adelantado cierra la sesión al cargar y uno atrasado renueva tarde (SES-05).
 */

let clockOffsetMs = 0;

/** Desfase actual: horaServidor ≈ Date.now() + offset. */
export const getClockOffsetMs = () => clockOffsetMs;

/** Alinea el reloj del cliente con la hora del servidor recibida en /me o /refresh. */
export const applyServerTime = (serverTime) => {
  if (typeof serverTime !== "number" || !Number.isFinite(serverTime)) {
    return;
  }
  clockOffsetMs = serverTime - Date.now();
};

/** Epoch ms en escala del servidor. */
export const nowMs = () => Date.now() + clockOffsetMs;

/** Solo para tests. */
export const resetClockOffset = () => {
  clockOffsetMs = 0;
};
