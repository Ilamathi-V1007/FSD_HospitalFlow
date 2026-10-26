import { useState, useEffect } from "react";
import { useNavigate, useParams, Link } from "react-router-dom";
import api from "../common/api";

const ROLE_META = {
  receptionist: {
    roleName: "RECEPTIONIST",
    title: "Receptionist Sign In",
    desc: "Intake coordination, patient registration, and triage grading.",
    defaultEmail: "reception@patientflow.local",
    home: "/receptionist",
  },
  doctor: {
    roleName: "DOCTOR",
    title: "Doctor Sign In",
    desc: "Clinical consults, orders, and patient bed allocations.",
    defaultEmail: "doctor1@patientflow.local",
    home: "/doctor",
  },
  lab: {
    roleName: "LAB_TECH",
    title: "Laboratory Sign In",
    desc: "Diagnostic tests, specimen queues, and result reporting.",
    defaultEmail: "lab@patientflow.local",
    home: "/lab",
  },
  pharmacy: {
    roleName: "PHARMACIST",
    title: "Pharmacy Sign In",
    desc: "Prescription fulfillment, dosage validation, and dispensing.",
    defaultEmail: "pharmacy@patientflow.local",
    home: "/pharmacy",
  },
  admin: {
    roleName: "ADMIN",
    title: "Administrator Sign In",
    desc: "Hospital-wide operational control and simulation engine.",
    defaultEmail: "admin@patientflow.local",
    home: "/admin",
  },
};

export default function RoleLoginPage() {
  const { roleKey } = useParams();
  const navigate = useNavigate();
  const config = ROLE_META[roleKey] || ROLE_META.admin;

  const [email, setEmail] = useState(config.defaultEmail);
  const [password, setPassword] = useState("Demo@1234");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // Keep default email in sync when navigating between different role portals
  useEffect(() => {
    setEmail(config.defaultEmail);
    setPassword("Demo@1234");
    setError("");
  }, [roleKey, config.defaultEmail]);

  async function handleSubmit(e) {
    e.preventDefault();
    setLoading(true);
    setError("");

    try {
      const res = await api.post("/auth/login", { email: email.trim(), password });
      const { token, role, name } = res.data;

      // Verify that the authenticated role matches the workspace role
      if (role !== config.roleName) {
        setError(`Access denied: Your account role is ${role}, but you are logging into the ${config.title} portal.`);
        setLoading(false);
        return;
      }

      localStorage.setItem("token", token);
      localStorage.setItem("role", role);
      localStorage.setItem("name", name);
      navigate(config.home);
    } catch (err) {
      setError(err.response?.data?.error || "Authentication failed. Verify email and password.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={{ minHeight: "100vh", backgroundColor: "var(--bg)", display: "flex", flexDirection: "column" }}>
      {/* Header */}
      <header style={{ padding: "1rem 2rem", borderBottom: "1px solid var(--rule)", backgroundColor: "var(--surface)", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <Link to="/" style={{ textDecoration: "none", display: "flex", alignItems: "center", gap: "10px", fontWeight: 700, fontSize: "1.05rem", color: "var(--ink)", fontFamily: "var(--font-heading)" }}>
          <span style={{ width: "24px", height: "24px", backgroundColor: "var(--petrol)", color: "#FFFFFF", display: "inline-flex", alignItems: "center", justifyContent: "center", borderRadius: "var(--radius-xs)", fontSize: "0.75rem", fontFamily: "var(--font-heading)" }}>FA</span>
          FlowAlloc
        </Link>
        <span className="demo-badge">Ward Terminal Duty Verification</span>
      </header>

      {/* Main Login Card */}
      <main style={{ flex: 1, display: "flex", alignItems: "center", justifyContent: "center", padding: "2rem" }}>
        <div style={{ width: "100%", maxWidth: "390px", backgroundColor: "var(--surface)", border: "1px solid var(--rule)", borderRadius: "var(--radius-sm)", padding: "2rem" }}>
          <div style={{ marginBottom: "1.5rem" }}>
            <div style={{ fontSize: "0.72rem", fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.06em", color: "var(--petrol)", marginBottom: "4px" }}>
              Station Clearance
            </div>
            <h2 style={{ fontSize: "1.25rem", fontWeight: 700, color: "var(--ink)", margin: "0 0 0.35rem 0", fontFamily: "var(--font-heading)" }}>
              {config.title}
            </h2>
            <p style={{ fontSize: "0.82rem", color: "var(--ink-muted)", margin: 0, lineHeight: 1.45 }}>
              {config.desc}
            </p>
          </div>

          {error && <div className="alert alert-danger">{error}</div>}

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="form-label">Hospital Email or Handle</label>
              <input
                type="text"
                required
                className="form-control"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>

            <div className="form-group" style={{ position: "relative" }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <label className="form-label">Station Password</label>
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  style={{ background: "none", border: "none", color: "var(--petrol)", fontSize: "0.75rem", fontWeight: 600, cursor: "pointer", padding: 0 }}
                >
                  {showPassword ? "Hide" : "Show"}
                </button>
              </div>
              <input
                type={showPassword ? "text" : "password"}
                required
                className="form-control"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="btn btn-primary"
              style={{ width: "100%", marginTop: "1.25rem", padding: "0.6rem" }}
            >
              {loading ? "Authenticating…" : `Verify & Sign in (${config.roleName})`}
            </button>
          </form>

          <div style={{ marginTop: "1.5rem", paddingTop: "1.25rem", borderTop: "1px solid var(--rule)", textAlign: "center" }}>
            <Link to="/" style={{ textDecoration: "none", fontSize: "0.8rem", color: "var(--petrol)", fontWeight: 600 }}>
              ← Return to Station Directory
            </Link>
          </div>
        </div>
      </main>
    </div>
  );
}
