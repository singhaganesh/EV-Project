import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { getEarningsSummary as fetchEarningsSummaryAPI } from '../api/axios';

export const fetchEarningsSummary = createAsyncThunk(
    'earnings/fetchSummary',
    async (ownerId, { rejectWithValue }) => {
        try {
            const response = await fetchEarningsSummaryAPI(ownerId);
            return response.data;
        } catch (error) {
            return rejectWithValue(error.response?.data?.message || 'Failed to fetch earnings summary');
        }
    }
);

const initialState = {
    summary: {
        currentBalance: 0,
        lifetimeRevenue: 0,
        pendingPayouts: 0,
        lastSettlement: 0
    },
    loading: false,
    error: null,
};

const earningsSlice = createSlice({
    name: 'earnings',
    initialState,
    reducers: {},
    extraReducers: (builder) => {
        builder
            .addCase(fetchEarningsSummary.pending, (state) => {
                state.loading = true;
            })
            .addCase(fetchEarningsSummary.fulfilled, (state, action) => {
                state.loading = false;
                state.summary = action.payload;
            })
            .addCase(fetchEarningsSummary.rejected, (state, action) => {
                state.loading = false;
                state.error = action.payload;
            });
    },
});

export const selectEarningsSummary = (state) => state.earnings.summary;
export const selectEarningsLoading = (state) => state.earnings.loading;

export default earningsSlice.reducer;
