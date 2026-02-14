import { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const Layout = ({ children }) => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(true);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isAdmin = user?.role === 'ADMIN';
  const isOwner = user?.role === 'STATION_OWNER';

  const adminLinks = [
    { path: '/admin/dashboard', label: 'Dashboard', icon: '📊' },
    { path: '/admin/users', label: 'Users', icon: '👥' },
    { path: '/admin/stations', label: 'All Stations', icon: '⚡' },
    { path: '/admin/bookings', label: 'All Bookings', icon: '📅' },
  ];

  const ownerLinks = [
    { path: '/owner/dashboard', label: 'Dashboard', icon: '📊' },
    { path: '/owner/stations', label: 'My Stations', icon: '⚡' },
    { path: '/owner/slots', label: 'Manage Slots', icon: '🔌' },
    { path: '/owner/bookings', label: 'Bookings', icon: '📅' },
  ];

  const links = isAdmin ? adminLinks : isOwner ? ownerLinks : [];

  return (
    <div className="flex min-h-screen bg-gray-100">
      {/* Sidebar */}
      <div className={`${sidebarOpen ? 'w-64' : 'w-20'} bg-gray-900 text-white transition-all duration-300 flex flex-col`}>
        <div className="p-4 flex items-center justify-between border-b border-gray-700">
          {sidebarOpen && <h1 className="text-xl font-bold">EV Portal</h1>}
          <button onClick={() => setSidebarOpen(!sidebarOpen)} className="text-gray-400 hover:text-white">
            {sidebarOpen ? '◀' : '▶'}
          </button>
        </div>
        
        <nav className="flex-1 py-4">
          {links.map((link) => (
            <Link
              key={link.path}
              to={link.path}
              className={`flex items-center px-4 py-3 hover:bg-gray-800 ${location.pathname === link.path ? 'bg-gray-800 border-l-4 border-blue-500' : ''}`}
            >
              <span className="text-xl">{link.icon}</span>
              {sidebarOpen && <span className="ml-3">{link.label}</span>}
            </Link>
          ))}
        </nav>

        <div className="p-4 border-t border-gray-700">
          {sidebarOpen && (
            <div className="mb-4">
              <p className="text-sm text-gray-400">Logged in as:</p>
              <p className="font-medium">{user?.name}</p>
              <p className="text-xs text-gray-500">{user?.role}</p>
            </div>
          )}
          <button
            onClick={handleLogout}
            className="w-full bg-red-600 hover:bg-red-700 px-4 py-2 rounded flex items-center justify-center"
          >
            <span>🚪</span>
            {sidebarOpen && <span className="ml-2">Logout</span>}
          </button>
        </div>
      </div>

      {/* Main Content */}
      <div className="flex-1 flex flex-col overflow-hidden">
        <header className="bg-white shadow-sm z-10">
          <div className="px-6 py-4 flex justify-between items-center">
            <h2 className="text-xl font-semibold text-gray-800">
              {isAdmin ? 'Admin Panel' : 'Station Owner Panel'}
            </h2>
            <div className="flex items-center space-x-4">
              <span className="text-gray-600">{user?.email}</span>
            </div>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto p-6">
          {children}
        </main>
      </div>
    </div>
  );
};

export default Layout;
