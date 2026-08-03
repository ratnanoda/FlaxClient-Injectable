package me.eldodebug.soar;

import java.io.File;
import java.util.Arrays;

import me.eldodebug.soar.gui.mainmenu.GuiGlideMainMenu;
import me.eldodebug.soar.gui.modmenu.GuiModMenu;
import me.eldodebug.soar.management.mods.RestrictedMod;
import me.eldodebug.soar.management.remote.blacklists.BlacklistManager;
import me.eldodebug.soar.management.remote.discord.DiscordStats;
import me.eldodebug.soar.management.remote.news.NewsManager;
import me.eldodebug.soar.management.remote.update.Update;
import me.eldodebug.soar.ui.ClickEffects;
import me.eldodebug.soar.utils.Sound;
import org.apache.commons.lang3.ArrayUtils;

import me.eldodebug.soar.logger.GlideLogger;
import me.eldodebug.soar.management.altmanager.AltManager;
import me.eldodebug.soar.management.cape.CapeManager;
import me.eldodebug.soar.management.remote.changelog.ChangelogManager;
import me.eldodebug.soar.management.color.ColorManager;
import me.eldodebug.soar.management.command.CommandManager;
import me.eldodebug.soar.management.event.EventManager;
import me.eldodebug.soar.management.file.FileManager;
import me.eldodebug.soar.management.language.LanguageManager;
import me.eldodebug.soar.management.mods.ModManager;
import me.eldodebug.soar.management.mods.impl.InternalSettingsMod;
import me.eldodebug.soar.management.mods.impl.YouTubePipMod;
import me.eldodebug.soar.management.music.MusicHudRenderer;
import me.eldodebug.soar.management.music.MusicManager;
import me.eldodebug.soar.management.youtube.YouTubeManager;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.notification.NotificationManager;
import me.eldodebug.soar.management.profile.ProfileManager;
import me.eldodebug.soar.management.quickplay.QuickPlayManager;
import me.eldodebug.soar.management.screenshot.ScreenshotManager;
import me.eldodebug.soar.management.security.SecurityFeatureManager;
import me.eldodebug.soar.management.waypoint.WaypointManager;
import me.eldodebug.soar.utils.OptifineUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.opengl.Display;

public class Glide {

	private static Glide instance = new Glide();
	private Minecraft mc = Minecraft.getMinecraft();
	private boolean updateNeeded, soar8Released;
	private String name, version;
	private int verIdentifier;
	
	private NanoVGManager nanoVGManager;
	private FileManager fileManager;
	private LanguageManager languageManager;
	private EventManager eventManager;
	private ModManager modManager;
	private CapeManager capeManager;
	private ColorManager colorManager;
	private ProfileManager profileManager;
	private CommandManager commandManager;
	private ScreenshotManager screenshotManager;
	private NotificationManager notificationManager;
	private SecurityFeatureManager securityFeatureManager;
	private QuickPlayManager quickPlayManager;
	private ChangelogManager changelogManager;
	private NewsManager newsManager;
	private DiscordStats discordStats;
    private WaypointManager waypointManager;
	private GuiModMenu modMenu;
	private GuiGlideMainMenu mainMenu;
	private long launchTime;
	private File firstLoginFile;
	private Update update;
	private ClickEffects clickEffects;
	private BlacklistManager blacklistManager;
	private RestrictedMod restrictedMod;
	private AltManager altManager;
	private MusicManager musicManager;
	private YouTubeManager youTubeManager;
	
	public Glide() {
		name = "FlaxClient";
		version = BuildVersion.DISPLAY_VERSION;
		verIdentifier = 1000 + BuildVersion.BUILD_NUMBER;
	}
	
	public void start() {
		try {
			OptifineUtils.disableFastRender();
			this.removeOptifineZoom();
		} catch(Throwable ignored) {}
		blacklistManager = new BlacklistManager();
		restrictedMod = new RestrictedMod();
		try {
			restrictedMod.shouldCheck = !System.getProperty("me.eldodebug.soar.glideclient.blacklistchecks", "true").equalsIgnoreCase("false");
		} catch (Exception ignored) {}
		fileManager = new FileManager();
		firstLoginFile = new File(fileManager.getCacheDir(), "first.tmp");
		languageManager = new LanguageManager();
		eventManager = new EventManager();
		modManager = new ModManager();

		modManager.init();

		if(me.eldodebug.soar.utils.mouse.NativeMouseBridge.isAvailable()) {
			GlideLogger.info("Native mouse bridge (uinput) loaded — AutoClicker / AimAssist will inject real input");
		} else {
			GlideLogger.warn("Native mouse bridge unavailable: " + me.eldodebug.soar.utils.mouse.NativeMouseBridge.getErrorDetail()
					+ " — AutoClicker / AimAssist will use the internal fallback");
		}
		
		capeManager = new CapeManager();
		colorManager = new ColorManager();
		profileManager = new ProfileManager();

		launchTime = System.currentTimeMillis();

		commandManager = new CommandManager();
		notificationManager = new NotificationManager();
		securityFeatureManager = new SecurityFeatureManager();
		changelogManager = new ChangelogManager();
		newsManager = new NewsManager();
		youTubeManager = new YouTubeManager();
		discordStats = new DiscordStats();
		discordStats.check();
		update = new Update();
		update.check();

		eventManager.register(new GlideHandler());
		eventManager.register(new MusicHudRenderer());
		if(YouTubePipMod.getInstance() != null && !YouTubePipMod.getInstance().isToggled()) {
			YouTubePipMod.getInstance().setToggled(true);
		}

		InternalSettingsMod.getInstance().setToggled(true);
		clickEffects = new ClickEffects();
	}
	
	public void stop() {
		profileManager.save();
		if(musicManager != null) musicManager.stop();
		if(youTubeManager != null) youTubeManager.shutdown();
		Sound.play("soar/audio/close.wav", true);

	}
	
	private void removeOptifineZoom() {
		if(hasOptifine()) {
			try {
				this.unregisterKeybind((KeyBinding) GameSettings.class.getField("ofKeyBindZoom").get(mc.gameSettings));
			} catch(Exception e) {
				GlideLogger.error("Failed to unregister zoom key", e);
			}
		}
	}

	private boolean hasOptifine() {
		ClassLoader loader = Glide.class.getClassLoader();
		try {
			Class.forName("Config", false, loader);
			return true;
		} catch(ClassNotFoundException ignored) {
		}
		try {
			Class.forName("optifine.Patcher", false, loader);
			return true;
		} catch(ClassNotFoundException ignored) {
			return false;
		}
	}
	
    private void unregisterKeybind(KeyBinding key) {
        if (Arrays.asList(mc.gameSettings.keyBindings).contains(key)) {
            mc.gameSettings.keyBindings = ArrayUtils.remove(mc.gameSettings.keyBindings, Arrays.asList(mc.gameSettings.keyBindings).indexOf(key));
            key.setKeyCode(0);
        }
    }
    
	public static Glide getInstance() {
		return instance;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {return version;}

	public int getVersionIdentifier() {return verIdentifier;}

	public FileManager getFileManager() {
		return fileManager;
	}

	public ModManager getModManager() {
		return modManager;
	}

	public LanguageManager getLanguageManager() {
		return languageManager;
	}

	public EventManager getEventManager() {
		return eventManager;
	}

	public NanoVGManager getNanoVGManager() {
		return nanoVGManager;
	}

	public ColorManager getColorManager() {
		return colorManager;
	}

	public ProfileManager getProfileManager() {
		return profileManager;
	}

	public CapeManager getCapeManager() {
		return capeManager;
	}

	public CommandManager getCommandManager() {
		return commandManager;
	}

	public ScreenshotManager getScreenshotManager() {
		if(screenshotManager == null) {
			screenshotManager = new ScreenshotManager();
		}
		return screenshotManager;
	}

	public void setNanoVGManager(NanoVGManager nanoVGManager) {
		this.nanoVGManager = nanoVGManager;
	}

	public NotificationManager getNotificationManager() {
		return notificationManager;
	}

	public SecurityFeatureManager getSecurityFeatureManager() {
		return securityFeatureManager;
	}

	public AltManager getAltManager() {
		if(altManager == null) {
			altManager = new AltManager();
		}
		return altManager;
	}

	public QuickPlayManager getQuickPlayManager() {
		if(quickPlayManager == null) {
			quickPlayManager = new QuickPlayManager();
		}
		return quickPlayManager;
	}

	public ChangelogManager getChangelogManager() {
		return changelogManager;
	}
	public NewsManager getNewsManager() { return newsManager; }

	public DiscordStats getDiscordStats() {
		return discordStats;
	}

	public WaypointManager getWaypointManager() {
		if(waypointManager == null) {
			waypointManager = new WaypointManager();
		}
		return waypointManager;
	}

	public GuiModMenu getModMenu() {
		if(modMenu == null) {
			modMenu = new GuiModMenu();
		}
		return modMenu;
	}

	public GuiGlideMainMenu getMainMenu() {
		if(mainMenu == null) {
			mainMenu = new GuiGlideMainMenu();
		}
		return mainMenu;
	}

	public long getLaunchTime() {
		return launchTime;
	}

	public void createFirstLoginFile() {
		Glide.getInstance().getFileManager().createFile(firstLoginFile);
	}

	public boolean isFirstLogin() {return !firstLoginFile.exists();}

	public Update getUpdateInstance(){
		return update;
	}

	public void setUpdateNeeded(boolean in) {updateNeeded = in;}
	public boolean getUpdateNeeded() {return updateNeeded;}

	public void setSoar8Released(boolean in) {soar8Released = in;}
	public boolean getSoar8Released() {return soar8Released;}

	public ClickEffects getClickEffects() {return clickEffects;}

	public BlacklistManager getBlacklistManager() { return blacklistManager; }
	public RestrictedMod getRestrictedMod() { return restrictedMod; }
	public MusicManager getMusicManager() { return musicManager; }
	public YouTubeManager getYouTubeManager() { return youTubeManager; }
}
