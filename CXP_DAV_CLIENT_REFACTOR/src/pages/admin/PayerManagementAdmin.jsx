import React, { useState, useRef, useCallback } from "react";
import { FaBuilding, FaPlus } from "react-icons/fa";
import Swal from "sweetalert2";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import Table from "../../components/Table";
import HeaderCard from "../../components/HeaderCard";
import Modal from "../../components/Modal";
import SearchInput from "../../components/SearchInput";
import StatusBadge from "../../components/StatusBadge";

import { entityService } from "../../services/admin/entityService";
import { payerSchema, payerUpdateSchema, payerUpdateWithoutAccountSchema } from "../../schemas/payerSchema";
import useServerPagination from "../../hooks/useServerPagination";
import { formatCurrency } from "../../utils/format";
import { apiErrorMessage, showError, showLoading, showSuccess } from "../../utils/errors";
import PayerFormFields from "./payer-management/PayerFormFields";
import {
  CALCULATION_BASE_LABELS,
  DEFAULT_FORM_VALUES,
  formatFractionAsPercent,
  percentToFraction,
  toPayerFormValues,
} from "./payer-management/payerForm";

const createResolver = zodResolver(payerSchema);
const updateResolver = zodResolver(payerUpdateSchema);
const updateWithoutAccountResolver = zodResolver(payerUpdateWithoutAccountSchema);

const ITEMS_PER_PAGE = 10;

const columns = [
  { header: "NOMBRE", accessor: "name" },
  { header: "NIT", accessor: "nit" },
  {
    header: "CUENTA BANCARIA",
    accessor: "accountNumber",
    render: (value) => value || "—",
  },
  {
    header: "N° CUPO",
    accessor: "creditFacilityNumber",
    render: (value) => value || "—",
  },
  {
    header: "MONTO APROBADO",
    accessor: "facilityLimitAmount",
    render: formatCurrency,
  },
  {
    header: "CONSUMIDO",
    accessor: "amountInUse",
    render: formatCurrency,
  },
  {
    header: "% INTERÉS",
    accessor: "interestRate",
    render: formatFractionAsPercent,
  },
  {
    header: "% COMISIÓN",
    accessor: "commissionRate",
    render: formatFractionAsPercent,
  },
  {
    header: "BASE CÁLCULO",
    accessor: "calculationBase",
    render: (value) => CALCULATION_BASE_LABELS[value] || "—",
  },
  {
    header: "UMBRAL",
    accessor: "warningThresholdPercentage",
    render: formatFractionAsPercent,
  },
  {
    header: "ESTADO",
    accessor: "status",
    render: (value) => <StatusBadge status={value === "ACTIVE" ? "ACTIVE" : "INACTIVE"} />,
  },
];

const logLoadError = (error) => console.error("Error al cargar pagadores", error);

const PayerManagementAdmin = () => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingPayer, setEditingPayer] = useState(null);

  const fetchPayers = useCallback(
    ({ search, page }) => entityService.getPayerSummaries({ search, page, size: ITEMS_PER_PAGE }),
    [],
  );

  const {
    items: payers,
    totalPages,
    isLoading,
    searchTerm,
    setSearchTerm,
    currentPage,
    setCurrentPage,
    reload,
  } = useServerPagination(fetchPayers, { onError: logLoadError });

  const isEditing = editingPayer !== null;
  const canEditAccount = !isEditing || Boolean(editingPayer.bankAccountId);
  const resolverRef = useRef(createResolver);
  resolverRef.current = !isEditing ? createResolver : canEditAccount ? updateResolver : updateWithoutAccountResolver;

  const form = useForm({
    resolver: (values, context, options) => resolverRef.current(values, context, options),
    defaultValues: DEFAULT_FORM_VALUES,
  });
  const {
    handleSubmit,
    reset,
    formState: { isSubmitting },
  } = form;

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setEditingPayer(null);
    reset(DEFAULT_FORM_VALUES);
  };

  const submitUpdate = async (data) => {
    showLoading();

    try {
      await entityService.updatePayer(editingPayer.id, {
        accountNumber: canEditAccount ? data.accountNumber.trim() : null,
        interestRate: percentToFraction(data.interestRate),
        commissionRate: percentToFraction(data.commissionRate),
        calculationBase: data.calculationBase,
        status: data.status,
        warningThresholdPercentage: percentToFraction(data.warningThreshold),
      });

      showSuccess("Pagador actualizado correctamente.");
      handleCloseModal();
      reload();
    } catch (error) {
      showError(apiErrorMessage(error, "No se pudo actualizar el pagador."));
    }
  };

  const submitCreate = async (data) => {
    showLoading("Registrando el pagador, por favor espere.");

    try {
      await entityService.registerPayer({
        name: data.name.trim(),
        nit: data.nit,
        accountNumber: data.accountNumber.trim(),
        creditFacilityNumber: data.creditFacilityNumber.trim(),
        facilityLimitAmount: Number(data.approvedAmount),
        interestRate: percentToFraction(data.interestRate),
        commissionRate: percentToFraction(data.commissionRate),
        calculationBase: data.calculationBase,
        warningThresholdPercentage: percentToFraction(data.warningThreshold),
        initialAmountInUse: Number(data.consumedAmount || 0),
        initialConsumptionReference: Number(data.consumedAmount) > 0 ? data.consumptionReference.trim() : null,
      });

      showSuccess("Pagador, cupo de crédito y tarifario registrados correctamente.");
      handleCloseModal();
      reload();
    } catch (error) {
      showError(apiErrorMessage(error, "No se pudo registrar el pagador."));
    }
  };

  const onSubmit = (data) => (isEditing ? submitUpdate(data) : submitCreate(data));

  const handleCreate = () => {
    setEditingPayer(null);
    reset(DEFAULT_FORM_VALUES);
    setIsModalOpen(true);
  };

  const handleEdit = (payer) => {
    if (!payer.creditFacilityId) {
      Swal.fire({
        icon: "info",
        title: "Sin cupo de crédito",
        text: "Este pagador no tiene un cupo de crédito asignado, por lo que no tiene condiciones que editar.",
        confirmButtonColor: "#dc2626",
      });
      return;
    }

    setEditingPayer(payer);
    reset(toPayerFormValues(payer));
    setIsModalOpen(true);
  };

  return (
    <div className="flex flex-col w-full lg:h-[calc(100dvh-5.5rem)] min-h-0 pb-3">
      <div className="shrink-0">
        <HeaderCard
          title={"Gestion de pagadores"}
          description={"Administre los atributos de los pagadores del sistema"}
          icon={<FaBuilding />}
        />
      </div>

      <div className="shrink-0 border-b border-gray-200 mb-4"></div>

      <div className="shrink-0 flex flex-col md:flex-row items-center justify-between gap-4">
        <SearchInput
          value={searchTerm}
          onChange={setSearchTerm}
          placeholder="Buscar pagador por nombre o NIT"
          ariaLabel="Buscar pagador"
          className="w-full md:w-1/3 md:max-w-md shadow-sm rounded-lg transition-shadow hover:shadow-md"
        />
        <button
          onClick={handleCreate}
          className="
                    font-montserrat
                    flex items-center           
                    bg-red-600 hover:bg-red-700
                    text-white 
                    py-2 px-6                
                    rounded-md
                    font-medium
                    shadow-lg
                    hover:cursor-pointer 
                    transition duration-300 ease-in-out
                    focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-opacity-50 
                    "
        >
          <FaPlus className="mr-2" />
          <span className="text-xs font-montserrat">Registrar pagador</span>
        </button>
      </div>

      <div className="mt-4 flex-1 min-h-0 flex flex-col">
        <Table
          columns={columns}
          data={payers}
          isLoading={isLoading}
          onEdit={handleEdit}
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={setCurrentPage}
          fillHeight
        />
      </div>

      <Modal
        isOpen={isModalOpen}
        onClose={handleCloseModal}
        title={isEditing ? "Modificar información del pagador" : "Registrar nuevo pagador"}
        footer={
          <>
            <button
              onClick={handleCloseModal}
              className="
                              py-2 px-4 rounded-lg text-xs font-medium 
                              bg-white text-gray-700 border border-gray-300 
                              hover:bg-gray-100
                              hover:cursor-pointer
                            "
            >
              Cancelar
            </button>
            <button
              onClick={handleSubmit(onSubmit)}
              disabled={isSubmitting}
              className="py-2 px-4 rounded-lg text-xs font-medium bg-red-600 text-white hover:bg-red-700 shadow-md hover:cursor-pointer disabled:opacity-60 disabled:cursor-not-allowed"
            >
              {isEditing ? "Guardar cambios" : "Guardar"}
            </button>
          </>
        }
      >
        <PayerFormFields form={form} isEditing={isEditing} canEditAccount={canEditAccount} onSubmit={onSubmit} />
      </Modal>
    </div>
  );
};

export default PayerManagementAdmin;
