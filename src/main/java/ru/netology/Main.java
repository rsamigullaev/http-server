package ru.netology;

public final class Main {
    public static void main(String[] args) {
        Server server = new Server();

        server.addHandler(Method.GET, "/messages", (request, responseStream) ->
                server.emptyContent(responseStream, "404", "Not Found")
        );

        server.addHandler(Method.POST, "/messages", (request, responseStream) ->
                server.emptyContent(responseStream, "503", "Service Unavailable")
        );

        server.addHandler(Method.GET, "/", ((request, outputStream) -> server.defaultHandler(outputStream, "index.html")));

        server.listen(9999);
    }
}


