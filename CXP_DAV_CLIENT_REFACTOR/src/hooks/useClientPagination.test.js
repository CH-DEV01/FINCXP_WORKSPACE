import { describe, expect, it } from "vitest";
import { act, renderHook } from "@testing-library/react";
import useClientPagination from "./useClientPagination.js";

const items = Array.from({ length: 25 }, (_, index) => ({
  id: index + 1,
  name: index % 2 === 0 ? `Alfa ${index + 1}` : `Beta ${index + 1}`,
}));

const matches = (item, term) => item.name.toLowerCase().includes(term);

const setup = (list = items, pageSize = 10) =>
  renderHook(({ data }) => useClientPagination(data, { pageSize, matches }), {
    initialProps: { data: list },
  });

describe("useClientPagination", () => {
  it("devuelve la primera página y el total de páginas", () => {
    const { result } = setup();
    expect(result.current.currentPage).toBe(1);
    expect(result.current.totalPages).toBe(3);
    expect(result.current.pageItems.map((item) => item.id)).toEqual([1, 2, 3, 4, 5, 6, 7, 8, 9, 10]);
    expect(result.current.filteredItems).toHaveLength(25);
  });

  it("cambia de página", () => {
    const { result } = setup();
    act(() => result.current.setCurrentPage(3));
    expect(result.current.pageItems.map((item) => item.id)).toEqual([21, 22, 23, 24, 25]);
  });

  it("filtra con el término recortado y en minúsculas y vuelve a la página 1", () => {
    const { result } = setup();
    act(() => result.current.setCurrentPage(2));
    act(() => result.current.setSearchTerm("  BETA "));
    expect(result.current.currentPage).toBe(1);
    expect(result.current.filteredItems).toHaveLength(12);
    expect(result.current.totalPages).toBe(2);
    expect(result.current.pageItems.every((item) => item.name.startsWith("Beta"))).toBe(true);
  });

  it("ajusta la página si la lista se reduce", () => {
    const { result, rerender } = setup();
    act(() => result.current.setCurrentPage(3));
    rerender({ data: items.slice(0, 12) });
    expect(result.current.currentPage).toBe(2);
    expect(result.current.pageItems.map((item) => item.id)).toEqual([11, 12]);
  });

  it("tolera listas vacías o no válidas", () => {
    const { result, rerender } = setup(null);
    expect(result.current.pageItems).toEqual([]);
    expect(result.current.totalPages).toBe(1);
    rerender({ data: [] });
    expect(result.current.currentPage).toBe(1);
    expect(result.current.filteredItems).toEqual([]);
  });
});
