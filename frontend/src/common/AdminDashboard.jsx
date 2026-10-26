import { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import AppShell from "./AppShell";
import api from "./api";

const ROLES = [
  { value: "DOCTOR", label: "Doctor (Consultation Staff)" },
  { value: "RECEPTIONIST", label: "Receptionist (Intake & Triage)" },
  { value: "LAB_TECH", label: "Lab Technician (Diagnostics)" },
  { value: "PHARMACIST", label: "Pharmacist (Dispensation)" },
  { value: "ADMIN", label: "Administrator (Operations & Audit)" },
];

export default function AdminDashboard() {
  const navigate = useNavigate();
  const [beds, setBeds] = useState([]);
  const [queue, setQueue] = useState([]);
  const [pendingLabs, setPendingLabs] = useState([]);
  const [pendingRx, setPendingRx] = useState([]);
  const [staffList, setStaffList] = useState([]);
  const [loading, setLoading] = useState(true);

  // Staff creation form state
  const [showStaffModal, setShowStaffModal] = useState(false);
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [role, setRole] = useState("DOCTOR");
  const [showPassword, setShowPassword] = useState(false);
  const [staffSubmitting, setStaffSubmitting] = useState(false);
  const [staffError, setStaffError] = useState("");
  const [staffSuccess, setStaffSuccess] = useState("");

  const loadData = useCallback(async () => {
    try {
      const [bedsRes, queueRes, labsRes, rxRes, staffRes] = await Promise.all([
        api.get("/allocation/beds").catch(() => ({ data: [] })),
        api.get("/queue").catch(() => ({ data: [] })),
        api.get("/labpharmacy/lab-requests/pending").catch(() => ({ data: [] })),
        api.get("/labpharmacy/prescriptions/pending").catch(() => ({ data: [] })),
        api.get("/auth/staff").catch(() => ({ data: [] })),
      ]);
      setBeds(bedsRes.data || []);
      setQueue(queueRes.data || []);
      setPendingLabs(labsRes.data || []);
      setPendingRx(rxRes.data || []);
      setStaffList(staffRes.data || []);
    } catch {
      // Keep UI resilient
    } finally {
      setLoading(false);
    }
  }, []);

  // Delete staff member (admin only)
  const handleDeleteStaff = async (id) => {
    if (window.confirm('Are you sure you want to delete this staff member?')) {
      try {
        await api.delete(`/auth/staff/${id}`);
        // Refresh staff list after deletion
        const refreshed = await api.get('/auth/staff');
        setStaffList(refreshed.data || []);
      } catch (err) {
        alert('Failed to delete staff: ' + (err.response?.data?.error || err.message));
      }
    }
  };

  useEffect(() => {
    loadData();
  }, [loadData]);

  async function handleCreateStaff(e) {
    e.preventDefault();
    setStaffError("");
    setStaffSuccess("");

    if (!name.trim() || !email.trim() || !password.trim()) {
      setStaffError("Please fill in all required fields.");
      return;
    }
    if (password.length < 6) {
      setStaffError("Password must be at least 6 characters long.");
      return;
    }
    if (password !== confirmPassword) {
      setStaffError("Passwords do not match.");
      return;
    }

    setStaffSubmitting(true);
    try {
      const res = await api.post("/auth/staff", {
        name: name.trim(),
        email: email.trim().toLowerCase(),
        password,
        role,
      });

      setStaffSuccess(`Staff member "${res.data.name}" (${res.data.role}) successfully created.`);
      setName("");
      setEmail("");
      setPassword("");
      setConfirmPassword("");
      setRole("DOCTOR");
      // Refresh staff list
      const updatedStaff = await api.get("/auth/staff");
      setStaffList(updatedStaff.data || []);
      setTimeout(() => {
        setShowStaffModal(false);
        setStaffSuccess("");
      }, 1800);
    } catch (err) {
      setStaffError(err.response?.data?.error || err.response?.data?.message || "Failed to create staff account.");
    } finally {
      setStaffSubmitting(false);
    }
  }

  const occupiedBeds = beds.filter((b) => b.status === "OCCUPIED").length;
  const availableBeds = beds.filter((b) => b.status === "AVAILABLE").length;
  const emergencyCount = queue.filter((q) => q.urgencyLevel === 1).length;

  return (
    <AppShell title="Operational Oversight Dashboard">
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", marginBottom: "1.75rem" }}>
        <div>
          <h2 style={{ fontSize: "1.25rem", fontWeight: 700, color: "var(--ink)", margin: "0 0 0.25rem 0", fontFamily: "var(--font-heading)" }}>
            Hospital Capacity & Operations Oversight
          </h2>
          <p style={{ fontSize: "0.82rem", color: "var(--ink-muted)", margin: 0 }}>
            Unified real-time metrics across Intake, Bed Allocation, Diagnostics, Staff Management, and Outpatient Flow.
          </p>
        </div>
        <div style={{ display: "flex", gap: "0.75rem", alignItems: "center" }}>
          <button
            type="button"
            className="btn btn-primary"
            onClick={() => {
              setShowStaffModal(true);
              setStaffError("");
              setStaffSuccess("");
            }}
            style={{ fontSize: "0.82rem", padding: "0.45rem 0.9rem" }}
          >
            + Create Staff Account
          </button>
          <span className="demo-badge">Ward Operations Sync</span>
        </div>
      </div>

      {loading ? (
        <div style={{ padding: "3rem", textAlign: "center", color: "var(--text-muted)" }}>
          Loading live operational metrics…
        </div>
      ) : (
        <>
          {/* Key Metrics Grid */}
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(210px, 1fr))", gap: "1rem", marginBottom: "2rem" }}>
            <div className="panel" style={{ padding: "1.25rem" }}>
              <div style={{ fontSize: "0.72rem", color: "var(--text-muted)", fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.04em" }}>
                Waiting in Queue
              </div>
              <div style={{ fontSize: "2rem", fontWeight: 800, color: "var(--navy)", marginTop: "4px" }}>
                {queue.length}
              </div>
              <div style={{ fontSize: "0.75rem", color: "var(--teal)", marginTop: "4px", fontWeight: 600 }}>
                {emergencyCount} Emergency (Level 1)
              </div>
            </div>

            <div className="panel" style={{ padding: "1.25rem" }}>
              <div style={{ fontSize: "0.72rem", color: "var(--text-muted)", fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.04em" }}>
                Bed Occupancy
              </div>
              <div style={{ fontSize: "2rem", fontWeight: 800, color: "var(--navy)", marginTop: "4px" }}>
                {occupiedBeds} / {beds.length}
              </div>
              <div style={{ fontSize: "0.75rem", color: "#166534", marginTop: "4px", fontWeight: 600 }}>
                {availableBeds} Available Beds
              </div>
            </div>

            <div className="panel" style={{ padding: "1.25rem" }}>
              <div style={{ fontSize: "0.72rem", color: "var(--text-muted)", fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.04em" }}>
                Pending Lab Orders
              </div>
              <div style={{ fontSize: "2rem", fontWeight: 800, color: "var(--navy)", marginTop: "4px" }}>
                {pendingLabs.length}
              </div>
              <div style={{ fontSize: "0.75rem", color: "var(--text-muted)", marginTop: "4px" }}>
                Awaiting specimen analysis
              </div>
            </div>

            <div className="panel" style={{ padding: "1.25rem" }}>
              <div style={{ fontSize: "0.72rem", color: "var(--text-muted)", fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.04em" }}>
                Pending Prescriptions
              </div>
              <div style={{ fontSize: "2rem", fontWeight: 800, color: "var(--navy)", marginTop: "4px" }}>
                {pendingRx.length}
              </div>
              <div style={{ fontSize: "0.75rem", color: "var(--text-muted)", marginTop: "4px" }}>
                In pharmacy dispensing queue
              </div>
            </div>
          </div>

          {/* Quick Management Links */}
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))", gap: "1.25rem", marginBottom: "2rem" }}>
            <div
              className="panel"
              onClick={() => navigate("/simulation")}
              style={{ cursor: "pointer", transition: "all 0.15s" }}
              onMouseEnter={(e) => (e.currentTarget.style.borderColor = "var(--teal)")}
              onMouseLeave={(e) => (e.currentTarget.style.borderColor = "var(--border)")}
            >
              <h3 style={{ fontSize: "1rem", fontWeight: 700, color: "var(--navy)", margin: "0 0 0.35rem 0" }}>
                📊 Scheduler Simulation (FCFS vs. Priority)
              </h3>
              <p style={{ fontSize: "0.82rem", color: "var(--text-muted)", margin: "0 0 1rem 0", lineHeight: 1.5 }}>
                Run Poisson synthetic arrivals to simulate waiting time reduction and analyze 90th percentile wait times.
              </p>
              <span style={{ fontSize: "0.78rem", fontWeight: 600, color: "var(--teal)" }}>Open Simulation Studio →</span>
            </div>

            <div
              className="panel"
              onClick={() => navigate("/beds")}
              style={{ cursor: "pointer", transition: "all 0.15s" }}
              onMouseEnter={(e) => (e.currentTarget.style.borderColor = "var(--teal)")}
              onMouseLeave={(e) => (e.currentTarget.style.borderColor = "var(--border)")}
            >
              <h3 style={{ fontSize: "1rem", fontWeight: 700, color: "var(--navy)", margin: "0 0 0.35rem 0" }}>
                🛏️ Hospital Bed Management
              </h3>
              <p style={{ fontSize: "0.82rem", color: "var(--text-muted)", margin: "0 0 1rem 0", lineHeight: 1.5 }}>
                Configure hospital wards, monitor real-time bed allocations, and prevent double-booking conflicts.
              </p>
              <span style={{ fontSize: "0.78rem", fontWeight: 600, color: "var(--teal)" }}>Manage Bed Grid →</span>
            </div>

            <div
              className="panel"
              onClick={() => navigate("/queue")}
              style={{ cursor: "pointer", transition: "all 0.15s" }}
              onMouseEnter={(e) => (e.currentTarget.style.borderColor = "var(--teal)")}
              onMouseLeave={(e) => (e.currentTarget.style.borderColor = "var(--border)")}
            >
              <h3 style={{ fontSize: "1rem", fontWeight: 700, color: "var(--navy)", margin: "0 0 0.35rem 0" }}>
                📋 Live Outpatient Queue
              </h3>
              <p style={{ fontSize: "0.82rem", color: "var(--text-muted)", margin: "0 0 1rem 0", lineHeight: 1.5 }}>
                Monitor dynamic ranking of waiting outpatients ordered by urgency and elapsed waiting minutes.
              </p>
              <span style={{ fontSize: "0.78rem", fontWeight: 600, color: "var(--teal)" }}>View Live Queue →</span>
            </div>
          </div>

          {/* Staff Accounts Management Section */}
          <div className="panel" style={{ marginBottom: "2rem" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1rem" }}>
              <div>
                <h3 className="panel-title" style={{ margin: 0 }}>Staff Directory & Account Management</h3>
                <p className="panel-subtitle" style={{ margin: "0.25rem 0 0 0" }}>
                  Manage multi-user accounts across all clinical and administrative roles. Passwords are encrypted on creation.
                </p>
              </div>
              <button
                type="button"
                className="btn btn-outline"
                onClick={() => {
                  setShowStaffModal(true);
                  setStaffError("");
                  setStaffSuccess("");
                }}
                style={{ fontSize: "0.8rem", padding: "0.4rem 0.8rem" }}
              >
                + Add Staff Member
              </button>
            </div>

            {staffList.length === 0 ? (
              <p style={{ color: "var(--text-muted)", fontSize: "0.85rem", margin: 0 }}>
                No staff records retrieved.
              </p>
            ) : (
              <div style={{ overflowX: "auto" }}>
                <table className="table" style={{ width: "100%", textAlign: "left", fontSize: "0.84rem" }}>
                  <thead>
                    <tr>
                      <th style={{ padding: "0.6rem 0.75rem", borderBottom: "1px solid var(--border)" }}>Staff Name</th>
                      <th style={{ padding: "0.6rem 0.75rem", borderBottom: "1px solid var(--border)" }}>Email / Username</th>
                      <th style={{ padding: "0.6rem 0.75rem", borderBottom: "1px solid var(--border)" }}>Assigned Role</th>
                      <th style={{ padding: "0.6rem 0.75rem", borderBottom: "1px solid var(--border)" }}>Account Status</th>
                      <th style={{ padding: "0.6rem 0.75rem", borderBottom: "1px solid var(--border)" }}>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {staffList.map((user) => (
                      <tr key={user.id}>
                        <td style={{ padding: "0.65rem 0.75rem", fontWeight: 600, color: "var(--ink)" }}>
                          {user.name}
                        </td>
                        <td style={{ padding: "0.65rem 0.75rem", color: "var(--text-muted)", fontFamily: "monospace" }}>
                          {user.email}
                        </td>
                        <td style={{ padding: "0.65rem 0.75rem" }}>
                          <span
                            style={{
                              display: "inline-block",
                              padding: "0.2rem 0.55rem",
                              borderRadius: "3px",
                              fontSize: "0.72rem",
                              fontWeight: 700,
                              letterSpacing: "0.03em",
                              backgroundColor:
                                user.role === "ADMIN"
                                  ? "#FEE2E2"
                                  : user.role === "DOCTOR"
                                  ? "#E0E7FF"
                                  : user.role === "LAB_TECH"
                                  ? "#FEF3C7"
                                  : user.role === "PHARMACIST"
                                  ? "#F3E8FF"
                                  : "#DCFCE7",
                              color:
                                user.role === "ADMIN"
                                  ? "#991B1B"
                                  : user.role === "DOCTOR"
                                  ? "#3730A3"
                                  : user.role === "LAB_TECH"
                                  ? "#92400E"
                                  : user.role === "PHARMACIST"
                                  ? "#6B21A8"
                                  : "#166534",
                            }}
                          >
                            {user.role}
                          </span>
                        </td>
                        <td style={{ padding: "0.65rem 0.75rem", color: "var(--teal)", fontWeight: 600 }}>
                          Active
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      )}

      {/* Create Staff Modal */}
      {showStaffModal && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            backgroundColor: "rgba(32, 43, 42, 0.45)",
            backdropFilter: "blur(2px)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 1000,
            padding: "1rem",
          }}
          onClick={(e) => {
            if (e.target === e.currentTarget && !staffSubmitting) {
              setShowStaffModal(false);
            }
          }}
        >
          <div
            style={{
              backgroundColor: "#FFFFFF",
              borderRadius: "4px",
              boxShadow: "0 10px 25px rgba(0,0,0,0.15)",
              border: "1px solid var(--border)",
              maxWidth: "480px",
              width: "100%",
              padding: "1.75rem",
            }}
          >
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1rem" }}>
              <h3 style={{ margin: 0, fontSize: "1.15rem", fontWeight: 700, color: "var(--ink)", fontFamily: "var(--font-heading)" }}>
                Create Staff Account
              </h3>
              <button
                type="button"
                onClick={() => setShowStaffModal(false)}
                disabled={staffSubmitting}
                style={{
                  background: "transparent",
                  border: "none",
                  fontSize: "1.2rem",
                  cursor: "pointer",
                  color: "var(--ink-muted)",
                }}
              >
                ✕
              </button>
            </div>

            <p style={{ fontSize: "0.82rem", color: "var(--text-muted)", margin: "0 0 1.25rem 0" }}>
              Authorize and provision individual credentials for clinical or administrative personnel.
            </p>

            {staffError && <div className="alert alert-danger" style={{ marginBottom: "1rem" }}>{staffError}</div>}
            {staffSuccess && <div className="alert alert-success" style={{ marginBottom: "1rem" }}>{staffSuccess}</div>}

            <form onSubmit={handleCreateStaff}>
              <div className="form-group" style={{ marginBottom: "0.9rem" }}>
                <label className="form-label" style={{ fontSize: "0.8rem" }}>Full Name *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Dr. Priya Sharma"
                  className="form-control"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  disabled={staffSubmitting}
                />
              </div>

              <div className="form-group" style={{ marginBottom: "0.9rem" }}>
                <label className="form-label" style={{ fontSize: "0.8rem" }}>Email / Sign-in Username *</label>
                <input
                  type="email"
                  required
                  placeholder="e.g. priya.sharma@patientflow.local"
                  className="form-control"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  disabled={staffSubmitting}
                />
              </div>

              <div className="form-group" style={{ marginBottom: "0.9rem" }}>
                <label className="form-label" style={{ fontSize: "0.8rem" }}>Assigned Role *</label>
                <select
                  className="form-control"
                  value={role}
                  onChange={(e) => setRole(e.target.value)}
                  disabled={staffSubmitting}
                >
                  {ROLES.map((r) => (
                    <option key={r.value} value={r.value}>
                      {r.label}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group" style={{ marginBottom: "0.9rem" }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline" }}>
                  <label className="form-label" style={{ fontSize: "0.8rem" }}>Password (min. 6 characters) *</label>
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    style={{
                      background: "none",
                      border: "none",
                      fontSize: "0.74rem",
                      color: "var(--teal)",
                      cursor: "pointer",
                      padding: 0,
                    }}
                  >
                    {showPassword ? "Hide password" : "Show password"}
                  </button>
                </div>
                <input
                  type={showPassword ? "text" : "password"}
                  required
                  placeholder="••••••••"
                  className="form-control"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  disabled={staffSubmitting}
                />
              </div>

              <div className="form-group" style={{ marginBottom: "1.25rem" }}>
                <label className="form-label" style={{ fontSize: "0.8rem" }}>Confirm Password *</label>
                <input
                  type={showPassword ? "text" : "password"}
                  required
                  placeholder="••••••••"
                  className="form-control"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  disabled={staffSubmitting}
                />
              </div>

              <div style={{ display: "flex", justifyContent: "flex-end", gap: "0.75rem" }}>
                <button
                  type="button"
                  className="btn btn-outline"
                  onClick={() => setShowStaffModal(false)}
                  disabled={staffSubmitting}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={staffSubmitting}
                >
                  {staffSubmitting ? "Creating Account…" : "Create Account"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </AppShell>
  );
}
