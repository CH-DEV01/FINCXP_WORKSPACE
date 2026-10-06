import React from "react";
import { FaSearch, FaTimes } from "react-icons/fa";

const SearchInput = ({ value, onChange, placeholder, ariaLabel, className = "" }) => (
  <div className={`relative ${className}`}>
    <div className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none">
      <FaSearch />
    </div>

    <input
      type="text"
      aria-label={ariaLabel || placeholder}
      placeholder={placeholder}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      className="w-full pl-10 pr-10 py-2 border border-gray-200 rounded-lg bg-white text-sm text-gray-700 font-montserrat placeholder:text-xs placeholder:text-gray-400 focus:outline-none focus:border-red-300 focus:ring-1 focus:ring-red-300 transition-all duration-200"
    />

    {value && (
      <button
        type="button"
        onClick={() => onChange("")}
        className="absolute right-3 top-1/2 -translate-y-1/2 p-1 text-gray-400 hover:text-red-500 rounded-md focus:outline-none focus:ring-2 focus:ring-red-200 transition-colors cursor-pointer"
        title="Limpiar búsqueda"
        aria-label="Limpiar búsqueda"
      >
        <FaTimes className="text-sm" />
      </button>
    )}
  </div>
);

export default SearchInput;
