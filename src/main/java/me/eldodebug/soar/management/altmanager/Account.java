package me.eldodebug.soar.management.altmanager;

/**
 * A saved account in the AltManager.
 *
 * Only the long-lived Microsoft refresh token plus the cached profile are
 * persisted (Gson skips transient fields). The short-lived Minecraft access
 * token is re-derived from the refresh token on demand and kept in memory only.
 */
public class Account {

	private String profileName;
	private String uuid;
	private String msRefreshToken;

	// Session-only, never written to disk.
	private transient String mcAccessToken;
	private transient long mcTokenExpiry;

	public Account() {
	}

	public Account(String profileName, String uuid, String msRefreshToken) {
		this.profileName = profileName;
		this.uuid = uuid;
		this.msRefreshToken = msRefreshToken;
	}

	public boolean sessionValid() {
		return mcAccessToken != null && System.currentTimeMillis() < mcTokenExpiry;
	}

	public void setSession(String mcAccessToken, long mcTokenExpiry) {
		this.mcAccessToken = mcAccessToken;
		this.mcTokenExpiry = mcTokenExpiry;
	}

	public String getProfileName() {
		return profileName == null ? "?" : profileName;
	}

	public void setProfileName(String profileName) {
		this.profileName = profileName;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getMsRefreshToken() {
		return msRefreshToken;
	}

	public void setMsRefreshToken(String msRefreshToken) {
		this.msRefreshToken = msRefreshToken;
	}

	public String getMcAccessToken() {
		return mcAccessToken;
	}
}
