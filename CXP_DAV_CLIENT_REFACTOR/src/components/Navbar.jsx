import React from "react";
import logo from "../assets/Davivienda-Logo.png";
import Icon from "@mdi/react";
import {
  mdiLogout,
  mdiChevronDown,
  mdiDomain,
  mdiEmailOutline,
  mdiCardAccountDetailsOutline,
} from "@mdi/js";
import { useAuth } from "../context/AuthContext.jsx";
import { useEffect, useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";

const NAV_ITEMS = [
  { path: "documents-history", label: "DOCUMENTOS" },
  { path: "disbursement-terminal", label: "DESEMBOLSOS" },
  { path: "dispersion-terminal", label: "DISPERSIONES" },
];

const ROLE_LABELS = {
  ADMIN: "Administrador",
  PAYER: "Pagador",
  SUPPLIER: "Proveedor",
  SYSTEM_ADMIN: "Administrador del sistema",
};

const UserInfoRow = ({ icon, label, value }) => (
  <div className="flex items-start gap-2">
    <Icon path={icon} size={0.65} className="text-gray-400 mt-0.5 shrink-0" />
    <div className="min-w-0">
      <dt className="text-[10px] uppercase tracking-wide text-gray-400">{label}</dt>
      <dd className="text-gray-700 font-medium truncate" title={value}>
        {value}
      </dd>
    </div>
  </div>
);

const Navbar = () => {
  const { logout, user, userData } = useAuth();
  const initials =
    [userData?.firstName, userData?.lastName]
      .filter(Boolean)
      .map((part) => part.trim().charAt(0).toUpperCase())
      .join("") || "U";
  const [isScrolled, setIsScrolled] = useState(false);
  const navigate = useNavigate();

  const location = useLocation();
  const currentPath = location.pathname;

  const allowedPaths = new Set((user?.routes ?? []).map((route) => route.path));
  const navItems = user?.defaultRoute ? NAV_ITEMS.filter((item) => allowedPaths.has(item.path)) : [];

  const handleLogout = () => {
    logout();
  };

  const goHome = () => {
    if (user?.defaultRoute) navigate(user.defaultRoute);
  };

  const goTo = (path) => {
    if (user?.defaultRoute) navigate(`${user.defaultRoute}/${path}`);
  };

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 10);
    };

    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  return (
    <header
      className={`font-montserrat fixed w-full z-50 transition-all duration-500 ${isScrolled ? "bg-gradient-to-r from-red-600 to-red-800 bg-red-800 shadow-xl py-1.5" : "bg-gradient-to-r from-red-600 to-red-800 bg-red-800 py-2"}`}
    >
      <div className="w-full max-w-none px-3 sm:px-4 md:px-6 lg:px-8 xl:px-10">
        <div className="flex justify-between items-center">
          <div className="flex items-center space-x-2">
            <button
              type="button"
              onClick={goHome}
              aria-label="Ir al inicio"
              className="bg-white p-1.5 rounded-lg shadow-md cursor-pointer transition-transform duration-300 hover:scale-105"
            >
              <img src={logo} alt="Davivienda" className="h-8 object-contain" />
            </button>
            <div className="flex flex-col">
              <span className="text-sm font-medium text-gray-200 tracking-wider uppercase">Banca empresas</span>
              <h1 className="text-xs text-white leading-tight">
                <span className="font-extrabold font-montserrat tracking-wider drop-shadow-sm">
                  FINANCIAMIENTO DE CUENTAS POR PAGAR
                </span>
              </h1>
            </div>
          </div>
          <nav className="flex items-center space-x-3">
            {navItems.map((item) => {
              const isActive = currentPath.includes(item.path);
              return (
                <button
                  key={item.path}
                  type="button"
                  onClick={() => goTo(item.path)}
                  className={`px-4 py-2 rounded-lg text-sm font-semibold tracking-wide transition-all duration-300 cursor-pointer ${
                    isActive
                      ? "text-white bg-red-900 shadow-inner"
                      : "text-white bg-red-700/80 shadow-lg hover:bg-red-900/80"
                  }`}
                >
                  {item.label}
                </button>
              );
            })}
            <p className="text-white p-0 m-0">|</p>
            <div className="relative group">
              <button className="cursor-pointer flex items-center space-x-2 px-3 py-2 rounded-lg text-sm font-medium text-gray-200 hover:text-white hover:bg-red-700/60 transition-all duration-300">
                <div className="w-7 h-7 rounded-full bg-white/20 flex items-center justify-center">
                  <span className="text-white font-medium text-xs">{initials}</span>
                </div>
                <span className="max-w-[160px] truncate">{userData?.firstName || userData?.name || "Usuario"}</span>
                <Icon path={mdiChevronDown} size={0.7} />
              </button>
              <div className="absolute right-0 pt-2 w-72 z-50 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-300 transform translate-y-1 group-hover:translate-y-0">
                <div className="bg-white rounded-lg shadow-xl ring-1 ring-black/5 overflow-hidden">
                  <div className="px-4 py-4 bg-gradient-to-br from-red-50 to-white border-b border-gray-100">
                    <div className="flex items-center gap-3">
                      <div className="w-11 h-11 shrink-0 rounded-full bg-gradient-to-br from-red-600 to-red-800 flex items-center justify-center shadow">
                        <span className="text-white font-semibold text-sm">{initials}</span>
                      </div>
                      <div className="min-w-0">
                        <p className="text-sm font-semibold text-gray-900 truncate" title={userData?.name}>
                          {userData?.name || "Usuario"}
                        </p>
                        {userData?.role && (
                          <span className="inline-block mt-1 px-2 py-0.5 rounded-full text-[10px] font-semibold uppercase tracking-wide bg-red-100 text-red-700">
                            {ROLE_LABELS[userData.role] || userData.role}
                          </span>
                        )}
                      </div>
                    </div>
                  </div>
                  <dl className="px-4 py-3 space-y-2 text-xs">
                    {userData?.entityName && (
                      <UserInfoRow icon={mdiDomain} label="Empresa" value={userData.entityName} />
                    )}
                    {userData?.email && <UserInfoRow icon={mdiEmailOutline} label="Correo" value={userData.email} />}
                    {userData?.dui && (
                      <UserInfoRow icon={mdiCardAccountDetailsOutline} label="DUI" value={userData.dui} />
                    )}
                  </dl>
                  <div className="border-t border-gray-100">
                    <button
                      onClick={handleLogout}
                      className="cursor-pointer flex items-center space-x-2 w-full px-4 py-2.5 text-sm font-medium text-gray-700 hover:bg-red-50 hover:text-red-700 transition-colors"
                    >
                      <Icon path={mdiLogout} size={0.9} className="text-red-600" />
                      <span>Cerrar Sesión</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </nav>
        </div>
      </div>
    </header>
  );
};

export default Navbar;
