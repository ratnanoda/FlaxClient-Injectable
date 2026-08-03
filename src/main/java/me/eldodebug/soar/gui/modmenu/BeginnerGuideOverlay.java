package me.eldodebug.soar.gui.modmenu;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import org.lwjgl.input.Keyboard;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.logger.GlideLogger;
import me.eldodebug.soar.management.color.AccentColor;
import me.eldodebug.soar.management.color.ColorManager;
import me.eldodebug.soar.management.color.palette.ColorPalette;
import me.eldodebug.soar.management.color.palette.ColorType;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.utils.ColorUtils;
import me.eldodebug.soar.utils.mouse.MouseUtils;

/**
 * First-run help entry point and modal guide for the floating mod menu.
 * All copy is intentionally kept in English so distribution builds have one
 * consistent onboarding experience regardless of the active language pack.
 */
public final class BeginnerGuideOverlay {

    private static final String[] PAGE_TITLES = {
            "Welcome to FlaxClient",
            "Finding Your Way Around",
            "YouTube: Video and Music",
            "Useful Tips"
    };

    private static final String[] PAGE_BODIES = {
            "FlaxClient is now attached to Minecraft. Use the sidebar to move between pages and press Escape to close the menu.\n\n"
                    + "Start with the Ghost page to review gameplay modules, then open Settings from the Flax logo at the top of the sidebar. Changes are saved automatically when you close the menu.",
            "1. Choose a page from the floating sidebar.\n\n"
                    + "2. Click a module or control to enable it and open its options.\n\n"
                    + "3. Use the search field when a page contains many items.\n\n"
                    + "4. Select the layout icon at the bottom of the sidebar to move and resize HUD elements.",
            "Paste a supported YouTube URL into the YouTube page and select Download. FlaxClient includes yt-dlp and FFmpeg, so no separate tools are required.\n\n"
                    + "Video mode uses the normal picture-in-picture player. Music mode plays the same downloaded media as audio and shows the compact player in the bottom-right corner. You can switch modes before or during playback.",
            "- Wait for the Minecraft or Lunar Client 1.8.9 main menu before injecting.\n\n"
                    + "- Run the injector only once for each game launch.\n\n"
                    + "- Large downloads can take a little time. Keep the game open until the item is ready.\n\n"
                    + "- Press Shift + Delete to stop Music playback quickly.\n\n"
                    + "- If something does not load, restart the game and inject again after the main menu appears."
    };

    private final GuiModMenu parent;
    private boolean open;
    private boolean firstRun;
    private boolean previousCanClose = true;
    private int page;

    public BeginnerGuideOverlay(GuiModMenu parent) {
        this.parent = parent;
    }

    public void initGui() {
        open = false;
        page = 0;
        firstRun = !markerFile().isFile();
    }

    public boolean isOpen() {
        return open;
    }

    public void draw(int mouseX, int mouseY, float partialTicks) {
        Glide instance = Glide.getInstance();
        NanoVGManager nvg = instance.getNanoVGManager();
        ColorManager colors = instance.getColorManager();
        ColorPalette palette = colors.getPalette();
        AccentColor accent = colors.getCurrentColor();

        drawGuideButton(nvg, palette, accent, mouseX, mouseY);
        if(open) {
            drawModal(nvg, palette, accent, mouseX, mouseY);
        }
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if(mouseButton != 0) {
            return open;
        }

        if(!open && MouseUtils.isInside(mouseX, mouseY,
                buttonX(), buttonY(), buttonWidth(), buttonHeight())) {
            openGuide();
            return true;
        }

        if(!open) {
            return false;
        }

        if(MouseUtils.isInside(mouseX, mouseY, modalX() + modalWidth() - 31, modalY() + 10, 20, 20)) {
            closeGuide();
            return true;
        }

        if(page > 0 && MouseUtils.isInside(mouseX, mouseY,
                modalX() + 18, modalY() + modalHeight() - 39, 78, 24)) {
            page--;
            return true;
        }

        if(MouseUtils.isInside(mouseX, mouseY,
                modalX() + modalWidth() - 110, modalY() + modalHeight() - 39, 92, 24)) {
            if(page + 1 < PAGE_TITLES.length) {
                page++;
            } else {
                closeGuide();
            }
            return true;
        }

        // The modal consumes all clicks so controls behind it cannot change.
        return true;
    }

    public boolean keyTyped(char typedChar, int keyCode) {
        if(!open) {
            return false;
        }
        if(keyCode == Keyboard.KEY_ESCAPE) {
            closeGuide();
        } else if(keyCode == Keyboard.KEY_LEFT && page > 0) {
            page--;
        } else if(keyCode == Keyboard.KEY_RIGHT && page + 1 < PAGE_TITLES.length) {
            page++;
        }
        return true;
    }

    private void openGuide() {
        previousCanClose = parent.isCanClose();
        parent.setCanClose(false);
        open = true;
        page = 0;
        markViewed();
    }

    private void closeGuide() {
        open = false;
        parent.setCanClose(previousCanClose);
    }

    private void drawGuideButton(NanoVGManager nvg, ColorPalette palette, AccentColor accent,
            int mouseX, int mouseY) {
        float x = buttonX();
        float y = buttonY();
        float width = buttonWidth();
        float height = buttonHeight();
        boolean hovered = MouseUtils.isInside(mouseX, mouseY, x, y, width, height);

        nvg.drawRoundedGlow(x, y, width, height, 8,
                ColorUtils.applyAlpha(accent.getColor1(), hovered ? 105 : 65), hovered ? 6 : 4);
        nvg.drawGradientRoundedRect(x, y, width, height, 8,
                ColorUtils.applyAlpha(accent.getColor1(), hovered ? 220 : 178),
                ColorUtils.applyAlpha(accent.getColor2(), hovered ? 220 : 178));
        nvg.drawOutlineRoundedRect(x + 0.5F, y + 0.5F, width - 1, height - 1, 8,
                0.8F, new Color(255, 255, 255, hovered ? 86 : 54));
        nvg.drawCenteredText("Open Beginner's Guide", x + width / 2.0F, y + 6.2F,
                Color.WHITE, 10.5F, Fonts.SEMIBOLD);

        if(firstRun) {
            float pillWidth = 27;
            nvg.drawRoundedRect(x + width - pillWidth - 5, y - 5, pillWidth, 12, 6,
                    new Color(255, 255, 255, 235));
            nvg.drawCenteredText("NEW", x + width - pillWidth / 2.0F - 5, y - 2.2F,
                    new Color(31, 35, 52), 6.5F, Fonts.SEMIBOLD);
        }
    }

    private void drawModal(NanoVGManager nvg, ColorPalette palette, AccentColor accent,
            int mouseX, int mouseY) {
        float x = modalX();
        float y = modalY();
        float width = modalWidth();
        float height = modalHeight();

        nvg.drawRect(0, 0, parent.getScaledWidth(), parent.getScaledHeight(),
                new Color(4, 7, 14, 178));
        nvg.drawShadow(x, y, width, height, 16, 10);
        Color panelBase = palette.getBackgroundColor(ColorType.DARK);
        nvg.drawRoundedRect(x, y, width, height, 16,
                new Color(panelBase.getRed(), panelBase.getGreen(), panelBase.getBlue(), 246));
        nvg.drawOutlineRoundedRect(x + 0.5F, y + 0.5F, width - 1, height - 1, 16,
                0.9F, new Color(255, 255, 255, 58));
        nvg.drawGradientRoundedRect(x, y, width, 4, 4,
                ColorUtils.applyAlpha(accent.getColor1(), 235),
                ColorUtils.applyAlpha(accent.getColor2(), 235));

        nvg.drawText("FlaxClient Beginner's Guide", x + 20, y + 18,
                palette.getFontColor(ColorType.DARK), 15, Fonts.SEMIBOLD);
        nvg.drawText((page + 1) + " / " + PAGE_TITLES.length,
                x + width - 73, y + 20, palette.getFontColor(ColorType.NORMAL, 180),
                9, Fonts.MEDIUM);

        boolean closeHovered = MouseUtils.isInside(mouseX, mouseY, x + width - 31, y + 10, 20, 20);
        nvg.drawRoundedRect(x + width - 31, y + 10, 20, 20, 7,
                new Color(255, 255, 255, closeHovered ? 35 : 18));
        nvg.drawCenteredText("x", x + width - 21, y + 14.6F,
                palette.getFontColor(ColorType.NORMAL), 10, Fonts.SEMIBOLD);

        nvg.drawText(PAGE_TITLES[page], x + 24, y + 57,
                ColorUtils.applyAlpha(accent.getColor1(), 255), 13, Fonts.SEMIBOLD);
        nvg.drawTextBox(PAGE_BODIES[page], x + 24, y + 82, width - 48,
                palette.getFontColor(ColorType.DARK, 230), 10.5F, Fonts.REGULAR);

        if(page > 0) {
            drawModalButton(nvg, palette, accent, "Back", x + 18, y + height - 39,
                    78, 24, MouseUtils.isInside(mouseX, mouseY, x + 18, y + height - 39, 78, 24), false);
        }

        String nextText = page + 1 < PAGE_TITLES.length ? "Next" : "Finish";
        drawModalButton(nvg, palette, accent, nextText, x + width - 110, y + height - 39,
                92, 24, MouseUtils.isInside(mouseX, mouseY, x + width - 110, y + height - 39, 92, 24), true);

        nvg.drawCenteredText("Use Left / Right Arrow to change pages. Escape closes the guide.",
                x + width / 2.0F, y + height - 55,
                palette.getFontColor(ColorType.NORMAL, 145), 7.5F, Fonts.REGULAR);
    }

    private void drawModalButton(NanoVGManager nvg, ColorPalette palette, AccentColor accent,
            String label, float x, float y, float width, float height, boolean hovered, boolean primary) {
        if(primary) {
            nvg.drawGradientRoundedRect(x, y, width, height, 7,
                    ColorUtils.applyAlpha(accent.getColor1(), hovered ? 230 : 190),
                    ColorUtils.applyAlpha(accent.getColor2(), hovered ? 230 : 190));
        } else {
            nvg.drawRoundedRect(x, y, width, height, 7,
                    new Color(255, 255, 255, hovered ? 35 : 20));
            nvg.drawOutlineRoundedRect(x + 0.5F, y + 0.5F, width - 1, height - 1, 7,
                    0.7F, new Color(255, 255, 255, 42));
        }
        nvg.drawCenteredText(label, x + width / 2.0F, y + 6.3F,
                primary ? Color.WHITE : palette.getFontColor(ColorType.NORMAL),
                9.5F, Fonts.SEMIBOLD);
    }

    private File markerFile() {
        return new File(Glide.getInstance().getFileManager().getCacheDir(), "beginner-guide-viewed-v1.dat");
    }

    private void markViewed() {
        if(!firstRun) {
            return;
        }
        File marker = markerFile();
        try {
            File parentDir = marker.getParentFile();
            if(parentDir != null && !parentDir.isDirectory()) {
                parentDir.mkdirs();
            }
            if(marker.createNewFile() || marker.isFile()) {
                firstRun = false;
            }
        } catch(IOException exception) {
            GlideLogger.warn("Could not save the beginner guide viewed state: " + exception.getMessage());
        }
    }

    private float buttonWidth() {
        return 168;
    }

    private float buttonHeight() {
        return 23;
    }

    private float buttonX() {
        float contentX = parent.getX() + 32;
        float contentWidth = parent.getWidth() - 32;
        return contentX + (contentWidth - buttonWidth()) / 2.0F;
    }

    private float buttonY() {
        return Math.min(
                parent.getY() + parent.getHeight() + 8,
                parent.getScaledHeight() - buttonHeight() - 4);
    }

    private float modalWidth() {
        return Math.min(560, parent.getScaledWidth() - 36);
    }

    private float modalHeight() {
        return Math.min(350, parent.getScaledHeight() - 36);
    }

    private float modalX() {
        return (parent.getScaledWidth() - modalWidth()) / 2.0F;
    }

    private float modalY() {
        return (parent.getScaledHeight() - modalHeight()) / 2.0F;
    }
}
