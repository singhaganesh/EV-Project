import React, { useState } from 'react';
import {
    BatteryCharging,
    MapPin,
    Zap,
    Wallet,
    AlertTriangle,
    ChevronDown,
    ChevronUp,
    Plus
} from 'lucide-react';

import StatCard from '../../components/common/StatCard';
import QuickActionCard from '../../components/common/QuickActionCard';
import StatusBadge from '../../components/common/StatusBadge';

// Mock Data structure reflecting Owner -> Stations -> Dispensers -> Connectors
const ownerData = {
    stats: {
        activeStations: 2,
        totalEnergy: "1,450 kWh",
        todayEarnings: "$342.50",
        alerts: 1
    },
    stations: [
        {
            id: 'ST-NW-01',
            name: 'Downtown Super C',
            location: '1200 4th Ave, Seattle',
            status: 'Active',
            dispensers: [
                {
                    id: 'DSP-01',
                    name: 'Dispenser 1 (Fast DC)',
                    status: 'Active',
                    connectors: [
                        { id: 'C1-A', type: 'CCS2', maxPower: '150kW', status: 'Charging', currentSession: '32m remaining' },
                        { id: 'C1-B', type: 'GB_T', maxPower: '50kW', status: 'Idle', currentSession: null }
                    ]
                },
                {
                    id: 'DSP-02',
                    name: 'Dispenser 2 (AC Level 2)',
                    status: 'Maintenance',
                    connectors: [
                        { id: 'C2-A', type: 'Type 2', maxPower: '22kW', status: 'Offline', currentSession: null },
                        { id: 'C2-B', type: 'Type 2', maxPower: '22kW', status: 'Offline', currentSession: null }
                    ]
                }
            ]
        },
        {
            id: 'ST-E-05',
            name: 'Bellevue Mall Hub',
            location: '500 Bellevue Way NE',
            status: 'Active',
            dispensers: [
                {
                    id: 'DSP-03',
                    name: 'Ultra Fast Hub A',
                    status: 'Active',
                    connectors: [
                        { id: 'C3-A', type: 'CCS2', maxPower: '350kW', status: 'Idle', currentSession: null }
                    ]
                }
            ]
        }
    ]
};

// Sub-component for a Dispenser row
const DispenserRow = ({ dispenser }) => {
    return (
        <div className="bg-slate-50 rounded-xl p-4 border border-slate-100 mt-3 ml-4 md:ml-12 relative before:absolute before:left-[-24px] before:top-8 before:w-5 before:h-px before:bg-slate-200">
            <div className="flex justify-between items-center mb-3">
                <div className="flex items-center gap-2">
                    <BatteryCharging className="w-4 h-4 text-slate-500" />
                    <h5 className="font-semibold text-sm text-slate-800">{dispenser.name}</h5>
                    <span className="text-xs text-slate-400 font-mono ml-2">{dispenser.id}</span>
                </div>
                <StatusBadge status={dispenser.status} />
            </div>

            {/* Connectors Grid */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                {dispenser.connectors.map(conn => (
                    <div key={conn.id} className="bg-white border border-slate-200 rounded-lg p-3 shadow-sm flex flex-col gap-1">
                        <div className="flex justify-between items-start">
                            <span className="font-bold text-[#1A2234] text-sm">{conn.type}</span>
                            <div className="flex items-center gap-1.5">
                                <div className={`w-2 h-2 rounded-full ${conn.status === 'Charging' ? 'bg-cyan-500 animate-pulse'
                                    : conn.status === 'Idle' ? 'bg-emerald-500'
                                        : 'bg-rose-500'
                                    }`} />
                                <span className="text-xs font-medium text-slate-500">{conn.status}</span>
                            </div>
                        </div>
                        <div className="flex justify-between items-end mt-1">
                            <span className="text-xs text-slate-400">{conn.id}</span>
                            <span className="text-xs font-semibold text-cyan-600 bg-cyan-50 px-2 py-0.5 rounded">{conn.maxPower}</span>
                        </div>
                        {conn.currentSession && (
                            <p className="text-xs text-amber-600 font-medium mt-1 bg-amber-50 px-2 py-1 rounded inline-block">
                                ⚡ {conn.currentSession}
                            </p>
                        )}
                    </div>
                ))}
            </div>
        </div>
    );
};


// Sub-component for a Station list item (Expandable)
const StationRow = ({ station }) => {
    const [expanded, setExpanded] = useState(true);

    return (
        <div className="bg-white rounded-2xl p-5 border border-slate-100 shadow-sm transition-all hover:shadow-md">
            <div
                className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 cursor-pointer"
                onClick={() => setExpanded(!expanded)}
            >
                <div className="flex items-center gap-4">
                    <div className="w-12 h-12 rounded-xl bg-[#1A2234] flex items-center justify-center flex-shrink-0">
                        <MapPin className="w-6 h-6 text-cyan-400" />
                    </div>
                    <div>
                        <div className="flex items-center gap-2">
                            <h3 className="font-bold text-[#1A2234] text-lg">{station.name}</h3>
                            <span className="text-xs text-slate-400 font-mono bg-slate-100 px-2 py-0.5 rounded">{station.id}</span>
                        </div>
                        <p className="text-sm font-medium text-slate-500 mt-1">{station.location}</p>
                    </div>
                </div>

                <div className="flex items-center gap-6 w-full sm:w-auto justify-between sm:justify-end">
                    <StatusBadge status={station.status} />
                    <div className="flex items-center gap-2 text-slate-400">
                        <span className="text-sm font-semibold text-slate-600">{station.dispensers.length}</span>
                        <span className="text-xs uppercase tracking-wider">Dispensers</span>
                        <button className="p-1 hover:bg-slate-100 rounded-full transition-colors ml-2">
                            {expanded ? <ChevronUp className="w-5 h-5" /> : <ChevronDown className="w-5 h-5" />}
                        </button>
                    </div>
                </div>
            </div>

            {/* Expanded Dispensers Area */}
            {expanded && (
                <div className="mt-4 border-t border-slate-100 pt-2 relative">
                    <div className="absolute left-6 top-2 bottom-4 w-px bg-slate-200 hidden md:block" />
                    {station.dispensers.map((disp, idx) => (
                        <DispenserRow key={disp.id} dispenser={disp} />
                    ))}
                    <div className="mt-4 ml-4 md:ml-12 pl-6">
                        <button className="text-sm font-semibold text-cyan-600 flex items-center gap-1 hover:text-cyan-700 transition-colors">
                            <Plus className="w-4 h-4" /> Add Dispenser to {station.name}
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
};


export default function PumpOwnerDashboard() {
    return (
        <div className="space-y-8 max-w-[1400px] mx-auto">
            {/* Top Stat Cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                <StatCard
                    title="My Active Stations"
                    value={ownerData.stats.activeStations}
                    icon={MapPin}
                    iconColor="bg-blue-600"
                    trend="up"
                    trendValue="+1"
                    trendLabel="since last month"
                />
                <StatCard
                    title="Energy Dispensed (24h)"
                    value={ownerData.stats.totalEnergy}
                    icon={Zap}
                    iconColor="bg-cyan-500"
                    trend="up"
                    trendValue="+12%"
                    trendLabel="vs yesterday"
                />
                <StatCard
                    title="Today's Earnings"
                    value={ownerData.stats.todayEarnings}
                    icon={Wallet}
                    iconColor="bg-emerald-500"
                    trend="up"
                    trendValue="+4%"
                    trendLabel="vs yesterday"
                />
                <StatCard
                    title="Hardware Alerts"
                    value={ownerData.stats.alerts}
                    icon={AlertTriangle}
                    iconColor="bg-rose-500"
                    trend="down"
                    trendValue="-2"
                    trendLabel="vs last week"
                />
            </div>

            {/* Quick Actions specific to Owner */}
            <div className="bg-white rounded-2xl p-6 shadow-sm shadow-slate-200/50 border border-slate-100/50">
                <div className="mb-4">
                    <h3 className="text-lg font-bold text-[#1A2234]">Quick Actions</h3>
                </div>
                <div className="flex flex-wrap gap-4">
                    <button className="flex items-center gap-2 bg-[#1A2234] hover:bg-slate-800 text-white px-5 py-2.5 rounded-full font-semibold text-sm transition-colors shadow-sm">
                        <Plus className="w-4 h-4" /> Add New Station
                    </button>
                    <button className="flex items-center gap-2 bg-white hover:bg-slate-50 text-slate-700 border border-slate-200 px-5 py-2.5 rounded-full font-semibold text-sm transition-colors shadow-sm">
                        <Wallet className="w-4 h-4 text-emerald-500" /> View Payouts
                    </button>
                    <button className="flex items-center gap-2 bg-white hover:bg-slate-50 text-slate-700 border border-slate-200 px-5 py-2.5 rounded-full font-semibold text-sm transition-colors shadow-sm">
                        <AlertTriangle className="w-4 h-4 text-rose-500" /> Request Maintenance
                    </button>
                </div>
            </div>

            {/* Stations Tree View */}
            <div className="space-y-4">
                <h3 className="text-xl font-bold text-[#1A2234] mb-2">My Stations Fleet</h3>
                {ownerData.stations.map(station => (
                    <StationRow key={station.id} station={station} />
                ))}
            </div>

        </div>
    );
}
