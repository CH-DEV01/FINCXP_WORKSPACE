import * as z from "zod";

const NAME_WORD = /^\p{L}+(?:['’-]\p{L}+)*$/u;
const NAME_PARTICLES = new Set(["de", "del", "la", "las", "los", "y", "da", "di", "van", "von"]);

const nameWords = (value) => value.trim().split(/\s+/).filter(Boolean);
const isCapitalized = (word) => word[0] === word[0].toLocaleUpperCase("es") && word[0] !== word[0].toLocaleLowerCase("es");

export const userSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, { message: "El nombre es obligatorio" })
    .max(255, { message: "Máximo 255 caracteres" })
    .refine((value) => nameWords(value).length >= 2, {
      message: "Ingrese al menos un nombre y un apellido",
    })
    .refine((value) => nameWords(value).every((word) => NAME_WORD.test(word)), {
      message: "El nombre solo puede contener letras",
    })
    .refine(
      (value) => {
        const words = nameWords(value);
        return words.every(
          (word, i) =>
            isCapitalized(word) || (i > 0 && i < words.length - 1 && NAME_PARTICLES.has(word.toLowerCase())),
        );
      },
      { message: "El nombre y el apellido deben iniciar con mayúscula, por ejemplo: Juan Pérez" },
    ),

  email: z
    .string()
    .min(1, { message: "El correo electrónico es obligatorio" })
    .max(255, { message: "Máximo 255 caracteres" })
    .email({ message: "Debe ser un formato de correo válido" }),

  dui: z
    .string()
    .min(1, { message: "El DUI es obligatorio" }) // <-- Revisa primero si está vacío
    .regex(/^\d{9}$/, { message: "El DUI debe tener exactamente 9 dígitos numéricos" }), // <-- Luego revisa el formato

  entityId: z.string().min(1, { message: "Debe seleccionar una entidad" }),

  roleId: z.string().min(1, { message: "Debe seleccionar un rol" }),

  status: z.enum(["ACTIVE", "INACTIVE"], { message: "Debe seleccionar un estado" }),
});
