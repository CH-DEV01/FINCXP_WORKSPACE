import React from "react";
import { Outlet } from "react-router-dom";
import Navbar from "../Navbar";

const MainLayout = () => {
  return (
    <div className="min-h-dvh w-full">
      <Navbar />
      <main className="bg-gray-100 pt-20 w-full min-h-dvh box-border px-3 sm:px-4 md:px-6 lg:px-8 xl:px-10 pb-3">
        <div className="w-full max-w-none">
          <Outlet />
        </div>
      </main>
    </div>
  );
};

export default MainLayout;
