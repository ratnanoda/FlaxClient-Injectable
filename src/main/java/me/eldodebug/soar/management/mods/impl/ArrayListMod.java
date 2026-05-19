package me.eldodebug.soar.management.mods.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.color.AccentColor;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventRender2D;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.HUDMod;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.BooleanSetting;
import me.eldodebug.soar.management.mods.settings.impl.ComboSetting;
import me.eldodebug.soar.management.mods.settings.impl.combo.Option;
import me.eldodebug.soar.management.mods.settings.impl.ColorSetting;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.utils.ColorUtils;

public class ArrayListMod extends HUDMod {

	private BooleanSetting backgroundSetting = new BooleanSetting(TranslateText.BACKGROUND, this, true);
	
	private BooleanSetting hudSetting = new BooleanSetting(TranslateText.HUD, this, false);
	private BooleanSetting renderSetting = new BooleanSetting(TranslateText.RENDER, this, false);
	private BooleanSetting playerSetting = new BooleanSetting(TranslateText.PLAYER, this, false);
	private BooleanSetting otherSetting = new BooleanSetting(TranslateText.OTHER, this, false);
	
	private ComboSetting modeSetting = new ComboSetting(TranslateText.MODE, this, TranslateText.RIGHT, new ArrayList<Option>(Arrays.asList(
			new Option(TranslateText.RIGHT), new Option(TranslateText.LEFT))));
	
	private ComboSetting colorModeSetting = new ComboSetting(TranslateText.COLOR, this, TranslateText.SYNC, new ArrayList<Option>(Arrays.asList(
			new Option(TranslateText.SYNC), new Option(TranslateText.CUSTOM), new Option(TranslateText.RAINBOW))));
	
	private ColorSetting customColorSetting = new ColorSetting(TranslateText.CUSTOM_COLOR, this, new Color(0, 199, 255), false);
	private NumberSetting backgroundAlphaSetting = new NumberSetting(TranslateText.ALPHA, this, 100, 0, 255, true);

	public ArrayListMod() {
		super(TranslateText.ARRAY_LIST, TranslateText.ARRAY_LIST_DESCRIPTION);
	}

	@EventTarget
	public void onRender2D(EventRender2D event) {
		
		NanoVGManager nvg = Glide.getInstance().getNanoVGManager();
		
		nvg.setupAndDraw(() -> drawNanoVG());
	}
	
	private void drawNanoVG() {
		
		Glide instance = Glide.getInstance();
		AccentColor currentColor = instance.getColorManager().getCurrentColor();
		
		ArrayList<Mod> enabledMods = new ArrayList<Mod>();
		int maxWidth = 0;
		
		for(Mod m : instance.getModManager().getMods()) {
			
			if(!hudSetting.isToggled() && m.getCategory().equals(ModCategory.HUD)) {
				continue;
			}
			
			if(!renderSetting.isToggled() && m.getCategory().equals(ModCategory.RENDER)) {
				continue;
			}
			
			if(!playerSetting.isToggled() && m.getCategory().equals(ModCategory.PLAYER)) {
				continue;
			}
			
			if(!otherSetting.isToggled() && m.getCategory().equals(ModCategory.OTHER)) {
				continue;
			}
			
			if(m.isToggled() && !m.isHide()) {
				
				float nameWidth = this.getTextWidth(m.getName(), 8.5F, getHudFont(1));
				
				enabledMods.add(m);
				
				if(maxWidth < nameWidth) {
					maxWidth = (int) nameWidth;
				}
			}
		}
		
		enabledMods.sort((m1, m2) -> (int) this.getTextWidth(m2.getName(), 8.5F, getHudFont(1)) - (int) this.getTextWidth(m1.getName(), 8.5F, getHudFont(1)));
		
		int y = 0;
		int colorIndex = 0;
		boolean isRight = modeSetting.getOption().getTranslate().equals(TranslateText.RIGHT);
		
		for(Mod m : enabledMods) {
			
			float nameWidth = this.getTextWidth(m.getName(), 8.5F, getHudFont(1));
			Color color;
			TranslateText colorMode = colorModeSetting.getOption().getTranslate();
			
			if (colorMode == TranslateText.SYNC) {
				color = currentColor.getInterpolateColor(colorIndex);
			} else if (colorMode == TranslateText.RAINBOW) {
				color = ColorUtils.getRainbow(colorIndex, 4.0, 255);
			} else {
				color = customColorSetting.getColor();
			}
			
			if(backgroundSetting.isToggled()) {
				this.drawRect((isRight ? (maxWidth - nameWidth) : 0), y, nameWidth + 5, 12, new Color(0, 0, 0, backgroundAlphaSetting.getValueInt()));
			}
			
			this.drawText(m.getName(), 3 + (isRight ? (maxWidth - nameWidth) : 0), 
					y + 2.5F, 8.5F, getHudFont(1), color);
			
			y += 12;
			colorIndex-=10;
		}
		
		this.setWidth(maxWidth + 4);
		this.setHeight(y);
	}
}
