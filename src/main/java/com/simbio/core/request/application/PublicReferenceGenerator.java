package com.simbio.core.request.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicReferenceGenerator {
    private final RequestReferenceStore counters;
    private final Clock clock;

    @Autowired
    public PublicReferenceGenerator(RequestReferenceStore counters) {
        this(counters, Clock.systemUTC());
    }

    PublicReferenceGenerator(RequestReferenceStore counters, Clock clock) {
        this.counters = Objects.requireNonNull(counters);
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional
    public String next() {
        int year = LocalDate.now(clock).getYear();
        return "SOL-%d-%06d".formatted(year, counters.next(year));
    }
}
