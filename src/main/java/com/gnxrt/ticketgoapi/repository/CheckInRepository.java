package com.gnxrt.ticketgoapi.repository;

import com.gnxrt.ticketgoapi.model.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CheckInRepository extends JpaRepository<CheckIn, Long> {
}
