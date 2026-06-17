package com.ganesh.EV_Project.repository;

import com.ganesh.EV_Project.model.BookingTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingTemplateRepository extends JpaRepository<BookingTemplate, Long> {
    List<BookingTemplate> findByUserId(Long userId);
    List<BookingTemplate> findByActiveTrue();
}
