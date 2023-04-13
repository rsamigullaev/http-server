package ru.netology;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public final class Server {
    public static final List<String> validPaths = List.of(
            "/index.html", "/spring.svg", "/spring.png",
            "/resources.html", "/styles.css", "/app.js", "/links.html",
            "/forms.html", "/classic.html", "/events.html", "/events.js"
    );

    private final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(64);
    private final ConcurrentHashMap<Method, Map<String, Handler>> handlers = new ConcurrentHashMap<>();

    public Server() {
    }

    public void listen(final int port) {
        try (final var serverSocket = new ServerSocket(port)) {
            while (!serverSocket.isClosed()) {
                final var socket = serverSocket.accept();
                executor.execute(() -> resolveCommand(socket));
            }
        } catch (final IOException cause) {
            throw new RuntimeException(cause);
        } finally {
            executor.shutdown();
        }
    }

    public void addHandler(final Method method, final String path, final Handler handler) {
        if (!handlers.containsKey(method)) handlers.put(method, new HashMap<>());
        handlers.get(method).put(path, handler);
    }

    public void emptyContent(
            final BufferedOutputStream out,
            final String responseCode,
            final String responseStatus
    ) {
        try {
            out.write((
                    "HTTP/1.1 " + responseCode + " " + responseStatus + "\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" + "\r\n"
            ).getBytes());
            out.flush();
        } catch (final IOException cause) {
            throw new RuntimeException(cause);
        }
    }

    public void defaultHandler(final BufferedOutputStream bos, final String path) {
        try {
            final var filePath = Path.of(".", "public", path);
            final var mimeType = Files.probeContentType(filePath);

            if (path.equals("/classic.html")) {
                final var template = Files.readString(filePath);
                final var content = template.replace(
                        "{time}",
                        LocalDateTime.now().toString()
                ).getBytes();
                bos.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + content.length + "\r\n" +
                                "Connection: close\r\n" + "\r\n"
                ).getBytes());
                bos.write(content);
                bos.flush();
                return;
            }

            final var length = Files.size(filePath);
            bos.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" + "\r\n"
            ).getBytes());
            Files.copy(filePath, bos);
            bos.flush();
        } catch (final IOException cause) {
            throw new RuntimeException(cause);
        }
    }

    private void resolveCommand(final Socket socket) {
        try (
                final var in = new BufferedInputStream(socket.getInputStream());
                final var out = new BufferedOutputStream(socket.getOutputStream())
        ) {
            final var request = Request.createRequest(in);

            if (request == null || !handlers.containsKey(request.getMethod())) {
                emptyContent(out, "400", "Bad Request");
                return;
            } else {
                System.out.println(request);
            }

            final var keyToHandler = handlers.get(request.getMethod());
            final var path = request.getPath().split("\\?")[0];
            if (keyToHandler.containsKey(path)) {
                final var handler = keyToHandler.get(path);
                handler.handle(request, out);
            } else {
                if (!validPaths.contains(path)) emptyContent(out, "404", "Not Found");
                else defaultHandler(out, path);
            }
        } catch (final IOException | URISyntaxException cause) {
            throw new RuntimeException(cause);
        }
    }
}
