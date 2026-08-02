package me.eldodebug.soar.gui.modmenu.category.impl.game.impl;

import java.awt.Color;
import java.util.ArrayDeque;
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

public class TetrisScene extends GameScene {

    private static final int COLUMNS = 10;
    private static final int ROWS = 20;
    private static final float LOCK_DELAY = 0.46F;
    private static final float CLEAR_DURATION = 0.28F;
    private static final float DAS = 0.145F;
    private static final float ARR = 0.042F;
    private static final float SOFT_DROP_REPEAT = 0.045F;

    private static final int[][][][] SHAPES = {
            {
                    {{0, 1}, {1, 1}, {2, 1}, {3, 1}},
                    {{2, 0}, {2, 1}, {2, 2}, {2, 3}},
                    {{0, 2}, {1, 2}, {2, 2}, {3, 2}},
                    {{1, 0}, {1, 1}, {1, 2}, {1, 3}}
            },
            {
                    {{0, 0}, {0, 1}, {1, 1}, {2, 1}},
                    {{1, 0}, {2, 0}, {1, 1}, {1, 2}},
                    {{0, 1}, {1, 1}, {2, 1}, {2, 2}},
                    {{1, 0}, {1, 1}, {0, 2}, {1, 2}}
            },
            {
                    {{2, 0}, {0, 1}, {1, 1}, {2, 1}},
                    {{1, 0}, {1, 1}, {1, 2}, {2, 2}},
                    {{0, 1}, {1, 1}, {2, 1}, {0, 2}},
                    {{0, 0}, {1, 0}, {1, 1}, {1, 2}}
            },
            {
                    {{1, 0}, {2, 0}, {1, 1}, {2, 1}},
                    {{1, 0}, {2, 0}, {1, 1}, {2, 1}},
                    {{1, 0}, {2, 0}, {1, 1}, {2, 1}},
                    {{1, 0}, {2, 0}, {1, 1}, {2, 1}}
            },
            {
                    {{1, 0}, {2, 0}, {0, 1}, {1, 1}},
                    {{1, 0}, {1, 1}, {2, 1}, {2, 2}},
                    {{1, 1}, {2, 1}, {0, 2}, {1, 2}},
                    {{0, 0}, {0, 1}, {1, 1}, {1, 2}}
            },
            {
                    {{1, 0}, {0, 1}, {1, 1}, {2, 1}},
                    {{1, 0}, {1, 1}, {2, 1}, {1, 2}},
                    {{0, 1}, {1, 1}, {2, 1}, {1, 2}},
                    {{1, 0}, {0, 1}, {1, 1}, {1, 2}}
            },
            {
                    {{0, 0}, {1, 0}, {1, 1}, {2, 1}},
                    {{2, 0}, {1, 1}, {2, 1}, {1, 2}},
                    {{0, 1}, {1, 1}, {1, 2}, {2, 2}},
                    {{1, 0}, {0, 1}, {1, 1}, {0, 2}}
            }
    };

    private static final Color[] PIECE_COLORS = {
            new Color(70, 220, 255),
            new Color(75, 120, 255),
            new Color(255, 157, 62),
            new Color(255, 218, 72),
            new Color(82, 220, 126),
            new Color(181, 94, 255),
            new Color(255, 82, 104)
    };

    private final int[][] board = new int[ROWS][COLUMNS];
    private final Random random = new Random();
    private final int[] bag = new int[7];
    private final ArrayDeque<Integer> nextQueue = new ArrayDeque<Integer>();
    private final ArrayList<Integer> clearingRows = new ArrayList<Integer>();
    private final ArrayList<BlockParticle> particles = new ArrayList<BlockParticle>();

    private int bagIndex = 7;
    private int currentType;
    private int rotation;
    private int pieceX;
    private int pieceY;
    private int holdType = -1;
    private int score;
    private int lines;
    private int level = 1;
    private int combo = -1;
    private int lockResets;

    private boolean started;
    private boolean gameOver;
    private boolean paused;
    private boolean holdUsed;

    private float fallTimer;
    private float lockTimer;
    private float clearTimer;
    private float visualPieceX;
    private float visualPieceY;
    private float piecePulse = 1.0F;
    private float boardShake;
    private float lineGlow;
    private float dropTrailAlpha;
    private int dropTrailType;
    private int dropTrailRotation;
    private int dropTrailX;
    private int dropTrailFromY;
    private int dropTrailToY;

    private long lastFrameNanos;
    private float elapsed;

    private int x;
    private int y;
    private int width;
    private int height;
    private float cellSize;
    private float boardX;
    private float boardY;
    private float boardWidth;
    private float boardHeight;

    private int horizontalDirection;
    private float horizontalHeld;
    private float horizontalRepeat;
    private float softDropHeld;
    private float softDropRepeat;
    private boolean downWasDown;
    private boolean rotateClockwiseWasDown;
    private boolean rotateCounterWasDown;
    private boolean hardDropWasDown;
    private boolean holdWasDown;
    private boolean pauseWasDown;
    private boolean restartWasDown;
    private boolean enterWasDown;

    public TetrisScene(GamesCategory parent) {
        super(parent, "Tetris", "Full controls, hold and next queue, ghost piece, and effects", LegacyIcon.GRID);
    }

    @Override
    public void initGui() {
        syncLayout();
        resetAttractMode();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        syncLayout();

        Glide instance = Glide.getInstance();
        NanoVGManager nvg = instance.getNanoVGManager();
        ColorManager colorManager = instance.getColorManager();
        ColorPalette palette = colorManager.getPalette();
        AccentColor accent = colorManager.getCurrentColor();

        float dt = frameDelta();
        elapsed += dt;
        pollInput(dt);

        if(started && !gameOver && !paused) {
            updateGame(dt);
        }
        visualPieceX = anim(visualPieceX, pieceX, 22.0F, dt);
        visualPieceY = anim(visualPieceY, pieceY, 19.0F, dt);
        piecePulse = anim(piecePulse, 1.0F, 12.0F, dt);
        boardShake = anim(boardShake, 0.0F, 10.0F, dt);
        lineGlow = anim(lineGlow, 0.0F, 7.0F, dt);
        dropTrailAlpha = anim(dropTrailAlpha, 0.0F, 9.0F, dt);
        updateParticles(dt);

        nvg.save();
        nvg.scissor(x, y, width, height);
        drawBackground(nvg, palette, accent);

        float shakeX = boardShake <= 0.03F ? 0.0F : (random.nextFloat() - 0.5F) * boardShake;
        float shakeY = boardShake <= 0.03F ? 0.0F : (random.nextFloat() - 0.5F) * boardShake;
        nvg.translate(shakeX, shakeY);

        drawSidePanels(nvg, palette, accent);
        drawBoard(nvg, palette, accent);
        drawParticles(nvg);
        drawOverlay(nvg, palette, accent);
        nvg.restore();

        nvg.drawOutlineRoundedRect(x, y, width, height, 10, 1.2F,
                alpha(palette.getFontColor(ColorType.NORMAL), 95));
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
        // Gameplay input is polled every frame so DAS/ARR and soft drop remain smooth.
    }

    private void syncLayout() {
        x = getX();
        y = getY();
        width = getWidth();
        height = getHeight();

        cellSize = (float) Math.floor(Math.min((height - 22.0F) / ROWS,
                Math.max(10.0F, width * 0.43F / COLUMNS)));
        cellSize = Math.max(10.0F, Math.min(17.0F, cellSize));
        boardWidth = COLUMNS * cellSize;
        boardHeight = ROWS * cellSize;
        boardX = x + (width - boardWidth) / 2.0F;
        boardY = y + (height - boardHeight) / 2.0F;
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

    private void resetAttractMode() {
        started = false;
        gameOver = false;
        paused = false;
        score = 0;
        lines = 0;
        level = 1;
        holdType = -1;
        particles.clear();
        clearBoard();
        lastFrameNanos = 0L;
        resetInputTracking();
    }

    private void startGame() {
        clearBoard();
        nextQueue.clear();
        clearingRows.clear();
        particles.clear();
        bagIndex = 7;
        holdType = -1;
        holdUsed = false;
        score = 0;
        lines = 0;
        level = 1;
        combo = -1;
        fallTimer = 0.0F;
        lockTimer = 0.0F;
        clearTimer = 0.0F;
        boardShake = 0.0F;
        lineGlow = 0.0F;
        started = true;
        gameOver = false;
        paused = false;
        fillNextQueue();
        spawnNextPiece();
        resetInputTracking();
    }

    private void clearBoard() {
        for(int row = 0; row < ROWS; row++) {
            for(int column = 0; column < COLUMNS; column++) {
                board[row][column] = 0;
            }
        }
    }

    private void pollInput(float dt) {
        boolean leftDown = Keyboard.isKeyDown(Keyboard.KEY_LEFT) || Keyboard.isKeyDown(Keyboard.KEY_A);
        boolean rightDown = Keyboard.isKeyDown(Keyboard.KEY_RIGHT) || Keyboard.isKeyDown(Keyboard.KEY_D);
        boolean downDown = Keyboard.isKeyDown(Keyboard.KEY_DOWN) || Keyboard.isKeyDown(Keyboard.KEY_S);
        boolean rotateClockwiseDown = Keyboard.isKeyDown(Keyboard.KEY_UP)
                || Keyboard.isKeyDown(Keyboard.KEY_W)
                || Keyboard.isKeyDown(Keyboard.KEY_X);
        boolean rotateCounterDown = Keyboard.isKeyDown(Keyboard.KEY_Z);
        boolean hardDropDown = Keyboard.isKeyDown(Keyboard.KEY_SPACE);
        boolean holdDown = Keyboard.isKeyDown(Keyboard.KEY_C)
                || Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
                || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        boolean pauseDown = Keyboard.isKeyDown(Keyboard.KEY_P);
        boolean restartDown = Keyboard.isKeyDown(Keyboard.KEY_R);
        boolean enterDown = Keyboard.isKeyDown(Keyboard.KEY_RETURN);

        if((!started || gameOver) && ((hardDropDown && !hardDropWasDown)
                || (enterDown && !enterWasDown))) {
            startGame();
            updateInputEdges(downDown, rotateClockwiseDown, rotateCounterDown,
                    hardDropDown, holdDown, pauseDown, restartDown, enterDown);
            return;
        }

        if(started && restartDown && !restartWasDown) {
            startGame();
            updateInputEdges(downDown, rotateClockwiseDown, rotateCounterDown,
                    hardDropDown, holdDown, pauseDown, restartDown, enterDown);
            return;
        }

        if(started && !gameOver && pauseDown && !pauseWasDown) {
            paused = !paused;
        }

        if(started && !gameOver && !paused && clearingRows.isEmpty()) {
            int direction = leftDown == rightDown ? 0 : (leftDown ? -1 : 1);
            handleHorizontal(direction, dt);

            if(downDown) {
                if(!downWasDown) {
                    moveDown(true);
                    softDropHeld = 0.0F;
                    softDropRepeat = 0.0F;
                } else {
                    softDropHeld += dt;
                    if(softDropHeld >= 0.08F) {
                        softDropRepeat += dt;
                        while(softDropRepeat >= SOFT_DROP_REPEAT) {
                            moveDown(true);
                            softDropRepeat -= SOFT_DROP_REPEAT;
                        }
                    }
                }
            } else {
                softDropHeld = 0.0F;
                softDropRepeat = 0.0F;
            }

            if(rotateClockwiseDown && !rotateClockwiseWasDown) {
                rotatePiece(1);
            }
            if(rotateCounterDown && !rotateCounterWasDown) {
                rotatePiece(-1);
            }
            if(hardDropDown && !hardDropWasDown) {
                hardDrop();
            }
            if(holdDown && !holdWasDown) {
                holdPiece();
            }
        } else {
            horizontalDirection = 0;
            horizontalHeld = 0.0F;
            horizontalRepeat = 0.0F;
        }

        updateInputEdges(downDown, rotateClockwiseDown, rotateCounterDown,
                hardDropDown, holdDown, pauseDown, restartDown, enterDown);
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
            tryMove(direction, 0, true);
            return;
        }

        horizontalHeld += dt;
        if(horizontalHeld >= DAS) {
            horizontalRepeat += dt;
            while(horizontalRepeat >= ARR) {
                tryMove(direction, 0, true);
                horizontalRepeat -= ARR;
            }
        }
    }

    private void updateInputEdges(boolean downDown, boolean rotateClockwiseDown,
            boolean rotateCounterDown, boolean hardDropDown, boolean holdDown,
            boolean pauseDown, boolean restartDown, boolean enterDown) {
        downWasDown = downDown;
        rotateClockwiseWasDown = rotateClockwiseDown;
        rotateCounterWasDown = rotateCounterDown;
        hardDropWasDown = hardDropDown;
        holdWasDown = holdDown;
        pauseWasDown = pauseDown;
        restartWasDown = restartDown;
        enterWasDown = enterDown;
    }

    private void resetInputTracking() {
        horizontalDirection = 0;
        horizontalHeld = 0.0F;
        horizontalRepeat = 0.0F;
        softDropHeld = 0.0F;
        softDropRepeat = 0.0F;
        downWasDown = Keyboard.isKeyDown(Keyboard.KEY_DOWN) || Keyboard.isKeyDown(Keyboard.KEY_S);
        rotateClockwiseWasDown = Keyboard.isKeyDown(Keyboard.KEY_UP)
                || Keyboard.isKeyDown(Keyboard.KEY_W)
                || Keyboard.isKeyDown(Keyboard.KEY_X);
        rotateCounterWasDown = Keyboard.isKeyDown(Keyboard.KEY_Z);
        hardDropWasDown = Keyboard.isKeyDown(Keyboard.KEY_SPACE);
        holdWasDown = Keyboard.isKeyDown(Keyboard.KEY_C)
                || Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
                || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        pauseWasDown = Keyboard.isKeyDown(Keyboard.KEY_P);
        restartWasDown = Keyboard.isKeyDown(Keyboard.KEY_R);
        enterWasDown = Keyboard.isKeyDown(Keyboard.KEY_RETURN);
    }

    private void updateGame(float dt) {
        if(!clearingRows.isEmpty()) {
            clearTimer -= dt;
            if(clearTimer <= 0.0F) {
                finishLineClear();
            }
            return;
        }

        fallTimer += dt;
        float interval = gravityInterval();
        while(fallTimer >= interval) {
            fallTimer -= interval;
            if(!moveDown(false)) {
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

    private float gravityInterval() {
        return Math.max(0.065F, (float) (0.78F * Math.pow(0.84F, level - 1)));
    }

    private boolean tryMove(int dx, int dy, boolean playerAction) {
        if(collides(currentType, rotation, pieceX + dx, pieceY + dy)) {
            return false;
        }
        pieceX += dx;
        pieceY += dy;
        if(playerAction && collides(currentType, rotation, pieceX, pieceY + 1)
                && lockResets < 12) {
            lockTimer = 0.0F;
            lockResets++;
        }
        return true;
    }

    private boolean moveDown(boolean softDrop) {
        if(!tryMove(0, 1, softDrop)) {
            return false;
        }
        if(softDrop) {
            score += 1;
        }
        return true;
    }

    private void rotatePiece(int direction) {
        int targetRotation = (rotation + direction + 4) % 4;
        int[][] kicks = {{0, 0}, {-1, 0}, {1, 0}, {-2, 0}, {2, 0}, {0, -1}, {-1, -1}, {1, -1}};
        for(int[] kick : kicks) {
            if(!collides(currentType, targetRotation, pieceX + kick[0], pieceY + kick[1])) {
                rotation = targetRotation;
                pieceX += kick[0];
                pieceY += kick[1];
                piecePulse = 1.16F;
                if(collides(currentType, rotation, pieceX, pieceY + 1) && lockResets < 12) {
                    lockTimer = 0.0F;
                    lockResets++;
                }
                return;
            }
        }
    }

    private void hardDrop() {
        int targetY = ghostY();
        int distance = targetY - pieceY;
        dropTrailType = currentType;
        dropTrailRotation = rotation;
        dropTrailX = pieceX;
        dropTrailFromY = pieceY;
        dropTrailToY = targetY;
        dropTrailAlpha = 1.0F;
        score += Math.max(0, distance) * 2;
        pieceY = targetY;
        visualPieceY = pieceY;
        lockPiece();
    }

    private void holdPiece() {
        if(holdUsed) {
            return;
        }
        int outgoing = currentType;
        if(holdType < 0) {
            holdType = outgoing;
            spawnNextPiece();
        } else {
            currentType = holdType;
            holdType = outgoing;
            resetCurrentPiece();
        }
        holdUsed = true;
        piecePulse = 1.18F;
    }

    private void lockPiece() {
        for(int[] block : SHAPES[currentType][rotation]) {
            if(pieceY + block[1] < 0) {
                triggerGameOver();
                return;
            }
        }
        for(int[] block : SHAPES[currentType][rotation]) {
            int bx = pieceX + block[0];
            int by = pieceY + block[1];
            board[by][bx] = currentType + 1;
        }

        clearingRows.clear();
        for(int row = 0; row < ROWS; row++) {
            boolean full = true;
            for(int column = 0; column < COLUMNS; column++) {
                if(board[row][column] == 0) {
                    full = false;
                    break;
                }
            }
            if(full) {
                clearingRows.add(Integer.valueOf(row));
            }
        }

        if(clearingRows.isEmpty()) {
            combo = -1;
            spawnNextPiece();
        } else {
            clearTimer = CLEAR_DURATION;
            lineGlow = 1.0F;
            boardShake = 3.5F + clearingRows.size() * 1.2F;
            spawnLineParticles();
        }
    }

    private void finishLineClear() {
        boolean[] remove = new boolean[ROWS];
        for(Integer row : clearingRows) {
            remove[row.intValue()] = true;
        }

        int destination = ROWS - 1;
        for(int source = ROWS - 1; source >= 0; source--) {
            if(remove[source]) {
                continue;
            }
            if(destination != source) {
                for(int column = 0; column < COLUMNS; column++) {
                    board[destination][column] = board[source][column];
                }
            }
            destination--;
        }
        while(destination >= 0) {
            for(int column = 0; column < COLUMNS; column++) {
                board[destination][column] = 0;
            }
            destination--;
        }

        int count = clearingRows.size();
        int[] baseScores = {0, 100, 300, 500, 800};
        combo++;
        score += baseScores[count] * level;
        if(combo > 0) {
            score += combo * 50 * level;
        }
        lines += count;
        level = lines / 10 + 1;
        clearingRows.clear();
        clearTimer = 0.0F;
        spawnNextPiece();
    }

    private void spawnNextPiece() {
        fillNextQueue();
        currentType = nextQueue.removeFirst().intValue();
        fillNextQueue();
        holdUsed = false;
        resetCurrentPiece();
    }

    private void resetCurrentPiece() {
        rotation = 0;
        pieceX = 3;
        pieceY = -1;
        visualPieceX = pieceX;
        visualPieceY = pieceY - 1.0F;
        fallTimer = 0.0F;
        lockTimer = 0.0F;
        lockResets = 0;
        if(collides(currentType, rotation, pieceX, pieceY)) {
            triggerGameOver();
        }
    }

    private void triggerGameOver() {
        gameOver = true;
        paused = false;
        boardShake = 8.0F;
        spawnGameOverParticles();
    }

    private void fillNextQueue() {
        while(nextQueue.size() < 5) {
            nextQueue.addLast(Integer.valueOf(nextFromBag()));
        }
    }

    private int nextFromBag() {
        if(bagIndex >= bag.length) {
            for(int i = 0; i < bag.length; i++) {
                bag[i] = i;
            }
            for(int i = bag.length - 1; i > 0; i--) {
                int swap = random.nextInt(i + 1);
                int value = bag[i];
                bag[i] = bag[swap];
                bag[swap] = value;
            }
            bagIndex = 0;
        }
        return bag[bagIndex++];
    }

    private boolean collides(int type, int pieceRotation, int testX, int testY) {
        for(int[] block : SHAPES[type][pieceRotation]) {
            int bx = testX + block[0];
            int by = testY + block[1];
            if(bx < 0 || bx >= COLUMNS || by >= ROWS) {
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
        Color upper = mix(base, accent.getColor1(), 0.16F, 246);
        Color lower = mix(base, accent.getColor2(), 0.07F, 246);
        nvg.drawVerticalGradientRect(x, y, width, height, upper, lower);

        for(int i = 0; i < 22; i++) {
            float px = x + ((i * 71.0F + elapsed * (4.0F + i % 4)) % (width + 20.0F)) - 10.0F;
            float py = y + (i * 53 % Math.max(20, height));
            nvg.drawCircle(px, py, 0.65F + (i % 3) * 0.2F,
                    new Color(255, 255, 255, 15 + i % 5 * 5));
        }
    }

    private void drawBoard(NanoVGManager nvg, ColorPalette palette, AccentColor accent) {
        nvg.drawShadow(boardX - 5, boardY - 5, boardWidth + 10, boardHeight + 10, 11, 8);
        nvg.drawRoundedRect(boardX - 5, boardY - 5, boardWidth + 10, boardHeight + 10, 11,
                new Color(9, 12, 23, 222));
        nvg.drawGradientOutlineRoundedRect(boardX - 4.5F, boardY - 4.5F,
                boardWidth + 9, boardHeight + 9, 11, 1.0F,
                alpha(accent.getColor1(), 115), alpha(accent.getColor2(), 115));

        for(int row = 0; row < ROWS; row++) {
            for(int column = 0; column < COLUMNS; column++) {
                float bx = boardX + column * cellSize;
                float by = boardY + row * cellSize;
                nvg.drawRoundedRect(bx + 1.0F, by + 1.0F, cellSize - 2.0F, cellSize - 2.0F,
                        2.2F, new Color(255, 255, 255, 7));
                int value = board[row][column];
                if(value != 0) {
                    drawBlock(nvg, bx, by, cellSize, PIECE_COLORS[value - 1], 1.0F);
                }
            }
        }

        if(!clearingRows.isEmpty()) {
            float progress = Math.max(0.0F, clearTimer / CLEAR_DURATION);
            float flash = 0.45F + 0.55F * (float) Math.sin((1.0F - progress) * Math.PI * 5.0F);
            for(Integer row : clearingRows) {
                float rowY = boardY + row.intValue() * cellSize;
                nvg.drawRoundedRect(boardX, rowY + 1, boardWidth, cellSize - 2, 3,
                        new Color(255, 255, 255, Math.max(35, (int) (flash * 205.0F))));
            }
        }

        if(dropTrailAlpha > 0.01F) {
            nvg.save();
            nvg.setAlpha(dropTrailAlpha * 0.18F);
            int step = dropTrailFromY <= dropTrailToY ? 1 : -1;
            for(int trailY = dropTrailFromY; trailY != dropTrailToY; trailY += step) {
                drawPieceAt(nvg, dropTrailType, dropTrailRotation, dropTrailX, trailY,
                        PIECE_COLORS[dropTrailType], 0.5F, false);
            }
            nvg.restore();
        }

        if(started && !gameOver && clearingRows.isEmpty()) {
            int ghost = ghostY();
            drawGhostPiece(nvg, currentType, rotation, pieceX, ghost);

            nvg.save();
            float centerX = boardX + (visualPieceX + 2.0F) * cellSize;
            float centerY = boardY + (visualPieceY + 2.0F) * cellSize;
            nvg.scale(centerX, centerY, piecePulse);
            drawPieceAt(nvg, currentType, rotation, visualPieceX, visualPieceY,
                    PIECE_COLORS[currentType], 1.0F, true);
            nvg.restore();
        }

        if(lineGlow > 0.01F) {
            nvg.drawRoundedGlow(boardX - 4, boardY - 4, boardWidth + 8, boardHeight + 8, 10,
                    alpha(accent.getColor1(), (int) (lineGlow * 95.0F)), 7);
        }
    }

    private void drawPieceAt(NanoVGManager nvg, int type, int pieceRotation,
            float gridX, float gridY, Color color, float alpha, boolean solid) {
        for(int[] block : SHAPES[type][pieceRotation]) {
            float gx = gridX + block[0];
            float gy = gridY + block[1];
            if(gy < 0.0F) {
                continue;
            }
            float bx = boardX + gx * cellSize;
            float by = boardY + gy * cellSize;
            if(solid) {
                drawBlock(nvg, bx, by, cellSize, color, alpha);
            } else {
                nvg.drawRoundedRect(bx + 1.5F, by + 1.5F, cellSize - 3.0F, cellSize - 3.0F,
                        2.2F, alpha(color, (int) (alpha * 180.0F)));
            }
        }
    }

    private void drawGhostPiece(NanoVGManager nvg, int type, int pieceRotation, int gridX, int gridY) {
        Color color = PIECE_COLORS[type];
        for(int[] block : SHAPES[type][pieceRotation]) {
            int gy = gridY + block[1];
            if(gy < 0) {
                continue;
            }
            float bx = boardX + (gridX + block[0]) * cellSize;
            float by = boardY + gy * cellSize;
            nvg.drawOutlineRoundedRect(bx + 2.0F, by + 2.0F, cellSize - 4.0F,
                    cellSize - 4.0F, 2.4F, 1.0F, alpha(color, 95));
            nvg.drawRoundedRect(bx + 4.0F, by + 4.0F, cellSize - 8.0F,
                    cellSize - 8.0F, 1.5F, alpha(color, 22));
        }
    }

    private void drawBlock(NanoVGManager nvg, float bx, float by, float size,
            Color color, float alphaValue) {
        int opacity = Math.max(0, Math.min(255, (int) (alphaValue * 255.0F)));
        Color dark = mix(color, Color.BLACK, 0.24F, opacity);
        Color light = mix(color, Color.WHITE, 0.18F, opacity);
        nvg.drawGradientRoundedRect(bx + 0.8F, by + 0.8F, size - 1.6F, size - 1.6F,
                Math.max(2.0F, size * 0.18F), light, dark);
        nvg.drawRoundedRect(bx + 2.0F, by + 2.0F, size - 4.0F, Math.max(1.2F, size * 0.12F),
                1.0F, new Color(255, 255, 255, Math.min(opacity, 72)));
        nvg.drawOutlineRoundedRect(bx + 1.0F, by + 1.0F, size - 2.0F, size - 2.0F,
                Math.max(2.0F, size * 0.18F), 0.65F,
                new Color(255, 255, 255, Math.min(opacity, 54)));
    }

    private void drawSidePanels(NanoVGManager nvg, ColorPalette palette, AccentColor accent) {
        float leftX = x + 12.0F;
        float leftWidth = Math.max(82.0F, boardX - leftX - 12.0F);
        float rightX = boardX + boardWidth + 12.0F;
        float rightWidth = Math.max(82.0F, x + width - rightX - 12.0F);

        drawPanel(nvg, leftX, boardY, leftWidth, boardHeight, palette, accent);
        drawPanel(nvg, rightX, boardY, rightWidth, boardHeight, palette, accent);

        nvg.drawCenteredText("TETRIS", leftX + leftWidth / 2.0F, boardY + 12,
                Color.WHITE, 13, Fonts.SEMIBOLD);
        drawStat(nvg, leftX, leftWidth, boardY + 42, "SCORE", String.valueOf(score), palette);
        drawStat(nvg, leftX, leftWidth, boardY + 80, "LINES", String.valueOf(lines), palette);
        drawStat(nvg, leftX, leftWidth, boardY + 118, "LEVEL", String.valueOf(level), palette);

        float holdY = boardY + 158;
        nvg.drawCenteredText("HOLD", leftX + leftWidth / 2.0F, holdY,
                palette.getFontColor(ColorType.NORMAL), 7.5F, Fonts.MEDIUM);
        nvg.drawRoundedRect(leftX + 8, holdY + 14, leftWidth - 16, 62, 7,
                new Color(0, 0, 0, 54));
        if(holdType >= 0) {
            drawMiniPiece(nvg, holdType, leftX + leftWidth / 2.0F, holdY + 45, cellSize * 0.68F,
                    holdUsed ? 0.45F : 1.0F);
        }

        nvg.drawCenteredText("NEXT", rightX + rightWidth / 2.0F, boardY + 12,
                palette.getFontColor(ColorType.NORMAL), 7.5F, Fonts.MEDIUM);
        int previewIndex = 0;
        for(Integer type : nextQueue) {
            if(previewIndex >= 2) {
                break;
            }
            float previewY = boardY + 43 + previewIndex * 48;
            nvg.drawRoundedRect(rightX + 8, previewY - 18, rightWidth - 16, 41, 7,
                    new Color(0, 0, 0, 48));
            drawMiniPiece(nvg, type.intValue(), rightX + rightWidth / 2.0F,
                    previewY + 4, cellSize * 0.56F, 1.0F);
            previewIndex++;
        }

        float controlsY = boardY + boardHeight - 112;
        nvg.drawCenteredText("CONTROLS", rightX + rightWidth / 2.0F, controlsY,
                alpha(accent.getColor1(), 235), 7.5F, Fonts.SEMIBOLD);
        drawControl(nvg, rightX + 10, controlsY + 17, "A/D  Move", palette);
        drawControl(nvg, rightX + 10, controlsY + 31, "S  Soft drop", palette);
        drawControl(nvg, rightX + 10, controlsY + 45, "W/X  Rotate", palette);
        drawControl(nvg, rightX + 10, controlsY + 59, "Z  Rotate back", palette);
        drawControl(nvg, rightX + 10, controlsY + 73, "SPACE  Drop", palette);
        drawControl(nvg, rightX + 10, controlsY + 87, "C/SHIFT  Hold", palette);
        drawControl(nvg, rightX + 10, controlsY + 101, "P Pause · R Reset", palette);
    }

    private void drawPanel(NanoVGManager nvg, float px, float py, float pw, float ph,
            ColorPalette palette, AccentColor accent) {
        nvg.drawRoundedRect(px, py, pw, ph, 10, new Color(8, 11, 22, 135));
        nvg.drawOutlineRoundedRect(px + 0.5F, py + 0.5F, pw - 1, ph - 1, 10, 0.7F,
                new Color(255, 255, 255, 28));
        nvg.drawHorizontalGradientRect(px + 12, py + 31, pw - 24, 1,
                alpha(accent.getColor1(), 120), alpha(accent.getColor2(), 30));
    }

    private void drawStat(NanoVGManager nvg, float panelX, float panelWidth, float statY,
            String label, String value, ColorPalette palette) {
        nvg.drawCenteredText(label, panelX + panelWidth / 2.0F, statY,
                palette.getFontColor(ColorType.NORMAL), 7, Fonts.MEDIUM);
        nvg.drawCenteredText(value, panelX + panelWidth / 2.0F, statY + 14,
                Color.WHITE, 12, Fonts.SEMIBOLD);
    }

    private void drawControl(NanoVGManager nvg, float controlX, float controlY,
            String text, ColorPalette palette) {
        nvg.drawText(text, controlX, controlY,
                alpha(palette.getFontColor(ColorType.NORMAL), 220), 6.5F, Fonts.REGULAR);
    }

    private void drawMiniPiece(NanoVGManager nvg, int type, float centerX, float centerY,
            float miniCell, float alphaValue) {
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
        float pieceWidth = (maxX - minX + 1) * miniCell;
        float pieceHeight = (maxY - minY + 1) * miniCell;
        float startX = centerX - pieceWidth / 2.0F - minX * miniCell;
        float startY = centerY - pieceHeight / 2.0F - minY * miniCell;
        for(int[] block : SHAPES[type][0]) {
            drawBlock(nvg, startX + block[0] * miniCell, startY + block[1] * miniCell,
                    miniCell, PIECE_COLORS[type], alphaValue);
        }
    }

    private void drawOverlay(NanoVGManager nvg, ColorPalette palette, AccentColor accent) {
        if(started && !gameOver && !paused) {
            return;
        }

        nvg.drawRoundedRect(boardX - 5, boardY - 5, boardWidth + 10, boardHeight + 10,
                11, new Color(4, 7, 16, 150));

        float cardWidth = Math.min(310.0F, width - 42.0F);
        float cardHeight = !started ? 126.0F : (paused ? 98.0F : 132.0F);
        float cardX = x + (width - cardWidth) / 2.0F;
        float cardY = y + (height - cardHeight) / 2.0F;
        float centerX = x + width / 2.0F;
        float pulse = 0.5F + 0.5F * (float) Math.sin(elapsed * 3.0F);

        nvg.drawShadow(cardX, cardY, cardWidth, cardHeight, 14, 8);
        nvg.drawRoundedRect(cardX, cardY, cardWidth, cardHeight, 14,
                new Color(8, 11, 23, 225));
        nvg.drawGradientOutlineRoundedRect(cardX + 0.5F, cardY + 0.5F,
                cardWidth - 1.0F, cardHeight - 1.0F, 14, 1.0F,
                alpha(accent.getColor1(), 150), alpha(accent.getColor2(), 150));

        if(!started) {
            nvg.drawCenteredText("TETRIS", centerX, cardY + 19,
                    Color.WHITE, 19, Fonts.SEMIBOLD);
            nvg.drawCenteredText("7-bag pieces · hold · ghost piece · lock delay",
                    centerX, cardY + 50,
                    palette.getFontColor(ColorType.NORMAL), 7.2F, Fonts.REGULAR);
            nvg.drawRoundedRect(centerX - 66, cardY + 82, 132, 25, 8,
                    alpha(accent.getColor1(), 90 + (int) (pulse * 55.0F)));
            nvg.drawCenteredText("ENTER / SPACE", centerX, cardY + 89,
                    Color.WHITE, 8.5F, Fonts.SEMIBOLD);
        } else if(paused) {
            nvg.drawCenteredText("PAUSED", centerX, cardY + 20,
                    Color.WHITE, 17, Fonts.SEMIBOLD);
            nvg.drawCenteredText("Press P or click to continue", centerX, cardY + 55,
                    palette.getFontColor(ColorType.NORMAL), 8, Fonts.REGULAR);
        } else {
            nvg.drawCenteredText("GAME OVER", centerX, cardY + 18,
                    new Color(255, 96, 122), 17, Fonts.SEMIBOLD);
            nvg.drawCenteredText("Score  " + score, centerX, cardY + 50,
                    Color.WHITE, 10, Fonts.MEDIUM);
            nvg.drawCenteredText("Lines  " + lines + "     Level  " + level,
                    centerX, cardY + 71,
                    palette.getFontColor(ColorType.NORMAL), 8, Fonts.REGULAR);
            nvg.drawRoundedRect(centerX - 72, cardY + 98, 144, 24, 8,
                    alpha(accent.getColor1(), 95 + (int) (pulse * 55.0F)));
            nvg.drawCenteredText("ENTER / SPACE TO RETRY", centerX, cardY + 105,
                    Color.WHITE, 7.5F, Fonts.SEMIBOLD);
        }
    }

    private void spawnLineParticles() {
        for(Integer rowValue : clearingRows) {
            int row = rowValue.intValue();
            for(int column = 0; column < COLUMNS; column++) {
                int value = board[row][column];
                Color color = value == 0 ? Color.WHITE : PIECE_COLORS[value - 1];
                float centerX = boardX + (column + 0.5F) * cellSize;
                float centerY = boardY + (row + 0.5F) * cellSize;
                for(int i = 0; i < 2; i++) {
                    particles.add(new BlockParticle(centerX, centerY,
                            (random.nextFloat() - 0.5F) * 125.0F,
                            -35.0F - random.nextFloat() * 105.0F,
                            0.48F + random.nextFloat() * 0.34F,
                            1.5F + random.nextFloat() * 2.5F,
                            color));
                }
            }
        }
    }

    private void spawnGameOverParticles() {
        for(int row = 0; row < ROWS; row++) {
            for(int column = 0; column < COLUMNS; column++) {
                int value = board[row][column];
                if(value == 0 || random.nextFloat() > 0.28F) {
                    continue;
                }
                particles.add(new BlockParticle(
                        boardX + (column + 0.5F) * cellSize,
                        boardY + (row + 0.5F) * cellSize,
                        (random.nextFloat() - 0.5F) * 95.0F,
                        -25.0F - random.nextFloat() * 90.0F,
                        0.65F + random.nextFloat() * 0.45F,
                        1.5F + random.nextFloat() * 3.0F,
                        PIECE_COLORS[value - 1]));
            }
        }
    }

    private void updateParticles(float dt) {
        Iterator<BlockParticle> iterator = particles.iterator();
        while(iterator.hasNext()) {
            BlockParticle particle = iterator.next();
            particle.life -= dt;
            if(particle.life <= 0.0F) {
                iterator.remove();
                continue;
            }
            particle.x += particle.vx * dt;
            particle.y += particle.vy * dt;
            particle.vy += 210.0F * dt;
            particle.vx *= (float) Math.pow(0.12F, dt);
        }
    }

    private void drawParticles(NanoVGManager nvg) {
        for(BlockParticle particle : particles) {
            float life = Math.max(0.0F, particle.life / particle.maxLife);
            float size = particle.size * (0.55F + life * 0.45F);
            nvg.drawRoundedRect(particle.x - size / 2.0F, particle.y - size / 2.0F,
                    size, size, 1.2F, alpha(particle.color, (int) (life * 220.0F)));
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

    private static final class BlockParticle {
        private float x;
        private float y;
        private float vx;
        private float vy;
        private float life;
        private final float maxLife;
        private final float size;
        private final Color color;

        private BlockParticle(float x, float y, float vx, float vy,
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
}
