import axios from 'axios';
import toast from 'react-hot-toast';
import { tokenStorage } from './tokenStorage';

const api = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

api.interceptors.request.use(
    (config) => {
        const token = tokenStorage.getToken();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

api.interceptors.response.use(
    (response) => {
        // Some responses wrap data inside response.data.data
        return response;
    },
    (error) => {
        if (error.response?.status === 401) {
            // Auto logout if 401 response returned from api
            tokenStorage.clear();
            localStorage.removeItem('user');
            window.location.href = '/login';
            toast.error('Session expired. Please log in again.');
        } else {
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
export const getEarningsTransactions = (ownerId, page = 0, size = 10) => 
    api.get(`/earnings/transactions/${ownerId}?page=${page}&size=${size}`);

export default api;
