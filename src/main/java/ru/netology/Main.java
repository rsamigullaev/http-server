package ru.netology;

public final class Main {
    public static void main(String[] args) {
        final var server = new Server();

        server.addHandler(Method.GET, "/messages", (request, bos) ->
                server.emptyContent(bos, "404", "Not Found")
        );

        server.addHandler(Method.POST, "/messages", (request, bos) ->
                server.emptyContent(bos, "503", "Service Unavailable")
        );

        server.addHandler(Method.GET, "/", ((request, bos) -> server.defaultHandler(bos, "index.html")));

        server.listen(9999);
    }
}


