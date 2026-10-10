import PageShell from "../common/PageShell";

export default function LiveQueueDashboard() {
  return (
    <PageShell
      title="Live Queue Dashboard"
      objective="Give all relevant staff a real-time, priority-sorted view of waiting patients."
      functions={[
        "Real-time list via WebSocket (no manual refresh)",
        "Sorted by priority score (urgency + wait time)",
        "Auto-updates on check-in, call-next, or removal",
        "Displays estimated wait time per patient",
      ]}
    />
  );
}
