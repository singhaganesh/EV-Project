import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { Zap, Mail, Lock, User, Loader2, Building2, Hash, Phone, Banknote, FileText, ShieldCheck, Wand2, Eye, EyeOff, ArrowRight, ArrowLeft } from 'lucide-react';
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
    'block w-full pl-10 pr-4 py-3 sm:text-sm border-0 bg-slate-50/50 focus:bg-white focus:ring-0 placeholder:text-slate-400 text-slate-800 focus:outline-none transition-colors';

const TextField = ({ icon: Icon, disabled, ...props }) => {
    const [showPassword, setShowPassword] = useState(false);
    const isPassword = props.type === 'password';
    const inputType = isPassword ? (showPassword ? 'text' : 'password') : props.type;

    return (
        <div className="mt-2 relative rounded-lg shadow-sm border border-slate-200 focus-within:ring-2 focus-within:ring-cyan-500/20 focus-within:border-cyan-500 transition-colors overflow-hidden">
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
    const [otpDigits, setOtpDigits] = useState(Array(6).fill(''));

    // Resend cooldown (seconds remaining) + in-flight flag to prevent spamming.
    const [resendTimer, setResendTimer] = useState(0);
    const [resendLoading, setResendLoading] = useState(false);

    const navigate = useNavigate();
    const user = useSelector(selectCurrentUser);

    useEffect(() => {
        if (user) {
            if (user.role === 'ADMIN') navigate('/admin', { replace: true });
            else if (user.role === 'STATION_OWNER') navigate('/owner', { replace: true });
        }
    }, [user, navigate]);

    useEffect(() => {
        if (step === 4) {
            setOtpDigits(Array(6).fill(''));
            setOtp('');
            setResendTimer(60);
        }
    }, [step]);

    // Tick the resend cooldown down to zero, one second at a time.
    useEffect(() => {
        if (resendTimer <= 0) return undefined;
        const id = setTimeout(() => setResendTimer((t) => t - 1), 1000);
        return () => clearTimeout(id);
    }, [resendTimer]);

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
        if (resendTimer > 0 || resendLoading) return;
        setResendLoading(true);
        try {
            await api.post('/auth/resend-verification', { userId });
            toast.success('A new code has been sent.');
            setResendTimer(60);
        } catch (err) {
            toast.error(err.response?.data?.message || 'Could not resend the code.');
        } finally {
            setResendLoading(false);
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

    const handleOtpDigitChange = (index, value) => {
        if (value && !/^[0-9]$/.test(value)) return;

        const newDigits = [...otpDigits];
        newDigits[index] = value;
        setOtpDigits(newDigits);

        const combinedOtp = newDigits.join('');
        setOtp(combinedOtp);

        if (value && index < 5) {
            document.getElementById(`otp-${index + 1}`)?.focus();
        }
    };

    const handleOtpKeyDown = (index, e) => {
        if (e.key === 'Backspace') {
            if (!otpDigits[index] && index > 0) {
                const newDigits = [...otpDigits];
                newDigits[index - 1] = '';
                setOtpDigits(newDigits);
                setOtp(newDigits.join(''));
                document.getElementById(`otp-${index - 1}`)?.focus();
            } else {
                const newDigits = [...otpDigits];
                newDigits[index] = '';
                setOtpDigits(newDigits);
                setOtp(newDigits.join(''));
            }
        }
    };

    const stepsInfo = [
        { id: 1, label: 'Contact' },
        { id: 2, label: 'Business' },
        { id: 3, label: 'Documents' },
    ];

    const resendDisabled = resendTimer > 0 || resendLoading;
    const resendLabel = resendLoading
        ? 'Sending...'
        : resendTimer > 0
            ? `Resend code (${resendTimer}s)`
            : 'Resend code';

    const heading = step === 4 ? 'Verify your email' : 'Register your Business';
    const subHeading = step === 4 ? (
        <>Enter the 6-digit code we emailed to <span className="font-semibold text-slate-700">{form.email}</span></>
    ) : (
        <>
            Set up your infrastructure provider account in three simple steps.
        </>
    );

    return (
        <div className="min-h-screen bg-[#F4F7FE] flex w-full font-sans overflow-hidden">
            {/* Left Side: Brand Hero Panel (Hidden on Mobile) */}
            <div className="hidden lg:flex w-[45%] relative bg-slate-900 overflow-hidden flex-col justify-between p-12">
                {/* Background Image */}
                <div className="absolute inset-0 z-0">
                    <img 
                        alt="Sleek electric vehicle charging cable render" 
                        className="w-full h-full object-cover opacity-70" 
                        src="https://lh3.googleusercontent.com/aida/AP1WRLuzFpAHULWS4KA0uWbokCFbjXO7nkm6Mkfl1LJdp8qwofBSenSxQDMdPQnL3zq8AQgy0BqYtmT7kWv6-d5avx9qO9Mkcg3I7Q9I6wq5IEehWXu0Vdy7U2rU-MuHfy2H-NGPTUDSwyEUZ4Qq7fb8_pmE9iLffbuQ-uSXq8RxtyeDKoHZKzJS_NgaTNYT5NSkjJt38j7_Yj5ZJdTUIkCOg_0pgM1LsMutlqpY24C7ydvX2mpthI0ds6UQJQUF"
                    />
                    <div className="absolute inset-0 bg-gradient-to-t from-slate-900 via-transparent to-transparent opacity-90" />
                </div>

                {/* Brand Header */}
                <div className="relative z-10 flex items-center gap-3">
                    <div className="w-10 h-10 rounded-xl bg-gradient-to-r from-cyan-400 to-cyan-500 flex items-center justify-center shadow-lg shadow-cyan-500/25">
                        <Zap className="h-6 w-6 text-white" />
                    </div>
                    <span className="text-2xl font-bold text-white tracking-tight">Plugsy</span>
                </div>

                {/* Telemetry/Branding Text */}
                <div className="relative z-10 max-w-lg mb-12">
                    <h1 className="text-4xl font-extrabold text-white tracking-tight leading-tight mb-4">
                        Powering the Future of Mobility
                    </h1>
                    <p className="text-lg text-slate-300">
                        Intelligent infrastructure management for next-generation electric vehicle fleets.
                    </p>
                </div>
            </div>

            {/* Right Side: Form Panel */}
            <div className="w-full lg:w-[55%] flex items-center justify-center p-6 sm:p-12 bg-[#F4F7FE] overflow-y-auto">
                <div className="w-full max-w-[550px] bg-white rounded-[24px] shadow-xl shadow-slate-200/40 p-8 sm:p-10 border border-slate-100 relative z-10 my-4">
                    
                    {/* Dev Autofill Shortcut */}
                    {import.meta.env.DEV && step <= 3 && (
                        <button
                            type="button"
                            onClick={handleAutofill}
                            className="mb-6 w-full flex justify-center items-center gap-2 py-2 px-4 rounded-lg text-xs font-semibold text-amber-700 bg-amber-50 border border-amber-200 hover:bg-amber-100 transition-colors"
                        >
                            <Wand2 className="w-4 h-4" /> Dev Mode: Autofill test details
                        </button>
                    )}

                    {/* Form Header */}
                    <div className="mb-6">
                        <h2 className="text-2xl font-bold text-[#1A2234]">{heading}</h2>
                        <p className="mt-1.5 text-sm text-slate-500">{subHeading}</p>
                    </div>

                    {/* Progress Step Indicator */}
                    {step <= 3 && (
                        <div className="mb-8 relative px-2">
                            <div className="flex items-center justify-between relative">
                                {/* Background Line */}
                                <div className="absolute top-4 left-0 right-0 h-1 bg-slate-100 rounded-full z-0" />
                                {/* Active Progress Line */}
                                <div 
                                    className="absolute top-4 left-0 h-1 bg-cyan-400 rounded-full transition-all duration-500 ease-in-out z-0"
                                    style={{ width: `${((step - 1) / (stepsInfo.length - 1)) * 100}%` }}
                                />
                                
                                {stepsInfo.map((s) => {
                                    const isCompleted = step > s.id;
                                    const isActive = step === s.id;
                                    return (
                                        <div key={s.id} className="flex flex-col items-center z-10 relative">
                                            <div className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold transition-all duration-300 ${
                                                isCompleted ? 'bg-cyan-500 text-white' : 
                                                isActive ? 'bg-[#1A2234] text-white ring-4 ring-cyan-100 border-2 border-cyan-400 animate-pulse' : 
                                                'bg-white border-2 border-slate-200 text-slate-400'
                                            }`}>
                                                {isCompleted ? '✓' : s.id}
                                            </div>
                                            <span className={`mt-2 text-[11px] font-semibold transition-colors duration-300 ${
                                                isActive ? 'text-[#1A2234]' : isCompleted ? 'text-cyan-500' : 'text-slate-400'
                                            }`}>
                                                {s.label}
                                            </span>
                                        </div>
                                    );
                                })}
                            </div>
                        </div>
                    )}

                    {/* Forms */}
                    {step === 1 && (
                        <div className="space-y-5">
                            <div>
                                <label className="block text-xs font-semibold text-slate-600 uppercase tracking-wider">Full Legal Name</label>
                                <TextField icon={User} type="text" required value={form.name} onChange={updateField('name')} placeholder="Jane Doe" disabled={loading} />
                            </div>
                            <div>
                                <label className="block text-xs font-semibold text-slate-600 uppercase tracking-wider">Work Email Address</label>
                                <TextField icon={Mail} type="email" required value={form.email} onChange={updateField('email')} placeholder="jane@infrastructure.co" disabled={loading} />
                            </div>
                            <div>
                                <label className="block text-xs font-semibold text-slate-600 uppercase tracking-wider">Account Password</label>
                                <TextField icon={Lock} type="password" required minLength={6} value={form.password} onChange={updateField('password')} placeholder="••••••••" disabled={loading} />
                            </div>
                            <div>
                                <label className="block text-xs font-semibold text-slate-600 uppercase tracking-wider">Primary Phone Number</label>
                                <TextField icon={Phone} type="tel" required value={form.phoneNumber} onChange={updateField('phoneNumber')} placeholder="+1 (555) 000-0000" disabled={loading} />
                            </div>

                            <div className="pt-2 border-t border-slate-100 flex justify-between items-center text-sm">
                                <Link to="/login" className="text-slate-500 hover:text-slate-700 transition-colors font-medium">
                                    Already registered? Sign in
                                </Link>
                                <button
                                    type="button"
                                    disabled={!isStep1Valid()}
                                    onClick={() => setStep(2)}
                                    className="py-3 px-5 border border-transparent rounded-lg shadow-lg shadow-cyan-500/10 text-sm font-semibold text-white bg-gradient-to-r from-cyan-600 to-cyan-500 hover:opacity-95 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-cyan-500 transition-all active:scale-[0.99] disabled:opacity-60 disabled:cursor-not-allowed flex items-center gap-2"
                                >
                                    Continue
                                    <ArrowRight className="w-4 h-4" />
                                </button>
                            </div>
                        </div>
                    )}

                    {step === 2 && (
                        <div className="space-y-5">
                            <div>
                                <label className="block text-xs font-semibold text-slate-600 uppercase tracking-wider">Registered Company Name</label>
                                <TextField icon={Building2} type="text" required value={form.companyName} onChange={updateField('companyName')} placeholder="Nexus Energy Solutions LLC" disabled={loading} />
                            </div>
                            <div>
                                <label className="block text-xs font-semibold text-slate-600 uppercase tracking-wider">Tax ID / EIN</label>
                                <TextField icon={Hash} type="text" required value={form.taxId} onChange={updateField('taxId')} placeholder="XX-XXXXXXX" disabled={loading} />
                            </div>
                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-xs font-semibold text-slate-600 uppercase tracking-wider">Bank account no.</label>
                                    <TextField icon={Banknote} type="text" required value={form.bankAccountNumber} onChange={updateField('bankAccountNumber')} placeholder="0000 0000 0000" disabled={loading} />
                                </div>
                                <div>
                                    <label className="block text-xs font-semibold text-slate-600 uppercase tracking-wider">IFSC / Routing Code</label>
                                    <TextField icon={Hash} type="text" required value={form.bankIfscCode} onChange={updateField('bankIfscCode')} placeholder="ROUT0000123" disabled={loading} />
                                </div>
                            </div>

                            <div className="pt-2 border-t border-slate-100 flex gap-4">
                                <button
                                    type="button"
                                    onClick={() => setStep(1)}
                                    className="flex-1 py-3 px-4 border border-slate-200 rounded-lg text-sm font-semibold text-slate-600 bg-white hover:bg-slate-50 transition-colors flex items-center justify-center gap-2"
                                >
                                    <ArrowLeft className="w-4 h-4" />
                                    Back
                                </button>
                                <button
                                    type="button"
                                    disabled={!isStep2Valid()}
                                    onClick={() => setStep(3)}
                                    className="flex-1 py-3 px-4 border border-transparent rounded-lg shadow-lg shadow-cyan-500/10 text-sm font-semibold text-white bg-gradient-to-r from-cyan-600 to-cyan-500 hover:opacity-95 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-cyan-500 transition-all active:scale-[0.99] disabled:opacity-60 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                                >
                                    Continue
                                    <ArrowRight className="w-4 h-4" />
                                </button>
                            </div>
                        </div>
                    )}

                    {step === 3 && (
                        <form className="space-y-5" onSubmit={handleRegister}>
                            <div className="space-y-4">
                                <label className="block text-xs font-semibold text-slate-600 uppercase tracking-wider mb-2">Upload Verification Documents</label>
                                
                                {/* Large Dropzone: Business Registration */}
                                <div className="group relative border-2 border-dashed border-slate-200 hover:border-cyan-400 bg-slate-50/50 hover:bg-white rounded-xl p-5 text-center cursor-pointer transition-all duration-200">
                                    <input 
                                        accept="application/pdf,image/*" 
                                        className="absolute inset-0 w-full h-full opacity-0 cursor-pointer z-10" 
                                        type="file" 
                                        disabled={loading}
                                        onChange={updateFile('registrationDoc')} 
                                    />
                                    <div className="flex flex-col items-center justify-center space-y-2">
                                        <div className="w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center group-hover:bg-cyan-50 group-hover:text-cyan-500 transition-colors">
                                            <FileText className="h-6 w-6 text-slate-400 group-hover:text-cyan-500" />
                                        </div>
                                        <div>
                                            <p className="text-xs font-semibold text-slate-700">Business Registration Certificate</p>
                                            <p className="text-[10px] text-slate-400 truncate max-w-[300px]">
                                                {files.registrationDoc ? files.registrationDoc.name : 'Drag & drop or click to browse'}
                                            </p>
                                        </div>
                                    </div>
                                </div>

                                {/* Two smaller Dropzones side-by-side */}
                                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                    {/* Electricity Bill */}
                                    <div className="group relative border-2 border-dashed border-slate-200 hover:border-cyan-400 bg-slate-50/50 hover:bg-white rounded-xl p-4 text-center cursor-pointer transition-all duration-200">
                                        <input 
                                            accept="application/pdf,image/*" 
                                            className="absolute inset-0 w-full h-full opacity-0 cursor-pointer z-10" 
                                            type="file" 
                                            disabled={loading}
                                            onChange={updateFile('electricityDoc')} 
                                        />
                                        <div className="flex flex-col items-center justify-center space-y-1">
                                            <FileText className="h-5 w-5 text-slate-400 group-hover:text-cyan-500 transition-colors" />
                                            <p className="text-xs font-semibold text-slate-700">Electricity Bill</p>
                                            <p className="text-[10px] text-slate-400 truncate max-w-[150px]">
                                                {files.electricityDoc ? files.electricityDoc.name : 'Browse...'}
                                            </p>
                                        </div>
                                    </div>

                                    {/* Cancelled Cheque */}
                                    <div className="group relative border-2 border-dashed border-slate-200 hover:border-cyan-400 bg-slate-50/50 hover:bg-white rounded-xl p-4 text-center cursor-pointer transition-all duration-200">
                                        <input 
                                            accept="application/pdf,image/*" 
                                            className="absolute inset-0 w-full h-full opacity-0 cursor-pointer z-10" 
                                            type="file" 
                                            disabled={loading}
                                            onChange={updateFile('bankDoc')} 
                                        />
                                        <div className="flex flex-col items-center justify-center space-y-1">
                                            <FileText className="h-5 w-5 text-slate-400 group-hover:text-cyan-500 transition-colors" />
                                            <p className="text-xs font-semibold text-slate-700">Cancelled Cheque</p>
                                            <p className="text-[10px] text-slate-400 truncate max-w-[150px]">
                                                {files.bankDoc ? files.bankDoc.name : 'Browse...'}
                                            </p>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div className="pt-2 border-t border-slate-100 flex gap-4">
                                <button
                                    type="button"
                                    disabled={loading}
                                    onClick={() => setStep(2)}
                                    className="flex-1 py-3 px-4 border border-slate-200 rounded-lg text-sm font-semibold text-slate-600 bg-white hover:bg-slate-50 transition-colors flex items-center justify-center gap-2"
                                >
                                    <ArrowLeft className="w-4 h-4" />
                                    Back
                                </button>
                                <button
                                    type="submit"
                                    disabled={loading || !isStep3Valid()}
                                    className="flex-1 py-3 px-4 border border-transparent rounded-lg shadow-lg shadow-slate-900/10 text-sm font-semibold text-white bg-slate-900 hover:bg-slate-800 transition-all active:scale-[0.99] disabled:opacity-60 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                                >
                                    {loading ? (<><Loader2 className="w-4 h-4 animate-spin" /> Registering...</>) : (
                                        <>
                                            Register Business
                                            <ShieldCheck className="w-4 h-4" />
                                        </>
                                    )}
                                </button>
                            </div>
                        </form>
                    )}

                    {step === 4 && (
                        <form className="space-y-6" onSubmit={handleVerify}>
                            <div>
                                <label className="block text-xs font-semibold text-slate-600 uppercase tracking-wider text-center">Enter Verification Code</label>
                                
                                {/* 6-Digit OTP Box Grid */}
                                <div className="flex justify-between gap-2 sm:gap-3 my-6">
                                    {otpDigits.map((digit, idx) => (
                                        <input
                                            key={idx}
                                            id={`otp-${idx}`}
                                            type="text"
                                            inputMode="numeric"
                                            maxLength={1}
                                            value={digit}
                                            onChange={(e) => handleOtpDigitChange(idx, e.target.value)}
                                            onKeyDown={(e) => handleOtpKeyDown(idx, e)}
                                            className="w-11 h-14 sm:w-12 sm:h-16 text-center text-2xl font-bold border border-slate-200 rounded-lg bg-slate-50 focus:bg-white focus:outline-none focus:border-cyan-500 focus:ring-4 focus:ring-cyan-500/10 transition-all text-[#1A2234]"
                                            disabled={loading}
                                        />
                                    ))}
                                </div>
                            </div>

                            <button
                                type="submit"
                                disabled={loading || otp.length < 6}
                                className="w-full flex justify-center items-center gap-2 py-3 px-4 border border-transparent rounded-lg shadow-lg shadow-cyan-500/10 text-sm font-semibold text-white bg-gradient-to-r from-cyan-600 to-cyan-500 hover:opacity-95 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-cyan-500 transition-all active:scale-[0.99] disabled:opacity-60 disabled:cursor-not-allowed"
                            >
                                {loading ? (<><Loader2 className="w-4 h-4 animate-spin" /> Verifying...</>) : (
                                    <>
                                        Verify email
                                        <ArrowRight className="w-4 h-4" />
                                    </>
                                )}
                            </button>

                            <div className="flex items-center justify-between text-sm pt-2">
                                <button 
                                    type="button" 
                                    onClick={() => setStep(1)} 
                                    className="text-slate-500 hover:text-slate-700 transition-colors font-medium"
                                >
                                    Back to details
                                </button>
                                <button
                                    type="button"
                                    onClick={handleResend}
                                    disabled={resendDisabled}
                                    className="font-semibold text-cyan-500 hover:text-cyan-600 transition-colors inline-flex items-center gap-1.5 disabled:text-slate-400 disabled:cursor-not-allowed"
                                >
                                    {resendLoading && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                                    {resendLabel}
                                </button>
                            </div>
                        </form>
                    )}
                </div>
            </div>
        </div>
    );
}
