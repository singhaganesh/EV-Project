import React from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import Header from './Header';

export default function DashboardLayout({ navItems, sidebarTitle }) {
    return (
        <div className="flex h-screen bg-[#F4F7FE] overflow-hidden">
            {/* Sidebar (Fixed width) */}
            <Sidebar navItems={navItems} title={sidebarTitle} />

            {/* Main Content Area */}
            <div className="flex-1 flex flex-col h-screen overflow-hidden">
                {/* Top Header */}
                <Header />

                {/* Scrollable Page Content */}
                <main className="flex-1 overflow-x-hidden overflow-y-auto bg-[#F4F7FE] p-8 pt-2">
                    {/* Outlet renders the child routes (e.g., Dashboard, Stations, etc.) */}
                    <Outlet />
                </main>
            </div>
        </div>
    );
}
