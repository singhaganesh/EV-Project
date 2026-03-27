package com.ganesh.EV_Project.repository;

import com.ganesh.EV_Project.enums.ConnectorType;
import com.ganesh.EV_Project.enums.SlotStatus;
import com.ganesh.EV_Project.model.ChargerSlot;
import com.ganesh.EV_Project.model.Dispensary;
import com.ganesh.EV_Project.model.Station;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChargerSlotRepository extends JpaRepository<ChargerSlot, Long> {
    List<ChargerSlot> findByStation(Station station);

    List<ChargerSlot> findByStatus(String status);

    List<ChargerSlot> findByStationAndStatus(Station station, SlotStatus status);

    List<ChargerSlot> findByDispensary(Dispensary dispensary);

    void deleteByStation(Station station);

    // Find all slots at a station
    List<ChargerSlot> findByStationId(Long stationId);

    @Query("SELECT COUNT(s) FROM ChargerSlot s " +
            "WHERE s.station.owner.id = :ownerId " +
            "AND s.status <> :excludedStatus")
    long countByOwnerIdAndStatusNot(@Param("ownerId") Long ownerId,
                                    @Param("excludedStatus") SlotStatus excludedStatus);

    @Query("SELECT COUNT(s) FROM ChargerSlot s " +
            "WHERE s.station.owner.id = :ownerId " +
            "AND s.status IN :statuses")
    long countByOwnerIdAndStatusIn(@Param("ownerId") Long ownerId,
                                   @Param("statuses") List<SlotStatus> statuses);

    // Pessimistic lock: find available slots of a specific connector type at a station
    // This prevents race conditions when two users book simultaneously
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ChargerSlot s WHERE s.station.id = :stationId " +
           "AND s.connectorType = :connectorType " +
           "AND s.status = 'AVAILABLE'")
    List<ChargerSlot> findAvailableSlotsForUpdate(
            @Param("stationId") Long stationId,
            @Param("connectorType") ConnectorType connectorType);
}
