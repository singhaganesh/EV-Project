import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
    Activity,
    Clock,
    TrendingUp,
    Zap,
    BarChart2,
    Calendar,
    Timer,
    DollarSign,
    MapPin,
    Layers
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
    Cell,
    PieChart,
    Pie
} from 'recharts';

import StatCard from '../../components/common/StatCard';
import { 
    fetchRevenueTrends, 
    selectRevenueTrends, 
    fetchPeakUsage, 
    selectPeakUsage,
    fetchAnalyticsSummary,
    selectEfficiencyMetrics,
    selectStationRevenue,
    selectConnectorRevenue
} from '../../store/stationSlice';

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884d8'];

export default function AnalyticsPage() {
    const dispatch = useDispatch();
    const revenueTrends = useSelector(selectRevenueTrends);
    const peakUsage = useSelector(selectPeakUsage);
    const efficiency = useSelector(selectEfficiencyMetrics);
    const stationRevenue = useSelector(selectStationRevenue);
    const connectorRevenue = useSelector(selectConnectorRevenue);
    
    const [loading, setLoading] = useState(true);
    const [days, setDays] = useState(7);

    useEffect(() => {
        const fetchData = async () => {
            try {
                setLoading(true);
                const userStr = localStorage.getItem('user');
                if (!userStr) return;
                const user = JSON.parse(userStr);

                await Promise.all([
                    dispatch(fetchRevenueTrends({ ownerId: user.id, days: days === 0 ? 7 : days })),
                    dispatch(fetchPeakUsage({ ownerId: user.id, days: days })),
                    dispatch(fetchAnalyticsSummary({ ownerId: user.id, days: days }))
                ]);
            } catch (error) {
                console.error('Error fetching analytics data:', error);
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, [dispatch, days]);

    const formatChartDate = (dateStr) => {
        const date = new Date(dateStr);
        return date.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
    };

    const formatHour = (hour) => {
        const h = hour % 12 || 12;
        const ampm = hour < 12 ? 'AM' : 'PM';
        return `${h}${ampm}`;
    };

    const CustomTrendTooltip = ({ active, payload, label }) => {
        if (active && payload && payload.length) {
            return (
                <div className="bg-white p-4 border border-slate-100 shadow-xl rounded-2xl min-w-[180px]">
                    <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-3">{formatChartDate(label)}</p>
                    <div className="space-y-3">
                        <div className="flex items-center justify-between gap-4">
                            <div className="flex items-center gap-2">
                                <div className="w-2 h-2 rounded-full bg-emerald-500"></div>
                                <span className="text-xs font-semibold text-slate-500">Revenue</span>
                            </div>
                            <span className="text-sm font-bold text-[#1A2234]">₹{payload[0].value.toFixed(2)}</span>
                        </div>
                        <div className="flex items-center justify-between gap-4">
                            <div className="flex items-center gap-2">
                                <div className="w-2 h-2 rounded-full bg-cyan-500"></div>
                                <span className="text-xs font-semibold text-slate-500">Energy</span>
                            </div>
                            <span className="text-sm font-bold text-[#1A2234]">{payload[1].value.toFixed(2)} kWh</span>
                        </div>
                    </div>
                </div>
            );
        }
        return null;
    };

    const CustomPeakTooltip = ({ active, payload, label }) => {
        if (active && payload && payload.length) {
            return (
                <div className="bg-white p-4 border border-slate-100 shadow-xl rounded-2xl min-w-[150px]">
                    <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-2">{formatHour(label)} Window</p>
                    <div className="flex items-center gap-2">
                        <div className="w-2 h-2 rounded-full bg-indigo-500"></div>
                        <span className="text-sm font-bold text-[#1A2234]">{payload[0].value} Sessions</span>
                    </div>
                </div>
            );
        }
        return null;
    };

    if (loading && !efficiency) {
        return (
            <div className="flex items-center justify-center h-[60vh]">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-cyan-500"></div>
            </div>
        );
    }

    return (
        <div className="space-y-8 max-w-[1400px] mx-auto pb-12">
            {/* Page Header */}
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-8 rounded-3xl shadow-sm border border-slate-100/50">
                <div>
                    <h1 className="text-2xl font-bold text-[#1A2234]">Analytics & Reports</h1>
                    <p className="text-slate-500 font-medium mt-1">Deep-dive into your station's performance and usage patterns.</p>
                </div>
                
                {/* Global Time Range Filter */}
                <div className="flex bg-slate-50 p-1.5 rounded-2xl border border-slate-100">
                    {[0, 7, 14, 30].map((d) => (
                        <button
                            key={d}
                            onClick={() => setDays(d)}
                            className={`px-6 py-2.5 rounded-xl text-xs font-bold transition-all flex items-center gap-2 ${
                                days === d 
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

            {/* Efficiency KPI Cards */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <StatCard
                    title="Avg. Session Revenue"
                    value={`₹${efficiency?.avgRevenue?.toFixed(2) || '0.00'}`}
                    icon={DollarSign}
                    iconColor="bg-emerald-500"
                    trend="up"
                    trendValue="Target: ₹300"
                    trendLabel=""
                />
                <StatCard
                    title="Avg. Energy / Session"
                    value={`${efficiency?.avgEnergy?.toFixed(1) || '0.0'} kWh`}
                    icon={Zap}
                    iconColor="bg-cyan-500"
                    trend="up"
                    trendValue="High Efficiency"
                    trendLabel=""
                />
                <StatCard
                    title="Avg. Session Duration"
                    value={`${efficiency?.avgDurationMinutes?.toFixed(0) || '0'} min`}
                    icon={Timer}
                    iconColor="bg-indigo-500"
                    trend="down"
                    trendValue="Optimal Flow"
                    trendLabel=""
                />
            </div>

            {/* Revenue & Energy Trends Chart */}
            <div className="bg-white rounded-3xl p-8 shadow-sm border border-slate-100/50">
                <div className="mb-8">
                    <h3 className="text-xl font-bold text-[#1A2234]">Revenue & Energy Trends</h3>
                    <p className="text-sm text-slate-500 font-medium mt-1">Correlation between earnings and energy dispensed</p>
                </div>
                <div className="h-[400px] w-full">
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
                            <Tooltip content={<CustomTrendTooltip />} cursor={{stroke: '#e2e8f0', strokeWidth: 2}} />
                            <Area yAxisId="left" type="monotone" dataKey="revenue" stroke="#10b981" strokeWidth={3} fillOpacity={1} fill="url(#colorRevenue)" />
                            <Area yAxisId="right" type="monotone" dataKey="energy" stroke="#06b6d4" strokeWidth={3} fillOpacity={1} fill="url(#colorEnergy)" />
                        </AreaChart>
                    </ResponsiveContainer>
                </div>
            </div>

            {/* Distribution Grid */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                {/* Station Performance */}
                <div className="bg-white rounded-3xl p-8 shadow-sm border border-slate-100/50">
                    <h3 className="text-xl font-bold text-[#1A2234] mb-8 flex items-center gap-3">
                        <MapPin className="w-5 h-5 text-cyan-500" /> Revenue by Station
                    </h3>
                    <div className="h-[300px]">
                        <ResponsiveContainer width="100%" height="100%">
                            <PieChart>
                                <Pie
                                    data={stationRevenue}
                                    dataKey="totalRevenue"
                                    nameKey="stationName"
                                    cx="50%"
                                    cy="50%"
                                    innerRadius={60}
                                    outerRadius={100}
                                    paddingAngle={5}
                                >
                                    {stationRevenue.map((entry, index) => (
                                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                    ))}
                                </Pie>
                                <Tooltip 
                                    content={({ active, payload }) => {
                                        if (active && payload && payload.length) {
                                            return (
                                                <div className="bg-white p-3 border border-slate-100 shadow-xl rounded-xl">
                                                    <p className="text-xs font-bold text-[#1A2234] mb-1">{payload[0].name}</p>
                                                    <p className="text-sm font-bold text-emerald-600">₹{payload[0].value.toFixed(2)}</p>
                                                </div>
                                            );
                                        }
                                        return null;
                                    }}
                                />
                            </PieChart>
                        </ResponsiveContainer>
                    </div>
                </div>

                {/* Connector Performance */}
                <div className="bg-white rounded-3xl p-8 shadow-sm border border-slate-100/50">
                    <h3 className="text-xl font-bold text-[#1A2234] mb-8 flex items-center gap-3">
                        <Layers className="w-5 h-5 text-indigo-500" /> Revenue by Connector
                    </h3>
                    <div className="h-[300px]">
                        <ResponsiveContainer width="100%" height="100%">
                            <PieChart>
                                <Pie
                                    data={connectorRevenue}
                                    dataKey="totalRevenue"
                                    nameKey="connectorType"
                                    cx="50%"
                                    cy="50%"
                                    innerRadius={60}
                                    outerRadius={100}
                                    paddingAngle={5}
                                >
                                    {connectorRevenue.map((entry, index) => (
                                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                    ))}
                                </Pie>
                                <Tooltip 
                                    content={({ active, payload }) => {
                                        if (active && payload && payload.length) {
                                            return (
                                                <div className="bg-white p-3 border border-slate-100 shadow-xl rounded-xl">
                                                    <p className="text-xs font-bold text-[#1A2234] mb-1">{payload[0].name}</p>
                                                    <p className="text-sm font-bold text-emerald-600">₹{payload[0].value.toFixed(2)}</p>
                                                </div>
                                            );
                                        }
                                        return null;
                                    }}
                                />
                            </PieChart>
                        </ResponsiveContainer>
                    </div>
                </div>
            </div>

            {/* Peak Hours Analysis */}
            <div className="bg-white rounded-3xl p-8 shadow-sm border border-slate-100/50">
                <div className="mb-8">
                    <h3 className="text-xl font-bold text-[#1A2234]">
                        {days === 0 ? "Today's Peak Usage" : "Peak Usage Patterns"}
                    </h3>
                    <p className="text-sm text-slate-500 font-medium mt-1">Usage distribution by hour</p>
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
                            <Tooltip content={<CustomPeakTooltip />} cursor={{fill: '#f8fafc', radius: 10}} />
                            <Bar dataKey="sessionCount" radius={[10, 10, 10, 10]} barSize={24}>
                                {peakUsage.map((entry, index) => (
                                    <Cell key={`cell-${index}`} fill={entry.sessionCount > 0 ? '#6366f1' : '#e2e8f0'} />
                                ))}
                            </Bar>
                        </BarChart>
                    </ResponsiveContainer>
                </div>
            </div>
        </div>
    );
}
