import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { 
    Wallet, 
    TrendingUp, 
    Clock, 
    Check, 
    ArrowRight,
    Download
} from 'lucide-react';
import StatCard from '../../components/common/StatCard';
import { fetchEarningsSummary, selectEarningsSummary, selectEarningsLoading } from '../../store/earningsSlice';

export default function EarningsPage() {
    const dispatch = useDispatch();
    const summary = useSelector(selectEarningsSummary);
    const loading = useSelector(selectEarningsLoading);

    useEffect(() => {
        const userStr = localStorage.getItem('user');
        if (!userStr) return;
        const user = JSON.parse(userStr);
        dispatch(fetchEarningsSummary(user.id));
    }, [dispatch]);

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
                <button className="flex items-center gap-2 bg-[#1A2234] hover:bg-slate-800 text-white px-6 py-3 rounded-2xl font-bold text-sm transition-all shadow-sm">
                    <Download className="w-4 h-4" /> Export Report
                </button>
            </div>

            {/* Wallet Header (The Summary Stats) */}
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
