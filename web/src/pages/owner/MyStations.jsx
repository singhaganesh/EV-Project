import React from 'react';
import { Search, Filter, Map, Zap, Activity, MoreHorizontal, ArrowRight, Plus, MapPin, AlertTriangle } from 'lucide-react';
import StatusBadge from '../../components/common/StatusBadge';

const stationsData = [
    {
        id: '#ST-001',
        name: 'Downtown Metro Hub',
        location: '123 Main St, Metro City',
        status: 'ONLINE',
        chargers: { total: 8, available: 0 },
        power: '150 kW DC',
        image: 'https://images.unsplash.com/photo-1620986144865-c9676771d1e4?q=80&w=250&auto=format&fit=crop',
        usersCount: 3
    },
    {
        id: '#ST-042',
        name: 'Westside Mall Plaza',
        location: '456 West Ave, Metro City',
        status: 'MAINTENANCE',
        chargers: { total: 4, offline: 4 },
        power: '50 kW AC',
        image: 'https://images.unsplash.com/photo-1662973166687-fa741c039fb6?q=80&w=250&auto=format&fit=crop',
        alert: 'Technician Scheduled'
    },
    {
        id: '#ST-289',
        name: 'Northside Supercharger',
        location: '88 North Loop, Metro City',
        status: 'ONLINE',
        chargers: { total: 12, available: 5 },
        power: '350 kW DC',
        image: 'https://images.unsplash.com/photo-1593941707882-a5bba14938c7?q=80&w=250&auto=format&fit=crop',
        usersCount: 1
    }
];

export default function MyStations() {
    return (
        <div className="space-y-8 max-w-[1400px] mx-auto pb-12">

            {/* Action Bar */}
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <div className="relative w-full sm:max-w-md">
                    <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                        <Search className="h-4 w-4 text-slate-400" />
                    </div>
                    <input
                        type="text"
                        className="block w-full pl-10 pr-4 py-3 bg-white border-0 shadow-[0_2px_10px_rgba(0,0,0,0.04)] rounded-full text-sm placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-cyan-500/20"
                        placeholder="Search by station name, ID, or location..."
                    />
                </div>

                <div className="flex gap-3 w-full sm:w-auto">
                    <button className="flex-1 sm:flex-none flex items-center justify-center gap-2 bg-white border border-slate-100 shadow-[0_2px_10px_rgba(0,0,0,0.04)] text-slate-700 px-5 py-3 rounded-full font-medium text-sm hover:bg-slate-50 transition-colors">
                        <Filter className="w-4 h-4" />
                        Filters
                    </button>
                    <button className="flex-1 sm:flex-none flex items-center justify-center gap-2 bg-white border border-slate-100 shadow-[0_2px_10px_rgba(0,0,0,0.04)] text-slate-700 px-5 py-3 rounded-full font-medium text-sm hover:bg-slate-50 transition-colors">
                        <Map className="w-4 h-4" />
                        Map View
                    </button>
                    <button className="w-full sm:w-auto flex items-center justify-center gap-2 px-6 py-3 bg-[#00E5FF] hover:bg-[#00B4DB] text-[#1A2234] font-bold rounded-full shadow-lg shadow-[#00E5FF]/20 transition-all sm:ml-2">
                        <Plus className="w-5 h-5" />
                        Add New Station
                    </button>
                </div>
            </div>

            {/* Quick Stats Pills */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="bg-white rounded-[24px] p-6 shadow-[0_2px_12px_rgba(0,0,0,0.03)] border border-slate-100/50 flex items-center justify-between">
                    <div>
                        <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-1">Total Stations</p>
                        <h2 className="text-3xl font-bold text-[#1A2234]">124</h2>
                    </div>
                    <div className="w-12 h-12 rounded-full bg-blue-50 flex items-center justify-center">
                        <MapPin className="w-5 h-5 text-blue-500" />
                    </div>
                </div>

                <div className="bg-white rounded-[24px] p-6 shadow-[0_2px_12px_rgba(0,0,0,0.03)] border border-slate-100/50 flex items-center justify-between">
                    <div>
                        <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-1">Active Chargers</p>
                        <h2 className="text-3xl font-bold text-[#1A2234]">892</h2>
                    </div>
                    <div className="w-12 h-12 rounded-full bg-emerald-50 flex items-center justify-center">
                        <Zap className="w-5 h-5 text-emerald-500" />
                    </div>
                </div>

                <div className="bg-white rounded-[24px] p-6 shadow-[0_2px_12px_rgba(0,0,0,0.03)] border border-slate-100/50 flex items-center justify-between">
                    <div>
                        <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-1">Utilization Rate</p>
                        <h2 className="text-3xl font-bold text-[#1A2234]">78%</h2>
                    </div>
                    <div className="w-12 h-12 rounded-full bg-purple-50 flex items-center justify-center">
                        <Activity className="w-5 h-5 text-purple-500" />
                    </div>
                </div>
            </div>

            {/* Station Cards Grid */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {stationsData.map((station) => {
                    const isOnline = station.status === 'ONLINE';

                    return (
                        <div key={station.id} className="bg-white rounded-[24px] p-6 shadow-[0_4px_20px_rgba(0,0,0,0.03)] border border-slate-100/50 relative overflow-hidden group hover:shadow-[0_8px_30px_rgba(0,0,0,0.06)] transition-all">

                            {/* Top Color Bar */}
                            <div className={`absolute top-0 left-6 right-6 h-1.5 rounded-b-md ${isOnline ? 'bg-[#00E5FF]' : 'bg-amber-400'}`} />

                            {/* Header */}
                            <div className="flex justify-between items-center mt-2 mb-6">
                                <div className={`px-2.5 py-1 rounded-full text-[10px] font-bold tracking-wider flex items-center gap-1.5
                                    ${isOnline ? 'bg-emerald-50 text-emerald-600' : 'bg-amber-50 text-amber-600'}`}
                                >
                                    <div className={`w-1.5 h-1.5 rounded-full ${isOnline ? 'bg-emerald-500' : 'bg-amber-500'}`} />
                                    {station.status}
                                </div>
                                <button className="text-slate-400 hover:text-slate-600 transition-colors">
                                    <MoreHorizontal className="w-5 h-5" />
                                </button>
                            </div>

                            {/* Main Info */}
                            <div className="flex items-center gap-5 mb-8">
                                <div className="w-20 h-20 rounded-2xl overflow-hidden shrink-0 shadow-sm">
                                    <img src={station.image} alt={station.name} className="w-full h-full object-cover" />
                                </div>
                                <div>
                                    <h3 className="text-lg font-bold text-[#1A2234] leading-tight mb-1">{station.name}</h3>
                                    <div className="flex items-center gap-1.5 text-slate-500 text-sm mb-1.5">
                                        <MapPin className="w-3.5 h-3.5" />
                                        {station.location}
                                    </div>
                                    <span className="text-xs font-mono font-medium text-slate-400 bg-slate-50 px-2 py-0.5 rounded">ID: {station.id}</span>
                                </div>
                            </div>

                            {/* Inner Stats Pills */}
                            <div className="flex gap-4 mb-6">
                                <div className="flex-1 bg-slate-50 rounded-2xl p-4 border border-slate-100/50">
                                    <p className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider mb-1">Chargers</p>
                                    <div className="flex items-baseline gap-1">
                                        <span className="text-xl font-bold text-[#1A2234]">{station.chargers.total}</span>
                                        <span className="text-xs font-medium text-slate-500">
                                            {isOnline ? `/ ${station.chargers.available} Avail` : `/ ${station.chargers.offline} Offline`}
                                        </span>
                                    </div>
                                </div>
                                <div className="flex-1 bg-slate-50 rounded-2xl p-4 border border-slate-100/50">
                                    <p className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider mb-1">Power Output</p>
                                    <div className="flex items-baseline gap-1">
                                        <span className="text-xl font-bold text-[#1A2234]">{station.power.split(' ')[0]}</span>
                                        <span className="text-xs font-medium text-slate-500">{station.power.split(' ').slice(1).join(' ')}</span>
                                    </div>
                                </div>
                            </div>

                            {/* Footer Actions */}
                            <div className="flex items-center justify-between pt-2">
                                {isOnline ? (
                                    <div className="flex items-center">
                                        {/* Fake Avatar Stack */}
                                        <div className="flex -space-x-2">
                                            <div className="w-7 h-7 rounded-full bg-slate-200 border-2 border-white overflow-hidden"><img src="https://i.pravatar.cc/100?img=1" alt="" /></div>
                                            <div className="w-7 h-7 rounded-full bg-slate-200 border-2 border-white overflow-hidden"><img src="https://i.pravatar.cc/100?img=2" alt="" /></div>
                                        </div>
                                        <span className="text-xs font-medium text-slate-500 ml-2">+{station.usersCount}</span>
                                    </div>
                                ) : (
                                    <div className="flex items-center gap-1.5 text-xs font-medium text-amber-600 bg-amber-50 px-3 py-1.5 rounded-lg">
                                        <AlertTriangle className="w-3.5 h-3.5" />
                                        {station.alert}
                                    </div>
                                )}

                                <button className="flex items-center gap-1.5 text-sm font-bold text-[#00B4DB] hover:text-[#00E5FF] transition-colors group-hover:translate-x-1 duration-300">
                                    Manage
                                    <ArrowRight className="w-4 h-4" />
                                </button>
                            </div>

                        </div>
                    );
                })}

                {/* Add New Station Card */}
                <button className="bg-slate-50/50 rounded-[24px] p-6 border-2 border-dashed border-slate-200 flex flex-col items-center justify-center min-h-[320px] hover:border-cyan-300 hover:bg-cyan-50/20 transition-all group">
                    <div className="w-16 h-16 rounded-full bg-white shadow-sm flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
                        <Plus className="w-6 h-6 text-slate-400 group-hover:text-cyan-500 transition-colors" />
                    </div>
                    <span className="font-bold text-slate-600 group-hover:text-[#1A2234] transition-colors">Register New Location</span>
                </button>

            </div>

        </div>
    );
}
