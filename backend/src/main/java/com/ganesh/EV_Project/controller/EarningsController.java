package com.ganesh.EV_Project.controller;

import com.ganesh.EV_Project.dto.EarningsSummaryDTO;
import com.ganesh.EV_Project.dto.TransactionRowDTO;
import com.ganesh.EV_Project.service.EarningsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/earnings")
public class EarningsController {

    @Autowired
    private EarningsService earningsService;

    @GetMapping("/summary/{ownerId}")
    @PreAuthorize("hasAnyRole('STATION_OWNER', 'ADMIN')")
    public ResponseEntity<EarningsSummaryDTO> getEarningsSummary(@PathVariable Long ownerId) {
        return ResponseEntity.ok(earningsService.getEarningsSummary(ownerId));
    }

    @GetMapping("/transactions/{ownerId}")
    @PreAuthorize("hasAnyRole('STATION_OWNER', 'ADMIN')")
    public ResponseEntity<Page<TransactionRowDTO>> getTransactionHistory(
            @PathVariable Long ownerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(earningsService.getTransactionHistory(ownerId, search, pageable));
    }

    @GetMapping(value = "/transactions/{ownerId}/export", produces = "text/csv")
    @PreAuthorize("hasAnyRole('STATION_OWNER', 'ADMIN')")
    public ResponseEntity<String> exportTransactions(@PathVariable Long ownerId) {
        java.util.List<TransactionRowDTO> rows = earningsService.getAllTransactions(ownerId);
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        StringBuilder sb = new StringBuilder();
        sb.append("Session ID,Date,Station,Energy (kWh),Amount (INR),Transaction ID,Status\n");
        for (TransactionRowDTO r : rows) {
            sb.append(r.sessionId()).append(',')
              .append(csv(r.timestamp() != null ? r.timestamp().format(fmt) : "")).append(',')
              .append(csv(r.stationName())).append(',')
              .append(r.energyKwh() != null ? r.energyKwh() : 0.0).append(',')
              .append(r.amount() != null ? r.amount() : 0.0).append(',')
              .append(csv(r.razorpayOrderId())).append(',')
              .append(csv(r.status())).append('\n');
        }

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=transactions.csv")
                .body(sb.toString());
    }

    /** Minimal CSV field escaping: quote fields containing comma, quote, or newline. */
    private String csv(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
