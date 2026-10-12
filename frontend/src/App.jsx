import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import LandingPage from "./common/LandingPage";
import RoleLoginPage from "./auth/RoleLoginPage";
import LoginPage from "./auth/LoginPage";

import AdminDashboard from "./common/AdminDashboard";
import ReceptionistDashboard from "./queue/ReceptionistDashboard";
import LiveQueueDashboard from "./queue/LiveQueueDashboard";
import DoctorDashboard from "./allocation/DoctorDashboard";
import BedStatusPage from "./allocation/BedStatusPage";
import LabDashboard from "./labpharmacy/LabDashboard";
import PharmacyDashboard from "./labpharmacy/PharmacyDashboard";
import DischargePage from "./labpharmacy/DischargePage";
import SimulationPage from "./simulation/SimulationPage";
import ProtectedRoute from "./common/ProtectedRoute";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Landing Page */}
        <Route path="/" element={<LandingPage />} />

        {/* 5 Role-Specific Login Routes */}
        <Route path="/login/:roleKey" element={<RoleLoginPage />} />
        {/* Fallback default login */}
        <Route path="/login" element={<LoginPage />} />

        {/* Administrator Routes */}
        <Route
          path="/admin"
          element={
            <ProtectedRoute roles={["ADMIN"]}>
              <AdminDashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/simulation"
          element={
            <ProtectedRoute roles={["ADMIN"]}>
              <SimulationPage />
            </ProtectedRoute>
          }
        />

        {/* Receptionist Routes */}
        <Route
          path="/receptionist"
          element={
            <ProtectedRoute roles={["RECEPTIONIST"]}>
              <ReceptionistDashboard />
            </ProtectedRoute>
          }
        />

        {/* Live Queue (Multi-role visibility) */}
        <Route
          path="/queue"
          element={
            <ProtectedRoute roles={["RECEPTIONIST", "DOCTOR", "ADMIN"]}>
              <LiveQueueDashboard />
            </ProtectedRoute>
          }
        />

        {/* Doctor Routes */}
        <Route
          path="/doctor"
          element={
            <ProtectedRoute roles={["DOCTOR"]}>
              <DoctorDashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/beds"
          element={
            <ProtectedRoute roles={["DOCTOR", "ADMIN"]}>
              <BedStatusPage />
            </ProtectedRoute>
          }
        />

        {/* Lab & Pharmacy Routes */}
        <Route
          path="/lab"
          element={
            <ProtectedRoute roles={["LAB_TECH"]}>
              <LabDashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/pharmacy"
          element={
            <ProtectedRoute roles={["PHARMACIST"]}>
              <PharmacyDashboard />
            </ProtectedRoute>
          }
        />

        {/* Discharge Desk */}
        <Route
          path="/discharge"
          element={
            <ProtectedRoute roles={["DOCTOR", "RECEPTIONIST", "ADMIN"]}>
              <DischargePage />
            </ProtectedRoute>
          }
        />

        {/* Catch-all */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
