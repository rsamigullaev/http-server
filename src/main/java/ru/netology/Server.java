package ru.netology;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
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
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
        } catch (final IOException cause) {
            throw new RuntimeException(cause);
        }
    }

    private void resolveCommand(final Socket socket) {
        try (final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             final var out = new BufferedOutputStream(socket.getOutputStream())) {

            // read only request line for simplicity
            // must be in form GET /path HTTP/1.1
            final var requestLine = in.readLine();
            final var parts = requestLine.split(" ");

            if (parts.length != 3) {
                // just close socket
                socket.close();
                return;
            }

            final var method = Method.valueOf(parts[0]);
            final var path = parts[1];
            Request request = createRequest(method, path);

            // Check for bad requests and drop connection
            if (!handlers.containsKey(request.getMethod())) {
                emptyContent(out, "400", "Bad Request");
                return;
            }

            // Get PATH, HANDLER Map
            Map<String, Handler> handlerMap = handlers.get(request.getMethod());
            String requestPath = request.getPath();
            if (handlerMap.containsKey(requestPath)) {
                Handler handler = handlerMap.get(requestPath);
                handler.handle(request, out);
            } else {  // Defaults
                // Resource not found
                if (!validPaths.contains(request.getPath())) {
                    emptyContent(out, "404", "Not Found");
                } else {
                    defaultHandler(out, path);
                }
            }

        } catch (final IOException cause) {
            throw new RuntimeException(cause);
        }
    }

    public void defaultHandler(BufferedOutputStream out, String path) {
        try {
            final var filePath = Path.of(".", "public", path);
            final var mimeType = Files.probeContentType(filePath);

            // special case for classic
            if (path.equals("/classic.html")) {
                final var template = Files.readString(filePath);
                final var content = template.replace(
                        "{time}",
                        LocalDateTime.now().toString()
                ).getBytes();
                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + content.length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.write(content);
                out.flush();
                return;
            }

            final var length = Files.size(filePath);
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            Files.copy(filePath, out);
            out.flush();
        } catch (final IOException cause) {
            throw new RuntimeException(cause);
        }
    }

    private Request createRequest(final Method method, final String path) {
        if (method == null) return null;
        return new Request(method, path);
    }
}
