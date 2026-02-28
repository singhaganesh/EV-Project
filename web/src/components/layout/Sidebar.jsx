import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import {
    HelpCircle,
    LogOut,
    Zap
} from 'lucide-react';
import { logout } from '../../store/authSlice';

export default function Sidebar({ navItems = [], title = "Plugsy Admin", subtitle = "EV Management" }) {
    const dispatch = useDispatch();
    const navigate = useNavigate();

    const handleLogout = () => {
        dispatch(logout());
        navigate('/login', { replace: true });
    };

    return (
        <div className="w-64 bg-slate-900 h-screen flex flex-col text-slate-300 border-r border-slate-800 shrink-0">
            {/* Logo container */}
            <div className="h-24 flex items-center px-6 gap-3 pt-4">
                <div className="w-10 h-10 rounded-full bg-cyan-500 flex items-center justify-center flex-shrink-0">
                    <Zap className="text-white w-6 h-6" />
                </div>
                <div>
                    <h1 className="text-white font-semibold text-lg leading-tight">{title}</h1>
                    <p className="text-slate-400 text-xs text-brand-secondary">{subtitle}</p>
                </div>
            </div>

            {/* Navigation Links */}
            <nav className="flex-1 px-4 mt-8 space-y-1">
                {navItems.map((item) => (
                    <NavLink
                        key={item.name}
                        to={item.path}
                        end={item.end ?? true}
                        className={({ isActive }) =>
                            `flex items-center gap-4 px-4 py-3 rounded-xl transition-all duration-200 ${isActive
                                ? 'bg-cyan-500 text-slate-900 font-medium shadow-md shadow-cyan-500/20'
                                : 'hover:bg-slate-800 hover:text-white'
                            }`
                        }
                    >
                        <item.icon className="w-5 h-5" />
                        <span>{item.name}</span>
                    </NavLink>
                ))}
            </nav>

            {/* Need Help Box */}
            <div className="px-4 mb-4">
                <div className="bg-slate-800/50 rounded-2xl p-4 border border-slate-800">
                    <div className="flex items-center gap-3 mb-2 text-white">
                        <div className="w-8 h-8 rounded-full bg-emerald-500/20 flex items-center justify-center">
                            <HelpCircle className="w-4 h-4 text-emerald-500" />
                        </div>
                        <span className="font-medium text-sm">Need Help?</span>
                    </div>
                    <p className="text-xs text-slate-400 mb-3">
                        Contact support for any issues.
                    </p>
                    <button className="w-full bg-emerald-500 hover:bg-emerald-600 outline-none text-white text-sm font-medium py-2 rounded-lg transition-colors">
                        Contact Support
                    </button>
                </div>
            </div>

            {/* Log out */}
            <div className="px-4 pb-6 border-t border-slate-800 pt-4">
                <button
                    onClick={handleLogout}
                    className="flex items-center gap-4 px-4 py-3 w-full text-slate-400 hover:text-white transition-colors rounded-xl hover:bg-slate-800"
                >
                    <LogOut className="w-5 h-5" />
                    <span>Log Out</span>
                </button>
            </div>
        </div>
    );
}
