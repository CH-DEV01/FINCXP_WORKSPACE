import React from "react";
import StatusBadge from "../../../components/StatusBadge";
import { DISPERSION_REQUEST_STATUS, REQUEST_BADGE_CLASS } from "../../../constants/status";

const DispersionStatusBadge = ({ status }) => (
  <StatusBadge status={status} statusMap={DISPERSION_REQUEST_STATUS} className={REQUEST_BADGE_CLASS} />
);

export default DispersionStatusBadge;
