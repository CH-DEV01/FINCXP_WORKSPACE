import React from "react";
import { Link } from "react-router-dom";
import { ROUTES } from "../../constants/routes";

const Unauthorized = () => {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-red-100 text-red-800 p-4 sm:p-6 lg:p-8 text-center overflow-hidden">
      <h1 className="text-4xl sm:text-5xl md:text-6xl font-bold mb-4">
        <span role="img" aria-label="No Entry Sign" className="mr-2 text-5xl sm:text-6xl md:text-7xl">
          🚫
        </span>
        Acceso no autorizado
      </h1>
      <p className="text-lg sm:text-xl md:text-2xl mb-6 max-w-xs sm:max-w-sm md:max-w-md lg:max-w-lg px-4">
        Lo sentimos, no tienes permiso para acceder a esta página.
      </p>
      <Link
        to={ROUTES.ROOT}
        className="bg-red-600 hover:bg-red-700 text-white font-semibold py-2 px-6 rounded-lg shadow-md transition duration-300 ease-in-out text-base sm:text-lg"
      >
        Volver a la página de inicio
      </Link>
    </div>
  );
};

export default Unauthorized;
