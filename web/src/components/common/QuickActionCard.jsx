import React from 'react';
import clsx from 'clsx';

export default function QuickActionCard({ title, icon: Icon, iconColor, onClick }) {
    return (
        <button
            onClick={onClick}
            className="bg-white rounded-2xl aspect-square flex flex-col items-center justify-center gap-4 shadow-sm shadow-slate-200/50 border border-slate-100/50 hover:shadow-md hover:-translate-y-1 transition-all group"
        >
            <div className={clsx("w-14 h-14 rounded-full flex items-center justify-center transition-transform group-hover:scale-110", iconColor)}>
                <Icon className="w-6 h-6" />
            </div>
            <span className="text-sm font-semibold text-[#1A2234]">{title}</span>
        </button>
    );
}
