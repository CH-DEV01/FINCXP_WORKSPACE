import React, { useState, useCallback } from "react";
import { FaBuilding } from "react-icons/fa";
import Swal from "sweetalert2";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import Table from "../../components/Table";
import HeaderCard from "../../components/HeaderCard";
import Modal from "../../components/Modal";
import SearchInput from "../../components/SearchInput";
import StatusBadge from "../../components/StatusBadge";
import FieldLabel from "../../components/form/FieldLabel";
import FieldError from "../../components/form/FieldError";
import inputClass from "../../components/form/inputClass";

import { entityService } from "../../services/admin/entityService";
import { supplierUpdateSchema } from "../../schemas/supplierSchema";
import useServerPagination from "../../hooks/useServerPagination";
import { STATUS_OPTIONS } from "../../constants/status";
import { apiErrorMessage, showError, showLoading, showSuccess } from "../../utils/errors";

const ITEMS_PER_PAGE = 10;

const DEFAULT_FORM_VALUES = {
  accountNumber: "",
  status: "ACTIVE",
};

const columns = [
  { header: "NIT", accessor: "nit" },
  { header: "NOMBRE", accessor: "name" },
  {
    header: "CUENTA BANCARIA",
    accessor: "accountNumber",
    render: (value) =>
      value ? <span className="font-mono">{value}</span> : <span className="text-gray-400">Sin registrar</span>,
  },
  {
    header: "ESTADO",
    accessor: "status",
    render: (value) => <StatusBadge status={value === "ACTIVE" ? "ACTIVE" : "INACTIVE"} />,
  },
];

const ReadOnlyField = ({ id, label, value }) => (
  <div>
    <FieldLabel htmlFor={id}>{label}</FieldLabel>
    <input id={id} type="text" readOnly value={value || ""} className={inputClass(false, true)} />
  </div>
);

const logLoadError = (error) => console.error("Error al cargar proveedores", error);

const SupplierManagement = () => {
  const [editingSupplier, setEditingSupplier] = useState(null);

  const fetchSuppliers = useCallback(
    ({ search, page }) => entityService.getSupplierSummaries({ search, page, size: ITEMS_PER_PAGE }),
    [],
  );

  const {
    items: suppliers,
    totalPages,
    isLoading,
    searchTerm,
    setSearchTerm,
    currentPage,
    setCurrentPage,
    reload,
  } = useServerPagination(fetchSuppliers, { onError: logLoadError });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(supplierUpdateSchema),
    defaultValues: DEFAULT_FORM_VALUES,
  });

  const handleEdit = (supplier) => {
    if (!supplier.bankAccountId) {
      Swal.fire({
        icon: "info",
        title: "Sin cuenta bancaria",
        text: "Este proveedor no tiene una cuenta bancaria principal registrada, por lo que no se puede editar.",
        confirmButtonColor: "#dc2626",
      });
      return;
    }

    setEditingSupplier(supplier);
    reset({
      accountNumber: supplier.accountNumber || "",
      status: supplier.status || DEFAULT_FORM_VALUES.status,
    });
  };

  const handleCloseModal = () => {
    setEditingSupplier(null);
    reset(DEFAULT_FORM_VALUES);
  };

  const onSubmit = async (data) => {
    showLoading();

    try {
      await entityService.updateSupplier(editingSupplier.id, {
        accountNumber: data.accountNumber.trim(),
        status: data.status,
      });

      showSuccess("Proveedor actualizado correctamente.");
      handleCloseModal();
      reload();
    } catch (error) {
      showError(apiErrorMessage(error, "No se pudo actualizar el proveedor."));
    }
  };

  return (
    <div className="flex flex-col w-full lg:h-[calc(100dvh-5.5rem)] min-h-0 pb-3">
      <div className="shrink-0">
        <HeaderCard
          title={"Gestion de proveedores"}
          description={"Administre los atributos de los proveedores del sistema"}
          icon={<FaBuilding />}
        />
      </div>

      <div className="shrink-0 border-b border-gray-200 mb-4"></div>

      <div className="shrink-0 flex flex-col md:flex-row items-center justify-between gap-4">
        <SearchInput
          value={searchTerm}
          onChange={setSearchTerm}
          placeholder="Buscar proveedor por NIT o nombre"
          ariaLabel="Buscar proveedor"
          className="w-full md:w-1/3 md:max-w-md shadow-sm rounded-lg transition-shadow hover:shadow-md"
        />
      </div>

      <div className="mt-4 flex-1 min-h-0 flex flex-col">
        <Table
          columns={columns}
          data={suppliers}
          isLoading={isLoading}
          onEdit={handleEdit}
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={setCurrentPage}
          fillHeight
        />
      </div>

      <Modal
        isOpen={editingSupplier !== null}
        onClose={handleCloseModal}
        title="Modificar información del proveedor"
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
              Guardar cambios
            </button>
          </>
        }
      >
        {editingSupplier && (
          <form className="grid grid-cols-1 md:grid-cols-2 gap-4" onSubmit={handleSubmit(onSubmit)}>
            <p className="md:col-span-2 rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 text-[11px] text-gray-600">
              Solo se pueden modificar la cuenta bancaria y el estado. El resto de datos del proveedor se muestran como
              referencia.
            </p>

            <ReadOnlyField id="supplier-nit" label="NIT" value={editingSupplier.nit} />
            <ReadOnlyField id="supplier-name" label="Nombre" value={editingSupplier.name} />

            <div>
              <FieldLabel htmlFor="supplier-account-number">Cuenta bancaria</FieldLabel>
              <input
                id="supplier-account-number"
                type="text"
                inputMode="numeric"
                maxLength={50}
                {...register("accountNumber", {
                  onChange: (e) => {
                    e.target.value = e.target.value.replace(/\D/g, "");
                  },
                })}
                className={inputClass(errors.accountNumber)}
                placeholder="Ej. 000123456789"
              />
              <p className="text-gray-400 text-[10px] mt-1">Cuenta de Banco Davivienda.</p>
              <FieldError error={errors.accountNumber} />
            </div>

            <div>
              <FieldLabel htmlFor="supplier-status">Estado</FieldLabel>
              <select id="supplier-status" {...register("status")} className={inputClass(errors.status)}>
                {STATUS_OPTIONS.map(({ value, label }) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
              <FieldError error={errors.status} />
            </div>
          </form>
        )}
      </Modal>
    </div>
  );
};

export default SupplierManagement;
