import React from "react";

const WindowsSpinner = () => (
  <div className="fixed inset-0 z-50 flex items-center justify-center bg-white bg-opacity-75">
    <div className="relative h-12 w-12 animate-spin">
      <div className="absolute top-0 left-1/2 -translate-x-1/2 h-3 w-3 rounded-full bg-red-600 animate-pulse [animation-delay:-1.0s]"></div>

      <div className="absolute top-[10%] right-[10%] h-3 w-3 rounded-full bg-red-600 animate-pulse [animation-delay:-0.9s]"></div>

      <div className="absolute top-1/2 right-0 -translate-y-1/2 h-3 w-3 rounded-full bg-red-600 animate-pulse [animation-delay:-0.8s]"></div>

      <div className="absolute bottom-[10%] right-[10%] h-3 w-3 rounded-full bg-red-600 animate-pulse [animation-delay:-0.7s]"></div>

      <div className="absolute bottom-0 left-1/2 -translate-x-1/2 h-3 w-3 rounded-full bg-red-600 animate-pulse [animation-delay:-0.6s]"></div>

      <div className="absolute bottom-[10%] left-[10%] h-3 w-3 rounded-full bg-red-600 animate-pulse [animation-delay:-0.5s]"></div>

      <div className="absolute top-1/2 left-0 -translate-y-1/2 h-3 w-3 rounded-full bg-red-600 animate-pulse [animation-delay:-0.4s]"></div>

      <div className="absolute top-[10%] left-[10%] h-3 w-3 rounded-full bg-red-600 animate-pulse [animation-delay:-0.3s]"></div>
    </div>
  </div>
);

export default WindowsSpinner;
