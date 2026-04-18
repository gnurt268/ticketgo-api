package com.gnxrt.ticketgoapi.repository;

import com.gnxrt.ticketgoapi.model.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {
}
