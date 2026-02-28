import React from 'react';
import { Search, Plus, Filter, MapPin, Edit2, Trash2, ChevronRight, Zap } from 'lucide-react';
import StatusBadge from '../../components/common/StatusBadge';

const stationsData = [
    { id: 'PLG-8821', name: 'Supercharger Downtown #A1', loc: 'Seattle, WA', status: 'Active', connect: 'CCS2 & Type 2', pwr: '150kW DC' },
    { id: 'PLG-9932', name: 'Metro Center North #B2', loc: 'Portland, OR', status: 'Maintenance', connect: 'GB_T', pwr: '50kW DC' },
    { id: 'PLG-1029', name: 'Westside Mall Parking #C1', loc: 'Bellevue, WA', status: 'Active', connect: 'CCS2', pwr: '350kW Ultra-Fast' },
    { id: 'PLG-3341', name: 'Airport Terminal 2 #D4', loc: 'SeaTac, WA', status: 'Offline', connect: 'Type 2', pwr: '22kW AC' },
    { id: 'PLG-5512', name: 'Tech Park South #E5', loc: 'Redmond, WA', status: 'Active', connect: 'CCS2', pwr: '150kW DC' },
];

export default function StationsList() {
    return (
        <div className="max-w-[1200px] mx-auto space-y-8">
            {/* Filters Bar */}
            <div className="flex flex-col md:flex-row gap-4 justify-between items-center">
                {/* Search */}
                <div className="relative w-full md:max-w-md">
                    <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                        <Search className="h-4 w-4 text-slate-400" />
                    </div>
                    <input
                        type="text"
                        className="block w-full pl-10 pr-4 py-2.5 bg-white border-0 shadow-sm shadow-slate-200/50 rounded-lg text-sm placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-cyan-500/20"
                        placeholder="Search stations by name, ID or location..."
                    />
                </div>

                {/* Dropdowns */}
                <div className="flex items-center gap-3 w-full md:w-auto overflow-x-auto pb-2 md:pb-0">
                    <select className="bg-slate-100 border-0 text-slate-700 text-sm font-medium py-2.5 px-4 pr-8 rounded-full focus:ring-0 cursor-pointer hover:bg-slate-200 transition-colors appearance-none bg-[url('data:image/svg+xml;charset=US-ASCII,%3Csvg%20width%3D%2224%22%20height%3D%2224%22%20viewBox%3D%220%200%2024%2024%22%20fill%3D%22none%22%20stroke%3D%22%23475569%22%20stroke-width%3D%222%22%20stroke-linecap%3D%22round%22%20stroke-linejoin%3D%22round%22%3E%3Cpolyline%20points%3D%226%209%2012%2015%2018%209%22%3E%3C%2Fpolyline%3E%3C%2Fsvg%3E')] bg-[length:16px_16px] bg-[right_12px_center] bg-no-repeat">
                        <option>Status</option>
                        <option>Active</option>
                        <option>Maintenance</option>
                        <option>Offline</option>
                    </select>

                    <select className="bg-slate-100 border-0 text-slate-700 text-sm font-medium py-2.5 px-4 pr-8 rounded-full focus:ring-0 cursor-pointer hover:bg-slate-200 transition-colors appearance-none bg-[url('data:image/svg+xml;charset=US-ASCII,%3Csvg%20width%3D%2224%22%20height%3D%2224%22%20viewBox%3D%220%200%2024%2024%22%20fill%3D%22none%22%20stroke%3D%22%23475569%22%20stroke-width%3D%222%22%20stroke-linecap%3D%22round%22%20stroke-linejoin%3D%22round%22%3E%3Cpolyline%20points%3D%226%209%2012%2015%2018%209%22%3E%3C%2Fpolyline%3E%3C%2Fsvg%3E')] bg-[length:16px_16px] bg-[right_12px_center] bg-no-repeat">
                        <option>Type</option>
                        <option>AC</option>
                        <option>DC Fast</option>
                    </select>

                    <select className="bg-slate-100 border-0 text-slate-700 text-sm font-medium py-2.5 px-4 pr-8 rounded-full focus:ring-0 cursor-pointer hover:bg-slate-200 transition-colors appearance-none bg-[url('data:image/svg+xml;charset=US-ASCII,%3Csvg%20width%3D%2224%22%20height%3D%2224%22%20viewBox%3D%220%200%2024%2024%22%20fill%3D%22none%22%20stroke%3D%22%23475569%22%20stroke-width%3D%222%22%20stroke-linecap%3D%22round%22%20stroke-linejoin%3D%22round%22%3E%3Cpolyline%20points%3D%226%209%2012%2015%2018%209%22%3E%3C%2Fpolyline%3E%3C%2Fsvg%3E')] bg-[length:16px_16px] bg-[right_12px_center] bg-no-repeat">
                        <option>Power</option>
                        <option>0-50 kW</option>
                        <option>50-150 kW</option>
                        <option>150+ kW</option>
                    </select>

                    <button className="bg-white border border-slate-200 p-2.5 rounded-full text-slate-500 hover:bg-slate-50 shadow-sm transition-colors">
                        <Filter className="w-4 h-4" />
                    </button>
                </div>
            </div>

            {/* Grid Headers Container (Hidden on mobile) */}
            <div className="hidden md:grid grid-cols-[3fr_2fr_1.5fr_2fr_auto] gap-4 px-6 text-xs font-bold text-slate-400 uppercase tracking-wider">
                <span>Station Details</span>
                <span>Location</span>
                <span>Status</span>
                <span>Connector</span>
                <span className="text-right w-24">Actions</span>
            </div>

            {/* Station Cards List */}
            <div className="space-y-4">
                {stationsData.map((st) => (
                    <div key={st.id} className="bg-white rounded-[24px] p-4 sm:p-6 shadow-[0_4px_24px_rgba(0,0,0,0.02)] border border-slate-100/50 flex flex-col md:grid md:grid-cols-[3fr_2fr_1.5fr_2fr_auto] md:items-center gap-4 transition-transform hover:-translate-y-1 hover:shadow-[0_8px_30px_rgba(0,0,0,0.04)]">

                        {/* Details Column */}
                        <div className="flex items-center gap-4">
                            <div className="w-12 h-12 rounded-2xl bg-cyan-50 flex items-center justify-center flex-shrink-0">
                                <Zap className="w-6 h-6 text-cyan-500" />
                            </div>
                            <div>
                                <h3 className="font-bold text-[#1A2234] text-base leading-tight">{st.name}</h3>
                                <p className="text-xs font-medium text-slate-400 tracking-wide mt-1">ID: {st.id}</p>
                            </div>
                        </div>

                        {/* Location Column */}
                        <div className="flex items-center gap-2 text-slate-600">
                            <MapPin className="w-4 h-4 text-slate-400" />
                            <span className="text-sm font-medium">{st.loc}</span>
                        </div>

                        {/* Status Column */}
                        <div>
                            <StatusBadge status={st.status} />
                        </div>

                        {/* Connector Column */}
                        <div>
                            <p className="font-semibold text-[#1A2234] text-sm">{st.connect}</p>
                            <p className="text-xs text-slate-400 mt-0.5">{st.pwr}</p>
                        </div>

                        {/* Actions Column */}
                        <div className="flex items-center justify-end gap-3 md:w-24 mt-4 md:mt-0 border-t border-slate-100 md:border-0 pt-4 md:pt-0">
                            <button className="p-2 text-slate-400 hover:text-cyan-600 transition-colors">
                                <Edit2 className="w-4 h-4" />
                            </button>
                            <button className="p-2 text-slate-400 hover:text-rose-600 transition-colors">
                                <Trash2 className="w-4 h-4" />
                            </button>
                            <button className="w-8 h-8 rounded-full bg-[#1A2234] hover:bg-slate-800 text-white flex items-center justify-center shadow-sm transition-colors cursor-pointer">
                                <ChevronRight className="w-4 h-4" />
                            </button>
                        </div>

                    </div>
                ))}
            </div>

        </div>
    );
}
