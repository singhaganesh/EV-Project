import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
    Activity,
    Clock,
    TrendingUp,
    Zap,
    BarChart2,
    Calendar
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

import api from '../../api/axios';
import { 
    fetchRevenueTrends, 
    selectRevenueTrends, 
    fetchPeakUsage, 
    selectPeakUsage 
} from '../../store/stationSlice';

export default function AnalyticsPage() {
    const dispatch = useDispatch();
    const revenueTrends = useSelector(selectRevenueTrends);
    const peakUsage = useSelector(selectPeakUsage);
    
    const [loading, setLoading] = useState(true);
    const [revenueDays, setRevenueDays] = useState(7);
    const [peakDays, setPeakDays] = useState(0); // Default to Today

    useEffect(() => {
        const fetchInitialData = async () => {
            try {
                setLoading(true);
                const userStr = localStorage.getItem('user');
                if (!userStr) return;
                const user = JSON.parse(userStr);

                // Initial fetch for both charts
                await Promise.all([
                    dispatch(fetchRevenueTrends({ ownerId: user.id, days: revenueDays })),
                    dispatch(fetchPeakUsage({ ownerId: user.id, days: peakDays }))
                ]);
            } catch (error) {
                console.error('Error fetching analytics data:', error);
            } finally {
                setLoading(false);
            }
        };

        fetchInitialData();
    }, [dispatch]);

    // Handle independent filter changes
    useEffect(() => {
        const userStr = localStorage.getItem('user');
        if (!userStr) return;
        const user = JSON.parse(userStr);
        dispatch(fetchRevenueTrends({ ownerId: user.id, days: revenueDays }));
    }, [dispatch, revenueDays]);

    useEffect(() => {
        const userStr = localStorage.getItem('user');
        if (!userStr) return;
        const user = JSON.parse(userStr);
        dispatch(fetchPeakUsage({ ownerId: user.id, days: peakDays }));
    }, [dispatch, peakDays]);

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

    if (loading && revenueTrends.length === 0) {
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
                <div className="flex items-center gap-3 bg-emerald-50 px-4 py-2 rounded-2xl border border-emerald-100/50">
                    <div className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></div>
                    <span className="text-sm font-bold text-emerald-700 uppercase tracking-wider">Live Data Active</span>
                </div>
            </div>

            {/* Revenue & Energy Trends Chart */}
            <div className="bg-white rounded-3xl p-8 shadow-sm shadow-slate-200/50 border border-slate-100/50">
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
                    <div className="flex items-center gap-4">
                        <div className="w-12 h-12 rounded-2xl bg-emerald-50 flex items-center justify-center">
                            <TrendingUp className="w-6 h-6 text-emerald-600" />
                        </div>
                        <div>
                            <h3 className="text-xl font-bold text-[#1A2234]">Revenue & Energy Trends</h3>
                            <p className="text-sm text-slate-500 font-medium mt-1">Correlation between earnings and energy dispensed</p>
                        </div>
                    </div>
                    
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

            {/* Peak Hours Analysis */}
            <div className="bg-white rounded-3xl p-8 shadow-sm shadow-slate-200/50 border border-slate-100/50">
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
                    <div className="flex items-center gap-4">
                        <div className="w-12 h-12 rounded-2xl bg-indigo-50 flex items-center justify-center">
                            <Clock className="w-6 h-6 text-indigo-600" />
                        </div>
                        <div>
                            <h3 className="text-xl font-bold text-[#1A2234]">
                                {peakDays === 0 ? "Today's Peak Usage" : "Peak Usage Patterns"}
                            </h3>
                            <p className="text-sm text-slate-500 font-medium mt-1">
                                {peakDays === 0 ? "Live charging sessions by hour" : `Hourly trends over the last ${peakDays} days`}
                            </p>
                        </div>
                    </div>
                    
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
        </div>
    );
}
