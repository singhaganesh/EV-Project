import { configureStore } from '@reduxjs/toolkit';
import authReducer from './authSlice';
import stationReducer from './stationSlice';
import earningsReducer from './earningsSlice';

export const store = configureStore({
    reducer: {
        auth: authReducer,
        station: stationReducer,
        earnings: earningsReducer,
    },
    middleware: (getDefaultMiddleware) => getDefaultMiddleware(),
    devTools: process.env.NODE_ENV !== 'production',
});
