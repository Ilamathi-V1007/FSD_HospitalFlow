import PageShell from "../common/PageShell";

export default function LabDashboard() {
  return (
    <PageShell
      title="Lab Technician Dashboard"
      objective="Manage the lifecycle of lab test requests."
      functions={[
        "View pending lab test requests",
        "Enter or upload results",
        "Mark a request as complete",
        "Trigger notification to the requesting doctor",
      ]}
    />
  );
}
