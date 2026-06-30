package me.eldodebug.soar.management.altmanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import me.eldodebug.soar.Glide;

/**
 * Microsoft OAuth2 device-code login for Minecraft.
 *
 * Flow: device code -> MSA access/refresh token -> Xbox Live token -> XSTS token
 * -> Minecraft access token -> profile (name + uuid). Everything here uses the
 * standard, Microsoft-hosted sign-in - the user authenticates in their own
 * browser and this app never sees a password or session cookie.
 *
 * Uses the Live Connect (legacy MSA) endpoints with Mojang's public launcher
 * client id, matching the working FlaxClientLauncher Rust flow. This avoids the
 * need for end users to register their own Azure AD application.
 */
public class MicrosoftAuth {

	/*
	 * Microsoft account client id.
	 *
	 * This is Mojang's public launcher id and is shared with the Live Connect
	 * device-code endpoint. It's the same id the FlaxClientLauncher Rust app
	 * uses, so end users do not need to register their own Azure AD app to
	 * sign in. To override (e.g. with a private Azure AD app id and the v2.0
	 * endpoint), drop the new id in '.minecraft/glide/msa_client_id.txt', or
	 * pass '-Dflax.msa.client_id=...', or set the FLAX_MSA_CLIENT_ID env var.
	 */
	private static final String CLIENT_ID = "00000000402b5328";
	private static final String CLIENT_ID_FILE = "msa_client_id.txt";
	private static final String SCOPE = "service::user.auth.xboxlive.com::MBI_SSL offline_access";

	private final String clientId = resolveClientId();

	private static final String DEVICE_CODE_URL = "https://login.live.com/oauth20_connect.srf";
	private static final String TOKEN_URL = "https://login.live.com/oauth20_token.srf";
	private static final String XBL_URL = "https://user.auth.xboxlive.com/user/authenticate";
	private static final String XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";
	private static final String MC_LOGIN_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";
	private static final String MC_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";

	private static final JsonParser PARSER = new JsonParser();

	public static class DeviceCode {
		public String deviceCode;
		public String userCode;
		public String verificationUri;
		public int interval;
		public int expiresIn;
	}

	private static class Msa {
		String accessToken;
		String refreshToken;
	}

	/** Step 1 - ask Microsoft for a code the user types in their browser. */
	public DeviceCode requestDeviceCode() throws IOException {
		requireClientId();
		String body = "client_id=" + enc(clientId)
				+ "&scope=" + enc(SCOPE)
				+ "&response_type=device_code";
		JsonObject o = json(post(DEVICE_CODE_URL, "application/x-www-form-urlencoded", body, null));

		if (o == null || !o.has("device_code")) {
			throw new IOException("Could not start sign-in" + errorOf(o));
		}

		DeviceCode dc = new DeviceCode();
		dc.deviceCode = o.get("device_code").getAsString();
		dc.userCode = o.get("user_code").getAsString();
		dc.verificationUri = o.get("verification_uri").getAsString();
		dc.interval = o.has("interval") ? o.get("interval").getAsInt() : 5;
		dc.expiresIn = o.has("expires_in") ? o.get("expires_in").getAsInt() : 900;
		return dc;
	}

	/**
	 * Step 2 onwards - block until the user finishes signing in, then exchange the
	 * resulting token for a playable Minecraft session and return the account.
	 */
	public Account pollAndAuthenticate(DeviceCode dc) throws IOException, InterruptedException {

		long deadline = System.currentTimeMillis() + dc.expiresIn * 1000L;
		int interval = Math.max(1, dc.interval);
		String body = "grant_type=urn:ietf:params:oauth:grant-type:device_code"
				+ "&client_id=" + enc(clientId) + "&device_code=" + enc(dc.deviceCode);

		Msa msa = null;

		while (System.currentTimeMillis() < deadline) {
			Thread.sleep(interval * 1000L);
			JsonObject o = json(post(TOKEN_URL, "application/x-www-form-urlencoded", body, null));

			if (o == null) {
				continue;
			}
			if (o.has("error")) {
				String err = o.get("error").getAsString();
				if (err.equals("authorization_pending")) {
					continue;
				}
				if (err.equals("slow_down")) {
					interval += 2;
					continue;
				}
				if (err.equals("expired_token")) {
					throw new IOException("Sign-in timed out");
				}
				if (err.equals("authorization_declined")) {
					throw new IOException("Sign-in was cancelled");
				}
				throw new IOException("Sign-in failed: " + err);
			}
			if (o.has("access_token")) {
				msa = new Msa();
				msa.accessToken = o.get("access_token").getAsString();
				msa.refreshToken = o.has("refresh_token") ? o.get("refresh_token").getAsString() : null;
				break;
			}
		}

		if (msa == null) {
			throw new IOException("Sign-in timed out");
		}

		Account account = new Account();
		account.setMsRefreshToken(msa.refreshToken);
		completeMinecraftLogin(account, msa.accessToken);
		return account;
	}

	/**
	 * Silent re-login for a saved account: refresh the MSA token, then walk the
	 * Xbox chain again to mint a fresh Minecraft session. Updates the account in
	 * place (rotated refresh token, profile, session token).
	 */
	public void refreshAndAuthenticate(Account account) throws IOException {

		if (account.getMsRefreshToken() == null) {
			throw new IOException("Account has no saved sign-in - re-add it");
		}

		requireClientId();
		String body = "grant_type=refresh_token&client_id=" + enc(clientId)
				+ "&scope=" + enc(SCOPE) + "&refresh_token=" + enc(account.getMsRefreshToken());
		JsonObject o = json(post(TOKEN_URL, "application/x-www-form-urlencoded", body, null));

		if (o == null || !o.has("access_token")) {
			throw new IOException("Session expired, please re-add this account" + errorOf(o));
		}

		if (o.has("refresh_token")) {
			account.setMsRefreshToken(o.get("refresh_token").getAsString());
		}

		completeMinecraftLogin(account, o.get("access_token").getAsString());
	}

	// Xbox Live -> XSTS -> Minecraft -> profile
	private void completeMinecraftLogin(Account account, String msAccessToken) throws IOException {

		// 3) Xbox Live. Live Connect tokens are passed through verbatim; the
		// "d=" prefix is only required for AAD v2.0 access tokens.
		JsonObject xblReq = new JsonObject();
		JsonObject xblProps = new JsonObject();
		xblProps.addProperty("AuthMethod", "RPS");
		xblProps.addProperty("SiteName", "user.auth.xboxlive.com");
		xblProps.addProperty("RpsTicket", msAccessToken);
		xblReq.add("Properties", xblProps);
		xblReq.addProperty("RelyingParty", "http://auth.xboxlive.com");
		xblReq.addProperty("TokenType", "JWT");

		JsonObject xbl = json(post(XBL_URL, "application/json", xblReq.toString(), null));
		if (xbl == null || !xbl.has("Token")) {
			throw new IOException("Xbox Live sign-in failed");
		}
		String xblToken = xbl.get("Token").getAsString();
		String userHash = uhs(xbl);

		// 4) XSTS
		JsonObject xstsReq = new JsonObject();
		JsonObject xstsProps = new JsonObject();
		xstsProps.addProperty("SandboxId", "RETAIL");
		JsonArray userTokens = new JsonArray();
		userTokens.add(new JsonPrimitive(xblToken));
		xstsProps.add("UserTokens", userTokens);
		xstsReq.add("Properties", xstsProps);
		xstsReq.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
		xstsReq.addProperty("TokenType", "JWT");

		Response xstsResp = post(XSTS_URL, "application/json", xstsReq.toString(), null);
		JsonObject xsts = json(xstsResp);
		if (xstsResp.code == 401 && xsts != null && xsts.has("XErr")) {
			throw new IOException(describeXErr(xsts.get("XErr").getAsLong()));
		}
		if (xsts == null || !xsts.has("Token")) {
			throw new IOException("Xbox authorization failed");
		}
		String xstsToken = xsts.get("Token").getAsString();

		// 5) Minecraft services
		JsonObject mcReq = new JsonObject();
		mcReq.addProperty("identityToken", "XBL3.0 x=" + userHash + ";" + xstsToken);
		JsonObject mc = json(post(MC_LOGIN_URL, "application/json", mcReq.toString(), null));
		if (mc == null || !mc.has("access_token")) {
			throw new IOException("Minecraft sign-in failed");
		}
		String mcToken = mc.get("access_token").getAsString();
		long expiry = System.currentTimeMillis() + (mc.has("expires_in") ? mc.get("expires_in").getAsLong() : 86400L) * 1000L;
		account.setSession(mcToken, expiry);

		// 6) Profile (also confirms game ownership)
		Response profResp = get(MC_PROFILE_URL, mcToken);
		JsonObject prof = json(profResp);
		if (profResp.code == 404 || prof == null || !prof.has("id")) {
			throw new IOException("This account does not own Minecraft: Java Edition");
		}
		account.setProfileName(prof.get("name").getAsString());
		account.setUuid(prof.get("id").getAsString());
	}

	private String uhs(JsonObject xboxResponse) throws IOException {
		try {
			return xboxResponse.getAsJsonObject("DisplayClaims").getAsJsonArray("xui")
					.get(0).getAsJsonObject().get("uhs").getAsString();
		} catch (Exception e) {
			throw new IOException("Unexpected Xbox response");
		}
	}

	private String describeXErr(long xErr) {
		if (xErr == 2148916233L) {
			return "This Microsoft account has no Xbox profile - create one first";
		}
		if (xErr == 2148916235L) {
			return "Xbox Live is not available in this account's region";
		}
		if (xErr == 2148916236L || xErr == 2148916237L) {
			return "This account needs adult verification";
		}
		if (xErr == 2148916238L) {
			return "This is a child account - add it to a Family group first";
		}
		return "Xbox authorization rejected (" + xErr + ")";
	}

	// --- tiny HTTP layer ---------------------------------------------------

	private static class Response {
		int code;
		String body;
	}

	private Response post(String url, String contentType, String body, String bearer) throws IOException {
		return request("POST", url, contentType, body, bearer);
	}

	private Response get(String url, String bearer) throws IOException {
		return request("GET", url, null, null, bearer);
	}

	private Response request(String method, String url, String contentType, String body, String bearer) throws IOException {

		HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
		c.setRequestMethod(method);
		c.setConnectTimeout(15000);
		c.setReadTimeout(15000);
		c.setRequestProperty("Accept", "application/json");
		if (contentType != null) {
			c.setRequestProperty("Content-Type", contentType);
		}
		if (bearer != null) {
			c.setRequestProperty("Authorization", "Bearer " + bearer);
		}

		if (body != null) {
			c.setDoOutput(true);
			byte[] out = body.getBytes(StandardCharsets.UTF_8);
			try (OutputStream os = c.getOutputStream()) {
				os.write(out);
			}
		}

		Response r = new Response();
		r.code = c.getResponseCode();
		InputStream is = r.code >= 400 ? c.getErrorStream() : c.getInputStream();
		r.body = read(is);
		c.disconnect();
		return r;
	}

	private String read(InputStream is) throws IOException {
		if (is == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
		}
		return sb.toString();
	}

	private JsonObject json(Response r) {
		if (r == null || r.body == null || r.body.isEmpty()) {
			return null;
		}
		try {
			return PARSER.parse(r.body).getAsJsonObject();
		} catch (Exception e) {
			return null;
		}
	}

	private String errorOf(JsonObject o) {
		if (o != null && o.has("error_description")) {
			return ": " + o.get("error_description").getAsString();
		}
		if (o != null && o.has("error")) {
			return ": " + o.get("error").getAsString();
		}
		return "";
	}

	private static String enc(String s) {
		try {
			return java.net.URLEncoder.encode(s, "UTF-8");
		} catch (Exception e) {
			return s;
		}
	}

	// --- client id resolution ---------------------------------------------

	private void requireClientId() throws IOException {
		if (isPlaceholder(clientId)) {
			throw new IOException("No Microsoft client id is set. Restore the bundled default by "
					+ "deleting glide/" + CLIENT_ID_FILE + ", or paste your own client id into that file.");
		}
	}

	private static boolean isPlaceholder(String id) {
		if (id == null) {
			return true;
		}
		String t = id.trim();
		// blank, or the all-zeros/dashes placeholder
		return t.isEmpty() || t.replace("-", "").replace("0", "").isEmpty();
	}

	private static String resolveClientId() {

		// 1) runtime config file - editable without recompiling
		try {
			File cfg = new File(Glide.getInstance().getFileManager().getGlideDir(), CLIENT_ID_FILE);
			if (cfg.exists()) {
				List<String> lines = Files.readAllLines(cfg.toPath());
				for (String line : lines) {
					String t = line.trim();
					if (!t.isEmpty() && !t.startsWith("#")) {
						return t;
					}
				}
			} else {
				writeTemplate(cfg);
			}
		} catch (Exception ignored) {
		}

		// 2) JVM system property / environment variable
		String sys = System.getProperty("flax.msa.client_id");
		if (sys != null && !sys.trim().isEmpty()) {
			return sys.trim();
		}
		String env = System.getenv("FLAX_MSA_CLIENT_ID");
		if (env != null && !env.trim().isEmpty()) {
			return env.trim();
		}

		// 3) compiled-in default
		return CLIENT_ID;
	}

	private static void writeTemplate(File cfg) {
		try {
			String template = "# Optional: override the Microsoft client id used for sign-in.\n"
					+ "# Leave this file empty to use the bundled Live Connect default\n"
					+ "# (the same id the Rust FlaxClientLauncher uses; works out of the box).\n"
					+ "# To use your own Azure AD app instead, paste its Application (client)\n"
					+ "# ID below on its own line and restart the game.\n";
			Files.write(cfg.toPath(), template.getBytes(StandardCharsets.UTF_8));
		} catch (Exception ignored) {
		}
	}
}
