import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { 
    getRevenueTrends as fetchRevenueTrendsAPI,
    getPeakUsage as fetchPeakUsageAPI
} from '../api/axios';

export const fetchRevenueTrends = createAsyncThunk(
    'station/fetchRevenueTrends',
    async ({ ownerId, days }, { rejectWithValue }) => {
        try {
            const response = await fetchRevenueTrendsAPI(ownerId, days);
            return response.data;
        } catch (error) {
            return rejectWithValue(error.response?.data?.message || 'Failed to fetch trends');
        }
    }
);

export const fetchPeakUsage = createAsyncThunk(
    'station/fetchPeakUsage',
    async ({ ownerId, days }, { rejectWithValue }) => {
        try {
            const response = await fetchPeakUsageAPI(ownerId, days);
            return response.data;
        } catch (error) {
            return rejectWithValue(error.response?.data?.message || 'Failed to fetch peak usage');
        }
    }
);

const initialState = {
    stations: [],
    revenueTrends: [],
    peakUsage: [],
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
    extraReducers: (builder) => {
        builder
            // Revenue Trends
            .addCase(fetchRevenueTrends.pending, (state) => {
                state.loading = true;
            })
            .addCase(fetchRevenueTrends.fulfilled, (state, action) => {
                state.loading = false;
                state.revenueTrends = action.payload;
            })
            .addCase(fetchRevenueTrends.rejected, (state, action) => {
                state.loading = false;
                state.error = action.payload;
            })
            // Peak Usage
            .addCase(fetchPeakUsage.pending, (state) => {
                state.loading = true;
            })
            .addCase(fetchPeakUsage.fulfilled, (state, action) => {
                state.loading = false;
                state.peakUsage = action.payload;
            })
            .addCase(fetchPeakUsage.rejected, (state, action) => {
                state.loading = false;
                state.error = action.payload;
            });
    }
});

export const { setStations, setDashboardStats, setRecentActivity } = stationSlice.actions;

export const selectAllStations = (state) => state.station.stations;
export const selectRevenueTrends = (state) => state.station.revenueTrends;
export const selectPeakUsage = (state) => state.station.peakUsage;
export const selectDashboardStats = (state) => state.station.dashboardStats;
export const selectRecentActivity = (state) => state.station.recentActivity;

export default stationSlice.reducer;
