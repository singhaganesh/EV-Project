import { createSlice } from '@reduxjs/toolkit';
import { tokenStorage } from '../api/tokenStorage';

const initialState = {
    token: tokenStorage.getToken() || null,
    user: JSON.parse(localStorage.getItem('user')) || null,
    isAuthenticated: !!tokenStorage.getToken(),
    loading: false,
    error: null,
};

const authSlice = createSlice({
    name: 'auth',
    initialState,
    reducers: {
        setCredentials: (state, action) => {
            const { user, token } = action.payload;
            state.user = user;
            state.token = token;
            state.isAuthenticated = true;
            tokenStorage.setToken(token);
            localStorage.setItem('user', JSON.stringify(user));
        },
        logout: (state) => {
            state.user = null;
            state.token = null;
            state.isAuthenticated = false;
            tokenStorage.clear();
            localStorage.removeItem('user');
        },
    },
});

export const { setCredentials, logout } = authSlice.actions;

export const selectCurrentUser = (state) => state.auth.user;
export const selectCurrentToken = (state) => state.auth.token;
export const selectIsAuthenticated = (state) => state.auth.isAuthenticated;

export default authSlice.reducer;
