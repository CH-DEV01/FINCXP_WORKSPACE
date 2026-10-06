import * as z from "zod";

export const userSchema = z.object({
  name: z.string().min(1, { message: "El nombre es obligatorio" }),

  email: z
    .string()
    .min(1, { message: "El correo electrónico es obligatorio" }) // <-- Revisa primero si está vacío
    .email({ message: "Debe ser un formato de correo válido" }), // <-- Luego revisa el formato

  dui: z
    .string()
    .min(1, { message: "El DUI es obligatorio" }) // <-- Revisa primero si está vacío
    .regex(/^\d{9}$/, { message: "El DUI debe tener exactamente 9 dígitos numéricos" }), // <-- Luego revisa el formato

  entityId: z.string().min(1, { message: "Debe seleccionar una entidad" }),

  roleId: z.string().min(1, { message: "Debe seleccionar un rol" }),

  status: z.enum(["ACTIVE", "INACTIVE"], { message: "Debe seleccionar un estado" }),
});
