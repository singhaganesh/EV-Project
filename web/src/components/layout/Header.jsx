import React from 'react';
import { useLocation } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { Search, Bell, ChevronDown } from 'lucide-react';
import { selectCurrentUser } from '../../store/authSlice';

const getPageInfo = (pathname) => {
    switch (pathname) {
        // Admin pages
        case '/admin':
            return { title: 'Dashboard Overview', subtitle: 'Welcome back, Admin' };
        case '/admin/stations':
            return { title: 'Station Management', subtitle: 'Monitor and manage all your charging points in real-time.' };
        case '/admin/users':
            return { title: 'User Management', subtitle: 'Manage platform users and roles.' };
        case '/admin/bookings':
            return { title: 'Bookings', subtitle: 'View and manage charging sessions.' };
        case '/admin/analytics':
            return { title: 'Analytics', subtitle: 'Platform-wide statistics and trends.' };
        case '/admin/settings':
            return { title: 'Settings', subtitle: 'System configurations and preferences.' };

        // Owner pages
        case '/owner':
            return { title: 'Station Manager', subtitle: 'Manage your stations, dispensers, and connectors.' };
        case '/owner/stations':
            return { title: 'My Stations', subtitle: 'Overview of all your charging locations.' };
        case '/owner/earnings':
            return { title: 'Earnings', subtitle: 'Payouts and financial reporting.' };
        case '/owner/settings':
            return { title: 'Account Settings', subtitle: 'Manage your partner profile and preferences.' };

        default:
            return { title: 'Dashboard', subtitle: '' };
    }
};

const getRoleLabel = (role) => {
    switch (role) {
        case 'ADMIN': return 'Super Admin';
        case 'STATION_OWNER': return 'Pump Owner';
        case 'CUSTOMER': return 'Customer';
        default: return role || 'User';
    }
};

export default function Header() {
    const location = useLocation();
    const { title, subtitle } = getPageInfo(location.pathname);
    const user = useSelector(selectCurrentUser);

    const displayName = user?.name || user?.email || 'User';
    const displayRole = getRoleLabel(user?.role);

    // Generate initials for avatar
    const initials = displayName
        .split(' ')
        .map((n) => n[0])
        .join('')
        .toUpperCase()
        .slice(0, 2);

    return (
        <header className="h-24 bg-[#F4F7FE] flex items-center justify-between px-8 shrink-0">
            {/* Page Title Area */}
            <div className="flex-1">
                <h2 className="text-2xl font-bold text-[#1A2234]">{title}</h2>
                <p className="text-sm text-slate-500 mt-1">{subtitle}</p>
            </div>

            <div className="flex items-center gap-6">
                {/* Search Bar */}
                <div className="relative group">
                    <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                        <Search className="h-4 w-4 text-slate-400 group-focus-within:text-cyan-500 transition-colors" />
                    </div>
                    <input
                        type="text"
                        className="block w-80 pl-10 pr-4 py-3 bg-white border-0 rounded-full text-sm placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-cyan-500/20 shadow-sm transition-all shadow-slate-200/50"
                        placeholder="Search stations, users..."
                    />
                </div>

                {/* Notification Bell */}
                <button className="relative p-3 bg-white rounded-full text-slate-500 hover:text-slate-700 shadow-sm shadow-slate-200/50 transition-colors">
                    <span className="absolute top-3 right-3 block h-2 w-2 rounded-full bg-rose-500 ring-2 ring-white" />
                    <Bell className="h-5 w-5" />
                </button>

                {/* User Profile */}
                <button className="flex items-center gap-3 pl-2 pr-4 py-1.5 bg-white rounded-full shadow-sm shadow-slate-200/50 hover:bg-slate-50 transition-colors">
                    {/* Initials avatar */}
                    <div className="h-9 w-9 rounded-full bg-cyan-500 flex items-center justify-center ring-2 ring-white shadow-sm flex-shrink-0">
                        <span className="text-white text-sm font-bold">{initials}</span>
                    </div>
                    <div className="text-left hidden sm:block">
                        <p className="text-sm font-semibold text-slate-700 leading-tight">{displayName}</p>
                        <p className="text-xs text-slate-500 leading-tight">{displayRole}</p>
                    </div>
                    <ChevronDown className="h-4 w-4 text-slate-400 ml-1" />
                </button>
            </div>
        </header>
    );
}
