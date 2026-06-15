import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Zap, Mail, Lock, Loader2, ShieldCheck, Eye, EyeOff, ArrowRight } from 'lucide-react';
import toast from 'react-hot-toast';
import api from '../../api/axios';
import { setCredentials, selectCurrentUser } from '../../store/authSlice';

export default function LoginPage() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [loading, setLoading] = useState(false);

    // view: 'login' | 'mfa' | 'verify' | 'forgot' | 'reset'
    const [view, setView] = useState('login');
    const [otp, setOtp] = useState('');
    const [otpDigits, setOtpDigits] = useState(Array(6).fill(''));
    const [tempLoginToken, setTempLoginToken] = useState('');
    const [verifyUserId, setVerifyUserId] = useState(null);

    // Password-reset form fields.
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [showNewPassword, setShowNewPassword] = useState(false);

    // Resend cooldown (seconds remaining) + in-flight flag to prevent spamming.
    const [resendTimer, setResendTimer] = useState(0);
    const [resendLoading, setResendLoading] = useState(false);

    const dispatch = useDispatch();
    const navigate = useNavigate();
    const user = useSelector(selectCurrentUser);

    // Redirect if already logged in
    useEffect(() => {
        if (user) {
            if (user.role === 'ADMIN') navigate('/admin', { replace: true });
            else if (user.role === 'STATION_OWNER') navigate('/owner', { replace: true });
        }
    }, [user, navigate]);

    // Reset OTP digits when view changes; start the resend cooldown on OTP views.
    useEffect(() => {
        setOtpDigits(Array(6).fill(''));
        setOtp('');
        if (view === 'mfa' || view === 'verify' || view === 'reset') {
            setResendTimer(60);
        }
    }, [view]);

    // Tick the resend cooldown down to zero, one second at a time.
    useEffect(() => {
        if (resendTimer <= 0) return undefined;
        const id = setTimeout(() => setResendTimer((t) => t - 1), 1000);
        return () => clearTimeout(id);
    }, [resendTimer]);

    const resetToLogin = () => {
        setView('login');
        setOtp('');
        setOtpDigits(Array(6).fill(''));
        setTempLoginToken('');
        setVerifyUserId(null);
        setNewPassword('');
        setConfirmPassword('');
        setShowNewPassword(false);
    };

    // Finalize a successful login (direct or post-MFA) and redirect by role.
    const completeLogin = (data) => {
        const { token, user: loggedInUser } = data;
        dispatch(setCredentials({ token, user: loggedInUser }));
        toast.success('Login successful!');

        if (loggedInUser.role === 'ADMIN') {
            navigate('/admin', { replace: true });
        } else if (loggedInUser.role === 'STATION_OWNER') {
            navigate('/owner', { replace: true });
        } else {
            toast.error('Access denied. This portal is for Admins and Pump Owners only.');
            dispatch({ type: 'auth/logout' });
        }
    };

    const handleLogin = async (e) => {
        e.preventDefault();
        setLoading(true);

        try {
            const response = await api.post('/auth/login', { email, password });
            const { success, message, data } = response.data;

            if (!success) {
                toast.error(message || 'Invalid email or password');
                return;
            }

            // Owner with MFA: move to the OTP step instead of logging in.
            if (data?.mfaRequired) {
                setTempLoginToken(data.tempLoginToken);
                setView('mfa');
                toast.success('MFA code sent to your email.');
                return;
            }

            // Unverified owner: backend re-sent a verification code — go verify.
            if (data?.needsEmailVerification) {
                setVerifyUserId(data.userId);
                setView('verify');
                toast('Please verify your email to continue.', { icon: '✉️' });
                return;
            }

            completeLogin(data);
        } catch (err) {
            const msg = err.response?.data?.message || 'Login failed. Please try again.';
            toast.error(msg);
        } finally {
            setLoading(false);
        }
    };

    const handleVerifyMfa = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            const response = await api.post('/auth/verify-mfa', { tempLoginToken, otp });
            const { success, message, data } = response.data;
            if (!success) {
                toast.error(message || 'Invalid MFA code');
                return;
            }
            completeLogin(data);
        } catch (err) {
            toast.error(err.response?.data?.message || 'Verification failed. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    const handleVerifyEmail = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            const response = await api.post('/auth/verify-registration', { userId: verifyUserId, otp });
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
            setPassword('');
            resetToLogin();
        } catch (err) {
            toast.error(err.response?.data?.message || 'Verification failed. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    const handleResend = async () => {
        if (resendTimer > 0 || resendLoading) return;
        setResendLoading(true);
        try {
            await api.post('/auth/resend-verification', { userId: verifyUserId });
            toast.success('A new code has been sent.');
            setResendTimer(60);
        } catch (err) {
            toast.error(err.response?.data?.message || 'Could not resend the code.');
        } finally {
            setResendLoading(false);
        }
    };

    const handleResendMfa = async () => {
        if (resendTimer > 0 || resendLoading) return;
        setResendLoading(true);
        try {
            await api.post('/auth/resend-mfa', { tempLoginToken });
            toast.success('A new MFA code has been sent.');
            setResendTimer(60);
        } catch (err) {
            toast.error(err.response?.data?.message || 'Could not resend the code.');
        } finally {
            setResendLoading(false);
        }
    };

    // Requests a password-reset code. Used both to start the flow (from the
    // 'forgot' view) and to resend a code (from the 'reset' view).
    const handleForgotPassword = async (e) => {
        if (e) e.preventDefault();
        const resending = view === 'reset';
        if (resending && (resendTimer > 0 || resendLoading)) return;
        if (resending) setResendLoading(true); else setLoading(true);
        try {
            await api.post('/auth/forgot-password', { email });
            toast.success('If the email exists, a reset code has been sent.');
            if (resending) {
                setResendTimer(60);
            } else {
                setView('reset');
            }
        } catch (err) {
            toast.error(err.response?.data?.message || 'Could not send the reset code.');
        } finally {
            if (resending) setResendLoading(false); else setLoading(false);
        }
    };

    const handleResetPassword = async (e) => {
        e.preventDefault();
        if (newPassword !== confirmPassword) {
            toast.error('Passwords do not match');
            return;
        }
        setLoading(true);
        try {
            const response = await api.post('/auth/reset-password', { email, otp, newPassword });
            const { success, message } = response.data;
            if (!success) {
                toast.error(message || 'Could not reset password.');
                return;
            }
            toast.success('Password reset successfully. Please sign in.');
            setPassword('');
            resetToLogin();
        } catch (err) {
            toast.error(err.response?.data?.message || 'Could not reset password.');
        } finally {
            setLoading(false);
        }
    };

    const handleOtpDigitChange = (index, value) => {
        // Only allow single digit numbers
        if (value && !/^[0-9]$/.test(value)) return;

        const newDigits = [...otpDigits];
        newDigits[index] = value;
        setOtpDigits(newDigits);

        const combinedOtp = newDigits.join('');
        setOtp(combinedOtp);

        // Auto-focus next field
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

    const isOtpView = view === 'mfa' || view === 'verify';
    const resendDisabled = resendTimer > 0 || resendLoading;
    const resendLabel = resendLoading
        ? 'Sending...'
        : resendTimer > 0
            ? `Resend code (${resendTimer}s)`
            : 'Resend code';
    const heading = view === 'mfa' ? 'Two-factor verification'
        : view === 'verify' ? 'Verify your email'
        : view === 'forgot' ? 'Forgot your password?'
        : view === 'reset' ? 'Reset your password'
        : 'Sign in to your account';

    return (
        <div className="min-h-screen bg-[#F4F7FE] flex w-full font-sans overflow-hidden">
            {/* Left Side: Brand Hero Panel (Hidden on Mobile) */}
            <div className="hidden lg:flex w-1/2 relative bg-slate-900 overflow-hidden flex-col justify-between p-12">
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
            <div className="w-full lg:w-1/2 flex items-center justify-center p-6 sm:p-12 bg-[#F4F7FE]">
                <div className="w-full max-w-md bg-white rounded-[24px] shadow-xl shadow-slate-200/40 p-8 sm:p-10 border border-slate-100 relative z-10">
                    
                    {/* Form Header */}
                    <div className="flex flex-col items-center mb-8">
                        <div className="w-14 h-14 rounded-2xl bg-slate-50 flex items-center justify-center mb-4 border border-slate-100">
                            {view === 'login' ? <Zap className="w-7 h-7 text-cyan-500" />
                                : view === 'forgot' ? <Mail className="w-7 h-7 text-cyan-500" />
                                : <ShieldCheck className="w-7 h-7 text-cyan-500" />}
                        </div>
                        <h2 className="text-2xl font-bold text-[#1A2234] text-center">{heading}</h2>
                        <p className="mt-2 text-center text-sm text-slate-500">
                            {isOtpView || view === 'reset' ? (
                                <>We emailed a 6-digit code to <span className="font-semibold text-slate-700">{email}</span></>
                            ) : view === 'forgot' ? (
                                <>Enter your account email and we'll send you a reset code.</>
                            ) : (
                                <>
                                    Or{' '}
                                    <Link to="/register" className="font-semibold text-cyan-500 hover:text-cyan-600 transition-colors underline-offset-2 hover:underline">
                                        register as a new Pump Owner
                                    </Link>
                                </>
                            )}
                        </p>
                    </div>

                    {/* Views */}
                    {view === 'login' && (
                        <form className="space-y-6" onSubmit={handleLogin}>
                            <div>
                                <label className="block text-xs font-semibold text-slate-600 uppercase tracking-wider">Email address</label>
                                <div className="mt-2 relative rounded-lg shadow-sm border border-slate-200 focus-within:ring-2 focus-within:ring-cyan-500/20 focus-within:border-cyan-500 transition-colors overflow-hidden">
                                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                        <Mail className="h-5 w-5 text-slate-400" />
                                    </div>
                                    <input
                                        type="email"
                                        required
                                        value={email}
                                        onChange={(e) => setEmail(e.target.value)}
                                        className="block w-full pl-10 pr-4 py-3 sm:text-sm border-0 bg-slate-50/50 focus:bg-white focus:ring-0 placeholder:text-slate-400 text-slate-800 focus:outline-none transition-colors"
                                        placeholder="admin@plugsy.com"
                                        disabled={loading}
                                    />
                                </div>
                            </div>

                            <div>
                                <div className="flex justify-between items-center">
                                    <label className="block text-xs font-semibold text-slate-600 uppercase tracking-wider">Password</label>
                                    <button
                                        type="button"
                                        onClick={() => setView('forgot')}
                                        className="text-xs font-semibold text-cyan-500 hover:text-cyan-600 transition-colors"
                                    >
                                        Forgot password?
                                    </button>
                                </div>
                                <div className="mt-2 relative rounded-lg shadow-sm border border-slate-200 focus-within:ring-2 focus-within:ring-cyan-500/20 focus-within:border-cyan-500 transition-colors overflow-hidden">
                                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                        <Lock className="h-5 w-5 text-slate-400" />
                                    </div>
                                    <input
                                        type={showPassword ? 'text' : 'password'}
                                        required
                                        value={password}
                                        onChange={(e) => setPassword(e.target.value)}
                                        className="block w-full pl-10 pr-10 py-3 sm:text-sm border-0 bg-slate-50/50 focus:bg-white focus:ring-0 placeholder:text-slate-400 text-slate-800 focus:outline-none transition-colors"
                                        placeholder="••••••••"
                                        disabled={loading}
                                    />
                                    <button
                                        type="button"
                                        onClick={() => setShowPassword(!showPassword)}
                                        className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-600 transition-colors z-20"
                                    >
                                        {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                                    </button>
                                </div>
                            </div>

                            <button
                                type="submit"
                                disabled={loading}
                                className="w-full flex justify-center items-center gap-2 py-3 px-4 border border-transparent rounded-lg shadow-lg shadow-cyan-500/10 text-sm font-semibold text-white bg-gradient-to-r from-cyan-600 to-cyan-500 hover:opacity-95 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-cyan-500 transition-all active:scale-[0.99] disabled:opacity-60 disabled:cursor-not-allowed"
                            >
                                {loading ? (<><Loader2 className="w-4 h-4 animate-spin" /> Signing in...</>) : (
                                    <>
                                        Sign in
                                        <ArrowRight className="w-4 h-4" />
                                    </>
                                )}
                            </button>
                        </form>
                    )}

                    {isOtpView && (
                        <form className="space-y-6" onSubmit={view === 'mfa' ? handleVerifyMfa : handleVerifyEmail}>
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
                                        {view === 'mfa' ? 'Verify & sign in' : 'Verify email'}
                                        <ArrowRight className="w-4 h-4" />
                                    </>
                                )}
                            </button>

                            <div className="flex items-center justify-between text-sm pt-2">
                                <button type="button" onClick={resetToLogin} className="text-slate-500 hover:text-slate-700 transition-colors font-medium">
                                    Back to sign in
                                </button>
                                <button
                                    type="button"
                                    onClick={view === 'verify' ? handleResend : handleResendMfa}
                                    disabled={resendDisabled}
                                    className="font-semibold text-cyan-500 hover:text-cyan-600 transition-colors inline-flex items-center gap-1.5 disabled:text-slate-400 disabled:cursor-not-allowed"
                                >
                                    {resendLoading && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                                    {resendLabel}
                                </button>
                            </div>
                        </form>
                    )}

                    {view === 'forgot' && (
                        <form className="space-y-6" onSubmit={handleForgotPassword}>
                            <div>
                                <label className="block text-xs font-semibold text-slate-600 uppercase tracking-wider">Email address</label>
                                <div className="mt-2 relative rounded-lg shadow-sm border border-slate-200 focus-within:ring-2 focus-within:ring-cyan-500/20 focus-within:border-cyan-500 transition-colors overflow-hidden">
                                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                        <Mail className="h-5 w-5 text-slate-400" />
                                    </div>
                                    <input
                                        type="email"
                                        required
                                        value={email}
                                        onChange={(e) => setEmail(e.target.value)}
                                        className="block w-full pl-10 pr-4 py-3 sm:text-sm border-0 bg-slate-50/50 focus:bg-white focus:ring-0 placeholder:text-slate-400 text-slate-800 focus:outline-none transition-colors"
                                        placeholder="you@company.com"
                                        disabled={loading}
                                    />
                                </div>
                            </div>

                            <button
                                type="submit"
                                disabled={loading}
                                className="w-full flex justify-center items-center gap-2 py-3 px-4 border border-transparent rounded-lg shadow-lg shadow-cyan-500/10 text-sm font-semibold text-white bg-gradient-to-r from-cyan-600 to-cyan-500 hover:opacity-95 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-cyan-500 transition-all active:scale-[0.99] disabled:opacity-60 disabled:cursor-not-allowed"
                            >
                                {loading ? (<><Loader2 className="w-4 h-4 animate-spin" /> Sending...</>) : (
                                    <>
                                        Send reset code
                                        <ArrowRight className="w-4 h-4" />
                                    </>
                                )}
                            </button>

                            <div className="text-sm pt-2 text-center">
                                <button type="button" onClick={resetToLogin} className="text-slate-500 hover:text-slate-700 transition-colors font-medium">
                                    Back to sign in
                                </button>
                            </div>
                        </form>
                    )}

                    {view === 'reset' && (
                        <form className="space-y-6" onSubmit={handleResetPassword}>
                            <div>
                                <label className="block text-xs font-semibold text-slate-600 uppercase tracking-wider text-center">Enter Reset Code</label>

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

                            <div>
                                <label className="block text-xs font-semibold text-slate-600 uppercase tracking-wider">New Password</label>
                                <div className="mt-2 relative rounded-lg shadow-sm border border-slate-200 focus-within:ring-2 focus-within:ring-cyan-500/20 focus-within:border-cyan-500 transition-colors overflow-hidden">
                                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                        <Lock className="h-5 w-5 text-slate-400" />
                                    </div>
                                    <input
                                        type={showNewPassword ? 'text' : 'password'}
                                        required
                                        minLength={6}
                                        value={newPassword}
                                        onChange={(e) => setNewPassword(e.target.value)}
                                        className="block w-full pl-10 pr-10 py-3 sm:text-sm border-0 bg-slate-50/50 focus:bg-white focus:ring-0 placeholder:text-slate-400 text-slate-800 focus:outline-none transition-colors"
                                        placeholder="••••••••"
                                        disabled={loading}
                                    />
                                    <button
                                        type="button"
                                        onClick={() => setShowNewPassword(!showNewPassword)}
                                        className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-600 transition-colors z-20"
                                    >
                                        {showNewPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                                    </button>
                                </div>
                            </div>

                            <div>
                                <label className="block text-xs font-semibold text-slate-600 uppercase tracking-wider">Confirm Password</label>
                                <div className="mt-2 relative rounded-lg shadow-sm border border-slate-200 focus-within:ring-2 focus-within:ring-cyan-500/20 focus-within:border-cyan-500 transition-colors overflow-hidden">
                                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                        <Lock className="h-5 w-5 text-slate-400" />
                                    </div>
                                    <input
                                        type={showNewPassword ? 'text' : 'password'}
                                        required
                                        minLength={6}
                                        value={confirmPassword}
                                        onChange={(e) => setConfirmPassword(e.target.value)}
                                        className="block w-full pl-10 pr-4 py-3 sm:text-sm border-0 bg-slate-50/50 focus:bg-white focus:ring-0 placeholder:text-slate-400 text-slate-800 focus:outline-none transition-colors"
                                        placeholder="••••••••"
                                        disabled={loading}
                                    />
                                </div>
                            </div>

                            <button
                                type="submit"
                                disabled={loading || otp.length < 6 || !newPassword || !confirmPassword}
                                className="w-full flex justify-center items-center gap-2 py-3 px-4 border border-transparent rounded-lg shadow-lg shadow-cyan-500/10 text-sm font-semibold text-white bg-gradient-to-r from-cyan-600 to-cyan-500 hover:opacity-95 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-cyan-500 transition-all active:scale-[0.99] disabled:opacity-60 disabled:cursor-not-allowed"
                            >
                                {loading ? (<><Loader2 className="w-4 h-4 animate-spin" /> Resetting...</>) : (
                                    <>
                                        Reset password
                                        <ArrowRight className="w-4 h-4" />
                                    </>
                                )}
                            </button>

                            <div className="flex items-center justify-between text-sm pt-2">
                                <button type="button" onClick={resetToLogin} className="text-slate-500 hover:text-slate-700 transition-colors font-medium">
                                    Back to sign in
                                </button>
                                <button
                                    type="button"
                                    onClick={handleForgotPassword}
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
