import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Calendar, ChevronLeft, ChevronRight, Car, Truck } from 'lucide-react';
import DataTable from '../../components/common/DataTable';
import {
    fetchOwnerBookings,
    selectOwnerBookings,
    selectBookingsPagination,
    selectBookingsLoading,
} from '../../store/bookingsSlice';

const STATUS_FILTERS = ['ALL', 'CONFIRMED', 'ONGOING', 'COMPLETED', 'CANCELLED', 'EXPIRED'];

const BOOKING_STATUS_STYLES = {
    CONFIRMED: 'bg-blue-50 text-blue-600',
    ONGOING: 'bg-amber-50 text-amber-600',
    COMPLETED: 'bg-emerald-50 text-emerald-600',
    CANCELLED: 'bg-slate-100 text-slate-500',
    EXPIRED: 'bg-rose-50 text-rose-600',
};

function BookingStatusPill({ status }) {
    const cls = BOOKING_STATUS_STYLES[status] || 'bg-slate-100 text-slate-500';
    return (
        <span className={`px-2.5 py-1 rounded-full text-[10px] font-bold tracking-wider ${cls}`}>
            {status || '—'}
        </span>
    );
}

export default function BookingsPage() {
    const dispatch = useDispatch();
    const bookings = useSelector(selectOwnerBookings);
    const pagination = useSelector(selectBookingsPagination);
    const loading = useSelector(selectBookingsLoading);

    const [page, setPage] = useState(0);
    const [status, setStatus] = useState('ALL');

    useEffect(() => {
        const userStr = localStorage.getItem('user');
        if (!userStr) return;
        const user = JSON.parse(userStr);
        dispatch(fetchOwnerBookings({
            ownerId: user.id,
            page,
            size: 10,
            status: status === 'ALL' ? '' : status,
        }));
    }, [dispatch, page, status]);

    const fmt = (val) => val
        ? new Date(val).toLocaleString('en-IN', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' })
        : '-';

    const columns = [
        { header: 'Booking', key: 'id', render: (v) => <span className="font-mono text-xs text-slate-500">#{v}</span> },
        { header: 'Customer', key: 'customerName', render: (v) => <span className="font-bold text-[#1A2234]">{v || '-'}</span> },
        { header: 'Station', key: 'stationName', render: (v) => <span className="text-slate-600">{v || '-'}</span> },
        { header: 'Slot', key: 'slotLabel', render: (v) => <span className="text-xs text-slate-500">{v || '-'}</span> },
        {
            header: 'Vehicle', key: 'vehicleType', render: (v) => (
                <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-600">
                    {v === 'TRUCK' ? <Truck className="w-3.5 h-3.5" /> : <Car className="w-3.5 h-3.5" />}{v || '-'}
                </span>
            )
        },
        { header: 'Start', key: 'startTime', render: (v) => <span className="text-xs font-semibold text-slate-600">{fmt(v)}</span> },
        { header: 'Status', key: 'status', render: (v) => <BookingStatusPill status={v} /> },
    ];

    return (
        <div className="space-y-8 max-w-[1400px] mx-auto pb-12">
            {/* Header + status filter */}
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-8 rounded-3xl shadow-sm border border-slate-100/50">
                <div>
                    <h1 className="text-2xl font-bold text-[#1A2234]">Bookings</h1>
                    <p className="text-slate-500 font-medium mt-1">Reservations and sessions across your stations.</p>
                </div>
                <div className="flex flex-wrap gap-2 bg-slate-50 p-1.5 rounded-2xl border border-slate-100">
                    {STATUS_FILTERS.map((s) => (
                        <button
                            key={s}
                            onClick={() => { setStatus(s); setPage(0); }}
                            className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
                                status === s ? 'bg-white text-[#1A2234] shadow-sm border border-slate-100' : 'text-slate-400 hover:text-slate-600'
                            }`}
                        >
                            {s === 'ALL' ? 'All' : s.charAt(0) + s.slice(1).toLowerCase()}
                        </button>
                    ))}
                </div>
            </div>

            {/* Table */}
            <div className="bg-white rounded-3xl p-8 shadow-sm border border-slate-100/50">
                <div className="flex items-center gap-3 mb-6">
                    <Calendar className="w-5 h-5 text-cyan-500" />
                    <h3 className="text-xl font-bold text-[#1A2234]">Booking Ledger</h3>
                </div>

                {loading ? (
                    <div className="text-center py-12 text-slate-400 font-medium">Loading bookings...</div>
                ) : (
                    <DataTable columns={columns} data={bookings} keyField="id" />
                )}

                {/* Pagination */}
                <div className="mt-8 flex items-center justify-between">
                    <p className="text-sm text-slate-500 font-medium">
                        Page <span className="text-[#1A2234] font-bold">{pagination.currentPage + 1}</span> of{' '}
                        <span className="text-[#1A2234] font-bold">{Math.max(pagination.totalPages, 1)}</span>
                    </p>
                    <div className="flex gap-3">
                        <button
                            onClick={() => setPage((p) => Math.max(0, p - 1))}
                            disabled={page === 0}
                            className="p-2.5 rounded-xl border border-slate-100 bg-white hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed transition-all shadow-sm"
                        >
                            <ChevronLeft className="w-5 h-5 text-[#1A2234]" />
                        </button>
                        <button
                            onClick={() => setPage((p) => p + 1)}
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
