import { useState, useEffect } from 'react';
import { getAllStations, getSlotsByStation, createSlot, deleteSlot } from '../../services/api';

const Slots = () => {
  const [stations, setStations] = useState([]);
  const [selectedStation, setSelectedStation] = useState(null);
  const [slots, setSlots] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [formData, setFormData] = useState({
    slotNumber: '',
    connectorType: 'CCS2',
    powerKw: 22,
    pricePerUnit: 15,
    status: 'AVAILABLE',
  });

  useEffect(() => {
    loadStations();
  }, []);

  useEffect(() => {
    if (selectedStation) {
      loadSlots(selectedStation.id);
    }
  }, [selectedStation]);

  const loadStations = async () => {
    try {
      const response = await getAllStations();
      setStations(response.data || []);
      if (response.data?.length > 0) {
        setSelectedStation(response.data[0]);
      }
    } catch (error) {
      console.error('Error loading stations:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadSlots = async (stationId) => {
    try {
      const response = await getSlotsByStation(stationId);
      setSlots(response.data?.data || response.data || []);
    } catch (error) {
      console.error('Error loading slots:', error);
    }
  };

  const handleCreateSlot = async (e) => {
    e.preventDefault();
    try {
      await createSlot({
        ...formData,
        stationId: selectedStation.id,
        powerKw: parseFloat(formData.powerKw),
        pricePerUnit: parseFloat(formData.pricePerUnit),
      });
      setShowModal(false);
      loadSlots(selectedStation.id);
      setFormData({
        slotNumber: '',
        connectorType: 'CCS2',
        powerKw: 22,
        pricePerUnit: 15,
        status: 'AVAILABLE',
      });
    } catch (error) {
      console.error('Error creating slot:', error);
    }
  };

  const handleDeleteSlot = async (id) => {
    if (!window.confirm('Are you sure you want to delete this slot?')) return;
    try {
      await deleteSlot(id);
      loadSlots(selectedStation.id);
    } catch (error) {
      console.error('Error deleting slot:', error);
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'AVAILABLE': return 'bg-green-100 text-green-800';
      case 'IN_USE': return 'bg-blue-100 text-blue-800';
      case 'MAINTENANCE': return 'bg-yellow-100 text-yellow-800';
      case 'OUT_OF_ORDER': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

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
        <h1 className="text-2xl font-bold">Manage Slots</h1>
        <button 
          onClick={() => setShowModal(true)}
          disabled={!selectedStation}
          className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg disabled:opacity-50"
        >
          + Add Slot
        </button>
      </div>

      {/* Station Selector */}
      <div className="mb-6">
        <label className="block text-sm font-medium mb-2">Select Station</label>
        <select
          value={selectedStation?.id || ''}
          onChange={(e) => setSelectedStation(stations.find(s => s.id === parseInt(e.target.value)))}
          className="w-full md:w-96 px-4 py-2 border rounded-lg"
        >
          {stations.map((station) => (
            <option key={station.id} value={station.id}>{station.name}</option>
          ))}
        </select>
      </div>

      {/* Slots Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {slots.map((slot) => (
          <div key={slot.id} className="bg-white rounded-lg shadow p-6">
            <div className="flex justify-between items-start mb-4">
              <div>
                <h3 className="text-lg font-semibold">Slot #{slot.slotNumber}</h3>
                <p className="text-gray-500 text-sm">{slot.connectorType}</p>
              </div>
              <span className={`px-2 py-1 text-xs font-semibold rounded-full ${getStatusColor(slot.status)}`}>
                {slot.status}
              </span>
            </div>
            <div className="space-y-2 mb-4">
              <p className="text-sm">⚡ Power: {slot.powerKw} kW</p>
              <p className="text-sm">💰 Price: ₹{slot.pricePerUnit}/kWh</p>
            </div>
            <button
              onClick={() => handleDeleteSlot(slot.id)}
              className="w-full bg-red-50 text-red-600 py-2 rounded hover:bg-red-100"
            >
              Delete Slot
            </button>
          </div>
        ))}
      </div>

      {slots.length === 0 && (
        <div className="text-center py-12 text-gray-500">
          <p className="text-xl mb-2">🔌</p>
          <p>No slots for this station. Click "Add Slot" to create one.</p>
        </div>
      )}

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <h2 className="text-xl font-bold mb-4">Add New Slot</h2>
            <form onSubmit={handleCreateSlot}>
              <div className="mb-4">
                <label className="block text-sm font-medium mb-1">Slot Number</label>
                <input
                  type="text"
                  value={formData.slotNumber}
                  onChange={(e) => setFormData({...formData, slotNumber: e.target.value})}
                  className="w-full px-4 py-2 border rounded-lg"
                  required
                />
              </div>
              <div className="mb-4">
                <label className="block text-sm font-medium mb-1">Connector Type</label>
                <select
                  value={formData.connectorType}
                  onChange={(e) => setFormData({...formData, connectorType: e.target.value})}
                  className="w-full px-4 py-2 border rounded-lg"
                >
                  <option value="CCS2">CCS2</option>
                  <option value="CHAdeMO">CHAdeMO</option>
                  <option value="GB_T">GB/T</option>
                  <option value="TESLA">Tesla</option>
                  <option value="TYPE2">Type 2</option>
                </select>
              </div>
              <div className="grid grid-cols-2 gap-4 mb-4">
                <div>
                  <label className="block text-sm font-medium mb-1">Power (kW)</label>
                  <input
                    type="number"
                    value={formData.powerKw}
                    onChange={(e) => setFormData({...formData, powerKw: e.target.value})}
                    className="w-full px-4 py-2 border rounded-lg"
                    required
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">Price/kWh (₹)</label>
                  <input
                    type="number"
                    value={formData.pricePerUnit}
                    onChange={(e) => setFormData({...formData, pricePerUnit: e.target.value})}
                    className="w-full px-4 py-2 border rounded-lg"
                    required
                  />
                </div>
              </div>
              <div className="flex space-x-2">
                <button
                  type="submit"
                  className="flex-1 bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700"
                >
                  Create Slot
                </button>
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="flex-1 bg-gray-200 text-gray-800 py-2 rounded-lg hover:bg-gray-300"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Slots;
