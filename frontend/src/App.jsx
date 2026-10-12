import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import LoginPage from "./auth/LoginPage";
import ProfilePage from "./auth/ProfilePage";
import AdminDashboard from "./common/AdminDashboard";
import AnalyticsDashboard from "./common/AnalyticsDashboard";
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
        <Route path="/login" element={<LoginPage />} />

        <Route path="/profile" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />

        <Route path="/admin" element={<ProtectedRoute roles={["ADMIN"]}><AdminDashboard /></ProtectedRoute>} />
        <Route path="/analytics" element={<ProtectedRoute roles={["ADMIN"]}><AnalyticsDashboard /></ProtectedRoute>} />

        <Route path="/simulation" element={<ProtectedRoute roles={["ADMIN"]}><SimulationPage /></ProtectedRoute>} />

        <Route path="/receptionist" element={<ProtectedRoute roles={["RECEPTIONIST"]}><ReceptionistDashboard /></ProtectedRoute>} />
        <Route path="/queue" element={<ProtectedRoute roles={["RECEPTIONIST", "DOCTOR", "ADMIN"]}><LiveQueueDashboard /></ProtectedRoute>} />

        <Route path="/doctor" element={<ProtectedRoute roles={["DOCTOR"]}><DoctorDashboard /></ProtectedRoute>} />
        <Route path="/beds" element={<ProtectedRoute roles={["DOCTOR", "ADMIN"]}><BedStatusPage /></ProtectedRoute>} />

        <Route path="/lab" element={<ProtectedRoute roles={["LAB_TECH"]}><LabDashboard /></ProtectedRoute>} />
        <Route path="/pharmacy" element={<ProtectedRoute roles={["PHARMACIST"]}><PharmacyDashboard /></ProtectedRoute>} />
        <Route path="/discharge" element={<ProtectedRoute roles={["DOCTOR", "RECEPTIONIST"]}><DischargePage /></ProtectedRoute>} />

        <Route path="/" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
