import PageShell from "../common/PageShell";

export default function BedStatusPage() {
  return (
    <PageShell
      title="Bed / Resource Status"
      objective="Prevent double-booking and give real-time visibility into bed occupancy."
      functions={[
        "Real-time bed status: available / occupied",
        "Assign a patient to a specific bed",
        "Conflict prevention via transactional locking",
        "Release bed automatically on patient discharge",
      ]}
    />
  );
}
