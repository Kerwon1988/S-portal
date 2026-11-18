package com.simbio.core.identity.api;

public record UserDirectoryOrder(Field field, Direction direction) {

    public static final UserDirectoryOrder DEFAULT =
            new UserDirectoryOrder(Field.CREATED_AT, Direction.DESC);

    public UserDirectoryOrder {
        if (field == null || direction == null) {
            throw new IllegalArgumentException("A directory sort field and direction are required");
        }
    }

    public enum Field {
        CREATED_AT,
        DISPLAY_NAME,
        EMAIL
    }

    public enum Direction {
        ASC,
        DESC
    }
}
