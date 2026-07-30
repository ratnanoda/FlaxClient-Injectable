package me.eldodebug.soar.management.altmanager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.attach.MinecraftAccess;
import me.eldodebug.soar.logger.GlideLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

/**
 * Stores and switches between Microsoft accounts the user owns.
 *
 * Persistence keeps only the long-lived refresh token + cached profile per
 * account. Switching account refreshes the Minecraft session on demand and
 * swaps the running {@link Session}.
 */
public class AltManager {

	private static final Type LIST_TYPE = new TypeToken<ArrayList<Account>>() {}.getType();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final File file;
	private final List<Account> accounts = new ArrayList<Account>();
	private Account activeAccount;

	public AltManager() {
		file = new File(Glide.getInstance().getFileManager().getGlideDir(), "alts.json");
		load();
	}

	public List<Account> getAccounts() {
		return accounts;
	}

	public Account getActiveAccount() {
		return activeAccount;
	}

	public void add(Account account) {
		accounts.add(account);
		save();
	}

	public void remove(Account account) {
		accounts.remove(account);
		if (activeAccount == account) {
			activeAccount = null;
		}
		save();
	}

	/**
	 * Make the account the active in-game session. Refreshes the Minecraft token
	 * first if the cached one is missing or expired. May block on network I/O, so
	 * call this off the render thread.
	 *
	 * @throws Exception with a user-facing message on failure
	 */
	public void login(Account account) throws Exception {

		if (!account.sessionValid()) {
			new MicrosoftAuth().refreshAndAuthenticate(account);
			save();
		}

		Session session = new Session(account.getProfileName(), account.getUuid(), account.getMcAccessToken(), "mojang");
		MinecraftAccess.setSession(Minecraft.getMinecraft(), session);
		activeAccount = account;
	}

	public void load() {
		accounts.clear();
		if (!file.exists()) {
			return;
		}
		try (FileReader reader = new FileReader(file)) {
			List<Account> loaded = GSON.fromJson(reader, LIST_TYPE);
			if (loaded != null) {
				accounts.addAll(loaded);
			}
		} catch (Exception e) {
			GlideLogger.error("Failed to load alts", e);
		}
	}

	public void save() {
		try (FileWriter writer = new FileWriter(file)) {
			GSON.toJson(accounts, LIST_TYPE, writer);
		} catch (Exception e) {
			GlideLogger.error("Failed to save alts", e);
		}
	}
}
