import { useCallback, useEffect, useRef, useState } from "react";
import useDebouncedValue from "./useDebouncedValue.js";

const EMPTY_PAGE = { content: [], totalPages: 1, totalElements: 0 };

/**
 * Paginación contra un endpoint que devuelve una Page de Spring.
 *
 * `fetchPage({ search, page })` recibe la página en base 0 y debe memoizarse con useCallback
 * sobre los filtros del llamador; al cambiar un filtro, el llamador vuelve a la página 1 en el
 * mismo manejador para que salga una sola petición. Con `fetchPage` nulo no se consulta nada.
 */
const useServerPagination = (fetchPage, { onError } = {}) => {
  const [searchTerm, setSearchTerm] = useState("");
  const debouncedSearch = useDebouncedValue(searchTerm.trim());
  const [appliedSearch, setAppliedSearch] = useState(debouncedSearch);
  const [currentPage, setCurrentPage] = useState(1);
  const [page, setPage] = useState(EMPTY_PAGE);
  const [isLoading, setIsLoading] = useState(Boolean(fetchPage));
  const [reloadKey, setReloadKey] = useState(0);

  const onErrorRef = useRef(onError);
  useEffect(() => {
    onErrorRef.current = onError;
  });

  if (appliedSearch !== debouncedSearch) {
    setAppliedSearch(debouncedSearch);
    setCurrentPage(1);
  }

  useEffect(() => {
    if (!fetchPage) return undefined;

    let cancelled = false;
    setIsLoading(true);

    fetchPage({ search: appliedSearch, page: currentPage - 1 })
      .then((result) => {
        if (!cancelled) setPage(result ?? EMPTY_PAGE);
      })
      .catch((error) => {
        if (cancelled) return;
        setPage(EMPTY_PAGE);
        onErrorRef.current?.(error);
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [fetchPage, appliedSearch, currentPage, reloadKey]);

  const reload = useCallback(() => setReloadKey((key) => key + 1), []);

  return {
    items: Array.isArray(page.content) ? page.content : [],
    page,
    totalPages: Math.max(1, page.totalPages || 1),
    totalElements: page.totalElements || 0,
    isLoading,
    searchTerm,
    setSearchTerm,
    currentPage,
    setCurrentPage,
    reload,
  };
};

export default useServerPagination;
