import React from 'react';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { Users, BatteryCharging, Calendar, AlertCircle, MapPin, UserPlus, Wrench, Download } from 'lucide-react';

import StatCard from '../../components/common/StatCard';
import QuickActionCard from '../../components/common/QuickActionCard';
import DataTable from '../../components/common/DataTable';
import StatusBadge from '../../components/common/StatusBadge';

const chartData = [
    { name: 'Jan', users: 1000 },
    { name: 'Feb', users: 1500 },
    { name: 'Mar', users: 2000 },
    { name: 'Apr', users: 2800 },
    { name: 'May', users: 4000 },
    { name: 'Jun', users: 5500 },
    { name: 'Jul', users: 8000 },
];

const recentActivityData = [
    { id: '#ST-4092', location: 'Downtown Plaza, L2', status: 'Charging', user: 'Sarah M.', charge: '24 kWh' },
    { id: '#ST-1029', location: 'Westside Mall, P4', status: 'Idle', user: 'David K.', charge: '-' },
    { id: '#ST-8832', location: 'Central Station, A1', status: 'Offline', user: 'System', charge: '-' },
];

const activityColumns = [
    { header: 'STATION ID', accessor: 'id', render: (row) => <span className="font-semibold text-slate-800">{row.id}</span> },
    { header: 'LOCATION', accessor: 'location' },
    { header: 'STATUS', accessor: 'status', render: (row) => <StatusBadge status={row.status} /> },
    {
        header: 'USER', accessor: 'user', render: (row) => (
            <div className="flex items-center gap-2">
                <div className="w-6 h-6 rounded-full bg-slate-200 flex-shrink-0" />
                <span>{row.user}</span>
            </div>
        )
    },
    { header: 'CHARGE', accessor: 'charge', render: (row) => <span className="font-medium text-slate-700">{row.charge}</span> },
];

export default function DashboardOverview() {
    return (
        <div className="space-y-6 max-w-[1600px] mx-auto">

            {/* Top Stat Cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                <StatCard
                    title="Total Users"
                    value="12,450"
                    icon={Users}
                    iconColor="bg-emerald-500"
                    trend="up"
                    trendValue="+12%"
                    trendLabel="vs last month"
                />
                <StatCard
                    title="Active Stations"
                    value="86"
                    icon={BatteryCharging}
                    iconColor="bg-blue-600"
                    trend="up"
                    trendValue="+5%"
                    trendLabel="vs last month"
                />
                <StatCard
                    title="Total Bookings"
                    value="3,204"
                    icon={Calendar}
                    iconColor="bg-amber-500"
                    trend="up"
                    trendValue="+18%"
                    trendLabel="vs last month"
                />
                <StatCard
                    title="Pending Approval"
                    value="14"
                    icon={AlertCircle}
                    iconColor="bg-rose-500"
                    trend="down"
                    trendValue="-2%"
                    trendLabel="vs last month"
                />
            </div>

            {/* Middle Section: Chart & Quick Actions */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Main Chart */}
                <div className="lg:col-span-2 bg-white rounded-2xl p-6 shadow-sm shadow-slate-200/50 border border-slate-100/50">
                    <div className="flex justify-between items-center mb-6">
                        <div>
                            <h3 className="text-lg font-bold text-[#1A2234]">User Growth Analytics</h3>
                            <p className="text-sm text-slate-500">Monthly user acquisition trend</p>
                        </div>
                        <div className="flex bg-slate-100 rounded-full p-1">
                            <button className="px-4 py-1.5 text-sm font-medium rounded-full text-slate-600 hover:text-slate-900">Weekly</button>
                            <button className="px-4 py-1.5 text-sm font-medium rounded-full bg-[#1A2234] text-white shadow-sm">Monthly</button>
                        </div>
                    </div>
                    <div className="h-[300px] w-full">
                        <ResponsiveContainer width="100%" height="100%">
                            <AreaChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                                <defs>
                                    <linearGradient id="colorUsers" x1="0" y1="0" x2="0" y2="1">
                                        <stop offset="5%" stopColor="#10b981" stopOpacity={0.3} />
                                        <stop offset="95%" stopColor="#10b981" stopOpacity={0} />
                                    </linearGradient>
                                </defs>
                                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                                <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fill: '#94a3b8', fontSize: 12 }} dy={10} />
                                <YAxis axisLine={false} tickLine={false} tick={{ fill: '#94a3b8', fontSize: 12 }} />
                                <Tooltip
                                    contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                                />
                                <Area type="monotone" dataKey="users" stroke="#10b981" strokeWidth={3} fillOpacity={1} fill="url(#colorUsers)" />
                            </AreaChart>
                        </ResponsiveContainer>
                    </div>
                </div>

                {/* Quick Actions */}
                <div className="bg-white rounded-2xl p-6 shadow-sm shadow-slate-200/50 border border-slate-100/50">
                    <h3 className="text-lg font-bold text-[#1A2234] mb-1">Quick Actions</h3>
                    <p className="text-sm text-slate-500 mb-6">Common management tasks</p>
                    <div className="grid grid-cols-2 gap-4">
                        <QuickActionCard title="Add Station" icon={MapPin} iconColor="bg-emerald-100 text-emerald-600" onClick={() => { }} />
                        <QuickActionCard title="New User" icon={UserPlus} iconColor="bg-blue-100 text-blue-600" onClick={() => { }} />
                        <QuickActionCard title="Maintenance" icon={Wrench} iconColor="bg-amber-100 text-amber-600" onClick={() => { }} />
                        <QuickActionCard title="Export Report" icon={Download} iconColor="bg-purple-100 text-purple-600" onClick={() => { }} />
                    </div>
                </div>
            </div>

            {/* Bottom Section: Activity Table */}
            <div className="bg-white rounded-2xl p-6 shadow-sm shadow-slate-200/50 border border-slate-100/50">
                <div className="flex justify-between items-center mb-6">
                    <h3 className="text-lg font-bold text-[#1A2234]">Recent Station Activity</h3>
                    <button className="text-emerald-600 font-semibold text-sm hover:text-emerald-700">View All</button>
                </div>
                <DataTable columns={activityColumns} data={recentActivityData} keyField="id" />
            </div>

        </div>
    );
}
