package com.davivienda.factoraje.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.davivienda.factoraje.domain.entities.UserModel;

@Repository
public interface UserRepository extends JpaRepository<UserModel, UUID> {

        @EntityGraph(attributePaths = { "role", "entity" })
        Optional<UserModel> findByDui(String dui);

        @EntityGraph(attributePaths = { "role", "entity" })
        Optional<UserModel> findByEmail(String email);

        @Override
        @EntityGraph(attributePaths = { "role", "entity" })
        Page<UserModel> findAll(Pageable pageable);

        @EntityGraph(attributePaths = { "role", "entity" })
        @Query(value = "SELECT u FROM UserModel u LEFT JOIN u.entity e "
                        + SEARCH_FILTER, countQuery = "SELECT COUNT(u) FROM UserModel u LEFT JOIN u.entity e "
                                        + SEARCH_FILTER)
        Page<UserModel> search(@Param("search") String search, Pageable pageable);

        String SEARCH_FILTER = "WHERE u.dui LIKE CONCAT('%', REPLACE(:search, '-', ''), '%') "
                        + "OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) "
                        + "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) "
                        + "OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) "
                        + "OR LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%'))";

        boolean existsByDui(String dui);

        boolean existsByEmail(String email);

        boolean existsByDuiAndIdNot(String dui, UUID id);

        boolean existsByEmailAndIdNot(String email, UUID id);

        @EntityGraph(attributePaths = { "role", "entity" })
        @Query("""
                        SELECT u FROM UserModel u
                        WHERE u.status = com.davivienda.factoraje.domain.enums.GeneralStatusEnum.ACTIVE
                          AND u.entity.id = :entityId
                        """)
        List<UserModel> findActiveByEntityId(@Param("entityId") UUID entityId);

        @EntityGraph(attributePaths = { "role", "entity" })
        @Query("""
                        SELECT u FROM UserModel u JOIN u.role r
                        WHERE u.status = com.davivienda.factoraje.domain.enums.GeneralStatusEnum.ACTIVE
                          AND r.name = :roleName
                        """)
        List<UserModel> findActiveByRoleName(@Param("roleName") String roleName);

}
