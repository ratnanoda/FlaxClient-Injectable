package me.eldodebug.soar.gui;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.management.nanovg.font.LegacyIcon;
import me.eldodebug.soar.utils.animation.normal.Animation;
import me.eldodebug.soar.utils.animation.normal.Direction;
import me.eldodebug.soar.utils.animation.normal.easing.EaseInOutCirc;
import me.eldodebug.soar.utils.animation.normal.easing.EaseLiner;
import me.eldodebug.soar.utils.buffer.ScreenAnimation;
import me.eldodebug.soar.utils.mouse.MouseUtils;
import me.eldodebug.soar.utils.render.BlurUtils;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.achievement.GuiAchievements;
import net.minecraft.client.gui.achievement.GuiStats;
import net.minecraft.client.resources.I18n;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

import java.awt.*;


public class GuiGameMenu extends GuiScreen {

    private Animation introAnimation;
    private final ScreenAnimation screenAnimation = new ScreenAnimation();
    private int x, y, width, height, centre, scaledWidth, scaledHeight;

    @Override
    public void initGui() {
        ScaledResolution sr = new ScaledResolution(mc);
        scaledWidth = sr.getScaledWidth();
        scaledHeight = sr.getScaledHeight();
        centre = scaledWidth / 2;
        x = centre - 90;
        y = (scaledHeight / 2) - 110;
        width = 180;
        height = 220;

        introAnimation = new EaseLiner(80, 1.0f);
        introAnimation.setDirection(Direction.FORWARDS);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        BlurUtils.drawBlurScreen(20);
        NanoVGManager nvg = Glide.getInstance().getNanoVGManager();
        screenAnimation.wrap(() -> drawNanoVG(nvg), x, y, width, height, 2 - introAnimation.getValueFloat(), Math.min(introAnimation.getValueFloat(), 1), false);
        if(introAnimation.isDone(Direction.BACKWARDS)) {
               this.mc.displayGuiScreen(null);
               this.mc.setIngameFocus();
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawNanoVG(NanoVGManager nvg) {
        nvg.drawRect(-5, -5, scaledWidth + 10, scaledHeight + 10,  new Color(0,0,0, 140));
        nvg.drawText(LegacyIcon.ARROW_LEFT, x, y + 5, new Color(255,255,255, 140),11, Fonts.LEGACYICON);
        nvg.drawCenteredText( I18n.format("menu.game"), centre, y + 5,  new Color(255,255,255, 200), 13, Fonts.SEMIBOLD);

        float standardPadding = 29.5f;
        float offset = 29.5F;
        drawButton(nvg, "Minecraft Options", LegacyIcon.SLIDERS, offset, false);
        offset += standardPadding;
        if(this.mc.isSingleplayer() && isSinglePlayerPrivateWorld()){
            drawButton(nvg, I18n.format("menu.shareToLan"), LegacyIcon.USERS, offset, false);
        } else {
            drawButton(nvg, TranslateText.EDIT_HUD.getText(), LegacyIcon.LAYOUT, offset, false);
        }
        offset += standardPadding;
        drawButton(nvg, I18n.format("gui.stats"), LegacyIcon.ARCHIVE, offset, false);
        offset += standardPadding;
        drawButton(nvg, I18n.format("gui.achievements"), LegacyIcon.MAP, offset, false);
        offset += standardPadding;
        drawButton(nvg, TranslateText.OPEN_MOD_MENU.getText(), LegacyIcon.SOAR, offset, true);
        offset += standardPadding;
        drawButton(nvg, !this.mc.isIntegratedServerRunning() ? I18n.format("menu.disconnect") : TranslateText.EXIT_WORLD_SINGLEPLAYER.getText(), LegacyIcon.LOGOUT, offset, false);
    }

    private void drawButton(NanoVGManager nvg, String s, String i, Float offset, boolean flaxIcon){
        nvg.drawRoundedRect(x, y + offset, width , 22, 6, new Color(230, 230, 230, 80));
        float iconWidth = flaxIcon ? 10F : nvg.getTextWidth(i, 9.5F, Fonts.LEGACYICON);
        float iconGap = 4F;
        float textWidth = nvg.getTextWidth(s, 9.5F, Fonts.MEDIUM);
        float startX = centre - ((iconWidth + iconGap + textWidth) / 2F);

        if(flaxIcon) {
            nvg.drawImage(new ResourceLocation("soar/logo.png"), startX, y + offset + 6, 10, 10);
        } else {
            nvg.drawText(i, startX, y + offset + 6.5F, Color.WHITE, 9.5F, Fonts.LEGACYICON);
        }

        nvg.drawText(s, startX + iconWidth + iconGap, y + offset + 7F, Color.WHITE, 9.5F, Fonts.MEDIUM);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!(mouseButton == 0)) {return;}
        float standardPadding = 29.5f;
        float offset = standardPadding;

        if (MouseUtils.isInside(mouseX, mouseY, x, y + offset, width, 22)){
            introAnimation.setDirection(Direction.BACKWARDS);
            this.mc.displayGuiScreen(new GuiOptions(this, this.mc.gameSettings));
        }
        offset += standardPadding;
        if (MouseUtils.isInside(mouseX, mouseY, x, y + offset, width, 22)){
            if(this.mc.isSingleplayer() && isSinglePlayerPrivateWorld()){
                this.mc.displayGuiScreen(new GuiShareToLan(this));
            } else {
                mc.displayGuiScreen(new GuiEditHUD(false));
            }
        }
        offset += standardPadding;
        if (MouseUtils.isInside(mouseX, mouseY, x, y + offset, width, 22)) {
            this.mc.displayGuiScreen(new GuiStats(this, this.mc.thePlayer.getStatFileWriter()));
        }
        offset += standardPadding;
        if (MouseUtils.isInside(mouseX, mouseY, x, y + offset, width, 22)) {
            this.mc.displayGuiScreen(new GuiAchievements(this, this.mc.thePlayer.getStatFileWriter()));
        }
        offset += standardPadding;
        if (MouseUtils.isInside(mouseX, mouseY, x, y + offset, width, 22)) {
            mc.displayGuiScreen(Glide.getInstance().getModMenu());
        }
        offset += standardPadding;
        if (MouseUtils.isInside(mouseX, mouseY, x, y + offset, width, 22)) {
            boolean flag = this.mc.isIntegratedServerRunning();
            if(this.mc.theWorld != null) {
                this.mc.theWorld.sendQuittingDisconnectingPacket();
            }
            this.mc.loadWorld(null);

            if (flag) {
                this.mc.displayGuiScreen(new GuiMainMenu());
            } else {
                this.mc.displayGuiScreen(new GuiMultiplayer(new GuiMainMenu()));
            }
        }
        if (!MouseUtils.isInside(mouseX, mouseY, x, y + standardPadding, width, offset - standardPadding + 22)) {
            introAnimation.setDirection(Direction.BACKWARDS);
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if(keyCode == Keyboard.KEY_ESCAPE) {
            introAnimation.setDirection(Direction.BACKWARDS);
        }
    }

    private boolean isSinglePlayerPrivateWorld() {
        IntegratedServer server = this.mc.getIntegratedServer();
        return server != null && !server.getPublic();
    }
}
