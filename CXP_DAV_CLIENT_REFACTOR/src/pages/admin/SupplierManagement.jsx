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
import FieldLabel from "../../components/form/FieldLabel";
import FieldError from "../../components/form/FieldError";
import inputClass from "../../components/form/inputClass";

import { entityService } from "../../services/admin/entityService";
import { supplierSchema, supplierUpdateSchema } from "../../schemas/supplierSchema";
import useServerPagination from "../../hooks/useServerPagination";
import { STATUS_OPTIONS } from "../../constants/status";
import { apiErrorMessage, showError, showLoading, showSuccess } from "../../utils/errors";
import { formatNIT } from "./payer-management/payerForm";

const createResolver = zodResolver(supplierSchema);
const updateResolver = zodResolver(supplierUpdateSchema);

const ITEMS_PER_PAGE = 10;

const DEFAULT_FORM_VALUES = {
  name: "",
  nit: "",
  niu: "",
  accountNumber: "",
  status: "ACTIVE",
};

const columns = [
  { header: "NIT", accessor: "nit" },
  {
    header: "NIU",
    accessor: "niu",
    render: (value) =>
      value ? <span className="font-mono">{value}</span> : <span className="text-gray-400">Sin registrar</span>,
  },
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

const logLoadError = (error) => console.error("Error al cargar proveedores", error);

const SupplierManagement = () => {
  const [isModalOpen, setIsModalOpen] = useState(false);
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

  const isEditing = editingSupplier !== null;
  const isMissingNiu = isEditing && !editingSupplier.niu;
  const resolverRef = useRef(createResolver);
  resolverRef.current = isEditing ? updateResolver : createResolver;

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: (values, context, options) => resolverRef.current(values, context, options),
    defaultValues: DEFAULT_FORM_VALUES,
  });

  const handleCreate = () => {
    setEditingSupplier(null);
    reset(DEFAULT_FORM_VALUES);
    setIsModalOpen(true);
  };

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
      name: supplier.name || "",
      nit: supplier.nit || "",
      niu: supplier.niu || "",
      accountNumber: supplier.accountNumber || "",
      status: supplier.status || DEFAULT_FORM_VALUES.status,
    });
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setEditingSupplier(null);
    reset(DEFAULT_FORM_VALUES);
  };

  const submitUpdate = async (data) => {
    showLoading();

    try {
      await entityService.updateSupplier(editingSupplier.id, {
        niu: data.niu.trim(),
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

  const submitCreate = async (data) => {
    showLoading("Registrando el proveedor, por favor espere.");

    try {
      await entityService.registerSupplier({
        name: data.name.trim(),
        nit: data.nit,
        niu: data.niu.trim(),
        accountNumber: data.accountNumber.trim(),
      });

      showSuccess("Proveedor registrado correctamente.");
      handleCloseModal();
      reload();
    } catch (error) {
      showError(apiErrorMessage(error, "No se pudo registrar el proveedor."));
    }
  };

  const onSubmit = (data) => (isEditing ? submitUpdate(data) : submitCreate(data));

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
          placeholder="Buscar proveedor por NIT, NIU o nombre"
          ariaLabel="Buscar proveedor"
          className="w-full md:w-1/3 md:max-w-md shadow-sm rounded-lg transition-shadow hover:shadow-md"
        />
        <button
          onClick={handleCreate}
          className="font-montserrat flex items-center bg-red-600 hover:bg-red-700 text-white py-2 px-6 rounded-md font-medium shadow-lg hover:cursor-pointer transition duration-300 ease-in-out focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-opacity-50"
        >
          <FaPlus className="mr-2" />
          <span className="text-xs font-montserrat">Registrar proveedor</span>
        </button>
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
        isOpen={isModalOpen}
        onClose={handleCloseModal}
        title={isEditing ? "Modificar información del proveedor" : "Registrar nuevo proveedor"}
        footer={
          <>
            <button
              onClick={handleCloseModal}
              className="py-2 px-4 rounded-lg text-xs font-medium bg-white text-gray-700 border border-gray-300 hover:bg-gray-100 hover:cursor-pointer"
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
        <form className="grid grid-cols-1 md:grid-cols-2 gap-4" onSubmit={handleSubmit(onSubmit)}>
          {isEditing && (
            <p className="md:col-span-2 rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 text-[11px] text-gray-600">
              Solo se pueden modificar el NIU, la cuenta bancaria y el estado. El resto de datos del proveedor se
              muestran como referencia.
            </p>
          )}

          {isMissingNiu && (
            <p className="md:col-span-2 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-[11px] text-amber-800">
              Este proveedor se creó desde la carga de documentos y aún no tiene NIU. Regístrelo para poder guardar los
              cambios.
            </p>
          )}

          <div>
            <FieldLabel htmlFor="supplier-nit">NIT</FieldLabel>
            <input
              id="supplier-nit"
              type="text"
              maxLength={17}
              readOnly={isEditing}
              {...register("nit", {
                onChange: (e) => {
                  e.target.value = formatNIT(e.target.value);
                },
              })}
              className={inputClass(errors.nit, isEditing)}
              placeholder="0000-000000-000-0"
            />
            <FieldError error={errors.nit} />
          </div>

          <div>
            <FieldLabel htmlFor="supplier-name">Nombre</FieldLabel>
            <input
              id="supplier-name"
              type="text"
              maxLength={255}
              readOnly={isEditing}
              {...register("name")}
              className={inputClass(errors.name, isEditing)}
              placeholder="Nombre del proveedor"
            />
            <FieldError error={errors.name} />
          </div>

          <div>
            <FieldLabel htmlFor="supplier-niu">NIU</FieldLabel>
            <input
              id="supplier-niu"
              type="text"
              inputMode="numeric"
              maxLength={25}
              {...register("niu", {
                onChange: (e) => {
                  e.target.value = e.target.value.replace(/\D/g, "");
                },
              })}
              className={inputClass(errors.niu)}
              placeholder="Ej. 1234567890"
            />
            <FieldError error={errors.niu} />
          </div>

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

          {isEditing && (
            <div className="md:col-span-2">
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
          )}
        </form>
      </Modal>
    </div>
  );
};

export default SupplierManagement;
