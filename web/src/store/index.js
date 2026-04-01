import { configureStore } from '@reduxjs/toolkit';
import authReducer from './authSlice';
import stationReducer from './stationSlice';

export const store = configureStore({
    reducer: {
        auth: authReducer,
        station: stationReducer,
    },
    middleware: (getDefaultMiddleware) => getDefaultMiddleware(),
    devTools: process.env.NODE_ENV !== 'production',
});
