package ru.netology;

import java.util.Arrays;

public enum Method {
    GET,
    HEAD,
    POST,
    PUT,
    DELETE,
    CONNECT,
    OPTIONS,
    TRACE,
    PATCH;

    public static final Method[] values = values();

    public static Method fromString(final String value) {
        return Arrays.stream(values)
                .filter(it -> it.name().equals(value))
                .findFirst()
                .orElse(null);
    }
}