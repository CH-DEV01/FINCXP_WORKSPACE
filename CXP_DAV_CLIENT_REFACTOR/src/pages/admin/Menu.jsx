import React, { useMemo } from "react";
import {
  FaDollarSign,
  FaClipboardList,
  FaUsers,
  FaBuilding,
  FaUniversity,
  FaCreditCard,
  FaHandshake,
  FaHistory,
  FaFileContract,
  FaCalendarAlt,
  FaExchangeAlt,
  FaListAlt,
  FaFolderOpen,
} from "react-icons/fa";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

const menuItems = [
  {
    id: "upload-file-admin",
    icon: FaClipboardList,
    title: "Carga de documentos",
    description: "Sube y gestiona los documentos clave de tu operación.",
  },
  {
    id: "master-agreement-management",
    icon: FaHandshake,
    title: "Gestion de convenios comerciales",
    description: "Administre los términos y condiciones para los convenios comerciales.",
  },
  {
    id: "user-management",
    icon: FaUsers,
    title: "Gestion de usuarios",
    description: "Controla los usuarios vinculados y sus permisos en la plataforma.",
  },
  {
    id: "payer-management-admin",
    icon: FaUniversity,
    title: "Gestion de pagadores",
    description: "Controla los pagadores vinculados en la plataforma.",
  },
  {
    id: "supplier-management",
    icon: FaBuilding,
    title: "Gestion de proveedores",
    description: "Controla los proveedores vinculados en la plataforma.",
  },
  {
    id: "payer-credit-line-management",
    icon: FaCreditCard,
    title: "Gestion de cupos de credito",
    description: "Administre el flujo operativo de las líneas de crédito.",
  },
  {
    id: "disbursement-terminal",
    icon: FaDollarSign,
    title: "Terminal de desembolsos",
    description: "Genera los lotes de las solicitudes de desembolso y confirma su desembolso.",
  },
  {
    id: "batches-history",
    icon: FaHistory,
    title: "Bitacora de lotes",
    description: "Consulta los lotes de desembolso generados, su estado y quién los gestionó.",
  },
  {
    id: "dispersion-terminal",
    icon: FaExchangeAlt,
    title: "Terminal de dispersiones",
    description: "Genera las solicitudes de dispersión de los documentos no anticipados y confirma su dispersión.",
  },
  {
    id: "dispersion-batches-history",
    icon: FaListAlt,
    title: "Bitacora de dispersiones",
    description: "Consulta los lotes de dispersión generados, su estado y quién los gestionó.",
  },
  {
    id: "term-management",
    icon: FaFileContract,
    title: "Terminos y condiciones",
    description: "Redacta y publica las versiones del texto legal que aceptan pagadores y proveedores.",
  },
  {
    id: "holiday-management",
    icon: FaCalendarAlt,
    title: "Calendario de dias feriados",
    description: "Registra los días feriados que se excluyen al calcular las fechas de desembolso.",
  },
  {
    id: "upload-resources-management",
    icon: FaFolderOpen,
    title: "Recursos de carga",
    description: "Publica la plantilla de Excel y el manual que se ofrecen en la carga de documentos.",
  },
];

const Menu = () => {
  const navigate = useNavigate();
  const { user } = useAuth();

  const visibleItems = useMemo(() => {
    const allowedPaths = new Set((user?.routes ?? []).map((route) => route.path));
    return menuItems.filter((item) => allowedPaths.has(item.id));
  }, [user]);

  const handleCardClick = (path) => {
    navigate(`${user.defaultRoute}/${path}`);
  };

  const handleKeyDown = (e, path) => {
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      handleCardClick(path);
    }
  };

  return (
    <div className="min-h-screen">
      <div
        className="
                space-y-3 
                w-full
                max-w-8xl mx-auto
            "
      >
        {visibleItems.map((item) => (
          <div
            key={item.id}
            onClick={() => handleCardClick(item.id)}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => handleKeyDown(e, item.id)}
            className="
                            group
                            bg-white font-montserrat rounded-xl shadow-lg 
                            p-4 flex flex-row items-center justify-between
                            cursor-pointer transition-all duration-300
                            hover:shadow-xl hover:-translate-y-1
                            border-b-4 border-transparent hover:border-red-500
                            focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-opacity-50
                        "
          >
            <div className="flex items-center gap-4">
              <div
                className="
                                bg-red-100 p-2 rounded-full shadow-inner inline-block flex-shrink-0
                                transition-transform duration-300
                                group-hover:scale-105
                            "
              >
                <item.icon className="text-red-500 text-xl" />
              </div>

              <div>
                <h3 className="text-xs uppercase font-montserrat font-semibold text-gray-600 mb-0.5">{item.title}</h3>
                <p className="text-xs text-gray-600 leading-normal">{item.description}</p>
              </div>
            </div>
            <div className="ml-4 flex-shrink-0">
              <div className="flex items-center text-red-600 font-medium">
                <span className="text-xs">Administrar</span>
                <svg
                  className="h-4 w-4 ml-1.5 transition-transform duration-200 group-hover:translate-x-1"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14 5l7 7m0 0l-7 7m7-7H3" />
                </svg>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default Menu;
