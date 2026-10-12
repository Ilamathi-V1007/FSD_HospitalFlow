import { useState, useEffect, useCallback } from "react";
import AppShell from "../common/AppShell";
import api from "../common/api";

const TRIAGE_OPTIONS = [
  { level: 1, label: "Level 1 — Emergency (Immediate, Base 100)" },
  { level: 2, label: "Level 2 — Urgent (High Priority, Base 60)" },
  { level: 3, label: "Level 3 — Semi-Urgent (Standard, Base 30)" },
  { level: 4, label: "Level 4 — Non-Urgent (Routine, Base 10)" },
];

export default function ReceptionistDashboard() {
  // Registration Form State
  const [regName, setRegName] = useState("");
  const [regAge, setRegAge] = useState("");
  const [regGender, setRegGender] = useState("Other");
  const [regContact, setRegContact] = useState("");
  const [regLoading, setRegLoading] = useState(false);
  const [regSuccess, setRegSuccess] = useState("");
  const [regError, setRegError] = useState("");

  // Check-In Form State
  const [patients, setPatients] = useState([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedPatientId, setSelectedPatientId] = useState("");
  const [symptoms, setSymptoms] = useState("");
  const [urgencyLevel, setUrgencyLevel] = useState(2);
  const [checkInLoading, setCheckInLoading] = useState(false);
  const [checkInSuccess, setCheckInSuccess] = useState("");
  const [checkInError, setCheckInError] = useState("");

  const loadPatients = useCallback(async (query = "") => {
    try {
      const res = await api.get(`/queue/patients/search?name=${encodeURIComponent(query)}`);
      setPatients(res.data);
      if (res.data.length > 0 && !selectedPatientId) {
        setSelectedPatientId(res.data[0].id);
      }
    } catch {
      // Ignore initial search error
    }
  }, [selectedPatientId]);

  useEffect(() => {
    loadPatients();
  }, [loadPatients]);

  async function handleRegister(e) {
    e.preventDefault();
    setRegLoading(true);
    setRegError("");
    setRegSuccess("");
    try {
      const res = await api.post("/queue/patients", {
        name: regName.trim(),
        age: regAge ? parseInt(regAge, 10) : null,
        gender: regGender,
        contact: regContact.trim(),
      });
      setRegSuccess(`Patient ${res.data.name} (ID: ${res.data.id}) registered successfully.`);
      setRegName("");
      setRegAge("");
      setRegContact("");
      await loadPatients();
      setSelectedPatientId(res.data.id);
    } catch (err) {
      setRegError(err.response?.data?.error || "Failed to register patient.");
    } finally {
      setRegLoading(false);
    }
  }

  async function handleCheckIn(e) {
    e.preventDefault();
    if (!selectedPatientId) {
      setCheckInError("Please select a registered patient.");
      return;
    }
    setCheckInLoading(true);
    setCheckInError("");
    setCheckInSuccess("");
    try {
      const res = await api.post("/queue/check-in", {
        patientId: Number(selectedPatientId),
        symptoms: symptoms.trim(),
        urgencyLevel: Number(urgencyLevel),
      });
      setCheckInSuccess(
        `Checked in: ${res.data.patientName} into live queue (Priority Score: ${res.data.priorityScore.toFixed(1)}).`
      );
      setSymptoms("");
    } catch (err) {
      setCheckInError(err.response?.data?.error || "Failed to check in patient.");
    } finally {
      setCheckInLoading(false);
    }
  }

  return (
    <AppShell title="Patient Intake & Triage Desk">
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1.5rem" }}>
        {/* Panel 1: Register Patient */}
        <div className="panel">
          <h3 className="panel-title">1. Register Patient</h3>
          <p className="panel-subtitle">
            Enter demographic details to create or verify medical record identity.
          </p>

          {regSuccess && <div className="alert alert-success">{regSuccess}</div>}
          {regError && <div className="alert alert-danger">{regError}</div>}

          <form onSubmit={handleRegister}>
            <div className="form-group">
              <label className="form-label">Full Name *</label>
              <input
                type="text"
                required
                className="form-control"
                placeholder="e.g. Ramesh Kumar"
                value={regName}
                onChange={(e) => setRegName(e.target.value)}
              />
            </div>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.75rem" }}>
              <div className="form-group">
                <label className="form-label">Age</label>
                <input
                  type="number"
                  className="form-control"
                  placeholder="e.g. 42"
                  min="1"
                  max="120"
                  value={regAge}
                  onChange={(e) => setRegAge(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Gender</label>
                <select
                  className="form-control"
                  value={regGender}
                  onChange={(e) => setRegGender(e.target.value)}
                >
                  <option value="Male">Male</option>
                  <option value="Female">Female</option>
                  <option value="Other">Other</option>
                </select>
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Contact Number</label>
              <input
                type="text"
                className="form-control"
                placeholder="e.g. +91 9876543210"
                value={regContact}
                onChange={(e) => setRegContact(e.target.value)}
              />
            </div>

            <button
              type="submit"
              disabled={regLoading}
              className="btn btn-navy"
              style={{ width: "100%", marginTop: "0.5rem" }}
            >
              {regLoading ? "Registering…" : "Register Patient Record"}
            </button>
          </form>
        </div>

        {/* Panel 2: Triage & Queue Check-In */}
        <div className="panel">
          <h3 className="panel-title">2. Triage & Queue Check-In</h3>
          <p className="panel-subtitle">
            Assign clinical urgency and push the patient into the live priority-ranked queue.
          </p>

          {checkInSuccess && <div className="alert alert-success">{checkInSuccess}</div>}
          {checkInError && <div className="alert alert-danger">{checkInError}</div>}

          <form onSubmit={handleCheckIn}>
            <div className="form-group">
              <label className="form-label">Select Registered Patient *</label>
              <input
                type="text"
                className="form-control"
                placeholder="Search patient by name…"
                value={searchQuery}
                onChange={(e) => {
                  setSearchQuery(e.target.value);
                  loadPatients(e.target.value);
                }}
                style={{ marginBottom: "6px" }}
              />
              <select
                required
                className="form-control"
                value={selectedPatientId}
                onChange={(e) => setSelectedPatientId(e.target.value)}
              >
                <option value="">-- Choose patient --</option>
                {patients.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name} (Age: {p.age || "—"}, {p.gender})
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">Triage Urgency Category *</label>
              <select
                className="form-control"
                value={urgencyLevel}
                onChange={(e) => setUrgencyLevel(Number(e.target.value))}
              >
                {TRIAGE_OPTIONS.map((opt) => (
                  <option key={opt.level} value={opt.level}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">Reported Symptoms & Notes</label>
              <textarea
                rows="3"
                className="form-control"
                placeholder="Chief complaints, pain score, onset time…"
                value={symptoms}
                onChange={(e) => setSymptoms(e.target.value)}
              />
            </div>

            <button
              type="submit"
              disabled={checkInLoading || !selectedPatientId}
              className="btn btn-primary"
              style={{ width: "100%", marginTop: "0.5rem" }}
            >
              {checkInLoading ? "Assigning to Queue…" : "Confirm Check-In to Queue"}
            </button>
          </form>
        </div>
      </div>
    </AppShell>
  );
}
