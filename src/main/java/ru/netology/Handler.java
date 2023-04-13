package ru.netology;

import java.io.BufferedOutputStream;

@FunctionalInterface
public interface Handler {
    void handle(final Request request, final BufferedOutputStream bos);
}