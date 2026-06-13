import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { Zap, Mail, Lock, User, Loader2, Building2, Hash, Phone, Banknote, FileText, ShieldCheck, Wand2, Eye, EyeOff } from 'lucide-react';
import toast from 'react-hot-toast';
import api from '../../api/axios';
import { selectCurrentUser } from '../../store/authSlice';

const EMPTY_FORM = {
    name: '',
    email: '',
    password: '',
    companyName: '',
    taxId: '',
    phoneNumber: '',
    bankAccountNumber: '',
    bankIfscCode: '',
};

// Dev-only mock PDF so registration can be tested without real documents.
const mockPdf = (name) => new File([new Blob(['mock content'], { type: 'application/pdf' })], name, { type: 'application/pdf' });

const inputClass =
    'block w-full pl-10 sm:text-sm border-slate-200 rounded-lg py-3 bg-slate-50 focus:bg-white focus:ring-2 focus:ring-cyan-500 focus:border-cyan-500 transition-colors';

const TextField = ({ icon: Icon, disabled, ...props }) => {
    const [showPassword, setShowPassword] = useState(false);
    const isPassword = props.type === 'password';
    const inputType = isPassword ? (showPassword ? 'text' : 'password') : props.type;

    return (
        <div className="mt-2 relative rounded-md shadow-sm">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <Icon className="h-5 w-5 text-slate-400" />
            </div>
            <input 
                className={`${inputClass} ${isPassword ? 'pr-10' : ''}`} 
                disabled={disabled} 
                {...props} 
                type={inputType}
            />
            {isPassword && (
                <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-600 transition-colors z-20"
                >
                    {showPassword ? (
                        <EyeOff className="h-5 w-5" />
                    ) : (
                        <Eye className="h-5 w-5" />
                    )}
                </button>
            )}
        </div>
    );
};

export default function RegisterPage() {
    const [form, setForm] = useState(EMPTY_FORM);
    const [files, setFiles] = useState({ registrationDoc: null, electricityDoc: null, bankDoc: null });
    const [loading, setLoading] = useState(false);

    // Form wizard steps: 1 (Contact), 2 (Business & Bank), 3 (Documents), 4 (OTP Verification)
    const [step, setStep] = useState(1);
    const [userId, setUserId] = useState(null);
    const [otp, setOtp] = useState('');

    const navigate = useNavigate();
    const user = useSelector(selectCurrentUser);

    useEffect(() => {
        if (user) {
            if (user.role === 'ADMIN') navigate('/admin', { replace: true });
            else if (user.role === 'STATION_OWNER') navigate('/owner', { replace: true });
        }
    }, [user, navigate]);

    const updateField = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));
    const updateFile = (key) => (e) => setFiles((f) => ({ ...f, [key]: e.target.files?.[0] || null }));

    const isStep1Valid = () => {
        return (
            form.name.trim() !== '' &&
            form.email.trim() !== '' &&
            /\S+@\S+\.\S+/.test(form.email) &&
            form.password.length >= 6 &&
            form.phoneNumber.trim() !== ''
        );
    };

    const isStep2Valid = () => {
        return (
            form.companyName.trim() !== '' &&
            form.taxId.trim() !== '' &&
            form.bankAccountNumber.trim() !== '' &&
            form.bankIfscCode.trim() !== ''
        );
    };

    const isStep3Valid = () => {
        return files.registrationDoc && files.electricityDoc && files.bankDoc;
    };

    const handleAutofill = () => {
        setForm({
            name: 'Test Pump Owner',
            email: 'owner_test_' + Date.now() + '@ev.com',
            password: 'password123',
            companyName: 'Dev Stations Ltd',
            taxId: 'GSTIN-DEV-12345',
            phoneNumber: '9999988888',
            bankAccountNumber: '1234567890',
            bankIfscCode: 'IFSC0001234',
        });
        setFiles({
            registrationDoc: mockPdf('registration.pdf'),
            electricityDoc: mockPdf('electricity.pdf'),
            bankDoc: mockPdf('bank.pdf'),
        });
        setStep(3); // Jump straight to step 3 documents upload for easy developer test submission
        toast.success('Test data filled & jumped to Step 3');
    };

    const handleRegister = async (e) => {
        e.preventDefault();
        if (!isStep3Valid()) {
            toast.error('Please attach all three documents.');
            return;
        }
        setLoading(true);

        try {
            const fd = new FormData();
            Object.entries(form).forEach(([k, v]) => fd.append(k, v));
            fd.append('registrationDoc', files.registrationDoc);
            fd.append('electricityDoc', files.electricityDoc);
            fd.append('bankDoc', files.bankDoc);

            const response = await api.post('/auth/register/owner', fd, {
                headers: { 'Content-Type': 'multipart/form-data' },
            });
            const { success, message, data } = response.data;

            if (!success) {
                toast.error(message || 'Registration failed. Please try again.');
                return;
            }

            setUserId(data.userId);
            if (data.otp) setOtp(data.otp); // dev convenience when OTP is exposed
            setStep(4);
            toast.success('Verification code sent to your email.');
        } catch (err) {
            const msg = err.response?.data?.message || 'Registration failed. Please try again.';
            toast.error(msg);
        } finally {
            setLoading(false);
        }
    };

    const handleResend = async () => {
        try {
            const response = await api.post('/auth/resend-verification', { userId });
            if (response.data?.data?.otp) setOtp(response.data.data.otp);
            toast.success('A new code has been sent.');
        } catch (err) {
            toast.error(err.response?.data?.message || 'Could not resend the code.');
        }
    };

    const handleVerify = async (e) => {
        e.preventDefault();
        setLoading(true);

        try {
            const response = await api.post('/auth/verify-registration', { userId, otp });
            const { success, message, data } = response.data;

            if (!success) {
                toast.error(message || 'Invalid code');
                return;
            }

            if (data?.status === 'APPROVED') {
                toast.success('Email verified! Your account is approved — please sign in.');
            } else {
                toast.success('Email verified! Your account is pending admin approval.');
            }
            navigate('/login', { replace: true });
        } catch (err) {
            const msg = err.response?.data?.message || 'Verification failed. Please try again.';
            toast.error(msg);
        } finally {
            setLoading(false);
        }
    };

    const stepsInfo = [
        { id: 1, label: 'Contact Details' },
        { id: 2, label: 'Business & Bank' },
        { id: 3, label: 'Documents' },
    ];

    const heading = step === 4 ? 'Verify your email' : 'Partner with Plugsy';
    const subHeading = step === 4 ? (
        <>Enter the 6-digit code we emailed to <span className="font-medium">{form.email}</span></>
    ) : (
        <>
            Register your station business or{' '}
            <Link to="/login" className="font-medium text-cyan-600 hover:text-cyan-500 transition-colors">
                sign in to your existing account
            </Link>
        </>
    );

    return (
        <div className="min-h-screen bg-[#F4F7FE] flex flex-col justify-center py-12 sm:px-6 lg:px-8">
            <div className="sm:mx-auto sm:w-full sm:max-w-lg text-center">
                <div className="w-16 h-16 rounded-full bg-[#1A2234] mx-auto flex items-center justify-center shadow-lg shadow-slate-900/20">
                    {step === 4 ? <ShieldCheck className="w-8 h-8 text-cyan-400" /> : <Zap className="w-8 h-8 text-cyan-400" />}
                </div>
                <h2 className="mt-6 text-center text-3xl font-extrabold text-[#1A2234]">
                    {heading}
                </h2>
                <p className="mt-2 text-center text-sm text-slate-500">
                    {subHeading}
                </p>
            </div>

            <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-lg">
                <div className="bg-white py-8 px-4 shadow-xl shadow-slate-200/40 sm:rounded-2xl sm:px-10 border border-slate-100">
                    {import.meta.env.DEV && step <= 3 && (
                        <button
                            type="button"
                            onClick={handleAutofill}
                            className="mb-6 w-full flex justify-center items-center gap-2 py-2 px-4 rounded-lg text-xs font-medium text-amber-700 bg-amber-50 border border-amber-200 hover:bg-amber-100 transition-colors"
                        >
                            <Wand2 className="w-4 h-4" /> Dev: fill test data
                        </button>
                    )}

                    {/* Progress Step Indicator */}
                    {step <= 3 && (
                        <div className="mb-8 relative px-4">
                            <div className="flex items-center justify-between relative">
                                {/* Background Line */}
                                <div className="absolute top-4 left-0 right-0 h-0.5 bg-slate-100 z-0" />
                                {/* Active Progress Line */}
                                <div 
                                    className="absolute top-4 left-0 h-0.5 bg-cyan-500 transition-all duration-300 z-0"
                                    style={{ width: `${((step - 1) / (stepsInfo.length - 1)) * 100}%` }}
                                />
                                
                                {stepsInfo.map((s) => {
                                    const isCompleted = step > s.id;
                                    const isActive = step === s.id;
                                    return (
                                        <div key={s.id} className="flex flex-col items-center z-10 relative">
                                            <div className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold transition-all duration-300 ${
                                                isCompleted ? 'bg-cyan-500 text-white' : 
                                                isActive ? 'bg-[#1A2234] text-white ring-4 ring-cyan-100 border-2 border-cyan-400' : 
                                                'bg-white border-2 border-slate-200 text-slate-400'
                                            }`}>
                                                {isCompleted ? '✓' : s.id}
                                            </div>
                                            <span className={`mt-2 text-[11px] font-semibold transition-colors duration-300 hidden sm:block ${
                                                isActive ? 'text-[#1A2234]' : isCompleted ? 'text-cyan-600' : 'text-slate-400'
                                            }`}>
                                                {s.label}
                                            </span>
                                        </div>
                                    );
                                })}
                            </div>
                        </div>
                    )}

                    {step === 1 && (
                        <div className="space-y-5">
                            <div>
                                <label className="block text-sm font-medium text-slate-700">Owner / contact name</label>
                                <TextField icon={User} type="text" required value={form.name} onChange={updateField('name')} placeholder="Your name" disabled={loading} />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-700">Email address</label>
                                <TextField icon={Mail} type="email" required value={form.email} onChange={updateField('email')} placeholder="business@example.com" disabled={loading} />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-700">Password</label>
                                <TextField icon={Lock} type="password" required minLength={6} value={form.password} onChange={updateField('password')} placeholder="••••••••" disabled={loading} />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-700">Phone number</label>
                                <TextField icon={Phone} type="tel" required value={form.phoneNumber} onChange={updateField('phoneNumber')} placeholder="9876543210" disabled={loading} />
                            </div>

                            <button
                                type="button"
                                disabled={!isStep1Valid()}
                                onClick={() => setStep(2)}
                                className="w-full flex justify-center items-center gap-2 py-3 px-4 border border-transparent rounded-xl shadow-md shadow-slate-900/20 text-sm font-medium text-white bg-[#1A2234] hover:bg-slate-800 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-slate-900 transition-all active:scale-[0.98] disabled:opacity-60 disabled:cursor-not-allowed"
                            >
                                Next Step
                            </button>
                        </div>
                    )}

                    {step === 2 && (
                        <div className="space-y-5">
                            <div>
                                <label className="block text-sm font-medium text-slate-700">Company name</label>
                                <TextField icon={Building2} type="text" required value={form.companyName} onChange={updateField('companyName')} placeholder="Your Stations Pvt Ltd" disabled={loading} />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-700">Tax ID (GSTIN / PAN)</label>
                                <TextField icon={Hash} type="text" required value={form.taxId} onChange={updateField('taxId')} placeholder="22AAAAA0000A1Z5" disabled={loading} />
                            </div>
                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-slate-700">Bank account no.</label>
                                    <TextField icon={Banknote} type="text" required value={form.bankAccountNumber} onChange={updateField('bankAccountNumber')} placeholder="1234567890" disabled={loading} />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-slate-700">IFSC code</label>
                                    <TextField icon={Hash} type="text" required value={form.bankIfscCode} onChange={updateField('bankIfscCode')} placeholder="HDFC0001234" disabled={loading} />
                                </div>
                            </div>

                            <div className="flex gap-4">
                                <button
                                    type="button"
                                    onClick={() => setStep(1)}
                                    className="flex-1 py-3 px-4 border border-slate-200 rounded-xl text-sm font-medium text-slate-700 bg-white hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-slate-500 transition-all active:scale-[0.98]"
                                >
                                    Back
                                </button>
                                <button
                                    type="button"
                                    disabled={!isStep2Valid()}
                                    onClick={() => setStep(3)}
                                    className="flex-1 flex justify-center items-center gap-2 py-3 px-4 border border-transparent rounded-xl shadow-md shadow-slate-900/20 text-sm font-medium text-white bg-[#1A2234] hover:bg-slate-800 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-slate-900 transition-all active:scale-[0.98] disabled:opacity-60 disabled:cursor-not-allowed"
                                >
                                    Next Step
                                </button>
                            </div>
                        </div>
                    )}

                    {step === 3 && (
                        <form className="space-y-5" onSubmit={handleRegister}>
                            <div className="space-y-3">
                                <p className="text-sm font-medium text-slate-700">Business documents (PDF / image)</p>
                                {[
                                    ['registrationDoc', 'Business registration'],
                                    ['electricityDoc', 'Electricity bill'],
                                    ['bankDoc', 'Bank proof (cancelled cheque)'],
                                ].map(([key, label]) => (
                                    <div key={key} className="flex items-center gap-3">
                                        <FileText className="h-5 w-5 text-slate-400 shrink-0" />
                                        <div className="flex-1">
                                            <label className="block text-xs text-slate-500">{label}</label>
                                            <input
                                                type="file"
                                                accept="application/pdf,image/*"
                                                onChange={updateFile(key)}
                                                disabled={loading}
                                                className="block w-full text-xs text-slate-600 file:mr-3 file:py-1.5 file:px-3 file:rounded-md file:border-0 file:text-xs file:font-medium file:bg-cyan-50 file:text-cyan-700 hover:file:bg-cyan-100"
                                            />
                                            {files[key] && <span className="text-[11px] text-emerald-600">{files[key].name}</span>}
                                        </div>
                                    </div>
                                ))}
                            </div>

                            <div className="flex gap-4">
                                <button
                                    type="button"
                                    disabled={loading}
                                    onClick={() => setStep(2)}
                                    className="flex-1 py-3 px-4 border border-slate-200 rounded-xl text-sm font-medium text-slate-700 bg-white hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-slate-500 transition-all active:scale-[0.98] disabled:opacity-60"
                                >
                                    Back
                                </button>
                                <button
                                    type="submit"
                                    disabled={loading || !isStep3Valid()}
                                    className="flex-1 flex justify-center items-center gap-2 py-3 px-4 border border-transparent rounded-xl shadow-md shadow-slate-900/20 text-sm font-medium text-white bg-[#1A2234] hover:bg-slate-800 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-slate-900 transition-all active:scale-[0.98] disabled:opacity-60 disabled:cursor-not-allowed"
                                >
                                    {loading ? (<><Loader2 className="w-4 h-4 animate-spin" /> Submitting...</>) : 'Register'}
                                </button>
                            </div>
                        </form>
                    )}

                    {step === 4 && (
                        <form className="space-y-6" onSubmit={handleVerify}>
                            <div>
                                <label className="block text-sm font-medium text-slate-700">6-digit code</label>
                                <div className="mt-2 relative rounded-md shadow-sm">
                                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                        <ShieldCheck className="h-5 w-5 text-slate-400" />
                                    </div>
                                    <input
                                        type="text"
                                        inputMode="numeric"
                                        autoComplete="one-time-code"
                                        maxLength={6}
                                        required
                                        value={otp}
                                        onChange={(e) => setOtp(e.target.value)}
                                        className="block w-full pl-10 tracking-[0.5em] text-center sm:text-sm border-slate-200 rounded-lg py-3 bg-slate-50 focus:bg-white focus:ring-2 focus:ring-cyan-500 focus:border-cyan-500 transition-colors"
                                        placeholder="••••••"
                                        disabled={loading}
                                    />
                                </div>
                            </div>

                            <button
                                type="submit"
                                disabled={loading}
                                className="w-full flex justify-center items-center gap-2 py-3 px-4 border border-transparent rounded-xl shadow-md shadow-slate-900/20 text-sm font-medium text-white bg-[#1A2234] hover:bg-slate-800 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-slate-900 transition-all active:scale-[0.98] disabled:opacity-60 disabled:cursor-not-allowed"
                            >
                                {loading ? (<><Loader2 className="w-4 h-4 animate-spin" /> Verifying...</>) : 'Verify email'}
                            </button>

                            <button
                                type="button"
                                onClick={handleResend}
                                className="w-full text-sm font-medium text-cyan-600 hover:text-cyan-500"
                            >
                                Resend code
                            </button>
                        </form>
                    )}
                </div>
            </div>
        </div>
    );
}
