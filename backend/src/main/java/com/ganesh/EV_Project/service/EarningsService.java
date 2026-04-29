package com.ganesh.EV_Project.service;

import com.ganesh.EV_Project.dto.EarningsSummaryDTO;
import com.ganesh.EV_Project.repository.ChargingSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class EarningsService {

    @Autowired
    private ChargingSessionRepository sessionRepository;

    public EarningsSummaryDTO getEarningsSummary(Long ownerId) {
        Double lifetimeRevenue = sessionRepository.getTotalLifetimeRevenue(ownerId);
        if (lifetimeRevenue == null) lifetimeRevenue = 0.0;

        // Pending payouts: Revenue from the last 48 hours (simulating settlement window)
        LocalDateTime settlementWindow = LocalDateTime.now().minusHours(48);
        Double pendingPayouts = sessionRepository.getRecentRevenue(ownerId, settlementWindow);
        if (pendingPayouts == null) pendingPayouts = 0.0;

        // Current Balance = Lifetime - (simulated payouts)
        // For now, let's assume 80% is settled and 20% is current balance/pending
        Double currentBalance = lifetimeRevenue - (lifetimeRevenue * 0.85); 
        Double lastSettlement = lifetimeRevenue * 0.15; // Dummy last payout amount

        return new EarningsSummaryDTO(
            currentBalance,
            lifetimeRevenue,
            pendingPayouts,
            lastSettlement
        );
    }
}
