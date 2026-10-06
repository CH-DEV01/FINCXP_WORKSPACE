import * as z from "zod";

export const supplierUpdateSchema = z.object({
  accountNumber: z
    .string()
    .trim()
    .min(1, { message: "Obligatorio" })
    .max(50, { message: "Máximo 50 caracteres" })
    .regex(/^\d+$/, { message: "Solo se permiten números" }),
  status: z.enum(["ACTIVE", "INACTIVE"], { message: "Seleccione un estado" }),
});
