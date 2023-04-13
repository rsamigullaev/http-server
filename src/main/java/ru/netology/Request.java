package ru.netology;

public final class Request {
    private final Method method;
    private final String path;

    public Request(Method method, String path) {
        this.method = method;
        this.path = path;
    }

    public Method getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }
}
