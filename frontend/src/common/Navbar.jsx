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
      <div className="navbar-brand">Patient Flow Coordination</div>
      <div className="navbar-user">
        <span>{name} · {role}</span>
        {role === "ADMIN" && (
          <button onClick={() => navigate("/simulation")}>Simulation</button>
        )}
        <button onClick={() => navigate("/profile")}>Profile</button>
        <button onClick={logout}>Logout</button>
      </div>
    </nav>
  );
}
