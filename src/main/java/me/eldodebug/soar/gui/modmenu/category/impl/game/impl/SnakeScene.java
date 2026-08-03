package me.eldodebug.soar.gui.modmenu.category.impl.game.impl;

import java.awt.Color;
import java.util.ArrayList;
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

public class SnakeScene extends GameScene {

    private static final int COLS = 24;
    private static final int ROWS = 16;

    private final Random random = new Random();
    private final List<Cell> snake = new ArrayList<Cell>();
    private final List<Cell> previousSnake = new ArrayList<Cell>();

    private int directionX = 1;
    private int directionY;
    private int queuedX = 1;
    private int queuedY;
    private int foodX;
    private int foodY;
    private int score;
    private int best;

    private boolean started;
    private boolean paused;
    private boolean dead;

    private float moveAccumulator;
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

    public SnakeScene(GamesCategory parent) {
        super(parent, "Snake", "Smooth classic Snake with keyboard controls", LegacyIcon.PLAY);
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
        if(started && !paused && !dead) {
            moveAccumulator += dt;
            float interval = moveInterval();
            while(moveAccumulator >= interval) {
                moveAccumulator -= interval;
                stepSnake();
                if(dead) {
                    break;
                }
                interval = moveInterval();
            }
        }

        Glide glide = Glide.getInstance();
        NanoVGManager nvg = glide.getNanoVGManager();
        ColorPalette palette = glide.getColorManager().getPalette();
        AccentColor accent = glide.getColorManager().getCurrentColor();

        nvg.save();
        nvg.scissor(x, y, width, height);
        drawBackground(nvg, palette, accent);
        drawBoard(nvg, accent);
        drawFood(nvg);
        drawSnake(nvg, accent);
        drawHud(nvg, palette, accent);
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
        if(!started || dead) {
            startGame();
        } else if(paused) {
            paused = false;
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if(keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_SPACE) {
            if(!started || dead) {
                startGame();
            }
            return;
        }
        if(keyCode == Keyboard.KEY_R) {
            startGame();
            return;
        }
        if(keyCode == Keyboard.KEY_P && started && !dead) {
            paused = !paused;
            return;
        }
        if(!started || dead || paused) {
            return;
        }

        if(keyCode == Keyboard.KEY_UP || keyCode == Keyboard.KEY_W) {
            queueDirection(0, -1);
        } else if(keyCode == Keyboard.KEY_DOWN || keyCode == Keyboard.KEY_S) {
            queueDirection(0, 1);
        } else if(keyCode == Keyboard.KEY_LEFT || keyCode == Keyboard.KEY_A) {
            queueDirection(-1, 0);
        } else if(keyCode == Keyboard.KEY_RIGHT || keyCode == Keyboard.KEY_D) {
            queueDirection(1, 0);
        }
    }

    private void syncLayout() {
        x = getX();
        y = getY();
        width = getWidth();
        height = getHeight();
        cell = (float)Math.floor(Math.min((width - 44.0F) / COLS, (height - 58.0F) / ROWS));
        cell = Math.max(8.0F, Math.min(18.0F, cell));
        boardWidth = COLS * cell;
        boardHeight = ROWS * cell;
        boardX = x + (width - boardWidth) / 2.0F;
        boardY = y + (height - boardHeight) / 2.0F + 10.0F;
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

    private float moveInterval() {
        return Math.max(0.065F, 0.135F - score * 0.0025F);
    }

    private void resetToTitle() {
        started = false;
        paused = false;
        dead = false;
        score = 0;
        moveAccumulator = 0.0F;
        lastFrameNanos = 0L;
        setupSnake();
    }

    private void startGame() {
        started = true;
        paused = false;
        dead = false;
        score = 0;
        moveAccumulator = 0.0F;
        setupSnake();
        spawnFood();
    }

    private void setupSnake() {
        snake.clear();
        previousSnake.clear();
        int centerX = COLS / 2;
        int centerY = ROWS / 2;
        snake.add(new Cell(centerX, centerY));
        snake.add(new Cell(centerX - 1, centerY));
        snake.add(new Cell(centerX - 2, centerY));
        copySnake(snake, previousSnake);
        directionX = 1;
        directionY = 0;
        queuedX = 1;
        queuedY = 0;
        foodX = centerX + 5;
        foodY = centerY;
    }

    private void queueDirection(int dx, int dy) {
        if(dx == -directionX && dy == -directionY) {
            return;
        }
        queuedX = dx;
        queuedY = dy;
    }

    private void stepSnake() {
        copySnake(snake, previousSnake);
        if(!(queuedX == -directionX && queuedY == -directionY)) {
            directionX = queuedX;
            directionY = queuedY;
        }

        Cell head = snake.get(0);
        int nextX = head.x + directionX;
        int nextY = head.y + directionY;
        if(nextX < 0 || nextX >= COLS || nextY < 0 || nextY >= ROWS
                || occupies(nextX, nextY, true)) {
            dead = true;
            best = Math.max(best, score);
            return;
        }

        snake.add(0, new Cell(nextX, nextY));
        if(nextX == foodX && nextY == foodY) {
            score++;
            best = Math.max(best, score);
            spawnFood();
        } else {
            snake.remove(snake.size() - 1);
        }
    }

    private boolean occupies(int targetX, int targetY, boolean ignoreTail) {
        int limit = snake.size() - (ignoreTail ? 1 : 0);
        for(int i = 0; i < limit; i++) {
            Cell cell = snake.get(i);
            if(cell.x == targetX && cell.y == targetY) {
                return true;
            }
        }
        return false;
    }

    private void spawnFood() {
        for(int attempt = 0; attempt < 300; attempt++) {
            int candidateX = random.nextInt(COLS);
            int candidateY = random.nextInt(ROWS);
            if(!occupies(candidateX, candidateY, false)) {
                foodX = candidateX;
                foodY = candidateY;
                return;
            }
        }
    }

    private void copySnake(List<Cell> source, List<Cell> target) {
        target.clear();
        for(Cell cell : source) {
            target.add(new Cell(cell.x, cell.y));
        }
    }

    private void drawBackground(NanoVGManager nvg, ColorPalette palette, AccentColor accent) {
        Color base = palette.getBackgroundColor(ColorType.DARK);
        nvg.drawVerticalGradientRect(x, y, width, height,
                mix(base, new Color(41, 95, 61), 0.24F, 245),
                mix(base, accent.getColor2(), 0.06F, 245));
    }

    private void drawBoard(NanoVGManager nvg, AccentColor accent) {
        nvg.drawShadow(boardX - 6.0F, boardY - 6.0F, boardWidth + 12.0F,
                boardHeight + 12.0F, 11.0F, 7);
        nvg.drawRoundedRect(boardX - 6.0F, boardY - 6.0F, boardWidth + 12.0F,
                boardHeight + 12.0F, 11.0F, new Color(7, 14, 10, 225));
        nvg.drawOutlineRoundedRect(boardX - 5.5F, boardY - 5.5F,
                boardWidth + 11.0F, boardHeight + 11.0F, 11.0F, 0.8F,
                alpha(accent.getColor1(), 95));
        for(int row = 0; row < ROWS; row++) {
            for(int col = 0; col < COLS; col++) {
                int shade = ((row + col) & 1) == 0 ? 18 : 12;
                nvg.drawRect(boardX + col * cell, boardY + row * cell, cell, cell,
                        new Color(116, 190, 100, shade));
            }
        }
    }

    private void drawFood(NanoVGManager nvg) {
        float pulse = 0.88F + (float)Math.sin(System.currentTimeMillis() / 170.0D) * 0.08F;
        float size = cell * 0.72F * pulse;
        float centerX = boardX + (foodX + 0.5F) * cell;
        float centerY = boardY + (foodY + 0.5F) * cell;
        nvg.drawCircle(centerX, centerY, size * 0.56F, new Color(232, 58, 67));
        nvg.drawCircle(centerX - size * 0.18F, centerY - size * 0.18F,
                size * 0.14F, new Color(255, 173, 171, 215));
        nvg.drawRect(centerX - 0.7F, centerY - size * 0.74F, 1.4F, size * 0.28F,
                new Color(102, 65, 37));
        nvg.drawRoundedRect(centerX + 0.6F, centerY - size * 0.69F,
                size * 0.32F, size * 0.18F, 2.0F, new Color(75, 183, 83));
    }

    private void drawSnake(NanoVGManager nvg, AccentColor accent) {
        if(snake.isEmpty()) {
            return;
        }
        float interval = moveInterval();
        float progress = started && !paused && !dead
                ? Math.max(0.0F, Math.min(1.0F, moveAccumulator / interval)) : 1.0F;
        for(int i = snake.size() - 1; i >= 0; i--) {
            Cell current = snake.get(i);
            Cell previous = i < previousSnake.size() ? previousSnake.get(i) : current;
            float gridX = previous.x + (current.x - previous.x) * progress;
            float gridY = previous.y + (current.y - previous.y) * progress;
            float px = boardX + gridX * cell + 1.2F;
            float py = boardY + gridY * cell + 1.2F;
            float size = cell - 2.4F;
            float taper = Math.max(0.72F, 1.0F - i * 0.012F);
            float inset = (size - size * taper) / 2.0F;
            Color base = i == 0 ? accent.getColor1() : mix(accent.getColor1(),
                    new Color(64, 202, 103), Math.min(0.75F, i * 0.035F), 255);
            nvg.drawVerticalGradientRect(px + inset, py + inset, size * taper, size * taper,
                    mix(base, Color.WHITE, 0.14F, 255),
                    mix(base, Color.BLACK, 0.18F, 255));
            nvg.drawOutlineRoundedRect(px + inset, py + inset, size * taper, size * taper,
                    Math.max(2.0F, cell * 0.20F), 0.7F, new Color(0, 0, 0, 75));
            if(i == 0) {
                drawEyes(nvg, px + inset, py + inset, size * taper);
            }
        }
    }

    private void drawEyes(NanoVGManager nvg, float px, float py, float size) {
        float eye = Math.max(1.2F, size * 0.12F);
        float firstX;
        float firstY;
        float secondX;
        float secondY;
        if(directionX != 0) {
            firstX = directionX > 0 ? px + size * 0.72F : px + size * 0.28F;
            secondX = firstX;
            firstY = py + size * 0.31F;
            secondY = py + size * 0.69F;
        } else {
            firstY = directionY > 0 ? py + size * 0.72F : py + size * 0.28F;
            secondY = firstY;
            firstX = px + size * 0.31F;
            secondX = px + size * 0.69F;
        }
        nvg.drawCircle(firstX, firstY, eye, Color.WHITE);
        nvg.drawCircle(secondX, secondY, eye, Color.WHITE);
        nvg.drawCircle(firstX, firstY, eye * 0.48F, new Color(20, 26, 24));
        nvg.drawCircle(secondX, secondY, eye * 0.48F, new Color(20, 26, 24));
    }

    private void drawHud(NanoVGManager nvg, ColorPalette palette, AccentColor accent) {
        nvg.drawText("SNAKE", boardX, boardY - 28.0F, Color.WHITE, 12.0F, Fonts.SEMIBOLD);
        nvg.drawText("Score " + score + "   Best " + best, boardX + 72.0F, boardY - 25.0F,
                palette.getFontColor(ColorType.NORMAL), 8.0F, Fonts.MEDIUM);
        nvg.drawText("WASD / arrows   P pause   R restart", boardX + boardWidth - 196.0F,
                boardY - 25.0F, alpha(accent.getColor1(), 220), 7.0F, Fonts.REGULAR);
    }

    private void drawOverlay(NanoVGManager nvg, ColorPalette palette, AccentColor accent) {
        if(started && !paused && !dead) {
            return;
        }
        float cardWidth = Math.min(310.0F, width - 48.0F);
        float cardHeight = 110.0F;
        float cardX = x + (width - cardWidth) / 2.0F;
        float cardY = y + (height - cardHeight) / 2.0F;
        nvg.drawRoundedRect(x, y, width, height, 10.0F, new Color(3, 7, 5, 128));
        nvg.drawShadow(cardX, cardY, cardWidth, cardHeight, 13.0F, 8);
        nvg.drawRoundedRect(cardX, cardY, cardWidth, cardHeight, 13.0F,
                new Color(8, 17, 12, 232));
        nvg.drawOutlineRoundedRect(cardX + 0.5F, cardY + 0.5F, cardWidth - 1.0F,
                cardHeight - 1.0F, 13.0F, 0.9F, alpha(accent.getColor1(), 150));
        String title = !started ? "SNAKE" : (paused ? "PAUSED" : "GAME OVER");
        Color titleColor = dead ? new Color(255, 100, 108) : Color.WHITE;
        nvg.drawCenteredText(title, x + width / 2.0F, cardY + 20.0F,
                titleColor, 17.0F, Fonts.SEMIBOLD);
        String message = !started ? "Press ENTER, SPACE, or click to start"
                : (paused ? "Press P or click to continue"
                : "Score " + score + "  -  press ENTER, SPACE, or click");
        nvg.drawCenteredText(message, x + width / 2.0F, cardY + 59.0F,
                palette.getFontColor(ColorType.NORMAL), 8.0F, Fonts.MEDIUM);
        nvg.drawCenteredText("Use WASD or the arrow keys", x + width / 2.0F,
                cardY + 81.0F, palette.getFontColor(ColorType.NORMAL), 7.0F, Fonts.REGULAR);
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

    private static final class Cell {
        private final int x;
        private final int y;

        private Cell(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
