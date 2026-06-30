import axios from 'axios';
import toast from 'react-hot-toast';
import { tokenStorage } from './tokenStorage';

const api = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true, // Send secure cookies with every request
});

api.interceptors.response.use(
    (response) => {
        // Some responses wrap data inside response.data.data
        return response;
    },
    (error) => {
        // Auth endpoints (login, MFA, registration/verification) own their error
        // UX — a 401 there means "bad credentials/OTP", not an expired session, so
        // don't force a logout/redirect or show the generic toast.
        const isAuthEndpoint = (error.config?.url || '').includes('/auth/');

        if (error.response?.status === 401 && !isAuthEndpoint) {
            // Auto logout if 401 response returned from api
            tokenStorage.clear();
            localStorage.removeItem('user');
            window.location.href = '/login';
            toast.error('Session expired. Please log in again.');
        } else if (!isAuthEndpoint) {
            toast.error(error.response?.data?.message || 'An unexpected error occurred.');
        }
        return Promise.reject(error);
    }
);

// Analytics
export const getOwnerStationStats = (ownerId) => api.get(`/stations/owner/${ownerId}/stats`);
export const getRevenueTrends = (ownerId, days = 7) => api.get(`/analytics/revenue-trends/${ownerId}?days=${days}`);
export const getPeakUsage = (ownerId, days = 7) => api.get(`/analytics/peak-usage/${ownerId}?days=${days}`);
export const getAnalyticsSummary = (ownerId, days = 7) => api.get(`/analytics/summary/${ownerId}?days=${days}`);
export const getEarningsSummary = (ownerId) => api.get(`/earnings/summary/${ownerId}`);
export const getEarningsTransactions = (ownerId, page = 0, size = 10, search = '') =>
    api.get(`/earnings/transactions/${ownerId}?page=${page}&size=${size}` +
        (search ? `&search=${encodeURIComponent(search)}` : ''));
export const exportEarningsTransactions = (ownerId) =>
    api.get(`/earnings/transactions/${ownerId}/export`, { responseType: 'blob' });

// Bookings
export const getOwnerBookings = (ownerId, page = 0, size = 10, status = '') =>
    api.get(`/bookings/owner/${ownerId}?page=${page}&size=${size}` +
        (status ? `&status=${encodeURIComponent(status)}` : ''));

export default api;
