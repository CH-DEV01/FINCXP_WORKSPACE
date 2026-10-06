import React, { useEffect, useState } from "react";
import { useAuth } from "../../context/AuthContext.jsx";

const WelcomeBannerCard = () => {
  const [name, setName] = useState(null);
  const [currentTime, setCurrentTime] = useState(new Date());

  const { userData } = useAuth();

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentTime(new Date());
    }, 1000);

    return () => clearInterval(timer);
  }, []);

  useEffect(() => {
    if (userData) {
      setName(userData.name);
    }
  }, [userData]);

  const formatTime = (date) => {
    return date.toLocaleTimeString([], {
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const formatDate = (date) => {
    const options = {
      weekday: "long",
      year: "numeric",
      month: "long",
      day: "numeric",
    };
    return date.toLocaleDateString("es-ES", options);
  };

  return (
    <div className="px-4 py-3 bg-white rounded-lg border-l-2 border-red-500 shadow-md font-montserrat w-full min-w-0 flex-1">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div className="min-w-0">
          <h1 className="text-sm sm:text-base font-bold text-gray-900 mb-1 truncate">
            Bienvenido/a, <span className="text-red-600 font-bold">{name}</span>
          </h1>
          <div className="w-16 h-1 bg-gradient-to-r from-red-500 to-red-300 rounded-full" />
        </div>

        <div className="flex items-center gap-4 sm:gap-6 shrink-0">
          <div className="flex items-center gap-2">
            <svg className="w-4 h-4 text-gray-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"
              />
            </svg>
            <p className="text-xs font-medium text-gray-500 capitalize">{formatDate(currentTime)}</p>
          </div>

          <div className="w-px h-8 bg-gray-200" />

          <div className="flex items-center gap-2">
            <svg className="w-5 h-5 text-red-500 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
              />
            </svg>
            <span className="text-md font-semibold text-gray-800 tracking-tight">{formatTime(currentTime)}</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default WelcomeBannerCard;
