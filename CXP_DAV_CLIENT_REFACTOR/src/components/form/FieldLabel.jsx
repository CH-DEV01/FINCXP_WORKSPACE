import React from "react";

const FieldLabel = ({ htmlFor, required = false, children }) => (
  <label htmlFor={htmlFor} className="block text-xs font-semibold text-gray-600 mb-2">
    {children}
    {required && <span className="text-red-500"> *</span>}
  </label>
);

export default FieldLabel;
