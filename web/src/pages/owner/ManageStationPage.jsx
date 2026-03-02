import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import {
    ArrowLeft, Save, Settings, Zap, Plus, Trash2, ChevronDown, ChevronUp,
    MapPin, Clock, IndianRupee, Truck, AlertTriangle, Loader2, Pencil,
    Coffee, Wifi, Car, CreditCard, BatteryCharging, Check
} from 'lucide-react';
import api from '../../api/axios';

// ─────────────────── Helper Components ───────────────────

// Available Amenities (Icons map to logical features)
const AMENITIES_LIST = [
    { id: 'Cafe', label: 'Cafe & Snacks', icon: Coffee },
    { id: 'WiFi', label: 'Free WiFi', icon: Wifi },
    { id: 'Restroom', label: 'Restrooms', icon: Car },
    { id: 'CCTV', label: '24/7 CCTV', icon: BatteryCharging },
    { id: 'Shopping', label: 'Shopping Area', icon: CreditCard },
    { id: 'Lounge', label: 'Waiting Lounge', icon: Coffee }
];

function TabButton({ active, icon: Icon, label, onClick }) {
    return (
        <button
            onClick={onClick}
            className={`flex items-center gap-2 px-6 py-3 text-sm font-semibold rounded-full transition-all
                ${active
                    ? 'bg-[#1A2234] text-white shadow-lg shadow-slate-900/10'
                    : 'text-slate-500 hover:text-slate-700 hover:bg-white/60'
                }`}
        >
            <Icon className="w-4 h-4" />
            {label}
        </button>
    );
}

function InputField({ label, value, onChange, type = 'text', placeholder, icon: Icon }) {
    return (
        <div>
            <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">{label}</label>
            <div className="relative">
                {Icon && (
                    <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                        <Icon className="w-4 h-4 text-slate-400" />
                    </div>
                )}
                <input
                    type={type}
                    value={value}
                    onChange={onChange}
                    onWheel={(e) => e.target.blur()}
                    placeholder={placeholder}
                    className={`block w-full ${Icon ? 'pl-11' : 'pl-4'} pr-4 py-3 bg-white border border-slate-200 rounded-xl text-sm text-slate-700 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-cyan-500/20 focus:border-cyan-500 transition-all`}
                />
            </div>
        </div>
    );
}

// ─────────────────── Status Badge ───────────────────

const statusColors = {
    AVAILABLE: 'bg-emerald-50 text-emerald-600',
    RESERVED: 'bg-blue-50 text-blue-600',
    BOOKED: 'bg-indigo-50 text-indigo-600',
    CHARGING: 'bg-amber-50 text-amber-600',
    MAINTENANCE: 'bg-red-50 text-red-600',
    OCCUPIED: 'bg-slate-100 text-slate-600',
};

function GunStatusBadge({ status }) {
    return (
        <span className={`px-2.5 py-1 rounded-full text-[10px] font-bold tracking-wider ${statusColors[status] || 'bg-slate-100 text-slate-500'}`}>
            {status}
        </span>
    );
}

// ─────────────────── Tab 1: Overview & Settings ───────────────────

function OverviewTab({ station, setStation, onSave, saving }) {
    if (!station) return null;

    const handleChange = (field) => (e) => {
        setStation(prev => ({ ...prev, [field]: e.target.value }));
    };

    const handleNumberChange = (field) => (e) => {
        setStation(prev => ({ ...prev, [field]: parseFloat(e.target.value) || 0 }));
    };

    const toggleAmenity = (id) => {
        setStation(prev => {
            const current = prev.amenities || [];
            const isSelected = current.includes(id);
            return {
                ...prev,
                amenities: isSelected ? current.filter(a => a !== id) : [...current, id]
            };
        });
    };

    return (
        <div className="space-y-8">
            {/* Station Identity */}
            <div className="bg-white rounded-[24px] p-8 shadow-[0_2px_12px_rgba(0,0,0,0.03)] border border-slate-100/50">
                <h3 className="text-lg font-bold text-[#1A2234] mb-6">Station Identity</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <InputField label="Station Name" value={station.name || ''} onChange={handleChange('name')} placeholder="e.g., Tata Power Kolkata Hub" icon={Zap} />
                    <InputField label="Address" value={station.address || ''} onChange={handleChange('address')} placeholder="Full street address" icon={MapPin} />
                    <InputField label="Latitude" value={station.latitude || ''} onChange={handleNumberChange('latitude')} type="number" placeholder="22.5726" />
                    <InputField label="Longitude" value={station.longitude || ''} onChange={handleNumberChange('longitude')} type="number" placeholder="88.3639" />
                </div>
            </div>

            {/* Operations */}
            <div className="bg-white rounded-[24px] p-8 shadow-[0_2px_12px_rgba(0,0,0,0.03)] border border-slate-100/50">
                <h3 className="text-lg font-bold text-[#1A2234] mb-6">Operations & Pricing</h3>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <div>
                        <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">Operating Hours</label>
                        <div className="space-y-3">
                            <div className="flex items-center justify-between p-3 bg-slate-50 border border-slate-200 rounded-xl">
                                <span className="text-sm font-medium text-slate-700">Open 24 Hours</span>
                                <button
                                    type="button"
                                    onClick={() => setStation(prev => ({ ...prev, is24Hours: !prev.is24Hours }))}
                                    className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${station.is24Hours ? 'bg-cyan-500' : 'bg-slate-300'}`}
                                >
                                    <span className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${station.is24Hours ? 'translate-x-6' : 'translate-x-1'}`} />
                                </button>
                            </div>

                            {!station.is24Hours && (
                                <div className="flex gap-3">
                                    <div className="flex-1">
                                        <select
                                            value={station.openTime || '08 AM'}
                                            onChange={handleChange('openTime')}
                                            className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-cyan-500 focus:border-cyan-500 transition-all text-sm text-slate-900"
                                        >
                                            {Array.from({ length: 12 }, (_, i) => i + 1).map(h => {
                                                const formatted = h < 10 ? `0${h}` : `${h}`;
                                                return (
                                                    <React.Fragment key={h}>
                                                        <option value={`${formatted} AM`}>{formatted} AM</option>
                                                        <option value={`${formatted} PM`}>{formatted} PM</option>
                                                    </React.Fragment>
                                                );
                                            })}
                                        </select>
                                    </div>
                                    <div className="flex items-center text-slate-400 font-medium">to</div>
                                    <div className="flex-1">
                                        <select
                                            value={station.closeTime || '10 PM'}
                                            onChange={handleChange('closeTime')}
                                            className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-cyan-500 focus:border-cyan-500 transition-all text-sm text-slate-900"
                                        >
                                            {Array.from({ length: 12 }, (_, i) => i + 1).map(h => {
                                                const formatted = h < 10 ? `0${h}` : `${h}`;
                                                return (
                                                    <React.Fragment key={h}>
                                                        <option value={`${formatted} AM`}>{formatted} AM</option>
                                                        <option value={`${formatted} PM`}>{formatted} PM</option>
                                                    </React.Fragment>
                                                );
                                            })}
                                        </select>
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>
                    <InputField label="Car Price (₹/kWh)" value={station.pricePerKwh || ''} onChange={handleNumberChange('pricePerKwh')} type="number" placeholder="15.50" icon={IndianRupee} />
                    <InputField label="Truck Price (₹/kWh)" value={station.truckPricePerKwh || ''} onChange={handleNumberChange('truckPricePerKwh')} type="number" placeholder="20.00" icon={Truck} />
                </div>
            </div>

            {/* Meta */}
            <div className="bg-white rounded-[24px] p-8 shadow-[0_2px_12px_rgba(0,0,0,0.03)] border border-slate-100/50">
                <h3 className="text-lg font-bold text-[#1A2234] mb-6">Station Amenities</h3>
                <div>
                    <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                        {AMENITIES_LIST.map((amenity) => {
                            const Icon = amenity.icon;
                            const isSelected = (station.amenities || []).includes(amenity.id);

                            return (
                                <button
                                    key={amenity.id}
                                    onClick={() => toggleAmenity(amenity.id)}
                                    className={`
                                        relative group flex flex-col items-center justify-center p-6 text-center border-2 rounded-2xl transition-all duration-200 ease-out
                                        ${isSelected
                                            ? 'bg-cyan-50/50 border-cyan-500 shadow-sm'
                                            : 'bg-white border-slate-100 hover:border-slate-300 hover:bg-slate-50'
                                        }
                                    `}
                                >
                                    <div className={`absolute top-3 right-3 w-5 h-5 rounded-full flex items-center justify-center transition-all ${isSelected ? 'bg-cyan-500 scale-100' : 'bg-transparent border border-slate-300 scale-90'}`}>
                                        {isSelected && <Check className="w-3 h-3 text-white" />}
                                    </div>
                                    <Icon className={`w-8 h-8 mb-3 transition-colors ${isSelected ? 'text-cyan-600' : 'text-slate-400 group-hover:text-slate-600'}`} strokeWidth={1.5} />
                                    <span className={`text-sm font-semibold transition-colors ${isSelected ? 'text-cyan-900' : 'text-slate-600'}`}>
                                        {amenity.label}
                                    </span>
                                </button>
                            );
                        })}
                    </div>
                </div>
            </div>

            {/* Save Button */}
            <div className="flex justify-end">
                <button
                    onClick={onSave}
                    disabled={saving}
                    className="flex items-center gap-2 px-8 py-3.5 bg-[#00E5FF] hover:bg-[#00B4DB] text-[#1A2234] font-bold rounded-full shadow-lg shadow-[#00E5FF]/20 transition-all disabled:opacity-50"
                >
                    {saving ? <Loader2 className="w-5 h-5 animate-spin" /> : <Save className="w-5 h-5" />}
                    {saving ? 'Saving...' : 'Save Changes'}
                </button>
            </div>
        </div>
    );
}

// ─────────────────── Tab 2: Dispensers & Connectors ───────────────────

function DispensersTab({ stationId }) {
    const [dispensers, setDispensers] = useState([]);
    const [slots, setSlots] = useState([]);
    const [loading, setLoading] = useState(true);
    const [expandedId, setExpandedId] = useState(null);
    const [addingNew, setAddingNew] = useState(false);
    const [isAddingDispensary, setIsAddingDispensary] = useState(false);
    const [connectorTypes, setConnectorTypes] = useState([]);
    const [newDispenser, setNewDispenser] = useState({ name: '', totalPowerKw: 60, acceptsTrucks: false, connectorType: 'CCS2' });

    const fetchData = useCallback(async () => {
        try {
            setLoading(true);
            const [dispRes, slotRes] = await Promise.all([
                api.get(`/dispensaries/station/${stationId}`),
                api.get(`/slots/station/${stationId}`),
            ]);
            const dispArr = Array.isArray(dispRes.data) ? dispRes.data : (dispRes.data.data || []);
            const slotArr = Array.isArray(slotRes.data) ? slotRes.data : (slotRes.data.data || []);
            setDispensers(dispArr);
            setSlots(slotArr);
        } catch (err) {
            console.error('Error fetching dispensers:', err);
        } finally {
            setLoading(false);
        }
    }, [stationId]);

    useEffect(() => { fetchData(); }, [fetchData]);

    // Fetch connector types dynamically from backend
    useEffect(() => {
        api.get('/dispensaries/connector-types')
            .then(res => setConnectorTypes(Array.isArray(res.data) ? res.data : []))
            .catch(() => setConnectorTypes(['CCS2', 'TYPE_2', 'GB_T', 'MCS']));
    }, []);

    const gunsForDispenser = (dispenserId) => slots.filter(s => s.dispensary?.id === dispenserId);

    const handleAddDispenser = async () => {
        if (!newDispenser.name.trim()) {
            toast.error('Please enter a dispenser name.');
            return;
        }
        try {
            setIsAddingDispensary(true);
            await api.post(`/dispensaries/station/${stationId}`, newDispenser);
            toast.success(`Dispenser "${newDispenser.name}" added with 2 guns.`);
            setNewDispenser({ name: '', totalPowerKw: 60, acceptsTrucks: false });
            setAddingNew(false);
            fetchData();
        } catch (err) {
            console.error('Error adding dispenser:', err);
        } finally {
            setIsAddingDispensary(false);
        }
    };

    const handleDeleteDispenser = async (id, name) => {
        if (window.confirm(`Delete dispenser "${name}" and all its guns?`)) {
            try {
                await api.delete(`/dispensaries/${id}`);
                toast.success('Dispenser deleted.');
                fetchData();
            } catch (err) {
                console.error('Error deleting dispenser:', err);
            }
        }
    };

    const handleToggleGunStatus = async (gunId, currentStatus) => {
        const newStatus = currentStatus === 'MAINTENANCE' ? 'AVAILABLE' : 'MAINTENANCE';
        try {
            await api.put(`/slots/${gunId}/status?status=${newStatus}`);
            toast.success(`Gun set to ${newStatus}.`);
            fetchData();
        } catch (err) {
            console.error('Error toggling gun status:', err);
        }
    };

    const handleChangeDispensaryConnectorType = async (dispensaryId, newType) => {
        try {
            await api.put(`/dispensaries/${dispensaryId}/connectorType?connectorType=${newType}`);
            toast.success(`Connector type updated to ${newType.replace('_', ' ')} for all guns.`);
            fetchData();
        } catch (err) {
            console.error('Error changing connector type:', err);
            toast.error('Failed to update connector type.');
        }
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center py-20">
                <Loader2 className="w-8 h-8 text-cyan-500 animate-spin" />
            </div>
        );
    }

    return (
        <div className="space-y-6">
            {/* Dispenser List */}
            {dispensers.length === 0 && !addingNew && (
                <div className="bg-white rounded-[24px] p-12 shadow-[0_2px_12px_rgba(0,0,0,0.03)] border border-slate-100/50 text-center">
                    <Zap className="w-12 h-12 text-slate-300 mx-auto mb-4" />
                    <p className="text-slate-500 font-medium mb-2">No dispensers registered yet.</p>
                    <p className="text-sm text-slate-400">Click the button below to add your first dispenser.</p>
                </div>
            )}

            {dispensers.map((disp) => {
                const guns = gunsForDispenser(disp.id);
                const isExpanded = expandedId === disp.id;

                return (
                    <div key={disp.id} className="bg-white rounded-[24px] shadow-[0_2px_12px_rgba(0,0,0,0.03)] border border-slate-100/50 overflow-hidden">
                        {/* Dispenser Header */}
                        <div
                            className="flex items-center justify-between p-6 cursor-pointer hover:bg-slate-50/50 transition-colors"
                            onClick={() => setExpandedId(isExpanded ? null : disp.id)}
                        >
                            <div className="flex items-center gap-4">
                                <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-cyan-50 to-blue-50 flex items-center justify-center">
                                    <Zap className="w-5 h-5 text-cyan-500" />
                                </div>
                                <div>
                                    <h4 className="font-bold text-[#1A2234]">{disp.name}</h4>
                                    <div className="flex items-center gap-3 mt-1">
                                        <span className="text-xs font-medium text-slate-500">{disp.totalPowerKw} kW</span>
                                        <span className="text-slate-300">·</span>
                                        <span className="text-xs font-medium text-slate-500">{guns.length} Guns</span>
                                        {disp.acceptsTrucks && (
                                            <>
                                                <span className="text-slate-300">·</span>
                                                <span className="text-xs font-semibold text-amber-600 bg-amber-50 px-2 py-0.5 rounded-full flex items-center gap-1">
                                                    <Truck className="w-3 h-3" />Trucks
                                                </span>
                                            </>
                                        )}
                                    </div>
                                </div>
                            </div>
                            <div className="flex items-center gap-3">
                                <button
                                    onClick={(e) => { e.stopPropagation(); handleDeleteDispenser(disp.id, disp.name); }}
                                    className="p-2 text-red-400 hover:text-red-600 hover:bg-red-50 rounded-xl transition-colors"
                                    title="Delete Dispenser"
                                >
                                    <Trash2 className="w-4 h-4" />
                                </button>
                                {isExpanded ? <ChevronUp className="w-5 h-5 text-slate-400" /> : <ChevronDown className="w-5 h-5 text-slate-400" />}
                            </div>
                        </div>

                        {/* Expanded Details */}
                        {isExpanded && (
                            <div className="border-t border-slate-100 bg-slate-50/30 p-6 space-y-5">
                                {/* Connector Type — reads from dispensary (source of truth) */}
                                <div>
                                    <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">Connector Type (all guns)</p>
                                    <div className="flex flex-wrap gap-2">
                                        {connectorTypes.map(ct => (
                                            <button
                                                key={ct}
                                                onClick={() => disp.connectorType !== ct && handleChangeDispensaryConnectorType(disp.id, ct)}
                                                className={`px-4 py-1.5 rounded-full text-xs font-semibold border transition-all ${
                                                    disp.connectorType === ct
                                                        ? 'bg-cyan-500 text-white border-cyan-500 shadow-sm shadow-cyan-200'
                                                        : 'bg-slate-100 text-slate-500 border-slate-200 hover:bg-slate-200'
                                                }`}
                                            >
                                                {ct.replace('_', ' ')}
                                            </button>
                                        ))}
                                    </div>
                                </div>

                                {/* Gun List */}
                                {guns.length === 0 && (
                                    <p className="text-sm text-slate-400 text-center py-4">No guns found for this dispenser.</p>
                                )}
                                {guns.map((gun) => (
                                    <div key={gun.id} className="flex items-center justify-between bg-white rounded-2xl p-5 border border-slate-100">
                                        <div className="flex items-center gap-4">
                                            <div className={`w-3 h-3 rounded-full ${gun.status === 'AVAILABLE' ? 'bg-emerald-500' : gun.status === 'CHARGING' ? 'bg-amber-400' : 'bg-red-400'}`} />
                                            <div>
                                                <p className="font-semibold text-sm text-[#1A2234]">{gun.slotNumber || gun.slotLabel}</p>
                                                <p className="text-xs text-slate-500 mt-0.5">
                                                    {gun.connectorType} · {gun.slotType} · {gun.powerRating || gun.powerKw} kW
                                                </p>
                                            </div>
                                        </div>
                                        <div className="flex items-center gap-3">
                                            <GunStatusBadge status={gun.status} />
                                            {(gun.status === 'AVAILABLE' || gun.status === 'MAINTENANCE') && (
                                                <button
                                                    onClick={() => handleToggleGunStatus(gun.id, gun.status)}
                                                    className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors
                                                        ${gun.status === 'MAINTENANCE'
                                                            ? 'bg-emerald-50 text-emerald-600 hover:bg-emerald-100'
                                                            : 'bg-red-50 text-red-500 hover:bg-red-100'
                                                        }`}
                                                >
                                                    {gun.status === 'MAINTENANCE' ? 'Set Available' : 'Set Maintenance'}
                                                </button>
                                            )}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                );
            })}

            {/* Add New Dispenser Form */}
            {addingNew && (
                <div className="bg-white rounded-[24px] p-8 shadow-[0_2px_12px_rgba(0,0,0,0.03)] border-2 border-dashed border-cyan-300 space-y-6">
                    <h4 className="font-bold text-[#1A2234]">New Dispenser</h4>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                        <InputField
                            label="Dispenser Name"
                            value={newDispenser.name}
                            onChange={(e) => setNewDispenser(prev => ({ ...prev, name: e.target.value }))}
                            placeholder="e.g., Dispenser A"
                        />
                        <InputField
                            label="Power (kW per gun)"
                            value={newDispenser.totalPowerKw}
                            onChange={(e) => setNewDispenser(prev => ({ ...prev, totalPowerKw: parseFloat(e.target.value) || 0 }))}
                            type="number"
                            placeholder="60"
                        />
                        <div className="flex flex-col justify-center">
                            <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-3">Accepts Trucks</label>
                            <label className="relative inline-flex items-center cursor-pointer w-fit">
                                <input type="checkbox" className="sr-only peer" checked={newDispenser.acceptsTrucks} onChange={(e) => setNewDispenser(prev => ({ ...prev, acceptsTrucks: e.target.checked }))} />
                                <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-amber-500"></div>
                                <span className="ml-3 text-sm font-bold text-slate-700">Accepts Trucks</span>
                            </label>
                        </div>
                    </div>
                    {/* Connector Type Selector */}
                    <div>
                        <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-3">Connector Type (applied to both guns)</label>
                        <div className="flex flex-wrap gap-2">
                            {connectorTypes.map(ct => (
                                <button
                                    key={ct}
                                    type="button"
                                    onClick={() => setNewDispenser(prev => ({ ...prev, connectorType: ct }))}
                                    className={`px-4 py-2 rounded-full text-sm font-semibold border transition-all ${
                                        newDispenser.connectorType === ct
                                            ? 'bg-cyan-500 text-white border-cyan-500 shadow-sm shadow-cyan-200'
                                            : 'bg-slate-100 text-slate-500 border-slate-200 hover:bg-slate-200'
                                    }`}
                                >
                                    {ct.replace('_', ' ')}
                                </button>
                            ))}
                        </div>
                    </div>
                    <div className="flex justify-end gap-3">
                        <button onClick={() => setAddingNew(false)} className="px-5 py-2.5 text-sm font-medium text-slate-500 hover:text-slate-700 transition-colors">
                            Cancel
                        </button>
                        <button
                            onClick={handleAddDispenser}
                            disabled={isAddingDispensary}
                            className="flex items-center gap-2 px-6 py-2.5 bg-[#00E5FF] hover:bg-[#00B4DB] text-[#1A2234] font-bold rounded-full shadow-lg shadow-[#00E5FF]/20 transition-all text-sm disabled:opacity-50"
                        >
                            {isAddingDispensary ? (
                                <Loader2 className="w-4 h-4 animate-spin" />
                            ) : (
                                <Plus className="w-4 h-4" />
                            )}
                            {isAddingDispensary ? 'Adding...' : 'Add Dispenser'}
                        </button>
                    </div>
                </div>
            )}

            {/* Add Dispenser Button */}
            {!addingNew && (
                <button
                    onClick={() => {
                        setNewDispenser(prev => ({ ...prev, name: `Dispenser ${dispensers.length + 1}` }));
                        setAddingNew(true);
                    }}
                    className="w-full py-5 rounded-[24px] border-2 border-dashed border-slate-200 flex items-center justify-center gap-2 text-slate-500 font-semibold hover:border-cyan-300 hover:text-cyan-600 hover:bg-cyan-50/20 transition-all"
                >
                    <Plus className="w-5 h-5" />
                    Add New Dispenser
                </button>
            )}
        </div>
    );
}

// ─────────────────── Main Page ───────────────────

export default function ManageStationPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [station, setStation] = useState(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [activeTab, setActiveTab] = useState('overview');

    useEffect(() => {
        const fetchStation = async () => {
            try {
                setLoading(true);
                const res = await api.get(`/stations/${id}`);
                const data = res.data?.data || res.data;

                // Parse operating hours
                if (data.operatingHours && data.operatingHours !== "24 Hours") {
                    data.is24Hours = false;
                    const parts = data.operatingHours.split(' - ');
                    if (parts.length === 2) {
                        data.openTime = parts[0];
                        data.closeTime = parts[1];
                    }
                } else {
                    data.is24Hours = true;
                    data.openTime = '08 AM';
                    data.closeTime = '10 PM';
                }

                // Parse meta
                try {
                    const parsedMeta = JSON.parse(data.meta || "{}");
                    data.amenities = parsedMeta.amenities || [];
                } catch (e) {
                    data.amenities = [];
                }

                setStation(data);
            } catch (err) {
                console.error('Error fetching station:', err);
                toast.error('Failed to load station.');
            } finally {
                setLoading(false);
            }
        };
        fetchStation();
    }, [id]);

    const handleSaveOverview = async () => {
        try {
            setSaving(true);
            const payload = {
                ...station,
                operatingHours: station.is24Hours ? "24 Hours" : `${station.openTime || '08 AM'} - ${station.closeTime || '10 PM'}`,
                meta: JSON.stringify({ amenities: station.amenities || [] })
            };
            await api.put(`/stations/${id}`, payload);
            toast.success('Station settings saved!');
            navigate('/owner/stations');
        } catch (err) {
            console.error('Error saving station:', err);
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center min-h-[60vh]">
                <Loader2 className="w-10 h-10 text-cyan-500 animate-spin" />
            </div>
        );
    }

    return (
        <div className="max-w-[1200px] mx-auto pb-12 space-y-8">
            {/* Back Button + Station Name */}
            <div className="flex items-center gap-4">
                <button
                    onClick={() => navigate('/owner/stations')}
                    className="p-2.5 bg-white rounded-xl border border-slate-100 shadow-sm hover:bg-slate-50 transition-colors"
                >
                    <ArrowLeft className="w-5 h-5 text-slate-600" />
                </button>
                <div>
                    <h1 className="text-2xl font-bold text-[#1A2234]">{station?.name}</h1>
                    <p className="text-sm text-slate-500 flex items-center gap-1.5 mt-0.5">
                        <MapPin className="w-3.5 h-3.5" />
                        {station?.address}
                    </p>
                </div>
            </div>

            {/* Tab Bar */}
            <div className="flex gap-2 bg-slate-100/60 p-1.5 rounded-full w-fit">
                <TabButton active={activeTab === 'overview'} icon={Settings} label="Overview & Settings" onClick={() => setActiveTab('overview')} />
                <TabButton active={activeTab === 'dispensers'} icon={Zap} label="Dispensers & Connectors" onClick={() => setActiveTab('dispensers')} />
            </div>

            {/* Tab Content */}
            {activeTab === 'overview' && (
                <OverviewTab station={station} setStation={setStation} onSave={handleSaveOverview} saving={saving} />
            )}
            {activeTab === 'dispensers' && (
                <DispensersTab stationId={id} />
            )}
        </div>
    );
}
