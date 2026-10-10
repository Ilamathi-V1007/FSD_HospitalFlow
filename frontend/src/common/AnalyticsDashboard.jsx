import PageShell from "./PageShell";

export default function AnalyticsDashboard() {
  return (
    <PageShell
      title="Analytics Dashboard"
      objective="Give admins data-driven insight into system performance."
      functions={[
        "Peak hours graph (patient inflow over time)",
        "Average wait time trend",
        "Bottleneck detection (queue vs. doctor vs. lab stage)",
        "Doctor workload distribution chart",
      ]}
    />
  );
}
