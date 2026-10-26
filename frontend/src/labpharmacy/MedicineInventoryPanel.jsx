import { useState } from "react";
import api from "../common/api";
import { MAX_STOCK, apiError, medicineLabel } from "./inventoryUtils";

const UNIT_SUGGESTIONS = ["tablets", "capsules", "bottles", "vials", "ampoules", "tubes", "sachets", "inhalers", "strips", "ml"];
const FORM_SUGGESTIONS = ["Tablet", "Capsule", "Syrup", "Suspension", "Injection", "Inhaler", "Nebuliser Solution", "Ointment", "Cream", "Drops", "Sachet"];

const STATUS_BADGES = {
  IN_STOCK: { className: "badge badge-available", label: "In Stock" },
  LOW_STOCK: { className: "badge badge-urgent", label: "Low Stock" },
  OUT_OF_STOCK: { className: "badge badge-occupied", label: "Out of Stock" },
};

const EMPTY_FORM = { name: "", strength: "", dosageForm: "", unit: "tablets", initialQuantity: "", reorderThreshold: "" };

export function StockBadge({ status }) {
  const b = STATUS_BADGES[status] || { className: "badge", label: status };
  return <span className={b.className}>{b.label}</span>;
}

function parseWholeNumber(raw, min) {
  const s = String(raw).trim();
  if (!/^\d+$/.test(s)) return null;
  const n = Number(s);
  return n >= min && n <= MAX_STOCK ? n : null;
}

function fmt(dt) {
  if (!dt) return "—";
  return new Date(dt).toLocaleString("en-IN", { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit", hour12: true });
}

export default function MedicineInventoryPanel({ medicines, summary, error, loaded, onChanged }) {
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [showAdd, setShowAdd] = useState(false);
  const [form, setForm] = useState(EMPTY_FORM);
  const [formErr, setFormErr] = useState("");
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState("");
  const [restockQty, setRestockQty] = useState({});
  const [restockErr, setRestockErr] = useState({});
  const [restocking, setRestocking] = useState({});

  const term = search.trim().toLowerCase();
  const visible = medicines.filter(
    (m) =>
      (statusFilter === "ALL" || m.status === statusFilter) &&
      (!term || medicineLabel(m).toLowerCase().includes(term))
  );

  function setField(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  async function handleAdd(e) {
    e.preventDefault();
    setFormErr("");
    setNotice("");
    const initialQuantity = parseWholeNumber(form.initialQuantity, 0);
    const reorderThreshold = parseWholeNumber(form.reorderThreshold, 0);
    if (!form.name.trim()) return setFormErr("Medicine name is required.");
    if (!form.unit.trim()) return setFormErr("Unit of measurement is required.");
    if (initialQuantity === null) return setFormErr(`Initial quantity must be a whole number from 0 to ${MAX_STOCK.toLocaleString()}.`);
    if (reorderThreshold === null) return setFormErr(`Reorder threshold must be a whole number from 0 to ${MAX_STOCK.toLocaleString()}.`);

    setSaving(true);
    try {
      const res = await api.post("/inventory", {
        name: form.name.trim(),
        strength: form.strength.trim() || null,
        dosageForm: form.dosageForm.trim() || null,
        unit: form.unit.trim(),
        initialQuantity,
        reorderThreshold,
      });
      setNotice(`${medicineLabel(res.data)} added with ${res.data.quantity} ${res.data.unit}.`);
      setForm(EMPTY_FORM);
      setShowAdd(false);
      await onChanged();
    } catch (err) {
      setFormErr(apiError(err, "Failed to add medicine."));
    } finally {
      setSaving(false);
    }
  }

  async function handleRestock(m) {
    setNotice("");
    const quantity = parseWholeNumber(restockQty[m.id] ?? "", 1);
    if (quantity === null) {
      setRestockErr((prev) => ({ ...prev, [m.id]: "Enter a whole number of at least 1." }));
      return;
    }
    if (m.quantity + quantity > MAX_STOCK) {
      setRestockErr((prev) => ({ ...prev, [m.id]: `Stock cannot exceed ${MAX_STOCK.toLocaleString()}.` }));
      return;
    }
    setRestocking((prev) => ({ ...prev, [m.id]: true }));
    setRestockErr((prev) => ({ ...prev, [m.id]: "" }));
    try {
      const res = await api.post(`/inventory/${m.id}/restock`, { quantity });
      setRestockQty((prev) => ({ ...prev, [m.id]: "" }));
      setNotice(`${medicineLabel(res.data)} restocked by ${quantity}. Now ${res.data.quantity} ${res.data.unit}.`);
      await onChanged();
    } catch (err) {
      setRestockErr((prev) => ({ ...prev, [m.id]: apiError(err, "Failed to restock.") }));
    } finally {
      setRestocking((prev) => ({ ...prev, [m.id]: false }));
    }
  }

  return (
    <div className="panel" style={{ marginBottom: "2rem" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: "1rem", flexWrap: "wrap" }}>
        <div>
          <h3 className="panel-title">Medicine Inventory</h3>
          <p className="panel-subtitle">Stock on hand. Status is calculated from quantity and reorder threshold.</p>
        </div>
        <button type="button" className={showAdd ? "btn btn-outline" : "btn btn-primary"} onClick={() => { setShowAdd((v) => !v); setFormErr(""); }}>
          {showAdd ? "Cancel" : "+ Add Medicine"}
        </button>
      </div>

      {summary && (
        <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginBottom: "1rem" }}>
          <span className="demo-badge">{summary.totalMedicines} Medicines</span>
          <span className="badge badge-available">{summary.inStock} In Stock</span>
          <span className="badge badge-urgent">{summary.lowStock} Low Stock</span>
          <span className="badge badge-occupied">{summary.outOfStock} Out of Stock</span>
        </div>
      )}

      {error && <div className="alert alert-danger">{error}</div>}
      {notice && <div className="alert alert-success">{notice}</div>}

      {showAdd && (
        <form onSubmit={handleAdd} noValidate style={{ border: "1px solid var(--rule)", borderRadius: "var(--radius-sm)", padding: "1rem", marginBottom: "1.25rem", background: "var(--paper)" }}>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(170px, 1fr))", gap: "0 1rem" }}>
            <div className="form-group">
              <label className="form-label">Medicine Name *</label>
              <input className="form-control" maxLength={120} placeholder="e.g. Amoxicillin" value={form.name} onChange={(e) => setField("name", e.target.value)} />
            </div>
            <div className="form-group">
              <label className="form-label">Strength</label>
              <input className="form-control" maxLength={60} placeholder="e.g. 500mg" value={form.strength} onChange={(e) => setField("strength", e.target.value)} />
            </div>
            <div className="form-group">
              <label className="form-label">Dosage Form</label>
              <input className="form-control" maxLength={60} list="inv-form-options" placeholder="e.g. Capsule" value={form.dosageForm} onChange={(e) => setField("dosageForm", e.target.value)} />
            </div>
            <div className="form-group">
              <label className="form-label">Unit *</label>
              <input className="form-control" maxLength={30} list="inv-unit-options" value={form.unit} onChange={(e) => setField("unit", e.target.value)} />
            </div>
            <div className="form-group">
              <label className="form-label">Initial Quantity *</label>
              <input className="form-control" type="number" min="0" max={MAX_STOCK} step="1" value={form.initialQuantity} onChange={(e) => setField("initialQuantity", e.target.value)} />
            </div>
            <div className="form-group">
              <label className="form-label">Reorder Threshold *</label>
              <input className="form-control" type="number" min="0" max={MAX_STOCK} step="1" value={form.reorderThreshold} onChange={(e) => setField("reorderThreshold", e.target.value)} />
            </div>
          </div>
          <datalist id="inv-unit-options">{UNIT_SUGGESTIONS.map((u) => <option key={u} value={u} />)}</datalist>
          <datalist id="inv-form-options">{FORM_SUGGESTIONS.map((f) => <option key={f} value={f} />)}</datalist>
          <p style={{ fontSize: "0.75rem", color: "var(--ink-muted)", margin: "0 0 0.75rem 0" }}>
            Use the name and strength as doctors write them on prescriptions, so dispensing can match them.
          </p>
          {formErr && <div className="alert alert-danger">{formErr}</div>}
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? "Saving…" : "Save Medicine"}
          </button>
        </form>
      )}

      <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginBottom: "1rem" }}>
        <input className="form-control" style={{ flex: "1 1 220px" }} placeholder="Search medicine name or strength…" value={search} onChange={(e) => setSearch(e.target.value)} />
        <select className="form-control" style={{ flex: "0 1 180px" }} value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="ALL">All statuses</option>
          <option value="IN_STOCK">In Stock</option>
          <option value="LOW_STOCK">Low Stock</option>
          <option value="OUT_OF_STOCK">Out of Stock</option>
        </select>
      </div>

      {!loaded ? (
        <p style={{ color: "var(--text-muted)", margin: 0, fontSize: "0.85rem" }}>Loading inventory…</p>
      ) : medicines.length === 0 ? (
        <p style={{ color: "var(--text-muted)", margin: 0, fontSize: "0.85rem" }}>
          No medicines in inventory yet. Use “+ Add Medicine” to record stock.
        </p>
      ) : visible.length === 0 ? (
        <p style={{ color: "var(--text-muted)", margin: 0, fontSize: "0.85rem" }}>No medicines match the current search or filter.</p>
      ) : (
        <div className="data-table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Medicine</th>
                <th>Strength / Form</th>
                <th>Available</th>
                <th>Unit</th>
                <th>Reorder At</th>
                <th>Status</th>
                <th>Restock</th>
              </tr>
            </thead>
            <tbody>
              {visible.map((m) => (
                <tr key={m.id}>
                  <td style={{ fontWeight: 600, color: "var(--navy)" }}>{m.name}</td>
                  <td>{[m.strength, m.dosageForm].filter(Boolean).join(" · ") || "—"}</td>
                  <td style={{ fontWeight: 700 }}>{m.quantity.toLocaleString()}</td>
                  <td>{m.unit}</td>
                  <td>{m.reorderThreshold.toLocaleString()}</td>
                  <td><StockBadge status={m.status} /></td>
                  <td>
                    <div style={{ display: "flex", gap: "6px", alignItems: "center" }}>
                      <input
                        className="form-control"
                        type="number"
                        min="1"
                        step="1"
                        aria-label={`Restock quantity for ${medicineLabel(m)}`}
                        placeholder="Qty"
                        style={{ width: "80px" }}
                        value={restockQty[m.id] ?? ""}
                        onChange={(e) => setRestockQty((prev) => ({ ...prev, [m.id]: e.target.value }))}
                      />
                      <button type="button" className="btn btn-outline" disabled={restocking[m.id]} onClick={() => handleRestock(m)}>
                        {restocking[m.id] ? "…" : "Restock"}
                      </button>
                    </div>
                    {restockErr[m.id] && <div style={{ fontSize: "0.75rem", color: "var(--triage-1-text)", marginTop: "4px", whiteSpace: "normal" }}>{restockErr[m.id]}</div>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

export function StockMovementLog({ movements }) {
  return (
    <div className="panel" style={{ marginBottom: "2rem" }}>
      <h3 className="panel-title">Recent Stock Movements</h3>
      <p className="panel-subtitle">Audit trail of additions, restocks and dispensing deductions (latest 50).</p>
      {movements.length === 0 ? (
        <p style={{ color: "var(--text-muted)", margin: 0, fontSize: "0.85rem" }}>No stock movements recorded yet.</p>
      ) : (
        <div className="data-table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Time</th>
                <th>Medicine</th>
                <th>Type</th>
                <th>Change</th>
                <th>Balance</th>
                <th>Rx</th>
                <th>By</th>
              </tr>
            </thead>
            <tbody>
              {movements.map((s) => (
                <tr key={s.id}>
                  <td>{fmt(s.createdAt)}</td>
                  <td style={{ fontWeight: 600, color: "var(--navy)" }}>{s.medicineLabel}</td>
                  <td>{s.type}</td>
                  <td style={{ fontWeight: 700, color: s.quantityChange < 0 ? "var(--triage-1-text)" : "var(--status-available-text)" }}>
                    {s.quantityChange > 0 ? `+${s.quantityChange}` : s.quantityChange} {s.unit}
                  </td>
                  <td>{s.quantityAfter}</td>
                  <td>{s.prescriptionId ? `#${s.prescriptionId}` : "—"}</td>
                  <td>{s.performedBy}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
