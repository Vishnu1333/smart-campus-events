package com.smartcampus.eventmanagement.repositories;

import com.smartcampus.eventmanagement.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmail(String email);

    Optional<User> findByFirebaseUid(String firebaseUid);
}
