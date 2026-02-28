import { createSlice } from '@reduxjs/toolkit';

const initialState = {
    stations: [],
    dashboardStats: {
        totalUsers: 0,
        activeStations: 0,
        totalBookings: 0,
        pendingApproval: 0
    },
    recentActivity: [],
    loading: false,
    error: null,
};

const stationSlice = createSlice({
    name: 'station',
    initialState,
    reducers: {
        setStations: (state, action) => {
            state.stations = action.payload;
        },
        setDashboardStats: (state, action) => {
            state.dashboardStats = action.payload;
        },
        setRecentActivity: (state, action) => {
            state.recentActivity = action.payload;
        }
    },
});

export const { setStations, setDashboardStats, setRecentActivity } = stationSlice.actions;

export const selectAllStations = (state) => state.station.stations;
export const selectDashboardStats = (state) => state.station.dashboardStats;
export const selectRecentActivity = (state) => state.station.recentActivity;

export default stationSlice.reducer;
