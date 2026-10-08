package com.solar.backend.repository;

import com.solar.backend.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<UserAccount, Long> {
    UserAccount findByUsername(String username);

    List<UserAccount> findByFarms_Id(Long farmId);
}