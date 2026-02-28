import axios from 'axios';
import toast from 'react-hot-toast';

const api = axios.create({
    baseURL: 'http://localhost:8080/api', // Adjust base URL as needed based on production/env
    headers: {
        'Content-Type': 'application/json',
    },
});

api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
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
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            window.location.href = '/login';
            toast.error('Session expired. Please log in again.');
        } else {
            toast.error(error.response?.data?.message || 'An unexpected error occurred.');
        }
        return Promise.reject(error);
    }
);

export default api;
