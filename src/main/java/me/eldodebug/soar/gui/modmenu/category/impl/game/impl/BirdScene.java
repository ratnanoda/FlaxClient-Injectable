package me.eldodebug.soar.gui.modmenu.category.impl.game.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import org.lwjgl.input.Keyboard;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.gui.modmenu.category.impl.GamesCategory;
import me.eldodebug.soar.gui.modmenu.category.impl.game.GameScene;
import me.eldodebug.soar.management.color.AccentColor;
import me.eldodebug.soar.management.color.ColorManager;
import me.eldodebug.soar.management.color.palette.ColorPalette;
import me.eldodebug.soar.management.color.palette.ColorType;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.management.nanovg.font.LegacyIcon;
import me.eldodebug.soar.utils.mouse.MouseUtils;

public class BirdScene extends GameScene {

    private static final float FIXED_STEP = 1.0F / 120.0F;
    private static final float GRAVITY = 520.0F;
    private static final float FLAP_VELOCITY = -188.0F;
    private static final float MAX_FALL_SPEED = 285.0F;
    private static final float BASE_PIPE_SPEED = 102.0F;
    private static final float PIPE_WIDTH = 34.0F;
    private static final float PLAYER_SIZE = 18.0F;

    private final Random random = new Random();
    private final Pipe[] pipes = {new Pipe(), new Pipe(), new Pipe()};
    private final ArrayList<Particle> particles = new ArrayList<Particle>();

    private int x;
    private int y;
    private int width;
    private int height;
    private int score;
    private int bestScore;

    private boolean running;
    private boolean dead;
    private long deathTime;
    private long lastFrameNanos;

    private float accumulator;
    private float playerX;
    private float playerY;
    private float playerVelocityY;
    private float pipeSpeed;
    private float gapHeight;
    private float elapsed;
    private float scorePulse;
    private float deathFlash;
    private float cameraShake;

    public BirdScene(GamesCategory parent) {
        super(parent, "Flappy Glide", "Smooth flight, responsive controls, and polished effects", LegacyIcon.PLAY);
    }

    @Override
    public void initGui() {
        syncBounds();
        resetIdleState();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        syncBounds();

        Glide instance = Glide.getInstance();
        NanoVGManager nvg = instance.getNanoVGManager();
        ColorManager colorManager = instance.getColorManager();
        ColorPalette palette = colorManager.getPalette();
        AccentColor accent = colorManager.getCurrentColor();

        float dt = frameDelta();
        elapsed += dt;
        scorePulse = anim(scorePulse, 0.0F, 7.0F, dt);
        deathFlash = anim(deathFlash, 0.0F, 4.5F, dt);
        cameraShake = anim(cameraShake, 0.0F, 9.0F, dt);

        if(running) {
            accumulator = Math.min(accumulator + dt, FIXED_STEP * 8.0F);
            int steps = 0;
            while(accumulator >= FIXED_STEP && steps++ < 8) {
                updateGame(FIXED_STEP, accent);
                accumulator -= FIXED_STEP;
            }
        } else if(!dead) {
            playerX = x + width * 0.23F;
            playerY = height * 0.47F + (float) Math.sin(elapsed * 2.5F) * 5.0F;
        }
        updateParticles(dt);

        nvg.save();
        nvg.scissor(x, y, width, height);

        float shakeX = cameraShake <= 0.05F ? 0.0F : (random.nextFloat() - 0.5F) * cameraShake;
        float shakeY = cameraShake <= 0.05F ? 0.0F : (random.nextFloat() - 0.5F) * cameraShake;
        nvg.translate(shakeX, shakeY);

        drawSky(nvg, palette, accent);
        drawParticles(nvg);
        if(running || dead) {
            drawPipes(nvg, palette, accent);
        }
        drawPlayer(nvg, accent);
        drawHud(nvg, palette, accent);
        drawStateOverlay(nvg, palette, accent);

        if(deathFlash > 0.01F) {
            nvg.drawRect(x, y, width, height, new Color(255, 255, 255,
                    Math.min(150, (int) (deathFlash * 150.0F))));
        }

        nvg.restore();
        nvg.drawOutlineRoundedRect(x, y, width, height, 10, 1.2F,
                alpha(palette.getFontColor(ColorType.NORMAL), 95));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if(mouseButton == 0 && MouseUtils.isInside(mouseX, mouseY, x, y, width, height)) {
            flap();
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if(keyCode == Keyboard.KEY_SPACE
                || keyCode == Keyboard.KEY_UP
                || keyCode == Keyboard.KEY_W
                || keyCode == Keyboard.KEY_RETURN) {
            flap();
        } else if(keyCode == Keyboard.KEY_R) {
            startGame();
        }
    }

    private void syncBounds() {
        x = getX();
        y = getY();
        width = getWidth();
        height = getHeight();
        gapHeight = Math.max(78.0F, Math.min(104.0F, height * 0.29F));
    }

    private float frameDelta() {
        long now = System.nanoTime();
        if(lastFrameNanos == 0L) {
            lastFrameNanos = now;
            return 1.0F / 60.0F;
        }
        float dt = (now - lastFrameNanos) / 1_000_000_000.0F;
        lastFrameNanos = now;
        return Math.max(0.0F, Math.min(dt, 0.05F));
    }

    private void resetIdleState() {
        running = false;
        dead = false;
        score = 0;
        accumulator = 0.0F;
        playerX = x + width * 0.23F;
        playerY = height * 0.47F;
        playerVelocityY = 0.0F;
        particles.clear();
        lastFrameNanos = 0L;
    }

    private void startGame() {
        running = true;
        dead = false;
        score = 0;
        accumulator = 0.0F;
        playerX = x + width * 0.23F;
        playerY = height * 0.47F;
        playerVelocityY = FLAP_VELOCITY;
        pipeSpeed = BASE_PIPE_SPEED;
        deathFlash = 0.0F;
        cameraShake = 0.0F;
        particles.clear();

        float spacing = Math.max(155.0F, width * 0.31F);
        for(int i = 0; i < pipes.length; i++) {
            pipes[i].x = x + width + 70.0F + i * spacing;
            pipes[i].gapTop = randomGapTop();
            pipes[i].scored = false;
        }
        spawnFlapParticles(Glide.getInstance().getColorManager().getCurrentColor());
    }

    private void flap() {
        if(dead) {
            if(System.currentTimeMillis() - deathTime < 350L) {
                return;
            }
            startGame();
            return;
        }
        if(!running) {
            startGame();
            return;
        }

        playerVelocityY = FLAP_VELOCITY;
        spawnFlapParticles(Glide.getInstance().getColorManager().getCurrentColor());
    }

    private void updateGame(float dt, AccentColor accent) {
        playerVelocityY = Math.min(MAX_FALL_SPEED, playerVelocityY + GRAVITY * dt);
        playerY += playerVelocityY * dt;
        pipeSpeed = BASE_PIPE_SPEED + Math.min(38.0F, score * 1.7F);

        float spacing = Math.max(155.0F, width * 0.31F);
        float rightMost = x;
        for(Pipe pipe : pipes) {
            pipe.x -= pipeSpeed * dt;
            rightMost = Math.max(rightMost, pipe.x);
        }

        for(Pipe pipe : pipes) {
            if(pipe.x + PIPE_WIDTH < x) {
                pipe.x = rightMost + spacing;
                rightMost = pipe.x;
                pipe.gapTop = randomGapTop();
                pipe.scored = false;
            }

            if(!pipe.scored && pipe.x + PIPE_WIDTH < playerX - PLAYER_SIZE * 0.35F) {
                pipe.scored = true;
                score++;
                bestScore = Math.max(bestScore, score);
                scorePulse = 1.0F;
                spawnScoreParticles(accent);
            }
        }

        if(collides()) {
            die(accent);
        }
    }

    private boolean collides() {
        float radius = PLAYER_SIZE * 0.39F;
        float left = playerX - radius;
        float right = playerX + radius;
        float top = playerY - radius;
        float bottom = playerY + radius;

        if(top <= 1.0F || bottom >= height - 1.0F) {
            return true;
        }

        for(Pipe pipe : pipes) {
            boolean horizontal = right >= pipe.x && left <= pipe.x + PIPE_WIDTH;
            if(horizontal && (top <= pipe.gapTop || bottom >= pipe.gapTop + gapHeight)) {
                return true;
            }
        }
        return false;
    }

    private void die(AccentColor accent) {
        if(dead) {
            return;
        }
        running = false;
        dead = true;
        deathTime = System.currentTimeMillis();
        bestScore = Math.max(bestScore, score);
        deathFlash = 1.0F;
        cameraShake = 8.0F;
        spawnDeathParticles(accent);
    }

    private float randomGapTop() {
        float margin = 24.0F;
        float available = Math.max(1.0F, height - gapHeight - margin * 2.0F);
        return margin + random.nextFloat() * available;
    }

    private void drawSky(NanoVGManager nvg, ColorPalette palette, AccentColor accent) {
        Color base = palette.getBackgroundColor(ColorType.DARK);
        Color upper = mix(base, accent.getColor1(), 0.24F, 245);
        Color lower = mix(base, accent.getColor2(), 0.10F, 245);
        nvg.drawVerticalGradientRect(x, y, width, height, upper, lower);

        for(int i = 0; i < 18; i++) {
            float sx = x + ((i * 83.0F + elapsed * (8.0F + i % 3)) % (width + 30.0F)) - 15.0F;
            float sy = y + 18.0F + (i * 47 % Math.max(30, height - 55));
            float twinkle = 0.45F + 0.35F * (float) Math.sin(elapsed * 2.0F + i);
            nvg.drawCircle(sx, sy, 0.7F + (i % 3) * 0.25F,
                    new Color(255, 255, 255, Math.max(18, (int) (twinkle * 95.0F))));
        }

        for(int i = 0; i < 4; i++) {
            float cloudX = x + ((i * 190.0F - elapsed * (10.0F + i * 2.0F)) % (width + 180.0F));
            if(cloudX < x - 90.0F) {
                cloudX += width + 180.0F;
            }
            float cloudY = y + 42.0F + (i % 3) * 62.0F;
            Color cloud = new Color(255, 255, 255, 18 + i * 3);
            nvg.drawRoundedRect(cloudX, cloudY, 70, 13, 7, cloud);
            nvg.drawCircle(cloudX + 18, cloudY + 1, 12, cloud);
            nvg.drawCircle(cloudX + 43, cloudY, 16, cloud);
        }

        nvg.drawVerticalGradientRect(x, y + height - 34, width, 34,
                new Color(0, 0, 0, 0), new Color(0, 0, 0, 48));
    }

    private void drawPipes(NanoVGManager nvg, ColorPalette palette, AccentColor accent) {
        Color pipeA = new Color(64, 226, 145, 245);
        Color pipeB = mix(new Color(26, 174, 108), accent.getColor1(), 0.22F, 245);
        Color outline = new Color(255, 255, 255, 42);

        for(Pipe pipe : pipes) {
            float topHeight = pipe.gapTop;
            float bottomY = y + pipe.gapTop + gapHeight;
            float bottomHeight = height - pipe.gapTop - gapHeight;

            nvg.drawShadow(pipe.x, y - 8, PIPE_WIDTH, topHeight + 8, 7, 5);
            nvg.drawGradientRoundedRect(pipe.x, y - 8, PIPE_WIDTH, topHeight + 8, 6, pipeA, pipeB);
            nvg.drawOutlineRoundedRect(pipe.x + 0.5F, y - 7.5F, PIPE_WIDTH - 1.0F,
                    topHeight + 7.0F, 6, 0.8F, outline);
            nvg.drawGradientRoundedRect(pipe.x - 4, y + topHeight - 10, PIPE_WIDTH + 8, 12, 4,
                    pipeA, pipeB);

            nvg.drawShadow(pipe.x, bottomY, PIPE_WIDTH, bottomHeight + 8, 7, 5);
            nvg.drawGradientRoundedRect(pipe.x, bottomY, PIPE_WIDTH, bottomHeight + 8, 6, pipeB, pipeA);
            nvg.drawOutlineRoundedRect(pipe.x + 0.5F, bottomY + 0.5F, PIPE_WIDTH - 1.0F,
                    bottomHeight + 7.0F, 6, 0.8F, outline);
            nvg.drawGradientRoundedRect(pipe.x - 4, bottomY - 2, PIPE_WIDTH + 8, 12, 4,
                    pipeB, pipeA);
        }
    }

    private void drawPlayer(NanoVGManager nvg, AccentColor accent) {
        float drawY = y + playerY;
        float angle = Math.max(-24.0F, Math.min(70.0F, playerVelocityY * 0.18F));
        if(!running && !dead) {
            angle = (float) Math.sin(elapsed * 2.5F) * 6.0F;
        }

        float bx = playerX - PLAYER_SIZE / 2.0F;
        float by = drawY - PLAYER_SIZE / 2.0F;
        nvg.save();
        nvg.rotate(bx, by, PLAYER_SIZE, PLAYER_SIZE, (float) Math.toRadians(angle));
        nvg.drawRoundedGlow(bx, by, PLAYER_SIZE, PLAYER_SIZE, 6,
                alpha(accent.getColor1(), 90), 5);
        nvg.drawGradientRoundedRect(bx, by, PLAYER_SIZE, PLAYER_SIZE, 6,
                accent.getColor1(), accent.getColor2());

        float wingLift = running ? Math.max(-2.0F, Math.min(3.0F, playerVelocityY / 90.0F)) :
                (float) Math.sin(elapsed * 7.0F) * 2.0F;
        nvg.drawRoundedRect(bx + 2.5F, by + 8.0F + wingLift, 8.0F, 5.0F, 2.5F,
                new Color(255, 255, 255, 150));
        nvg.drawCircle(bx + 13.0F, by + 5.2F, 3.0F, Color.WHITE);
        nvg.drawCircle(bx + 13.8F, by + 5.2F, 1.25F, new Color(35, 39, 55));
        nvg.drawRoundedRect(bx + 16.0F, by + 8.0F, 5.5F, 3.0F, 1.5F,
                new Color(255, 191, 65));
        nvg.restore();
    }

    private void drawHud(NanoVGManager nvg, ColorPalette palette, AccentColor accent) {
        if(running || dead) {
            float scoreScale = 1.0F + scorePulse * 0.22F;
            nvg.save();
            nvg.scale(x + width / 2.0F, y + 14.0F, scoreScale);
            nvg.drawCenteredText(String.valueOf(score), x + width / 2.0F, y + 8.0F,
                    Color.WHITE, 16, Fonts.SEMIBOLD);
            nvg.restore();

            nvg.drawRoundedRect(x + 10, y + 9, 76, 20, 7, new Color(0, 0, 0, 62));
            nvg.drawText("BEST  " + bestScore, x + 20, y + 14,
                    alpha(palette.getFontColor(ColorType.DARK), 225), 8, Fonts.MEDIUM);

            nvg.drawRoundedRect(x + width - 126, y + 9, 116, 20, 7,
                    new Color(0, 0, 0, 62));
            nvg.drawCenteredText("SPACE / CLICK", x + width - 68, y + 14,
                    alpha(accent.getColor1(), 235), 7.5F, Fonts.MEDIUM);
        }
    }

    private void drawStateOverlay(NanoVGManager nvg, ColorPalette palette, AccentColor accent) {
        if(running) {
            return;
        }

        float cardWidth = Math.min(310.0F, width - 46.0F);
        float cardHeight = dead ? 132.0F : 116.0F;
        float cardX = x + (width - cardWidth) / 2.0F;
        float cardY = y + (height - cardHeight) / 2.0F;
        float pulse = 0.5F + 0.5F * (float) Math.sin(elapsed * 3.0F);

        nvg.drawShadow(cardX, cardY, cardWidth, cardHeight, 14, 8);
        nvg.drawRoundedRect(cardX, cardY, cardWidth, cardHeight, 14,
                new Color(14, 18, 31, 214));
        nvg.drawGradientOutlineRoundedRect(cardX + 0.5F, cardY + 0.5F,
                cardWidth - 1.0F, cardHeight - 1.0F, 14, 1.1F,
                alpha(accent.getColor1(), 170), alpha(accent.getColor2(), 170));

        if(dead) {
            nvg.drawCenteredText("FLIGHT ENDED", x + width / 2.0F, cardY + 20,
                    new Color(255, 102, 125), 14, Fonts.SEMIBOLD);
            nvg.drawCenteredText("Score  " + score + "     Best  " + bestScore,
                    x + width / 2.0F, cardY + 48,
                    palette.getFontColor(ColorType.DARK), 9, Fonts.MEDIUM);
            nvg.drawCenteredText("Press SPACE, W, UP, ENTER, or click to retry",
                    x + width / 2.0F, cardY + 78,
                    alpha(palette.getFontColor(ColorType.NORMAL), 225), 8, Fonts.REGULAR);
            if(System.currentTimeMillis() - deathTime >= 350L) {
                nvg.drawRoundedRect(cardX + 58, cardY + 101, cardWidth - 116, 20, 7,
                        alpha(accent.getColor1(), 80 + (int) (pulse * 55.0F)));
                nvg.drawCenteredText("READY", x + width / 2.0F, cardY + 106,
                        Color.WHITE, 8, Fonts.SEMIBOLD);
            }
        } else {
            nvg.drawCenteredText("FLAPPY GLIDE", x + width / 2.0F, cardY + 20,
                    Color.WHITE, 15, Fonts.SEMIBOLD);
            nvg.drawCenteredText("Velocity-based movement with consistent timing",
                    x + width / 2.0F, cardY + 48,
                    palette.getFontColor(ColorType.NORMAL), 8, Fonts.REGULAR);
            nvg.drawRoundedRect(cardX + 46, cardY + 78, cardWidth - 92, 24, 8,
                    alpha(accent.getColor1(), 85 + (int) (pulse * 55.0F)));
            nvg.drawCenteredText("SPACE / CLICK TO FLY", x + width / 2.0F, cardY + 85,
                    Color.WHITE, 8.5F, Fonts.SEMIBOLD);
        }
    }

    private void spawnFlapParticles(AccentColor accent) {
        for(int i = 0; i < 8; i++) {
            float speed = 26.0F + random.nextFloat() * 58.0F;
            particles.add(new Particle(
                    playerX - PLAYER_SIZE * 0.45F,
                    y + playerY + (random.nextFloat() - 0.5F) * 7.0F,
                    -speed,
                    (random.nextFloat() - 0.5F) * 46.0F,
                    0.38F + random.nextFloat() * 0.24F,
                    1.6F + random.nextFloat() * 2.8F,
                    i % 2 == 0 ? accent.getColor1() : accent.getColor2()));
        }
    }

    private void spawnScoreParticles(AccentColor accent) {
        for(int i = 0; i < 12; i++) {
            float angle = (float) (Math.PI * 2.0 * i / 12.0);
            float speed = 22.0F + random.nextFloat() * 40.0F;
            particles.add(new Particle(
                    playerX,
                    y + playerY,
                    (float) Math.cos(angle) * speed,
                    (float) Math.sin(angle) * speed,
                    0.48F + random.nextFloat() * 0.24F,
                    1.4F + random.nextFloat() * 2.6F,
                    i % 2 == 0 ? accent.getColor1() : Color.WHITE));
        }
    }

    private void spawnDeathParticles(AccentColor accent) {
        for(int i = 0; i < 26; i++) {
            float angle = random.nextFloat() * (float) Math.PI * 2.0F;
            float speed = 45.0F + random.nextFloat() * 115.0F;
            particles.add(new Particle(
                    playerX,
                    y + playerY,
                    (float) Math.cos(angle) * speed,
                    (float) Math.sin(angle) * speed,
                    0.55F + random.nextFloat() * 0.45F,
                    1.8F + random.nextFloat() * 3.8F,
                    i % 3 == 0 ? Color.WHITE : (i % 2 == 0 ? accent.getColor1() : accent.getColor2())));
        }
    }

    private void updateParticles(float dt) {
        Iterator<Particle> iterator = particles.iterator();
        while(iterator.hasNext()) {
            Particle particle = iterator.next();
            particle.life -= dt;
            if(particle.life <= 0.0F) {
                iterator.remove();
                continue;
            }
            particle.x += particle.vx * dt;
            particle.y += particle.vy * dt;
            particle.vx *= (float) Math.pow(0.07F, dt);
            particle.vy += 70.0F * dt;
        }
    }

    private void drawParticles(NanoVGManager nvg) {
        for(Particle particle : particles) {
            float alpha = Math.max(0.0F, particle.life / particle.maxLife);
            nvg.drawCircle(particle.x, particle.y, particle.size * (0.55F + alpha * 0.45F),
                    alpha(particle.color, (int) (alpha * 205.0F)));
        }
    }

    private static Color alpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(),
                Math.max(0, Math.min(255, alpha)));
    }

    private static Color mix(Color a, Color b, float amount, int alpha) {
        float t = Math.max(0.0F, Math.min(1.0F, amount));
        int red = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
        int green = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int blue = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(red, green, blue, Math.max(0, Math.min(255, alpha)));
    }

    private static final class Pipe {
        private float x;
        private float gapTop;
        private boolean scored;
    }

    private static final class Particle {
        private float x;
        private float y;
        private float vx;
        private float vy;
        private float life;
        private final float maxLife;
        private final float size;
        private final Color color;

        private Particle(float x, float y, float vx, float vy, float life, float size, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = life;
            this.maxLife = life;
            this.size = size;
            this.color = color;
        }
    }
}
