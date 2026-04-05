import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import {
    BatteryCharging,
    MapPin,
    Zap,
    Wallet,
    AlertTriangle,
    ChevronDown,
    ChevronUp,
    Plus,
    Activity,
    ArrowRight
} from 'lucide-react';
import {
    AreaChart,
    Area,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    ResponsiveContainer
} from 'recharts';

import StatCard from '../../components/common/StatCard';
import StatusBadge from '../../components/common/StatusBadge';
import api from '../../api/axios';
import { 
    fetchRevenueTrends, 
    selectRevenueTrends 
} from '../../store/stationSlice';

// Sub-component for a Dispenser row
const DispenserRow = ({ dispenser }) => {
    return (
        <div className="bg-slate-50 rounded-xl p-4 border border-slate-100 mt-3 ml-4 md:ml-12 relative before:absolute before:left-[-24px] before:top-8 before:w-5 before:h-px before:bg-slate-200">
            <div className="flex justify-between items-center mb-3">
                <div className="flex items-center gap-2">
                    <BatteryCharging className="w-4 h-4 text-slate-500" />
                    <h5 className="font-semibold text-sm text-slate-800">{dispenser.name}</h5>
                    <span className="text-xs text-slate-400 font-mono ml-2">#{dispenser.id}</span>
                </div>
                <StatusBadge status={dispenser.status || 'Active'} />
            </div>

            <div className="flex gap-4">
                <div className="bg-white px-3 py-2 rounded-lg border border-slate-200 shadow-sm flex items-center gap-2">
                    <div className="w-2 h-2 rounded-full bg-emerald-500"></div>
                    <span className="text-xs font-medium text-slate-600">{dispenser.numberOfGuns || 2} Guns Available</span>
                </div>
                <div className="bg-white px-3 py-2 rounded-lg border border-slate-200 shadow-sm flex items-center gap-2">
                    <Zap className="w-3 h-3 text-cyan-500" />
                    <span className="text-xs font-medium text-slate-600">{dispenser.totalPowerKw} kW Total</span>
                </div>
            </div>
        </div>
    );
};


// Sub-component for a Station list item (Expandable)
const StationRow = ({ station }) => {
    const [expanded, setExpanded] = useState(false);

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
                            <span className="text-xs text-slate-400 font-mono bg-slate-100 px-2 py-0.5 rounded">#{station.id}</span>
                        </div>
                        <p className="text-sm font-medium text-slate-500 mt-1">{station.address}</p>
                    </div>
                </div>

                <div className="flex items-center gap-6 w-full sm:w-auto justify-between sm:justify-end">
                    <StatusBadge status={station.isOpen ? 'Active' : 'Closed'} />
                    <div className="flex items-center gap-2 text-slate-400">
                        <span className="text-sm font-semibold text-slate-600">{station.dispensaries?.length || 0}</span>
                        <span className="text-xs uppercase tracking-wider">Dispensers</span>
                        <button className="p-1 hover:bg-slate-100 rounded-full transition-colors ml-2">
                            {expanded ? <ChevronUp className="w-5 h-5" /> : <ChevronDown className="w-5 h-5" />}
                        </button>
                    </div>
                </div>
            </div>

            {expanded && (
                <div className="mt-4 border-t border-slate-100 pt-2 relative">
                    <div className="absolute left-6 top-2 bottom-4 w-px bg-slate-200 hidden md:block" />
                    {station.dispensaries && station.dispensaries.length > 0 ? (
                        station.dispensaries.map((disp) => (
                            <DispenserRow key={disp.id} dispenser={disp} />
                        ))
                    ) : (
                        <p className="text-xs text-slate-400 mt-4 ml-12">No dispensaries configured for this station.</p>
                    )}
                </div>
            )}
        </div>
    );
};


export default function PumpOwnerDashboard() {
    const dispatch = useDispatch();
    const navigate = useNavigate();
    const revenueTrends = useSelector(selectRevenueTrends);
    
    const [stats, setStats] = useState({
        totalStations: 0,
        activeStationsCount: 0,
        todayEnergyKwh: 0,
        todayEarnings: 0,
        utilizationRate: 0
    });
    const [stations, setStations] = useState([]);
    const [loading, setLoading] = useState(true);

    const fetchDashboardData = async () => {
        try {
            setLoading(true);
            const userStr = localStorage.getItem('user');
            if (!userStr) return;
            const user = JSON.parse(userStr);

            const [statsRes, stationsRes] = await Promise.all([
                api.get(`/stations/owner/${user.id}/stats`),
                api.get(`/stations/owner/${user.id}`)
            ]);

            const statsData = statsRes.data?.data || statsRes.data || {};
            setStats({
                totalStations: statsData.totalStations || 0,
                activeStationsCount: statsData.activeStationsCount || 0,
                todayEnergyKwh: statsData.todayEnergyKwh || 0,
                todayEarnings: statsData.todayEarnings || 0,
                utilizationRate: statsData.utilizationRate || 0
            });

            const stationList = Array.isArray(stationsRes.data) ? stationsRes.data : (stationsRes.data?.data || []);
            setStations(stationList);

            // Fetch a simple 7-day trend for the "Pulse" sparkline
            dispatch(fetchRevenueTrends({ ownerId: user.id, days: 7 }));
        } catch (error) {
            console.error('Error fetching dashboard data:', error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchDashboardData();
    }, [dispatch]);

    if (loading) {
        return (
            <div className="flex items-center justify-center h-[60vh]">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-cyan-500"></div>
            </div>
        );
    }

    return (
        <div className="space-y-8 max-w-[1400px] mx-auto pb-12">
            {/* Top Stat Cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                <StatCard
                    title="Active Stations"
                    value={`${stats.activeStationsCount} / ${stats.totalStations}`}
                    icon={MapPin}
                    iconColor="bg-blue-600"
                    trend={stats.activeStationsCount === stats.totalStations ? 'up' : 'down'}
                    trendValue={stats.activeStationsCount === stats.totalStations ? "All Online" : "Action Needed"}
                    trendLabel=""
                />
                <StatCard
                    title="Energy Dispensed (Today)"
                    value={`${stats.todayEnergyKwh.toFixed(1)} kWh`}
                    icon={Zap}
                    iconColor="bg-cyan-500"
                    trend="up"
                    trendValue="Live"
                    trendLabel="Updating real-time"
                />
                <StatCard
                    title="Today's Earnings"
                    value={`₹${stats.todayEarnings.toLocaleString('en-IN')}`}
                    icon={Wallet}
                    iconColor="bg-emerald-500"
                    trend="up"
                    trendValue="Today"
                    trendLabel="Gross Revenue"
                />
                <StatCard
                    title="Hardware Alerts"
                    value="0"
                    icon={AlertTriangle}
                    iconColor="bg-rose-500"
                    trend="down"
                    trendValue="-2"
                    trendLabel="vs last week"
                />
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                {/* Left Side: Quick Actions & Sparkline */}
                <div className="lg:col-span-1 space-y-8">
                    {/* Revenue Sparkline (The Pulse) */}
                    <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100/50">
                        <div className="flex items-center justify-between mb-4">
                            <div>
                                <h3 className="text-sm font-bold text-slate-400 uppercase tracking-wider">Weekly Trend</h3>
                                <p className="text-lg font-bold text-[#1A2234]">Revenue</p>
                            </div>
                            <button 
                                onClick={() => navigate('/owner/analytics')}
                                className="p-2 hover:bg-slate-50 rounded-xl transition-colors text-cyan-600"
                            >
                                <ArrowRight className="w-5 h-5" />
                            </button>
                        </div>
                        
                        <div className="h-[250px] w-full">
                            <ResponsiveContainer width="100%" height="100%">
                                <AreaChart data={revenueTrends} margin={{ top: 5, right: 5, left: -20, bottom: 0 }}>
                                    <defs>
                                        <linearGradient id="colorPulse" x1="0" y1="0" x2="0" y2="1">
                                            <stop offset="5%" stopColor="#06b6d4" stopOpacity={0.2}/>
                                            <stop offset="95%" stopColor="#06b6d4" stopOpacity={0}/>
                                        </linearGradient>
                                    </defs>
                                    <XAxis 
                                        dataKey="date" 
                                        hide={false}
                                        axisLine={false}
                                        tickLine={false}
                                        tick={{fill: '#94a3b8', fontSize: 10, fontWeight: 600}}
                                        tickFormatter={(date) => new Date(date).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })}
                                    />
                                    <YAxis 
                                        hide={false}
                                        axisLine={false}
                                        tickLine={false}
                                        tick={{fill: '#94a3b8', fontSize: 10, fontWeight: 600}}
                                        tickFormatter={(val) => `₹${val}`}
                                    />
                                    <Area
                                        type="monotone"
                                        dataKey="revenue"
                                        stroke="#06b6d4"
                                        strokeWidth={3}
                                        fillOpacity={1}
                                        fill="url(#colorPulse)"
                                    />
                                    <Tooltip 
                                        content={({ active, payload }) => {
                                            if (active && payload && payload.length) {
                                                return (
                                                    <div className="bg-[#1A2234] text-white px-3 py-1.5 rounded-lg text-xs font-bold shadow-xl">
                                                        ₹{payload[0].value.toLocaleString('en-IN')}
                                                    </div>
                                                );
                                            }
                                            return null;
                                        }}
                                    />
                                </AreaChart>
                            </ResponsiveContainer>
                        </div>
                    </div>

                    {/* Quick Actions */}
                    <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100/50">
                        <h3 className="text-lg font-bold text-[#1A2234] mb-4">Quick Actions</h3>
                        <div className="space-y-3">
                            <button className="w-full flex items-center justify-between bg-[#1A2234] hover:bg-slate-800 text-white p-4 rounded-2xl font-semibold text-sm transition-all shadow-sm group">
                                <span className="flex items-center gap-3"><Plus className="w-5 h-5 text-cyan-400" /> Add Station</span>
                                <ArrowRight className="w-4 h-4 opacity-0 group-hover:opacity-100 -translate-x-2 group-hover:translate-x-0 transition-all" />
                            </button>
                            <button className="w-full flex items-center justify-between bg-slate-50 hover:bg-slate-100 text-slate-700 p-4 rounded-2xl font-semibold text-sm transition-all group">
                                <span className="flex items-center gap-3"><Wallet className="w-5 h-5 text-emerald-500" /> View Payouts</span>
                                <ArrowRight className="w-4 h-4 opacity-0 group-hover:opacity-100 -translate-x-2 group-hover:translate-x-0 transition-all" />
                            </button>
                            <button className="w-full flex items-center justify-between bg-slate-50 hover:bg-slate-100 text-slate-700 p-4 rounded-2xl font-semibold text-sm transition-all group">
                                <span className="flex items-center gap-3"><AlertTriangle className="w-5 h-5 text-rose-500" /> Maintenance</span>
                                <ArrowRight className="w-4 h-4 opacity-0 group-hover:opacity-100 -translate-x-2 group-hover:translate-x-0 transition-all" />
                            </button>
                        </div>
                    </div>
                </div>

                {/* Right Side: Stations Fleet (Full width on mobile, 2/3 on desktop) */}
                <div className="lg:col-span-2 space-y-4">
                    <div className="flex items-center justify-between mb-2">
                        <h3 className="text-xl font-bold text-[#1A2234]">My Stations Fleet</h3>
                        <span className="bg-cyan-100 text-cyan-700 text-xs font-bold px-3 py-1 rounded-full">
                            {stations.length} STATIONS
                        </span>
                    </div>
                    
                    {stations.length > 0 ? (
                        <div className="grid grid-cols-1 gap-4">
                            {stations.map(station => (
                                <StationRow key={station.id} station={station} />
                            ))}
                        </div>
                    ) : (
                        <div className="bg-slate-50 rounded-3xl p-12 text-center border-2 border-dashed border-slate-200">
                            <MapPin className="w-12 h-12 text-slate-300 mx-auto mb-4" />
                            <p className="text-slate-500 font-medium">You haven't registered any stations yet.</p>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
