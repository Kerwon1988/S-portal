package com.simbio.core.staff.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.identity.api.InvitationAccepted;
import com.simbio.core.identity.api.IdentityAccountChanged;
import com.simbio.core.identity.api.StaffAccountAccessManager;
import com.simbio.core.shared.paging.PageQuery;
import com.simbio.core.shared.paging.PageResult;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;
import com.simbio.core.staff.api.StaffDirectory;
import com.simbio.core.staff.api.StaffSummary;
import com.simbio.core.staff.domain.StaffProfileEntity;

@Service
public class StaffProfileService implements StaffDirectory {

    private final StaffProfileStore store;
    private final AuditRecorder auditRecorder;
    private final StaffAccountAccessManager accountAccess;
    private final Clock clock;

    @Autowired
    public StaffProfileService(StaffProfileStore store, AuditRecorder auditRecorder,
            StaffAccountAccessManager accountAccess) {
        this(store, auditRecorder, accountAccess, Clock.systemUTC());
    }

    StaffProfileService(StaffProfileStore store, AuditRecorder auditRecorder,
            StaffAccountAccessManager accountAccess, Clock clock) {
        this.store = store;
        this.auditRecorder = auditRecorder;
        this.accountAccess = accountAccess;
        this.clock = clock;
    }

    @Transactional
    public void createFromInvitation(InvitationAccepted event) {
        if (event == null || !"EMPLOYEE".equals(event.roleCode())) {
            return;
        }
        Instant acceptedAt = Instant.now(clock);
        store.find(event.userId()).orElseGet(() -> store.save(new StaffProfileEntity(event.userId(),
                LocalDate.ofInstant(acceptedAt, ZoneOffset.UTC), acceptedAt)));
    }

    @Transactional
    public void synchronizeIdentityAccount(IdentityAccountChanged event) {
        if (event == null) {
            return;
        }
        Optional<StaffProfileEntity> existing = store.find(event.userId());
        if ("EMPLOYEE".equals(event.roleCode()) || "ADMIN".equals(event.roleCode())) {
            String profileStatus = event.statusCode();
            if (existing.isEmpty()) {
                Instant now = Instant.now(clock);
                StaffProfileEntity profile = new StaffProfileEntity(
                        event.userId(), LocalDate.ofInstant(now, ZoneOffset.UTC), now);
                profile.update(null, profileStatus);
                store.save(profile);
            } else if (!profileStatus.equals(existing.get().getStatus())) {
                existing.get().update(existing.get().getJobTitle(), profileStatus);
                store.save(existing.get());
            }
        } else if (existing.isPresent() && "ACTIVE".equals(existing.get().getStatus())) {
            existing.get().update(existing.get().getJobTitle(), "INACTIVE");
            store.save(existing.get());
        }
    }

    @Transactional
    public StaffSummary update(
            AuthenticatedUser actor,
            UUID userPublicId,
            String jobTitle,
            String status,
            List<String> requestedSpecialties) {
        requirePermission(actor);
        StaffProfileEntity profile = store.find(userPublicId)
                .orElseThrow(() -> new IllegalArgumentException("Worker profile is unavailable"));
        List<String> specialties = normalizeSpecialties(requestedSpecialties);
        String previousStatus = profile.getStatus();
        profile.update(jobTitle, status);
        if (!previousStatus.equals(status)) {
            accountAccess.setActive(actor, userPublicId, "ACTIVE".equals(status));
        }
        store.save(profile);
        store.replaceSpecialties(userPublicId, specialties);
        auditRecorder.record(new AuditRecord(actor.userId(), "STAFF_PROFILE_UPDATED", "STAFF_PROFILE",
                userPublicId, null));
        if (!previousStatus.equals(status)) {
            auditRecorder.record(new AuditRecord(actor.userId(), "STAFF_STATUS_CHANGED", "STAFF_PROFILE",
                    userPublicId, null));
        }
        return summary(profile, specialties);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<StaffSummary> search(String search, String status, PageQuery page) {
        String normalizedSearch = normalizeSearch(search);
        String normalizedStatus = normalizeStatus(status);
        var result = store.search(normalizedSearch, normalizedStatus,
                PageRequest.of(page.page(), page.size(), Sort.by(Sort.Direction.DESC, "createdAt")));
        List<StaffSummary> content = result.getContent().stream()
                .map(profile -> summary(profile, store.specialties(profile.getUserPublicId())))
                .toList();
        return new PageResult<>(content, page.page(), page.size(), result.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StaffSummary> findByUserId(UUID userPublicId) {
        return store.find(userPublicId)
                .map(profile -> summary(profile, store.specialties(userPublicId)));
    }

    private static void requirePermission(AuthenticatedUser actor) {
        if (actor == null || actor.role() != Role.ADMIN || !actor.hasPermission(Permission.STAFF_MANAGE)) {
            throw new AccessDeniedException("Worker management permission required");
        }
    }

    private static List<String> normalizeSpecialties(List<String> requested) {
        if (requested == null || requested.size() > 20) {
            throw new IllegalArgumentException("At most 20 specialties are allowed");
        }
        java.util.TreeMap<String, String> unique = new java.util.TreeMap<>();
        for (String item : requested) {
            if (item == null || item.isBlank() || item.trim().length() > 80) {
                throw new IllegalArgumentException("Invalid worker specialty");
            }
            String value = item.trim();
            unique.putIfAbsent(value.toLowerCase(java.util.Locale.ROOT), value);
        }
        return List.copyOf(unique.values());
    }

    private static String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String normalized = search.trim();
        if (normalized.length() > 120) {
            throw new IllegalArgumentException("Search text exceeds the allowed length");
        }
        return normalized;
    }

    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase(java.util.Locale.ROOT);
        if (!"ACTIVE".equals(normalized) && !"INACTIVE".equals(normalized)) {
            throw new IllegalArgumentException("Unsupported worker status");
        }
        return normalized;
    }

    private static StaffSummary summary(StaffProfileEntity profile, List<String> specialties) {
        return new StaffSummary(profile.getUserPublicId(), profile.getJobTitle(), profile.getEmploymentStartDate(),
                profile.getStatus(),
                specialties, profile.getCreatedAt());
    }
}
