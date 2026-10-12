import { useNavigate } from "react-router-dom";

export default function Navbar() {
  const navigate = useNavigate();
  const role = localStorage.getItem("role");
  const name = localStorage.getItem("name");

  function logout() {
    localStorage.clear();
    navigate("/login");
  }

  if (!role) return null;

  return (
    <nav className="navbar">
      <div className="navbar-brand" style={{ cursor: "pointer" }} onClick={() => navigate("/")}>
        FlowAlloc · Hospital Flow
      </div>
      <div className="navbar-user">
        <span style={{ marginRight: "8px", fontWeight: "600" }}>{name} · {role}</span>

        {/* Role-specific quick links */}
        {role === "RECEPTIONIST" && (
          <>
            <button onClick={() => navigate("/receptionist")}>Check-In</button>
            <button onClick={() => navigate("/queue")}>Live Queue</button>
            <button onClick={() => navigate("/discharge")}>Discharge</button>
          </>
        )}

        {role === "DOCTOR" && (
          <>
            <button onClick={() => navigate("/doctor")}>My Desk</button>
            <button onClick={() => navigate("/queue")}>Queue</button>
            <button onClick={() => navigate("/beds")}>Beds</button>
            <button onClick={() => navigate("/discharge")}>Discharge</button>
          </>
        )}

        {role === "LAB_TECH" && (
          <button onClick={() => navigate("/lab")}>Lab Requests</button>
        )}

        {role === "PHARMACIST" && (
          <button onClick={() => navigate("/pharmacy")}>Pharmacy</button>
        )}

        {role === "ADMIN" && (
          <>
            <button onClick={() => navigate("/queue")}>Queue</button>
            <button onClick={() => navigate("/beds")}>Beds</button>
            <button onClick={() => navigate("/simulation")}>Simulation</button>
          </>
        )}

        <button onClick={logout} style={{ marginLeft: "6px", background: "#ef4444" }}>Logout</button>
      </div>
    </nav>
  );
}
