// Mirrors InventoryService.MAX_STOCK on the backend (which enforces it independently)
export const MAX_STOCK = 1_000_000;

export function medicineLabel(m) {
  return [m.name, m.strength, m.dosageForm ? `(${m.dosageForm})` : null].filter(Boolean).join(" ");
}

/** Readable message from an API error: {error}, field-validation map, or network failure. */
export function apiError(e, fallback) {
  if (!e.response) return "Network error: could not reach the server. Please try again.";
  const data = e.response.data;
  if (data?.error) return data.error;
  if (data && typeof data === "object") {
    const msgs = Object.values(data).filter((v) => typeof v === "string");
    if (msgs.length) return msgs.join(" ");
  }
  return fallback;
}
