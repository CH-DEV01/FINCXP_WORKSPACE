import React from "react";
import ReactMarkdown from "react-markdown";

// react-markdown pasa el nodo del AST en `node`; no debe llegar al DOM.
const styled = (Tag, className, extraProps = {}) => {
  const StyledElement = (props) => {
    const domProps = { ...props };
    delete domProps.node;
    return <Tag className={className} {...extraProps} {...domProps} />;
  };
  return StyledElement;
};

// react-markdown no interpreta HTML crudo, así que el contenido administrado no puede inyectar scripts.
const components = {
  h1: styled("h3", "text-base font-bold text-gray-900 mt-4"),
  h2: styled("h4", "text-sm font-bold text-gray-900 mt-4"),
  h3: styled("h5", "text-sm font-semibold text-gray-900 mt-3"),
  p: styled("p", "leading-relaxed"),
  strong: styled("strong", "font-semibold text-gray-900"),
  ul: styled("ul", "list-none pl-4 space-y-1"),
  ol: styled("ol", "list-decimal pl-6 space-y-1"),
  blockquote: styled("blockquote", "border-l-4 border-amber-400 bg-amber-50 px-3 py-2 text-amber-900 rounded-r"),
  a: styled("a", "text-red-700 underline", { target: "_blank", rel: "noopener noreferrer" }),
};

const TermsMarkdown = ({ content, className = "" }) => (
  <div className={`text-sm text-gray-700 space-y-3 ${className}`}>
    <ReactMarkdown components={components}>{content ?? ""}</ReactMarkdown>
  </div>
);

export default TermsMarkdown;
