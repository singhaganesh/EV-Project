import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
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
    Calendar,
    Clock
} from 'lucide-react';
import {
    AreaChart,
    Area,
    BarChart,
    Bar,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    ResponsiveContainer,
    Cell
} from 'recharts';

import StatCard from '../../components/common/StatCard';
import StatusBadge from '../../components/common/StatusBadge';
import api from '../../api/axios';
import { 
    fetchRevenueTrends, 
    selectRevenueTrends, 
    fetchPeakUsage, 
    selectPeakUsage 
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
    const revenueTrends = useSelector(selectRevenueTrends);
    const peakUsage = useSelector(selectPeakUsage);
    
    const [stats, setStats] = useState({
        totalStations: 0,
        activeStationsCount: 0,
        todayEnergyKwh: 0,
        todayEarnings: 0,
        utilizationRate: 0
    });
    const [stations, setStations] = useState([]);
    const [loading, setLoading] = useState(true);
    
    // Independent filters for each chart
    const [revenueDays, setRevenueDays] = useState(7);
    const [peakDays, setPeakDays] = useState(0); // Default to Today

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
        } catch (error) {
            console.error('Error fetching dashboard data:', error);
        } finally {
            setLoading(false);
        }
    };

    // Load static dashboard data once
    useEffect(() => {
        fetchDashboardData();
    }, []);

    // Fetch Revenue Trends when revenueDays filter changes
    useEffect(() => {
        const userStr = localStorage.getItem('user');
        if (!userStr) return;
        const user = JSON.parse(userStr);
        dispatch(fetchRevenueTrends({ ownerId: user.id, days: revenueDays }));
    }, [dispatch, revenueDays]);

    // Fetch Peak Usage when peakDays filter changes
    useEffect(() => {
        const userStr = localStorage.getItem('user');
        if (!userStr) return;
        const user = JSON.parse(userStr);
        dispatch(fetchPeakUsage({ ownerId: user.id, days: peakDays }));
    }, [dispatch, peakDays]);

    if (loading) {
        return (
            <div className="flex items-center justify-center h-[60vh]">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-cyan-500"></div>
            </div>
        );
    }

    const formatChartDate = (dateStr) => {
        const date = new Date(dateStr);
        return date.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
    };

    const formatHour = (hour) => {
        const h = hour % 12 || 12;
        const ampm = hour < 12 ? 'AM' : 'PM';
        return `${h}${ampm}`;
    };

    const CustomTooltip = ({ active, payload, label }) => {
        if (active && payload && payload.length) {
            return (
                <div className="bg-white p-4 border border-slate-100 shadow-xl rounded-2xl">
                    <p className="text-xs font-bold text-slate-400 uppercase mb-2">{formatChartDate(label)}</p>
                    <div className="space-y-1">
                        <p className="text-sm font-bold text-emerald-600">
                            Revenue: ₹{payload[0].value.toLocaleString('en-IN')}
                        </p>
                        <p className="text-sm font-bold text-cyan-600">
                            Energy: {payload[1].value.toFixed(1)} kWh
                        </p>
                    </div>
                </div>
            );
        }
        return null;
    };

    const CustomPeakTooltip = ({ active, payload, label }) => {
        if (active && payload && payload.length) {
            return (
                <div className="bg-white p-4 border border-slate-100 shadow-xl rounded-2xl">
                    <p className="text-xs font-bold text-slate-400 uppercase mb-2">{formatHour(label)} Window</p>
                    <p className="text-sm font-bold text-[#6366f1]">
                        Sessions: {payload[0].value}
                    </p>
                </div>
            );
        }
        return null;
    };

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

            {/* Main Analytics: Revenue & Energy Trends */}
            <div className="bg-white rounded-3xl p-8 shadow-sm shadow-slate-200/50 border border-slate-100/50">
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
                    <div>
                        <h3 className="text-xl font-bold text-[#1A2234]">Revenue & Energy Trends</h3>
                        <p className="text-sm text-slate-500 font-medium mt-1">Growth analysis over time</p>
                    </div>
                    
                    {/* Independent Filter for Revenue */}
                    <div className="flex bg-slate-50 p-1 rounded-xl border border-slate-100">
                        {[7, 14, 30].map((d) => (
                            <button
                                key={d}
                                onClick={() => setRevenueDays(d)}
                                className={`px-4 py-2 rounded-lg text-xs font-bold transition-all ${
                                    revenueDays === d 
                                    ? 'bg-white text-[#1A2234] shadow-sm border border-slate-100' 
                                    : 'text-slate-400 hover:text-slate-600'
                                }`}
                            >
                                {d} Days
                            </button>
                        ))}
                    </div>
                </div>

                <div className="h-[350px] w-full">
                    <ResponsiveContainer width="100%" height="100%">
                        <AreaChart data={revenueTrends}>
                            <defs>
                                <linearGradient id="colorRevenue" x1="0" y1="0" x2="0" y2="1">
                                    <stop offset="5%" stopColor="#10b981" stopOpacity={0.1}/>
                                    <stop offset="95%" stopColor="#10b981" stopOpacity={0}/>
                                </linearGradient>
                                <linearGradient id="colorEnergy" x1="0" y1="0" x2="0" y2="1">
                                    <stop offset="5%" stopColor="#06b6d4" stopOpacity={0.1}/>
                                    <stop offset="95%" stopColor="#06b6d4" stopOpacity={0}/>
                                </linearGradient>
                            </defs>
                            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                            <XAxis 
                                dataKey="date" 
                                tickFormatter={formatChartDate}
                                axisLine={false}
                                tickLine={false}
                                tick={{fill: '#94a3b8', fontSize: 12, fontWeight: 600}}
                                dy={10}
                            />
                            <YAxis 
                                yAxisId="left"
                                axisLine={false}
                                tickLine={false}
                                tick={{fill: '#94a3b8', fontSize: 12, fontWeight: 600}}
                                tickFormatter={(value) => `₹${value}`}
                            />
                            <YAxis 
                                yAxisId="right" 
                                orientation="right"
                                axisLine={false}
                                tickLine={false}
                                tick={{fill: '#94a3b8', fontSize: 12, fontWeight: 600}}
                                tickFormatter={(value) => `${value}kWh`}
                            />
                            <Tooltip content={<CustomTooltip />} cursor={{stroke: '#e2e8f0', strokeWidth: 2}} />
                            <Area
                                yAxisId="left"
                                type="monotone"
                                dataKey="revenue"
                                stroke="#10b981"
                                strokeWidth={3}
                                fillOpacity={1}
                                fill="url(#colorRevenue)"
                                name="Revenue"
                            />
                            <Area
                                yAxisId="right"
                                type="monotone"
                                dataKey="energy"
                                stroke="#06b6d4"
                                strokeWidth={3}
                                fillOpacity={1}
                                fill="url(#colorEnergy)"
                                name="Energy"
                            />
                        </AreaChart>
                    </ResponsiveContainer>
                </div>
            </div>

            {/* Peak Hours Analysis (Stacked Below) */}
            <div className="bg-white rounded-3xl p-8 shadow-sm shadow-slate-200/50 border border-slate-100/50">
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
                    <div>
                        <h3 className="text-xl font-bold text-[#1A2234]">
                            {peakDays === 0 ? "Today's Peak Usage" : "Peak Usage Patterns"}
                        </h3>
                        <p className="text-sm text-slate-500 font-medium mt-1">
                            {peakDays === 0 ? "Live charging sessions by hour" : `Hourly trends over the last ${peakDays} days`}
                        </p>
                    </div>
                    
                    {/* Independent Filter for Peak Hours */}
                    <div className="flex bg-slate-50 p-1 rounded-xl border border-slate-100">
                        {[0, 7, 14, 30].map((d) => (
                            <button
                                key={d}
                                onClick={() => setPeakDays(d)}
                                className={`px-4 py-2 rounded-lg text-xs font-bold transition-all flex items-center gap-2 ${
                                    peakDays === d 
                                    ? 'bg-white text-[#1A2234] shadow-sm border border-slate-100' 
                                    : 'text-slate-400 hover:text-slate-600'
                                }`}
                            >
                                {d === 0 ? <Clock className="w-3 h-3" /> : null}
                                {d === 0 ? 'Today' : `${d} Days`}
                            </button>
                        ))}
                    </div>
                </div>

                <div className="h-[350px] w-full">
                    <ResponsiveContainer width="100%" height="100%">
                        <BarChart data={peakUsage}>
                            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                            <XAxis 
                                dataKey="hour" 
                                tickFormatter={formatHour}
                                axisLine={false}
                                tickLine={false}
                                tick={{fill: '#94a3b8', fontSize: 10, fontWeight: 600}}
                                interval={1}
                            />
                            <YAxis hide={true} />
                            <Tooltip 
                                content={<CustomPeakTooltip />} 
                                cursor={{fill: '#f8fafc', radius: 10}} 
                            />
                            <Bar 
                                dataKey="sessionCount" 
                                radius={[10, 10, 10, 10]}
                                barSize={24}
                            >
                                {peakUsage.map((entry, index) => (
                                    <Cell 
                                        key={`cell-${index}`} 
                                        fill={entry.sessionCount > 0 ? '#6366f1' : '#e2e8f0'} 
                                    />
                                ))}
                            </Bar>
                        </BarChart>
                    </ResponsiveContainer>
                </div>
                
                <div className="mt-6 flex items-center justify-center gap-8">
                    <div className="flex items-center gap-2">
                        <div className="w-3 h-3 rounded-full bg-[#6366f1]"></div>
                        <span className="text-xs font-bold text-slate-500 uppercase tracking-tight">Active Sessions</span>
                    </div>
                    <div className="flex items-center gap-2">
                        <div className="w-3 h-3 rounded-full bg-[#e2e8f0]"></div>
                        <span className="text-xs font-bold text-slate-500 uppercase tracking-tight">No Activity</span>
                    </div>
                </div>
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
                {stations.length > 0 ? (
                    stations.map(station => (
                        <StationRow key={station.id} station={station} />
                    ))
                ) : (
                    <div className="bg-slate-50 rounded-2xl p-12 text-center border-2 border-dashed border-slate-200">
                        <MapPin className="w-12 h-12 text-slate-300 mx-auto mb-4" />
                        <p className="text-slate-500 font-medium">You haven't registered any stations yet.</p>
                    </div>
                )}
            </div>

        </div>
    );
}
