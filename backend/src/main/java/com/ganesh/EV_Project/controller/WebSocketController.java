package com.ganesh.EV_Project.controller;

import com.ganesh.EV_Project.model.ChargerSlot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Send slot status update to all subscribers of a station
    public void notifySlotStatusChange(Long stationId, ChargerSlot slot) {
        messagingTemplate.convertAndSend("/topic/station/" + stationId + "/slots", slot);
    }

    // Send booking update to specific user
    public void notifyUserBookingUpdate(Long userId, Object bookingUpdate) {
        messagingTemplate.convertAndSend("/topic/user/" + userId + "/bookings", bookingUpdate);
    }

    // Broadcast to all connected clients
    public void broadcastStationUpdate(Long stationId, Object update) {
        messagingTemplate.convertAndSend("/topic/stations/" + stationId, update);
    }
}
