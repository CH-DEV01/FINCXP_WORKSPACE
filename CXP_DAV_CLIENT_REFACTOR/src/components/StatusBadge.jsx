import React from "react";
import { BADGE_CLASS, ENTITY_STATUS } from "../constants/status.js";

const FALLBACK_CLASS = "bg-gray-100 text-gray-800";

/**
 * `statusMap` asocia cada estado con { label, className, dot? }; por defecto, Activo / Inactivo.
 * Con `showDot` se antepone el punto de color definido en `dot`.
 */
const StatusBadge = ({ status, statusMap = ENTITY_STATUS, className = BADGE_CLASS, showDot = false }) => {
  const meta = statusMap[status];
  return (
    <span className={`${className} ${meta?.className ?? FALLBACK_CLASS}`}>
      {showDot && <span className={`w-1.5 h-1.5 rounded-full ${meta?.dot ?? "bg-gray-400"}`} />}
      {meta?.label ?? status ?? "N/A"}
    </span>
  );
};

export default StatusBadge;
