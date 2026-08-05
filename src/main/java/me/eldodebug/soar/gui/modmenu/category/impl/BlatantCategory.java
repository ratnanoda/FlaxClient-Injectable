package me.eldodebug.soar.gui.modmenu.category.impl;

import java.lang.reflect.Field;
import java.util.ArrayList;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.gui.modmenu.GuiModMenu;
import me.eldodebug.soar.gui.modmenu.category.Category;
import me.eldodebug.soar.logger.GlideLogger;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.management.nanovg.font.LegacyIcon;

/**
 * Dedicated module workspace for modules that intentionally automate gameplay.
 *
 * It inherits ModuleCategory's floating window implementation, so the list can
 * be dragged, resized from every edge, scrolled, searched, and used to open the
 * same settings/keybind UI as the Ghost module list.
 */
public final class BlatantCategory extends ModuleCategory {

    private static final String NAME = "Blatant";
    private static final String NAME_KEY = "text.blatant";

    public BlatantCategory(GuiModMenu parent) {
        super(parent, TranslateText.MODULE, LegacyIcon.ALERT_TRIANGLE, Fonts.LEGACYICON,
                ModCategory.BLATANT, false);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getNameKey() {
        return NAME_KEY;
    }

    /**
     * GuiModMenu predates extensible navigation registration. Install the new
     * page once while modules are being created, before the menu is opened.
     */
    @SuppressWarnings("unchecked")
    public static void install() {
        GuiModMenu menu = Glide.getInstance().getModMenu();
        ArrayList<Category> categories = menu.getCategories();

        for(Category category : categories) {
            if(category instanceof BlatantCategory) {
                return;
            }
        }

        BlatantCategory blatant = new BlatantCategory(menu);
        int categoryIndex = indexAfterGhost(categories);
        categories.add(categoryIndex, blatant);

        try {
            Field navigationField = GuiModMenu.class.getDeclaredField("navigationCategories");
            navigationField.setAccessible(true);
            ArrayList<Category> navigation = (ArrayList<Category>) navigationField.get(menu);
            navigation.add(indexAfterGhost(navigation), blatant);
        } catch(Exception exception) {
            categories.remove(blatant);
            GlideLogger.error("Failed to register the Blatant module category", exception);
        }
    }

    private static int indexAfterGhost(ArrayList<Category> categories) {
        for(int index = 0; index < categories.size(); index++) {
            if(categories.get(index) instanceof GhostCategory) {
                return index + 1;
            }
        }
        return categories.size();
    }
}
