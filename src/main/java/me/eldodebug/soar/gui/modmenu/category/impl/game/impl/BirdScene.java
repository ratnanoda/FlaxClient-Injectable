package me.eldodebug.soar.gui.modmenu.category.impl.game.impl;

import java.awt.Color;
import java.util.Random;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.gui.modmenu.category.impl.GamesCategory;
import me.eldodebug.soar.gui.modmenu.category.impl.game.GameScene;
import me.eldodebug.soar.management.color.AccentColor;
import me.eldodebug.soar.management.color.palette.ColorPalette;
import me.eldodebug.soar.management.color.palette.ColorType;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;

public class BirdScene extends GameScene {
    private static final Random RANDOM = new Random();
    private static final int LAVA_HEIGHT = 18;
    private static float deltaTime;

    private int x, y, width, height, score;
    private boolean gameStarted, shouldStart, inPipeOne, inPipeTwo, isPlayerDead;
    private final float gravity = 300.0F;
    private final int pipeHoleHeight = 82, pipeWidth = 34;
    private float pipeOneX, pipeTwoX, pipeOneYGap, pipeTwoYGap, pipeSpeed;
    private final float playerWidth = 18.0F, playerHeight = 28.0F;
    private float playerTargetYPosition, playerActualYPosition, playerX;
    private long deathTime;

    public BirdScene(GamesCategory parent) {
        super(parent, "Flappy Glide", "Fly as Steve through Minecraft blocks", ">");
    }

    @Override
    public void initGui() {
        x = getX();
        y = getY();
        width = getWidth();
        height = getHeight();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        DeltaTime.getInstance().update();
        deltaTime = DeltaTime.getInstance().getDeltaTime();
        Glide glide = Glide.getInstance();
        NanoVGManager nvg = glide.getNanoVGManager();
        ColorPalette palette = glide.getColorManager().getPalette();
        AccentColor accent = glide.getColorManager().getCurrentColor();

        nvg.save();
        nvg.scissor(x, y, width, height);
        drawSky(nvg);
        drawClouds(nvg);
        if (gameStarted) {
            updatePlayer();
            updatePipes();
        }
        drawPipes(nvg);
        drawLava(nvg);
        if (gameStarted) {
            drawSteve(nvg, accent);
            nvg.drawText(Integer.toString(score), x + 10.0F, y + 10.0F,
                    Color.WHITE, 9.0F, Fonts.SEMIBOLD);
            if (!isPlayerDead) {
                detectCollisions();
            }
        } else if (isPlayerDead) {
            drawDeathScreen(nvg, palette);
            if (shouldStart) startGame();
        } else {
            nvg.drawCenteredText("Steve Glide", x + width / 2.0F, y + height / 2.0F - 20.0F,
                    palette.getFontColor(ColorType.NORMAL), 15.0F, Fonts.SEMIBOLD);
            nvg.drawCenteredText("Press SPACE or CLICK to start!", x + width / 2.0F,
                    y + height / 2.0F, palette.getFontColor(ColorType.DARK), 8.0F, Fonts.MEDIUM);
            if (shouldStart) startGame();
        }
        nvg.restore();
        nvg.drawOutlineRoundedRect(x, y, width, height, 10.0F, 8.0F,
                palette.getBackgroundColor(ColorType.NORMAL));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        jump();
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 17 || keyCode == 28 || keyCode == 57 || keyCode == 200) jump();
    }

    public float getNewPipeHeight() {
        float min = 18.0F;
        float max = Math.max(min + 10.0F, height - pipeHoleHeight - LAVA_HEIGHT - 26.0F);
        return min + RANDOM.nextFloat() * (max - min);
    }

    private void updatePlayer() {
        if (isPlayerDead) return;
        float maxY = height - LAVA_HEIGHT - playerHeight / 2.0F - 2.0F;
        playerTargetYPosition = Math.min(maxY, playerTargetYPosition + gravity * deltaTime);
        playerActualYPosition = anim(playerActualYPosition, playerTargetYPosition, 10.0F, deltaTime);
    }

    private void updatePipes() {
        if (pipeOneX < x - pipeWidth) {
            pipeOneX = x + width + pipeWidth;
            pipeOneYGap = getNewPipeHeight();
            inPipeOne = false;
        }
        if (pipeTwoX < x - pipeWidth) {
            pipeTwoX = x + width + pipeWidth;
            pipeTwoYGap = getNewPipeHeight();
            inPipeTwo = false;
        }
        if (!isPlayerDead) {
            pipeOneX -= pipeSpeed * deltaTime;
            pipeTwoX -= pipeSpeed * deltaTime;
        }
    }

    private void detectCollisions() {
        float left = playerX - playerWidth / 2.0F;
        float right = playerX + playerWidth / 2.0F;
        float top = y + playerActualYPosition - playerHeight / 2.0F;
        float bottom = y + playerActualYPosition + playerHeight / 2.0F;
        checkPipe(pipeOneX, pipeOneYGap, true, left, right, top, bottom);
        checkPipe(pipeTwoX, pipeTwoYGap, false, left, right, top, bottom);
        if (top <= y || bottom >= y + height - LAVA_HEIGHT) die();
    }

    private void checkPipe(float pipeX, float gap, boolean first,
                           float left, float right, float top, float bottom) {
        boolean overlaps = pipeX <= right && pipeX + pipeWidth >= left;
        if (overlaps) {
            if (top < y + gap || bottom > y + gap + pipeHoleHeight) die();
            if (first) inPipeOne = true; else inPipeTwo = true;
        } else if (first && inPipeOne) {
            score++;
            inPipeOne = false;
        } else if (!first && inPipeTwo) {
            score++;
            inPipeTwo = false;
        }
    }

    private void jump() {
        if (!gameStarted) {
            if (!isPlayerDead || System.currentTimeMillis() - deathTime >= 400L) shouldStart = true;
        }
        playerTargetYPosition -= gravity / 3.0F;
    }

    private void die() {
        if (isPlayerDead) return;
        deathTime = System.currentTimeMillis();
        isPlayerDead = true;
        gameStarted = false;
    }

    private void drawDeathScreen(NanoVGManager nvg, ColorPalette palette) {
        nvg.drawCenteredText("You Died!", x + width / 2.0F, y + height / 2.0F - 22.0F,
                new Color(255, 84, 54), 15.0F, Fonts.SEMIBOLD);
        nvg.drawCenteredText("Your score is " + score, x + width / 2.0F, y + height / 2.0F - 2.0F,
                palette.getFontColor(ColorType.DARK), 8.0F, Fonts.MEDIUM);
        if (System.currentTimeMillis() - deathTime >= 400L) {
            nvg.drawCenteredText("Press SPACE or CLICK to start!", x + width / 2.0F,
                    y + height - 20.0F, palette.getFontColor(ColorType.DARK), 8.0F, Fonts.MEDIUM);
        }
    }

    private void drawSky(NanoVGManager nvg) {
        nvg.drawVerticalGradientRect(x, y, width, height,
                new Color(104, 181, 255), new Color(184, 226, 255));
    }

    private void drawClouds(NanoVGManager nvg) {
        float t = (System.currentTimeMillis() % 40000L) / 40000.0F;
        float travel = width + 80.0F;
        drawCloud(nvg, x + width - t * travel, y + 18.0F, 1.0F);
        drawCloud(nvg, x + width - ((t + 0.38F) % 1.0F) * travel, y + 48.0F, 0.85F);
        drawCloud(nvg, x + width - ((t + 0.70F) % 1.0F) * travel, y + 78.0F, 1.1F);
    }

    private void drawCloud(NanoVGManager nvg, float cx, float cy, float scale) {
        float s = 6.0F * scale;
        int[][] blocks = {{1,0},{2,0},{3,0},{0,1},{1,1},{2,1},{3,1},{4,1},{1,2},{2,2},{3,2}};
        for (int[] block : blocks) {
            float bx = cx + block[0] * s;
            float by = cy + block[1] * s;
            nvg.drawRect(bx, by, s, s, new Color(255, 255, 255, 225));
            nvg.drawRect(bx, by + s - 1.0F, s, 1.0F, new Color(220, 229, 239, 225));
        }
    }

    private void drawPipes(NanoVGManager nvg) {
        drawBlockColumn(nvg, pipeOneX, y, pipeWidth, pipeOneYGap, true);
        drawBlockColumn(nvg, pipeOneX, y + pipeOneYGap + pipeHoleHeight, pipeWidth,
                height - pipeOneYGap - pipeHoleHeight - LAVA_HEIGHT, false);
        drawBlockColumn(nvg, pipeTwoX, y, pipeWidth, pipeTwoYGap, true);
        drawBlockColumn(nvg, pipeTwoX, y + pipeTwoYGap + pipeHoleHeight, pipeWidth,
                height - pipeTwoYGap - pipeHoleHeight - LAVA_HEIGHT, false);
    }

    private void drawBlockColumn(NanoVGManager nvg, float px, float py, float w, float h, boolean top) {
        if (h <= 0.0F) return;
        nvg.drawRect(px, py, w, h, new Color(70, 44, 26));
        float cell = 6.0F;
        for (float yy = 1.0F; yy < h - 1.0F; yy += cell) {
            for (float xx = 1.0F; xx < w - 1.0F; xx += cell) {
                float cw = Math.min(cell, w - 1.0F - xx);
                float ch = Math.min(cell, h - 1.0F - yy);
                nvg.drawRect(px + xx, py + yy, cw, ch, new Color(126, 83, 51));
                nvg.drawRect(px + xx, py + yy, cw, 1.0F, new Color(153, 105, 67));
                nvg.drawRect(px + xx, py + yy + ch - 1.0F, cw, 1.0F, new Color(91, 59, 37));
                boolean grass = top ? yy >= h - cell - 1.0F : yy <= cell + 1.0F;
                if (grass) {
                    nvg.drawRect(px + xx, py + yy, cw, 2.0F, new Color(101, 174, 58));
                    nvg.drawRect(px + xx + 1.0F, py + yy, Math.max(1.0F, cw - 2.0F), 1.0F,
                            new Color(132, 197, 76));
                }
            }
        }
        if (top) {
            nvg.drawRect(px - 2.0F, py + h - 5.0F, w + 4.0F, 5.0F, new Color(70, 135, 40));
            nvg.drawRect(px - 2.0F, py + h - 5.0F, w + 4.0F, 2.0F, new Color(130, 195, 74));
        } else {
            nvg.drawRect(px - 2.0F, py, w + 4.0F, 5.0F, new Color(70, 135, 40));
            nvg.drawRect(px - 2.0F, py, w + 4.0F, 2.0F, new Color(130, 195, 74));
        }
    }

    private void drawLava(NanoVGManager nvg) {
        float ly = y + height - LAVA_HEIGHT;
        nvg.drawRect(x, ly, width, LAVA_HEIGHT, new Color(116, 25, 8));
        nvg.drawRect(x, ly, width, 2.0F, new Color(255, 208, 64));
        float phase = (System.currentTimeMillis() % 1200L) / 1200.0F;
        for (int i = 0; i < width / 10 + 2; i++) {
            float bx = x + i * 10.0F - phase * 10.0F;
            nvg.drawRect(bx, ly + 2.0F, 8.0F, 6.0F, new Color(255, 101, 25));
            nvg.drawRect(bx + 1.0F, ly + 3.0F, 4.0F, 2.0F, new Color(255, 220, 77));
            nvg.drawRect(bx + 3.0F, ly + 8.0F, 6.0F, 5.0F, new Color(194, 45, 14));
        }
    }

    private void drawSteve(NanoVGManager nvg, AccentColor accent) {
        float px = playerX - playerWidth / 2.0F;
        float py = y + playerActualYPosition - playerHeight / 2.0F;
        float unit = playerHeight / 20.0F;
        float swing = (float)Math.sin(System.currentTimeMillis() / 95.0D) * 1.35F;
        py += (float)Math.sin(System.currentTimeMillis() / 120.0D) * 0.65F;
        Color skin = new Color(214, 171, 133), hair = new Color(91, 58, 39);
        Color shirt = new Color(75, 158, 169), shirtDark = new Color(55, 128, 140);
        Color pants = new Color(62, 75, 177), pantsDark = new Color(45, 56, 140);
        nvg.drawRoundedRect(px - 1.0F, py - 1.0F, playerWidth + 2.0F, playerHeight + 2.0F,
                2.0F, new Color(30, 30, 30, 110));
        nvg.drawRect(px + 3*unit, py, 8*unit, 8*unit, skin);
        nvg.drawRect(px + 3*unit, py, 8*unit, 2*unit, hair);
        nvg.drawRect(px + 3*unit, py + 2*unit, unit, 3*unit, hair);
        nvg.drawRect(px + 10*unit, py + 2*unit, unit, 3*unit, hair);
        nvg.drawRect(px + 5*unit, py + 4*unit, unit, unit, new Color(40, 72, 130));
        nvg.drawRect(px + 8*unit, py + 4*unit, unit, unit, new Color(40, 72, 130));
        nvg.drawRect(px + 4*unit, py + 8*unit, 6*unit, 6*unit, shirt);
        nvg.drawRect(px + 4*unit, py + 13*unit, 6*unit, unit, shirtDark);
        nvg.drawRect(px + 2*unit, py + 8*unit + swing, 2*unit, 6*unit, skin);
        nvg.drawRect(px + 2*unit, py + 8*unit + swing, 2*unit, 4*unit, shirtDark);
        nvg.drawRect(px + 10*unit, py + 8*unit - swing, 2*unit, 6*unit, skin);
        nvg.drawRect(px + 10*unit, py + 8*unit - swing, 2*unit, 4*unit, shirtDark);
        nvg.drawRect(px + 4*unit, py + 14*unit - swing, 3*unit, 6*unit, pants);
        nvg.drawRect(px + 7*unit, py + 14*unit + swing, 3*unit, 6*unit, pantsDark);
        nvg.drawVerticalGradientRect(px - 3.0F, py + 8.0F, 3.0F, 10.0F,
                new Color(accent.getColor1().getRed(), accent.getColor1().getGreen(),
                        accent.getColor1().getBlue(), 110),
                new Color(accent.getColor2().getRed(), accent.getColor2().getGreen(),
                        accent.getColor2().getBlue(), 0));
    }

    private void startGame() {
        pipeOneX = x + width + pipeWidth;
        pipeTwoX = x + width + width / 2.0F + pipeWidth;
        pipeOneYGap = getNewPipeHeight();
        pipeTwoYGap = getNewPipeHeight();
        pipeSpeed = 100.0F;
        score = 0;
        playerX = x + 40.0F;
        playerTargetYPosition = height / 2.0F;
        playerActualYPosition = height / 2.0F;
        inPipeOne = inPipeTwo = isPlayerDead = shouldStart = false;
        gameStarted = true;
    }
}
