import PageShell from "./PageShell";

export default function AdminDashboard() {
  return (
    <PageShell
      title="Admin Dashboard"
      objective="Give administrators full oversight and control of the system."
      functions={[
        "Create / edit / deactivate staff accounts, assign roles",
        "View real-time bed availability across the hospital",
        "View system-wide active patient count",
        "Access the Analytics Dashboard",
      ]}
    />
  );
}
