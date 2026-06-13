import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Zap, Mail, Lock, Loader2, ShieldCheck, Eye, EyeOff } from 'lucide-react';
import toast from 'react-hot-toast';
import api from '../../api/axios';
import { setCredentials, selectCurrentUser } from '../../store/authSlice';

export default function LoginPage() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [loading, setLoading] = useState(false);

    // view: 'login' | 'mfa' | 'verify'
    const [view, setView] = useState('login');
    const [otp, setOtp] = useState('');
    const [tempLoginToken, setTempLoginToken] = useState('');
    const [verifyUserId, setVerifyUserId] = useState(null);

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

    const resetToLogin = () => {
        setView('login');
        setOtp('');
        setTempLoginToken('');
        setVerifyUserId(null);
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
                if (data.otp) setOtp(data.otp); // dev convenience when OTP is exposed
                setView('mfa');
                toast.success('MFA code sent to your email.');
                return;
            }

            // Unverified owner: backend re-sent a verification code — go verify.
            if (data?.needsEmailVerification) {
                setVerifyUserId(data.userId);
                if (data.otp) setOtp(data.otp);
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
        try {
            const response = await api.post('/auth/resend-verification', { userId: verifyUserId });
            if (response.data?.data?.otp) setOtp(response.data.data.otp);
            toast.success('A new code has been sent.');
        } catch (err) {
            toast.error(err.response?.data?.message || 'Could not resend the code.');
        }
    };

    const isOtpView = view === 'mfa' || view === 'verify';
    const heading = view === 'mfa' ? 'Two-factor verification'
        : view === 'verify' ? 'Verify your email'
        : 'Sign in to your account';

    return (
        <div className="min-h-screen bg-[#F4F7FE] flex flex-col justify-center py-12 sm:px-6 lg:px-8">
            <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
                <div className="w-16 h-16 rounded-full bg-cyan-500 mx-auto flex items-center justify-center shadow-lg shadow-cyan-500/30">
                    {isOtpView ? <ShieldCheck className="w-8 h-8 text-white" /> : <Zap className="w-8 h-8 text-white" />}
                </div>
                <h2 className="mt-6 text-center text-3xl font-extrabold text-[#1A2234]">{heading}</h2>
                <p className="mt-2 text-center text-sm text-slate-500">
                    {isOtpView ? (
                        <>We emailed a 6-digit code to <span className="font-medium">{email}</span></>
                    ) : (
                        <>
                            Or{' '}
                            <Link to="/register" className="font-medium text-cyan-600 hover:text-cyan-500 transition-colors">
                                register as a new Pump Owner
                            </Link>
                        </>
                    )}
                </p>
            </div>

            <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
                <div className="bg-white py-8 px-4 shadow-xl shadow-slate-200/40 sm:rounded-2xl sm:px-10 border border-slate-100">
                    {view === 'login' && (
                        <form className="space-y-6" onSubmit={handleLogin}>
                            <div>
                                <label className="block text-sm font-medium text-slate-700">Email address</label>
                                <div className="mt-2 relative rounded-md shadow-sm">
                                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                        <Mail className="h-5 w-5 text-slate-400" />
                                    </div>
                                    <input
                                        type="email"
                                        required
                                        value={email}
                                        onChange={(e) => setEmail(e.target.value)}
                                        className="block w-full pl-10 sm:text-sm border-slate-200 rounded-lg py-3 bg-slate-50 focus:bg-white focus:ring-2 focus:ring-cyan-500 focus:border-cyan-500 transition-colors"
                                        placeholder="admin@plugsy.com"
                                        disabled={loading}
                                    />
                                </div>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-slate-700">Password</label>
                                <div className="mt-2 relative rounded-md shadow-sm">
                                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                        <Lock className="h-5 w-5 text-slate-400" />
                                    </div>
                                    <input
                                        type={showPassword ? 'text' : 'password'}
                                        required
                                        value={password}
                                        onChange={(e) => setPassword(e.target.value)}
                                        className="block w-full pl-10 pr-10 sm:text-sm border-slate-200 rounded-lg py-3 bg-slate-50 focus:bg-white focus:ring-2 focus:ring-cyan-500 focus:border-cyan-500 transition-colors"
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
                                className="w-full flex justify-center items-center gap-2 py-3 px-4 border border-transparent rounded-xl shadow-md shadow-cyan-500/20 text-sm font-medium text-white bg-cyan-500 hover:bg-cyan-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-cyan-500 transition-all active:scale-[0.98] disabled:opacity-60 disabled:cursor-not-allowed"
                            >
                                {loading ? (<><Loader2 className="w-4 h-4 animate-spin" /> Signing in...</>) : 'Sign in'}
                            </button>
                        </form>
                    )}

                    {isOtpView && (
                        <form className="space-y-6" onSubmit={view === 'mfa' ? handleVerifyMfa : handleVerifyEmail}>
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
                                className="w-full flex justify-center items-center gap-2 py-3 px-4 border border-transparent rounded-xl shadow-md shadow-cyan-500/20 text-sm font-medium text-white bg-cyan-500 hover:bg-cyan-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-cyan-500 transition-all active:scale-[0.98] disabled:opacity-60 disabled:cursor-not-allowed"
                            >
                                {loading ? (<><Loader2 className="w-4 h-4 animate-spin" /> Verifying...</>) : (view === 'mfa' ? 'Verify & sign in' : 'Verify email')}
                            </button>

                            <div className="flex items-center justify-between text-sm">
                                <button type="button" onClick={resetToLogin} className="text-slate-500 hover:text-slate-700">
                                    Back to sign in
                                </button>
                                {view === 'verify' && (
                                    <button type="button" onClick={handleResend} className="font-medium text-cyan-600 hover:text-cyan-500">
                                        Resend code
                                    </button>
                                )}
                            </div>
                        </form>
                    )}
                </div>
            </div>
        </div>
    );
}
