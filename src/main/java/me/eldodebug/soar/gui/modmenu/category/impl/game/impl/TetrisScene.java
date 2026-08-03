package me.eldodebug.soar.gui.modmenu.category.impl.game.impl;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.lwjgl.input.Keyboard;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.gui.modmenu.category.impl.GamesCategory;
import me.eldodebug.soar.gui.modmenu.category.impl.game.GameScene;
import me.eldodebug.soar.management.color.AccentColor;
import me.eldodebug.soar.management.color.palette.ColorPalette;
import me.eldodebug.soar.management.color.palette.ColorType;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.management.nanovg.font.LegacyIcon;
import me.eldodebug.soar.utils.mouse.MouseUtils;

public class TetrisScene extends GameScene {

    private static final int COLS = 10;
    private static final int ROWS = 20;
    private static final float LOCK_DELAY = 0.42F;
    private static final float DAS = 0.14F;
    private static final float ARR = 0.045F;

    private static final int[][][][] SHAPES = {
            {
                    {{0,1},{1,1},{2,1},{3,1}}, {{2,0},{2,1},{2,2},{2,3}},
                    {{0,2},{1,2},{2,2},{3,2}}, {{1,0},{1,1},{1,2},{1,3}}
            },
            {
                    {{0,0},{0,1},{1,1},{2,1}}, {{1,0},{2,0},{1,1},{1,2}},
                    {{0,1},{1,1},{2,1},{2,2}}, {{1,0},{1,1},{0,2},{1,2}}
            },
            {
                    {{2,0},{0,1},{1,1},{2,1}}, {{1,0},{1,1},{1,2},{2,2}},
                    {{0,1},{1,1},{2,1},{0,2}}, {{0,0},{1,0},{1,1},{1,2}}
            },
            {
                    {{1,0},{2,0},{1,1},{2,1}}, {{1,0},{2,0},{1,1},{2,1}},
                    {{1,0},{2,0},{1,1},{2,1}}, {{1,0},{2,0},{1,1},{2,1}}
            },
            {
                    {{1,0},{2,0},{0,1},{1,1}}, {{1,0},{1,1},{2,1},{2,2}},
                    {{1,1},{2,1},{0,2},{1,2}}, {{0,0},{0,1},{1,1},{1,2}}
            },
            {
                    {{1,0},{0,1},{1,1},{2,1}}, {{1,0},{1,1},{2,1},{1,2}},
                    {{0,1},{1,1},{2,1},{1,2}}, {{1,0},{0,1},{1,1},{1,2}}
            },
            {
                    {{0,0},{1,0},{1,1},{2,1}}, {{2,0},{1,1},{2,1},{1,2}},
                    {{0,1},{1,1},{1,2},{2,2}}, {{1,0},{0,1},{1,1},{0,2}}
            }
    };

    private static final Color[] COLORS = {
            new Color(65, 220, 244), new Color(68, 105, 231),
            new Color(245, 145, 45), new Color(244, 213, 52),
            new Color(70, 205, 104), new Color(166, 83, 220),
            new Color(232, 68, 86)
    };

    private final int[][] board = new int[ROWS][COLS];
    private final Random random = new Random();
    private final ArrayDeque<Integer> next = new ArrayDeque<Integer>();
    private final List<Integer> bag = new ArrayList<Integer>();
    private final List<EffectParticle> effectParticles = new ArrayList<EffectParticle>();

    private int currentType;
    private int rotation;
    private int pieceX;
    private int pieceY;
    private int score;
    private int lines;
    private int level;

    private boolean started;
    private boolean paused;
    private boolean gameOver;

    private float fallTimer;
    private float lockTimer;
    private float visualX;
    private float visualY;
    private float horizontalHeld;
    private float horizontalRepeat;
    private int horizontalDirection;
    private float screenFlash;
    private float shockwave;
    private float clearBanner;
    private float boardShake;
    private float backgroundPulse;
    private float piecePulse;
    private float dropTrail;
    private float elapsed;
    private int lastClearCount;
    private int combo;
    private int dropTrailType;
    private int dropTrailRotation;
    private int dropTrailX;
    private int dropTrailFromY;
    private int dropTrailToY;

    private boolean rotateWasDown;
    private boolean rotateBackWasDown;
    private boolean hardDropWasDown;
    private boolean pauseWasDown;
    private boolean restartWasDown;
    private boolean enterWasDown;

    private long lastFrameNanos;
    private int x;
    private int y;
    private int width;
    private int height;
    private float cell;
    private float boardX;
    private float boardY;
    private float boardWidth;
    private float boardHeight;

    public TetrisScene(GamesCategory parent) {
        super(parent, "Tetris", "Classic falling blocks with smooth controls and a ghost piece", LegacyIcon.GRID);
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
        nvg.restore();
        nvg.drawOutlineRoundedRect(x, y, width, height, 10.0F, 1.0F,
                new Color(255, 255, 255, 55));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if(mouseButton != 0 || !MouseUtils.isInside(mouseX, mouseY, x, y, width, height)) {
            return;
        }
        if(!started || gameOver) {
            startGame();
        } else if(paused) {
            paused = false;
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        // Movement is polled each frame so held keys remain smooth and predictable.
    }

    private void syncLayout() {
        x = getX();
        y = getY();
        width = getWidth();
        height = getHeight();
        cell = (float)Math.floor(Math.min((height - 20.0F) / ROWS,
                Math.max(9.0F, width * 0.48F / COLS)));
        cell = Math.max(9.0F, Math.min(16.0F, cell));
        boardWidth = COLS * cell;
        boardHeight = ROWS * cell;
        boardX = x + width * 0.46F - boardWidth / 2.0F;
        boardY = y + (height - boardHeight) / 2.0F;
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
        clearBoard();
        started = false;
        paused = false;
        gameOver = false;
        score = 0;
        lines = 0;
        level = 1;
        effectParticles.clear();
        resetEffectState();
        lastFrameNanos = 0L;
        resetInputEdges();
    }

    private void startGame() {
        clearBoard();
        bag.clear();
        next.clear();
        score = 0;
        lines = 0;
        level = 1;
        fallTimer = 0.0F;
        lockTimer = 0.0F;
        started = true;
        paused = false;
        gameOver = false;
        effectParticles.clear();
        resetEffectState();
        fillNext();
        spawnPiece();
        resetInputEdges();
    }

    private void clearBoard() {
        for(int row = 0; row < ROWS; row++) {
            for(int col = 0; col < COLS; col++) {
                board[row][col] = 0;
            }
        }
    }

    private void fillNext() {
        while(next.size() < 4) {
            next.addLast(Integer.valueOf(nextFromBag()));
        }
    }

    private int nextFromBag() {
        if(bag.isEmpty()) {
            for(int i = 0; i < 7; i++) {
                bag.add(Integer.valueOf(i));
            }
            Collections.shuffle(bag, random);
        }
        return bag.remove(bag.size() - 1).intValue();
    }

    private void spawnPiece() {
        fillNext();
        currentType = next.removeFirst().intValue();
        fillNext();
        rotation = 0;
        pieceX = 3;
        pieceY = -1;
        visualX = pieceX;
        visualY = pieceY - 0.5F;
        fallTimer = 0.0F;
        lockTimer = 0.0F;
        if(collides(currentType, rotation, pieceX, pieceY)) {
            gameOver = true;
        }
    }

    private void pollInput(float dt) {
        boolean left = Keyboard.isKeyDown(Keyboard.KEY_LEFT) || Keyboard.isKeyDown(Keyboard.KEY_A);
        boolean right = Keyboard.isKeyDown(Keyboard.KEY_RIGHT) || Keyboard.isKeyDown(Keyboard.KEY_D);
        boolean down = Keyboard.isKeyDown(Keyboard.KEY_DOWN) || Keyboard.isKeyDown(Keyboard.KEY_S);
        boolean rotate = Keyboard.isKeyDown(Keyboard.KEY_UP) || Keyboard.isKeyDown(Keyboard.KEY_W)
                || Keyboard.isKeyDown(Keyboard.KEY_X);
        boolean rotateBack = Keyboard.isKeyDown(Keyboard.KEY_Z);
        boolean hardDrop = Keyboard.isKeyDown(Keyboard.KEY_SPACE);
        boolean pause = Keyboard.isKeyDown(Keyboard.KEY_P);
        boolean restart = Keyboard.isKeyDown(Keyboard.KEY_R);
        boolean enter = Keyboard.isKeyDown(Keyboard.KEY_RETURN);

        if((!started || gameOver) && ((!hardDropWasDown && hardDrop) || (!enterWasDown && enter))) {
            startGame();
            rememberEdges(rotate, rotateBack, hardDrop, pause, restart, enter);
            return;
        }
        if(started && !restartWasDown && restart) {
            startGame();
            rememberEdges(rotate, rotateBack, hardDrop, pause, restart, enter);
            return;
        }
        if(started && !gameOver && !pauseWasDown && pause) {
            paused = !paused;
        }

        if(started && !gameOver && !paused) {
            int direction = left == right ? 0 : (left ? -1 : 1);
            handleHorizontal(direction, dt);
            if(down && tryMove(0, 1)) {
                score++;
                fallTimer = 0.0F;
            }
            if(rotate && !rotateWasDown) {
                rotatePiece(1);
            }
            if(rotateBack && !rotateBackWasDown) {
                rotatePiece(-1);
            }
            if(hardDrop && !hardDropWasDown) {
                hardDrop();
            }
        } else {
            horizontalDirection = 0;
            horizontalHeld = 0.0F;
            horizontalRepeat = 0.0F;
        }
        rememberEdges(rotate, rotateBack, hardDrop, pause, restart, enter);
    }

    private void handleHorizontal(int direction, float dt) {
        if(direction == 0) {
            horizontalDirection = 0;
            horizontalHeld = 0.0F;
            horizontalRepeat = 0.0F;
            return;
        }
        if(direction != horizontalDirection) {
            horizontalDirection = direction;
            horizontalHeld = 0.0F;
            horizontalRepeat = 0.0F;
            tryMove(direction, 0);
            return;
        }
        horizontalHeld += dt;
        if(horizontalHeld >= DAS) {
            horizontalRepeat += dt;
            while(horizontalRepeat >= ARR) {
                tryMove(direction, 0);
                horizontalRepeat -= ARR;
            }
        }
    }

    private void rememberEdges(boolean rotate, boolean rotateBack, boolean hardDrop,
            boolean pause, boolean restart, boolean enter) {
        rotateWasDown = rotate;
        rotateBackWasDown = rotateBack;
        hardDropWasDown = hardDrop;
        pauseWasDown = pause;
        restartWasDown = restart;
        enterWasDown = enter;
    }

    private void resetInputEdges() {
        rememberEdges(
                Keyboard.isKeyDown(Keyboard.KEY_UP) || Keyboard.isKeyDown(Keyboard.KEY_W)
                        || Keyboard.isKeyDown(Keyboard.KEY_X),
                Keyboard.isKeyDown(Keyboard.KEY_Z),
                Keyboard.isKeyDown(Keyboard.KEY_SPACE),
                Keyboard.isKeyDown(Keyboard.KEY_P),
                Keyboard.isKeyDown(Keyboard.KEY_R),
                Keyboard.isKeyDown(Keyboard.KEY_RETURN));
    }

    private void updateGame(float dt) {
        fallTimer += dt;
        float interval = Math.max(0.07F, 0.72F - (level - 1) * 0.055F);
        while(fallTimer >= interval) {
            fallTimer -= interval;
            if(!tryMove(0, 1)) {
                break;
            }
        }
        if(collides(currentType, rotation, pieceX, pieceY + 1)) {
            lockTimer += dt;
            if(lockTimer >= LOCK_DELAY) {
                lockPiece();
            }
        } else {
            lockTimer = 0.0F;
        }
    }

    private boolean tryMove(int dx, int dy) {
        if(collides(currentType, rotation, pieceX + dx, pieceY + dy)) {
            return false;
        }
        pieceX += dx;
        pieceY += dy;
        if(dy == 0) {
            lockTimer = 0.0F;
        }
        return true;
    }

    private void rotatePiece(int direction) {
        int target = (rotation + direction + 4) % 4;
        int[] kicks = {0, -1, 1, -2, 2};
        for(int kick : kicks) {
            if(!collides(currentType, target, pieceX + kick, pieceY)) {
                rotation = target;
                pieceX += kick;
                lockTimer = 0.0F;
                piecePulse = Math.max(piecePulse, 0.72F);
                return;
            }
        }
    }

    private void hardDrop() {
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

    private void lockPiece() {
        for(int[] block : SHAPES[currentType][rotation]) {
            int bx = pieceX + block[0];
            int by = pieceY + block[1];
            if(by < 0) {
                gameOver = true;
                return;
            }
            board[by][bx] = currentType + 1;
        }
        spawnLockParticles();
        clearLines();
        spawnPiece();
    }

    private void clearLines() {
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

    private boolean collides(int type, int targetRotation, int targetX, int targetY) {
        for(int[] block : SHAPES[type][targetRotation]) {
            int bx = targetX + block[0];
            int by = targetY + block[1];
            if(bx < 0 || bx >= COLS || by >= ROWS) {
                return true;
            }
            if(by >= 0 && board[by][bx] != 0) {
                return true;
            }
        }
        return false;
    }

    private int ghostY() {
        int target = pieceY;
        while(!collides(currentType, rotation, pieceX, target + 1)) {
            target++;
        }
        return target;
    }

    private void drawBackground(NanoVGManager nvg, ColorPalette palette, AccentColor accent) {
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

    private void drawBoard(NanoVGManager nvg, ColorPalette palette, AccentColor accent) {
        nvg.drawShadow(boardX - 5.0F, boardY - 5.0F, boardWidth + 10.0F, boardHeight + 10.0F,
                10.0F, 7);
        nvg.drawRoundedRect(boardX - 5.0F, boardY - 5.0F, boardWidth + 10.0F,
                boardHeight + 10.0F, 10.0F, new Color(7, 10, 18, 226));
        nvg.drawOutlineRoundedRect(boardX - 4.5F, boardY - 4.5F,
                boardWidth + 9.0F, boardHeight + 9.0F, 10.0F, 0.8F,
                new Color(accent.getColor1().getRed(), accent.getColor1().getGreen(),
                        accent.getColor1().getBlue(), 100));
        for(int row = 0; row < ROWS; row++) {
            for(int col = 0; col < COLS; col++) {
                float bx = boardX + col * cell;
                float by = boardY + row * cell;
                nvg.drawRect(bx + 0.5F, by + 0.5F, cell - 1.0F, cell - 1.0F,
                        new Color(255, 255, 255, 8));
                if(board[row][col] != 0) {
                    drawBlock(nvg, bx, by, COLORS[board[row][col] - 1], 255);
                }
            }
        }
        if(dropTrail > 0.01F) {
            for(int trailY = dropTrailFromY; trailY <= dropTrailToY; trailY++) {
                for(int[] block : SHAPES[dropTrailType][dropTrailRotation]) {
                    int gy = trailY + block[1];
                    if(gy < 0 || gy >= ROWS) continue;
                    float bx = boardX + (dropTrailX + block[0]) * cell;
                    float by = boardY + gy * cell;
                    nvg.drawRoundedRect(bx + 3.0F, by + 3.0F, cell - 6.0F, cell - 6.0F,
                            2.0F, alpha(COLORS[dropTrailType], (int)(dropTrail * 34.0F)));
                }
            }
        }
        if(started && !gameOver) {
            int ghost = ghostY();
            for(int[] block : SHAPES[currentType][rotation]) {
                int gy = ghost + block[1];
                if(gy < 0) {
                    continue;
                }
                float bx = boardX + (pieceX + block[0]) * cell;
                float by = boardY + gy * cell;
                nvg.drawOutlineRoundedRect(bx + 2.0F, by + 2.0F, cell - 4.0F, cell - 4.0F,
                        2.0F, 1.0F, alpha(COLORS[currentType], 95));
            }
            for(int[] block : SHAPES[currentType][rotation]) {
                float gy = visualY + block[1];
                if(gy < 0.0F) {
                    continue;
                }
                float bx = boardX + (visualX + block[0]) * cell;
                float by = boardY + gy * cell;
                drawBlock(nvg, bx, by, COLORS[currentType], 255);
                if(piecePulse > 0.02F) {
                    nvg.drawOutlineRoundedRect(bx - 1.5F, by - 1.5F, cell + 3.0F, cell + 3.0F,
                            3.0F, 1.2F, alpha(Color.WHITE, (int)(piecePulse * 120.0F)));
                }
            }
        }
    }

    private void drawBlock(NanoVGManager nvg, float bx, float by, Color color, int opacity) {
        Color light = mix(color, Color.WHITE, 0.18F, opacity);
        Color dark = mix(color, Color.BLACK, 0.22F, opacity);
        nvg.drawVerticalGradientRect(bx + 0.6F, by + 0.6F, cell - 1.2F, cell - 1.2F,
                light, dark);
        nvg.drawRect(bx + 1.5F, by + 1.5F, cell - 3.0F, Math.max(1.0F, cell * 0.10F),
                new Color(255, 255, 255, Math.min(70, opacity)));
        nvg.drawOutlineRoundedRect(bx + 0.6F, by + 0.6F, cell - 1.2F, cell - 1.2F,
                1.8F, 0.7F, new Color(0, 0, 0, Math.min(95, opacity)));
    }

    private void drawSidebar(NanoVGManager nvg, ColorPalette palette, AccentColor accent) {
        float panelX = boardX + boardWidth + 15.0F;
        float panelWidth = Math.max(92.0F, x + width - panelX - 12.0F);
        nvg.drawRoundedRect(panelX, boardY, panelWidth, boardHeight, 10.0F,
                new Color(8, 11, 20, 155));
        nvg.drawOutlineRoundedRect(panelX + 0.5F, boardY + 0.5F, panelWidth - 1.0F,
                boardHeight - 1.0F, 10.0F, 0.7F, new Color(255, 255, 255, 28));
        nvg.drawCenteredText("TETRIS", panelX + panelWidth / 2.0F, boardY + 12.0F,
                Color.WHITE, 12.0F, Fonts.SEMIBOLD);
        drawStat(nvg, palette, panelX, panelWidth, boardY + 43.0F, "SCORE", score);
        drawStat(nvg, palette, panelX, panelWidth, boardY + 82.0F, "LINES", lines);
        drawStat(nvg, palette, panelX, panelWidth, boardY + 121.0F, "LEVEL", level);

        nvg.drawCenteredText("NEXT", panelX + panelWidth / 2.0F, boardY + 164.0F,
                alpha(accent.getColor1(), 230), 7.5F, Fonts.SEMIBOLD);
        Integer nextType = next.peekFirst();
        if(nextType != null) {
            drawMiniPiece(nvg, nextType.intValue(), panelX + panelWidth / 2.0F,
                    boardY + 201.0F, Math.max(5.0F, cell * 0.55F));
        }

        float controlsY = boardY + boardHeight - 91.0F;
        nvg.drawText("A/D or arrows: move", panelX + 10.0F, controlsY,
                palette.getFontColor(ColorType.NORMAL), 6.5F, Fonts.REGULAR);
        nvg.drawText("W/X: rotate   Z: back", panelX + 10.0F, controlsY + 15.0F,
                palette.getFontColor(ColorType.NORMAL), 6.5F, Fonts.REGULAR);
        nvg.drawText("S: soft drop", panelX + 10.0F, controlsY + 30.0F,
                palette.getFontColor(ColorType.NORMAL), 6.5F, Fonts.REGULAR);
        nvg.drawText("SPACE: hard drop", panelX + 10.0F, controlsY + 45.0F,
                palette.getFontColor(ColorType.NORMAL), 6.5F, Fonts.REGULAR);
        nvg.drawText("P: pause   R: restart", panelX + 10.0F, controlsY + 60.0F,
                palette.getFontColor(ColorType.NORMAL), 6.5F, Fonts.REGULAR);
    }

    private void drawStat(NanoVGManager nvg, ColorPalette palette, float panelX,
            float panelWidth, float statY, String label, int value) {
        nvg.drawCenteredText(label, panelX + panelWidth / 2.0F, statY,
                palette.getFontColor(ColorType.NORMAL), 7.0F, Fonts.MEDIUM);
        nvg.drawCenteredText(String.valueOf(value), panelX + panelWidth / 2.0F, statY + 14.0F,
                Color.WHITE, 11.0F, Fonts.SEMIBOLD);
    }

    private void drawMiniPiece(NanoVGManager nvg, int type, float centerX, float centerY,
            float size) {
        int minX = 4;
        int maxX = 0;
        int minY = 4;
        int maxY = 0;
        for(int[] block : SHAPES[type][0]) {
            minX = Math.min(minX, block[0]);
            maxX = Math.max(maxX, block[0]);
            minY = Math.min(minY, block[1]);
            maxY = Math.max(maxY, block[1]);
        }
        float startX = centerX - (maxX - minX + 1) * size / 2.0F - minX * size;
        float startY = centerY - (maxY - minY + 1) * size / 2.0F - minY * size;
        for(int[] block : SHAPES[type][0]) {
            Color color = COLORS[type];
            nvg.drawVerticalGradientRect(startX + block[0] * size,
                    startY + block[1] * size, size - 0.7F, size - 0.7F,
                    mix(color, Color.WHITE, 0.18F, 255),
                    mix(color, Color.BLACK, 0.20F, 255));
        }
    }


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

    private void drawOverlay(NanoVGManager nvg, ColorPalette palette, AccentColor accent) {
        if(started && !paused && !gameOver) {
            return;
        }
        float cardWidth = Math.min(300.0F, width - 50.0F);
        float cardHeight = 112.0F;
        float cardX = x + (width - cardWidth) / 2.0F;
        float cardY = y + (height - cardHeight) / 2.0F;
        nvg.drawRoundedRect(x, y, width, height, 10.0F, new Color(3, 6, 12, 128));
        nvg.drawShadow(cardX, cardY, cardWidth, cardHeight, 13.0F, 8);
        nvg.drawRoundedRect(cardX, cardY, cardWidth, cardHeight, 13.0F,
                new Color(8, 12, 23, 232));
        nvg.drawOutlineRoundedRect(cardX + 0.5F, cardY + 0.5F, cardWidth - 1.0F,
                cardHeight - 1.0F, 13.0F, 0.9F, alpha(accent.getColor1(), 150));
        String title = !started ? "TETRIS" : (paused ? "PAUSED" : "GAME OVER");
        Color titleColor = gameOver ? new Color(255, 92, 111) : Color.WHITE;
        nvg.drawCenteredText(title, x + width / 2.0F, cardY + 20.0F,
                titleColor, 17.0F, Fonts.SEMIBOLD);
        String message = !started ? "Press ENTER, SPACE, or click to start"
                : (paused ? "Press P or click to continue"
                : "Score " + score + "  -  press ENTER, SPACE, or click");
        nvg.drawCenteredText(message, x + width / 2.0F, cardY + 60.0F,
                palette.getFontColor(ColorType.NORMAL), 8.0F, Fonts.MEDIUM);
        nvg.drawCenteredText("R restarts at any time", x + width / 2.0F, cardY + 82.0F,
                palette.getFontColor(ColorType.NORMAL), 7.0F, Fonts.REGULAR);
    }

    private static Color alpha(Color color, int opacity) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(),
                Math.max(0, Math.min(255, opacity)));
    }

    private static Color mix(Color first, Color second, float amount, int opacity) {
        float t = Math.max(0.0F, Math.min(1.0F, amount));
        return new Color(
                (int)(first.getRed() + (second.getRed() - first.getRed()) * t),
                (int)(first.getGreen() + (second.getGreen() - first.getGreen()) * t),
                (int)(first.getBlue() + (second.getBlue() - first.getBlue()) * t),
                Math.max(0, Math.min(255, opacity)));
    }
}
