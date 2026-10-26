import { useState, useEffect, useCallback } from "react";
import AppShell from "../common/AppShell";
import api from "../common/api";
import MedicineInventoryPanel, { StockMovementLog, StockBadge } from "./MedicineInventoryPanel";
import { MAX_STOCK, apiError, medicineLabel } from "./inventoryUtils";

const POLL_MS = 12_000;

/**
 * Works out what the pharmacist has entered for one prescription and whether it can be dispensed.
 * This only drives the UI; the backend re-checks matching and stock inside a locked transaction.
 */
/** Stocked medicines whose name appears in the prescription text (used only to explain a non-match). */
function sameNameMedicines(details, medicines) {
  const text = (details || "").toLowerCase();
  return medicines.filter((m) => {
    const core = m.name.toLowerCase().replace(/\b(tab|tabs|tablets?|caps?|capsules?|syrup|inj|injection)\b\.?/g, "").trim();
    return core.length > 2 && text.includes(core);
  });
}

function buildDispensePlan(rxMatches, entered, inventoryFailed, nearMatches = []) {
  if (rxMatches === undefined) {
    return {
      lines: [],
      items: [],
      blockReason: inventoryFailed
        ? "Inventory unavailable, so stock cannot be verified. Dispensing is disabled."
        : "Checking inventory…",
    };
  }
  const lines = rxMatches.map((m) => {
    const raw = String(entered[m.id] ?? "").trim();
    const n = Number(raw);
    const valid = raw === "" || (/^\d+$/.test(raw) && n >= 1 && n <= MAX_STOCK);
    return { m, raw, n, valid, short: valid && raw !== "" && n > m.quantity };
  });
  const items = lines.filter((l) => l.raw !== "").map((l) => ({ medicineId: l.m.id, quantity: l.n }));
  const shortLine = lines.find((l) => l.short);

  let blockReason = "";
  if (rxMatches.length === 0 && nearMatches.length > 0) {
    blockReason = `Inventory has ${nearMatches.map(medicineLabel).join(", ")}, but the prescription states a different strength. Add the prescribed strength to inventory or confirm with the doctor.`;
  } else if (rxMatches.length === 0) {
    blockReason = "No inventory medicine matches this prescription. Add it to inventory using the prescribed name and strength.";
  } else if (lines.every((l) => l.m.quantity === 0)) {
    blockReason = `Out of stock: ${rxMatches.map(medicineLabel).join(", ")}. Restock before dispensing.`;
  } else if (lines.some((l) => !l.valid)) {
    blockReason = "Quantities must be whole numbers of at least 1.";
  } else if (shortLine) {
    blockReason = `Insufficient stock for ${medicineLabel(shortLine.m)}: only ${shortLine.m.quantity} ${shortLine.m.unit} available.`;
  } else if (items.length === 0) {
    blockReason = "Enter the quantity to hand over for at least one matched medicine.";
  }
  return { lines, items, blockReason };
}

export default function PharmacyDashboard() {
  const [pending, setPending] = useState([]);
  const [dispensed, setDispensed] = useState([]);
  const [loadErr, setLoadErr] = useState("");
  const [dispensing, setDispensing] = useState({});
  const [actionErr, setActionErr] = useState({});

  // Inventory state (loaded separately so a failure never hides the prescription queue)
  const [medicines, setMedicines] = useState([]);
  const [summary, setSummary] = useState(null);
  const [movements, setMovements] = useState([]);
  const [matches, setMatches] = useState({});
  const [inventoryLoaded, setInventoryLoaded] = useState(false);
  const [inventoryErr, setInventoryErr] = useState("");
  const [dispenseQty, setDispenseQty] = useState({});
  const [dispenseMsg, setDispenseMsg] = useState("");

  const loadInventory = useCallback(async () => {
    try {
      const [inv, sum, mov, mat] = await Promise.all([
        api.get("/inventory"),
        api.get("/inventory/summary"),
        api.get("/inventory/movements"),
        api.get("/inventory/prescriptions/pending/matches"),
      ]);
      setMedicines(inv.data);
      setSummary(sum.data);
      setMovements(mov.data);
      setMatches(Object.fromEntries(mat.data.map((x) => [x.prescriptionId, x.matches])));
      setInventoryErr("");
      setInventoryLoaded(true);
    } catch (e) {
      setMatches({});
      setInventoryErr(apiError(e, "Could not retrieve medicine inventory from server."));
    }
  }, []);

  const load = useCallback(async () => {
    try {
      const [p, d] = await Promise.all([
        api.get("/labpharmacy/prescriptions/pending"),
        api.get("/labpharmacy/prescriptions/dispensed"),
      ]);
      setPending(p.data);
      setDispensed(d.data.slice(-10).reverse());
      setLoadErr("");
    } catch {
      setLoadErr("Could not retrieve prescriptions from server.");
    }
    await loadInventory();
  }, [loadInventory]);

  useEffect(() => {
    load();
    const id = setInterval(load, POLL_MS);
    return () => clearInterval(id);
  }, [load]);

  async function handleDispense(rx, plan) {
    if (plan.blockReason) return;
    setDispensing((prev) => ({ ...prev, [rx.id]: true }));
    setActionErr((prev) => ({ ...prev, [rx.id]: "" }));
    setDispenseMsg("");
    try {
      await api.post(`/labpharmacy/prescriptions/${rx.id}/dispense`, { items: plan.items });
      setDispenseQty((prev) => {
        const n = { ...prev };
        delete n[rx.id];
        return n;
      });
      setDispenseMsg(`Prescription for ${rx.patientName} dispensed and stock deducted.`);
      await load();
    } catch (e) {
      setActionErr((prev) => ({
        ...prev,
        [rx.id]: apiError(e, "Failed to dispense medication."),
      }));
      await loadInventory(); // show the latest stock after a rejection
    } finally {
      setDispensing((prev) => ({ ...prev, [rx.id]: false }));
    }
  }

  function setLineQty(rxId, medicineId, value) {
    setDispenseQty((prev) => ({ ...prev, [rxId]: { ...(prev[rxId] || {}), [medicineId]: value } }));
  }

  const lowOrOut = summary ? summary.lowStock + summary.outOfStock : 0;

  return (
    <AppShell title="Pharmacy Dispensing Desk">
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", marginBottom: "1.75rem" }}>
        <div>
          <h2 style={{ fontSize: "1.25rem", fontWeight: 700, color: "var(--ink)", margin: "0 0 0.25rem 0", fontFamily: "var(--font-heading)" }}>
            Prescription Fulfillment Ledger
          </h2>
          <p style={{ fontSize: "0.82rem", color: "var(--ink-muted)", margin: 0 }}>
            Review physician orders, prepare dosages, and record medication dispensing.
          </p>
        </div>
        <div style={{ display: "flex", gap: "6px", flexWrap: "wrap", justifyContent: "flex-end" }}>
          <span className="demo-badge">{pending.length} In Fulfillment Queue</span>
          {lowOrOut > 0 && <span className="badge badge-urgent">{lowOrOut} Need Reorder</span>}
        </div>
      </div>

      {loadErr && <div className="alert alert-danger">{loadErr}</div>}
      {dispenseMsg && <div className="alert alert-success">{dispenseMsg}</div>}

      {/* Pending Prescriptions */}
      <div className="panel" style={{ marginBottom: "2rem" }}>
        <h3 className="panel-title">Pending Orders</h3>
        <p className="panel-subtitle">Prescriptions awaiting pharmacist verification and handover.</p>

        {pending.length === 0 ? (
          <p style={{ color: "var(--text-muted)", margin: 0, fontSize: "0.85rem" }}>
            No pending prescriptions in queue.
          </p>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
            {pending.map((rx) => {
              const rxMatches = inventoryErr || !inventoryLoaded ? undefined : matches[rx.id] ?? [];
              const nearMatches = rxMatches && rxMatches.length === 0 ? sameNameMedicines(rx.medicineDetails, medicines) : [];
              const plan = buildDispensePlan(rxMatches, dispenseQty[rx.id] || {}, !!inventoryErr, nearMatches);
              return (
              <div
                key={rx.id}
                style={{
                  border: "1px solid var(--border)",
                  borderRadius: "var(--radius-sm)",
                  padding: "1rem 1.25rem",
                  backgroundColor: "#FFFFFF",
                }}
              >
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: "1rem", flexWrap: "wrap" }}>
                  <div style={{ flex: "1 1 260px" }}>
                    <div style={{ fontWeight: 700, fontSize: "0.95rem", color: "var(--navy)" }}>
                      Patient: {rx.patientName}
                    </div>
                    <div style={{ fontSize: "0.82rem", color: "var(--text-main)", marginTop: "4px", background: "#F8FAF9", padding: "6px 10px", borderRadius: "var(--radius-sm)", border: "1px solid var(--border-light)" }}>
                      {rx.medicineDetails}
                    </div>
                    <div style={{ fontSize: "0.75rem", color: "var(--text-muted)", marginTop: "4px" }}>
                      Appt #{rx.appointmentId} · Status: <strong>{rx.status}</strong>
                    </div>
                  </div>
                  <button
                    type="button"
                    disabled={dispensing[rx.id] || !!plan.blockReason}
                    title={plan.blockReason || undefined}
                    className="btn btn-primary"
                    onClick={() => handleDispense(rx, plan)}
                  >
                    {dispensing[rx.id] ? "Dispensing…" : "✓ Mark Dispensed"}
                  </button>
                </div>

                {/* Inventory check: matched medicines and quantity to hand over */}
                {plan.lines.length > 0 && (
                  <div className="data-table-container" style={{ marginTop: "0.75rem" }}>
                    <table className="data-table">
                      <thead>
                        <tr>
                          <th>Matched Inventory Medicine</th>
                          <th>Available</th>
                          <th>Status</th>
                          <th>Qty to Dispense</th>
                        </tr>
                      </thead>
                      <tbody>
                        {plan.lines.map(({ m, raw, valid, short }) => (
                          <tr key={m.id}>
                            <td style={{ fontWeight: 600, color: "var(--navy)" }}>{medicineLabel(m)}</td>
                            <td>{m.quantity} {m.unit}</td>
                            <td><StockBadge status={m.status} /></td>
                            <td>
                              <input
                                className="form-control"
                                type="number"
                                min="1"
                                max={m.quantity}
                                step="1"
                                placeholder={m.quantity === 0 ? "—" : "Qty"}
                                disabled={m.quantity === 0 || dispensing[rx.id]}
                                aria-label={`Quantity of ${medicineLabel(m)} to dispense`}
                                style={{ width: "90px", borderColor: !valid || short ? "var(--triage-1-text)" : undefined }}
                                value={raw}
                                onChange={(e) => setLineQty(rx.id, m.id, e.target.value)}
                              />
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
                {plan.blockReason && (
                  <div style={{ fontSize: "0.78rem", color: "var(--triage-2-text)", marginTop: "0.6rem" }}>
                    {plan.blockReason}
                  </div>
                )}
                {actionErr[rx.id] && <div className="alert alert-danger" style={{ marginTop: "0.75rem" }}>{actionErr[rx.id]}</div>}
              </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Medicine Inventory */}
      <MedicineInventoryPanel
        medicines={medicines}
        summary={summary}
        error={inventoryErr}
        loaded={inventoryLoaded}
        onChanged={loadInventory}
      />
      <StockMovementLog movements={movements} />

      {/* Dispensed History */}
      <div className="panel">
        <h3 className="panel-title">Recent Dispensed Medications</h3>
        <p className="panel-subtitle">Audit log of fulfilled outpatient prescriptions.</p>

        {dispensed.length === 0 ? (
          <p style={{ color: "var(--text-muted)", margin: 0, fontSize: "0.85rem" }}>
            No medications dispensed recently.
          </p>
        ) : (
          <div className="data-table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Patient</th>
                  <th>Medication Details</th>
                  <th>Appt ID</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {dispensed.map((d) => (
                  <tr key={d.id}>
                    <td style={{ fontWeight: 600, color: "var(--navy)" }}>{d.patientName}</td>
                    <td>{d.medicineDetails}</td>
                    <td>#{d.appointmentId}</td>
                    <td>
                      <span className="badge" style={{ background: "#F0FDF4", color: "#166534", border: "1px solid #BBF7D0" }}>
                        DISPENSED
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </AppShell>
  );
}
