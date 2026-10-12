import { useState, useEffect, useCallback } from "react";
import AppShell from "../common/AppShell";
import api from "../common/api";

const TRIAGE_LABELS = {
  1: "Level 1 · Emergency",
  2: "Level 2 · Urgent",
  3: "Level 3 · Semi-Urgent",
  4: "Level 4 · Non-Urgent",
};

export default function DoctorDashboard() {
  const [doctorState, setDoctorState] = useState(null);
  const [beds, setBeds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionSuccess, setActionSuccess] = useState("");

  // Sub-action states
  const [selectedBedId, setSelectedBedId] = useState("");
  const [labTestName, setLabTestName] = useState("");
  const [prescriptionText, setPrescriptionText] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const loadDoctorState = useCallback(async () => {
    try {
      const [stateRes, bedsRes] = await Promise.all([
        api.get("/allocation/doctor/me"),
        api.get("/allocation/beds"),
      ]);
      setDoctorState(stateRes.data);
      setBeds(bedsRes.data);
      setError("");
    } catch (e) {
      setError(e.response?.data?.error || "Could not retrieve doctor profile.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadDoctorState();
  }, [loadDoctorState]);

  async function handleToggleStatus(newStatus) {
    try {
      setError("");
      await api.post("/allocation/doctor/status", { status: newStatus });
      await loadDoctorState();
    } catch (e) {
      setError(e.response?.data?.error || "Failed to update availability status.");
    }
  }

  async function handleCallNext() {
    setSubmitting(true);
    setError("");
    setActionSuccess("");
    try {
      await api.post("/allocation/call-next");
      setActionSuccess("Next patient claimed successfully into active consultation.");
      await loadDoctorState();
    } catch (e) {
      setError(e.response?.data?.error || "Unable to call next patient.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleOrderLab(e) {
    e.preventDefault();
    if (!doctorState?.activeConsultation || !labTestName.trim()) return;
    setSubmitting(true);
    setError("");
    setActionSuccess("");
    try {
      await api.post("/labpharmacy/lab-requests", {
        appointmentId: doctorState.activeConsultation.appointmentId,
        testName: labTestName.trim(),
      });
      setActionSuccess(`Lab order "${labTestName}" submitted to Laboratory queue.`);
      setLabTestName("");
    } catch (e) {
      setError(e.response?.data?.error || "Failed to create lab request.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleWritePrescription(e) {
    e.preventDefault();
    if (!doctorState?.activeConsultation || !prescriptionText.trim()) return;
    setSubmitting(true);
    setError("");
    setActionSuccess("");
    try {
      await api.post("/labpharmacy/prescriptions", {
        appointmentId: doctorState.activeConsultation.appointmentId,
        medicineDetails: prescriptionText.trim(),
      });
      setActionSuccess("Prescription recorded and queued for Pharmacy dispensation.");
      setPrescriptionText("");
    } catch (e) {
      setError(e.response?.data?.error || "Failed to create prescription.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleAssignBed(e) {
    e.preventDefault();
    if (!doctorState?.activeConsultation || !selectedBedId) return;
    setSubmitting(true);
    setError("");
    setActionSuccess("");
    try {
      await api.post(`/allocation/appointments/${doctorState.activeConsultation.appointmentId}/assign-bed`, {
        bedId: Number(selectedBedId),
      });
      setActionSuccess("Hospital bed reserved and assigned to patient.");
      setSelectedBedId("");
      await loadDoctorState();
    } catch (e) {
      setError(e.response?.data?.error || "Failed to assign bed.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleCompleteConsultation() {
    if (!doctorState?.activeConsultation) return;
    setSubmitting(true);
    setError("");
    setActionSuccess("");
    try {
      await api.post(`/allocation/appointments/${doctorState.activeConsultation.appointmentId}/complete`);
      setActionSuccess("Consultation concluded. Patient routed for diagnostics or discharge.");
      await loadDoctorState();
    } catch (e) {
      setError(e.response?.data?.error || "Failed to finish consultation.");
    } finally {
      setSubmitting(false);
    }
  }

  const active = doctorState?.activeConsultation;
  const availableBeds = beds.filter((b) => b.status === "AVAILABLE");

  return (
    <AppShell title="Doctor Consultation Desk">
      {/* Top Status Banner */}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1.5rem" }}>
        <div>
          <h2 style={{ fontSize: "1.25rem", fontWeight: 700, color: "var(--ink)", margin: "0 0 0.25rem 0", fontFamily: "var(--font-heading)" }}>
            Clinical Examination Workspace
          </h2>
          <p style={{ fontSize: "0.82rem", color: "var(--ink-muted)", margin: 0 }}>
            Department: <strong>{doctorState?.department || "General OPD"}</strong> · Status:{" "}
            <span className="badge badge-available">{doctorState?.availabilityStatus}</span>
          </p>
        </div>

        {doctorState && (
          <div style={{ display: "flex", gap: "8px" }}>
            <button
              type="button"
              className="btn btn-outline"
              onClick={() =>
                handleToggleStatus(doctorState.availabilityStatus === "AVAILABLE" ? "ON_BREAK" : "AVAILABLE")
              }
            >
              {doctorState.availabilityStatus === "AVAILABLE" ? "Toggle Break Status" : "Mark Available"}
            </button>
          </div>
        )}
      </div>

      {error && <div className="alert alert-danger">{error}</div>}
      {actionSuccess && <div className="alert alert-success">{actionSuccess}</div>}

      {loading ? (
        <div style={{ padding: "3rem", textAlign: "center", color: "var(--text-muted)" }}>
          Initializing physician desk…
        </div>
      ) : !active ? (
        /* Standby / Next Patient Queue */
        <div className="panel" style={{ textAlign: "center", padding: "3.5rem 2rem" }}>
          <h3 style={{ fontSize: "1.2rem", fontWeight: 700, color: "var(--navy)", margin: "0 0 0.4rem 0" }}>
            No Active Consultation
          </h3>
          <p style={{ fontSize: "0.85rem", color: "var(--text-muted)", maxWidth: "460px", margin: "0 auto 1.75rem auto", lineHeight: 1.5 }}>
            Ready to examine the next patient? Click below to atomically claim the highest-priority arrival waiting in the OPD queue.
          </p>
          <button
            type="button"
            className="btn btn-primary"
            style={{ padding: "0.65rem 1.5rem", fontSize: "0.9rem" }}
            onClick={handleCallNext}
            disabled={submitting || doctorState?.availabilityStatus === "ON_BREAK"}
          >
            {submitting ? "Calling…" : "✦ Call Next Patient from Queue"}
          </button>
        </div>
      ) : (
        /* Active Consultation Workspace */
        <div style={{ display: "grid", gridTemplateColumns: "1.1fr 1.9fr", gap: "1.5rem" }}>
          {/* Patient Details Panel */}
          <div className="panel">
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1rem" }}>
              <span className="badge" style={{ background: "#EFF6FF", color: "#1D4ED8", border: "1px solid #DBEAFE" }}>
                IN CONSULTATION
              </span>
              <span style={{ fontSize: "0.75rem", color: "var(--text-muted)" }}>
                Appt #{active.appointmentId}
              </span>
            </div>

            <div style={{ borderBottom: "1px solid var(--border-light)", paddingBottom: "1rem", marginBottom: "1rem" }}>
              <div style={{ fontSize: "1.25rem", fontWeight: 800, color: "var(--navy)" }}>{active.patientName}</div>
              <div style={{ fontSize: "0.8rem", color: "var(--text-muted)", marginTop: "2px" }}>
                Age: {active.age || "—"} · Gender: {active.gender || "—"} · Contact: {active.contact || "—"}
              </div>
            </div>

            <div style={{ marginBottom: "1rem" }}>
              <div style={{ fontSize: "0.72rem", color: "var(--text-muted)", fontWeight: 700, textTransform: "uppercase" }}>Triage Priority</div>
              <div style={{ fontWeight: 600, color: "var(--navy)", marginTop: "3px" }}>
                {TRIAGE_LABELS[active.urgencyLevel] || "Level 2 · Urgent"}
              </div>
            </div>

            <div style={{ marginBottom: "1rem" }}>
              <div style={{ fontSize: "0.72rem", color: "var(--text-muted)", fontWeight: 700, textTransform: "uppercase" }}>Symptoms & Chief Complaint</div>
              <div style={{ fontSize: "0.85rem", color: "var(--text-main)", background: "#F8FAF9", padding: "0.6rem 0.75rem", borderRadius: "var(--radius-sm)", border: "1px solid var(--border-light)", marginTop: "4px" }}>
                {active.symptoms || "No recorded complaints."}
              </div>
            </div>

            <div style={{ marginBottom: "1.5rem" }}>
              <div style={{ fontSize: "0.72rem", color: "var(--text-muted)", fontWeight: 700, textTransform: "uppercase" }}>Assigned Hospital Bed</div>
              <div style={{ fontWeight: 600, color: active.bedNumber ? "var(--teal)" : "var(--text-muted)", marginTop: "3px" }}>
                {active.bedNumber ? `Bed ${active.bedNumber}` : "None allocated"}
              </div>
            </div>

            <button
              type="button"
              className="btn btn-navy"
              style={{ width: "100%", padding: "0.6rem" }}
              onClick={handleCompleteConsultation}
              disabled={submitting}
            >
              {submitting ? "Processing…" : "✓ Conclude Consultation"}
            </button>
          </div>

          {/* Clinical Orders & Bed Allocation Panel */}
          <div style={{ display: "flex", flexDirection: "column", gap: "1.25rem" }}>
            {/* 1. Laboratory Order */}
            <div className="panel" style={{ padding: "1.25rem" }}>
              <h4 style={{ fontSize: "0.95rem", fontWeight: 700, color: "var(--navy)", margin: "0 0 0.75rem 0" }}>
                1. Order Diagnostic Laboratory Test
              </h4>
              <form onSubmit={handleOrderLab} style={{ display: "flex", gap: "8px" }}>
                <input
                  type="text"
                  required
                  className="form-control"
                  style={{ flex: 1 }}
                  placeholder="e.g. Complete Blood Count (CBC), Troponin I"
                  value={labTestName}
                  onChange={(e) => setLabTestName(e.target.value)}
                />
                <button type="submit" disabled={submitting} className="btn btn-primary">
                  Order Test
                </button>
              </form>
            </div>

            {/* 2. Prescription Order */}
            <div className="panel" style={{ padding: "1.25rem" }}>
              <h4 style={{ fontSize: "0.95rem", fontWeight: 700, color: "var(--navy)", margin: "0 0 0.75rem 0" }}>
                2. Prescribe Medication
              </h4>
              <form onSubmit={handleWritePrescription} style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
                <textarea
                  rows="2"
                  required
                  className="form-control"
                  placeholder="e.g. Paracetamol 650mg TDS, Amoxicillin 500mg BD x 5 days"
                  value={prescriptionText}
                  onChange={(e) => setPrescriptionText(e.target.value)}
                />
                <button type="submit" disabled={submitting} className="btn btn-primary" style={{ alignSelf: "flex-end" }}>
                  Record Prescription
                </button>
              </form>
            </div>

            {/* 3. Bed Assignment */}
            <div className="panel" style={{ padding: "1.25rem" }}>
              <h4 style={{ fontSize: "0.95rem", fontWeight: 700, color: "var(--navy)", margin: "0 0 0.75rem 0" }}>
                3. Allocate Hospital Bed
              </h4>
              <form onSubmit={handleAssignBed} style={{ display: "flex", gap: "8px" }}>
                <select
                  required
                  className="form-control"
                  style={{ flex: 1 }}
                  value={selectedBedId}
                  onChange={(e) => setSelectedBedId(e.target.value)}
                >
                  <option value="">-- Select available bed --</option>
                  {availableBeds.map((b) => (
                    <option key={b.id} value={b.id}>
                      Bed {b.bedNumber} (Available)
                    </option>
                  ))}
                </select>
                <button type="submit" disabled={submitting || !selectedBedId} className="btn btn-navy">
                  Assign Bed
                </button>
              </form>
            </div>
          </div>
        </div>
      )}
    </AppShell>
  );
}
