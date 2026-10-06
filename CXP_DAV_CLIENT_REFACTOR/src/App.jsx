import React, { useEffect } from "react";
import { BrowserRouter, Routes, Route, Navigate, Outlet } from "react-router-dom";
import { AgreementProvider } from "./context/AgreementContext";
import { AuthProvider, useAuth } from "./context/AuthContext";
import LoadingSpinner from "./components/LoadingSpinner";
import { PORTAL_URL } from "./constants/apiConstants";
import { ROLES } from "./constants/roles";
import { ROUTES } from "./constants/routes";

import Menu from "./pages/admin/Menu";
import AgreementManagement from "./pages/admin/AgreementManagement";
import ParamManagement from "./pages/admin/ParamManagement";
import PayerManagementAdmin from "./pages/admin/PayerManagementAdmin";
import UserManagement from "./pages/admin/UserManagement";
import UploadFilePageAdmin from "./pages/admin/UploadFilePageAdmin";
import SupplierManagement from "./pages/admin/SupplierManagement";
import HolidayManagement from "./pages/admin/HolidayManagement";
import PayerCreditLineManager from "./pages/admin/PayerCreditLineManager";
import TermManagement from "./pages/admin/TermManagement";
import UploadResourceManagement from "./pages/admin/UploadResourceManagement";
import ControlTerminal from "./pages/operator/ControlTerminal";
import BatchLog from "./pages/operator/BatchLog";
import DispersionTerminal from "./pages/operator/DispersionTerminal";
import DispersionBatchLog from "./pages/operator/DispersionBatchLog";
import OperatorDocumentLog from "./pages/operator/OperatorDocumentLog";
import UploadFilePage from "./pages/payer/UploadFilePage";
import PayerDocumentLog from "./pages/payer/PayerDocumentLog";
import SelectDocuments from "./pages/supplier/SelectDocuments";
import DocumentLog from "./pages/supplier/DocumentLog";
import ResourceNotFound from "./pages/shared-pages/ResourceNotFound";
import Unauthorized from "./pages/shared-pages/Unauthorized";
import SelectAgreementToStart from "./pages/shared-pages/SelectAgreementToStart";
import Login from "./pages/auth/Login";
import Layout from "./components/layouts/MainLayout";

/** Traduce el componentName que envía el backend en user.routes al componente real. */
const COMPONENT_REGISTRY = {
  Menu,
  AgreementManagement,
  PayerManagementAdmin,
  UserManagement,
  ParamManagement,
  UploadFilePageAdmin,
  TermManagement,
  HolidayManagement,
  UploadResourceManagement,
  PayerCreditLineManager,
  SupplierManagement,
  ControlTerminal,
  BatchLog,
  DispersionTerminal,
  DispersionBatchLog,
  OperatorDocumentLog,
  UploadFilePage,
  PayerDocumentLog,
  SelectDocuments,
  DocumentLog,
};

const ProtectedRoute = ({ allowedRoles }) => {
  const { user, isLoading } = useAuth();

  useEffect(() => {
    if (!user && !isLoading) {
      window.location.href = PORTAL_URL;
    }
  }, [user, isLoading]);

  if (isLoading) return <LoadingSpinner />;
  if (!user) return null;

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to={ROUTES.UNAUTHORIZED} replace />;
  }

  return <Outlet />;
};

/** Ruta de aterrizaje post-login (puede diferir de la base del Layout). */
const getLandingRoute = (user) => {
  if (!user) return ROUTES.UNAUTHORIZED;
  if (user.role === ROLES.SUPPLIER) return ROUTES.SELECT_AGREEMENT;
  return user.defaultRoute || ROUTES.UNAUTHORIZED;
};

const DynamicRedirect = () => {
  const { user, isLoading } = useAuth();
  if (isLoading) return <LoadingSpinner />;
  if (!user) return null;
  return <Navigate to={getLandingRoute(user)} replace />;
};

const renderDynamicRoutes = (routes) =>
  routes.map((route) => {
    const Component = COMPONENT_REGISTRY[route.componentName];

    if (!Component) {
      console.warn(`Componente no encontrado en registro: ${route.componentName}`);
      return null;
    }

    if (route.isIndex) {
      return <Route key="index" index element={<Component />} />;
    }

    return <Route key={route.path} path={route.path} element={<Component />} />;
  });

const AppContent = () => {
  const { user, isLoading } = useAuth();

  if (isLoading) return <LoadingSpinner />;

  return (
    <Routes>
      <Route path={ROUTES.ROOT} element={<DynamicRedirect />} />
      <Route path={ROUTES.UNAUTHORIZED} element={<Unauthorized />} />
      <Route path={ROUTES.LOGIN} element={<Login />} />

      {/* Selector de convenio (fuera del Layout; landing del proveedor) */}
      <Route element={<ProtectedRoute allowedRoles={[ROLES.SUPPLIER]} />}>
        <Route path={ROUTES.SELECT_AGREEMENT} element={<SelectAgreementToStart />} />
      </Route>

      {user?.defaultRoute && (
        <Route element={<ProtectedRoute />}>
          <Route path={user.defaultRoute.replace(/^\//, "")} element={<Layout />}>
            {renderDynamicRoutes(user.routes ?? [])}
          </Route>
        </Route>
      )}

      <Route path="*" element={<ResourceNotFound />} />
    </Routes>
  );
};

function App() {
  return (
    <BrowserRouter basename="/financiamientocuentasporpagar/">
      <AuthProvider>
        <AgreementProvider>
          <AppContent />
        </AgreementProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
