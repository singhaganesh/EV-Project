import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Zap, Mail, Lock, User, Loader2 } from 'lucide-react';
import toast from 'react-hot-toast';
import api from '../../api/axios';
import { setCredentials, selectCurrentUser } from '../../store/authSlice';

export default function RegisterPage() {
    const [name, setName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [loading, setLoading] = useState(false);

    const dispatch = useDispatch();
    const navigate = useNavigate();
    const user = useSelector(selectCurrentUser);

    // Redirect if already logged in
    useEffect(() => {
        if (user) {
            if (user.role === 'ADMIN') {
                navigate('/admin', { replace: true });
            } else if (user.role === 'STATION_OWNER') {
                navigate('/owner', { replace: true });
            }
        }
    }, [user, navigate]);

    const handleRegister = async (e) => {
        e.preventDefault();
        setLoading(true);

        try {
            const response = await api.post('/auth/register', {
                name,
                email,
                password,
                role: 'PUMP_OWNER', // Only Pump Owners register via web
            });

            const { success, message, data } = response.data;

            if (!success) {
                toast.error(message || 'Registration failed. Please try again.');
                return;
            }

            const { token, user } = data;

            // Save to Redux store + localStorage
            dispatch(setCredentials({ token, user }));

            toast.success('Registration successful! Welcome to Plugsy Partner.');

            // Pump owners always go to owner dashboard
            navigate('/owner', { replace: true });
        } catch (err) {
            const msg = err.response?.data?.message || 'Registration failed. Please try again.';
            toast.error(msg);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-[#F4F7FE] flex flex-col justify-center py-12 sm:px-6 lg:px-8">
            <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
                <div className="w-16 h-16 rounded-full bg-[#1A2234] mx-auto flex items-center justify-center shadow-lg shadow-slate-900/20">
                    <Zap className="w-8 h-8 text-cyan-400" />
                </div>
                <h2 className="mt-6 text-center text-3xl font-extrabold text-[#1A2234]">
                    Partner with Plugsy
                </h2>
                <p className="mt-2 text-center text-sm text-slate-500">
                    Register your station business or{' '}
                    <Link to="/login" className="font-medium text-cyan-600 hover:text-cyan-500 transition-colors">
                        sign in to your existing account
                    </Link>
                </p>
            </div>

            <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
                <div className="bg-white py-8 px-4 shadow-xl shadow-slate-200/40 sm:rounded-2xl sm:px-10 border border-slate-100">
                    <form className="space-y-6" onSubmit={handleRegister}>
                        <div>
                            <label className="block text-sm font-medium text-slate-700">Full Name / Business Name</label>
                            <div className="mt-2 relative rounded-md shadow-sm">
                                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                    <User className="h-5 w-5 text-slate-400" />
                                </div>
                                <input
                                    type="text"
                                    required
                                    value={name}
                                    onChange={(e) => setName(e.target.value)}
                                    className="block w-full pl-10 sm:text-sm border-slate-200 rounded-lg py-3 bg-slate-50 focus:bg-white focus:ring-2 focus:ring-cyan-500 focus:border-cyan-500 transition-colors"
                                    placeholder="Your Name or Business Name"
                                    disabled={loading}
                                />
                            </div>
                        </div>

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
                                    placeholder="business@example.com"
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
                                    type="password"
                                    required
                                    minLength={6}
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    className="block w-full pl-10 sm:text-sm border-slate-200 rounded-lg py-3 bg-slate-50 focus:bg-white focus:ring-2 focus:ring-cyan-500 focus:border-cyan-500 transition-colors"
                                    placeholder="••••••••"
                                    disabled={loading}
                                />
                            </div>
                        </div>

                        <div>
                            <button
                                type="submit"
                                disabled={loading}
                                className="w-full flex justify-center items-center gap-2 py-3 px-4 border border-transparent rounded-xl shadow-md shadow-slate-900/20 text-sm font-medium text-white bg-[#1A2234] hover:bg-slate-800 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-slate-900 transition-all active:scale-[0.98] disabled:opacity-60 disabled:cursor-not-allowed"
                            >
                                {loading ? (
                                    <>
                                        <Loader2 className="w-4 h-4 animate-spin" />
                                        Registering...
                                    </>
                                ) : (
                                    'Register as Pump Owner'
                                )}
                            </button>
                        </div>

                        <div className="mt-4 text-center">
                            <p className="text-xs text-slate-400">
                                Customers must register via the Plugsy mobile app.
                            </p>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
}
