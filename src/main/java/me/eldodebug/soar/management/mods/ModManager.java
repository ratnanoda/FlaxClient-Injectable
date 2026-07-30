package me.eldodebug.soar.management.mods;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.LogManager;

import me.eldodebug.soar.management.mods.impl.*;
import me.eldodebug.soar.management.mods.settings.Setting;
import me.eldodebug.soar.utils.Sound;

public class ModManager {

	private ArrayList<Mod> mods = new ArrayList<Mod>();
	private ArrayList<Setting> settings = new ArrayList<Setting>();
	
	public void init() {
		// Hidden runtime settings required by the menu, sounds and RSHIFT binding.
		// It is never exposed as a user module.
		mods.add(new InternalSettingsMod());
		mods.add(new AimAssistMod());
		mods.add(new AutoClickerMod());
		mods.add(new BedESPMod());
		mods.add(new BreakProgressMod());
		mods.add(new ESPMod());
		mods.add(new FastPlaceMod());
		mods.add(new GhostFreelookMod());
		mods.add(new GhostNametagsMod());
		mods.add(new HealthbarMod());
		mods.add(new JumpResetMod());
		mods.add(new SafeWalkMod());
		YouTubePipMod youtubePip = new YouTubePipMod();
		mods.add(youtubePip);
	}
	
	public ArrayList<Mod> getMods() {
		return mods;
	}
	
	public Mod getModByTranslateKey(String key) {
		
		for(Mod m : mods) {
			if(m.getNameKey().equals(key)) {
				return m;
			}
		}
		
		return null;
	}
	
	public ArrayList<HUDMod> getHudMods(){
		
		ArrayList<HUDMod> result = new ArrayList<HUDMod>();
		
		for(Mod m : mods) {
			if(m instanceof HUDMod && ((HUDMod) m).isDraggable()) {
				result.add((HUDMod) m);
			}
		}
		
		return result;
	}

	public ArrayList<Setting> getSettings() {
		return settings;
	}
	
	public ArrayList<Setting> getSettingsByMod(Mod m){
		
		ArrayList<Setting> result = new ArrayList<Setting>();
		
		for(Setting s : settings) {
			if(s.getParent().equals(m)) {
				result.add(s);
			}
		}
		
		if(result.isEmpty()) {
			return null;
		}
		
		return result;
	}
	
	public String getWords(Mod mod) {
		
		String result = "";
		
		for(Mod m : mods) {
			if(m.equals(mod)) {
				result = result + m.getName() + " ";
			}
		}
		
		for(Setting s : settings) {
			if(s.getParent().equals(mod)) {
				result = result + s.getName() + " ";
			}
		}

		for(Mod m : mods) {
			if(m.equals(mod) && !Objects.equals(m.getAlias(), "\u200B")) {
				result = result + m.getAlias() + " ";
			}
		}
		
		return result;
	}
	
	public void addSettings(Setting... settingsList) {
		settings.addAll(Arrays.asList(settingsList));
	}
	
	public void disableAll() {
		for(Mod m : mods) {
			m.setToggled(false);
		}
		InternalSettingsMod.getInstance().setToggled(true);
	}

	public void playToggleSound(boolean toggled){
		if(toggled){
			Sound.play("soar/audio/positive.wav", true);
		} else {
			Sound.play("soar/audio/negative.wav", true);
		}

	}

}
