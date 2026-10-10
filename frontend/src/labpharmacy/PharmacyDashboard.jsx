import PageShell from "../common/PageShell";

export default function PharmacyDashboard() {
  return (
    <PageShell
      title="Pharmacist Dashboard"
      objective="Track prescription fulfillment."
      functions={[
        "View prescriptions issued by doctors",
        "Mark a prescription as dispensed",
        "View dispensing history",
      ]}
    />
  );
}
