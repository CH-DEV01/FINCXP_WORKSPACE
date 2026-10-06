import React from "react";
import Icon from "@mdi/react";
import { mdiLoading, mdiCheckAll, mdiDownload, mdiEyeOutline } from "@mdi/js";
import { requestKey } from "./requestUtils";

const RequestActions = ({ request, generatingKey, onGenerate, onConfirm, onDetail }) => {
  const isGeneratingThis = generatingKey === requestKey(request);
  return (
    <div className="flex justify-center gap-2">
      {request.status === "INGRESADO" && (
        <button
          onClick={() => onGenerate(request)}
          disabled={generatingKey !== null}
          className="p-2 text-red-500 hover:text-white hover:bg-red-500 rounded-lg disabled:opacity-40 disabled:hover:bg-transparent disabled:hover:text-red-500"
          title="Generar lote y descargar reporte"
        >
          <Icon
            path={isGeneratingThis ? mdiLoading : mdiDownload}
            size={0.8}
            className={isGeneratingThis ? "animate-spin" : ""}
          />
        </button>
      )}
      {request.status === "EN_PROCESO" && (
        <button
          onClick={() => onConfirm(request)}
          className="p-2 text-red-500 hover:text-white hover:bg-red-500 rounded-lg"
          title="Confirmar desembolso"
        >
          <Icon path={mdiCheckAll} size={0.8} />
        </button>
      )}
      <button
        onClick={() => onDetail(request)}
        className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-200 rounded-lg"
        title="Ver detalle"
      >
        <Icon path={mdiEyeOutline} size={0.8} />
      </button>
    </div>
  );
};

export default RequestActions;
