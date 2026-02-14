import { useState, useEffect } from 'react';
import { getAdminStats, getAllUsers, getAllStations, getAllBookings } from '../../services/api';

const AdminDashboard = () => {
  const [stats, setStats] = useState({
    totalUsers: 0,
    totalStations: 0,
    totalBookings: 0,
    totalRevenue: 0,
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadStats();
  }, []);

  const loadStats = async () => {
    try {
      const [usersRes, stationsRes, bookingsRes] = await Promise.all([
        getAllUsers(),
        getAllStations(),
        getAllBookings(),
      ]);

      setStats({
        totalUsers: usersRes.data?.length || 0,
        totalStations: stationsRes.data?.length || 0,
        totalBookings: bookingsRes.data?.length || 0,
        totalRevenue: (bookingsRes.data?.length || 0) * 150, // Estimated
      });
    } catch (error) {
      console.error('Error loading stats:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  const statCards = [
    { title: 'Total Users', value: stats.totalUsers, icon: '👥', color: 'bg-blue-500' },
    { title: 'Total Stations', value: stats.totalStations, icon: '⚡', color: 'bg-green-500' },
    { title: 'Total Bookings', value: stats.totalBookings, icon: '📅', color: 'bg-purple-500' },
    { title: 'Total Revenue', value: `₹${stats.totalRevenue}`, icon: '💰', color: 'bg-yellow-500' },
  ];

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">Admin Dashboard</h1>
      
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {statCards.map((stat, index) => (
          <div key={index} className="bg-white rounded-lg shadow p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-500 text-sm">{stat.title}</p>
                <p className="text-2xl font-bold mt-1">{stat.value}</p>
              </div>
              <div className={`${stat.color} p-3 rounded-full text-white text-xl`}>
                {stat.icon}
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="mt-8 bg-white rounded-lg shadow p-6">
        <h2 className="text-xl font-semibold mb-4">Quick Actions</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <a href="/admin/users" className="p-4 bg-blue-50 rounded-lg hover:bg-blue-100 text-center">
            <span className="text-2xl">👥</span>
            <p className="mt-2 text-sm font-medium">Manage Users</p>
          </a>
          <a href="/admin/stations" className="p-4 bg-green-50 rounded-lg hover:bg-green-100 text-center">
            <span className="text-2xl">⚡</span>
            <p className="mt-2 text-sm font-medium">Manage Stations</p>
          </a>
          <a href="/admin/bookings" className="p-4 bg-purple-50 rounded-lg hover:bg-purple-100 text-center">
            <span className="text-2xl">📅</span>
            <p className="mt-2 text-sm font-medium">View Bookings</p>
          </a>
          <a href="/admin/users" className="p-4 bg-yellow-50 rounded-lg hover:bg-yellow-100 text-center">
            <span className="text-2xl">➕</span>
            <p className="mt-2 text-sm font-medium">Add Owner</p>
          </a>
        </div>
      </div>
    </div>
  );
};

export default AdminDashboard;
