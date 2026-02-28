import React from 'react';
import { TrendingUp, TrendingDown } from 'lucide-react';
import clsx from 'clsx';

export default function StatCard({ title, value, icon: Icon, iconColor, trend, trendValue, trendLabel }) {
    const isPositive = trend === 'up';

    return (
        <div className="bg-white rounded-2xl p-6 shadow-sm shadow-slate-200/50 border border-slate-100/50 flex flex-col justify-between">
            <div className="flex justify-between items-start mb-4">
                <div>
                    <p className="text-sm font-medium text-slate-500 mb-1">{title}</p>
                    <h3 className="text-3xl font-bold text-[#1A2234]">{value}</h3>
                </div>
                <div className={clsx("w-12 h-12 rounded-xl flex items-center justify-center", iconColor)}>
                    <Icon className="w-6 h-6 text-white" />
                </div>
            </div>

            <div className="flex items-center gap-2 mt-4">
                <div
                    className={clsx(
                        "flex items-center gap-1 px-2 py-1 rounded-md text-xs font-semibold",
                        isPositive ? "bg-emerald-50 text-emerald-600" : "bg-rose-50 text-rose-600"
                    )}
                >
                    {isPositive ? <TrendingUp className="w-3 h-3" /> : <TrendingDown className="w-3 h-3" />}
                    <span>{trendValue}</span>
                </div>
                <span className="text-xs text-slate-400">{trendLabel}</span>
            </div>
        </div>
    );
}
