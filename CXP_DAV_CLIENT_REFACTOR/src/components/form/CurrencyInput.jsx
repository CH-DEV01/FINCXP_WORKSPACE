import React from "react";
import { Controller } from "react-hook-form";
import inputClass from "./inputClass.js";
import { formatAmountMask, sanitizeAmount } from "../../utils/format.js";

/** El valor del formulario se guarda limpio ("1500.5"); la máscara ($ 1,500.5) solo se aplica al mostrarlo. */
const CurrencyInput = ({ id, name, control, hasError, locked = false, placeholder = "$ 0.00" }) => (
  <Controller
    name={name}
    control={control}
    render={({ field }) => (
      <input
        id={id}
        name={field.name}
        ref={field.ref}
        type="text"
        inputMode="decimal"
        readOnly={locked}
        value={formatAmountMask(field.value, "$ ")}
        onChange={(e) => field.onChange(sanitizeAmount(e.target.value))}
        onBlur={field.onBlur}
        className={inputClass(hasError, locked)}
        placeholder={placeholder}
      />
    )}
  />
);

export default CurrencyInput;
