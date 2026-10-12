import { useState, useEffect, useCallback } from "react";
import AppShell from "../common/AppShell";
import api from "../common/api";

const POLL_MS = 10_000;

export default function BedStatusPage() {
  const [beds, setBeds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [newBedNumber, setNewBedNumber] = useState("");
  const [addingBed, setAddingBed] = useState(false);
  const role = localStorage.getItem("role");

  const loadBeds = useCallback(async () => {
    try {
      const res = await api.get("/allocation/beds");
      setBeds(res.data);
      setError("");
    } catch {
      setError("Failed to load hospital beds from database.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadBeds();
    const interval = setInterval(loadBeds, POLL_MS);
    return () => clearInterval(interval);
  }, [loadBeds]);

  async function handleAddBed(e) {
    e.preventDefault();
    if (!newBedNumber.trim()) return;
    setAddingBed(true);
    try {
      await api.post("/allocation/beds", { bedNumber: newBedNumber.trim() });
      setNewBedNumber("");
      await loadBeds();
    } catch (err) {
      setError(err.response?.data?.error || "Failed to configure bed.");
    } finally {
      setAddingBed(false);
    }
  }

  const occupiedCount = beds.filter((b) => b.status === "OCCUPIED").length;
  const availableCount = beds.filter((b) => b.status === "AVAILABLE").length;

  return (
    <AppShell title="Hospital Bed Capacity Management">
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", marginBottom: "1.75rem" }}>
        <div>
          <h2 style={{ fontSize: "1.25rem", fontWeight: 700, color: "var(--ink)", margin: "0 0 0.25rem 0", fontFamily: "var(--font-heading)" }}>
            Real-Time Bed Availability Ledger
          </h2>
          <p style={{ fontSize: "0.82rem", color: "var(--ink-muted)", margin: 0 }}>
            Occupancy is updated dynamically upon doctor admission and patient discharge.
          </p>
        </div>
        <div style={{ display: "flex", gap: "10px" }}>
          <span className="badge badge-available">{availableCount} Available</span>
          <span className="badge badge-occupied">{occupiedCount} Occupied</span>
        </div>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      {role === "ADMIN" && (
        <div className="panel" style={{ marginBottom: "1.75rem", padding: "1.25rem" }}>
          <h4 style={{ fontSize: "0.95rem", fontWeight: 700, color: "var(--navy)", margin: "0 0 0.5rem 0" }}>
            Add New Hospital Bed
          </h4>
          <form onSubmit={handleAddBed} style={{ display: "flex", gap: "10px", maxWidth: "420px" }}>
            <input
              type="text"
              required
              className="form-control"
              placeholder="Bed Number (e.g. 301, ICU-2)"
              value={newBedNumber}
              onChange={(e) => setNewBedNumber(e.target.value)}
            />
            <button type="submit" disabled={addingBed} className="btn btn-navy">
              {addingBed ? "Adding…" : "+ Register Bed"}
            </button>
          </form>
        </div>
      )}

      {loading ? (
        <div style={{ padding: "3rem", textAlign: "center", color: "var(--text-muted)" }}>
          Loading hospital bed inventory…
        </div>
      ) : beds.length === 0 ? (
        <div className="panel" style={{ textAlign: "center", padding: "3rem" }}>
          <p style={{ color: "var(--text-muted)", margin: 0 }}>No beds configured in the system yet.</p>
        </div>
      ) : (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(180px, 1fr))", gap: "1rem" }}>
          {beds.map((b) => {
            const isOccupied = b.status === "OCCUPIED";
            return (
              <div
                key={b.id}
                style={{
                  backgroundColor: "#FFFFFF",
                  border: `1px solid ${isOccupied ? "var(--danger-border)" : "var(--border)"}`,
                  borderLeft: `4px solid ${isOccupied ? "var(--danger-text)" : "var(--teal)"}`,
                  borderRadius: "var(--radius-sm)",
                  padding: "1.1rem 1rem",
                  boxShadow: "0 1px 2px rgba(20,44,64,0.02)",
                }}
              >
                <div style={{ fontSize: "1.15rem", fontWeight: 800, color: "var(--navy)" }}>
                  Bed {b.bedNumber}
                </div>
                <div style={{ marginTop: "6px" }}>
                  <span className={isOccupied ? "badge badge-occupied" : "badge badge-available"}>
                    {isOccupied ? "OCCUPIED" : "AVAILABLE"}
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </AppShell>
  );
}
