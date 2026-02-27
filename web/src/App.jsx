import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { LayoutDashboard, MapPin, Users, Calendar, BarChart2, Settings, Wallet } from 'lucide-react';

import DashboardLayout from './components/layout/DashboardLayout';
import PrivateRoute from './components/auth/PrivateRoute';
import RoleRoute from './components/auth/RoleRoute';

import DashboardOverview from './pages/admin/DashboardOverview';
import PumpOwnerDashboard from './pages/owner/PumpOwnerDashboard';
import MyStations from './pages/owner/MyStations';
import StationsList from './pages/admin/StationsList';
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';

// Admin sidebar navigation — all paths under /admin/
const adminNav = [
    { name: 'Dashboard', path: '/admin', icon: LayoutDashboard },
    { name: 'Stations', path: '/admin/stations', icon: MapPin },
    { name: 'Users', path: '/admin/users', icon: Users },
    { name: 'Bookings', path: '/admin/bookings', icon: Calendar },
    { name: 'Analytics', path: '/admin/analytics', icon: BarChart2 },
    { name: 'Settings', path: '/admin/settings', icon: Settings },
];

// Owner sidebar navigation — all paths under /owner/
const ownerNav = [
    { name: 'Dashboard', path: '/owner', icon: LayoutDashboard },
    { name: 'My Stations', path: '/owner/stations', icon: MapPin },
    { name: 'Earnings', path: '/owner/earnings', icon: Wallet },
    { name: 'Settings', path: '/owner/settings', icon: Settings },
];

function App() {
    return (
        <BrowserRouter>
            <Routes>
                {/* ── Public Routes (no sidebar/header) ── */}
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />

                {/* ── Admin Routes (/admin/*) ── */}
                {/* Must be authenticated AND have ADMIN role */}
                <Route element={<PrivateRoute />}>
                    <Route element={<RoleRoute allowedRole="ADMIN" />}>
                        <Route element={<DashboardLayout navItems={adminNav} sidebarTitle="Plugsy Admin" />}>
                            <Route path="/admin" element={<DashboardOverview />} />
                            <Route path="/admin/stations" element={<StationsList />} />
                            <Route path="/admin/users" element={<div className="p-8 text-slate-700 font-medium">Users Page — Coming Soon</div>} />
                            <Route path="/admin/bookings" element={<div className="p-8 text-slate-700 font-medium">Bookings Page — Coming Soon</div>} />
                            <Route path="/admin/analytics" element={<div className="p-8 text-slate-700 font-medium">Analytics Page — Coming Soon</div>} />
                            <Route path="/admin/settings" element={<div className="p-8 text-slate-700 font-medium">Settings Page — Coming Soon</div>} />
                        </Route>
                    </Route>
                </Route>

                {/* ── Pump Owner Routes (/owner/*) ── */}
                {/* Must be authenticated AND have STATION_OWNER role */}
                <Route element={<PrivateRoute />}>
                    <Route element={<RoleRoute allowedRole="STATION_OWNER" />}>
                        <Route element={<DashboardLayout navItems={ownerNav} sidebarTitle="Plugsy Partner" />}>
                            <Route path="/owner" element={<PumpOwnerDashboard />} />
                            <Route path="/owner/stations" element={<MyStations />} />
                            <Route path="/owner/earnings" element={<div className="p-8 text-slate-700 font-medium">Earnings Report — Coming Soon</div>} />
                            <Route path="/owner/settings" element={<div className="p-8 text-slate-700 font-medium">Owner Settings — Coming Soon</div>} />
                        </Route>
                    </Route>
                </Route>

                {/* ── Fallback: redirect everything else to /login ── */}
                <Route path="*" element={<Navigate to="/login" replace />} />
            </Routes>
        </BrowserRouter>
    );
}

export default App;
