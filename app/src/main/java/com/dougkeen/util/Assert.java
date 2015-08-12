package com.dougkeen.util;

public final class Assert {
    // Uninstantiable
    private Assert() {}

    public static <T> T notNull(T obj) {
        if (obj == null) {
            throw new AssertionError("Expected object to be non-null");
        }
        return obj;
    }
}
