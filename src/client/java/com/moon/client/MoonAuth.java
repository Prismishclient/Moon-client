package com.moon.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

final class MoonAuth {
    private static final String url = "https://mbdfafutagpnsbrbppsm.supabase.co/auth/v1";
    private static final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    static Session session;

    record Session(String token, Instant expires) {
        @Override
        public String toString() {
            return "Moon Client session";
        }
    }

    private static String key() {
        try (var in = MoonAuth.class.getResourceAsStream("/moon-supabase-key.txt")) {
            if (in != null) {
                String key = new String(in.readAllBytes(), StandardCharsets.UTF_8).strip();
                if (key.startsWith("sb_publishable_")) {
                    return key;
                }
            }
        } catch (IOException ignored) {
        }
        throw new IllegalStateException("Supabase publishable key is unavailable");
    }

    enum Action {
        LOGIN("/token?grant_type=password"),
        REGISTER("/signup"),
        VERIFY("/verify"),
        RESEND("/resend");

        final String path;

        Action(String path) {
            this.path = path;
        }
    }

    record Reply(Session session, String message, boolean ok) {}

    static CompletableFuture<Reply> submit(Action act, String mail, String val) {
        var data = new JsonObject();
        data.addProperty("email", mail);
        switch (act) {
            case LOGIN, REGISTER -> data.addProperty("password", val);
            case VERIFY -> {
                data.addProperty("token", val);
                data.addProperty("type", "email");
            }
            case RESEND -> data.addProperty("type", "signup");
        }
        var req = HttpRequest.newBuilder(URI.create(url + act.path))
                .timeout(Duration.ofSeconds(12))
                .header("apikey", key())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(data.toString()))
                .build();
        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString()).handle((res, err) -> {
            if (err != null) {
                return new Reply(null, "Can't reach Moon Client. Check your connection.", false);
            }
            return parse(act, res.statusCode(), res.body());
        });
    }

    static Reply parse(Action act, int status, String json) {
        if (status == 429) {
            return new Reply(null, "Too many attempts. Wait a bit and try again.", false);
        }
        try {
            var body = JsonParser.parseString(json).getAsJsonObject();
            if (status >= 200 && status < 300) {
                if (act != Action.RESEND && body.has("access_token") && !body.get("access_token").isJsonNull()) {
                    String token = body.get("access_token").getAsString();
                    long life = body.get("expires_in").getAsLong();
                    if (!token.isBlank() && life > 0 && body.has("user") && body.get("user").isJsonObject()) {
                        return new Reply(new Session(token, Instant.now().plusSeconds(life)), "Signed in to Moon Client", true);
                    }
                }
                if (act == Action.REGISTER || act == Action.RESEND) {
                    return new Reply(null, "Check your email for a Moon Client code.", true);
                }
            }
            String code = body.has("error_code") ? body.get("error_code").getAsString() : "";
            String msg = switch (code) {
                case "email_not_confirmed" -> "Confirm your Moon Client email first.";
                case "invalid_credentials" -> "Email or password is incorrect.";
                case "otp_expired" -> "Code is invalid or expired. Try again or resend it.";
                case "weak_password" -> "Choose a longer, stronger password.";
                case "email_address_not_authorized", "email_address_not_allowed" -> "Supabase can't send to this email yet.";
                case "email_address_invalid" -> "Enter a valid email address.";
                case "signup_disabled" -> "Registration is disabled.";
                default -> act == Action.VERIFY ? "Could not verify that code." : "Request failed. Please try again.";
            };
            return new Reply(null, msg, false);
        } catch (RuntimeException ignored) {
            return new Reply(null, "Unexpected response. Please try again.", false);
        }
    }

    static CompletableFuture<Boolean> signOut(Session ses) {
        var req = HttpRequest.newBuilder(URI.create(url + "/logout?scope=local"))
                .timeout(Duration.ofSeconds(12))
                .header("apikey", key())
                .header("Authorization", "Bearer " + ses.token())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return http.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                .handle((res, err) -> err == null && res.statusCode() == 204);
    }
}
