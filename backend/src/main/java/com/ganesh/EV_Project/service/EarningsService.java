package com.ganesh.EV_Project.service;

import com.ganesh.EV_Project.dto.EarningsSummaryDTO;
import com.ganesh.EV_Project.dto.TransactionRowDTO;
import com.ganesh.EV_Project.repository.ChargingSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class EarningsService {

    @Autowired
    private ChargingSessionRepository sessionRepository;

    public EarningsSummaryDTO getEarningsSummary(Long ownerId) {
        Double lifetimeRevenue = sessionRepository.getTotalLifetimeRevenue(ownerId);
        if (lifetimeRevenue == null) lifetimeRevenue = 0.0;

        // Cost of the energy sold = sum(kWh × each station's grid tariff) over paid sessions.
        Double energyCost = sessionRepository.getTotalEnergyCost(ownerId);
        if (energyCost == null) energyCost = 0.0;

        // Real revenue settled in the last 48h (no fabricated balances/settlements).
        LocalDateTime since = LocalDateTime.now().minusHours(48);
        Double revenueLast48h = sessionRepository.getRecentRevenue(ownerId, since);
        if (revenueLast48h == null) revenueLast48h = 0.0;

        double netMargin = lifetimeRevenue - energyCost;

        return new EarningsSummaryDTO(
            lifetimeRevenue,
            energyCost,
            netMargin,
            revenueLast48h
        );
    }

    public Page<TransactionRowDTO> getTransactionHistory(Long ownerId, String search, Pageable pageable) {
        String q = (search != null && !search.isBlank()) ? search.trim() : null;
        return sessionRepository.getTransactionHistory(ownerId, q, pageable);
    }

    public java.util.List<TransactionRowDTO> getAllTransactions(Long ownerId) {
        return sessionRepository.getAllTransactions(ownerId);
    }
}
