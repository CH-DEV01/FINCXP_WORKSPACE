import React, { useState, useEffect, useCallback } from "react";
import { FaCalendarAlt, FaPlus } from "react-icons/fa";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import Swal from "sweetalert2";

import Table from "../../components/Table";
import HeaderCard from "../../components/HeaderCard";
import Modal from "../../components/Modal";
import SearchInput from "../../components/SearchInput";
import FieldError from "../../components/form/FieldError";

import { holidayService } from "../../services/admin/holidayService";
import { holidaySchema } from "../../schemas/holidaySchema";
import useClientPagination from "../../hooks/useClientPagination";
import { formatDate } from "../../utils/format";
import { apiErrorMessage, showError, showLoading, showSuccess } from "../../utils/errors";

const ITEMS_PER_PAGE = 10;

const formatHolidayDate = (value) => formatDate(value, "");

const columns = [
  {
    header: "Fecha del dia feriado",
    accessor: "holidayDate",
    render: (val) => <span className="font-semibold text-slate-700">{formatHolidayDate(val)}</span>,
  },
  { header: "Descripcion", accessor: "description" },
];

const matchesHoliday = (item, term) =>
  (item.description?.toLowerCase() || "").includes(term) || (item.holidayDate?.toLowerCase() || "").includes(term);

const HolidaysManagement = () => {
  const [holidays, setHolidays] = useState([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingHoliday, setEditingHoliday] = useState(null);

  const { searchTerm, setSearchTerm, currentPage, setCurrentPage, totalPages, pageItems } = useClientPagination(
    holidays,
    { pageSize: ITEMS_PER_PAGE, matches: matchesHoliday },
  );

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(holidaySchema),
    defaultValues: {
      holidayDate: "",
      description: "",
    },
  });

  const fetchHolidays = useCallback(async () => {
    try {
      setHolidays(await holidayService.getHolidays());
    } catch (error) {
      console.error("Error fetching holidays:", error);
      setHolidays([]);
      showError(apiErrorMessage(error, "No se pudieron cargar los días feriados."));
    }
  }, []);

  useEffect(() => {
    fetchHolidays();
  }, [fetchHolidays]);

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setEditingHoliday(null);
    reset();
  };

  const onSubmit = async (data) => {
    showLoading();

    try {
      if (editingHoliday) {
        await holidayService.updateHoliday(editingHoliday.id, data);
      } else {
        await holidayService.createHoliday(data);
      }

      showSuccess(editingHoliday ? "Día feriado actualizado correctamente." : "Día feriado registrado correctamente.");
      fetchHolidays();
      handleCloseModal();
    } catch (error) {
      showError(apiErrorMessage(error, "No se pudo guardar la información. Intenta de nuevo."));
    }
  };

  const handleCreate = () => {
    setEditingHoliday(null);
    reset({ holidayDate: "", description: "" });
    setIsModalOpen(true);
  };

  const handleEdit = (holiday) => {
    setEditingHoliday(holiday);
    reset({
      holidayDate: holiday.holidayDate || "",
      description: holiday.description || "",
    });
    setIsModalOpen(true);
  };

  const handleDelete = async (holiday) => {
    const result = await Swal.fire({
      title: "¿Estás seguro?",
      text: `Eliminarás el día feriado del ${formatHolidayDate(holiday.holidayDate)} (${holiday.description}). Esta operación no se puede deshacer.`,
      icon: "warning",
      showCancelButton: true,
      confirmButtonColor: "#dc2626",
      cancelButtonColor: "#4b5563",
      confirmButtonText: "Sí, eliminar",
      cancelButtonText: "Cancelar",
      reverseButtons: true,
    });

    if (!result.isConfirmed) return;

    showLoading("", "Eliminando...");

    try {
      await holidayService.deleteHoliday(holiday.id);
      await Swal.fire({
        icon: "success",
        title: "¡Eliminado!",
        text: "El día feriado ha sido removido del sistema.",
        timer: 1500,
        showConfirmButton: false,
      });
      fetchHolidays();
    } catch (error) {
      showError(apiErrorMessage(error, "No se pudo eliminar el día feriado."));
    }
  };

  return (
    <div className="flex flex-col w-full lg:h-[calc(100dvh-5.5rem)] min-h-0 pb-3">
      <div className="shrink-0">
        <HeaderCard
          title={"Calendario de dias feriados"}
          description={"Administre los días feriados no laborables para el banco"}
          icon={<FaCalendarAlt />}
        />
      </div>

      <div className="shrink-0 border-b border-gray-200 mb-4"></div>

      <div className="shrink-0 flex flex-col md:flex-row items-center justify-between gap-4">
        <SearchInput
          value={searchTerm}
          onChange={setSearchTerm}
          placeholder="Buscar asueto por descripción o fecha..."
          className="w-full md:w-1/3 shadow-sm rounded-lg"
        />

        <button
          onClick={handleCreate}
          className="
                        font-montserrat
                        flex items-center          
                        bg-red-600 hover:bg-red-700
                        text-white 
                        py-2.5 px-6                
                        rounded-lg
                        font-medium
                        shadow-md
                        hover:cursor-pointer 
                        transition duration-300 ease-in-out
                        focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-opacity-50 
                    "
        >
          <FaPlus className="mr-2" />
          <span className="text-xs font-montserrat">Registrar día feriado</span>
        </button>
      </div>

      <div className="mt-4 flex-1 min-h-0 flex flex-col">
        <Table
          columns={columns}
          data={pageItems}
          onEdit={handleEdit}
          onDelete={handleDelete}
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={setCurrentPage}
          fillHeight
        />
      </div>

      <Modal
        isOpen={isModalOpen}
        onClose={handleCloseModal}
        title={editingHoliday ? "Modificar día feriado" : "Registrar nuevo día feriado"}
        footer={
          <>
            <button
              onClick={handleCloseModal}
              className="
                              py-2 px-4 rounded-lg text-xs font-medium 
                              bg-white text-gray-700 border border-gray-300 
                              hover:bg-gray-100 hover:cursor-pointer transition-colors
                            "
            >
              Cancelar
            </button>
            <button
              onClick={handleSubmit(onSubmit)}
              className="py-2 px-4 rounded-lg text-xs font-medium bg-red-600 text-white hover:bg-red-700 shadow-md hover:cursor-pointer transition-colors"
            >
              {editingHoliday ? "Guardar cambios" : "Registrar"}
            </button>
          </>
        }
      >
        <form className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-gray-600 mb-1">
              Fecha del día feriado <span className="text-red-500">*</span>
            </label>
            <input
              type="date"
              {...register("holidayDate")}
              className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 text-sm font-montserrat ${errors.holidayDate ? "border-red-500 focus:ring-red-300" : "border-gray-300 focus:ring-red-300"}`}
            />
            <FieldError error={errors.holidayDate} />
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-600 mb-1">
              Descripción / Motivo <span className="text-red-500">*</span>
            </label>
            <textarea
              {...register("description")}
              placeholder="Ej. Fiestas Agostinas"
              rows={3}
              className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 text-sm font-montserrat resize-none ${errors.description ? "border-red-500 focus:ring-red-300" : "border-gray-300 focus:ring-red-300"}`}
            />
            <FieldError error={errors.description} />
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default HolidaysManagement;
