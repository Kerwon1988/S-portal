package com.simbio.core.content.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.simbio.core.content.domain.TeamMemberTranslationEntity;

interface TeamMemberTranslationJpaRepository extends JpaRepository<TeamMemberTranslationEntity, Long> {

    @Query("""
            select translation
            from TeamMemberTranslationEntity translation
            where translation.member.publicId = :memberId and translation.locale = :locale
            """)
    java.util.Optional<TeamMemberTranslationEntity> findByMemberIdAndLocale(
            @Param("memberId") UUID memberId, @Param("locale") String locale);

    @Query("""
            select count(translation) > 0
            from TeamMemberTranslationEntity translation
            where translation.photoPublicId = :filePublicId
              and translation.member.active = true and translation.member.materialsApproved = true
            """)
    boolean existsInActiveMember(@Param("filePublicId") UUID filePublicId);

    @Query("""
            select translation
            from TeamMemberTranslationEntity translation
            where translation.member.publicId = :memberId
            order by translation.locale
            """)
    List<TeamMemberTranslationEntity> findAllByMemberId(@Param("memberId") UUID memberId);
}
