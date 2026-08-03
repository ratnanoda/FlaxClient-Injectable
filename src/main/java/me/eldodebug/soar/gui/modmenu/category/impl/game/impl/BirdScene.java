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
    private static final float RISE_GRAVITY = 405.0F;
    private static final float FALL_GRAVITY = 485.0F;
    private static final float JUMP_VELOCITY = -166.0F;
    private static final float MAX_FALL_SPEED = 255.0F;
    private static final float HEAD_SIZE = 22.0F;
    private static final float PIPE_WIDTH = 42.0F;
    private static final float PIPE_SPACING = 190.0F;
    private static final float LAVA_HEIGHT = 26.0F;

    // Front-face pixels sampled from the classic default Steve skin reference
    // published online. Keeping the pixels in code avoids any network request at
    // runtime and keeps the game usable offline.
    private static final int[][] STEVE_FACE = {
            {0x2B1B12, 0x332016, 0x3A2519, 0x40281C, 0x3A2418, 0x342016, 0x2E1C13, 0x28180F},
            {0x321F15, 0x3A2418, 0x452C1F, 0x4A3021, 0x43291C, 0x3D261A, 0x352116, 0x2D1B12},
            {0x3B2519, 0xB97857, 0xC88764, 0xD0916D, 0xCC8966, 0xC27F5D, 0xB87454, 0x382217},
            {0x4A2E20, 0xC98664, 0xD49672, 0xD89A76, 0xD2926D, 0xC98763, 0xBD7959, 0x43291C},
            {0xB87354, 0xF3F2EA, 0x4A6EA8, 0xC58463, 0xC08060, 0x4A6EA8, 0xF3F2EA, 0xB36E51},
            {0xB87556, 0xC78463, 0xC98665, 0xA96149, 0xA96149, 0xC37F60, 0xBD795A, 0xAE694F},
            {0x9E5A43, 0x74402F, 0x7E4936, 0xB36B50, 0xB36B50, 0x7C4735, 0x70402F, 0x96533E},
            {0x7A4634, 0x6B3C2C, 0x734130, 0x7F4936, 0x7D4735, 0x70402F, 0x683929, 0x74402F}
    };


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
    private float jumpCooldown;
    private float cameraKick;
    private final List<FlightParticle> flightParticles = new ArrayList<FlightParticle>();

    private int score;
    private boolean started;
    private boolean dead;
    private long deathTime;
    private long lastFrameNanos;

    public BirdScene(GamesCategory parent) {
        super(parent, "Flappy Steve", "Smooth Steve flight through Minecraft terrain", LegacyIcon.PLAY);
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
        cameraKick = approach(cameraKick, 0.0F, 12.0F, dt);
        updateFlightParticles(dt);

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
        drawFlightParticles(nvg);
        drawSteveHead(nvg, renderPlayerY - cameraKick);
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
        jumpCooldown = 0.0F;
        cameraKick = 0.0F;
        flightParticles.clear();
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
        jumpCooldown = 0.0F;
        cameraKick = 2.2F;
        flightParticles.clear();
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
        jumpBuffer = 0.15F;
    }

    private void updateFixed(float step) {
        previousPlayerY = playerY;
        jumpBuffer = Math.max(0.0F, jumpBuffer - step);
        jumpCooldown = Math.max(0.0F, jumpCooldown - step);
        if(jumpBuffer > 0.0F && jumpCooldown <= 0.0F) {
            // Preserve a little upward momentum while cancelling harsh downward
            // speed. This makes repeated taps responsive without feeling jerky.
            float carriedMomentum = Math.min(0.0F, velocityY) * 0.24F;
            velocityY = JUMP_VELOCITY + carriedMomentum;
            jumpBuffer = 0.0F;
            jumpCooldown = 0.072F;
            cameraKick = 2.6F;
            spawnJumpParticles();
        }

        float gravity = velocityY < 0.0F ? RISE_GRAVITY : FALL_GRAVITY;
        velocityY = Math.min(MAX_FALL_SPEED, velocityY + gravity * step);
        velocityY *= (float)Math.pow(0.9965F, step * 120.0F);
        playerY += velocityY * step;

        float speed = Math.min(146.0F, 88.0F + score * 2.25F);
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
        return Math.max(78.0F, 104.0F - score * 0.58F);
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
        float half = HEAD_SIZE * 0.355F;
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
                new Color(72, 43, 27));
        float block = 12.0F;
        int row = 0;
        for(float py = 0.0F; py < columnHeight; py += block) {
            int col = 0;
            for(float px = 0.0F; px < columnWidth; px += block) {
                float bw = Math.min(block, columnWidth - px);
                float bh = Math.min(block, columnHeight - py);
                float bx = columnX + px;
                float by = columnY + py;
                Color base = ((row + col) & 1) == 0
                        ? new Color(128, 84, 49) : new Color(116, 73, 43);
                nvg.drawRect(bx, by, bw, bh, base);
                nvg.drawRect(bx + 1.0F, by + 1.0F, Math.max(1.0F, bw - 2.0F), 1.0F,
                        new Color(157, 108, 67, 175));
                nvg.drawRect(bx + bw * 0.18F, by + bh * 0.34F, 2.0F, 2.0F,
                        new Color(91, 56, 35, 215));
                nvg.drawRect(bx + bw * 0.62F, by + bh * 0.62F, 2.5F, 1.5F,
                        new Color(78, 48, 31, 205));
                nvg.drawRect(bx + bw * 0.52F, by + bh * 0.18F, 2.0F, 1.2F,
                        new Color(176, 122, 75, 150));
                col++;
            }
            row++;
        }

        float capY = hangingFromTop ? columnY + columnHeight - 9.0F : columnY;
        nvg.drawRect(columnX - 2.0F, capY, columnWidth + 4.0F, 9.0F,
                new Color(72, 126, 42));
        nvg.drawRect(columnX - 2.0F, capY, columnWidth + 4.0F, 3.0F,
                new Color(127, 188, 67));
        for(float gx = 0.0F; gx < columnWidth + 2.0F; gx += 5.0F) {
            float blade = ((int)(gx / 5.0F) & 1) == 0 ? 2.0F : 3.0F;
            float grassPixelY = hangingFromTop ? capY + 5.0F : capY + 3.0F;
            nvg.drawRect(columnX - 1.0F + gx, grassPixelY, 2.5F, blade,
                    new Color(91, 158, 48));
        }
        nvg.drawRect(columnX - 2.0F, hangingFromTop ? capY + 8.0F : capY,
                columnWidth + 4.0F, 1.0F, new Color(45, 77, 28, 210));
        nvg.drawOutlineRoundedRect(columnX - 0.5F, columnY - 0.5F,
                columnWidth + 1.0F, columnHeight + 1.0F, 2.0F, 0.9F,
                new Color(32, 22, 16, 155));
    }

    private void drawLava(NanoVGManager nvg) {
        float lavaY = y + height - LAVA_HEIGHT;
        nvg.drawVerticalGradientRect(x, lavaY, width, LAVA_HEIGHT,
                new Color(250, 88, 17), new Color(112, 22, 9));
        nvg.drawRect(x, lavaY, width, 2.5F, new Color(255, 231, 92));
        float phase = (cloudTime * 23.0F) % 16.0F;
        for(float px = -16.0F; px < width + 18.0F; px += 16.0F) {
            float waveX = x + px - phase;
            nvg.drawRect(waveX, lavaY + 3.0F, 12.0F, 5.0F,
                    new Color(255, 149, 28));
            nvg.drawRect(waveX + 2.0F, lavaY + 4.0F, 5.0F, 2.0F,
                    new Color(255, 238, 112));
            nvg.drawRect(waveX + 8.0F, lavaY + 9.0F, 7.0F, 6.0F,
                    new Color(188, 42, 13));
            nvg.drawRect(waveX + 4.0F, lavaY + 17.0F, 10.0F, 5.0F,
                    new Color(101, 20, 10));
            nvg.drawRect(waveX + 1.0F, lavaY + 13.0F, 4.0F, 3.0F,
                    new Color(239, 70, 15));
        }
    }

    private void drawSteveHead(NanoVGManager nvg, float localY) {
        float bob = !started && !dead
                ? (float)Math.sin(idleTime * 3.2F) * 3.0F : 0.0F;
        float headX = x + playerX - HEAD_SIZE / 2.0F;
        float headY = y + localY - HEAD_SIZE / 2.0F + bob;
        float unit = HEAD_SIZE / 8.0F;

        nvg.drawRoundedRect(headX - 3.0F, headY - 3.0F,
                HEAD_SIZE + 6.0F, HEAD_SIZE + 6.0F,
                4.0F, new Color(0, 0, 0, 112));
        for(int row = 0; row < 8; row++) {
            for(int column = 0; column < 8; column++) {
                int rgb = STEVE_FACE[row][column];
                nvg.drawRect(headX + column * unit, headY + row * unit,
                        unit + 0.15F, unit + 0.15F, new Color(rgb));
            }
        }
        nvg.drawOutlineRoundedRect(headX - 0.5F, headY - 0.5F,
                HEAD_SIZE + 1.0F, HEAD_SIZE + 1.0F,
                2.0F, 1.0F, new Color(20, 13, 9, 185));
        if(Math.abs(velocityY) > 45.0F) {
            int alpha = Math.min(85, (int)(Math.abs(velocityY) * 0.32F));
            nvg.drawRect(headX - 6.0F, headY + HEAD_SIZE * 0.28F,
                    4.0F, HEAD_SIZE * 0.16F, new Color(255, 255, 255, alpha));
            nvg.drawRect(headX - 10.0F, headY + HEAD_SIZE * 0.58F,
                    6.0F, HEAD_SIZE * 0.11F, new Color(255, 255, 255, alpha / 2));
        }
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
        String title = dead ? "YOU DIED" : "FLAPPY STEVE";
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

    private void spawnJumpParticles() {
        float originX = playerX - HEAD_SIZE * 0.58F;
        float originY = playerY + HEAD_SIZE * 0.18F;
        for(int i = 0; i < 6; i++) {
            flightParticles.add(new FlightParticle(originX, originY,
                    -26.0F - random.nextFloat() * 34.0F,
                    (random.nextFloat() - 0.5F) * 42.0F,
                    0.28F + random.nextFloat() * 0.22F,
                    1.2F + random.nextFloat() * 1.8F));
        }
    }

    private void updateFlightParticles(float dt) {
        for(int i = flightParticles.size() - 1; i >= 0; i--) {
            FlightParticle particle = flightParticles.get(i);
            particle.life -= dt;
            if(particle.life <= 0.0F) {
                flightParticles.remove(i);
                continue;
            }
            particle.x += particle.vx * dt;
            particle.y += particle.vy * dt;
            particle.vy += 38.0F * dt;
        }
    }

    private void drawFlightParticles(NanoVGManager nvg) {
        for(FlightParticle particle : flightParticles) {
            float life = Math.max(0.0F, particle.life / particle.maxLife);
            float size = particle.size * (0.55F + life * 0.45F);
            nvg.drawRect(x + particle.x - size / 2.0F,
                    y + particle.y - size / 2.0F,
                    size, size, new Color(225, 239, 249, (int)(life * 125.0F)));
        }
    }

    private float approach(float current, float target, float speed, float dt) {
        float factor = 1.0F - (float)Math.exp(-speed * dt);
        return current + (target - current) * factor;
    }

    private static final class FlightParticle {
        private float x;
        private float y;
        private final float vx;
        private float vy;
        private float life;
        private final float maxLife;
        private final float size;

        private FlightParticle(float x, float y, float vx, float vy,
                float life, float size) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = life;
            this.maxLife = life;
            this.size = size;
        }
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
