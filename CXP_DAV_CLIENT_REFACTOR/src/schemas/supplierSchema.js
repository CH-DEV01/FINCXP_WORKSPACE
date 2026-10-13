import * as z from "zod";

const required = { message: "Obligatorio" };

const accountNumber = z
  .string()
  .trim()
  .min(1, required)
  .max(50, { message: "Máximo 50 caracteres" })
  .regex(/^\d+$/, { message: "Solo se permiten números" });

const niu = z
  .string()
  .trim()
  .min(1, required)
  .max(25, { message: "Máximo 25 dígitos" })
  .regex(/^\d+$/, { message: "Solo se permiten números" });

export const supplierUpdateSchema = z.object({
  niu,
  accountNumber,
  status: z.enum(["ACTIVE", "INACTIVE"], { message: "Seleccione un estado" }),
});

export const supplierSchema = z.object({
  name: z.string().trim().min(1, required).max(255, { message: "Máximo 255 caracteres" }),
  nit: z
    .string()
    .min(1, required)
    .regex(/^\d{4}-\d{6}-\d{3}-\d$/, { message: "Formato 0000-000000-000-0" }),
  niu,
  accountNumber,
});
