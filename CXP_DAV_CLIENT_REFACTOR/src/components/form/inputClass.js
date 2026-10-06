/** Clases de los inputs de formulario; `locked` los muestra como solo lectura. */
const inputClass = (hasError, locked = false) =>
  `w-full px-3 py-2 border rounded-lg focus:outline-none text-xs font-montserrat ${
    locked
      ? "bg-gray-100 text-gray-500 border-gray-200 cursor-not-allowed"
      : `bg-white focus:ring-2 ${hasError ? "border-red-500 focus:ring-red-300" : "border-gray-300 focus:ring-red-300"}`
  }`;

export default inputClass;
