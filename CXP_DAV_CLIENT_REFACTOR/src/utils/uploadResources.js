/** Mismos límites que valida el backend (UploadResourceServiceImpl). */
export const TEMPLATE_RULES = { label: "La plantilla", empty: "vacía", extension: ".xlsx", maxSizeMb: 1 };
export const MANUAL_RULES = { label: "El manual", empty: "vacío", extension: ".pdf", maxSizeMb: 10 };

/** Mensaje de error para el archivo de un recurso, o null si se puede enviar. */
export const resourceFileError = (file, { label, empty, extension, maxSizeMb }) => {
  if (!file) return `Seleccione ${label.toLowerCase()}.`;
  if (!file.name.toLowerCase().endsWith(extension)) return `${label} debe ser un archivo ${extension}.`;
  if (file.size === 0) return `${label} está ${empty}.`;
  if (file.size > maxSizeMb * 1024 * 1024) {
    return `${label} supera el tamaño máximo permitido de ${maxSizeMb} MB.`;
  }
  return null;
};

/** 2048 -> "2.0 KB"; 1572864 -> "1.50 MB". */
export const formatFileSize = (bytes) => {
  const value = Number(bytes) || 0;
  if (value >= 1024 * 1024) return `${(value / (1024 * 1024)).toFixed(2)} MB`;
  return `${(value / 1024).toFixed(1)} KB`;
};
