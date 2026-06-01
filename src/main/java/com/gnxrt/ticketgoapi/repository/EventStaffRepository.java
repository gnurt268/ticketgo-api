package com.gnxrt.ticketgoapi.repository;

import com.gnxrt.ticketgoapi.model.EventStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventStaffRepository extends JpaRepository<EventStaff, Long> {

    boolean existsByEventIdAndStaffId(Long eventId, Long staffId);

    List<EventStaff> findByEventId(Long eventId);

    List<EventStaff> findByStaffId(Long staffId);

    void deleteByEventIdAndStaffId(Long eventId, Long staffId);
}
