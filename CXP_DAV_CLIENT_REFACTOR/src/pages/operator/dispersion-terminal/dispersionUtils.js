export const dispersionRequestKey = (request) => request.batchId || `pending|${request.dueDate}`;

export const safeFileSegment = (value, fallback) => (value || fallback).replace(/[^a-zA-Z0-9]/g, "_");
