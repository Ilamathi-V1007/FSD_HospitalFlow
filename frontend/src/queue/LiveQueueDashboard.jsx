import { useState, useEffect, useCallback } from "react";
import AppShell from "../common/AppShell";
import api from "../common/api";

const POLL_MS = 6_000;

const TRIAGE_BADGES = {
  1: { text: "Level 1 · Emergency", className: "badge badge-emergency" },
  2: { text: "Level 2 · Urgent", className: "badge badge-urgent" },
  3: { text: "Level 3 · Semi-Urgent", className: "badge badge-semi-urgent" },
  4: { text: "Level 4 · Non-Urgent", className: "badge badge-non-urgent" },
};

export default function LiveQueueDashboard() {
  const [queue, setQueue] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadErr, setLoadErr] = useState("");
  const [lastRefreshed, setLastRefreshed] = useState(new Date());

  const fetchQueue = useCallback(async () => {
    try {
      const res = await api.get("/queue");
      setQueue(res.data);
      setLoadErr("");
      setLastRefreshed(new Date());
    } catch {
      setLoadErr("Could not retrieve live queue status from server.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchQueue();
    const timer = setInterval(fetchQueue, POLL_MS);
    return () => clearInterval(timer);
  }, [fetchQueue]);

  return (
    <AppShell title="Live Outpatient Queue Board">
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", marginBottom: "1.5rem" }}>
        <div>
          <h2 style={{ fontSize: "1.25rem", fontWeight: 700, color: "var(--ink)", margin: "0 0 0.25rem 0", fontFamily: "var(--font-heading)" }}>
            Dynamic Priority Queue Ledger
          </h2>
          <p style={{ fontSize: "0.82rem", color: "var(--ink-muted)", margin: 0 }}>
            Ranked by Triage Weight + Time Waited (Aging Factor = 1 pt/min). Automatically refreshed.
          </p>
        </div>
        <div style={{ fontSize: "0.78rem", color: "var(--ink-muted)", display: "flex", alignItems: "center", gap: "8px" }}>
          <span>Last updated: <span style={{ fontVariantNumeric: "tabular-nums" }}>{lastRefreshed.toLocaleTimeString()}</span></span>
          <span className="demo-badge">Auto-Poll (6s)</span>
        </div>
      </div>

      {loadErr && <div className="alert alert-danger">{loadErr}</div>}

      <div className="data-table-container">
        {loading && queue.length === 0 ? (
          <div style={{ padding: "3rem", textAlign: "center", color: "var(--text-muted)" }}>
            Polling queue…
          </div>
        ) : queue.length === 0 ? (
          <div style={{ padding: "3.5rem", textAlign: "center" }}>
            <h4 style={{ fontSize: "1.05rem", color: "var(--navy)", margin: "0 0 0.35rem 0" }}>No Waiting Patients</h4>
            <p style={{ fontSize: "0.82rem", color: "var(--text-muted)", margin: 0 }}>
              All checked-in patients have been called into consultation.
            </p>
          </div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th style={{ width: "80px" }}>Rank</th>
                <th>Patient Name</th>
                <th>Triage Priority</th>
                <th>Reported Symptoms</th>
                <th>Wait Duration</th>
                <th>Priority Score</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {queue.map((item, index) => {
                const badge = TRIAGE_BADGES[item.urgencyLevel] || TRIAGE_BADGES[4];
                const isNext = index === 0;
                return (
                  <tr
                    key={item.queueId}
                    style={{
                      backgroundColor: isNext ? "#FAFDFD" : "transparent",
                    }}
                  >
                    <td style={{ fontWeight: 700, color: isNext ? "var(--teal)" : "var(--text-muted)" }}>
                      #{index + 1} {isNext && <span style={{ fontSize: "0.72rem", color: "var(--teal)", fontWeight: 800 }}>[NEXT]</span>}
                    </td>
                    <td>
                      <div style={{ fontWeight: 600, color: "var(--navy)" }}>{item.patientName}</div>
                      <div style={{ fontSize: "0.75rem", color: "var(--text-muted)" }}>
                        ID #{item.patientId} · {item.age ? `${item.age} yrs` : "—"} · {item.gender}
                      </div>
                    </td>
                    <td>
                      <span className={badge.className}>{badge.text}</span>
                    </td>
                    <td style={{ color: "var(--text-muted)", maxWidth: "260px" }}>
                      {item.symptoms || "None noted"}
                    </td>
                    <td>
                      <span style={{ fontWeight: 600, color: "var(--navy)" }}>{item.waitMinutes} min</span>
                    </td>
                    <td>
                      <span style={{ fontWeight: 700, color: "var(--teal)", fontSize: "0.9rem" }}>
                        {item.priorityScore.toFixed(1)}
                      </span>
                    </td>
                    <td>
                      <span className="badge" style={{ background: "#EFF6FF", color: "#1D4ED8", border: "1px solid #DBEAFE" }}>
                        {item.status}
                      </span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </AppShell>
  );
}
