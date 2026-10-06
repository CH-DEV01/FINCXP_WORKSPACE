import React from "react";
import { Link } from "react-router-dom";
import { ROUTES } from "../../constants/routes";

const ResourceNotFound = () => {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-gray-100 text-gray-800 p-4 sm:p-6 lg:p-8 text-center overflow-hidden">
      <h1 className="text-4xl sm:text-5xl md:text-6xl font-bold mb-4">Recurso no encontrado</h1>
      <p className="text-lg sm:text-xl md:text-2xl mb-6 max-w-xs sm:max-w-sm md:max-w-md lg:max-w-lg px-4">
        Lo sentimos, el recurso que estás buscando no se pudo encontrar.
      </p>
      <Link
        to={ROUTES.ROOT}
        className="bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 px-6 rounded-lg shadow-md transition duration-300 ease-in-out text-base sm:text-lg"
      >
        Volver a la página de inicio
      </Link>
    </div>
  );
};

export default ResourceNotFound;
