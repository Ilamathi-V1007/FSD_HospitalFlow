import PageShell from "../common/PageShell";

export default function DoctorDashboard() {
  return (
    <PageShell
      title="Doctor Dashboard"
      objective="Let doctors manage their patient load and trigger downstream actions (labs, beds)."
      functions={[
        "View list of currently assigned patients",
        "Call next patient from the queue",
        "View patient symptoms/urgency/history before consultation",
        "Mark consultation complete",
        "Request a lab test",
        "Assign patient to a bed if admission is needed",
        "Toggle own availability status",
      ]}
    />
  );
}
