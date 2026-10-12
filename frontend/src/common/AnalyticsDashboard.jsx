import AppShell from "./AppShell";

export default function AnalyticsDashboard() {
  return (
    <AppShell title="Clinical Flow & Throughput Analytics">
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: "1rem", marginBottom: "1.5rem" }}>
        <div className="card">
          <div className="card-label">Mean Consultation Duration</div>
          <div className="card-value">14.2 <span style={{ fontSize: "0.85rem", fontWeight: 400, color: "var(--ink-muted)" }}>min</span></div>
          <div style={{ fontSize: "0.75rem", color: "var(--petrol)", marginTop: "4px" }}>Within target benchmark (&lt; 15 min)</div>
        </div>

        <div className="card">
          <div className="card-label">Queue Turnaround Time</div>
          <div className="card-value">22.8 <span style={{ fontSize: "0.85rem", fontWeight: 400, color: "var(--ink-muted)" }}>min</span></div>
          <div style={{ fontSize: "0.75rem", color: "var(--ink-muted)", marginTop: "4px" }}>Registration to clinical intake</div>
        </div>

        <div className="card">
          <div className="card-label">Diagnostics Bottleneck Index</div>
          <div className="card-value">1.18</div>
          <div style={{ fontSize: "0.75rem", color: "var(--ink-muted)", marginTop: "4px" }}>Lab order completion ratio</div>
        </div>

        <div className="card">
          <div className="card-label">Bed Utilization Rate</div>
          <div className="card-value">62.5%</div>
          <div style={{ fontSize: "0.75rem", color: "var(--petrol)", marginTop: "4px" }}>Ward capacity optimal</div>
        </div>
      </div>

      <div className="table-container" style={{ marginBottom: "1.5rem" }}>
        <div style={{ padding: "0.85rem 1rem", borderBottom: "1px solid var(--rule)", background: "var(--surface)", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <div>
            <h3 style={{ margin: 0, fontSize: "0.95rem", fontFamily: "var(--font-heading)" }}>OPD Workload Distribution by Department</h3>
            <span style={{ fontSize: "0.75rem", color: "var(--ink-muted)" }}>Live shift performance ledger</span>
          </div>
          <span className="badge badge-teal">Shift Active</span>
        </div>
        <table className="ward-table">
          <thead>
            <tr>
              <th>Clinical Department</th>
              <th style={{ textAlign: "right" }}>Active Consultations</th>
              <th style={{ textAlign: "right" }}>Patients Queued</th>
              <th style={{ textAlign: "right" }}>Avg Wait (min)</th>
              <th style={{ textAlign: "right" }}>Completed Today</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><strong>General Medicine</strong></td>
              <td style={{ textAlign: "right", fontVariantNumeric: "tabular-nums" }}>4</td>
              <td style={{ textAlign: "right", fontVariantNumeric: "tabular-nums" }}>7</td>
              <td style={{ textAlign: "right", fontVariantNumeric: "tabular-nums" }}>18.4</td>
              <td style={{ textAlign: "right", fontVariantNumeric: "tabular-nums" }}>24</td>
            </tr>
            <tr>
              <td><strong>Emergency & Triage</strong></td>
              <td style={{ textAlign: "right", fontVariantNumeric: "tabular-nums" }}>2</td>
              <td style={{ textAlign: "right", fontVariantNumeric: "tabular-nums" }}>1</td>
              <td style={{ textAlign: "right", fontVariantNumeric: "tabular-nums" }}>4.2</td>
              <td style={{ textAlign: "right", fontVariantNumeric: "tabular-nums" }}>11</td>
            </tr>
            <tr>
              <td><strong>Orthopaedics</strong></td>
              <td style={{ textAlign: "right", fontVariantNumeric: "tabular-nums" }}>1</td>
              <td style={{ textAlign: "right", fontVariantNumeric: "tabular-nums" }}>3</td>
              <td style={{ textAlign: "right", fontVariantNumeric: "tabular-nums" }}>21.0</td>
              <td style={{ textAlign: "right", fontVariantNumeric: "tabular-nums" }}>9</td>
            </tr>
            <tr>
              <td><strong>Paediatrics</strong></td>
              <td style={{ textAlign: "right", fontVariantNumeric: "tabular-nums" }}>2</td>
              <td style={{ textAlign: "right", fontVariantNumeric: "tabular-nums" }}>4</td>
              <td style={{ textAlign: "right", fontVariantNumeric: "tabular-nums" }}>14.8</td>
              <td style={{ textAlign: "right", fontVariantNumeric: "tabular-nums" }}>16</td>
            </tr>
          </tbody>
        </table>
      </div>
    </AppShell>
  );
}
