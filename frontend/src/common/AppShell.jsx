import { useNavigate, useLocation } from "react-router-dom";

export default function AppShell({ title, children }) {
  const navigate = useNavigate();
  const location = useLocation();
  const role = localStorage.getItem("role") || "";
  const name = localStorage.getItem("name") || "Staff Member";

  function handleLogout() {
    localStorage.clear();
    navigate("/");
  }

  // Role-specific navigation items
  const navItems = [
    ...(role === "ADMIN"
      ? [
          { label: "Dashboard", path: "/admin" },
          { label: "Live Queue", path: "/queue" },
          { label: "Bed Capacity", path: "/beds" },
          { label: "Scheduler Simulation", path: "/simulation" },
        ]
      : []),
    ...(role === "RECEPTIONIST"
      ? [
          { label: "Patient Intake", path: "/receptionist" },
          { label: "Live Queue", path: "/queue" },
          { label: "Discharge Desk", path: "/discharge" },
        ]
      : []),
    ...(role === "DOCTOR"
      ? [
          { label: "Consultation Desk", path: "/doctor" },
          { label: "Patient Queue", path: "/queue" },
          { label: "Hospital Beds", path: "/beds" },
          { label: "Discharge Desk", path: "/discharge" },
        ]
      : []),
    ...(role === "LAB_TECH"
      ? [{ label: "Laboratory Orders", path: "/lab" }]
      : []),
    ...(role === "PHARMACIST"
      ? [{ label: "Pharmacy Queue", path: "/pharmacy" }]
      : []),
  ];

  return (
    <div className="app-shell">
      {/* Sidebar Navigation */}
      <aside className="app-sidebar">
        <div className="sidebar-header">
          <div className="brand-title">
            <span style={{ width: "24px", height: "24px", backgroundColor: "var(--petrol)", color: "#FFFFFF", display: "inline-flex", alignItems: "center", justifyContent: "center", borderRadius: "var(--radius-xs)", fontSize: "0.75rem", fontFamily: "var(--font-heading)" }}>FA</span>
            FlowAlloc
          </div>
          <div className="brand-tagline">OPD Ward Board System</div>
        </div>

        <nav className="sidebar-nav">
          <div style={{ fontSize: "0.68rem", fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.08em", color: "rgba(247,246,241,0.4)", margin: "0.5rem 0.75rem 0.35rem" }}>
            Ward Station
          </div>
          {navItems.map((item) => (
            <button
              key={item.path}
              type="button"
              className={`nav-link ${location.pathname === item.path ? "active" : ""}`}
              onClick={() => navigate(item.path)}
            >
              <span>{item.label}</span>
            </button>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div style={{ marginBottom: "6px" }}>
            <span className="demo-badge">Demo Mode</span>
          </div>
          <div style={{ fontSize: "0.72rem", color: "rgba(255,255,255,0.5)" }}>
            FlowAlloc Core v1.1
          </div>
        </div>
      </aside>

      {/* Main Page Area */}
      <div className="app-main">
        {/* Top Navigation */}
        <header className="app-topbar">
          <div className="topbar-title">
            <span>{title}</span>
          </div>

          <div className="topbar-user">
            <span className="demo-badge">Synthetic Seed Active</span>
            <div className="user-identity">
              <strong>{name}</strong> · <span className="user-role-badge">{role}</span>
            </div>
            <button type="button" className="btn-logout" onClick={handleLogout}>
              Sign out
            </button>
          </div>
        </header>

        {/* Dynamic Page Content */}
        <main className="app-content">
          {children}
        </main>
      </div>
    </div>
  );
}
