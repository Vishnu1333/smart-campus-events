package com.smartcampus.eventmanagement.config;

import com.smartcampus.eventmanagement.models.Event;
import com.smartcampus.eventmanagement.repositories.EventRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class DatabaseSeeder {

    @Bean
    public CommandLineRunner seedDatabase(EventRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                Event event1 = new Event();
                event1.setName("Annual Tech Symposium");
                event1.setDescription("A gathering of tech enthusiasts to showcase innovative projects and attend expert talks.");
                event1.setVenue("Main Auditorium");
                event1.setEventDate(LocalDateTime.now().plusDays(5).withHour(10).withMinute(0).withSecond(0));
                
                Event event2 = new Event();
                event2.setName("Campus Cultural Fest");
                event2.setDescription("A vibrant celebration of art, music, and dance from various student clubs.");
                event2.setVenue("Open Air Theatre");
                event2.setEventDate(LocalDateTime.now().plusDays(14).withHour(17).withMinute(30).withSecond(0));

                Event event3 = new Event();
                event3.setName("Alumni Career Fair");
                event3.setDescription("Connect with distinguished alumni and explore internship and job opportunities.");
                event3.setVenue("Student Activity Center");
                event3.setEventDate(LocalDateTime.now().plusDays(2).withHour(9).withMinute(0).withSecond(0));
                Event event4 = new Event();
                event4.setName("Hackathon: Code for Good");
                event4.setDescription("A 24-hour coding marathon to build solutions for local non-profit organizations.");
                event4.setVenue("Computer Science Lab 3");
                event4.setEventDate(LocalDateTime.now().plusDays(21).withHour(18).withMinute(0).withSecond(0));

                Event event5 = new Event();
                event5.setName("Guest Lecture: AI in Healthcare");
                event5.setDescription("Dr. Smith from the National Research Institute will discuss the future of AI in medical diagnostics.");
                event5.setVenue("Seminar Hall B");
                event5.setEventDate(LocalDateTime.now().plusDays(8).withHour(14).withMinute(0).withSecond(0));

                Event event6 = new Event();
                event6.setName("Inter-department Sports Meet");
                event6.setDescription("Annual sports competition. Come support your department in basketball, soccer, and track events!");
                event6.setVenue("University Stadium");
                event6.setEventDate(LocalDateTime.now().plusDays(30).withHour(8).withMinute(30).withSecond(0));

                repository.save(event1);
                repository.save(event2);
                repository.save(event3);
                repository.save(event4);
                repository.save(event5);
                repository.save(event6);
            }
        };
    }
}
