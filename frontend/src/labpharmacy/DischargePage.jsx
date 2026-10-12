import { useState, useEffect, useCallback } from "react";
import AppShell from "../common/AppShell";
import api from "../common/api";

const POLL_MS = 15_000;

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

export default function DischargePage() {
  const [ready, setReady] = useState([]);
  const [loadErr, setLoadErr] = useState("");
  const [active, setActive] = useState(null);
  const [diagnosis, setDiagnosis] = useState({});
  const [discharging, setDischarging] = useState({});
  const [actionErr, setActionErr] = useState({});
  const [successMsg, setSuccessMsg] = useState({});

  const load = useCallback(async () => {
    try {
      const res = await api.get("/labpharmacy/discharge/ready");
      setReady(res.data);
      setLoadErr("");
    } catch {
      setLoadErr("Could not retrieve discharge queue from server.");
    }
  }, []);

  useEffect(() => {
    load();
    const id = setInterval(load, POLL_MS);
    return () => clearInterval(id);
  }, [load]);

  async function handleDischarge(apptId) {
    const diagText = (diagnosis[apptId] || "").trim();
    if (!diagText) {
      setActionErr((prev) => ({ ...prev, [apptId]: "Discharge diagnosis summary is required." }));
      return;
    }
    setDischarging((prev) => ({ ...prev, [apptId]: true }));
    setActionErr((prev) => ({ ...prev, [apptId]: "" }));
    try {
      const res = await api.post(`/labpharmacy/discharge/${apptId}`, { diagnosis: diagText });
      setSuccessMsg((prev) => ({
        ...prev,
        [apptId]: `Patient ${res.data.patientName} discharged successfully. Bed released to available pool.`,
      }));
      setActive(null);
      setDiagnosis((prev) => {
        const n = { ...prev };
        delete n[apptId];
        return n;
      });
      setTimeout(load, 1500);
    } catch (e) {
      setActionErr((prev) => ({
        ...prev,
        [apptId]: e.response?.data?.error || "Discharge failed.",
      }));
    } finally {
      setDischarging((prev) => ({ ...prev, [apptId]: false }));
    }
  }

  return (
    <AppShell title="Patient Discharge Coordination">
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", marginBottom: "1.75rem" }}>
        <div>
          <h2 style={{ fontSize: "1.25rem", fontWeight: 700, color: "var(--ink)", margin: "0 0 0.25rem 0", fontFamily: "var(--font-heading)" }}>
            Discharge Eligibility Ledger
          </h2>
          <p style={{ fontSize: "0.82rem", color: "var(--ink-muted)", margin: 0 }}>
            Patients with completed examinations and cleared laboratory tests. Finalizes visit and frees bed.
          </p>
        </div>
        <span className="demo-badge">{ready.length} Eligible for Discharge</span>
      </div>

      {loadErr && <div className="alert alert-danger">{loadErr}</div>}

      <div className="panel">
        <h3 className="panel-title">Patients Eligible for Discharge</h3>
        <p className="panel-subtitle">
          Discharge requires visit summary diagnosis and automatically returns the occupied bed to the available pool.
        </p>

        {ready.length === 0 ? (
          <p style={{ color: "var(--text-muted)", margin: 0, fontSize: "0.85rem" }}>
            No patients currently ready for discharge. (Pending laboratory tests will block discharge until completed).
          </p>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
            {ready.map((appt) => (
              <div
                key={appt.appointmentId}
                style={{
                  border: "1px solid var(--border)",
                  borderRadius: "var(--radius-sm)",
                  padding: "1.25rem",
                  backgroundColor: "#FFFFFF",
                }}
              >
                {successMsg[appt.appointmentId] && (
                  <div className="alert alert-success">{successMsg[appt.appointmentId]}</div>
                )}

                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
                  <div>
                    <div style={{ fontWeight: 800, fontSize: "1.05rem", color: "var(--navy)" }}>
                      {appt.patientName}
                    </div>
                    <div style={{ fontSize: "0.8rem", color: "var(--text-muted)", marginTop: "3px" }}>
                      Appt #{appt.appointmentId} · Started: {fmt(appt.startedAt)} ·{" "}
                      <strong>{appt.bedNumber ? `Bed ${appt.bedNumber}` : "No Bed"}</strong> ·{" "}
                      {appt.labCount === 0 ? "0 Lab Orders" : `${appt.labCount} Labs Cleared`}
                    </div>
                  </div>

                  <button
                    type="button"
                    className="btn btn-outline"
                    onClick={() => setActive(active === appt.appointmentId ? null : appt.appointmentId)}
                  >
                    {active === appt.appointmentId ? "Cancel" : "Process Discharge"}
                  </button>
                </div>

                {active === appt.appointmentId && (
                  <div style={{ marginTop: "1rem", paddingTop: "0.85rem", borderTop: "1px solid var(--border-light)" }}>
                    <div className="form-group">
                      <label className="form-label">Clinical Discharge Summary & Diagnosis *</label>
                      <textarea
                        rows="3"
                        className="form-control"
                        placeholder="e.g. Acute exacerbation resolved. Prescribed oral antibiotics. Advised review in 1 week."
                        value={diagnosis[appt.appointmentId] || ""}
                        onChange={(e) =>
                          setDiagnosis({ ...diagnosis, [appt.appointmentId]: e.target.value })
                        }
                      />
                    </div>
                    {actionErr[appt.appointmentId] && (
                      <div className="alert alert-danger">{actionErr[appt.appointmentId]}</div>
                    )}
                    <button
                      type="button"
                      disabled={discharging[appt.appointmentId]}
                      className="btn btn-primary"
                      onClick={() => handleDischarge(appt.appointmentId)}
                    >
                      {discharging[appt.appointmentId] ? "Processing…" : "Confirm Discharge & Release Bed"}
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </AppShell>
  );
}
