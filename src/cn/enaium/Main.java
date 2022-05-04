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

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;

import java.awt.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author Enaium
 */
public class Main {
    public static void main(String[] args) throws IOException, URISyntaxException {
        var authorize = Http.buildUrl("https://login.live.com/oauth20_authorize.srf"
                , Map.of(
                        //register app https://docs.microsoft.com/en-us/azure/active-directory/develop/quickstart-register-app
                        "client_id", "65669fcd-02f7-4ccb-a512-69e8a2261a5b"//client id
                        , "response_type", "code"
                        , "redirect_uri", "http://127.0.0.1:30828"//app redirect_uri
                        , "scope", "XboxLive.signin offline_access"
                ));
        Desktop.getDesktop().browse(new URI(authorize));

        var httpServer = HttpServer.create(new InetSocketAddress(30828), 0);
        httpServer.createContext("/", exchange -> {
            var code = exchange.getRequestURI().toString();
            code = code.substring(code.lastIndexOf('=') + 1);
            var token = "https://login.live.com/oauth20_token.srf";
            var oauth = Http.postURL(token, Map.of(
                    "client_id", "65669fcd-02f7-4ccb-a512-69e8a2261a5b"//client id
                    , "code", code
                    , "grant_type", "authorization_code"
                    , "redirect_uri", "http://127.0.0.1:30828"
            ));

            var access_token = Http.gson().fromJson(oauth, JsonObject.class).get("access_token").getAsString();

            var xbl = Http.gson().fromJson(Http.postJSON("https://user.auth.xboxlive.com/user/authenticate"
                    , Map.of(
                            "Properties", Map.of(
                                    "AuthMethod", "RPS"
                                    , "SiteName", "user.auth.xboxlive.com"
                                    , "RpsTicket", "d=" + access_token
                            )
                            , "RelyingParty", "http://auth.xboxlive.com"
                            , "TokenType", "JWT"
                    )
            ), JsonObject.class);

            var xbl_token = xbl.get("Token").getAsString();
            var xbl_uhs = xbl.get("DisplayClaims").getAsJsonObject().get("xui").getAsJsonArray().get(0).getAsJsonObject().get("uhs").getAsString();

            var xsts = Http.gson().fromJson(Http.postJSON("https://xsts.auth.xboxlive.com/xsts/authorize"
                    , Map.of(
                            "Properties", Map.of(
                                    "SandboxId", "RETAIL",
                                    "UserTokens", new String[]{xbl_token}
                            )
                            , "RelyingParty", "rp://api.minecraftservices.com/"
                            , "TokenType", "JWT"
                    )
            ), JsonObject.class);


            var xsts_token = xsts.get("Token").getAsString();
            var xsts_uhs = xsts.get("DisplayClaims").getAsJsonObject().get("xui").getAsJsonArray().get(0).getAsJsonObject().get("uhs").getAsString();


            System.out.println(Http.postJSON("https://api.minecraftservices.com/authentication/login_with_xbox",
                    Map.of("identityToken", String.format("XBL3.0 x=%s;%s", xsts_uhs, xsts_token))));


            var success = "Success";
            exchange.sendResponseHeaders(200, success.length());
            var responseBody = exchange.getResponseBody();
            responseBody.write(success.getBytes(StandardCharsets.UTF_8));
            responseBody.close();

            httpServer.stop(2);
        });
        httpServer.setExecutor(null);
        httpServer.start();
    }
}