import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import api from "../common/api";

const ROLE_HOME = {
  ADMIN: "/admin",
  DOCTOR: "/doctor",
  RECEPTIONIST: "/receptionist",
  LAB_TECH: "/lab",
  PHARMACIST: "/pharmacy",
};

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  async function handleSubmit(e) {
    e.preventDefault();
    setLoading(true);
    setError("");
    try {
      const res = await api.post("/auth/login", { email: email.trim(), password });
      const { token, role, name } = res.data;
      localStorage.setItem("token", token);
      localStorage.setItem("role", role);
      localStorage.setItem("name", name);
      navigate(ROLE_HOME[role] || "/admin");
    } catch (err) {
      setError(err.response?.data?.error || "Authentication failed. Check your credentials.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={{ minHeight: "100vh", backgroundColor: "var(--paper)", display: "flex", flexDirection: "column" }}>
      <header style={{ padding: "1rem 2rem", borderBottom: "1px solid var(--rule)", backgroundColor: "var(--surface)", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <Link to="/" style={{ textDecoration: "none", display: "flex", alignItems: "center", gap: "10px", fontWeight: 700, fontSize: "1.05rem", color: "var(--ink)", fontFamily: "var(--font-heading)" }}>
          <span style={{ width: "24px", height: "24px", backgroundColor: "var(--petrol)", color: "#FFFFFF", display: "inline-flex", alignItems: "center", justifyContent: "center", borderRadius: "var(--radius-xs)", fontSize: "0.75rem", fontFamily: "var(--font-heading)" }}>FA</span>
          FlowAlloc
        </Link>
        <span className="demo-badge">Ward Terminal Duty Verification</span>
      </header>

      <main style={{ flex: 1, display: "flex", alignItems: "center", justifyContent: "center", padding: "2rem" }}>
        <div style={{ width: "100%", maxWidth: "390px", backgroundColor: "var(--surface)", border: "1px solid var(--rule)", borderRadius: "var(--radius-sm)", padding: "2rem" }}>
          <div style={{ marginBottom: "1.5rem" }}>
            <div style={{ fontSize: "0.72rem", fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.06em", color: "var(--petrol)", marginBottom: "4px" }}>
              Staff Access
            </div>
            <h2 style={{ fontSize: "1.25rem", fontWeight: 700, color: "var(--ink)", margin: "0 0 0.35rem 0", fontFamily: "var(--font-heading)" }}>
              Hospital Sign In
            </h2>
            <p style={{ fontSize: "0.82rem", color: "var(--ink-muted)", margin: 0 }}>
              Authenticate with your assigned hospital credentials.
            </p>
          </div>

          {error && <div className="alert alert-danger">{error}</div>}

          {/* Quick Demo Credentials Bar */}
          <div style={{ marginBottom: "1.25rem", padding: "0.75rem", backgroundColor: "var(--surface-muted, #f8f9fa)", borderRadius: "var(--radius-xs, 4px)", border: "1px solid var(--rule, #e9ecef)" }}>
            <div style={{ fontSize: "0.72rem", fontWeight: 700, color: "var(--ink-muted, #6c757d)", textTransform: "uppercase", letterSpacing: "0.05em", marginBottom: "0.5rem" }}>
              Quick Demo Fill (Password: Demo@1234)
            </div>
            <div style={{ display: "flex", flexWrap: "wrap", gap: "6px" }}>
              {[
                { label: "Admin", email: "admin@patientflow.local" },
                { label: "Doctor", email: "doctor1@patientflow.local" },
                { label: "Reception", email: "reception@patientflow.local" },
                { label: "Lab", email: "lab@patientflow.local" },
                { label: "Pharmacy", email: "pharmacy@patientflow.local" },
              ].map((role) => (
                <button
                  key={role.label}
                  type="button"
                  onClick={() => {
                    setEmail(role.email);
                    setPassword("Demo@1234");
                    setError("");
                  }}
                  style={{
                    fontSize: "0.74rem",
                    padding: "3px 8px",
                    borderRadius: "3px",
                    border: "1px solid var(--petrol, #176B63)",
                    backgroundColor: email === role.email ? "var(--petrol, #176B63)" : "#FFFFFF",
                    color: email === role.email ? "#FFFFFF" : "var(--petrol, #176B63)",
                    cursor: "pointer",
                    fontWeight: 600,
                  }}
                >
                  {role.label}
                </button>
              ))}
            </div>
          </div>

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="form-label">Email Address or Handle</label>
              <input
                type="text"
                required
                className="form-control"
                placeholder="e.g. admin, doctor, or doctor1@patientflow.local"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>

            <div className="form-group">
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <label className="form-label">Password</label>
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
                placeholder="Demo@1234"
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
              {loading ? "Authenticating…" : "Sign In"}
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
