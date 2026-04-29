import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { 
    Wallet, 
    TrendingUp, 
    Clock, 
    Check, 
    ArrowRight,
    Download,
    MapPin,
    BarChart3
} from 'lucide-react';
import {
    LineChart,
    Line,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    ResponsiveContainer,
    AreaChart,
    Area
} from 'recharts';
import StatCard from '../../components/common/StatCard';
import { fetchEarningsSummary, selectEarningsSummary, selectEarningsLoading } from '../../store/earningsSlice';
import { 
    fetchRevenueTrends, 
    selectRevenueTrends, 
    fetchAnalyticsSummary, 
    selectStationRevenue 
} from '../../store/stationSlice';

export default function EarningsPage() {
    const dispatch = useDispatch();
    const summary = useSelector(selectEarningsSummary);
    const earningsLoading = useSelector(selectEarningsLoading);
    const revenueTrends = useSelector(selectRevenueTrends);
    const stationRevenue = useSelector(selectStationRevenue);

    useEffect(() => {
        const userStr = localStorage.getItem('user');
        if (!userStr) return;
        const user = JSON.parse(userStr);
        
        // Fetch financial summary
        dispatch(fetchEarningsSummary(user.id));
        
        // Fetch 14-day cash flow trend
        dispatch(fetchRevenueTrends({ ownerId: user.id, days: 14 }));
        
        // Fetch station-wise breakdown
        dispatch(fetchAnalyticsSummary({ ownerId: user.id, days: 30 }));
    }, [dispatch]);

    const formatChartDate = (dateStr) => {
        const date = new Date(dateStr);
        return date.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
    };

    if (earningsLoading && summary.lifetimeRevenue === 0) {
        return (
            <div className="flex items-center justify-center h-[60vh]">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-emerald-500"></div>
            </div>
        );
    }

    return (
        <div className="space-y-8 max-w-[1400px] mx-auto pb-12">
            {/* Page Header */}
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-8 rounded-3xl shadow-sm border border-slate-100/50">
                <div>
                    <h1 className="text-2xl font-bold text-[#1A2234]">Earnings & Payouts</h1>
                    <p className="text-slate-500 font-medium mt-1">Track your revenue and manage your bank settlements.</p>
                </div>
                <button className="flex items-center gap-2 bg-[#1A2234] hover:bg-slate-800 text-white px-6 py-3 rounded-2xl font-bold text-sm transition-all shadow-sm">
                    <Download className="w-4 h-4" /> Export Report
                </button>
            </div>

            {/* Wallet Header */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                <StatCard
                    title="Current Balance"
                    value={`₹${summary.currentBalance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}`}
                    icon={Wallet}
                    iconColor="bg-emerald-500"
                    trend="up"
                    trendValue="Available"
                    trendLabel="for next payout"
                />
                <StatCard
                    title="Lifetime Revenue"
                    value={`₹${summary.lifetimeRevenue.toLocaleString('en-IN', { minimumFractionDigits: 2 })}`}
                    icon={TrendingUp}
                    iconColor="bg-blue-600"
                    trend="up"
                    trendValue="Total"
                    trendLabel="Gross earnings"
                />
                <StatCard
                    title="Pending Payouts"
                    value={`₹${summary.pendingPayouts.toLocaleString('en-IN', { minimumFractionDigits: 2 })}`}
                    icon={Clock}
                    iconColor="bg-amber-500"
                    trend="down"
                    trendValue="48h window"
                    trendLabel="In process"
                />
                <StatCard
                    title="Last Settlement"
                    value={`₹${summary.lastSettlement.toLocaleString('en-IN', { minimumFractionDigits: 2 })}`}
                    icon={Check}
                    iconColor="bg-slate-700"
                    trend="up"
                    trendValue="Success"
                    trendLabel="Last transfer"
                />
            </div>

            {/* Revenue Breakdown Section */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                {/* Cash Flow Trend (2/3 width) */}
                <div className="lg:col-span-2 bg-white rounded-3xl p-8 shadow-sm border border-slate-100/50">
                    <div className="flex items-center justify-between mb-8">
                        <div>
                            <h3 className="text-xl font-bold text-[#1A2234]">Cash Flow Trend</h3>
                            <p className="text-sm text-slate-500 font-medium mt-1">Daily revenue over the last 14 days</p>
                        </div>
                        <div className="bg-emerald-50 text-emerald-700 px-3 py-1 rounded-full text-xs font-bold border border-emerald-100">
                            LIVE UPDATES
                        </div>
                    </div>
                    
                    <div className="h-[300px] w-full">
                        <ResponsiveContainer width="100%" height="100%">
                            <AreaChart data={revenueTrends}>
                                <defs>
                                    <linearGradient id="colorRevenue" x1="0" y1="0" x2="0" y2="1">
                                        <stop offset="5%" stopColor="#10b981" stopOpacity={0.1}/>
                                        <stop offset="95%" stopColor="#10b981" stopOpacity={0}/>
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
                                    axisLine={false}
                                    tickLine={false}
                                    tick={{fill: '#94a3b8', fontSize: 12, fontWeight: 600}}
                                    tickFormatter={(val) => `₹${val}`}
                                />
                                <Tooltip 
                                    content={({ active, payload }) => {
                                        if (active && payload && payload.length) {
                                            return (
                                                <div className="bg-white p-3 border border-slate-100 shadow-xl rounded-xl">
                                                    <p className="text-[10px] font-bold text-slate-400 uppercase mb-1">{formatChartDate(payload[0].payload.date)}</p>
                                                    <p className="text-sm font-bold text-emerald-600">₹{payload[0].value.toFixed(2)}</p>
                                                </div>
                                            );
                                        }
                                        return null;
                                    }}
                                />
                                <Area
                                    type="monotone"
                                    dataKey="revenue"
                                    stroke="#10b981"
                                    strokeWidth={3}
                                    fillOpacity={1}
                                    fill="url(#colorRevenue)"
                                />
                            </AreaChart>
                        </ResponsiveContainer>
                    </div>
                </div>

                {/* Earnings by Station (1/3 width) */}
                <div className="bg-white rounded-3xl p-8 shadow-sm border border-slate-100/50">
                    <div className="mb-8">
                        <h3 className="text-xl font-bold text-[#1A2234]">Station Breakdown</h3>
                        <p className="text-sm text-slate-500 font-medium mt-1">Revenue distribution by location</p>
                    </div>

                    <div className="space-y-6">
                        {stationRevenue.length > 0 ? (
                            stationRevenue.map((station, index) => {
                                const percentage = ((station.totalRevenue / summary.lifetimeRevenue) * 100).toFixed(1);
                                return (
                                    <div key={index} className="group">
                                        <div className="flex justify-between items-start mb-2">
                                            <div className="flex items-center gap-3">
                                                <div className="p-2 bg-slate-50 rounded-lg group-hover:bg-emerald-50 transition-colors">
                                                    <MapPin className="w-4 h-4 text-slate-400 group-hover:text-emerald-500" />
                                                </div>
                                                <div>
                                                    <p className="text-sm font-bold text-[#1A2234]">{station.stationName}</p>
                                                    <p className="text-[11px] font-bold text-slate-400 uppercase">{percentage}% contribution</p>
                                                </div>
                                            </div>
                                            <p className="font-bold text-slate-700">₹{station.totalRevenue.toLocaleString('en-IN')}</p>
                                        </div>
                                        <div className="w-full bg-slate-100 h-1.5 rounded-full overflow-hidden">
                                            <div 
                                                className="bg-emerald-500 h-full rounded-full" 
                                                style={{ width: `${percentage}%` }}
                                            />
                                        </div>
                                    </div>
                                );
                            })
                        ) : (
                            <div className="text-center py-12">
                                <BarChart3 className="w-12 h-12 text-slate-200 mx-auto mb-3" />
                                <p className="text-sm text-slate-400 font-medium">No station data available yet.</p>
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {/* Placeholder for Transaction History */}
            <div className="bg-white rounded-3xl p-12 text-center border-2 border-dashed border-slate-200">
                <Clock className="w-12 h-12 text-slate-300 mx-auto mb-4" />
                <h3 className="text-lg font-bold text-[#1A2234]">Transaction History Coming Soon</h3>
                <p className="text-slate-500 font-medium max-w-sm mx-auto mt-2">
                    We are currently building the detailed ledger view for your charging sessions.
                </p>
            </div>
        </div>
    );
}
