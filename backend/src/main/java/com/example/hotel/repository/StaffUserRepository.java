package com.example.hotel.repository;
import com.example.hotel.domain.StaffUser; import java.util.Optional; import org.springframework.data.jpa.repository.JpaRepository;
public interface StaffUserRepository extends JpaRepository<StaffUser,Long>{Optional<StaffUser> findByUsername(String username);}
