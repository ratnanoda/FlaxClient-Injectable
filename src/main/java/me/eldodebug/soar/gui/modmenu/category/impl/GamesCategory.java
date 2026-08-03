package me.eldodebug.soar.gui.modmenu.category.impl;

import java.util.ArrayList;

import org.lwjgl.input.Keyboard;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.gui.modmenu.GuiModMenu;
import me.eldodebug.soar.gui.modmenu.category.Category;
import me.eldodebug.soar.gui.modmenu.category.impl.game.GameScene;
import me.eldodebug.soar.gui.modmenu.category.impl.game.impl.BirdScene;
import me.eldodebug.soar.gui.modmenu.category.impl.game.impl.SnakeScene;
import me.eldodebug.soar.gui.modmenu.category.impl.game.impl.TetrisScene;
import me.eldodebug.soar.management.color.palette.ColorPalette;
import me.eldodebug.soar.management.color.palette.ColorType;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.management.nanovg.font.LegacyIcon;
import me.eldodebug.soar.utils.animation.normal.Animation;
import me.eldodebug.soar.utils.animation.normal.Direction;
import me.eldodebug.soar.utils.animation.normal.other.SmoothStepAnimation;
import me.eldodebug.soar.utils.mouse.MouseUtils;

public class GamesCategory extends Category {

    private Animation sceneAnimation;
    private final ArrayList<GameScene> scenes = new ArrayList<GameScene>();
    private GameScene currentScene;

    public GamesCategory(GuiModMenu parent) {
        super(parent, TranslateText.SETTINGS, LegacyIcon.GAME, false, false);
        scenes.add(new BirdScene(this));
        scenes.add(new TetrisScene(this));
        scenes.add(new SnakeScene(this));
    }

    @Override
    public void initGui() {
        sceneAnimation = new SmoothStepAnimation(260, 1.0);
        sceneAnimation.setValue(1.0);
        for(GameScene scene : scenes) {
            scene.initGui();
        }
    }

    @Override
    public void initCategory() {
        scroll.resetAll();
        sceneAnimation = new SmoothStepAnimation(260, 1.0);
        sceneAnimation.setValue(1.0);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Glide glide = Glide.getInstance();
        NanoVGManager nvg = glide.getNanoVGManager();
        ColorPalette palette = glide.getColorManager().getPalette();
        float offsetY = 15.0F;

        if(sceneAnimation.isDone(Direction.FORWARDS)) {
            setCanClose(true);
            currentScene = null;
        }

        float slideDistance = getWidth() + 80.0F;
        nvg.save();
        nvg.translate((float)-(slideDistance - sceneAnimation.getValue() * slideDistance), 0.0F);
        for(GameScene scene : scenes) {
            float cardX = getX() + 15.0F;
            float cardY = getY() + offsetY;
            float cardWidth = getWidth() - 30.0F;
            boolean hovered = currentScene == null
                    && MouseUtils.isInside(mouseX, mouseY, cardX, cardY, cardWidth, 40.0F);
            nvg.drawRoundedRect(cardX, cardY, cardWidth, 40.0F, 8.0F,
                    palette.getBackgroundColor(hovered ? ColorType.NORMAL : ColorType.DARK));
            nvg.drawOutlineRoundedRect(cardX + 0.5F, cardY + 0.5F,
                    cardWidth - 1.0F, 39.0F, 8.0F, 0.6F,
                    new java.awt.Color(255, 255, 255, hovered ? 42 : 22));
            nvg.drawText(scene.getIcon(), cardX + 11.0F, cardY + 13.0F,
                    palette.getFontColor(ColorType.DARK), 14.0F, Fonts.LEGACYICON);
            nvg.drawText(scene.getName(), cardX + 32.0F, cardY + 9.0F,
                    palette.getFontColor(ColorType.DARK), 12.5F, Fonts.MEDIUM);
            nvg.drawText(scene.getDescription(), cardX + 32.0F, cardY + 23.0F,
                    palette.getFontColor(ColorType.NORMAL), 7.5F, Fonts.REGULAR);
            nvg.drawText(LegacyIcon.CHEVRON_RIGHT,
                    cardX + cardWidth - 17.0F, cardY + 15.0F,
                    palette.getFontColor(ColorType.NORMAL), 10.0F, Fonts.LEGACYICON);
            offsetY += 50.0F;
        }
        nvg.restore();

        nvg.save();
        nvg.translate((float)(sceneAnimation.getValue() * slideDistance), 0.0F);
        if(currentScene != null) {
            currentScene.drawScreen(mouseX, mouseY, partialTicks);
        }
        nvg.restore();
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        float offsetY = 15.0F;
        for(GameScene scene : scenes) {
            if(currentScene == null && mouseButton == 0
                    && MouseUtils.isInside(mouseX, mouseY, getX() + 15.0F,
                            getY() + offsetY, getWidth() - 30.0F, 40.0F)) {
                currentScene = scene;
                setCanClose(false);
                sceneAnimation.setDirection(Direction.BACKWARDS);
                return;
            }
            offsetY += 50.0F;
        }

        if(currentScene != null && sceneAnimation.isDone(Direction.BACKWARDS)) {
            currentScene.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if(currentScene != null && mouseButton == 3) {
            sceneAnimation.setDirection(Direction.FORWARDS);
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        if(currentScene != null && sceneAnimation.isDone(Direction.BACKWARDS)) {
            currentScene.mouseReleased(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if(currentScene != null && keyCode == Keyboard.KEY_ESCAPE) {
            sceneAnimation.setDirection(Direction.FORWARDS);
            return;
        }
        if(currentScene != null && sceneAnimation.isDone(Direction.BACKWARDS)) {
            currentScene.keyTyped(typedChar, keyCode);
        }
    }

    public int getSceneX() {
        return getX() + 15;
    }

    public int getSceneY() {
        return getY() + 15;
    }

    public int getSceneWidth() {
        return getWidth() - 30;
    }

    public int getSceneHeight() {
        return getHeight() - 30;
    }
}
