package me.eldodebug.soar.gui.modmenu.category.impl;

import me.eldodebug.soar.gui.modmenu.GuiModMenu;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.management.nanovg.font.Icons;

public class GhostCategory extends ModuleCategory {

	public GhostCategory(GuiModMenu parent) {
		super(parent, TranslateText.GHOST, Icons.EMOJI_SURPRISE_20, Fonts.ICON_OUTLINE, ModCategory.GHOST, false);
	}
}
