import PageShell from "../common/PageShell";

export default function DischargePage() {
  return (
    <PageShell
      title="Discharge / Visit Summary"
      objective="Properly close out a patient's visit and free up resources."
      functions={[
        "Compile visit details: diagnosis, lab results, prescriptions",
        "Generate a visit summary",
        "Close the active visit record",
        "Release any assigned bed",
        "Remove patient from active queue/tracking",
      ]}
    />
  );
}
