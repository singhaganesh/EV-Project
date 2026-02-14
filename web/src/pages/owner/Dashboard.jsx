import { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { getAllStations, getAllBookings } from '../../services/api';

const OwnerDashboard = () => {
  const { user } = useAuth();
  const [stats, setStats] = useState({
    totalStations: 0,
    totalSlots: 0,
    totalBookings: 0,
    totalRevenue: 0,
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadStats();
  }, []);

  const loadStats = async () => {
    try {
      const [stationsRes, bookingsRes] = await Promise.all([
        getAllStations(),
        getAllBookings(),
      ]);

      const stations = stationsRes.data || [];
      const bookings = bookingsRes.data?.data || bookingsRes.data || [];

      setStats({
        totalStations: stations.length,
        totalSlots: stations.reduce((acc, s) => acc + (s.slots?.length || 0), 0),
        totalBookings: bookings.length,
        totalRevenue: bookings.reduce((acc, b) => acc + (b.priceEstimate || 0), 0),
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
    { title: 'My Stations', value: stats.totalStations, icon: '⚡', color: 'bg-blue-500' },
    { title: 'Total Slots', value: stats.totalSlots, icon: '🔌', color: 'bg-green-500' },
    { title: 'Bookings', value: stats.totalBookings, icon: '📅', color: 'bg-purple-500' },
    { title: 'Revenue', value: `₹${stats.totalRevenue}`, icon: '💰', color: 'bg-yellow-500' },
  ];

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">Station Owner Dashboard</h1>
      <p className="text-gray-600 mb-6">Welcome back, {user?.name}!</p>
      
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
          <a href="/owner/stations" className="p-4 bg-blue-50 rounded-lg hover:bg-blue-100 text-center">
            <span className="text-2xl">⚡</span>
            <p className="mt-2 text-sm font-medium">My Stations</p>
          </a>
          <a href="/owner/slots" className="p-4 bg-green-50 rounded-lg hover:bg-green-100 text-center">
            <span className="text-2xl">🔌</span>
            <p className="mt-2 text-sm font-medium">Manage Slots</p>
          </a>
          <a href="/owner/bookings" className="p-4 bg-purple-50 rounded-lg hover:bg-purple-100 text-center">
            <span className="text-2xl">📅</span>
            <p className="mt-2 text-sm font-medium">Bookings</p>
          </a>
          <button className="p-4 bg-yellow-50 rounded-lg hover:bg-yellow-100 text-center">
            <span className="text-2xl">📊</span>
            <p className="mt-2 text-sm font-medium">Reports</p>
          </button>
        </div>
      </div>
    </div>
  );
};

export default OwnerDashboard;
