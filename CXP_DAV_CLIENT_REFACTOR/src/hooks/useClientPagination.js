import { useCallback, useMemo, useState } from "react";

/**
 * Búsqueda y paginación en memoria. `matches(item, term)` recibe el término ya recortado y en
 * minúsculas; debe ser estable (definido fuera del componente o memoizado).
 */
const useClientPagination = (items, { pageSize, matches }) => {
  const [searchTerm, setSearchTermState] = useState("");
  const [currentPage, setCurrentPage] = useState(1);

  const filteredItems = useMemo(() => {
    const list = Array.isArray(items) ? items : [];
    const term = searchTerm.trim().toLowerCase();
    return term ? list.filter((item) => matches(item, term)) : list;
  }, [items, searchTerm, matches]);

  const totalPages = Math.max(1, Math.ceil(filteredItems.length / pageSize));
  const page = Math.min(currentPage, totalPages);

  const pageItems = useMemo(
    () => filteredItems.slice((page - 1) * pageSize, page * pageSize),
    [filteredItems, page, pageSize],
  );

  const setSearchTerm = useCallback((value) => {
    setSearchTermState(value);
    setCurrentPage(1);
  }, []);

  return {
    searchTerm,
    setSearchTerm,
    currentPage: page,
    setCurrentPage,
    totalPages,
    pageItems,
    filteredItems,
  };
};

export default useClientPagination;
