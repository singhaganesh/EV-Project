import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { 
    getEarningsSummary as fetchEarningsSummaryAPI,
    getEarningsTransactions as fetchEarningsTransactionsAPI 
} from '../api/axios';

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

export const fetchEarningsTransactions = createAsyncThunk(
    'earnings/fetchTransactions',
    async ({ ownerId, page, size, search }, { rejectWithValue }) => {
        try {
            const response = await fetchEarningsTransactionsAPI(ownerId, page, size, search);
            return response.data;
        } catch (error) {
            return rejectWithValue(error.response?.data?.message || 'Failed to fetch transactions');
        }
    }
);

const initialState = {
    summary: {
        lifetimeRevenue: 0,
        energyCost: 0,
        netMargin: 0,
        revenueLast48h: 0
    },
    transactions: [],
    pagination: {
        totalPages: 0,
        totalElements: 0,
        currentPage: 0,
        pageSize: 10
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
            // Summary
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
            })
            // Transactions
            .addCase(fetchEarningsTransactions.pending, (state) => {
                state.loading = true;
            })
            .addCase(fetchEarningsTransactions.fulfilled, (state, action) => {
                state.loading = false;
                state.transactions = action.payload.content;
                state.pagination = {
                    totalPages: action.payload.totalPages,
                    totalElements: action.payload.totalElements,
                    currentPage: action.payload.number,
                    pageSize: action.payload.size
                };
            })
            .addCase(fetchEarningsTransactions.rejected, (state, action) => {
                state.loading = false;
                state.error = action.payload;
            });
    },
});

export const selectEarningsSummary = (state) => state.earnings.summary;
export const selectEarningsTransactions = (state) => state.earnings.transactions;
export const selectEarningsPagination = (state) => state.earnings.pagination;
export const selectEarningsLoading = (state) => state.earnings.loading;

export default earningsSlice.reducer;
