import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import PrivateRoute from './components/PrivateRoute';
import Layout from './components/Layout';

// Pages
import Login from './pages/Login';
import Unauthorized from './pages/Unauthorized';

// Admin Pages
import AdminDashboard from './pages/admin/Dashboard';
import Users from './pages/admin/Users';
import AllStations from './pages/admin/AllStations';
import AllBookings from './pages/admin/AllBookings';

// Owner Pages
import OwnerDashboard from './pages/owner/Dashboard';
import MyStations from './pages/owner/MyStations';
import Slots from './pages/owner/Slots';
import OwnerBookings from './pages/owner/Bookings';

function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          {/* Public Routes */}
          <Route path="/login" element={<Login />} />
          <Route path="/unauthorized" element={<Unauthorized />} />
          
          {/* Admin Routes */}
          <Route path="/admin" element={
            <PrivateRoute allowedRoles={['ADMIN']}>
              <Layout />
            </PrivateRoute>
          }>
            <Route path="dashboard" element={<AdminDashboard />} />
            <Route path="users" element={<Users />} />
            <Route path="stations" element={<AllStations />} />
            <Route path="bookings" element={<AllBookings />} />
          </Route>
          
          {/* Station Owner Routes */}
          <Route path="/owner" element={
            <PrivateRoute allowedRoles={['STATION_OWNER']}>
              <Layout />
            </PrivateRoute>
          }>
            <Route path="dashboard" element={<OwnerDashboard />} />
            <Route path="stations" element={<MyStations />} />
            <Route path="slots" element={<Slots />} />
            <Route path="bookings" element={<OwnerBookings />} />
          </Route>
          
          {/* Default redirect */}
          <Route path="/" element={<Navigate to="/login" replace />} />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;
