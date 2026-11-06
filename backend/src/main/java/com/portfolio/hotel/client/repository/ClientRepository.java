package com.portfolio.hotel.client.repository;

import com.portfolio.hotel.client.domain.ClientEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClientRepository extends JpaRepository<ClientEntity, Long> {

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            SELECT c FROM ClientEntity c
            WHERE :query IS NULL
               OR LOWER(c.lastName) LIKE LOWER(CONCAT(CONCAT('%', :query), '%'))
               OR LOWER(c.firstName) LIKE LOWER(CONCAT(CONCAT('%', :query), '%'))
               OR LOWER(COALESCE(c.email, '')) LIKE LOWER(CONCAT(CONCAT('%', :query), '%'))
            """)
    Page<ClientEntity> search(@Param("query") String query, Pageable pageable);
}
