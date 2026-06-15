import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
    Wallet,
    TrendingUp,
    Clock,
    Zap,
    ArrowRight,
    Download,
    MapPin,
    BarChart3,
    ChevronLeft,
    ChevronRight,
    Search
} from 'lucide-react';
import {
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    ResponsiveContainer,
    AreaChart,
    Area
} from 'recharts';
import toast from 'react-hot-toast';
import StatCard from '../../components/common/StatCard';
import DataTable from '../../components/common/DataTable';
import StatusBadge from '../../components/common/StatusBadge';
import { exportEarningsTransactions } from '../../api/axios';
import { 
    fetchEarningsSummary, 
    selectEarningsSummary, 
    fetchEarningsTransactions,
    selectEarningsTransactions,
    selectEarningsPagination,
    selectEarningsLoading 
} from '../../store/earningsSlice';
import { 
    fetchRevenueTrends, 
    selectRevenueTrends, 
    fetchAnalyticsSummary, 
    selectStationRevenue 
} from '../../store/stationSlice';

export default function EarningsPage() {
    const dispatch = useDispatch();
    const summary = useSelector(selectEarningsSummary);
    const transactions = useSelector(selectEarningsTransactions);
    const pagination = useSelector(selectEarningsPagination);
    const loading = useSelector(selectEarningsLoading);
    
    const revenueTrends = useSelector(selectRevenueTrends);
    const stationRevenue = useSelector(selectStationRevenue);

    const [page, setPage] = useState(0);
    const [searchInput, setSearchInput] = useState('');
    const [search, setSearch] = useState('');
    const [exporting, setExporting] = useState(false);

    const handleExport = async () => {
        const userStr = localStorage.getItem('user');
        if (!userStr) return;
        const user = JSON.parse(userStr);
        try {
            setExporting(true);
            const response = await exportEarningsTransactions(user.id);
            const blob = new Blob([response.data], { type: 'text/csv' });
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `transactions-${new Date().toISOString().slice(0, 10)}.csv`;
            document.body.appendChild(a);
            a.click();
            a.remove();
            window.URL.revokeObjectURL(url);
            toast.success('Report exported.');
        } catch (err) {
            toast.error('Failed to export report.');
        } finally {
            setExporting(false);
        }
    };

    // Debounce the ledger search box; reset to the first page on a new query.
    useEffect(() => {
        const t = setTimeout(() => {
            setSearch(searchInput.trim());
            setPage(0);
        }, 400);
        return () => clearTimeout(t);
    }, [searchInput]);

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

    useEffect(() => {
        const userStr = localStorage.getItem('user');
        if (!userStr) return;
        const user = JSON.parse(userStr);
        
        // Fetch transactions for current page
        dispatch(fetchEarningsTransactions({ ownerId: user.id, page, size: 10, search }));
    }, [dispatch, page, search]);

    const formatChartDate = (dateStr) => {
        const date = new Date(dateStr);
        return date.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
    };

    const columns = [
        { 
            header: 'Date', 
            key: 'timestamp',
            render: (val) => (
                <span className="text-xs font-semibold text-slate-600">
                    {new Date(val).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })}
                </span>
            )
        },
        { 
            header: 'Station', 
            key: 'stationName',
            render: (val) => <span className="font-bold text-[#1A2234]">{val}</span>
        },
        { 
            header: 'Energy', 
            key: 'energyKwh',
            render: (val) => <span className="text-xs font-bold text-slate-500">{Number(val || 0).toFixed(2)} kWh</span>
        },
        { 
            header: 'Revenue', 
            key: 'amount',
            render: (val) => <span className="font-bold text-emerald-600">₹{Number(val || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
        },
        { 
            header: 'Transaction ID', 
            key: 'razorpayOrderId',
            render: (val) => <span className="text-[10px] font-mono text-slate-400 bg-slate-50 px-2 py-1 rounded border border-slate-100">{val || 'N/A'}</span>
        },
        { 
            header: 'Status', 
            key: 'status',
            render: (val) => <StatusBadge status={val === 'PAID' ? 'active' : 'pending'} />
        }
    ];

    if (loading && summary.lifetimeRevenue === 0) {
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
                <button
                    onClick={handleExport}
                    disabled={exporting}
                    className="flex items-center gap-2 bg-[#1A2234] hover:bg-slate-800 text-white px-6 py-3 rounded-2xl font-bold text-sm transition-all shadow-sm disabled:opacity-60 disabled:cursor-not-allowed"
                >
                    <Download className="w-4 h-4" /> {exporting ? 'Exporting...' : 'Export Report'}
                </button>
            </div>

            {/* Wallet Header */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                <StatCard
                    title="Lifetime Revenue"
                    value={`₹${(summary.lifetimeRevenue || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`}
                    icon={TrendingUp}
                    iconColor="bg-blue-600"
                    trend="up"
                    trendValue="Gross"
                    trendLabel="All paid sessions"
                />
                <StatCard
                    title="Energy Cost"
                    value={`₹${(summary.energyCost || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`}
                    icon={Zap}
                    iconColor="bg-amber-500"
                    trend="down"
                    trendValue="Grid tariff"
                    trendLabel="Cost of energy sold"
                />
                <StatCard
                    title="Net Margin"
                    value={`₹${(summary.netMargin || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`}
                    icon={Wallet}
                    iconColor="bg-emerald-500"
                    trend="up"
                    trendValue="Revenue − energy cost"
                    trendLabel=""
                />
                <StatCard
                    title="Revenue (Last 48h)"
                    value={`₹${(summary.revenueLast48h || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`}
                    icon={Clock}
                    iconColor="bg-slate-700"
                    trend="up"
                    trendValue="Recent"
                    trendLabel="Paid in last 48h"
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
                                const percentage = summary.lifetimeRevenue > 0 
                                    ? ((station.totalRevenue / summary.lifetimeRevenue) * 100).toFixed(1)
                                    : '0.0';
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

            {/* Transaction Ledger Table */}
            <div className="bg-white rounded-3xl p-8 shadow-sm border border-slate-100/50">
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
                    <div>
                        <h3 className="text-xl font-bold text-[#1A2234]">Transaction Ledger</h3>
                        <p className="text-sm text-slate-500 font-medium mt-1">Detailed audit of all charging sessions</p>
                    </div>
                    <div className="relative group">
                        <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 group-focus-within:text-emerald-500 transition-colors" />
                        <input
                            type="text"
                            value={searchInput}
                            onChange={(e) => setSearchInput(e.target.value)}
                            placeholder="Search station or txn ID..."
                            className="pl-11 pr-4 py-2.5 bg-slate-50 border border-slate-100 rounded-xl text-sm font-medium focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:bg-white transition-all min-w-[240px]"
                        />
                    </div>
                </div>

                <DataTable columns={columns} data={transactions} keyField="sessionId" />

                {/* Pagination Controls */}
                <div className="mt-8 flex items-center justify-between">
                    <p className="text-sm text-slate-500 font-medium">
                        Page <span className="text-[#1A2234] font-bold">{pagination.currentPage + 1}</span> of <span className="text-[#1A2234] font-bold">{pagination.totalPages}</span>
                    </p>
                    <div className="flex gap-3">
                        <button 
                            onClick={() => setPage(p => Math.max(0, p - 1))}
                            disabled={page === 0}
                            className="p-2.5 rounded-xl border border-slate-100 bg-white hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed transition-all shadow-sm"
                        >
                            <ChevronLeft className="w-5 h-5 text-[#1A2234]" />
                        </button>
                        <button 
                            onClick={() => setPage(p => p + 1)}
                            disabled={page >= pagination.totalPages - 1}
                            className="p-2.5 rounded-xl border border-slate-100 bg-white hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed transition-all shadow-sm"
                        >
                            <ChevronRight className="w-5 h-5 text-[#1A2234]" />
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}
