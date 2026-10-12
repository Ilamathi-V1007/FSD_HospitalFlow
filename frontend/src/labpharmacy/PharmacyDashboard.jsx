import { useState, useEffect, useCallback } from "react";
import AppShell from "../common/AppShell";
import api from "../common/api";

const POLL_MS = 12_000;

export default function PharmacyDashboard() {
  const [pending, setPending] = useState([]);
  const [dispensed, setDispensed] = useState([]);
  const [loadErr, setLoadErr] = useState("");
  const [dispensing, setDispensing] = useState({});
  const [actionErr, setActionErr] = useState({});

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
  }, []);

  useEffect(() => {
    load();
    const id = setInterval(load, POLL_MS);
    return () => clearInterval(id);
  }, [load]);

  async function handleDispense(rx) {
    setDispensing((prev) => ({ ...prev, [rx.id]: true }));
    setActionErr((prev) => ({ ...prev, [rx.id]: "" }));
    try {
      await api.post(`/labpharmacy/prescriptions/${rx.id}/dispense`);
      await load();
    } catch (e) {
      setActionErr((prev) => ({
        ...prev,
        [rx.id]: e.response?.data?.error || "Failed to dispense medication.",
      }));
    } finally {
      setDispensing((prev) => ({ ...prev, [rx.id]: false }));
    }
  }

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
        <span className="demo-badge">{pending.length} In Fulfillment Queue</span>
      </div>

      {loadErr && <div className="alert alert-danger">{loadErr}</div>}

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
            {pending.map((rx) => (
              <div
                key={rx.id}
                style={{
                  border: "1px solid var(--border)",
                  borderRadius: "var(--radius-sm)",
                  padding: "1rem 1.25rem",
                  backgroundColor: "#FFFFFF",
                }}
              >
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
                  <div>
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
                    disabled={dispensing[rx.id]}
                    className="btn btn-primary"
                    onClick={() => handleDispense(rx)}
                  >
                    {dispensing[rx.id] ? "Dispensing…" : "✓ Mark Dispensed"}
                  </button>
                </div>
                {actionErr[rx.id] && <div className="alert alert-danger" style={{ marginTop: "0.75rem" }}>{actionErr[rx.id]}</div>}
              </div>
            ))}
          </div>
        )}
      </div>

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
