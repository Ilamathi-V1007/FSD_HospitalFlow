import PageShell from "../common/PageShell";

export default function ReceptionistDashboard() {
  return (
    <PageShell
      title="Receptionist Dashboard"
      objective="Handle patient registration and check-in efficiently, feeding the queue accurately."
      functions={[
        "Register a new patient (name, age, contact, etc.)",
        "Search existing patients to avoid duplicate records",
        "Enter symptoms and select urgency level at check-in",
        "Push patient into the live queue",
        "View a patient's past visit history",
      ]}
    />
  );
}
