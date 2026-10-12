import AppShell from "../common/AppShell";

export default function ProfilePage() {
  const name = localStorage.getItem("name") || "Hospital Staff";
  const role = localStorage.getItem("role") || "STAFF";

  return (
    <AppShell title="Staff Profile & Credentials">
      <div style={{ maxWidth: "700px" }}>
        <div className="card" style={{ padding: "1.5rem" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "1rem", marginBottom: "1.5rem", paddingBottom: "1rem", borderBottom: "1px solid var(--rule)" }}>
            <div
              style={{
                width: "48px",
                height: "48px",
                borderRadius: "var(--radius-xs)",
                backgroundColor: "var(--petrol)",
                color: "#FFFFFF",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                fontFamily: "var(--font-heading)",
                fontSize: "1.25rem",
                fontWeight: 700,
              }}
            >
              {name.charAt(0).toUpperCase()}
            </div>
            <div>
              <h2 style={{ margin: 0, fontSize: "1.15rem", fontFamily: "var(--font-heading)" }}>{name}</h2>
              <span className="badge badge-teal" style={{ marginTop: "4px" }}>{role}</span>
            </div>
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "140px 1fr", gap: "0.75rem 1rem", fontSize: "0.85rem", marginBottom: "1.5rem" }}>
            <div style={{ color: "var(--ink-muted)", fontWeight: 600 }}>Operating Station:</div>
            <div>FlowAlloc OPD Ward Terminal</div>

            <div style={{ color: "var(--ink-muted)", fontWeight: 600 }}>Active Role:</div>
            <div><code>{role}</code></div>

            <div style={{ color: "var(--ink-muted)", fontWeight: 600 }}>Session Type:</div>
            <div>Clinical Duty Session</div>
          </div>

          <div style={{ background: "var(--paper)", border: "1px solid var(--rule)", padding: "1rem", borderRadius: "var(--radius-xs)", fontSize: "0.82rem", color: "var(--ink-muted)" }}>
            <strong style={{ color: "var(--ink)" }}>Clinical Operations Note:</strong> Role privileges and station clearances are managed centrally by the OPD Administrator. To request credential rotation or schedule reassignments, contact the medical administration desk.
          </div>
        </div>
      </div>
    </AppShell>
  );
}
