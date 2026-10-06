import React from "react";
import StatusBadge from "../../../components/StatusBadge";
import { REQUEST_BADGE_CLASS, REQUEST_STATUS } from "../../../constants/status";

const RequestStatusBadge = ({ status }) => (
  <StatusBadge status={status} statusMap={REQUEST_STATUS} className={REQUEST_BADGE_CLASS} />
);

export default RequestStatusBadge;
