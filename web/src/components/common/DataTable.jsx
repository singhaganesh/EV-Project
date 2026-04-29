import React from 'react';

/**
 * A robust and reusable data table component.
 * Supports both simple key access and custom render functions.
 */
export default function DataTable({ columns, data, keyField }) {
    return (
        <div className="w-full overflow-x-auto bg-white rounded-2xl shadow-sm shadow-slate-200/50 border border-slate-100/50">
            <table className="w-full text-left text-sm text-slate-600">
                <thead className="text-xs text-slate-400 uppercase bg-slate-50/50 border-b border-slate-100">
                    <tr>
                        {columns.map((col, idx) => (
                            <th key={idx} scope="col" className="px-6 py-4 font-semibold tracking-wider">
                                {col.header}
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {data && data.length > 0 ? (
                        data.map((row, rowIndex) => (
                            <tr
                                key={row[keyField] || rowIndex}
                                className="bg-white border-b border-slate-50 last:border-0 hover:bg-slate-50/50 transition-colors"
                            >
                                {columns.map((col, colIndex) => {
                                    // Support both 'key' and 'accessor' for backward compatibility
                                    const dataKey = col.key || col.accessor;
                                    const value = row[dataKey];
                                    
                                    return (
                                        <td key={colIndex} className="px-6 py-4 whitespace-nowrap text-slate-700">
                                            {col.render 
                                                ? col.render(value, row) // Pass both value and full row to render
                                                : (value !== null && value !== undefined ? String(value) : '-')
                                            }
                                        </td>
                                    );
                                })}
                            </tr>
                        ))
                    ) : (
                        <tr>
                            <td colSpan={columns.length} className="px-6 py-12 text-center text-slate-400 font-medium italic">
                                No data available in this table
                            </td>
                        </tr>
                    )}
                </tbody>
            </table>
        </div>
    );
}
