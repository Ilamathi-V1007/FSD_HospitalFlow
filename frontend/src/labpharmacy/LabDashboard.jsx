import { useState, useEffect, useCallback } from "react";
import AppShell from "../common/AppShell";
import api from "../common/api";

const POLL_MS = 12_000;

function fmt(dt) {
  if (!dt) return "—";
  return new Date(dt).toLocaleString("en-IN", {
    day: "2-digit",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
    hour12: true,
  });
}

export default function LabDashboard() {
  const [pending, setPending] = useState([]);
  const [completed, setCompleted] = useState([]);
  const [loadErr, setLoadErr] = useState("");
  const [active, setActive] = useState(null);
  const [resultInput, setResultInput] = useState({});
  const [actionErr, setActionErr] = useState({});
  const [saving, setSaving] = useState({});

  const load = useCallback(async () => {
    try {
      const [p, c] = await Promise.all([
        api.get("/labpharmacy/lab-requests/pending"),
        api.get("/labpharmacy/lab-requests/completed"),
      ]);
      setPending(p.data);
      setCompleted(c.data.slice(-10).reverse());
      setLoadErr("");
    } catch {
      setLoadErr("Could not retrieve lab requests from server.");
    }
  }, []);

  useEffect(() => {
    load();
    const id = setInterval(load, POLL_MS);
    return () => clearInterval(id);
  }, [load]);

  async function handleComplete(req) {
    const result = (resultInput[req.id] || "").trim();
    if (!result) {
      setActionErr((prev) => ({ ...prev, [req.id]: "Laboratory result text is required." }));
      return;
    }
    setSaving((prev) => ({ ...prev, [req.id]: true }));
    setActionErr((prev) => ({ ...prev, [req.id]: "" }));
    try {
      await api.post(`/labpharmacy/lab-requests/${req.id}/complete`, { result });
      setActive(null);
      setResultInput((prev) => {
        const n = { ...prev };
        delete n[req.id];
        return n;
      });
      await load();
    } catch (e) {
      setActionErr((prev) => ({
        ...prev,
        [req.id]: e.response?.data?.error || "Failed to record lab result.",
      }));
    } finally {
      setSaving((prev) => ({ ...prev, [req.id]: false }));
    }
  }

  return (
    <AppShell title="Clinical Laboratory Desk">
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", marginBottom: "1.75rem" }}>
        <div>
          <h2 style={{ fontSize: "1.25rem", fontWeight: 700, color: "var(--ink)", margin: "0 0 0.25rem 0", fontFamily: "var(--font-heading)" }}>
            Diagnostic Specimen Queue Ledger
          </h2>
          <p style={{ fontSize: "0.82rem", color: "var(--ink-muted)", margin: 0 }}>
            Process doctor-ordered investigations. Discharge cannot proceed until all orders are marked complete.
          </p>
        </div>
        <span className="demo-badge">{pending.length} Pending Analysis</span>
      </div>

      {loadErr && <div className="alert alert-danger">{loadErr}</div>}

      {/* Pending Orders Panel */}
      <div className="panel" style={{ marginBottom: "2rem" }}>
        <h3 className="panel-title">Pending Orders</h3>
        <p className="panel-subtitle">Orders requiring laboratory analysis and result entry.</p>

        {pending.length === 0 ? (
          <p style={{ color: "var(--text-muted)", margin: 0, fontSize: "0.85rem" }}>
            No pending laboratory requests in queue.
          </p>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
            {pending.map((req) => (
              <div
                key={req.id}
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
                      {req.testName}
                    </div>
                    <div style={{ fontSize: "0.78rem", color: "var(--text-muted)", marginTop: "2px" }}>
                      Patient: <strong>{req.patientName}</strong> · Appt #{req.appointmentId} · Requested: {fmt(req.requestedAt)}
                    </div>
                  </div>
                  <button
                    type="button"
                    className="btn btn-outline"
                    onClick={() => setActive(active === req.id ? null : req.id)}
                  >
                    {active === req.id ? "Cancel" : "Enter Result"}
                  </button>
                </div>

                {active === req.id && (
                  <div style={{ marginTop: "1rem", paddingTop: "0.85rem", borderTop: "1px solid var(--border-light)" }}>
                    <div className="form-group">
                      <label className="form-label">Clinical Result & Values *</label>
                      <textarea
                        rows="3"
                        className="form-control"
                        placeholder="e.g. Troponin I: 0.12 ng/mL (Normal < 0.04), SpO2: 94%"
                        value={resultInput[req.id] || ""}
                        onChange={(e) => setResultInput({ ...resultInput, [req.id]: e.target.value })}
                      />
                    </div>
                    {actionErr[req.id] && <div className="alert alert-danger">{actionErr[req.id]}</div>}
                    <button
                      type="button"
                      disabled={saving[req.id]}
                      className="btn btn-primary"
                      onClick={() => handleComplete(req)}
                    >
                      {saving[req.id] ? "Saving…" : "Save & Complete Order"}
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Completed Orders History */}
      <div className="panel">
        <h3 className="panel-title">Recent Completed Tests</h3>
        <p className="panel-subtitle">History of recently processed laboratory reports.</p>

        {completed.length === 0 ? (
          <p style={{ color: "var(--text-muted)", margin: 0, fontSize: "0.85rem" }}>
            No recent completed reports.
          </p>
        ) : (
          <div className="data-table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Test Name</th>
                  <th>Patient</th>
                  <th>Clinical Result</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {completed.map((c) => (
                  <tr key={c.id}>
                    <td style={{ fontWeight: 600, color: "var(--navy)" }}>{c.testName}</td>
                    <td>{c.patientName}</td>
                    <td style={{ color: "var(--teal-dark)", fontWeight: 500 }}>{c.result}</td>
                    <td>
                      <span className="badge" style={{ background: "#F0FDF4", color: "#166534", border: "1px solid #BBF7D0" }}>
                        COMPLETED
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
