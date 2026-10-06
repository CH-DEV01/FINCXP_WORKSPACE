import React from "react";
import Icon from "@mdi/react";
import { mdiLoading, mdiCheckAll, mdiEyeOutline, mdiDownload } from "@mdi/js";
import { dispersionRequestKey } from "./dispersionUtils";

const DispersionRequestActions = ({ request, downloadingKey, onOpen, onDownload }) => {
  const isDownloadingThis = downloadingKey === dispersionRequestKey(request);
  return (
    <div className="flex justify-center gap-2">
      {request.status === "INGRESADO" && (
        <button
          onClick={() => onOpen(request)}
          className="p-2 text-red-500 hover:text-white hover:bg-red-500 rounded-lg"
          title="Generar lote y descargar solicitud"
        >
          <Icon path={mdiDownload} size={0.8} />
        </button>
      )}
      {request.status === "EN_PROCESO" && (
        <button
          onClick={() => onOpen(request)}
          className="p-2 text-red-500 hover:text-white hover:bg-red-500 rounded-lg"
          title="Confirmar dispersión"
        >
          <Icon path={mdiCheckAll} size={0.8} />
        </button>
      )}
      {request.batchId && (
        <button
          onClick={() => onDownload(request)}
          disabled={downloadingKey !== null}
          className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-200 rounded-lg disabled:opacity-40"
          title="Descargar solicitud de dispersión"
        >
          <Icon
            path={isDownloadingThis ? mdiLoading : mdiDownload}
            size={0.8}
            className={isDownloadingThis ? "animate-spin" : ""}
          />
        </button>
      )}
      <button
        onClick={() => onOpen(request)}
        className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-200 rounded-lg"
        title="Ver detalle"
      >
        <Icon path={mdiEyeOutline} size={0.8} />
      </button>
    </div>
  );
};

export default DispersionRequestActions;
