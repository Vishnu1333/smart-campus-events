package com.smartcampus.eventmanagement.repositories;

import com.smartcampus.eventmanagement.models.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
}
