import * as z from "zod";

const required = { message: "Obligatorio" };

const percentField = (min, max, rangeMessage) =>
  z
    .string()
    .min(1, required)
    .refine((val) => !Number.isNaN(Number(val)), { message: "Ingrese un número válido" })
    .refine((val) => Number(val) >= min && Number(val) <= max, { message: rangeMessage });

const editableFields = {
  interestRate: percentField(0, 100, "Debe estar entre 0 y 100"),
  commissionRate: percentField(0, 100, "Debe estar entre 0 y 100"),
  calculationBase: z.enum(["COMERCIAL_360", "CALENDARIO_365"], { message: "Seleccione una base" }),
  status: z.enum(["ACTIVE", "INACTIVE"], { message: "Seleccione un estado" }),
  // warning_threshold_percentage es NUMERIC(5,2) como fracción: solo admite porcentajes enteros
  warningThreshold: percentField(1, 100, "Debe estar entre 1 y 100").refine((val) => Number.isInteger(Number(val)), {
    message: "Debe ser un número entero",
  }),
};

const accountNumber = z
  .string()
  .trim()
  .min(1, required)
  .max(50, { message: "Máximo 50 dígitos" })
  .regex(/^\d+$/, { message: "Solo números" });

// En edición el resto de campos se muestran bloqueados y no se validan
export const payerUpdateSchema = z.object({ ...editableFields, accountNumber });

// La cuenta solo se modifica si ya existe; sin cuenta se muestra bloqueada
export const payerUpdateWithoutAccountSchema = z.object(editableFields);

export const payerSchema = z
  .object({
    name: z.string().trim().min(1, required).max(255, { message: "Máximo 255 caracteres" }),
    nit: z
      .string()
      .min(1, required)
      .regex(/^\d{4}-\d{6}-\d{3}-\d$/, { message: "Formato 0000-000000-000-0" }),
    accountNumber,
    creditFacilityNumber: z.string().trim().min(1, required).max(255, { message: "Máximo 255 caracteres" }),
    approvedAmount: z
      .string()
      .min(1, required)
      .refine((val) => Number(val) > 0, { message: "Debe ser mayor a 0" }),
    ...editableFields,
    consumedAmount: z.string(),
    consumptionReference: z.string().trim().max(255, { message: "Máximo 255 caracteres" }),
  })
  // Todo pagador se registra activo; el estado solo se cambia al editar
  .omit({ status: true })
  .superRefine((data, ctx) => {
    const consumed = Number(data.consumedAmount || 0);
    const approved = Number(data.approvedAmount || 0);

    if (approved > 0 && consumed > approved) {
      ctx.addIssue({
        code: "custom",
        path: ["consumedAmount"],
        message: "No puede superar el monto aprobado",
      });
    }
    if (consumed > 0 && !data.consumptionReference) {
      ctx.addIssue({
        code: "custom",
        path: ["consumptionReference"],
        message: "Obligatoria cuando hay monto consumido",
      });
    }
  });
