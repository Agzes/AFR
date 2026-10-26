// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.agzes.afr.AFR;
import io.github.agzes.afr.client.config.ConfigCodec;
import io.github.agzes.afr.client.config.ConfigPaths;
import io.github.agzes.afr.client.model.RpProfile;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;

public final class AfrCloud {

	private static final int MAX_BODY_LENGTH = 8 * 1024 * 1024;

	public static final String DEFAULT_BASE = "https://agzes.github.io/AFR/api";

	public static String base() {
		String system = System.getProperty("afr.cloud");
		if (system != null && !system.isBlank()) {
			return stripTrailing(system);
		}
		return DEFAULT_BASE;
	}

	private static String stripTrailing(String url) {
		String value = url.strip();
		while (value.endsWith("/")) {
			value = value.substring(0, value.length() - 1);
		}
		return value;
	}

	private static final HttpClient CLIENT = HttpClient.newBuilder()
		.followRedirects(HttpClient.Redirect.NORMAL)
		.connectTimeout(Duration.ofSeconds(10))
		.build();

	private AfrCloud() {}

	public record Library(JsonObject json, boolean fromCache) {}

	public static CompletableFuture<Library> library() {
		return get(base() + "/configs/library.json")
			.thenApply(body -> {
				JsonObject object = JsonParser.parseString(
					body
				).getAsJsonObject();
				if (
					!object.has("configs") ||
					!object.get("configs").isJsonArray()
				) {
					throw new IllegalStateException(
						"library.json has no 'configs' array"
					);
				}
				cacheLibrary(object);
				return new Library(object, false);
			})
			.exceptionallyCompose(error -> {
				JsonObject cached = readCachedLibrary();
				if (cached != null) {
					AFR.LOGGER.warn(
						"AFR: library unavailable, using cached catalog"
					);
					return CompletableFuture.completedFuture(
						new Library(cached, true)
					);
				}
				return CompletableFuture.failedFuture(error);
			});
	}

	private static void cacheLibrary(JsonObject object) {
		try {
			Files.createDirectories(ConfigPaths.root());
			Files.writeString(
				ConfigPaths.libraryCache(),
				object.toString(),
				StandardCharsets.UTF_8
			);
		} catch (IOException e) {
			AFR.LOGGER.warn("AFR: failed to cache the library catalog", e);
		}
	}

	private static JsonObject readCachedLibrary() {
		try {
			Path path = ConfigPaths.libraryCache();
			if (Files.exists(path)) {
				JsonObject object = JsonParser.parseString(
					Files.readString(path, StandardCharsets.UTF_8)
				).getAsJsonObject();
				if (
					object.has("configs") && object.get("configs").isJsonArray()
				) {
					return object;
				}
			}
		} catch (Exception e) {
			AFR.LOGGER.warn(
				"AFR: failed to read the cached library catalog",
				e
			);
		}
		return null;
	}

	public static CompletableFuture<JsonArray> announcements() {
		return get(base() + "/announcements.json").thenApply(body -> {
			var element = JsonParser.parseString(body);
			return element.isJsonArray()
				? element.getAsJsonArray()
				: new JsonArray();
		});
	}

	public static CompletableFuture<JsonObject> productionArgs() {
		return get(base() + "/production-args.json").thenApply(body ->
			JsonParser.parseString(body).getAsJsonObject()
		);
	}

	public static CompletableFuture<JsonObject> version(
		String project,
		String channel
	) {
		return get(
			base() + "/versions/" + project + "/" + channel + ".json"
		).thenApply(body -> JsonParser.parseString(body).getAsJsonObject());
	}

	public static CompletableFuture<RpProfile> downloadConfig(String url) {
		return get(url).thenApply(body -> {
			RpProfile profile = ConfigCodec.readString(body, RpProfile.class);
			if (profile == null || profile.meta == null) {
				throw new IllegalStateException("config has no meta");
			}
			profile.normalize();
			return profile;
		});
	}

	public static void runOnClient(Runnable runnable) {
		Minecraft client = Minecraft.getInstance();
		if (client.isSameThread()) {
			runnable.run();
		} else {
			client.execute(runnable);
		}
	}

	private static CompletableFuture<String> get(String url) {
		String resolved = resolveUrl(url);
		if (resolved == null || resolved.isBlank()) {
			return CompletableFuture.failedFuture(
				new IllegalArgumentException("empty URL")
			);
		}
		HttpRequest request;
		try {
			request = HttpRequest.newBuilder(URI.create(resolved))
				.header("User-Agent", "AFR")
				.timeout(Duration.ofSeconds(20))
				.GET()
				.build();
		} catch (RuntimeException error) {
			return CompletableFuture.failedFuture(error);
		}
		return CLIENT.sendAsync(
			request,
			HttpResponse.BodyHandlers.ofString()
		).thenApply(response -> {
			if (response.statusCode() != 200) {
				throw new RuntimeException(
					"HTTP " + response.statusCode() + " for " + resolved
				);
			}
			String body = response.body();
			if (body.length() > MAX_BODY_LENGTH) {
				throw new RuntimeException(
					"response is too large (" + body.length() + " bytes)"
				);
			}
			return body;
		});
	}

	public static String resolveUrl(String url) {
		if (url == null || url.isBlank()) {
			return url;
		}
		if (isAbsolute(url)) {
			return url;
		}
		String base = base().endsWith("/") ? base() : base() + "/";
		return base + (url.startsWith("/") ? url.substring(1) : url);
	}

	public static String siteBase() {
		String value = base();
		if (value.endsWith("/api")) {
			return value.substring(0, value.length() - 4);
		}
		return value;
	}

	public static String resolveSite(String url) {
		if (url == null || url.isBlank()) {
			return url;
		}
		if (isAbsolute(url)) {
			return url;
		}
		String base = siteBase();
		if (!base.endsWith("/")) {
			base = base + "/";
		}
		return base + (url.startsWith("/") ? url.substring(1) : url);
	}

	private static boolean isAbsolute(String url) {
		return url.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*");
	}
}
