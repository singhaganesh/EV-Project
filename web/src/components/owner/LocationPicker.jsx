import React, { useState, useEffect, useRef, useMemo, useCallback } from 'react';
import { MapContainer, TileLayer, Marker, useMap, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { MapPin, Search, Navigation, Loader2, X } from 'lucide-react';

// Fix Leaflet's default icon issue with bundlers
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
    iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
    iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
    shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
});

const INDIA_CENTER = [20.5937, 78.9629];
const DEFAULT_ZOOM = 5;
const LOCATED_ZOOM = 16;

// Reverse geocode a lat/lng into a readable address string via Nominatim
async function reverseGeocode(lat, lng) {
    try {
        const res = await fetch(
            `https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lng}&format=json&addressdetails=1`,
            { headers: { 'Accept-Language': 'en' } }
        );
        const data = await res.json();
        return data.display_name || '';
    } catch {
        return '';
    }
}

// Forward geocode a query string into a list of suggestions (India-scoped)
async function forwardGeocode(query) {
    try {
        const res = await fetch(
            `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(query)}&format=json&countrycodes=in&limit=5&addressdetails=1`,
            { headers: { 'Accept-Language': 'en' } }
        );
        return await res.json();
    } catch {
        return [];
    }
}

// Sub-component: Fly the map to a new center when position changes
function MapFlyTo({ position, zoom }) {
    const map = useMap();

    // Fix leaflet grey tiles and sizing issues in animated modals
    useEffect(() => {
        // Run invalidateSize several times during and just after the modal "zoom-in" animation (200ms)
        const timeouts = [50, 200, 400].map(ms => 
            setTimeout(() => map.invalidateSize(), ms)
        );
        return () => timeouts.forEach(clearTimeout);
    }, [map]);

    useEffect(() => {
        if (position && Array.isArray(position)) {
            const lat = parseFloat(position[0]);
            const lng = parseFloat(position[1]);
            // Only fly to valid coordinates
            if (!isNaN(lat) && !isNaN(lng)) {
                try {
                    // Check if map container has a valid layout size before animating (prevents NaN error)
                    const size = map.getSize();
                    if (size.x > 0 && size.y > 0) {
                        map.flyTo([lat, lng], zoom, { duration: 1.2 });
                    } else {
                        map.setView([lat, lng], zoom);
                    }
                } catch (e) {
                    // Fallback to instant jump if animation fails
                    map.setView([lat, lng], zoom);
                }
            }
        }
    }, [position, zoom, map]);
    return null;
}

// Sub-component: Draggable marker that reports its position on drag end
function DraggableMarker({ position, onDragEnd }) {
    const markerRef = useRef(null);

    const eventHandlers = useMemo(() => ({
        dragend() {
            const marker = markerRef.current;
            if (marker) {
                const { lat, lng } = marker.getLatLng();
                onDragEnd(lat, lng);
            }
        },
    }), [onDragEnd]);

    return (
        <Marker
            draggable
            eventHandlers={eventHandlers}
            position={position}
            ref={markerRef}
        />
    );
}

// Sub-component: Click on map to place marker
function MapClickHandler({ onClick }) {
    useMapEvents({
        click(e) {
            onClick(e.latlng.lat, e.latlng.lng);
        },
    });
    return null;
}

export default function LocationPicker({ latitude, longitude, address, onLocationChange }) {
    const [searchQuery, setSearchQuery] = useState('');
    const [suggestions, setSuggestions] = useState([]);
    const [searching, setSearching] = useState(false);
    const [gpsLoading, setGpsLoading] = useState(false);
    const [showSuggestions, setShowSuggestions] = useState(false);
    const [mapPosition, setMapPosition] = useState(null);
    const [mapZoom, setMapZoom] = useState(DEFAULT_ZOOM);
    const [initialDetectDone, setInitialDetectDone] = useState(false);
    const searchTimeoutRef = useRef(null);
    const wrapperRef = useRef(null);

    // The marker position (from props or default)
    // Validate that latitude and longitude are actually numbers, not empty strings
    const markerPosition = useMemo(() => {
        const lat = parseFloat(latitude);
        const lng = parseFloat(longitude);
        if (!isNaN(lat) && !isNaN(lng)) {
            return [lat, lng];
        }
        return null;
    }, [latitude, longitude]);

    // Auto-detect GPS on mount
    useEffect(() => {
        if (initialDetectDone) return;
        if (latitude && longitude) {
            setInitialDetectDone(true);
            return;
        }

        if (navigator.geolocation) {
            setGpsLoading(true);
            navigator.geolocation.getCurrentPosition(
                async (pos) => {
                    const lat = pos.coords.latitude;
                    const lng = pos.coords.longitude;
                    const addr = await reverseGeocode(lat, lng);
                    onLocationChange(lat, lng, addr);
                    setMapPosition([lat, lng]);
                    setMapZoom(LOCATED_ZOOM);
                    setGpsLoading(false);
                    setInitialDetectDone(true);
                },
                () => {
                    // Permission denied or error — stay at India center
                    setGpsLoading(false);
                    setInitialDetectDone(true);
                },
                { enableHighAccuracy: true, timeout: 8000, maximumAge: 0 }
            );
        } else {
            setInitialDetectDone(true);
        }
    }, [initialDetectDone, latitude, longitude, onLocationChange]);

    // Debounced search
    useEffect(() => {
        if (!searchQuery || searchQuery.length < 3) {
            setSuggestions([]);
            setShowSuggestions(false);
            return;
        }

        if (searchTimeoutRef.current) clearTimeout(searchTimeoutRef.current);

        searchTimeoutRef.current = setTimeout(async () => {
            setSearching(true);
            const results = await forwardGeocode(searchQuery);
            setSuggestions(results);
            setShowSuggestions(results.length > 0);
            setSearching(false);
        }, 400);

        return () => clearTimeout(searchTimeoutRef.current);
    }, [searchQuery]);

    // Close suggestions on outside click
    useEffect(() => {
        function handleClickOutside(e) {
            if (wrapperRef.current && !wrapperRef.current.contains(e.target)) {
                setShowSuggestions(false);
            }
        }
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const handleSelectSuggestion = useCallback((suggestion) => {
        const lat = parseFloat(suggestion.lat);
        const lng = parseFloat(suggestion.lon);
        const addr = suggestion.display_name;
        onLocationChange(lat, lng, addr);
        setMapPosition([lat, lng]);
        setMapZoom(LOCATED_ZOOM);
        setSearchQuery('');
        setSuggestions([]);
        setShowSuggestions(false);
    }, [onLocationChange]);

    const handleMarkerDrag = useCallback(async (lat, lng) => {
        const addr = await reverseGeocode(lat, lng);
        onLocationChange(lat, lng, addr);
    }, [onLocationChange]);

    const handleMapClick = useCallback(async (lat, lng) => {
        const addr = await reverseGeocode(lat, lng);
        onLocationChange(lat, lng, addr);
    }, [onLocationChange]);

    const handleUseMyLocation = useCallback(() => {
        if (!navigator.geolocation) return;
        setGpsLoading(true);
        navigator.geolocation.getCurrentPosition(
            async (pos) => {
                const lat = pos.coords.latitude;
                const lng = pos.coords.longitude;
                const addr = await reverseGeocode(lat, lng);
                onLocationChange(lat, lng, addr);
                setMapPosition([lat, lng]);
                setMapZoom(LOCATED_ZOOM);
                setGpsLoading(false);
            },
            () => setGpsLoading(false),
            { enableHighAccuracy: true, timeout: 8000 }
        );
    }, [onLocationChange]);

    const mapCenter = mapPosition || (markerPosition ? markerPosition : INDIA_CENTER);

    return (
        <div className="space-y-4" ref={wrapperRef}>
            {/* Search Bar */}
            <div className="relative">
                <div className="absolute left-4 top-1/2 -translate-y-1/2 z-10 pointer-events-none">
                    {searching ? (
                        <Loader2 className="w-4 h-4 text-cyan-500 animate-spin" />
                    ) : (
                        <Search className="w-4 h-4 text-slate-400" />
                    )}
                </div>
                <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    onFocus={() => suggestions.length > 0 && setShowSuggestions(true)}
                    placeholder="Search for an address or place in India..."
                    className="w-full pl-11 pr-10 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-cyan-500 focus:border-cyan-500 focus:bg-white transition-all text-sm text-slate-900"
                />
                {searchQuery && (
                    <button
                        onClick={() => { setSearchQuery(''); setSuggestions([]); setShowSuggestions(false); }}
                        className="absolute right-3 top-1/2 -translate-y-1/2 p-1 text-slate-400 hover:text-slate-600 rounded-full"
                    >
                        <X className="w-4 h-4" />
                    </button>
                )}

                {/* Suggestions Dropdown */}
                {showSuggestions && (
                    <div className="absolute z-[1000] w-full mt-1 bg-white border border-slate-200 rounded-xl shadow-xl overflow-hidden max-h-60 overflow-y-auto">
                        {suggestions.map((s, i) => (
                            <button
                                key={i}
                                onClick={() => handleSelectSuggestion(s)}
                                className="w-full text-left px-4 py-3 hover:bg-cyan-50 transition-colors flex items-start gap-3 border-b border-slate-50 last:border-0"
                            >
                                <MapPin className="w-4 h-4 text-cyan-500 mt-0.5 flex-shrink-0" />
                                <span className="text-sm text-slate-700 leading-snug">{s.display_name}</span>
                            </button>
                        ))}
                    </div>
                )}
            </div>

            {/* Map */}
            <div className="relative rounded-2xl overflow-hidden border border-slate-200 shadow-sm">
                <MapContainer
                    center={mapCenter}
                    zoom={mapZoom}
                    style={{ height: '300px', width: '100%' }}
                    className="z-0"
                >
                    <TileLayer
                        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                    />
                    <MapFlyTo position={mapPosition} zoom={mapZoom} />
                    <MapClickHandler onClick={handleMapClick} />
                    {markerPosition && (
                        <DraggableMarker position={markerPosition} onDragEnd={handleMarkerDrag} />
                    )}
                </MapContainer>

                {/* GPS Button */}
                <button
                    type="button"
                    onClick={handleUseMyLocation}
                    disabled={gpsLoading}
                    className="absolute bottom-4 right-4 z-[400] bg-white text-slate-700 hover:bg-cyan-50 hover:text-cyan-600 px-3 py-2 rounded-xl shadow-lg border border-slate-200 flex items-center gap-2 text-xs font-semibold transition-all disabled:opacity-60"
                >
                    {gpsLoading ? (
                        <Loader2 className="w-4 h-4 animate-spin" />
                    ) : (
                        <Navigation className="w-4 h-4" />
                    )}
                    {gpsLoading ? 'Detecting...' : 'Use My Location'}
                </button>
            </div>

            {/* Read-only location preview */}
            {address && (
                <div className="bg-slate-50 rounded-xl p-4 space-y-2 border border-slate-100">
                    <div className="flex items-start gap-2">
                        <MapPin className="w-4 h-4 text-cyan-500 mt-0.5 flex-shrink-0" />
                        <p className="text-sm text-slate-700 leading-snug">{address}</p>
                    </div>
                    <div className="flex gap-4 text-xs text-slate-400 font-mono pl-6">
                        <span>Lat: {parseFloat(latitude).toFixed(6)}</span>
                        <span>Lng: {parseFloat(longitude).toFixed(6)}</span>
                    </div>
                </div>
            )}
        </div>
    );
}
