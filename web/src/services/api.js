import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add token to requests if available
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Handle auth errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Auth APIs
export const login = (email, password) => 
  api.post('/auth/login', { email, password });

export const register = (userData) => 
  api.post('/auth/register', userData);

export const getCurrentUser = () => 
  api.get('/auth/me');

// Station APIs
export const getAllStations = () => api.get('/stations');
export const getStationById = (id) => api.get(`/stations/${id}`);
export const createStation = (station) => api.post('/stations', station);
export const updateStation = (id, station) => api.put(`/stations/${id}`, station);
export const deleteStation = (id) => api.delete(`/stations/${id}`);

// Slot APIs
export const getSlotsByStation = (stationId) => api.get(`/slots/station/${stationId}`);
export const createSlot = (slot) => api.post('/slots', slot);
export const updateSlot = (id, slot) => api.put(`/slots/${id}`, slot);
export const deleteSlot = (id) => api.delete(`/slots/${id}`);

// Booking APIs
export const getAllBookings = () => api.get('/bookings');
export const getUserBookings = (userId) => api.get(`/bookings/user/${userId}`);
export const getStationBookings = (stationId) => api.get(`/bookings/station/${stationId}`);
export const createBooking = (booking) => api.post('/bookings', booking);
export const cancelBooking = (bookingId) => api.put(`/bookings/${bookingId}/cancel`);

// Charging Session APIs
export const getChargingHistory = (userId) => api.get(`/charging/user/${userId}`);
export const startCharging = (bookingId) => api.post('/charging/start', { bookingId });
export const stopCharging = (sessionId) => api.post(`/charging/stop/${sessionId}`);

// Admin APIs
export const getAllUsers = () => api.get('/admin/users');
export const updateUserRole = (userId, role) => api.put(`/admin/users/${userId}/role`, { role });
export const getAdminStats = () => api.get('/admin/stats');

// Owner APIs
export const getOwnerStations = (ownerId) => api.get(`/owner/stations/${ownerId}`);
export const getOwnerStats = (ownerId) => api.get(`/owner/stats/${ownerId}`);

export default api;
