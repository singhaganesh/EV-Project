import React, { useState, useEffect } from 'react';
import { X, Plus, Trash2, MapPin, Coffee, Wifi, Car, CreditCard, BatteryCharging, Check, ArrowRight, ArrowLeft } from 'lucide-react';
import api from '../../api/axios';
import toast from 'react-hot-toast';

// Available Amenities (Icons map to logical features)
const AMENITIES_LIST = [
    { id: 'Cafe', label: 'Cafe & Snacks', icon: Coffee },
    { id: 'WiFi', label: 'Free WiFi', icon: Wifi },
    { id: 'Restroom', label: 'Restrooms', icon: Car },
    { id: 'CCTV', label: '24/7 CCTV', icon: BatteryCharging },
    { id: 'Shopping', label: 'Shopping Area', icon: CreditCard },
    { id: 'Lounge', label: 'Waiting Lounge', icon: Coffee }
];

export default function AddStationModal({ isOpen, onClose, onSuccess }) {
    const [loading, setLoading] = useState(false);
    const [currentStep, setCurrentStep] = useState(1);
    const totalSteps = 4;

    const [formData, setFormData] = useState({
        name: '',
        address: '',
        latitude: '',
        longitude: '',
        is24Hours: true,
        openTime: '08 AM',
        closeTime: '10 PM',
        pricePerKwh: '',
        truckPricePerKwh: '',
        amenities: [],
        dispensaries: [
            { name: 'Dispenser 1', totalPowerKw: 60, acceptsTrucks: true }
        ]
    });

    useEffect(() => {
        if (isOpen) {
            setCurrentStep(1);
            setFormData({
                name: '',
                address: '',
                latitude: '',
                longitude: '',
                is24Hours: true,
                openTime: '08 AM',
                closeTime: '10 PM',
                pricePerKwh: '',
                truckPricePerKwh: '',
                amenities: [],
                dispensaries: [
                    { name: 'Dispenser 1', totalPowerKw: 60, acceptsTrucks: true }
                ]
            });
        }
    }, [isOpen]);

    if (!isOpen) return null;

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const toggleAmenity = (id) => {
        setFormData(prev => {
            const current = prev.amenities;
            const isSelected = current.includes(id);
            return {
                ...prev,
                amenities: isSelected
                    ? current.filter(a => a !== id)
                    : [...current, id]
            };
        });
    };

    const handleDispensaryChange = (index, field, value) => {
        const updatedDispensaries = [...formData.dispensaries];
        updatedDispensaries[index][field] = value;
        setFormData(prev => ({ ...prev, dispensaries: updatedDispensaries }));
    };

    const addDispensary = () => {
        setFormData(prev => ({
            ...prev,
            dispensaries: [
                ...prev.dispensaries,
                { name: `Dispenser ${prev.dispensaries.length + 1}`, totalPowerKw: 60, acceptsTrucks: true }
            ]
        }));
    };

    const removeDispensary = (index) => {
        const updatedDispensaries = formData.dispensaries.filter((_, i) => i !== index);
        setFormData(prev => ({ ...prev, dispensaries: updatedDispensaries }));
    };

    const nextStep = () => {
        // Basic validation before jumping
        if (currentStep === 1) {
            if (!formData.name || !formData.address || !formData.latitude || !formData.longitude) {
                toast.error("Please fill all basic information");
                return;
            }
        }
        if (currentStep === 3) {
            if (!formData.pricePerKwh || !formData.truckPricePerKwh) {
                toast.error("Please fill pricing configurations");
                return;
            }
        }
        setCurrentStep(prev => Math.min(prev + 1, totalSteps));
    };

    const prevStep = () => {
        setCurrentStep(prev => Math.max(prev - 1, 1));
    };

    const handleSubmit = async (e) => {
        if (e) e.preventDefault();
        if (currentStep !== totalSteps) return;

        setLoading(true);
        try {
            const userStr = localStorage.getItem('user');
            const user = userStr ? JSON.parse(userStr) : null;

            // We will stringify amenities into the 'meta' column 
            const metaJson = JSON.stringify({ amenities: formData.amenities });

            const payload = {
                ...formData,
                operatingHours: formData.is24Hours ? "24 Hours" : `${formData.openTime} - ${formData.closeTime}`,
                meta: metaJson,      // Store JSON here as supported by backend
                owner: user ? { id: user.id } : null,
                latitude: parseFloat(formData.latitude),
                longitude: parseFloat(formData.longitude),
                pricePerKwh: parseFloat(formData.pricePerKwh),
                truckPricePerKwh: parseFloat(formData.truckPricePerKwh),
            };

            await api.post('/stations', payload);
            toast.success('Station Registered & Launched successfully!');
            onSuccess();
            onClose();
        } catch (error) {
            console.error('Error adding station:', error);
        } finally {
            setLoading(false);
        }
    };

    const stepTitles = ["Basic Information", "Station Amenities", "Pricing Setup", "Dispensary Config"];

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 bg-slate-900/60 backdrop-blur-md">
            {/* 
        We use an asymmetric 90/10 split aesthetic for the wizard. 
        A sidebar for progress, and the main canvas for action.
      */}
            <div className="bg-white rounded-3xl w-full max-w-5xl h-[85vh] flex overflow-hidden shadow-2xl ring-1 ring-slate-900/5">

                {/* Left Side: Progress Fragment (Hidden on mobile) */}
                <div className="hidden md:flex flex-col w-[280px] bg-slate-50 border-r border-slate-100 p-8">
                    <div className="mb-12">
                        <h2 className="text-2xl font-bold tracking-tight text-slate-900 leading-tight">
                            Create<br />Station
                        </h2>
                        <p className="text-sm text-slate-500 mt-2">Deploy a new hub into the EV network.</p>
                    </div>

                    <div className="relative flex-1">
                        {/* Connecting Line */}
                        <div className="absolute left-[15px] top-[14px] bottom-[14px] w-0.5 bg-slate-200">
                            <div
                                className="w-full bg-cyan-500 transition-all duration-500 ease-out"
                                style={{ height: `${((currentStep - 1) / (totalSteps - 1)) * 100}%` }}
                            />
                        </div>

                        <ul className="relative space-y-8">
                            {stepTitles.map((title, index) => {
                                const step = index + 1;
                                const isActive = step === currentStep;
                                const isPast = step < currentStep;

                                return (
                                    <li key={step} className={`flex items-start gap-4 transition-opacity duration-300 ${!isActive && !isPast ? 'opacity-40' : ''}`}>
                                        <div className={`
                      relative z-10 w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold border-2
                      ${isActive ? 'bg-white border-cyan-500 text-cyan-500 shadow-sm' : ''}
                      ${isPast ? 'bg-cyan-500 border-cyan-500 text-white' : ''}
                      ${!isActive && !isPast ? 'bg-slate-50 border-slate-300 text-slate-400' : ''}
                    `}>
                                            {isPast ? <Check className="w-4 h-4" /> : step}
                                        </div>
                                        <div className="pt-1.5">
                                            <p className={`text-sm font-semibold ${isActive ? 'text-slate-900' : 'text-slate-600'}`}>
                                                {title}
                                            </p>
                                        </div>
                                    </li>
                                );
                            })}
                        </ul>
                    </div>
                </div>

                {/* Right Side: Main Interactive Area */}
                <div className="flex-1 flex flex-col relative bg-white">
                    {/* Header Mobile Support & Close */}
                    <div className="flex items-center justify-end p-6 md:p-8">
                        <button onClick={onClose} className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-50 rounded-full transition-colors">
                            <X className="w-5 h-5" />
                        </button>
                    </div>

                    {/* Dynamic Step Content */}
                    <div className="flex-1 px-6 md:px-12 overflow-y-auto pb-24">

                        <h3 className="text-3xl font-bold tracking-tight text-slate-900 mb-8 font-serif">
                            {stepTitles[currentStep - 1]}
                        </h3>

                        {/* STEP 1: Basic Information */}
                        <div className={`transition-all duration-300 ${currentStep === 1 ? 'opacity-100 translate-x-0 block' : 'opacity-0 translate-x-4 hidden'}`}>
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-7">
                                <div>
                                    <label className="block text-sm font-semibold text-slate-700 mb-2">Station Name</label>
                                    <input required autoFocus type="text" name="name" value={formData.name} onChange={handleChange} className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-cyan-500 focus:border-cyan-500 focus:bg-white transition-all text-slate-900" placeholder="e.g. Nexus Energy Point" />
                                </div>
                                {/* Operating Hours Field */}
                                <div>
                                    <label className="block text-sm font-semibold text-slate-700 mb-2">Operating Hours</label>
                                    <div className="space-y-3">
                                        <div className="flex items-center justify-between p-3 bg-slate-50 border border-slate-200 rounded-xl">
                                            <span className="text-sm font-medium text-slate-700">Open 24 Hours</span>
                                            <button
                                                type="button"
                                                onClick={() => setFormData({ ...formData, is24Hours: !formData.is24Hours })}
                                                className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${formData.is24Hours ? 'bg-cyan-500' : 'bg-slate-300'}`}
                                            >
                                                <span className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${formData.is24Hours ? 'translate-x-6' : 'translate-x-1'}`} />
                                            </button>
                                        </div>

                                        {!formData.is24Hours && (
                                            <div className="flex gap-3">
                                                <div className="flex-1">
                                                    <select
                                                        name="openTime"
                                                        value={formData.openTime}
                                                        onChange={handleChange}
                                                        className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-cyan-500 focus:border-cyan-500 transition-all text-sm text-slate-900"
                                                    >
                                                        {Array.from({ length: 12 }, (_, i) => i + 1).map(h => (
                                                            <React.Fragment key={h}>
                                                                <option value={`${h} AM`}>{h} AM</option>
                                                                <option value={`${h} PM`}>{h} PM</option>
                                                            </React.Fragment>
                                                        ))}
                                                    </select>
                                                </div>
                                                <div className="flex items-center text-slate-400 font-medium">to</div>
                                                <div className="flex-1">
                                                    <select
                                                        name="closeTime"
                                                        value={formData.closeTime}
                                                        onChange={handleChange}
                                                        className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-cyan-500 focus:border-cyan-500 transition-all text-sm text-slate-900"
                                                    >
                                                        {Array.from({ length: 12 }, (_, i) => i + 1).map(h => (
                                                            <React.Fragment key={h}>
                                                                <option value={`${h} AM`}>{h} AM</option>
                                                                <option value={`${h} PM`}>{h} PM</option>
                                                            </React.Fragment>
                                                        ))}
                                                    </select>
                                                </div>
                                            </div>
                                        )}
                                    </div>
                                </div>
                                <div className="md:col-span-2">
                                    <label className="block text-sm font-semibold text-slate-700 mb-2">Full Address</label>
                                    <div className="relative">
                                        <MapPin className="absolute left-4 top-3.5 w-5 h-5 text-slate-400" />
                                        <input required type="text" name="address" value={formData.address} onChange={handleChange} className="w-full pl-12 pr-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-cyan-500 focus:border-cyan-500 focus:bg-white transition-all text-slate-900" placeholder="123 Energy Blvd, Tech District" />
                                    </div>
                                </div>
                                <div>
                                    <label className="block text-sm font-semibold text-slate-700 mb-2">Latitude</label>
                                    <input required type="number" step="any" name="latitude" value={formData.latitude} onChange={handleChange} className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-cyan-500 focus:border-cyan-500 focus:bg-white transition-all font-mono text-sm" placeholder="28.6139" />
                                </div>
                                <div>
                                    <label className="block text-sm font-semibold text-slate-700 mb-2">Longitude</label>
                                    <input required type="number" step="any" name="longitude" value={formData.longitude} onChange={handleChange} className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-cyan-500 focus:border-cyan-500 focus:bg-white transition-all font-mono text-sm" placeholder="77.2090" />
                                </div>
                            </div>
                        </div>

                        {/* STEP 2: Amenities */}
                        <div className={`transition-all duration-300 ${currentStep === 2 ? 'opacity-100 translate-x-0 block' : 'opacity-0 translate-x-4 hidden'}`}>
                            <p className="text-slate-500 mb-6">Select available facilities for waiting drivers to increase your station's attractiveness.</p>

                            <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                                {AMENITIES_LIST.map((amenity) => {
                                    const Icon = amenity.icon;
                                    const isSelected = formData.amenities.includes(amenity.id);

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
                                            {/* Selection Checkmark */}
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

                        {/* STEP 3: Pricing Setup */}
                        <div className={`transition-all duration-300 ${currentStep === 3 ? 'opacity-100 translate-x-0 block' : 'opacity-0 translate-x-4 hidden'}`}>
                            <div className="bg-slate-50 p-6 rounded-2xl border border-slate-100 mb-8">
                                <p className="text-sm text-slate-600 leading-relaxed">
                                    Configure per-kWh rates. Trucks usually command a higher premium due to taking up multiple slots and requiring specialized high-power infrastructure.
                                </p>
                            </div>

                            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                                <div className="bg-white p-6 rounded-2xl border-2 border-slate-100 shadow-sm relative overflow-hidden group focus-within:border-cyan-500 transition-colors">
                                    <div className="absolute top-0 right-0 w-24 h-24 bg-slate-50 rounded-bl-full -z-10 transition-colors group-focus-within:bg-cyan-50/50" />
                                    <label className="block text-sm font-bold text-slate-800 mb-4 flex items-center gap-2">
                                        <Car className="w-5 h-5 text-slate-400" /> Standard Car Rate
                                    </label>
                                    <div className="flex items-center">
                                        <span className="text-xl font-medium text-slate-400 mr-2">Rs.</span>
                                        <input autoFocus required type="number" step="0.5" name="pricePerKwh" value={formData.pricePerKwh} onChange={handleChange} className="w-full text-3xl font-bold bg-transparent border-0 focus:outline-none focus:ring-0 p-0 text-slate-900 placeholder:text-slate-300" placeholder="15.0" />
                                    </div>
                                    <div className="text-xs font-medium text-slate-400 uppercase tracking-wider mt-2">Per kWh</div>
                                </div>

                                <div className="bg-white p-6 rounded-2xl border-2 border-slate-100 shadow-sm relative overflow-hidden group focus-within:border-orange-500 transition-colors">
                                    <div className="absolute top-0 right-0 w-24 h-24 bg-slate-50 rounded-bl-full -z-10 transition-colors group-focus-within:bg-orange-50/50" />
                                    <label className="block text-sm font-bold text-slate-800 mb-4 flex items-center gap-2">
                                        <BatteryCharging className="w-5 h-5 text-slate-400" /> Commercial Truck Rate
                                    </label>
                                    <div className="flex items-center">
                                        <span className="text-xl font-medium text-slate-400 mr-2">Rs.</span>
                                        <input required type="number" step="0.5" name="truckPricePerKwh" value={formData.truckPricePerKwh} onChange={handleChange} className="w-full text-3xl font-bold bg-transparent border-0 focus:outline-none focus:ring-0 p-0 text-slate-900 placeholder:text-slate-300" placeholder="20.0" />
                                    </div>
                                    <div className="text-xs font-medium text-slate-400 uppercase tracking-wider mt-2">Per kWh</div>
                                </div>
                            </div>
                        </div>

                        {/* STEP 4: Dispensary Config */}
                        <div className={`transition-all duration-300 ${currentStep === 4 ? 'opacity-100 translate-x-0 block' : 'opacity-0 translate-x-4 hidden'}`}>
                            <div className="flex items-center justify-between mb-6">
                                <p className="text-slate-500 text-sm max-w-md">Each dispensary unit physically splits its power into 2 charging slots.</p>

                                <button type="button" onClick={addDispensary} className="flex items-center gap-1.5 text-sm font-bold text-slate-700 bg-white border border-slate-200 px-4 py-2 rounded-xl hover:bg-slate-50 hover:border-slate-300 transition-all shadow-sm">
                                    <Plus className="w-4 h-4" /> Add Unit
                                </button>
                            </div>

                            <div className="space-y-4">
                                {formData.dispensaries.map((disp, index) => (
                                    <div key={index} className="bg-white border-2 border-slate-100 p-5 rounded-2xl relative group transition-all hover:border-slate-200 shadow-sm hover:shadow-md">
                                        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                                            <div>
                                                <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">Hardware Name</label>
                                                <input required type="text" value={disp.name} onChange={(e) => handleDispensaryChange(index, 'name', e.target.value)} className="w-full px-0 py-2 text-base font-semibold border-0 border-b-2 border-slate-100 focus:outline-none focus:ring-0 focus:border-cyan-500 bg-transparent transition-colors shadow-none" placeholder="e.g. Dual-Gun C2" />
                                            </div>
                                            <div>
                                                <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">Power Output (kW)</label>
                                                <div className="relative">
                                                    <input required type="number" step="1" value={disp.totalPowerKw} onChange={(e) => handleDispensaryChange(index, 'totalPowerKw', parseFloat(e.target.value))} className="w-full px-0 py-2 pl-6 font-mono text-lg font-bold border-0 border-b-2 border-slate-100 focus:outline-none focus:ring-0 focus:border-cyan-500 bg-transparent transition-colors shadow-none" placeholder="60" />
                                                    <span className="absolute left-0 top-3 border-r border-transparent pr-2 text-slate-400 font-bold">kW</span>
                                                </div>
                                            </div>
                                            <div className="flex items-center md:justify-end pt-2 md:pt-6">
                                                <label className="relative inline-flex items-center cursor-pointer">
                                                    <input type="checkbox" className="sr-only peer" checked={disp.acceptsTrucks} onChange={(e) => handleDispensaryChange(index, 'acceptsTrucks', e.target.checked)} />
                                                    <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-cyan-500"></div>
                                                    <span className="ml-3 text-sm font-bold text-slate-700">Accepts Trucks</span>
                                                </label>
                                            </div>
                                        </div>
                                        {formData.dispensaries.length > 1 && (
                                            <button type="button" onClick={() => removeDispensary(index)} className="absolute -right-3 -top-3 bg-red-50 text-red-500 p-2 rounded-full border border-red-100 hover:bg-red-500 hover:text-white opacity-0 group-hover:opacity-100 transition-all shadow-sm">
                                                <Trash2 className="w-4 h-4" />
                                            </button>
                                        )}
                                    </div>
                                ))}
                            </div>
                        </div>

                    </div>

                    {/* Absolute Navigation Footer overlaying bottom */}
                    <div className="absolute bottom-0 left-0 right-0 bg-white/80 backdrop-blur-md border-t border-slate-100 p-6 z-20 flex justify-between items-center rounded-br-3xl md:rounded-bl-none rounded-bl-3xl">
                        <button
                            type="button"
                            onClick={currentStep === 1 ? onClose : prevStep}
                            className="px-5 py-2.5 text-sm font-bold text-slate-500 hover:text-slate-800 hover:bg-slate-50 rounded-xl transition-all flex items-center gap-2"
                        >
                            {currentStep === 1 ? 'Cancel' : <><ArrowLeft className="w-4 h-4" /> Back</>}
                        </button>

                        {currentStep < totalSteps ? (
                            <button
                                type="button"
                                onClick={nextStep}
                                className="px-8 py-3 text-sm font-bold text-white bg-slate-900 rounded-xl hover:bg-cyan-600 transition-all shadow-lg hover:shadow-cyan-600/20 flex items-center gap-2"
                            >
                                Continue <ArrowRight className="w-4 h-4" />
                            </button>
                        ) : (
                            <button
                                type="button"
                                onClick={handleSubmit}
                                disabled={loading}
                                className="px-8 py-3 text-sm font-bold text-white bg-cyan-600 rounded-xl hover:bg-cyan-700 transition-all shadow-lg hover:shadow-cyan-600/30 disabled:opacity-70 flex items-center gap-2"
                            >
                                {loading ? 'Initializing Station...' : <><Check className="w-4 h-4" /> Confirm & Launch</>}
                            </button>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}
