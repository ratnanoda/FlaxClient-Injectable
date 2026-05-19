package me.eldodebug.flax.common.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class HttpUtils {

	private static final Gson GSON = new Gson();

	private HttpUtils() {
	}

	public static JsonObject readJson(HttpURLConnection connection) {
		return GSON.fromJson(readResponse(connection), JsonObject.class);
	}

	public static String readResponse(HttpURLConnection connection) {
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			return builder.toString();
		} catch (Exception error) {
			throw new RuntimeException(error);
		}
	}

	public static HttpURLConnection openConnection(String url, String method) throws Exception {
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.setRequestMethod(method);
		connection.setConnectTimeout(10000);
		connection.setReadTimeout(10000);
		connection.setRequestProperty("Accept", "application/json");
		connection.setRequestProperty("User-Agent", "FlaxClient/1.21.11");
		return connection;
	}
}
