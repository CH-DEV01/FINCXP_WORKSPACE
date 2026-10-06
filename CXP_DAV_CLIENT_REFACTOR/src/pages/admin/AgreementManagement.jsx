import React, { useState, useEffect, useCallback } from "react";
import { FaHandshake } from "react-icons/fa";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";

import { entityService } from "../../services/admin/entityService.js";
import { masterAgreementService } from "../../services/admin/masterAgreementService.js";
import { policyService } from "../../services/admin/policyService.js";

import Table from "../../components/Table";
import HeaderCard from "../../components/HeaderCard";
import Modal from "../../components/Modal";
import SearchInput from "../../components/SearchInput";
import StatusBadge from "../../components/StatusBadge";
import FieldError from "../../components/form/FieldError";
import PayerDirectory from "../../components/payer/PayerDirectory.jsx";
import useServerPagination from "../../hooks/useServerPagination";
import { STATUS_OPTIONS } from "../../constants/status";
import { apiErrorMessage, showError, showLoading, showSuccess } from "../../utils/errors";

const masterAgreementSchema = z.object({
  status: z.string().min(1, "El estado es requerido"),
  agreementType: z.string().min(1, "El tipo de convenio es requerido"),
  payerId: z.string().uuid("Seleccione un pagador válido"),
  supplierId: z.string().uuid("Seleccione un proveedor válido"),
  paymentPolicyId: z.string().uuid("Seleccione una política de pago"),
  disbursementPolicyId: z.string().uuid("Seleccione una política de desembolso"),
});

const ITEMS_PER_PAGE = 10;

const columns = [
  {
    header: "Proveedor",
    accessor: "supplierName",
    render: (val) => <span className="font-semibold text-slate-700 text-xs">{val}</span>,
  },
  {
    header: "Politica de pago",
    accessor: "paymentPolicyDays",
    render: (val) => <span className="text-xs">{val}</span>,
  },
  {
    header: "Politica de desembolso",
    accessor: "disbursementPolicyName",
    render: (val) => <span className="text-xs">{val}</span>,
  },
  {
    header: "Tipo Convenio",
    accessor: "agreementType",
    render: (val) => <span className="text-xs">{val}</span>,
  },
  {
    header: "Estado",
    accessor: "status",
    render: (val) => <StatusBadge status={val} />,
  },
];

const lockedClass = (isLocked) =>
  isLocked ? "bg-gray-100 text-gray-500 cursor-not-allowed pointer-events-none" : "bg-white";

// No se usa `disabled` porque react-hook-form dejaría el campo fuera de los valores enviados.
const lockedProps = (isLocked) => (isLocked ? { tabIndex: -1, "aria-disabled": true } : {});

const logLoadError = (error) => console.error("Error fetching master agreements:", error);

const MasterAgreementManagement = () => {
  const [payers, setPayers] = useState([]);
  const [isLoadingPayers, setIsLoadingPayers] = useState(true);
  const [selectedPayer, setSelectedPayer] = useState(null);
  const [suppliers, setSuppliers] = useState([]);
  const [paymentPolicies, setPaymentPolicies] = useState([]);
  const [disbursementPolicies, setDisbursementPolicies] = useState([]);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingParam, setEditingParam] = useState(null);
  const selectedPayerId = selectedPayer?.id ?? null;

  const fetchAgreements = useCallback(
    ({ page, search }) =>
      masterAgreementService.getMasterAgreements({ page, size: ITEMS_PER_PAGE, search, payerId: selectedPayerId }),
    [selectedPayerId],
  );

  const {
    items: visibleAgreements,
    totalPages,
    isLoading,
    searchTerm,
    setSearchTerm,
    currentPage,
    setCurrentPage,
    reload,
  } = useServerPagination(selectedPayerId ? fetchAgreements : null, { onError: logLoadError });

  const selectPayer = (payer) => {
    setSelectedPayer(payer);
    setCurrentPage(1);
  };

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(masterAgreementSchema),
    defaultValues: {
      status: "ACTIVE",
      agreementType: "STANDARD",
      payerId: "",
      supplierId: "",
      paymentPolicyId: "",
      disbursementPolicyId: "",
    },
  });

  useEffect(() => {
    const fetchCatalogs = async () => {
      try {
        const [payerPage, supplierPage, payment, disbursement] = await Promise.all([
          entityService.getPayers(),
          entityService.getSuppliers(),
          policyService.getPaymentPolicies(),
          policyService.getDisbursementPolicies(),
        ]);

        const payerList = payerPage?.content ?? [];
        setPayers(payerList);
        setSelectedPayer(payerList[0] ?? null);
        setSuppliers(supplierPage?.content ?? []);
        setPaymentPolicies(payment);
        setDisbursementPolicies(disbursement);
      } catch (error) {
        console.error("Error al cargar los catálogos", error);
      } finally {
        setIsLoadingPayers(false);
      }
    };
    fetchCatalogs();
  }, []);

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setEditingParam(null);
    reset();
  };

  const onSubmit = async (formData) => {
    showLoading("Guardando la información, por favor espere.");

    try {
      const requestPayload = {
        status: formData.status,
        agreementType: formData.agreementType,
        payerId: formData.payerId,
        supplierId: formData.supplierId,
        paymentPolicyId: formData.paymentPolicyId,
        disbursementPolicyId: formData.disbursementPolicyId,
      };

      if (editingParam) {
        await masterAgreementService.updateMasterAgreement(editingParam.id, requestPayload);
      } else {
        await masterAgreementService.createMasterAgreement(requestPayload);
      }

      showSuccess(editingParam ? "Convenio actualizado correctamente." : "Convenio registrado correctamente.");
      reload();
      handleCloseModal();
    } catch (error) {
      showError(apiErrorMessage(error, "No se pudo guardar el convenio. Intenta de nuevo."));
    }
  };

  const handleEdit = (param) => {
    setEditingParam(param);

    reset({
      status: param.status || "ACTIVE",
      agreementType: param.agreementType || "STANDARD",
      payerId: param.payerId || "",
      supplierId: param.supplierId || "",
      paymentPolicyId: param.paymentPolicyId || "",
      disbursementPolicyId: param.disbursementPolicyId || "",
    });
    setIsModalOpen(true);
  };

  return (
    <div className="flex flex-col w-full lg:h-[calc(100dvh-5.5rem)] min-h-0 pb-3">
      <div className="shrink-0">
        <HeaderCard
          title={"Gestion de Convenios Marco"}
          description={"Administre los acuerdos comerciales entre pagadores y proveedores."}
          icon={<FaHandshake />}
        />
      </div>

      <div className="shrink-0 border-b border-gray-200 mb-4"></div>

      <div className="flex flex-col lg:flex-row flex-1 min-h-0 gap-4">
        <aside className="lg:w-64 xl:w-72 shrink-0 flex flex-col min-h-[480px] lg:min-h-0 shadow-2xl bg-gradient-to-r from-red-600 to-red-800 bg-red-800 text-white p-4 rounded-xl font-montserrat">
          <PayerDirectory
            payers={payers}
            isLoading={isLoadingPayers}
            selectedPayerId={selectedPayerId}
            onSelect={selectPayer}
          />
        </aside>

        <section className="flex-1 min-w-0 min-h-0 flex flex-col">
          {selectedPayer ? (
            <>
              <div className="shrink-0 flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div className="min-w-0">
                  <h2 className="text-sm font-bold text-gray-900 truncate">Convenios de {selectedPayer.name}</h2>
                  <p className="text-xs text-gray-500">NIT {selectedPayer.nit || "-"}</p>
                </div>
                <SearchInput
                  value={searchTerm}
                  onChange={setSearchTerm}
                  placeholder="Buscar por proveedor..."
                  ariaLabel="Buscar convenios del pagador"
                  className="w-full md:w-1/2 shadow-sm rounded-lg"
                />
              </div>

              <div className="mt-4 flex-1 min-h-0 flex flex-col">
                <Table
                  columns={columns}
                  data={visibleAgreements}
                  isLoading={isLoading}
                  onEdit={handleEdit}
                  currentPage={currentPage}
                  totalPages={totalPages}
                  onPageChange={setCurrentPage}
                  fillHeight
                />
              </div>
            </>
          ) : (
            !isLoadingPayers && (
              <div className="flex-1 flex items-center justify-center min-h-[200px] bg-white border border-gray-200 rounded-lg shadow-sm p-6 text-sm text-gray-500">
                Seleccione un pagador del directorio para ver sus convenios.
              </div>
            )
          )}
        </section>
      </div>

      <Modal
        isOpen={isModalOpen}
        onClose={handleCloseModal}
        title={editingParam ? "Modificar convenio marco" : "Registrar nuevo convenio marco"}
        footer={
          <>
            <button
              onClick={handleCloseModal}
              className="py-2 px-4 rounded-lg text-xs font-medium bg-white text-gray-700 border border-gray-300 hover:bg-gray-100 hover:cursor-pointer transition-colors"
            >
              Cancelar
            </button>
            <button
              onClick={handleSubmit(onSubmit)}
              className="py-2 px-4 rounded-lg text-xs font-medium bg-red-600 text-white hover:bg-red-700 shadow-md hover:cursor-pointer transition-colors"
            >
              {editingParam ? "Guardar cambios" : "Registrar Convenio"}
            </button>
          </>
        }
      >
        <form className="grid grid-cols-2 gap-4">
          {/* Estado, tipo, pagador y proveedor quedan bloqueados en edición; solo cambian las políticas */}
          <div className="col-span-2 md:col-span-1">
            <label className="block text-xs font-semibold text-gray-600 mb-1">
              Estado {!editingParam && <span className="text-red-500">*</span>}
            </label>
            <select
              {...register("status")}
              {...lockedProps(editingParam)}
              className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 border-gray-300 focus:ring-red-300 text-sm font-montserrat
                                ${lockedClass(editingParam)}`}
            >
              {STATUS_OPTIONS.map(({ value, label }) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </div>

          <div className="col-span-2 md:col-span-1">
            <label className="block text-xs font-semibold text-gray-600 mb-1">
              Tipo de Convenio {!editingParam && <span className="text-red-500">*</span>}
            </label>
            <select
              {...register("agreementType")}
              {...lockedProps(editingParam)}
              className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 text-sm font-montserrat 
                                ${errors.agreementType ? "border-red-500 focus:ring-red-300" : "border-gray-300 focus:ring-red-300"}
                                ${lockedClass(editingParam)}`}
            >
              <option value="STANDARD">Estándar</option>
              <option value="SPECIAL">Especial</option>
            </select>
          </div>

          <div className="col-span-2 md:col-span-1">
            <label className="block text-xs font-semibold text-gray-600 mb-1">
              Pagador {!editingParam && <span className="text-red-500">*</span>}
            </label>
            <select
              {...register("payerId")}
              {...lockedProps(editingParam)}
              className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 text-sm font-montserrat 
                                ${errors.payerId ? "border-red-500 focus:ring-red-300" : "border-gray-300 focus:ring-red-300"}
                                ${lockedClass(editingParam)}`}
            >
              <option value="">Seleccione un pagador</option>
              {payers.map((payer) => (
                <option key={payer.id} value={payer.id}>
                  {payer.name}
                </option>
              ))}
            </select>
            <FieldError error={errors.payerId} />
          </div>

          <div className="col-span-2 md:col-span-1">
            <label className="block text-xs font-semibold text-gray-600 mb-1">
              Proveedor {!editingParam && <span className="text-red-500">*</span>}
            </label>
            <select
              {...register("supplierId")}
              {...lockedProps(editingParam)}
              className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 text-sm font-montserrat 
                                ${errors.supplierId ? "border-red-500 focus:ring-red-300" : "border-gray-300 focus:ring-red-300"}
                                ${lockedClass(editingParam)}`}
            >
              <option value="">Seleccione un proveedor</option>
              {suppliers.map((supplier) => (
                <option key={supplier.id} value={supplier.id}>
                  {supplier.name}
                </option>
              ))}
            </select>
            <FieldError error={errors.supplierId} />
          </div>

          <div className="col-span-2 md:col-span-1">
            <label className="block text-xs font-semibold text-gray-600 mb-1">
              Política de Pago <span className="text-red-500">*</span>
            </label>
            <select
              {...register("paymentPolicyId")}
              className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 text-sm font-montserrat bg-white
                                ${errors.paymentPolicyId ? "border-red-500 focus:ring-red-300" : "border-gray-300 focus:ring-blue-300"}`}
            >
              <option value="">Seleccione política de pago</option>
              {paymentPolicies.map((policy) => (
                <option key={policy.id} value={policy.id}>
                  {policy.description}
                </option>
              ))}
            </select>
            <FieldError error={errors.paymentPolicyId} />
          </div>

          <div className="col-span-2 md:col-span-1">
            <label className="block text-xs font-semibold text-gray-600 mb-1">
              Política de Desembolso <span className="text-red-500">*</span>
            </label>
            <select
              {...register("disbursementPolicyId")}
              className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 text-sm font-montserrat bg-white
                                ${errors.disbursementPolicyId ? "border-red-500 focus:ring-red-300" : "border-gray-300 focus:ring-blue-300"}`}
            >
              <option value="">Seleccione pol. de desembolso</option>
              {disbursementPolicies.map((policy) => (
                <option key={policy.id} value={policy.id}>
                  {policy.name}
                </option>
              ))}
            </select>
            <FieldError error={errors.disbursementPolicyId} />
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default MasterAgreementManagement;
