package ru.netology;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class Request {
    private final Method method;
    private final String path;
    private final List<String> headers;
    private final List<NameValuePair> params;

    public Request(
            final Method method,
            final String path,
            final List<String> headers,
            final List<NameValuePair> params
    ) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.params = params;
    }

    public Method getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public List<NameValuePair> getQueryParams() {
        return params;
    }

    public NameValuePair getQueryParam(String name) {
        return params.stream()
                .filter(param -> param.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(
                        new NameValuePair() {
                            @Override
                            public String getName() {
                                return name;
                            }

                            @Override
                            public String getValue() {
                                return "";
                            }
                        }
                );
    }

    @Override
    public String toString() {
        return "Request{" +
                "method=" + method +
                ", path='" + path + '\'' +
                ", headers=" + headers +
                ", params=" + params +
                '}';
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static Request createRequest(final BufferedInputStream bin) throws IOException, URISyntaxException {
        final var limit = 4096;
        bin.mark(limit);
        final var buffer = new byte[limit];
        final var read = bin.read(buffer);

        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1) {
            return null;
        }

        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) {
            return null;
        }

        final var method = Method.fromString(requestLine[0]);
        if (!Arrays.stream(Method.values).collect(Collectors.toList()).contains(method)) {
            return null;
        }

        final var path = requestLine[1];

        final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            return null;
        }

        bin.reset();
        bin.skip(headersStart);

        final var headersBytes = bin.readNBytes(headersEnd - headersStart);
        List<String> headers = Arrays.asList(new String(headersBytes).split("\r\n"));

        List<NameValuePair> params = new URIBuilder(new URI(path), StandardCharsets.UTF_8).getQueryParams();

        return new Request(method, path, headers, params);
    }

    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}