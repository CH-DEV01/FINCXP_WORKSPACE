import { describe, expect, it, vi } from "vitest";
import { act, renderHook, waitFor } from "@testing-library/react";
import useServerPagination from "./useServerPagination.js";

const pageOf = (content, totalPages = 3, totalElements = 25) => ({
  content,
  totalPages,
  totalElements,
  number: 0,
  size: 10,
});

describe("useServerPagination", () => {
  it("consulta la primera página (base 0) y expone el resultado", async () => {
    const fetchPage = vi.fn(() => Promise.resolve(pageOf([{ id: 1 }, { id: 2 }])));
    const { result } = renderHook(() => useServerPagination(fetchPage));

    expect(result.current.isLoading).toBe(true);
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(fetchPage).toHaveBeenCalledWith({ search: "", page: 0 });
    expect(result.current.items).toEqual([{ id: 1 }, { id: 2 }]);
    expect(result.current.totalPages).toBe(3);
    expect(result.current.totalElements).toBe(25);
    expect(result.current.currentPage).toBe(1);
  });

  it("pide la página seleccionada", async () => {
    const fetchPage = vi.fn(({ page }) => Promise.resolve(pageOf([{ id: page }])));
    const { result } = renderHook(() => useServerPagination(fetchPage));
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    act(() => result.current.setCurrentPage(3));
    await waitFor(() => expect(result.current.items).toEqual([{ id: 2 }]));
    expect(fetchPage).toHaveBeenLastCalledWith({ search: "", page: 2 });
  });

  it("aplica la búsqueda con retardo, recortada, y vuelve a la página 1", async () => {
    const fetchPage = vi.fn(() => Promise.resolve(pageOf([])));
    const { result } = renderHook(() => useServerPagination(fetchPage));
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    act(() => result.current.setCurrentPage(2));
    await waitFor(() => expect(fetchPage).toHaveBeenLastCalledWith({ search: "", page: 1 }));

    act(() => result.current.setSearchTerm("  acme "));
    expect(fetchPage).not.toHaveBeenCalledWith(expect.objectContaining({ search: "acme" }));

    await waitFor(() => expect(fetchPage).toHaveBeenLastCalledWith({ search: "acme", page: 0 }));
    expect(result.current.currentPage).toBe(1);
  });

  it("vuelve a consultar con reload", async () => {
    const fetchPage = vi.fn(() => Promise.resolve(pageOf([])));
    const { result } = renderHook(() => useServerPagination(fetchPage));
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    const calls = fetchPage.mock.calls.length;

    act(() => result.current.reload());
    await waitFor(() => expect(fetchPage).toHaveBeenCalledTimes(calls + 1));
  });

  it("deja la página vacía y avisa por onError si falla", async () => {
    const error = new Error("boom");
    const onError = vi.fn();
    const fetchPage = vi.fn(() => Promise.reject(error));
    const { result } = renderHook(() => useServerPagination(fetchPage, { onError }));

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(onError).toHaveBeenCalledWith(error);
    expect(result.current.items).toEqual([]);
    expect(result.current.totalPages).toBe(1);
    expect(result.current.totalElements).toBe(0);
  });

  it("tolera respuestas nulas o sin content", async () => {
    const fetchPage = vi.fn(() => Promise.resolve(null));
    const { result } = renderHook(() => useServerPagination(fetchPage));
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.items).toEqual([]);
    expect(result.current.totalPages).toBe(1);
  });

  it("no consulta nada si fetchPage es nulo", () => {
    const { result } = renderHook(() => useServerPagination(null));
    expect(result.current.isLoading).toBe(false);
    expect(result.current.items).toEqual([]);
  });

  it("ignora la respuesta de una petición reemplazada", async () => {
    let resolveFirst;
    const first = vi.fn(
      () =>
        new Promise((resolve) => {
          resolveFirst = resolve;
        }),
    );
    const second = vi.fn(() => Promise.resolve(pageOf([{ id: "nuevo" }])));
    const { result, rerender } = renderHook(({ fetchPage }) => useServerPagination(fetchPage), {
      initialProps: { fetchPage: first },
    });

    rerender({ fetchPage: second });
    await waitFor(() => expect(result.current.items).toEqual([{ id: "nuevo" }]));

    await act(async () => {
      resolveFirst(pageOf([{ id: "viejo" }]));
    });
    expect(result.current.items).toEqual([{ id: "nuevo" }]);
  });
});
