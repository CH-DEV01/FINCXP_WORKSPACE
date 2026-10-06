import { useEffect, useState } from "react";

/** Devuelve `value` solo cuando deja de cambiar durante `delay` ms (búsquedas que consultan al servidor). */
const useDebouncedValue = (value, delay = 300) => {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);

  return debounced;
};

export default useDebouncedValue;
