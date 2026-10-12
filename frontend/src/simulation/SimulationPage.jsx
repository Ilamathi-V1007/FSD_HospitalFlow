import { useState } from "react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import AppShell from "../common/AppShell";
import api from "../common/api";
import "./simulation.css";

const LEVEL_LABEL = {
  1: "1 · Emergency",
  2: "2 · Urgent",
  3: "3 · Semi-urgent",
  4: "4 · Non-urgent",
};

const FCFS_COLOR = "#8a9a9d";
const PRIORITY_COLOR = "#1f7a6c";

// Same defaults and limits as the backend (SimulationRequest.java)
const FIELDS = [
  { key: "patients", label: "Patients per run", min: 10, max: 2000, step: 10, def: 300 },
  { key: "doctors", label: "Doctors", min: 1, max: 20, step: 1, def: 3 },
  { key: "arrivalsPerHour", label: "Arrivals per hour", min: 1, max: 500, step: 1, def: 14 },
  { key: "avgConsultMinutes", label: "Avg consultation (min)", min: 3, max: 60, step: 1, def: 12 },
  { key: "replications", label: "Replications", min: 1, max: 50, step: 1, def: 20 },
  { key: "seed", label: "Random seed", min: 0, max: 999999999, step: 1, def: 42 },
];

const DEFAULT_FORM = Object.fromEntries(FIELDS.map((f) => [f.key, String(f.def)]));

function fmt(n) {
  return Number(n).toFixed(1);
}

function pct(n) {
  const v = Number(n);
  return `${v > 0 ? "+" : ""}${v.toFixed(1)}%`;
}

function errorMessage(err) {
  const data = err.response?.data;
  if (err.response?.status === 403) return "Only admins can run the simulation.";
  if (data?.error) return data.error;
  if (data && typeof data === "object") return Object.values(data).join(" · ");
  return "Could not run the simulation. Is the backend running?";
}

function toCsv(result) {
  const header = [
    "triage_level", "patients",
    "fcfs_avg_wait", "fcfs_p90_wait", "priority_avg_wait", "priority_p90_wait",
    "avg_improvement_pct", "p90_improvement_pct",
  ];
  const rows = result.comparison.map((c, i) => {
    const f = i < 4 ? result.fcfs.byLevel[i] : result.fcfs.overall;
    const p = i < 4 ? result.priority.byLevel[i] : result.priority.overall;
    return [
      c.triageLevel === 0 ? "ALL" : c.triageLevel,
      f.count, f.avgWait, f.p90Wait, p.avgWait, p.p90Wait,
      c.avgImprovementPct, c.p90ImprovementPct,
    ].join(",");
  });
  return [header.join(","), ...rows].join("\n");
}

export default function SimulationPage() {
  const [form, setForm] = useState(DEFAULT_FORM);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  function update(key, value) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  function clientValidate() {
    for (const f of FIELDS) {
      const raw = form[f.key];
      const n = Number(raw);
      if (raw === "" || Number.isNaN(n) || n < f.min || n > f.max) {
        return `${f.label} must be a number between ${f.min} and ${f.max}.`;
      }
    }
    return "";
  }

  async function run(e) {
    e.preventDefault();
    const problem = clientValidate();
    if (problem) {
      setError(problem);
      return;
    }
    setError("");
    setLoading(true);
    try {
      const body = Object.fromEntries(FIELDS.map((f) => [f.key, Number(form[f.key])]));
      const res = await api.post("/simulation/run", body);
      setResult(res.data);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  function downloadCsv() {
    const blob = new Blob([toCsv(result)], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `simulation-seed-${result.parameters.seed}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  const chartData = result
    ? result.fcfs.byLevel.map((f, i) => ({
        level: LEVEL_LABEL[f.triageLevel],
        avgFcfs: f.avgWait,
        avgPriority: result.priority.byLevel[i].avgWait,
        p90Fcfs: f.p90Wait,
        p90Priority: result.priority.byLevel[i].p90Wait,
      }))
    : [];

  const emergency = result?.comparison[0];
  const nonUrgent = result?.comparison[3];

  return (
    <AppShell title="Scheduler Simulation (FCFS vs. Priority)">
      <div className="sim-page" style={{ padding: "0 0 2rem 0", maxWidth: "100%" }}>
        <p className="sim-intro">
          The same synthetic patients are run through first-come-first-served (FCFS) and our priority
          scheduler (base weight by triage level + 1 point per minute waited). Results pool all replications.
          All data is synthetic.
        </p>

        <form className="sim-form" onSubmit={run}>
          {FIELDS.map((f) => (
            <label key={f.key} className="sim-field">
              <span>{f.label}</span>
              <input
                type="number"
                min={f.min}
                max={f.max}
                step={f.step}
                value={form[f.key]}
                onChange={(e) => update(f.key, e.target.value)}
              />
            </label>
          ))}
          <div className="sim-actions">
            <button type="submit" disabled={loading}>
              {loading ? "Running…" : "Run simulation"}
            </button>
            <button type="button" className="sim-secondary" onClick={() => setForm(DEFAULT_FORM)} disabled={loading}>
              Reset
            </button>
          </div>
        </form>

        {error && <p className="sim-error" role="alert">{error}</p>}

        {result && (
          <section className="sim-results">
            <div className="sim-cards">
              <div className="sim-card">
                <div className="sim-card-label">Emergency avg wait</div>
                <div className="sim-card-value">
                  {fmt(result.fcfs.byLevel[0].avgWait)} → {fmt(result.priority.byLevel[0].avgWait)} min
                </div>
                <div className="sim-card-note sim-good">{pct(emergency.avgImprovementPct)} vs FCFS</div>
              </div>
              <div className="sim-card">
                <div className="sim-card-label">Emergency 90th percentile</div>
                <div className="sim-card-value">
                  {fmt(result.fcfs.byLevel[0].p90Wait)} → {fmt(result.priority.byLevel[0].p90Wait)} min
                </div>
                <div className="sim-card-note sim-good">{pct(emergency.p90ImprovementPct)} vs FCFS</div>
              </div>
              <div className="sim-card">
                <div className="sim-card-label">Non-urgent avg wait</div>
                <div className="sim-card-value">
                  {fmt(result.fcfs.byLevel[3].avgWait)} → {fmt(result.priority.byLevel[3].avgWait)} min
                </div>
                <div className={`sim-card-note ${nonUrgent.avgImprovementPct < 0 ? "sim-bad" : "sim-good"}`}>
                  {pct(nonUrgent.avgImprovementPct)} vs FCFS
                </div>
              </div>
              <div className="sim-card">
                <div className="sim-card-label">Offered load</div>
                <div className="sim-card-value">{Math.round(result.offeredLoad * 100)}%</div>
                <div className="sim-card-note">of doctor capacity</div>
              </div>
            </div>

            {result.offeredLoad >= 1 && (
              <p className="sim-warning">
                Offered load is at or above 100%: patients arrive faster than doctors can see them, so waits keep
                growing during the run. Add doctors or lower the arrival rate for a steady-state comparison.
              </p>
            )}
            {result.offeredLoad < 0.5 && (
              <p className="sim-warning">
                Offered load is low, so there is little queueing and both policies look alike. Raise arrivals or
                reduce doctors to see the effect.
              </p>
            )}

            <div className="sim-charts">
              <div className="sim-chart">
                <h2>Average wait by triage level (min)</h2>
                <ResponsiveContainer width="100%" height={280}>
                  <BarChart data={chartData} margin={{ top: 8, right: 8, left: 0, bottom: 8 }}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="level" tick={{ fontSize: 12 }} />
                    <YAxis tick={{ fontSize: 12 }} />
                    <Tooltip formatter={(v) => `${fmt(v)} min`} />
                    <Legend />
                    <Bar dataKey="avgFcfs" name="FCFS" fill={FCFS_COLOR} />
                    <Bar dataKey="avgPriority" name="Priority" fill={PRIORITY_COLOR} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
              <div className="sim-chart">
                <h2>90th-percentile wait by triage level (min)</h2>
                <ResponsiveContainer width="100%" height={280}>
                  <BarChart data={chartData} margin={{ top: 8, right: 8, left: 0, bottom: 8 }}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="level" tick={{ fontSize: 12 }} />
                    <YAxis tick={{ fontSize: 12 }} />
                    <Tooltip formatter={(v) => `${fmt(v)} min`} />
                    <Legend />
                    <Bar dataKey="p90Fcfs" name="FCFS" fill={FCFS_COLOR} />
                    <Bar dataKey="p90Priority" name="Priority" fill={PRIORITY_COLOR} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>

            <div className="sim-table-wrap">
              <table className="sim-table">
                <thead>
                  <tr>
                    <th rowSpan={2}>Triage level</th>
                    <th rowSpan={2}>Patients</th>
                    <th colSpan={2}>FCFS (min)</th>
                    <th colSpan={2}>Priority (min)</th>
                    <th colSpan={2}>Change vs FCFS</th>
                  </tr>
                  <tr>
                    <th>Avg</th><th>P90</th>
                    <th>Avg</th><th>P90</th>
                    <th>Avg</th><th>P90</th>
                  </tr>
                </thead>
                <tbody>
                  {result.comparison.map((c, i) => {
                    const f = i < 4 ? result.fcfs.byLevel[i] : result.fcfs.overall;
                    const p = i < 4 ? result.priority.byLevel[i] : result.priority.overall;
                    const empty = f.count === 0;
                    return (
                      <tr key={c.triageLevel} className={c.triageLevel === 0 ? "sim-total" : ""}>
                        <td>{c.triageLevel === 0 ? "All patients" : LEVEL_LABEL[c.triageLevel]}</td>
                        <td>{f.count}</td>
                        <td>{empty ? "—" : fmt(f.avgWait)}</td>
                        <td>{empty ? "—" : fmt(f.p90Wait)}</td>
                        <td>{empty ? "—" : fmt(p.avgWait)}</td>
                        <td>{empty ? "—" : fmt(p.p90Wait)}</td>
                        <td className={c.avgImprovementPct >= 0 ? "sim-good" : "sim-bad"}>
                          {empty ? "—" : pct(c.avgImprovementPct)}
                        </td>
                        <td className={c.p90ImprovementPct >= 0 ? "sim-good" : "sim-bad"}>
                          {empty ? "—" : pct(c.p90ImprovementPct)}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            <p className="sim-footnote">
              Positive change = priority waits less than FCFS. The priority scheduler shifts waiting from
              urgent patients to non-urgent ones; aging keeps the non-urgent wait bounded.
            </p>
            <button type="button" className="sim-secondary" onClick={downloadCsv}>
              Download results (CSV)
            </button>
          </section>
        )}
      </div>
    </AppShell>
  );
}
