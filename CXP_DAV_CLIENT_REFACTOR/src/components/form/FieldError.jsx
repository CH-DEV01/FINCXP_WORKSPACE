import React from "react";

const FieldError = ({ error }) => (error ? <p className="text-red-500 text-[10px] mt-1">{error.message}</p> : null);

export default FieldError;
