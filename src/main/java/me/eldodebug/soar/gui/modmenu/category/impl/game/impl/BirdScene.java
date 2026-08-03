package me.eldodebug.soar.gui.modmenu.category.impl.game.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.lwjgl.input.Keyboard;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.gui.modmenu.category.impl.GamesCategory;
import me.eldodebug.soar.gui.modmenu.category.impl.game.GameScene;
import me.eldodebug.soar.management.color.palette.ColorPalette;
import me.eldodebug.soar.management.color.palette.ColorType;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.management.nanovg.font.LegacyIcon;
import me.eldodebug.soar.utils.mouse.MouseUtils;

public class BirdScene extends GameScene {

    private static final float FIXED_STEP = 1.0F / 120.0F;
    private static final float GRAVITY = 520.0F;
    private static final float JUMP_VELOCITY = -190.0F;
    private static final float MAX_FALL_SPEED = 300.0F;
    private static final float HEAD_SIZE = 19.0F;
    private static final float PIPE_WIDTH = 38.0F;
    private static final float PIPE_SPACING = 176.0F;
    private static final float LAVA_HEIGHT = 22.0F;

    private final Random random = new Random();
    private final List<Pipe> pipes = new ArrayList<Pipe>();

    private int x;
    private int y;
    private int width;
    private int height;
    private int previousWidth;
    private int previousHeight;

    private float playerX;
    private float playerY;
    private float previousPlayerY;
    private float velocityY;
    private float accumulator;
    private float jumpBuffer;
    private float idleTime;
    private float cloudTime;

    private int score;
    private boolean started;
    private boolean dead;
    private long deathTime;
    private long lastFrameNanos;

    public BirdScene(GamesCategory parent) {
        super(parent, "Flappy Glide", "Smooth Steve-head flight through Minecraft terrain", LegacyIcon.PLAY);
    }

    @Override
    public void initGui() {
        syncLayout();
        resetToTitle();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        syncLayout();
        float dt = frameDelta();
        cloudTime += dt;
        idleTime += dt;

        if(started && !dead) {
            accumulator = Math.min(accumulator + dt, FIXED_STEP * 8.0F);
            while(accumulator >= FIXED_STEP) {
                updateFixed(FIXED_STEP);
                accumulator -= FIXED_STEP;
                if(dead) {
                    break;
                }
            }
        }

        float alpha = started && !dead
                ? Math.max(0.0F, Math.min(1.0F, accumulator / FIXED_STEP)) : 1.0F;
        float renderPlayerY = previousPlayerY + (playerY - previousPlayerY) * alpha;

        Glide glide = Glide.getInstance();
        NanoVGManager nvg = glide.getNanoVGManager();
        ColorPalette palette = glide.getColorManager().getPalette();

        nvg.save();
        nvg.scissor(x, y, width, height);
        drawSky(nvg);
        drawCloudLayer(nvg);
        drawPipes(nvg, alpha);
        drawLava(nvg);
        drawSteveHead(nvg, renderPlayerY);
        drawHud(nvg, palette);
        drawOverlay(nvg, palette);
        nvg.restore();
        nvg.drawOutlineRoundedRect(x, y, width, height, 10.0F, 1.0F,
                new Color(255, 255, 255, 70));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if(mouseButton == 0 && MouseUtils.isInside(mouseX, mouseY, x, y, width, height)) {
            queueJump();
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if(keyCode == Keyboard.KEY_SPACE || keyCode == Keyboard.KEY_UP
                || keyCode == Keyboard.KEY_W || keyCode == Keyboard.KEY_RETURN) {
            queueJump();
        }
    }

    private void syncLayout() {
        x = getX();
        y = getY();
        width = getWidth();
        height = getHeight();
        if(previousWidth != 0 && (previousWidth != width || previousHeight != height)) {
            resetToTitle();
        }
        previousWidth = width;
        previousHeight = height;
        playerX = Math.max(48.0F, width * 0.22F);
    }

    private float frameDelta() {
        long now = System.nanoTime();
        if(lastFrameNanos == 0L) {
            lastFrameNanos = now;
            return 1.0F / 60.0F;
        }
        float dt = (now - lastFrameNanos) / 1000000000.0F;
        lastFrameNanos = now;
        return Math.max(0.0F, Math.min(0.05F, dt));
    }

    private void resetToTitle() {
        started = false;
        dead = false;
        score = 0;
        accumulator = 0.0F;
        jumpBuffer = 0.0F;
        velocityY = 0.0F;
        playerY = Math.max(50.0F, (height - LAVA_HEIGHT) * 0.50F);
        previousPlayerY = playerY;
        pipes.clear();
        createPipes();
        lastFrameNanos = 0L;
    }

    private void startGame() {
        started = true;
        dead = false;
        score = 0;
        accumulator = 0.0F;
        velocityY = JUMP_VELOCITY;
        playerY = Math.max(50.0F, (height - LAVA_HEIGHT) * 0.50F);
        previousPlayerY = playerY;
        pipes.clear();
        createPipes();
    }

    private void createPipes() {
        float startX = Math.max(width + 65.0F, playerX + 210.0F);
        for(int i = 0; i < 3; i++) {
            pipes.add(new Pipe(startX + i * PIPE_SPACING, randomGapY(), currentGapHeight()));
        }
    }

    private void queueJump() {
        if(!started) {
            startGame();
            return;
        }
        if(dead) {
            if(System.currentTimeMillis() - deathTime >= 350L) {
                startGame();
            }
            return;
        }
        jumpBuffer = 0.12F;
    }

    private void updateFixed(float step) {
        previousPlayerY = playerY;
        jumpBuffer = Math.max(0.0F, jumpBuffer - step);
        if(jumpBuffer > 0.0F) {
            velocityY = JUMP_VELOCITY;
            jumpBuffer = 0.0F;
        }
        velocityY = Math.min(MAX_FALL_SPEED, velocityY + GRAVITY * step);
        playerY += velocityY * step;

        float speed = Math.min(154.0F, 96.0F + score * 2.6F);
        for(Pipe pipe : pipes) {
            pipe.previousX = pipe.x;
            pipe.x -= speed * step;
        }
        recyclePipes();
        detectCollisionsAndScore();
    }

    private void recyclePipes() {
        float rightMost = 0.0F;
        for(Pipe pipe : pipes) {
            rightMost = Math.max(rightMost, pipe.x);
        }
        for(Pipe pipe : pipes) {
            if(pipe.x + PIPE_WIDTH < -4.0F) {
                pipe.x = rightMost + PIPE_SPACING;
                pipe.previousX = pipe.x;
                pipe.gapHeight = currentGapHeight();
                pipe.gapY = randomGapY(pipe.gapHeight);
                pipe.scored = false;
                rightMost = pipe.x;
            }
        }
    }

    private float currentGapHeight() {
        return Math.max(72.0F, 96.0F - score * 0.65F);
    }

    private float randomGapY() {
        return randomGapY(currentGapHeight());
    }

    private float randomGapY(float gapHeight) {
        float min = 28.0F;
        float max = Math.max(min + 8.0F,
                height - LAVA_HEIGHT - gapHeight - 24.0F);
        return min + random.nextFloat() * (max - min);
    }

    private void detectCollisionsAndScore() {
        float half = HEAD_SIZE * 0.43F;
        float left = playerX - half;
        float right = playerX + half;
        float top = playerY - half;
        float bottom = playerY + half;

        if(top <= 0.0F || bottom >= height - LAVA_HEIGHT) {
            die();
            return;
        }

        for(Pipe pipe : pipes) {
            boolean overlaps = right >= pipe.x + 2.0F
                    && left <= pipe.x + PIPE_WIDTH - 2.0F;
            if(overlaps && (top <= pipe.gapY
                    || bottom >= pipe.gapY + pipe.gapHeight)) {
                die();
                return;
            }
            if(!pipe.scored && pipe.x + PIPE_WIDTH < playerX) {
                pipe.scored = true;
                score++;
            }
        }
    }

    private void die() {
        if(dead) {
            return;
        }
        dead = true;
        started = false;
        deathTime = System.currentTimeMillis();
    }

    private void drawSky(NanoVGManager nvg) {
        nvg.drawVerticalGradientRect(x, y, width, height,
                new Color(84, 167, 244), new Color(187, 226, 250));
        nvg.drawCircle(x + width - 52.0F, y + 46.0F, 23.0F,
                new Color(255, 244, 180, 175));
    }

    private void drawCloudLayer(NanoVGManager nvg) {
        float travel = width + 130.0F;
        drawCloud(nvg, x + width - (cloudTime * 8.0F % travel),
                y + 30.0F, 1.0F, 150);
        drawCloud(nvg,
                x + width - ((cloudTime * 5.5F + travel * 0.38F) % travel),
                y + 82.0F, 0.78F, 115);
        drawCloud(nvg,
                x + width - ((cloudTime * 7.0F + travel * 0.71F) % travel),
                y + 55.0F, 1.18F, 135);
    }

    private void drawCloud(NanoVGManager nvg, float cloudX, float cloudY,
            float scale, int opacity) {
        float unit = 7.0F * scale;
        int[][] pixels = {
                {2,0},{3,0},{4,0},
                {1,1},{2,1},{3,1},{4,1},{5,1},
                {0,2},{1,2},{2,2},{3,2},{4,2},{5,2},{6,2},
                {1,3},{2,3},{3,3},{4,3},{5,3}
        };
        for(int[] pixel : pixels) {
            float px = cloudX + pixel[0] * unit;
            float py = cloudY + pixel[1] * unit;
            nvg.drawRect(px, py, unit + 0.2F, unit + 0.2F,
                    new Color(255, 255, 255, opacity));
            nvg.drawRect(px, py + unit - 1.0F, unit, 1.0F,
                    new Color(205, 220, 232, Math.max(20, opacity - 35)));
        }
    }

    private void drawPipes(NanoVGManager nvg, float alpha) {
        for(Pipe pipe : pipes) {
            float renderX = pipe.previousX + (pipe.x - pipe.previousX) * alpha;
            drawTerrainColumn(nvg, x + renderX, y,
                    PIPE_WIDTH, pipe.gapY, true);
            float lowerY = pipe.gapY + pipe.gapHeight;
            drawTerrainColumn(nvg, x + renderX, y + lowerY,
                    PIPE_WIDTH, height - LAVA_HEIGHT - lowerY, false);
        }
    }

    private void drawTerrainColumn(NanoVGManager nvg, float columnX,
            float columnY, float columnWidth, float columnHeight,
            boolean hangingFromTop) {
        if(columnHeight <= 0.0F) {
            return;
        }
        nvg.drawRect(columnX, columnY, columnWidth, columnHeight,
                new Color(104, 68, 40));
        float tile = 8.0F;
        int row = 0;
        for(float py = 0.0F; py < columnHeight; py += tile) {
            int col = 0;
            for(float px = 0.0F; px < columnWidth; px += tile) {
                float tileWidth = Math.min(tile, columnWidth - px);
                float tileHeight = Math.min(tile, columnHeight - py);
                Color dirt = ((row + col) & 1) == 0
                        ? new Color(126, 82, 48) : new Color(113, 72, 43);
                nvg.drawRect(columnX + px, columnY + py,
                        tileWidth, tileHeight, dirt);
                nvg.drawRect(columnX + px + 1.0F, columnY + py + 1.0F,
                        Math.max(1.0F, tileWidth - 5.0F), 1.2F,
                        new Color(157, 107, 66, 180));
                nvg.drawRect(columnX + px + tileWidth - 2.5F,
                        columnY + py + tileHeight - 2.5F,
                        1.5F, 1.5F, new Color(76, 48, 31, 190));
                col++;
            }
            row++;
        }

        float grassY = hangingFromTop
                ? columnY + columnHeight - 7.0F : columnY;
        nvg.drawRect(columnX - 2.0F, grassY, columnWidth + 4.0F, 7.0F,
                new Color(73, 139, 45));
        nvg.drawRect(columnX - 2.0F, grassY, columnWidth + 4.0F, 2.5F,
                new Color(132, 193, 72));
        for(float grassX = 1.0F; grassX < columnWidth; grassX += 6.0F) {
            float bladeY = hangingFromTop ? grassY + 4.0F : grassY + 2.0F;
            nvg.drawRect(columnX + grassX, bladeY, 2.0F, 2.0F,
                    new Color(92, 165, 55));
        }
        nvg.drawOutlineRoundedRect(columnX, columnY,
                columnWidth, columnHeight, 1.0F, 0.8F,
                new Color(44, 31, 21, 105));
    }

    private void drawLava(NanoVGManager nvg) {
        float lavaY = y + height - LAVA_HEIGHT;
        nvg.drawRect(x, lavaY, width, LAVA_HEIGHT,
                new Color(135, 32, 10));
        nvg.drawRect(x, lavaY, width, 2.0F,
                new Color(255, 220, 76));
        float phase = (cloudTime * 20.0F) % 16.0F;
        for(float px = -16.0F; px < width + 16.0F; px += 16.0F) {
            float waveX = x + px - phase;
            nvg.drawRect(waveX, lavaY + 3.0F, 13.0F, 6.0F,
                    new Color(255, 101, 25));
            nvg.drawRect(waveX + 2.0F, lavaY + 4.0F, 6.0F, 2.0F,
                    new Color(255, 226, 79));
            nvg.drawRect(waveX + 6.0F, lavaY + 10.0F, 8.0F, 6.0F,
                    new Color(193, 45, 13));
            nvg.drawRect(waveX + 9.0F, lavaY + 12.0F, 4.0F, 2.0F,
                    new Color(242, 72, 18));
        }
    }

    private void drawSteveHead(NanoVGManager nvg, float localY) {
        float bob = !started && !dead
                ? (float)Math.sin(idleTime * 3.2F) * 4.0F : 0.0F;
        float headX = x + playerX - HEAD_SIZE / 2.0F;
        float headY = y + localY - HEAD_SIZE / 2.0F + bob;
        float unit = HEAD_SIZE / 8.0F;
        Color skin = new Color(198, 145, 104);
        Color skinLight = new Color(229, 181, 139);
        Color skinDark = new Color(151, 101, 72);
        Color hair = new Color(67, 43, 30);
        Color hairLight = new Color(91, 58, 39);

        nvg.drawRoundedRect(headX - 2.0F, headY - 2.0F,
                HEAD_SIZE + 4.0F, HEAD_SIZE + 4.0F,
                3.0F, new Color(0, 0, 0, 85));
        nvg.drawRect(headX, headY, HEAD_SIZE, HEAD_SIZE, skin);
        nvg.drawRect(headX + unit, headY + unit * 2.0F,
                unit * 6.0F, unit * 5.0F, skinLight);
        nvg.drawRect(headX, headY, HEAD_SIZE, unit * 2.0F, hair);
        nvg.drawRect(headX, headY + unit * 2.0F,
                unit, unit * 3.0F, hairLight);
        nvg.drawRect(headX + unit * 7.0F, headY + unit * 2.0F,
                unit, unit * 3.0F, hair);
        nvg.drawRect(headX + unit, headY + unit,
                unit * 2.0F, unit, hairLight);
        nvg.drawRect(headX + unit * 5.0F, headY + unit,
                unit * 2.0F, unit, hairLight);
        nvg.drawRect(headX + unit * 2.0F, headY + unit * 3.0F,
                unit, unit, new Color(238, 238, 232));
        nvg.drawRect(headX + unit * 5.0F, headY + unit * 3.0F,
                unit, unit, new Color(238, 238, 232));
        nvg.drawRect(headX + unit * 2.45F, headY + unit * 3.1F,
                unit * 0.45F, unit * 0.75F, new Color(55, 96, 151));
        nvg.drawRect(headX + unit * 5.45F, headY + unit * 3.1F,
                unit * 0.45F, unit * 0.75F, new Color(55, 96, 151));
        nvg.drawRect(headX + unit * 3.0F, headY + unit * 4.0F,
                unit * 2.0F, unit * 2.0F, skin);
        nvg.drawRect(headX + unit * 2.0F, headY + unit * 6.0F,
                unit, unit, hairLight);
        nvg.drawRect(headX + unit * 5.0F, headY + unit * 6.0F,
                unit, unit, hairLight);
        nvg.drawRect(headX + unit * 3.0F, headY + unit * 6.0F,
                unit * 2.0F, unit, skinDark);
        nvg.drawOutlineRoundedRect(headX, headY, HEAD_SIZE, HEAD_SIZE,
                1.0F, 0.8F, new Color(35, 24, 18, 130));
    }

    private void drawHud(NanoVGManager nvg, ColorPalette palette) {
        if(started || dead) {
            nvg.drawRoundedRect(x + 9.0F, y + 9.0F,
                    66.0F, 25.0F, 7.0F,
                    new Color(7, 14, 24, 125));
            nvg.drawText("Score " + score, x + 18.0F, y + 17.0F,
                    Color.WHITE, 8.5F, Fonts.SEMIBOLD);
        }
        nvg.drawText("SPACE / CLICK", x + width - 94.0F, y + 14.0F,
                palette.getFontColor(ColorType.NORMAL), 7.0F, Fonts.MEDIUM);
    }

    private void drawOverlay(NanoVGManager nvg, ColorPalette palette) {
        if(started && !dead) {
            return;
        }
        float cardWidth = Math.min(310.0F, width - 48.0F);
        float cardHeight = dead ? 116.0F : 108.0F;
        float cardX = x + (width - cardWidth) / 2.0F;
        float cardY = y + (height - cardHeight) / 2.0F - 4.0F;
        nvg.drawRoundedRect(cardX, cardY, cardWidth, cardHeight,
                13.0F, new Color(8, 17, 27, 210));
        nvg.drawOutlineRoundedRect(cardX + 0.5F, cardY + 0.5F,
                cardWidth - 1.0F, cardHeight - 1.0F,
                13.0F, 0.8F, new Color(255, 255, 255, 55));
        String title = dead ? "YOU DIED" : "FLAPPY GLIDE";
        nvg.drawCenteredText(title, x + width / 2.0F, cardY + 19.0F,
                dead ? new Color(255, 91, 78) : Color.WHITE,
                16.0F, Fonts.SEMIBOLD);
        String line = dead ? "Score " + score
                : "Guide Steve's head between the grass blocks";
        nvg.drawCenteredText(line, x + width / 2.0F, cardY + 54.0F,
                palette.getFontColor(ColorType.NORMAL), 8.0F, Fonts.MEDIUM);
        String action = dead ? "Press SPACE or click to retry"
                : "Press SPACE or click to fly";
        nvg.drawCenteredText(action, x + width / 2.0F, cardY + 79.0F,
                Color.WHITE, 8.0F, Fonts.SEMIBOLD);
    }

    private static final class Pipe {
        private float x;
        private float previousX;
        private float gapY;
        private float gapHeight;
        private boolean scored;

        private Pipe(float x, float gapY, float gapHeight) {
            this.x = x;
            this.previousX = x;
            this.gapY = gapY;
            this.gapHeight = gapHeight;
        }
    }
}
