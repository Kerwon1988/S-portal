package com.simbio.core.content.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.simbio.core.content.domain.TeamMemberEntity;

interface TeamMemberJpaRepository extends JpaRepository<TeamMemberEntity, Long> {

    Optional<TeamMemberEntity> findByPublicId(UUID publicId);

    List<TeamMemberEntity> findAllByOrderByPositionAsc();

    List<TeamMemberEntity> findAllByActiveTrueOrderByPositionAsc();
}
