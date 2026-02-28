import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { selectCurrentUser } from '../../store/authSlice';

/**
 * RoleRoute — ensures the logged-in user has the required role.
 * If the user has a different role, redirects them to their correct dashboard.
 *
 * Usage: <RoleRoute allowedRole="ADMIN" />
 *        <RoleRoute allowedRole="STATION_OWNER" />
 */
export default function RoleRoute({ allowedRole }) {
    const user = useSelector(selectCurrentUser);

    if (!user) {
        return <Navigate to="/login" replace />;
    }

    if (user.role !== allowedRole) {
        // Redirect to the correct dashboard based on actual role
        if (user.role === 'ADMIN') {
            return <Navigate to="/admin" replace />;
        }
        if (user.role === 'STATION_OWNER') {
            return <Navigate to="/owner" replace />;
        }
        // CUSTOMER or unknown role — send to login
        return <Navigate to="/login" replace />;
    }

    return <Outlet />;
}
