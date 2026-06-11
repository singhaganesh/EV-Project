package com.ganesh.EV_Project.config;

import com.ganesh.EV_Project.model.Booking;
import com.ganesh.EV_Project.model.Station;
import com.ganesh.EV_Project.model.User;
import com.ganesh.EV_Project.repository.BookingRepository;
import com.ganesh.EV_Project.repository.StationRepository;
import com.ganesh.EV_Project.service.UserService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Authenticates the STOMP CONNECT frame using the JWT supplied in the
 * "Authorization: Bearer ..." native header, and authorizes each SUBSCRIBE
 * against the destination so a client can only receive its own telemetry.
 */
@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final BookingRepository bookingRepository;
    private final StationRepository stationRepository;

    public JwtChannelInterceptor(JwtUtil jwtUtil,
                                 UserService userService,
                                 BookingRepository bookingRepository,
                                 StationRepository stationRepository) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.bookingRepository = bookingRepository;
        this.stationRepository = stationRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            authorizeSubscription(accessor);
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String token = extractToken(accessor);
        if (token == null) {
            throw new MessageDeliveryException("Missing authentication token");
        }

        String username;
        try {
            username = jwtUtil.extractUsername(token);
        } catch (Exception e) {
            throw new MessageDeliveryException("Invalid authentication token");
        }

        User user = (username != null) ? userService.findByPhoneNumber(username) : null;
        if (user == null && username != null) {
            user = userService.findByEmail(username);
        }
        if (user == null || !jwtUtil.validateToken(token, username)) {
            throw new MessageDeliveryException("Unauthorized WebSocket connection");
        }

        String role = user.getRole().name();
        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        accessor.setUser(principal);

        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs != null) {
            attrs.put("userId", user.getId());
            attrs.put("role", role);
        }
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs == null || attrs.get("userId") == null) {
            throw new MessageDeliveryException("Unauthorized subscription");
        }
        Long userId = (Long) attrs.get("userId");
        boolean isAdmin = "ADMIN".equals(attrs.get("role"));
        if (isAdmin) {
            return; // admins may observe any topic
        }

        // Private driver telemetry: /topic/session/{bookingId}
        if (destination.startsWith("/topic/session/")) {
            Long bookingId = parseTrailingId(destination);
            if (!bookingBelongsToUser(bookingId, userId)) {
                throw new MessageDeliveryException("Not allowed to subscribe to this session");
            }
            return;
        }

        // Per-user booking updates: /topic/user/{userId}/bookings
        if (destination.startsWith("/topic/user/")) {
            Long targetUserId = parseSegment(destination, 3);
            if (targetUserId == null || !targetUserId.equals(userId)) {
                throw new MessageDeliveryException("Not allowed to subscribe to this user channel");
            }
            return;
        }

        // Owner health metrics: /topic/owner/station/{stationId}
        if (destination.startsWith("/topic/owner/station/")) {
            Long stationId = parseTrailingId(destination);
            if (!stationOwnedBy(stationId, userId)) {
                throw new MessageDeliveryException("Not allowed to subscribe to this owner channel");
            }
            return;
        }

        // Public aggregate topics (/topic/station/{id}, /topic/station/{id}/slots)
        // are allowed for any authenticated user.
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        // Fallback: a raw "token" native header
        return accessor.getFirstNativeHeader("token");
    }

    private boolean bookingBelongsToUser(Long bookingId, Long userId) {
        if (bookingId == null) {
            return false;
        }
        return bookingRepository.findById(bookingId)
                .map(Booking::getUser)
                .map(u -> u != null && u.getId().equals(userId))
                .orElse(false);
    }

    private boolean stationOwnedBy(Long stationId, Long userId) {
        if (stationId == null) {
            return false;
        }
        return stationRepository.findById(stationId)
                .map(Station::getOwner)
                .map(o -> o != null && o.getId().equals(userId))
                .orElse(false);
    }

    /** Parses the last path segment as a Long, e.g. "/topic/session/42" -> 42. */
    private Long parseTrailingId(String destination) {
        String[] parts = destination.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            if (!parts[i].isEmpty()) {
                try {
                    return Long.parseLong(parts[i]);
                } catch (NumberFormatException ignored) {
                    // keep scanning backwards (e.g. trailing "/slots")
                }
            }
        }
        return null;
    }

    /** Parses the path segment at the given index (0-based on the leading slash split). */
    private Long parseSegment(String destination, int index) {
        String[] parts = destination.split("/");
        if (index < parts.length) {
            try {
                return Long.parseLong(parts[index]);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
