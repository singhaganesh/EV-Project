import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { LayoutDashboard, MapPin, Users, Calendar, BarChart2, Settings, Wallet } from 'lucide-react';

import DashboardLayout from './components/layout/DashboardLayout';
import DashboardOverview from './pages/admin/DashboardOverview';
import PumpOwnerDashboard from './pages/owner/PumpOwnerDashboard';
import MyStations from './pages/owner/MyStations';
import StationsList from './pages/admin/StationsList';
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';

const adminNav = [
    { name: 'Dashboard', path: '/', icon: LayoutDashboard },
    { name: 'Stations', path: '/stations', icon: MapPin },
    { name: 'Users', path: '/users', icon: Users },
    { name: 'Bookings', path: '/bookings', icon: Calendar },
    { name: 'Analytics', path: '/analytics', icon: BarChart2 },
    { name: 'Settings', path: '/settings', icon: Settings },
];

const ownerNav = [
    { name: 'Dashboard', path: '/owner-dashboard', icon: LayoutDashboard },
    { name: 'My Stations', path: '/owner-stations', icon: MapPin },
    { name: 'Earnings', path: '/owner-earnings', icon: Wallet },
    { name: 'Settings', path: '/owner-settings', icon: Settings },
];

function App() {
    return (
        <BrowserRouter>
            <Routes>
                {/* Public Routes without Sidebar/Header */}
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />

                {/* Admin Routes */}
                <Route element={<DashboardLayout navItems={adminNav} sidebarTitle="Plugsy Admin" />}>
                    <Route path="/" element={<DashboardOverview />} />
                    <Route path="/stations" element={<StationsList />} />
                    <Route path="/users" element={<div className="p-8">Users Page</div>} />
                    <Route path="/bookings" element={<div className="p-8">Bookings Page</div>} />
                    <Route path="/analytics" element={<div className="p-8">Analytics Page</div>} />
                    <Route path="/settings" element={<div className="p-8">Settings Page</div>} />
                </Route>

                {/* Pump Owner Routes */}
                <Route element={<DashboardLayout navItems={ownerNav} sidebarTitle="Plugsy Partner" />}>
                    <Route path="/owner-dashboard" element={<PumpOwnerDashboard />} />
                    <Route path="/owner-stations" element={<MyStations />} />
                    <Route path="/owner-earnings" element={<div className="p-8">Earnings Report</div>} />
                    <Route path="/owner-settings" element={<div className="p-8">Owner Settings</div>} />
                </Route>

                {/* Fallback route */}
                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </BrowserRouter>
    );
}

export default App;
