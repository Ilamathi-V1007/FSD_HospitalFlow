import { Navigate } from "react-router-dom";

// Wrap any page with <ProtectedRoute roles={["ADMIN"]}>...</ProtectedRoute>
// Omit `roles` to just require "logged in, any role".
export default function ProtectedRoute({ children, roles }) {
  const token = localStorage.getItem("token");
  const userRole = localStorage.getItem("role");

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  if (roles && !roles.includes(userRole)) {
    return <Navigate to="/" replace />;
  }

  return children;
}
