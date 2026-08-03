from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def replace_once(text, old, new, label):
    if old not in text:
        raise RuntimeError("Missing replacement target: " + label)
    return text.replace(old, new, 1)


def replace_pattern(text, pattern, replacement, label):
    compiled = re.compile(pattern, re.S)
    if not compiled.search(text):
        raise RuntimeError("Missing regex target: " + label)
    return compiled.sub(replacement, text, count=1)


# ---------------------------------------------------------------------------
# Flappy Steve: softer fixed-step control, web-referenced Steve face pixels,
# and more Minecraft-like grass/dirt/lava textures.
# ---------------------------------------------------------------------------
bird_path = ROOT / "src/main/java/me/eldodebug/soar/gui/modmenu/category/impl/game/impl/BirdScene.java"
bird = bird_path.read_text(encoding="utf-8")

bird = replace_once(
    bird,
    "    private static final float GRAVITY = 520.0F;\n"
    "    private static final float JUMP_VELOCITY = -190.0F;\n"
    "    private static final float MAX_FALL_SPEED = 300.0F;\n"
    "    private static final float HEAD_SIZE = 19.0F;\n"
    "    private static final float PIPE_WIDTH = 38.0F;\n"
    "    private static final float PIPE_SPACING = 176.0F;\n"
    "    private static final float LAVA_HEIGHT = 22.0F;",
    "    private static final float RISE_GRAVITY = 405.0F;\n"
    "    private static final float FALL_GRAVITY = 485.0F;\n"
    "    private static final float JUMP_VELOCITY = -166.0F;\n"
    "    private static final float MAX_FALL_SPEED = 255.0F;\n"
    "    private static final float HEAD_SIZE = 22.0F;\n"
    "    private static final float PIPE_WIDTH = 42.0F;\n"
    "    private static final float PIPE_SPACING = 190.0F;\n"
    "    private static final float LAVA_HEIGHT = 26.0F;",
    "Flappy constants",
)

face_block = r'''

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
'''
bird = replace_once(bird, "    private static final float LAVA_HEIGHT = 26.0F;\n", "    private static final float LAVA_HEIGHT = 26.0F;" + face_block + "\n", "Steve face pixels")

bird = replace_once(
    bird,
    "    private float idleTime;\n    private float cloudTime;",
    "    private float idleTime;\n"
    "    private float cloudTime;\n"
    "    private float jumpCooldown;\n"
    "    private float cameraKick;\n"
    "    private final List<FlightParticle> flightParticles = new ArrayList<FlightParticle>();",
    "Flappy effect fields",
)

bird = replace_once(
    bird,
    '        super(parent, "Flappy Glide", "Smooth Steve-head flight through Minecraft terrain", LegacyIcon.PLAY);',
    '        super(parent, "Flappy Steve", "Smooth Steve flight through Minecraft terrain", LegacyIcon.PLAY);',
    "Flappy name",
)

bird = replace_once(
    bird,
    "        cloudTime += dt;\n        idleTime += dt;",
    "        cloudTime += dt;\n"
    "        idleTime += dt;\n"
    "        cameraKick = approach(cameraKick, 0.0F, 12.0F, dt);\n"
    "        updateFlightParticles(dt);",
    "Flappy frame effects",
)

bird = replace_once(
    bird,
    "        drawPipes(nvg, alpha);\n        drawLava(nvg);\n        drawSteveHead(nvg, renderPlayerY);",
    "        drawPipes(nvg, alpha);\n"
    "        drawLava(nvg);\n"
    "        drawFlightParticles(nvg);\n"
    "        drawSteveHead(nvg, renderPlayerY - cameraKick);",
    "Flappy particle draw",
)

bird = replace_once(
    bird,
    "        jumpBuffer = 0.0F;\n        velocityY = 0.0F;",
    "        jumpBuffer = 0.0F;\n"
    "        jumpCooldown = 0.0F;\n"
    "        cameraKick = 0.0F;\n"
    "        flightParticles.clear();\n"
    "        velocityY = 0.0F;",
    "Flappy title reset",
)

bird = replace_once(
    bird,
    "        accumulator = 0.0F;\n        velocityY = JUMP_VELOCITY;",
    "        accumulator = 0.0F;\n"
    "        jumpCooldown = 0.0F;\n"
    "        cameraKick = 2.2F;\n"
    "        flightParticles.clear();\n"
    "        velocityY = JUMP_VELOCITY;",
    "Flappy start reset",
)

bird = replace_once(bird, "        jumpBuffer = 0.12F;", "        jumpBuffer = 0.15F;", "Flappy input buffer")

bird = replace_pattern(
    bird,
    r"    private void updateFixed\(float step\) \{.*?\n    \}\n\n    private void recyclePipes",
    r'''    private void updateFixed(float step) {
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

    private void recyclePipes''',
    "Flappy fixed-step physics",
)

bird = replace_once(
    bird,
    "        return Math.max(72.0F, 96.0F - score * 0.65F);",
    "        return Math.max(78.0F, 104.0F - score * 0.58F);",
    "Flappy gap tuning",
)
bird = replace_once(bird, "        float half = HEAD_SIZE * 0.43F;", "        float half = HEAD_SIZE * 0.355F;", "Flappy hitbox")

bird = replace_pattern(
    bird,
    r"    private void drawTerrainColumn\(NanoVGManager nvg, float columnX,.*?\n    \}\n\n    private void drawLava",
    r'''    private void drawTerrainColumn(NanoVGManager nvg, float columnX,
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

    private void drawLava''',
    "Flappy terrain texture",
)

bird = replace_pattern(
    bird,
    r"    private void drawLava\(NanoVGManager nvg\) \{.*?\n    \}\n\n    private void drawSteveHead",
    r'''    private void drawLava(NanoVGManager nvg) {
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

    private void drawSteveHead''',
    "Flappy lava texture",
)

bird = replace_pattern(
    bird,
    r"    private void drawSteveHead\(NanoVGManager nvg, float localY\) \{.*?\n    \}\n\n    private void drawHud",
    r'''    private void drawSteveHead(NanoVGManager nvg, float localY) {
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

    private void drawHud''',
    "Steve web face rendering",
)

bird = bird.replace('String title = dead ? "YOU DIED" : "FLAPPY GLIDE";', 'String title = dead ? "YOU DIED" : "FLAPPY STEVE";')

bird = replace_once(
    bird,
    "    private static final class Pipe {",
    r'''    private void spawnJumpParticles() {
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

    private static final class Pipe {''',
    "Flappy helpers",
)

bird_path.write_text(bird, encoding="utf-8")


# ---------------------------------------------------------------------------
# Tetris: stronger line-clear, hard-drop, combo, flash, shake, shockwave and
# particle effects while keeping the existing controls and game rules.
# ---------------------------------------------------------------------------
tetris_path = ROOT / "src/main/java/me/eldodebug/soar/gui/modmenu/category/impl/game/impl/TetrisScene.java"
tetris = tetris_path.read_text(encoding="utf-8")

tetris = replace_once(
    tetris,
    "    private final List<Integer> bag = new ArrayList<Integer>();",
    "    private final List<Integer> bag = new ArrayList<Integer>();\n"
    "    private final List<EffectParticle> effectParticles = new ArrayList<EffectParticle>();",
    "Tetris particles field",
)

tetris = replace_once(
    tetris,
    "    private float horizontalRepeat;\n    private int horizontalDirection;",
    "    private float horizontalRepeat;\n"
    "    private int horizontalDirection;\n"
    "    private float screenFlash;\n"
    "    private float shockwave;\n"
    "    private float clearBanner;\n"
    "    private float boardShake;\n"
    "    private float backgroundPulse;\n"
    "    private float piecePulse;\n"
    "    private float dropTrail;\n"
    "    private float elapsed;\n"
    "    private int lastClearCount;\n"
    "    private int combo;\n"
    "    private int dropTrailType;\n"
    "    private int dropTrailRotation;\n"
    "    private int dropTrailX;\n"
    "    private int dropTrailFromY;\n"
    "    private int dropTrailToY;",
    "Tetris effect state",
)

old_draw = '''        syncLayout();
        float dt = frameDelta();
        pollInput(dt);
        if(started && !paused && !gameOver) {
            updateGame(dt);
        }
        visualX = anim(visualX, pieceX, 24.0F, dt);
        visualY = anim(visualY, pieceY, 22.0F, dt);

        Glide glide = Glide.getInstance();
        NanoVGManager nvg = glide.getNanoVGManager();
        ColorPalette palette = glide.getColorManager().getPalette();
        AccentColor accent = glide.getColorManager().getCurrentColor();

        nvg.save();
        nvg.scissor(x, y, width, height);
        drawBackground(nvg, palette, accent);
        drawBoard(nvg, palette, accent);
        drawSidebar(nvg, palette, accent);
        drawOverlay(nvg, palette, accent);
        nvg.restore();'''
new_draw = '''        syncLayout();
        float dt = frameDelta();
        elapsed += dt;
        pollInput(dt);
        if(started && !paused && !gameOver) {
            updateGame(dt);
        }
        visualX = anim(visualX, pieceX, 24.0F, dt);
        visualY = anim(visualY, pieceY, 22.0F, dt);
        updateEffects(dt);

        Glide glide = Glide.getInstance();
        NanoVGManager nvg = glide.getNanoVGManager();
        ColorPalette palette = glide.getColorManager().getPalette();
        AccentColor accent = glide.getColorManager().getCurrentColor();

        nvg.save();
        nvg.scissor(x, y, width, height);
        drawBackground(nvg, palette, accent);
        nvg.save();
        float shakeX = boardShake > 0.02F ? (random.nextFloat() - 0.5F) * boardShake : 0.0F;
        float shakeY = boardShake > 0.02F ? (random.nextFloat() - 0.5F) * boardShake : 0.0F;
        nvg.translate(shakeX, shakeY);
        drawBoard(nvg, palette, accent);
        drawEffectParticles(nvg);
        nvg.restore();
        drawSidebar(nvg, palette, accent);
        drawCelebrationOverlay(nvg, accent);
        drawOverlay(nvg, palette, accent);
        nvg.restore();'''
tetris = replace_once(tetris, old_draw, new_draw, "Tetris draw pipeline")

for marker in ["        lastFrameNanos = 0L;\n        resetInputEdges();", "        fillNext();\n        spawnPiece();\n        resetInputEdges();"]:
    if marker not in tetris:
        raise RuntimeError("Missing Tetris reset marker")

tetris = tetris.replace(
    "        lastFrameNanos = 0L;\n        resetInputEdges();",
    "        effectParticles.clear();\n"
    "        resetEffectState();\n"
    "        lastFrameNanos = 0L;\n"
    "        resetInputEdges();",
    1,
)
tetris = tetris.replace(
    "        fillNext();\n        spawnPiece();\n        resetInputEdges();",
    "        effectParticles.clear();\n"
    "        resetEffectState();\n"
    "        fillNext();\n"
    "        spawnPiece();\n"
    "        resetInputEdges();",
    1,
)

tetris = replace_once(
    tetris,
    "                rotation = target;\n                pieceX += kick;\n                lockTimer = 0.0F;",
    "                rotation = target;\n"
    "                pieceX += kick;\n"
    "                lockTimer = 0.0F;\n"
    "                piecePulse = Math.max(piecePulse, 0.72F);",
    "Tetris rotation pulse",
)

tetris = replace_pattern(
    tetris,
    r"    private void hardDrop\(\) \{.*?\n    \}\n\n    private void lockPiece",
    r'''    private void hardDrop() {
        int target = ghostY();
        int distance = Math.max(0, target - pieceY);
        dropTrailType = currentType;
        dropTrailRotation = rotation;
        dropTrailX = pieceX;
        dropTrailFromY = pieceY;
        dropTrailToY = target;
        dropTrail = distance > 0 ? 1.0F : 0.0F;
        score += distance * 2;
        pieceY = target;
        visualY = target;
        boardShake = Math.max(boardShake, 2.2F + Math.min(4.0F, distance * 0.18F));
        screenFlash = Math.max(screenFlash, 0.22F);
        spawnImpactParticles(target);
        lockPiece();
    }

    private void lockPiece''',
    "Tetris hard drop effects",
)

# Add a lock sparkle before line processing.
tetris = replace_once(
    tetris,
    "        clearLines();\n        spawnPiece();",
    "        spawnLockParticles();\n"
    "        clearLines();\n"
    "        spawnPiece();",
    "Tetris lock particles",
)

tetris = replace_pattern(
    tetris,
    r"    private void clearLines\(\) \{.*?\n    \}\n\n    private boolean collides",
    r'''    private void clearLines() {
        int cleared = 0;
        for(int row = ROWS - 1; row >= 0; row--) {
            boolean full = true;
            for(int col = 0; col < COLS; col++) {
                if(board[row][col] == 0) {
                    full = false;
                    break;
                }
            }
            if(!full) {
                continue;
            }

            spawnLineParticles(row);
            cleared++;
            for(int pull = row; pull > 0; pull--) {
                for(int col = 0; col < COLS; col++) {
                    board[pull][col] = board[pull - 1][col];
                }
            }
            for(int col = 0; col < COLS; col++) {
                board[0][col] = 0;
            }
            row++;
        }

        if(cleared > 0) {
            int[] rewards = {0, 100, 300, 500, 800};
            combo++;
            score += rewards[cleared] * level + Math.max(0, combo - 1) * 75 * level;
            lines += cleared;
            level = lines / 10 + 1;
            lastClearCount = cleared;
            clearBanner = 1.0F;
            screenFlash = Math.max(screenFlash, cleared == 4 ? 1.0F : 0.55F + cleared * 0.08F);
            shockwave = 1.0F;
            backgroundPulse = 1.0F;
            boardShake = Math.max(boardShake, 3.0F + cleared * 2.2F);
        } else {
            combo = 0;
        }
    }

    private boolean collides''',
    "Tetris line clear effects",
)

# Enhance background with animated particles and pulse.
tetris = replace_pattern(
    tetris,
    r"    private void drawBackground\(NanoVGManager nvg, ColorPalette palette, AccentColor accent\) \{.*?\n    \}\n\n    private void drawBoard",
    r'''    private void drawBackground(NanoVGManager nvg, ColorPalette palette, AccentColor accent) {
        Color base = palette.getBackgroundColor(ColorType.DARK);
        float pulse = Math.max(0.0F, backgroundPulse);
        nvg.drawVerticalGradientRect(x, y, width, height,
                mix(base, accent.getColor1(), 0.14F + pulse * 0.13F, 245),
                mix(base, accent.getColor2(), 0.05F + pulse * 0.09F, 245));
        for(int i = 0; i < 28; i++) {
            float px = x + ((i * 79.0F + elapsed * (8.0F + i % 5)) % (width + 24.0F)) - 12.0F;
            float py = y + (i * 47 % Math.max(20, height));
            int alpha = 11 + (i % 5) * 5 + (int)(pulse * 24.0F);
            nvg.drawCircle(px, py, 0.7F + (i % 3) * 0.35F,
                    new Color(255, 255, 255, Math.min(70, alpha)));
        }
    }

    private void drawBoard''',
    "Tetris animated background",
)

# Draw a bright trail through every grid position crossed by a hard drop.
tetris = replace_once(
    tetris,
    "        if(started && !gameOver) {\n            int ghost = ghostY();",
    "        if(dropTrail > 0.01F) {\n"
    "            for(int trailY = dropTrailFromY; trailY <= dropTrailToY; trailY++) {\n"
    "                for(int[] block : SHAPES[dropTrailType][dropTrailRotation]) {\n"
    "                    int gy = trailY + block[1];\n"
    "                    if(gy < 0 || gy >= ROWS) continue;\n"
    "                    float bx = boardX + (dropTrailX + block[0]) * cell;\n"
    "                    float by = boardY + gy * cell;\n"
    "                    nvg.drawRoundedRect(bx + 3.0F, by + 3.0F, cell - 6.0F, cell - 6.0F,\n"
    "                            2.0F, alpha(COLORS[dropTrailType], (int)(dropTrail * 34.0F)));\n"
    "                }\n"
    "            }\n"
    "        }\n"
    "        if(started && !gameOver) {\n"
    "            int ghost = ghostY();",
    "Tetris drop trail drawing",
)

# Add a pulse/glow over the active piece.
tetris = replace_once(
    tetris,
    "                drawBlock(nvg, bx, by, COLORS[currentType], 255);\n            }\n        }\n    }",
    "                drawBlock(nvg, bx, by, COLORS[currentType], 255);\n"
    "                if(piecePulse > 0.02F) {\n"
    "                    nvg.drawOutlineRoundedRect(bx - 1.5F, by - 1.5F, cell + 3.0F, cell + 3.0F,\n"
    "                            3.0F, 1.2F, alpha(Color.WHITE, (int)(piecePulse * 120.0F)));\n"
    "                }\n"
    "            }\n"
    "        }\n"
    "    }",
    "Tetris piece pulse drawing",
)

helpers = r'''

    private void resetEffectState() {
        screenFlash = 0.0F;
        shockwave = 0.0F;
        clearBanner = 0.0F;
        boardShake = 0.0F;
        backgroundPulse = 0.0F;
        piecePulse = 0.0F;
        dropTrail = 0.0F;
        elapsed = 0.0F;
        lastClearCount = 0;
        combo = 0;
    }

    private void updateEffects(float dt) {
        screenFlash = decay(screenFlash, 7.8F, dt);
        shockwave = decay(shockwave, 3.6F, dt);
        clearBanner = decay(clearBanner, 2.2F, dt);
        boardShake = decay(boardShake, 11.0F, dt);
        backgroundPulse = decay(backgroundPulse, 3.1F, dt);
        piecePulse = decay(piecePulse, 7.0F, dt);
        dropTrail = decay(dropTrail, 8.5F, dt);

        for(int i = effectParticles.size() - 1; i >= 0; i--) {
            EffectParticle particle = effectParticles.get(i);
            particle.life -= dt;
            if(particle.life <= 0.0F) {
                effectParticles.remove(i);
                continue;
            }
            particle.x += particle.vx * dt;
            particle.y += particle.vy * dt;
            particle.vy += 190.0F * dt;
            particle.vx *= (float)Math.pow(0.20F, dt);
        }
    }

    private float decay(float value, float speed, float dt) {
        return value * (float)Math.exp(-speed * dt);
    }

    private void spawnLineParticles(int row) {
        for(int col = 0; col < COLS; col++) {
            int value = board[row][col];
            Color color = value == 0 ? Color.WHITE : COLORS[value - 1];
            float centerX = boardX + (col + 0.5F) * cell;
            float centerY = boardY + (row + 0.5F) * cell;
            for(int i = 0; i < 4; i++) {
                effectParticles.add(new EffectParticle(centerX, centerY,
                        (random.nextFloat() - 0.5F) * 220.0F,
                        -65.0F - random.nextFloat() * 170.0F,
                        0.52F + random.nextFloat() * 0.48F,
                        1.4F + random.nextFloat() * 3.7F,
                        color));
            }
        }
    }

    private void spawnLockParticles() {
        for(int[] block : SHAPES[currentType][rotation]) {
            int gy = pieceY + block[1];
            if(gy < 0) continue;
            float centerX = boardX + (pieceX + block[0] + 0.5F) * cell;
            float centerY = boardY + (gy + 0.5F) * cell;
            for(int i = 0; i < 2; i++) {
                effectParticles.add(new EffectParticle(centerX, centerY,
                        (random.nextFloat() - 0.5F) * 72.0F,
                        -20.0F - random.nextFloat() * 45.0F,
                        0.24F + random.nextFloat() * 0.20F,
                        1.0F + random.nextFloat() * 2.0F,
                        COLORS[currentType]));
            }
        }
    }

    private void spawnImpactParticles(int targetY) {
        for(int[] block : SHAPES[currentType][rotation]) {
            int gy = targetY + block[1];
            if(gy < 0) continue;
            float centerX = boardX + (pieceX + block[0] + 0.5F) * cell;
            float centerY = boardY + (gy + 1.0F) * cell;
            for(int i = 0; i < 3; i++) {
                effectParticles.add(new EffectParticle(centerX, centerY,
                        (random.nextFloat() - 0.5F) * 135.0F,
                        -12.0F - random.nextFloat() * 80.0F,
                        0.30F + random.nextFloat() * 0.26F,
                        1.2F + random.nextFloat() * 2.7F,
                        COLORS[currentType]));
            }
        }
    }

    private void drawEffectParticles(NanoVGManager nvg) {
        for(EffectParticle particle : effectParticles) {
            float life = Math.max(0.0F, particle.life / particle.maxLife);
            float size = particle.size * (0.55F + life * 0.55F);
            nvg.drawRoundedRect(particle.x - size / 2.0F, particle.y - size / 2.0F,
                    size, size, 1.2F, alpha(particle.color, (int)(life * 235.0F)));
        }
    }

    private void drawCelebrationOverlay(NanoVGManager nvg, AccentColor accent) {
        if(shockwave > 0.01F) {
            float progress = 1.0F - shockwave;
            float expansion = 8.0F + progress * 34.0F;
            int alpha = (int)(shockwave * 150.0F);
            nvg.drawOutlineRoundedRect(boardX - expansion, boardY - expansion,
                    boardWidth + expansion * 2.0F, boardHeight + expansion * 2.0F,
                    13.0F + expansion * 0.18F, 1.5F,
                    alpha(accent.getColor1(), alpha));
            nvg.drawOutlineRoundedRect(boardX - expansion * 0.55F, boardY - expansion * 0.55F,
                    boardWidth + expansion * 1.10F, boardHeight + expansion * 1.10F,
                    12.0F, 1.0F, alpha(Color.WHITE, alpha / 2));
        }
        if(clearBanner > 0.01F) {
            String text = lastClearCount >= 4 ? "TETRIS!"
                    : lastClearCount == 3 ? "TRIPLE CLEAR"
                    : lastClearCount == 2 ? "DOUBLE CLEAR" : "LINE CLEAR";
            if(combo > 1) text += "  x" + combo;
            float bannerY = y + 18.0F - (1.0F - clearBanner) * 7.0F;
            int alpha = Math.min(255, (int)(clearBanner * 310.0F));
            nvg.drawRoundedRect(x + width / 2.0F - 88.0F, bannerY - 7.0F,
                    176.0F, 31.0F, 9.0F, new Color(3, 6, 14, Math.min(205, alpha)));
            nvg.drawCenteredText(text, x + width / 2.0F, bannerY,
                    lastClearCount >= 4 ? new Color(255, 226, 84, alpha)
                            : new Color(255, 255, 255, alpha),
                    lastClearCount >= 4 ? 14.5F : 11.5F, Fonts.SEMIBOLD);
        }
        if(screenFlash > 0.01F) {
            int alpha = Math.min(125, (int)(screenFlash * 120.0F));
            Color flash = lastClearCount >= 4
                    ? new Color(255, 225, 95, alpha)
                    : new Color(255, 255, 255, alpha);
            nvg.drawRoundedRect(x, y, width, height, 10.0F, flash);
        }
    }

    private static final class EffectParticle {
        private float x;
        private float y;
        private float vx;
        private float vy;
        private float life;
        private final float maxLife;
        private final float size;
        private final Color color;

        private EffectParticle(float x, float y, float vx, float vy,
                float life, float size, Color color) {
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
'''
tetris = replace_once(tetris, "\n    private void drawOverlay", helpers + "\n    private void drawOverlay", "Tetris helper methods")

tetris_path.write_text(tetris, encoding="utf-8")


# ---------------------------------------------------------------------------
# BedESP: persistent screen-space smoothing, off-screen rejection, larger icons,
# and an opaque black ClickGUI-style rounded panel.
# ---------------------------------------------------------------------------
bed_path = ROOT / "src/main/java/me/eldodebug/soar/management/mods/impl/BedESPMod.java"
bed = bed_path.read_text(encoding="utf-8")

bed = replace_once(
    bed,
    "import java.util.Collections;\nimport java.util.LinkedHashMap;",
    "import java.util.Collections;\n"
    "import java.util.HashMap;\n"
    "import java.util.HashSet;\n"
    "import java.util.LinkedHashMap;",
    "BedESP map imports",
)
bed = replace_once(
    bed,
    "import java.util.Map;",
    "import java.util.Map;\nimport java.util.Set;",
    "BedESP set import",
)

bed = replace_once(
    bed,
    "\tprivate static final int ICON_SIZE = 16;\n"
    "\tprivate static final int ICON_GAP = 2;\n"
    "\tprivate static final int PANEL_PADDING = 4;",
    "\tprivate static final int ICON_SIZE = 18;\n"
    "\tprivate static final int ICON_GAP = 3;\n"
    "\tprivate static final int PANEL_PADDING = 6;",
    "BedESP panel sizing",
)

bed = replace_once(
    bed,
    "\tprivate final List<Bed> beds = new ArrayList<Bed>();\n\tprivate int scanTimer;",
    "\tprivate final List<Bed> beds = new ArrayList<Bed>();\n"
    "\tprivate final Map<BlockPos, PanelMotion> panelMotions = new HashMap<BlockPos, PanelMotion>();\n"
    "\tprivate int scanTimer;\n"
    "\tprivate long lastPanelFrameNanos;",
    "BedESP motion fields",
)

bed = bed.replace("\t\tbeds.clear();\n\t\tscanTimer = 0;", "\t\tbeds.clear();\n\t\tpanelMotions.clear();\n\t\tlastPanelFrameNanos = 0L;\n\t\tscanTimer = 0;", 1)
bed = bed.replace("\t\tbeds.clear();\n\t}\n\n\t@EventTarget\n\tpublic void onUpdate", "\t\tbeds.clear();\n\t\tpanelMotions.clear();\n\t\tlastPanelFrameNanos = 0L;\n\t}\n\n\t@EventTarget\n\tpublic void onUpdate", 1)
bed = replace_once(
    bed,
    "\t\tif(mc.theWorld == null || mc.thePlayer == null) {\n\t\t\tbeds.clear();\n\t\t\treturn;",
    "\t\tif(mc.theWorld == null || mc.thePlayer == null) {\n"
    "\t\t\tbeds.clear();\n"
    "\t\t\tpanelMotions.clear();\n"
    "\t\t\tlastPanelFrameNanos = 0L;\n"
    "\t\t\treturn;",
    "BedESP world reset",
)

bed = replace_once(
    bed,
    "\t\tbeds.clear();\n\t\tbeds.addAll(found.values());",
    "\t\tbeds.clear();\n"
    "\t\tbeds.addAll(found.values());\n"
    "\t\tpanelMotions.keySet().retainAll(found.keySet());",
    "BedESP retain panel motion",
)

bed = replace_pattern(
    bed,
    r"\t@EventTarget\n\tpublic void onRender2D\(EventRender2D event\) \{.*?\n\tprivate static class DefenseIcon",
    r'''	@EventTarget
	public void onRender2D(EventRender2D event) {
		if(mc.theWorld == null || mc.thePlayer == null || beds.isEmpty()) return;
		if(!showBedColorSetting.isToggled() && !checkDefBlockSetting.isToggled()) return;

		ScaledResolution resolution = new ScaledResolution(mc);
		float dt = panelFrameDelta();
		List<ProjectedPanel> panels = new ArrayList<ProjectedPanel>();
		Set<BlockPos> activeKeys = new HashSet<BlockPos>();

		for(Bed bed : beds) {
			activeKeys.add(bed.footPos);
			List<ItemStack> icons = buildDisplayIcons(bed);
			if(icons.isEmpty()) continue;

			double centerX = (bed.box.minX + bed.box.maxX) / 2.0D;
			double centerY = bed.box.maxY + 0.96D;
			double centerZ = (bed.box.minZ + bed.box.maxZ) / 2.0D;
			float[] screen = WorldToScreen.project(centerX, centerY, centerZ);
			if(screen == null) continue;

			double distance = mc.thePlayer.getDistance(centerX, centerY, centerZ);
			if(distance > MAX_SCAN_CHUNKS * 16.0D + 16.0D) continue;

			int columns = Math.min(MAX_ICONS_PER_ROW, icons.size());
			int rows = (icons.size() + MAX_ICONS_PER_ROW - 1) / MAX_ICONS_PER_ROW;
			int panelWidth = columns * ICON_SIZE + Math.max(0, columns - 1) * ICON_GAP + PANEL_PADDING * 2;
			int panelHeight = rows * ICON_SIZE + Math.max(0, rows - 1) * ICON_GAP + PANEL_PADDING * 2;
			float scale = (float)Math.max(0.82D, Math.min(1.0D, 1.05D - distance / 560.0D));
			float scaledWidth = panelWidth * scale;
			float scaledHeight = panelHeight * scale;

			// Do not pin labels to screen edges. Edge-clamping made panels jump and
			// slide along the border when the camera crossed behind a bed.
			float margin = Math.max(28.0F, scaledWidth * 0.65F);
			if(screen[0] < -margin || screen[0] > resolution.getScaledWidth() + margin
					|| screen[1] < -margin || screen[1] > resolution.getScaledHeight() + margin) {
				continue;
			}

			float targetX = screen[0];
			float targetBottomY = screen[1];
			PanelMotion motion = panelMotions.get(bed.footPos);
			if(motion == null) {
				motion = new PanelMotion(targetX, targetBottomY, scale);
				panelMotions.put(bed.footPos, motion);
			} else {
				motion.update(targetX, targetBottomY, scale, dt);
			}
			panels.add(new ProjectedPanel(motion.x, motion.y, motion.scale,
					distance, icons));
		}

		panelMotions.keySet().retainAll(activeKeys);
		Collections.sort(panels, (first, second) -> Double.compare(second.distance, first.distance));
		for(ProjectedPanel panel : panels) {
			renderProjectedIcons(panel, resolution);
		}
	}

	private float panelFrameDelta() {
		long now = System.nanoTime();
		if(lastPanelFrameNanos == 0L) {
			lastPanelFrameNanos = now;
			return 1.0F / 60.0F;
		}
		float dt = (now - lastPanelFrameNanos) / 1000000000.0F;
		lastPanelFrameNanos = now;
		return Math.max(0.0F, Math.min(0.05F, dt));
	}

	private List<ItemStack> buildDisplayIcons(Bed bed) {
		List<ItemStack> icons = new ArrayList<ItemStack>();
		if(showBedColorSetting.isToggled()) icons.add(bed.bedStack);
		if(checkDefBlockSetting.isToggled()) icons.addAll(bed.defenseStacks);
		return icons;
	}

	private void renderProjectedIcons(ProjectedPanel panel, ScaledResolution resolution) {
		List<ItemStack> icons = panel.icons;
		int columns = Math.min(MAX_ICONS_PER_ROW, icons.size());
		int rows = (icons.size() + MAX_ICONS_PER_ROW - 1) / MAX_ICONS_PER_ROW;
		int contentWidth = columns * ICON_SIZE + Math.max(0, columns - 1) * ICON_GAP;
		int panelWidth = contentWidth + PANEL_PADDING * 2;
		int panelHeight = rows * ICON_SIZE + Math.max(0, rows - 1) * ICON_GAP + PANEL_PADDING * 2;

		float scale = panel.scale;
		float scaledWidth = panelWidth * scale;
		float scaledHeight = panelHeight * scale;
		float centerX = Math.max(scaledWidth / 2.0F + 3.0F,
				Math.min(resolution.getScaledWidth() - scaledWidth / 2.0F - 3.0F, panel.screenX));
		float bottomY = Math.max(scaledHeight + 3.0F,
				Math.min(resolution.getScaledHeight() - 3.0F, panel.screenY));

		GlStateManager.pushMatrix();
		try {
			GlStateManager.translate(centerX, bottomY - scaledHeight, 0.0F);
			GlStateManager.scale(scale, scale, 1.0F);
			GlStateManager.disableLighting();
			GlStateManager.disableDepth();
			GlStateManager.depthMask(false);
			GlStateManager.enableBlend();
			GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
			GlStateManager.enableAlpha();
			GlStateManager.enableTexture2D();
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

			// Opaque rounded black panel matching the ClickGUI glass-card shape.
			RenderUtils.drawRoundedRect(-panelWidth / 2.0F - 2.5F, -2.5F,
					panelWidth + 5.0F, panelHeight + 5.0F, 7.0F, new Color(0, 0, 0, 255));
			RenderUtils.drawRoundedRect(-panelWidth / 2.0F, 0.0F,
					panelWidth, panelHeight, 6.0F, new Color(5, 5, 7, 248));
			RenderUtils.drawRoundedOutline(-panelWidth / 2.0F, 0.0F,
					panelWidth, panelHeight, 6.0F, 1.0F, new Color(0, 0, 0, 255));

			GlStateManager.enableRescaleNormal();
			GlStateManager.enableColorMaterial();
			RenderHelper.enableGUIStandardItemLighting();
			float oldZLevel = mc.getRenderItem().zLevel;
			mc.getRenderItem().zLevel = 220.0F;
			try {
				for(int i = 0; i < icons.size(); i++) {
					int row = i / MAX_ICONS_PER_ROW;
					int column = i % MAX_ICONS_PER_ROW;
					int itemsInRow = Math.min(MAX_ICONS_PER_ROW, icons.size() - row * MAX_ICONS_PER_ROW);
					int rowWidth = itemsInRow * ICON_SIZE + Math.max(0, itemsInRow - 1) * ICON_GAP;
					int slotX = -rowWidth / 2 + column * (ICON_SIZE + ICON_GAP);
					int slotY = PANEL_PADDING + row * (ICON_SIZE + ICON_GAP);
					RenderUtils.drawRoundedRect(slotX - 1.0F, slotY - 1.0F,
							ICON_SIZE + 2.0F, ICON_SIZE + 2.0F, 4.0F,
							new Color(0, 0, 0, 230));
					RenderUtils.drawRoundedOutline(slotX - 1.0F, slotY - 1.0F,
							ICON_SIZE + 2.0F, ICON_SIZE + 2.0F, 4.0F, 0.6F,
							new Color(42, 42, 46, 235));
					GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
					mc.getRenderItem().renderItemAndEffectIntoGUI(icons.get(i), slotX + 1, slotY + 1);
				}
			} finally {
				mc.getRenderItem().zLevel = oldZLevel;
				RenderHelper.disableStandardItemLighting();
				GlStateManager.disableRescaleNormal();
				GlStateManager.disableColorMaterial();
			}
		} finally {
			GlStateManager.depthMask(true);
			GlStateManager.enableDepth();
			GlStateManager.disableBlend();
			GlStateManager.enableLighting();
			GlStateManager.enableTexture2D();
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			GlStateManager.popMatrix();
		}
	}

	private static class ProjectedPanel {
		private final float screenX, screenY, scale;
		private final double distance;
		private final List<ItemStack> icons;

		private ProjectedPanel(float screenX, float screenY, float scale,
				double distance, List<ItemStack> icons) {
			this.screenX = screenX;
			this.screenY = screenY;
			this.scale = scale;
			this.distance = distance;
			this.icons = icons;
		}
	}

	private static class PanelMotion {
		private float x, y, scale;

		private PanelMotion(float x, float y, float scale) {
			this.x = x;
			this.y = y;
			this.scale = scale;
		}

		private void update(float targetX, float targetY, float targetScale, float dt) {
			float positionFactor = 1.0F - (float)Math.exp(-18.0F * dt);
			float scaleFactor = 1.0F - (float)Math.exp(-12.0F * dt);
			x += (targetX - x) * positionFactor;
			y += (targetY - y) * positionFactor;
			scale += (targetScale - scale) * scaleFactor;
		}
	}

	private static class DefenseIcon''',
    "BedESP projected panel redesign",
)

bed_path.write_text(bed, encoding="utf-8")

print("Applied Flappy Steve, Tetris effects, and BedESP readability/smoothing polish")
