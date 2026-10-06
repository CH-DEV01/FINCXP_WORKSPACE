import React from "react";
import { useState, useEffect } from "react";
import bg from "../../assets/bg_4.jpg";
import { masterAgreementService } from "../../services/admin/masterAgreementService";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { useAgreement } from "../../context/AgreementContext";
import { ROUTES } from "../../constants/routes";

const mapMasterAgreementToOption = (agreement) => ({
  ...agreement,
  name: agreement.PayerName || agreement.payerName || "Convenio sin nombre",
});

const SelectAgreementToStart = () => {
  const [agreements, setAgreements] = useState([]);
  const [selectedAgreementToSave, setSelectedAgreementToSave] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isFetching, setIsFetching] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  const { user, logout } = useAuth();
  const { saveAgreement } = useAgreement();

  useEffect(() => {
    const fetchAgreements = async () => {
      if (!user?.entityId) {
        setError("No se pudo determinar la entidad del usuario.");
        setIsFetching(false);
        return;
      }

      try {
        const data = await masterAgreementService.getMasterAgreementsBySupplier(user.entityId);
        setAgreements(Array.isArray(data) ? data.map(mapMasterAgreementToOption) : []);
      } catch (error) {
        console.error("Error fetching agreements:", error);
        setError("Error al cargar los convenios. Por favor intente más tarde.");
      } finally {
        setIsFetching(false);
      }
    };

    fetchAgreements();
  }, [user]);

  const handleLogout = async () => {
    logout();
  };

  const handleSubmit = () => {
    if (!selectedAgreementToSave) {
      setError("Por favor seleccione un pagador");
      return;
    }

    setIsLoading(true);

    try {
      saveAgreement(selectedAgreementToSave);

      if (!user) {
        navigate(ROUTES.LOGIN);
        return;
      }

      navigate(user.defaultRoute || ROUTES.UNAUTHORIZED);
    } catch (e) {
      console.error("Error saving agreement:", e);
      setError("Error al guardar el convenio seleccionado");
    } finally {
      setIsLoading(false);
    }
  };

  const handleAgreementChange = (agreementId) => {
    const agreement = agreements.find((a) => a.id === agreementId);
    setSelectedAgreementToSave(agreement || null);
    setError(null);
  };

  return (
    <div
      className="min-h-screen bg-cover bg-center bg-no-repeat flex items-center justify-center p-8 font-montserrat"
      style={{ backgroundImage: `url(${bg})` }}
    >
      <div className="w-full md:w-1/3 bg-white rounded-xl shadow-2xl overflow-hidden">
        {isFetching ? (
          <div className="flex justify-center items-center p-8">
            <svg
              className="animate-spin h-8 w-8 text-red-800"
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
            >
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
              <path
                className="opacity-75"
                fill="currentColor"
                d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
              ></path>
            </svg>
          </div>
        ) : (
          <>
            <div className="pt-6 px-6 pb-4">
              <h1 className="text-3xl font-bold text-gray-800">Bienvenido/a</h1>
              {(user?.firstName || user?.lastName) && (
                <p className="text-lg text-gray-500 mt-2">
                  {[user.firstName, user.lastName].filter(Boolean).join(" ")}
                </p>
              )}
            </div>

            <div className="border-b border-gray-300"></div>

            <div className="p-6 space-y-4">
              {error && <div className="text-sm text-red-600 p-2 bg-red-50 rounded-md">{error}</div>}

              <div className="space-y-3">
                <label htmlFor="convenio" className="block text-base font-medium text-gray-700">
                  Seleccionar pagador
                </label>
                <select
                  id="convenio"
                  value={selectedAgreementToSave?.id || ""}
                  onChange={(e) => handleAgreementChange(e.target.value)}
                  className="cursor-pointer block w-full px-4 py-3 text-base rounded-lg border border-gray-300 focus:ring-2 focus:ring-red-500 focus:border-red-600 transition-all"
                  disabled={agreements.length === 0}
                >
                  <option value="" disabled>
                    Seleccionar pagador
                  </option>
                  {agreements.length > 0 ? (
                    agreements.map((agreement) => (
                      <option key={agreement.id} value={agreement.id}>
                        {agreement.name}
                      </option>
                    ))
                  ) : (
                    <option disabled>No hay pagadores disponibles</option>
                  )}
                </select>
                {agreements.length === 0 && !isFetching && (
                  <p className="text-sm text-red-600">No se encontraron pagadores disponibles</p>
                )}
              </div>

              <button
                onClick={handleSubmit}
                disabled={!selectedAgreementToSave || isLoading || agreements.length === 0}
                className={`cursor-pointer w-full ${
                  !selectedAgreementToSave || isLoading || agreements.length === 0
                    ? "bg-gray-400 cursor-not-allowed"
                    : "bg-red-800 hover:bg-red-700"
                } 
                                    text-white py-3 px-4 rounded-full transition flex items-center justify-center`}
              >
                {isLoading ? (
                  <>
                    <svg
                      className="animate-spin -ml-1 mr-3 h-5 w-5 text-white"
                      xmlns="http://www.w3.org/2000/svg"
                      fill="none"
                      viewBox="0 0 24 24"
                    >
                      <circle
                        className="opacity-25"
                        cx="12"
                        cy="12"
                        r="10"
                        stroke="currentColor"
                        strokeWidth="4"
                      ></circle>
                      <path
                        className="opacity-75"
                        fill="currentColor"
                        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                      ></path>
                    </svg>
                    Procesando...
                  </>
                ) : (
                  "Continuar"
                )}
              </button>

              <button
                onClick={handleLogout}
                className={`cursor-pointer w-full text-white py-3 px-4 rounded-full flex items-center justify-center bg-gray-600 hover:bg-gray-700 transition`}
              >
                Cerrar sesión
              </button>
            </div>

            <div className="px-6 py-4 bg-gray-50 border-t border-gray-200">
              <p className="text-sm text-gray-500 text-center">
                © {new Date().getFullYear()} Davivienda S.A de C.V. Todos los derechos reservados.
              </p>
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default SelectAgreementToStart;
