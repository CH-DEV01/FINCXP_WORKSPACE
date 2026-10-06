# CXP_DAV_CLIENT — Financiamiento de Cuentas por Pagar

Cliente web (React 19 + Vite 6 + Tailwind 4) del sistema de Financiamiento de Cuentas por Pagar de Davivienda. Consume la API de `CXP_DAV_SERVER_REFACTOR`.

## Requisitos

- Node.js 18 o superior
- Backend corriendo y accesible desde el navegador

## Configuración

Copie `.env.example` al archivo del ambiente (`.env.development` o `.env.production`) y complete:

| Variable | Descripción |
|---|---|
| `VITE_API_BASE_URL` | URL base del backend, sin `/api` ni barra final |
| `VITE_PORTAL_URL` | Portal del banco al que se redirige sin sesión o al salir |

Si falta alguna variable la aplicación no arranca y muestra el nombre de la variable faltante.

## Comandos

| Comando | Uso |
|---|---|
| `npm install` | Instala dependencias |
| `npm run dev` | Servidor de desarrollo |
| `npm run build` | Genera `dist/` (en Windows también `build.bat`) |
| `npm run preview` | Sirve el build localmente |
| `npm run lint` | ESLint |
| `npm run format` | Prettier sobre `src/` |
| `npm test` | Pruebas con Vitest |

La aplicación se publica bajo la ruta `/financiamientocuentasporpagar/` (ver `base` en `vite.config.js`).

## Estructura

- `src/pages/<rol>/` — pantallas por rol (admin, operator, payer, supplier, system)
- `src/components/` — componentes compartidos
- `src/services/` — llamadas a la API; cada función devuelve `response.data.data`
- `src/utils/` — formato, errores y utilidades
- `src/context/` — sesión (`AuthContext`) y convenio seleccionado (`AgreementContext`)

Las rutas y menús de cada rol vienen del backend (`routes_cat`, `menus_cat`); el componente de cada ruta se resuelve en `COMPONENT_REGISTRY` de `src/App.jsx`.

## Convenciones

- Finales de línea CRLF (ver `.editorconfig` y `.gitattributes`).
- Textos de la interfaz en español; identificadores en inglés camelCase.
