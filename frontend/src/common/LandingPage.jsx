import { useState } from "react";
import { useNavigate } from "react-router-dom";
import heroClinicianImg from "../assets/Focused Clinician in Modern Hospital Ward.png";

const WORKSPACES = [
  {
    key: "doctor",
    path: "/login/doctor",
    title: "Doctor",
    code: "DOC-OPD",
    desc: "Call prioritized outpatients, assess vitals, record diagnostic findings, order lab panels, and prescribe medications.",
  },
  {
    key: "admin",
    path: "/login/admin",
    title: "Administrator",
    code: "ADM-OPS",
    desc: "Oversee hospital bed capacity, provision staff accounts, analyze triage wait times, and run discrete-event simulations.",
  },
  {
    key: "lab",
    path: "/login/lab",
    title: "Lab Assistant",
    code: "LAB-DX",
    desc: "Process pending diagnostic specimens, enter pathology test results, and clear clinical investigation holds.",
  },
  {
    key: "pharmacy",
    path: "/login/pharmacy",
    title: "Pharmacist",
    code: "RX-DISP",
    desc: "Verify physician prescription orders, review clinical dosage schedules, and dispense outpatient pharmaceuticals.",
  },
  {
    key: "receptionist",
    path: "/login/receptionist",
    title: "Receptionist",
    code: "REC-TRIAGE",
    desc: "Register incoming patient arrivals, assign standardized urgency triage scores, and route into live doctor queues.",
  },
];

export default function LandingPage() {
  const navigate = useNavigate();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const scrollToSection = (id) => {
    setMobileMenuOpen(false);
    const element = document.getElementById(id);
    if (element) {
      element.scrollIntoView({ behavior: "smooth" });
    }
  };

  return (
    <div style={{ minHeight: "100vh", backgroundColor: "var(--paper)", display: "flex", flexDirection: "column", color: "var(--ink)" }}>
      {/* ─── Compact Header & Navigation ──────────────────────────────────── */}
      <header
        style={{
          position: "sticky",
          top: 0,
          zIndex: 100,
          backgroundColor: "rgba(255, 255, 255, 0.96)",
          backdropFilter: "blur(8px)",
          borderBottom: "1px solid var(--rule)",
          padding: "0.85rem 2rem",
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: "10px", cursor: "pointer" }} onClick={() => window.scrollTo({ top: 0, behavior: "smooth" })}>
          {/* Original FlowAlloc Logo Mark */}
          <div
            style={{
              width: "32px",
              height: "32px",
              backgroundColor: "var(--petrol)",
              borderRadius: "4px",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              boxShadow: "0 2px 5px rgba(23, 107, 99, 0.2)",
            }}
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#FFFFFF" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2" />
              <rect x="8" y="2" width="8" height="4" rx="1" ry="1" />
              <path d="M12 11h4" />
              <path d="M12 16h4" />
              <path d="M8 11h.01" />
              <path d="M8 16h.01" />
            </svg>
          </div>
          <div>
            <div style={{ fontWeight: 800, fontSize: "1.15rem", letterSpacing: "-0.02em", color: "var(--ink)", fontFamily: "var(--font-heading)" }}>
              FlowAlloc
            </div>
            <div style={{ fontSize: "0.68rem", color: "var(--ink-muted)", textTransform: "uppercase", letterSpacing: "0.06em", fontWeight: 600 }}>
              Hospital Operations
            </div>
          </div>
        </div>

        {/* Desktop Nav Links */}
        <nav className="desktop-nav" style={{ display: "flex", alignItems: "center", gap: "2rem" }}>
          <button
            type="button"
            onClick={() => window.scrollTo({ top: 0, behavior: "smooth" })}
            style={{ background: "none", border: "none", fontSize: "0.88rem", fontWeight: 600, color: "var(--ink)", cursor: "pointer" }}
          >
            Home
          </button>
          <button
            type="button"
            onClick={() => scrollToSection("patient-journey")}
            style={{ background: "none", border: "none", fontSize: "0.88rem", fontWeight: 600, color: "var(--ink-muted)", cursor: "pointer" }}
          >
            Patient Journey
          </button>
          <button
            type="button"
            onClick={() => scrollToSection("workspaces")}
            style={{ background: "none", border: "none", fontSize: "0.88rem", fontWeight: 600, color: "var(--ink-muted)", cursor: "pointer" }}
          >
            Workspaces
          </button>
          <button
            type="button"
            className="btn btn-primary"
            onClick={() => scrollToSection("workspaces")}
            style={{ fontSize: "0.84rem", padding: "0.5rem 1.1rem" }}
          >
            Access workspace →
          </button>
        </nav>

        {/* Mobile Nav Toggle */}
        <button
          type="button"
          className="mobile-nav-toggle"
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          style={{ background: "none", border: "1px solid var(--rule)", padding: "6px 10px", borderRadius: "4px", cursor: "pointer" }}
        >
          ☰
        </button>
      </header>

      {/* Mobile Nav Menu Dropdown */}
      {mobileMenuOpen && (
        <div style={{ backgroundColor: "#FFFFFF", borderBottom: "1px solid var(--rule)", padding: "1rem 2rem", display: "flex", flexDirection: "column", gap: "1rem" }}>
          <button
            type="button"
            onClick={() => { window.scrollTo({ top: 0, behavior: "smooth" }); setMobileMenuOpen(false); }}
            style={{ background: "none", border: "none", textAlign: "left", fontSize: "0.95rem", fontWeight: 600, color: "var(--ink)" }}
          >
            Home
          </button>
          <button
            type="button"
            onClick={() => scrollToSection("patient-journey")}
            style={{ background: "none", border: "none", textAlign: "left", fontSize: "0.95rem", fontWeight: 600, color: "var(--ink-muted)" }}
          >
            Patient Journey
          </button>
          <button
            type="button"
            onClick={() => scrollToSection("workspaces")}
            style={{ background: "none", border: "none", textAlign: "left", fontSize: "0.95rem", fontWeight: 600, color: "var(--ink-muted)" }}
          >
            Workspaces
          </button>
          <button
            type="button"
            className="btn btn-primary"
            onClick={() => scrollToSection("workspaces")}
            style={{ textAlign: "center" }}
          >
            Access workspace →
          </button>
        </div>
      )}

      {/* ─── Hero Section (Editorial Left-Text / Right-Photograph Split) ─── */}
      <section
        style={{
          minHeight: "calc(88vh - 70px)",
          display: "flex",
          alignItems: "center",
          padding: "3.5rem 2rem 4rem 2rem",
          maxWidth: "1280px",
          margin: "0 auto",
          width: "100%",
        }}
      >
        <div className="hero-split" style={{ display: "grid", gridTemplateColumns: "1.1fr 0.9fr", gap: "3.5rem", alignItems: "center", width: "100%" }}>
          {/* Left Column: Typography and CTAs */}
          <div className="hero-text-animate">
            <div
              style={{
                display: "inline-block",
                fontSize: "0.74rem",
                fontWeight: 700,
                color: "var(--petrol)",
                letterSpacing: "0.1em",
                textTransform: "uppercase",
                marginBottom: "1rem",
                background: "var(--petrol-light)",
                padding: "4px 10px",
                borderRadius: "3px",
                border: "1px solid var(--triage-3-border)",
              }}
            >
              HOSPITAL OPERATIONS, A STEP AHEAD
            </div>

            <h1
              style={{
                fontSize: "clamp(2.4rem, 4.5vw, 3.8rem)",
                fontWeight: 800,
                lineHeight: 1.1,
                color: "var(--ink)",
                letterSpacing: "-0.03em",
                fontFamily: "var(--font-heading)",
                margin: "0 0 1.5rem 0",
              }}
            >
              Every patient.
              <br />
              <span style={{ color: "var(--petrol)" }}>A clearer path.</span>
            </h1>

            <p
              style={{
                fontSize: "1.08rem",
                lineHeight: 1.6,
                color: "var(--ink-muted)",
                maxWidth: "540px",
                margin: "0 0 2.25rem 0",
              }}
            >
              Connecting patients, healthcare teams, and hospital resources through intelligent outpatient flow coordination.
            </p>

            <div style={{ display: "flex", flexWrap: "wrap", gap: "1rem", alignItems: "center", marginBottom: "3rem" }}>
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => scrollToSection("patient-journey")}
                style={{ fontSize: "0.95rem", padding: "0.75rem 1.6rem" }}
              >
                Explore FlowAlloc →
              </button>
              <button
                type="button"
                className="btn btn-outline"
                onClick={() => scrollToSection("workspaces")}
                style={{ fontSize: "0.95rem", padding: "0.75rem 1.6rem" }}
              >
                Choose your workspace ↓
              </button>
            </div>

            {/* Restrained Highlights (No fake statistics) */}
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(3, 1fr)",
                gap: "1.25rem",
                borderTop: "1px solid var(--rule)",
                paddingTop: "1.5rem",
              }}
            >
              <div>
                <div style={{ fontWeight: 700, fontSize: "0.88rem", color: "var(--ink)" }}>Clearer patient flow</div>
                <div style={{ fontSize: "0.75rem", color: "var(--ink-muted)", marginTop: "2px" }}>Dynamic triage priority</div>
              </div>
              <div>
                <div style={{ fontWeight: 700, fontSize: "0.88rem", color: "var(--ink)" }}>Coordinated resources</div>
                <div style={{ fontSize: "0.75rem", color: "var(--ink-muted)", marginTop: "2px" }}>Synchronized beds & desks</div>
              </div>
              <div>
                <div style={{ fontWeight: 700, fontSize: "0.88rem", color: "var(--ink)" }}>Connected care teams</div>
                <div style={{ fontSize: "0.75rem", color: "var(--ink-muted)", marginTop: "2px" }}>Real-time cross-station handoffs</div>
              </div>
            </div>
          </div>

          {/* Right Column: Realistic Clinical Photograph as Hero Background */}
          <div
            className="hero-image-animate"
            style={{
              position: "relative",
              width: "100%",
              height: "100%",
              minHeight: "440px",
              borderRadius: "12px",
              overflow: "hidden",
              boxShadow: "0 16px 36px rgba(32, 43, 42, 0.1)",
              border: "1px solid var(--rule)",
            }}
          >
            <div
              style={{
                width: "100%",
                height: "100%",
                minHeight: "440px",
                backgroundImage: `url("${heroClinicianImg}")`,
                backgroundSize: "cover",
                backgroundPosition: "center 25%",
                backgroundRepeat: "no-repeat",
              }}
              role="img"
              aria-label="Doctor reviewing clinical tablet in modern hospital outpatient setting"
            />
          </div>
        </div>
      </section>

      {/* ─── Patient Journey Section ────────────────────────────────────────── */}
      <section
        id="patient-journey"
        style={{
          backgroundColor: "#FFFFFF",
          borderTop: "1px solid var(--rule)",
          borderBottom: "1px solid var(--rule)",
          padding: "5rem 2rem",
        }}
      >
        <div style={{ maxWidth: "1180px", margin: "0 auto", width: "100%" }}>
          <div style={{ textAlign: "center", marginBottom: "3.5rem" }}>
            <div style={{ fontSize: "0.74rem", fontWeight: 700, color: "var(--petrol)", letterSpacing: "0.08em", textTransform: "uppercase", marginBottom: "0.4rem" }}>
              Operational Architecture
            </div>
            <h2
              style={{
                fontSize: "2.1rem",
                fontWeight: 700,
                color: "var(--ink)",
                margin: "0 0 0.6rem 0",
                fontFamily: "var(--font-heading)",
                letterSpacing: "-0.02em",
              }}
            >
              One connected patient journey.
            </h2>
            <p style={{ fontSize: "0.95rem", color: "var(--ink-muted)", maxWidth: "600px", margin: "0 auto", lineHeight: 1.5 }}>
              From arrival to consultation and onward services, FlowAlloc helps teams coordinate the next step.
            </p>
          </div>

          {/* Clean Custom SVG/CSS Visual Flow: Reception → Consultation → Labs / Pharmacy → Discharge */}
          <div className="journey-flow-grid" style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: "1.5rem", position: "relative" }}>
            {/* Step 1: Reception */}
            <div
              className="journey-step-card"
              style={{
                backgroundColor: "var(--paper)",
                border: "1px solid var(--rule)",
                borderRadius: "6px",
                padding: "1.6rem 1.3rem",
                display: "flex",
                flexDirection: "column",
                position: "relative",
              }}
            >
              <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "1rem" }}>
                <div
                  style={{
                    width: "36px",
                    height: "36px",
                    borderRadius: "50%",
                    backgroundColor: "var(--petrol-light)",
                    border: "1px solid var(--triage-3-border)",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    color: "var(--petrol)",
                    fontWeight: 700,
                    fontSize: "0.9rem",
                  }}
                >
                  1
                </div>
                <div style={{ fontWeight: 700, fontSize: "1.05rem", color: "var(--ink)" }}>Reception</div>
              </div>
              <p style={{ fontSize: "0.82rem", color: "var(--ink-muted)", lineHeight: 1.5, margin: "0 0 0.8rem 0" }}>
                Patient intake and rapid triage scoring based on urgency criteria. Automatically queues into prioritized OPD waiting ledger.
              </p>
              <div style={{ fontSize: "0.72rem", color: "var(--petrol)", fontWeight: 700, marginTop: "auto" }}>
                Triage & Check-in
              </div>
            </div>

            {/* Step 2: Consultation */}
            <div
              className="journey-step-card"
              style={{
                backgroundColor: "var(--paper)",
                border: "1px solid var(--rule)",
                borderRadius: "6px",
                padding: "1.6rem 1.3rem",
                display: "flex",
                flexDirection: "column",
                position: "relative",
              }}
            >
              <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "1rem" }}>
                <div
                  style={{
                    width: "36px",
                    height: "36px",
                    borderRadius: "50%",
                    backgroundColor: "var(--petrol-light)",
                    border: "1px solid var(--triage-3-border)",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    color: "var(--petrol)",
                    fontWeight: 700,
                    fontSize: "0.9rem",
                  }}
                >
                  2
                </div>
                <div style={{ fontWeight: 700, fontSize: "1.05rem", color: "var(--ink)" }}>Consultation</div>
              </div>
              <p style={{ fontSize: "0.82rem", color: "var(--ink-muted)", lineHeight: 1.5, margin: "0 0 0.8rem 0" }}>
                Doctor calls the highest priority patient, records clinical examination, assigns ward beds if required, and orders diagnostics.
              </p>
              <div style={{ fontSize: "0.72rem", color: "var(--petrol)", fontWeight: 700, marginTop: "auto" }}>
                Physician Desk
              </div>
            </div>

            {/* Step 3: Laboratory / Pharmacy (Conditional Path) */}
            <div
              className="journey-step-card"
              style={{
                backgroundColor: "var(--paper)",
                border: "1px solid var(--rule)",
                borderRadius: "6px",
                padding: "1.6rem 1.3rem",
                display: "flex",
                flexDirection: "column",
                position: "relative",
              }}
            >
              <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "1rem" }}>
                <div
                  style={{
                    width: "36px",
                    height: "36px",
                    borderRadius: "50%",
                    backgroundColor: "var(--petrol-light)",
                    border: "1px solid var(--triage-3-border)",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    color: "var(--petrol)",
                    fontWeight: 700,
                    fontSize: "0.9rem",
                  }}
                >
                  3
                </div>
                <div>
                  <div style={{ fontWeight: 700, fontSize: "1.05rem", color: "var(--ink)" }}>Laboratory / Pharmacy</div>
                  <div style={{ fontSize: "0.68rem", color: "var(--ink-muted)", fontStyle: "italic" }}>Conditional path</div>
                </div>
              </div>
              <p style={{ fontSize: "0.82rem", color: "var(--ink-muted)", lineHeight: 1.5, margin: "0 0 0.8rem 0" }}>
                Parallel diagnostic tests processed by laboratory technicians and medications fulfilled by dispensary pharmacists.
              </p>
              <div style={{ fontSize: "0.72rem", color: "var(--petrol)", fontWeight: 700, marginTop: "auto" }}>
                Specimen & Prescriptions
              </div>
            </div>

            {/* Step 4: Discharge */}
            <div
              className="journey-step-card"
              style={{
                backgroundColor: "var(--paper)",
                border: "1px solid var(--rule)",
                borderRadius: "6px",
                padding: "1.6rem 1.3rem",
                display: "flex",
                flexDirection: "column",
                position: "relative",
              }}
            >
              <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "1rem" }}>
                <div
                  style={{
                    width: "36px",
                    height: "36px",
                    borderRadius: "50%",
                    backgroundColor: "var(--petrol-light)",
                    border: "1px solid var(--triage-3-border)",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    color: "var(--petrol)",
                    fontWeight: 700,
                    fontSize: "0.9rem",
                  }}
                >
                  4
                </div>
                <div style={{ fontWeight: 700, fontSize: "1.05rem", color: "var(--ink)" }}>Discharge</div>
              </div>
              <p style={{ fontSize: "0.82rem", color: "var(--ink-muted)", lineHeight: 1.5, margin: "0 0 0.8rem 0" }}>
                Clinical visit summary finalized upon completion of investigations, releasing assigned beds back to the available hospital pool.
              </p>
              <div style={{ fontSize: "0.72rem", color: "var(--petrol)", fontWeight: 700, marginTop: "auto" }}>
                Summary & Resource Release
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ─── Role-Based Workspace Selector ─────────────────────────────────── */}
      <section
        id="workspaces"
        style={{
          padding: "5rem 2rem 6rem 2rem",
          maxWidth: "1180px",
          margin: "0 auto",
          width: "100%",
        }}
      >
        <div style={{ marginBottom: "2.75rem", borderBottom: "1px solid var(--rule)", paddingBottom: "1.5rem" }}>
          <div style={{ fontSize: "0.74rem", fontWeight: 700, color: "var(--petrol)", letterSpacing: "0.08em", textTransform: "uppercase", marginBottom: "0.4rem" }}>
            Operational Directory
          </div>
          <h2
            style={{
              fontSize: "2.1rem",
              fontWeight: 700,
              color: "var(--ink)",
              margin: "0 0 0.5rem 0",
              fontFamily: "var(--font-heading)",
              letterSpacing: "-0.02em",
            }}
          >
            Choose your workspace.
          </h2>
          <p style={{ fontSize: "0.95rem", color: "var(--ink-muted)", margin: 0, lineHeight: 1.5 }}>
            Select your role to continue to your sign-in page.
          </p>
        </div>

        {/* Structured 5-Workspace Directory Grid */}
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))", gap: "1.25rem" }}>
          {WORKSPACES.map((ws) => (
            <div
              key={ws.key}
              className="workspace-card"
              onClick={() => navigate(ws.path)}
              style={{
                backgroundColor: "#FFFFFF",
                border: "1px solid var(--rule)",
                borderRadius: "6px",
                padding: "1.5rem 1.6rem",
                cursor: "pointer",
                display: "flex",
                flexDirection: "column",
                justifyContent: "space-between",
              }}
            >
              <div>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1rem" }}>
                  <span
                    style={{
                      fontSize: "0.72rem",
                      fontWeight: 700,
                      fontFamily: "var(--font-mono)",
                      color: "var(--petrol)",
                      background: "var(--petrol-light)",
                      border: "1px solid var(--triage-3-border)",
                      padding: "2px 8px",
                      borderRadius: "3px",
                    }}
                  >
                    {ws.code}
                  </span>
                  <span style={{ fontSize: "0.8rem", color: "var(--ink-muted)", fontWeight: 600 }}>
                    Sign in →
                  </span>
                </div>
                <h3 style={{ fontSize: "1.2rem", fontWeight: 700, color: "var(--ink)", margin: "0 0 0.5rem 0", fontFamily: "var(--font-heading)" }}>
                  {ws.title}
                </h3>
                <p style={{ fontSize: "0.85rem", color: "var(--ink-muted)", lineHeight: 1.5, margin: 0 }}>
                  {ws.desc}
                </p>
              </div>

              <div style={{ borderTop: "1px solid var(--rule)", marginTop: "1.5rem", paddingTop: "0.85rem", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <span style={{ fontSize: "0.75rem", color: "var(--ink-muted)" }}>Workspace access</span>
                <span style={{ fontSize: "0.82rem", fontWeight: 700, color: "var(--petrol)" }}>Launch Desk →</span>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* ─── Footer (No Disclaimers) ────────────────────────────────────────── */}
      <footer
        style={{
          marginTop: "auto",
          backgroundColor: "#1B2423",
          color: "#ECEFEA",
          borderTop: "1px solid #101B19",
          padding: "3rem 2rem 2.5rem 2rem",
        }}
      >
        <div style={{ maxWidth: "1180px", margin: "0 auto", width: "100%" }}>
          <div style={{ display: "flex", flexWrap: "wrap", justifyContent: "space-between", alignItems: "flex-start", gap: "2rem", marginBottom: "2.5rem" }}>
            <div>
              <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "0.5rem" }}>
                <div
                  style={{
                    width: "26px",
                    height: "26px",
                    backgroundColor: "var(--petrol)",
                    borderRadius: "3px",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                  }}
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#FFFFFF" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2" />
                    <rect x="8" y="2" width="8" height="4" rx="1" ry="1" />
                    <path d="M12 11h4" />
                    <path d="M12 16h4" />
                  </svg>
                </div>
                <span style={{ fontFamily: "var(--font-heading)", fontSize: "1.15rem", fontWeight: 700, color: "#FFFFFF" }}>
                  FlowAlloc
                </span>
              </div>
              <p style={{ fontSize: "0.86rem", color: "#B9C4C0", margin: 0, fontWeight: 500 }}>
                Right patient. Right resource. Right time.
              </p>
            </div>

            <div style={{ display: "flex", gap: "2.5rem", alignItems: "center" }}>
              <button
                type="button"
                onClick={() => window.scrollTo({ top: 0, behavior: "smooth" })}
                style={{ background: "none", border: "none", color: "#CAD2CE", fontSize: "0.85rem", cursor: "pointer", padding: 0 }}
              >
                Home
              </button>
              <button
                type="button"
                onClick={() => scrollToSection("patient-journey")}
                style={{ background: "none", border: "none", color: "#CAD2CE", fontSize: "0.85rem", cursor: "pointer", padding: 0 }}
              >
                Patient Journey
              </button>
              <button
                type="button"
                onClick={() => scrollToSection("workspaces")}
                style={{ background: "none", border: "none", color: "#CAD2CE", fontSize: "0.85rem", cursor: "pointer", padding: 0 }}
              >
                Workspaces
              </button>
            </div>
          </div>

          <div style={{ borderTop: "1px solid #283634", paddingTop: "1.5rem", display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "1rem" }}>
            <span style={{ fontSize: "0.78rem", color: "#8E9E9B" }}>
              Dynamic Patient Flow and Resource Optimization System
            </span>
            <span style={{ fontSize: "0.78rem", color: "#8E9E9B" }}>
              © 2026 FlowAlloc
            </span>
          </div>
        </div>
      </footer>
    </div>
  );
}
