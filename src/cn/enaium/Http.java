/**
 * Copyright (c) 2022 Enaium
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package cn.enaium;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author Enaium
 */
public class Http {

    public static Gson gson() {
        return new GsonBuilder().disableHtmlEscaping().create();
    }

    public static String buildParam(Map<String, String> map) {
        var stringBuilder = new StringBuilder();
        for (Map.Entry<String, String> stringStringEntry : map.entrySet()) {
            stringBuilder
                    .append(stringStringEntry.getKey())
                    .append("=")
                    .append(URLEncoder.encode(stringStringEntry.getValue(), StandardCharsets.UTF_8))
                    .append("&");
        }
        return stringBuilder.toString();
    }

    public static String buildUrl(String url, Map<String, String> map) {
        var stringBuilder = new StringBuilder(url);
        if (!map.isEmpty()) {
            stringBuilder.append("?");
            stringBuilder.append(buildParam(map));
        }
        return stringBuilder.toString();
    }

    public static String postURL(String url, Map<String, String> data) {
        try {
            return HttpClient.newBuilder().build().send(
                    HttpRequest.newBuilder()
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .uri(new URI(url))
                            .POST(HttpRequest.BodyPublishers.ofString(buildParam(data))).build()
                    , HttpResponse.BodyHandlers.ofString()).body();
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static String postJSON(String url, Map<String, Object> data) {
        try {
            return HttpClient.newBuilder().build().send(
                    HttpRequest.newBuilder()
                            .header("Content-Type", "application/json")
                            .header("Accept", "application/json")
                            .uri(new URI(url))
                            .POST(HttpRequest.BodyPublishers.ofString(gson().toJson(data)))
                            .build(), HttpResponse.BodyHandlers.ofString()
            ).body();
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
