import React from 'react';
import clsx from 'clsx';

const STATUS_CONFIG = {
    active: {
        bg: 'bg-emerald-100',
        text: 'text-emerald-700',
        dot: 'bg-emerald-500',
        label: 'Active'
    },
    maintenance: {
        bg: 'bg-amber-100',
        text: 'text-amber-700',
        dot: 'bg-amber-500',
        label: 'Maintenance'
    },
    offline: {
        bg: 'bg-rose-100',
        text: 'text-rose-700',
        dot: 'bg-rose-500',
        label: 'Offline'
    },
    charging: {
        bg: 'bg-emerald-100',
        text: 'text-emerald-700',
        dot: 'bg-emerald-500',
        label: 'Charging'
    },
    idle: {
        bg: 'bg-amber-100',
        text: 'text-amber-700',
        dot: 'bg-amber-500',
        label: 'Idle'
    }
};

export default function StatusBadge({ status }) {
    const config = STATUS_CONFIG[status.toLowerCase()] || STATUS_CONFIG.offline;

    return (
        <div className={clsx("inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold", config.bg, config.text)}>
            <span className={clsx("w-1.5 h-1.5 rounded-full", config.dot)}></span>
            {config.label}
        </div>
    );
}
