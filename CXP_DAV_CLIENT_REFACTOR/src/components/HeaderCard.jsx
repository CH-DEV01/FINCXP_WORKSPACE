import React from "react";

/**
 * Un componente de cabecera estilizado como una tarjeta.
 *
 * @param {string} title - El texto del título principal.
 * @param {string} description - El texto del subtítulo.
 * @param {React.Component} icon - El icono principal a mostrar (ej: <FaUsers />).
 */
const HeaderCard = ({ title, description, icon }) => {
  return (
    <div
      className="                             
      p-4 pr-0 flex flex-col md:flex-row justify-between items-center              
      border-l-2 border-red-500
      relative overflow-hidden
    "
    >
      <div className="flex items-center mb-4 md:mb-0">
        {icon && (
          <div className="bg-red-100 p-2 rounded-full mr-3">
            {React.cloneElement(icon, { className: "text-red-600 text-xl" })}
          </div>
        )}

        <div>
          <h1
            className="
            text-xs
            uppercase                  
            text-gray-600
            font-semibold           
            font-montserrat
            mb-1
          "
          >
            {title}
          </h1>
          <h4
            className="
            font-montserrat
            text-xs                   
            text-gray-400             
          "
          >
            {description}
          </h4>
        </div>
      </div>

      <div className="absolute right-0 top-0 h-full overflow-hidden pointer-events-none">
        {icon &&
          React.cloneElement(icon, {
            className: "text-red-600/5 text-8xl -rotate-12 translate-x-1/20 translate-y-1/20",
          })}
      </div>
    </div>
  );
};

export default HeaderCard;
