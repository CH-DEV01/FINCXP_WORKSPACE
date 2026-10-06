import React, { useState, useEffect, useCallback } from "react";
import { FaCog } from "react-icons/fa";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import Table from "../../components/Table";
import HeaderCard from "../../components/HeaderCard";
import Modal from "../../components/Modal";
import SearchInput from "../../components/SearchInput";
import FieldLabel from "../../components/form/FieldLabel";
import FieldError from "../../components/form/FieldError";
import inputClass from "../../components/form/inputClass";

import { parameterService } from "../../services/admin/parameterService";
import { paramSchema } from "../../schemas/paramSchema";
import useClientPagination from "../../hooks/useClientPagination";
import { apiErrorMessage, showError, showLoading, showSuccess } from "../../utils/errors";

const ITEMS_PER_PAGE = 10;

const columns = [
  { header: "Clave", accessor: "key" },
  { header: "Valor", accessor: "value" },
];

const matchesParameter = (item, term) =>
  (item.key?.toLowerCase() || "").includes(term) ||
  (item.value?.toLowerCase() || "").includes(term) ||
  (item.description?.toLowerCase() || "").includes(term);

const ParamsManagement = () => {
  const [parameters, setParameters] = useState([]);
  const [editingParam, setEditingParam] = useState(null);

  const { searchTerm, setSearchTerm, currentPage, setCurrentPage, totalPages, pageItems } = useClientPagination(
    parameters,
    { pageSize: ITEMS_PER_PAGE, matches: matchesParameter },
  );

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(paramSchema),
    defaultValues: {
      key: "",
      value: "",
    },
  });

  const fetchParams = useCallback(async () => {
    try {
      const page = await parameterService.getParameters();
      setParameters(page?.content ?? []);
    } catch (error) {
      console.error("Error fetching params:", error);
      setParameters([]);
    }
  }, []);

  useEffect(() => {
    fetchParams();
  }, [fetchParams]);

  const handleCloseModal = () => {
    setEditingParam(null);
    reset();
  };

  const onSubmit = async (data) => {
    showLoading();

    try {
      await parameterService.updateParameter(editingParam.id, {
        key: data.key,
        value: data.value,
      });

      showSuccess("Parámetro actualizado correctamente.");
      fetchParams();
      handleCloseModal();
    } catch (error) {
      showError(apiErrorMessage(error, "No se pudo guardar la información. Intenta de nuevo."));
    }
  };

  const handleEdit = (param) => {
    setEditingParam(param);
    reset({
      key: param.key || "",
      value: param.value || "",
    });
  };

  return (
    <div className="flex flex-col w-full lg:h-[calc(100dvh-5.5rem)] min-h-0 pb-3">
      <div className="shrink-0">
        <HeaderCard
          title={"Gestion de parametros"}
          description={"Administre parametros globales del sistema"}
          icon={<FaCog />}
        />
      </div>

      <div className="shrink-0 border-b border-gray-200 mb-4"></div>

      <div className="shrink-0 flex flex-col md:flex-row items-center justify-between gap-4">
        <SearchInput
          value={searchTerm}
          onChange={setSearchTerm}
          placeholder="Buscar en el sistema por clave o valor"
          className="w-full md:w-1/3 shadow-sm rounded-lg"
        />
      </div>

      <div className="mt-4 flex-1 min-h-0 flex flex-col">
        <Table
          columns={columns}
          data={pageItems}
          onEdit={handleEdit}
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={setCurrentPage}
          fillHeight
        />
      </div>

      <Modal
        isOpen={Boolean(editingParam)}
        onClose={handleCloseModal}
        title="Modificar Parametro"
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
              className="py-2 px-4 rounded-lg text-xs font-medium bg-red-600 text-white hover:bg-red-700 shadow-md hover:cursor-pointer"
            >
              Guardar cambios
            </button>
          </>
        }
      >
        <form className="space-y-4">
          <div>
            <FieldLabel htmlFor="param-key" required>
              Clave (Key)
            </FieldLabel>
            <input id="param-key" type="text" {...register("key")} readOnly className={inputClass(errors.key, true)} />
            <FieldError error={errors.key} />
          </div>

          <div>
            <FieldLabel htmlFor="param-value" required>
              Valor (Value)
            </FieldLabel>
            <input id="param-value" type="text" {...register("value")} className={inputClass(errors.value)} />
            <FieldError error={errors.value} />
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default ParamsManagement;
