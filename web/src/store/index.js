import { configureStore } from '@reduxjs/toolkit';
import authReducer from './authSlice';
import stationReducer from './stationSlice';
import earningsReducer from './earningsSlice';
import bookingsReducer from './bookingsSlice';

export const store = configureStore({
    reducer: {
        auth: authReducer,
        station: stationReducer,
        earnings: earningsReducer,
        bookings: bookingsReducer,
    },
    middleware: (getDefaultMiddleware) => getDefaultMiddleware(),
    devTools: process.env.NODE_ENV !== 'production',
});
