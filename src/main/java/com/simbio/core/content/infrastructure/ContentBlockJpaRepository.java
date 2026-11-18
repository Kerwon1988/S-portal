package com.simbio.core.content.infrastructure;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.simbio.core.content.domain.ContentBlockEntity;

interface ContentBlockJpaRepository extends JpaRepository<ContentBlockEntity, Long> {

    @Query("""
            select count(block) > 0
            from ContentBlockEntity block
            where block.mediaPublicId = :filePublicId
              and block.translation.state = com.simbio.core.content.domain.TranslationState.PUBLISHED
            """)
    boolean existsInPublishedContent(@Param("filePublicId") UUID filePublicId);
}
