import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { tokenStorage } from './tokenStorage';

// The STOMP endpoint lives at the server root (/ws), not under the /api base.
const API_BASE = import.meta.env.VITE_API_BASE_URL || window.location.origin;
const WS_URL = API_BASE.replace(/\/api\/?$/, '') + '/ws';

let client = null;
// Active subscriptions, retained so we can (re)subscribe after a reconnect.
const subs = [];

function ensureClient() {
    if (client) return client;

    client = new Client({
        webSocketFactory: () => new SockJS(WS_URL),
        connectHeaders: { Authorization: `Bearer ${tokenStorage.getToken() || ''}` },
        reconnectDelay: 5000,
        onConnect: () => {
            // (Re)bind every tracked subscription on connect / reconnect.
            subs.forEach((entry) => {
                if (!entry.sub) entry.sub = bind(entry);
            });
        },
    });

    client.activate();
    return client;
}

function bind(entry) {
    return client.subscribe(entry.destination, (msg) => {
        try {
            entry.callback(JSON.parse(msg.body));
        } catch {
            // ignore malformed frames
        }
    });
}

/**
 * Subscribe to a STOMP destination. Returns an unsubscribe function.
 * Safe to call before the socket has connected — it binds on connect.
 */
export function subscribe(destination, callback) {
    ensureClient();
    const entry = { destination, callback, sub: null };
    if (client.connected) {
        entry.sub = bind(entry);
    }
    subs.push(entry);

    return () => {
        if (entry.sub) {
            try { entry.sub.unsubscribe(); } catch { /* ignore */ }
        }
        const i = subs.indexOf(entry);
        if (i >= 0) subs.splice(i, 1);
    };
}

/** Subscribe to live slot/gun status changes for a station. */
export function subscribeStationSlots(stationId, callback) {
    return subscribe(`/topic/station/${stationId}/slots`, callback);
}

/** Tear down the socket and drop all subscriptions. */
export function disconnect() {
    if (client) {
        try { client.deactivate(); } catch { /* ignore */ }
        client = null;
        subs.length = 0;
    }
}
