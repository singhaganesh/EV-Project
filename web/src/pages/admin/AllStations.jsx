import { useState, useEffect } from 'react';
import { getAllStations, deleteStation } from '../../services/api';

const AllStations = () => {
  const [stations, setStations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    loadStations();
  }, []);

  const loadStations = async () => {
    try {
      const response = await getAllStations();
      setStations(response.data || []);
    } catch (error) {
      console.error('Error loading stations:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this station?')) return;
    try {
      await deleteStation(id);
      loadStations();
    } catch (error) {
      console.error('Error deleting station:', error);
    }
  };

  const filteredStations = stations.filter(station =>
    station.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    station.address.toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">All Stations</h1>
        <button className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg">
          + Add Station
        </button>
      </div>

      <div className="mb-4">
        <input
          type="text"
          placeholder="Search stations..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="w-full md:w-96 px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {filteredStations.map((station) => (
          <div key={station.id} className="bg-white rounded-lg shadow p-6">
            <div className="flex justify-between items-start mb-4">
              <h3 className="text-lg font-semibold">{station.name}</h3>
              <span className="bg-green-100 text-green-800 text-xs px-2 py-1 rounded">
                Active
              </span>
            </div>
            <p className="text-gray-600 text-sm mb-2">📍 {station.address}</p>
            <p className="text-gray-500 text-xs mb-4">
              📌 {station.latitude}, {station.longitude}
            </p>
            <div className="flex space-x-2">
              <button className="flex-1 bg-blue-50 text-blue-600 py-2 rounded hover:bg-blue-100">
                View
              </button>
              <button className="flex-1 bg-yellow-50 text-yellow-600 py-2 rounded hover:bg-yellow-100">
                Edit
              </button>
              <button
                onClick={() => handleDelete(station.id)}
                className="flex-1 bg-red-50 text-red-600 py-2 rounded hover:bg-red-100"
              >
                Delete
              </button>
            </div>
          </div>
        ))}
      </div>

      {filteredStations.length === 0 && (
        <div className="text-center py-12 text-gray-500">
          No stations found
        </div>
      )}
    </div>
  );
};

export default AllStations;
