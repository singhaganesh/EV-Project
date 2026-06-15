import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { getOwnerBookings } from '../api/axios';

export const fetchOwnerBookings = createAsyncThunk(
    'bookings/fetchOwner',
    async ({ ownerId, page, size, status }, { rejectWithValue }) => {
        try {
            const response = await getOwnerBookings(ownerId, page, size, status);
            return response.data;
        } catch (error) {
            return rejectWithValue(error.response?.data?.message || 'Failed to fetch bookings');
        }
    }
);

const initialState = {
    bookings: [],
    pagination: {
        totalPages: 0,
        totalElements: 0,
        currentPage: 0,
        pageSize: 10
    },
    loading: false,
    error: null,
};

const bookingsSlice = createSlice({
    name: 'bookings',
    initialState,
    reducers: {},
    extraReducers: (builder) => {
        builder
            .addCase(fetchOwnerBookings.pending, (state) => {
                state.loading = true;
            })
            .addCase(fetchOwnerBookings.fulfilled, (state, action) => {
                state.loading = false;
                state.bookings = action.payload.content || [];
                state.pagination = {
                    totalPages: action.payload.totalPages || 0,
                    totalElements: action.payload.totalElements || 0,
                    currentPage: action.payload.number || 0,
                    pageSize: action.payload.size || 10
                };
            })
            .addCase(fetchOwnerBookings.rejected, (state, action) => {
                state.loading = false;
                state.error = action.payload;
            });
    },
});

export const selectOwnerBookings = (state) => state.bookings.bookings;
export const selectBookingsPagination = (state) => state.bookings.pagination;
export const selectBookingsLoading = (state) => state.bookings.loading;

export default bookingsSlice.reducer;
