import React, { useState, useEffect, useCallback, useMemo } from "react";
import { FaPlus, FaUsers } from "react-icons/fa";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import Table from "../../components/Table";
import HeaderCard from "../../components/HeaderCard";
import Modal from "../../components/Modal";
import SearchInput from "../../components/SearchInput";
import StatusBadge from "../../components/StatusBadge";
import FieldError from "../../components/form/FieldError";

import EntityPicker from "./user-management/EntityPicker";

import { userService } from "../../services/admin/userService";
import { roleService } from "../../services/admin/roleService";
import { userSchema } from "../../schemas/userSchema";
import useServerPagination from "../../hooks/useServerPagination";
import { USER_BADGE_CLASS, USER_STATUS } from "../../constants/status";
import { ROLES } from "../../constants/roles";
import { apiErrorMessage, showError, showLoading, showSuccess } from "../../utils/errors";

const columns = [
  { header: "DUI", accessor: "dui" },
  { header: "NOMBRE", accessor: "name" },
  { header: "EMAIL", accessor: "email" },
  { header: "ENTIDAD", accessor: "entityName" },
  { header: "ROL", accessor: "roleName" },
  {
    header: "ESTADO",
    accessor: "status",
    render: (status) => <StatusBadge status={status} statusMap={USER_STATUS} className={USER_BADGE_CLASS} showDot />,
  },
];

const ITEMS_PER_PAGE = 10;

const EMPTY_FORM = { name: "", email: "", dui: "", entityId: "", roleId: "", status: "ACTIVE" };

const formatDUIFormat = (value) => {
  if (!value) return "";
  const digits = value.replace(/\D/g, "");
  if (digits.length <= 8) return digits;
  return `${digits.slice(0, 8)}-${digits.slice(8, 9)}`;
};

/** UserDTOResponse expone el apellido como "LastName"; aquí se normaliza a lastName. */
const toUserRow = ({ LastName, ...user }) => ({
  ...user,
  lastName: LastName || "",
  name: `${user.firstName || ""} ${LastName || ""}`.trim(),
});

const inputClass = (hasError, extra = "") =>
  `w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 text-xs font-montserrat ${extra} ${
    hasError ? "border-red-500 focus:ring-red-300" : "border-gray-300 focus:ring-red-300"
  }`;

const logLoadError = (error) => console.error("Error al cargar usuarios de forma segura", error);

const UserManagement = () => {
  const [selectedEntity, setSelectedEntity] = useState(null);
  const [roles, setRoles] = useState([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingParam, setEditingParam] = useState(null);
  const fetchUsers = useCallback(
    ({ page, search }) => userService.getUsers({ page, size: ITEMS_PER_PAGE, search }),
    [],
  );

  const { items, totalPages, isLoading, searchTerm, setSearchTerm, currentPage, setCurrentPage, reload } =
    useServerPagination(fetchUsers, { onError: logLoadError });

  const visibleUsers = useMemo(() => items.map(toUserRow), [items]);

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(userSchema),
    defaultValues: EMPTY_FORM,
  });

  const watchDui = watch("dui", "");
  const watchStatus = watch("status", "ACTIVE");
  const watchRoleId = watch("roleId", "");

  useEffect(() => {
    roleService
      .getRoles()
      .then(setRoles)
      .catch((error) => console.error("Error al cargar roles", error));
  }, []);

  const selectEntity = (entity) => {
    setSelectedEntity(entity);
    setValue("entityId", entity ? entity.id.toString() : "", { shouldValidate: Boolean(entity) });
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setEditingParam(null);
    setSelectedEntity(null);
    reset();
  };

  const onSubmit = async (data) => {
    showLoading();

    try {
      // Si el nombre no cambió se conserva la separación original nombre/apellido.
      const nameUnchanged = editingParam && data.name.trim() === editingParam.name;
      const nameParts = data.name.trim().split(/\s+/);
      const firstName = nameUnchanged ? editingParam.firstName : nameParts[0] || "";
      const lastName = nameUnchanged ? editingParam.lastName : nameParts.slice(1).join(" ");

      const payload = {
        dui: data.dui.replace(/\D/g, ""),
        email: data.email,
        firstName,
        lastName,
        entityId: data.entityId,
        roleId: data.roleId,
        status: data.status,
      };

      if (editingParam) {
        await userService.updateUser(editingParam.id, payload);
      } else {
        await userService.createUser(payload);
      }

      showSuccess(editingParam ? "Usuario actualizado correctamente." : "Usuario registrado correctamente.");
      reload();
      handleCloseModal();
    } catch (error) {
      showError(apiErrorMessage(error, "No se pudo guardar la información. Intenta de nuevo."));
    }
  };

  const handleCreate = () => {
    setEditingParam(null);
    setSelectedEntity(null);
    reset(EMPTY_FORM);
    setIsModalOpen(true);
  };

  const handleEdit = (param) => {
    if (param.roleName === ROLES.SYSTEM_ADMIN) {
      showError("Los usuarios administradores del sistema solo se administran directamente en la base de datos.");
      return;
    }
    setEditingParam(param);
    setSelectedEntity(param.entityId ? { id: param.entityId, name: param.entityName } : null);
    reset({
      name: param.name || "",
      email: param.email || "",
      dui: (param.dui || "").replace(/\D/g, ""),
      entityId: param.entityId ? param.entityId.toString() : "",
      roleId: param.roleId ? param.roleId.toString() : "",
      status: param.status || "ACTIVE",
    });
    setIsModalOpen(true);
  };

  return (
    <div className="flex flex-col w-full lg:h-[calc(100dvh-5.5rem)] min-h-0 pb-3">
      <div className="shrink-0">
        <HeaderCard
          title={"Gestion de usuarios"}
          description={"Administre los usuarios registrados en el sistema"}
          icon={<FaUsers />}
        />
      </div>

      <div className="shrink-0 border-b border-gray-200 mb-4"></div>

      <div className="shrink-0 flex flex-col md:flex-row items-center justify-between gap-4">
        <SearchInput
          value={searchTerm}
          onChange={setSearchTerm}
          placeholder="Buscar por DUI, nombre o entidad"
          ariaLabel="Buscar usuarios"
          className="w-full md:w-1/3 md:max-w-md shadow-sm rounded-lg transition-shadow hover:shadow-md"
        />

        <button
          onClick={handleCreate}
          className="font-montserrat flex items-center bg-red-600 hover:bg-red-700 text-white py-2 px-6 rounded-md font-medium shadow-lg hover:cursor-pointer transition duration-300 ease-in-out focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-opacity-50"
        >
          <FaPlus className="mr-2" />
          <span className="text-xs font-montserrat">Registrar usuario</span>
        </button>
      </div>

      <div className="mt-4 flex-1 min-h-0 flex flex-col">
        <Table
          columns={columns}
          data={visibleUsers}
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
        title={editingParam ? "Modificar información del usuario" : "Registrar nuevo usuario"}
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
              {editingParam ? "Guardar cambios" : "Registrar"}
            </button>
          </>
        }
      >
        <form className="space-y-4">
          <div>
            <label className="block text-xs text-gray-600 mb-2">Nombre</label>
            <input type="text" {...register("name")} className={inputClass(errors.name)} />
            <FieldError error={errors.name} />
          </div>

          <div>
            <label className="block text-xs text-gray-600 mb-2">E-mail</label>
            <input type="text" {...register("email")} className={inputClass(errors.email)} />
            <FieldError error={errors.email} />
          </div>

          <div>
            <label className="block text-xs text-gray-600 mb-2">DUI</label>
            <input
              type="text"
              maxLength={10}
              value={formatDUIFormat(watchDui)}
              onChange={(e) => {
                const cleanValue = e.target.value.replace(/\D/g, "");
                setValue("dui", cleanValue.slice(0, 9), {
                  shouldValidate: true,
                });
              }}
              className={inputClass(errors.dui)}
              placeholder="00000000-0"
            />
            <FieldError error={errors.dui} />
          </div>

          <div>
            <label className="block text-xs text-gray-600 mb-2">Rol</label>
            <select
              {...register("roleId", { onChange: () => selectEntity(null) })}
              className={inputClass(errors.roleId, "bg-white")}
            >
              <option value="">Seleccione un rol</option>
              {roles.map((role) => (
                <option key={role.id} value={role.id}>
                  {role.name} - ({role.description})
                </option>
              ))}
            </select>
            <FieldError error={errors.roleId} />
          </div>

          <div>
            <label className="block text-xs text-gray-600 mb-2">Entidad</label>
            <EntityPicker
              key={watchRoleId}
              roleId={watchRoleId}
              selected={selectedEntity}
              onSelect={selectEntity}
              hasError={Boolean(errors.entityId)}
            />
            <FieldError error={errors.entityId} />
          </div>

          {editingParam && (
            <div>
              <label className="block text-xs text-gray-600 mb-2">Estado</label>
              <select {...register("status")} className={inputClass(errors.status, "bg-white")}>
                {Object.entries(USER_STATUS).map(([value, { label }]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
              <FieldError error={errors.status} />
              {watchStatus === "INACTIVE" && (
                <p className="text-amber-600 text-[10px] mt-1">
                  Un usuario inactivo no podrá iniciar sesión y sus sesiones abiertas dejarán de ser válidas.
                </p>
              )}
            </div>
          )}
        </form>
      </Modal>
    </div>
  );
};

export default UserManagement;
