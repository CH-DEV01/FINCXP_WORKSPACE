const HTML_ENTITIES = {
  "&": "&amp;",
  "<": "&lt;",
  ">": "&gt;",
  '"': "&quot;",
  "'": "&#39;",
};

// SweetAlert2 renderiza `title`, `html` y el segundo argumento posicional como HTML.
const escapeHtml = (value) => String(value ?? "").replace(/[&<>"']/g, (char) => HTML_ENTITIES[char]);

export default escapeHtml;
