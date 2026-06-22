package me.eldodebug.soar.gui.altmanager;

import java.awt.Color;
import java.io.IOException;
import java.util.List;

import org.lwjgl.input.Keyboard;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.altmanager.Account;
import me.eldodebug.soar.management.altmanager.AltManager;
import me.eldodebug.soar.management.altmanager.MicrosoftAuth;
import me.eldodebug.soar.management.color.palette.ColorPalette;
import me.eldodebug.soar.management.color.palette.ColorType;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.utils.mouse.MouseUtils;
import me.eldodebug.soar.utils.mouse.Scroll;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;

/**
 * Account switcher UI. Lists saved Microsoft accounts and lets the user add a
 * new one via the official device-code sign-in, or switch the active session.
 */
public class GuiAltManager extends GuiScreen {

	private final GuiScreen parent;
	private final Scroll scroll = new Scroll();

	private int x, y, width, height;

	private static final int ROW_STEP = 32;
	private static final int ROW_H = 28;

	// Background sign-in state (mutated from the auth thread, read while drawing).
	private volatile boolean busy;
	private volatile boolean showCode;
	private volatile String userCode = "";
	private volatile String verificationUri = "";
	private volatile String message = "";
	private Thread authThread;

	public GuiAltManager(GuiScreen parent) {
		this.parent = parent;
	}

	@Override
	public void initGui() {
		ScaledResolution sr = new ScaledResolution(mc);
		width = 300;
		height = 214;
		x = (sr.getScaledWidth() - width) / 2;
		y = (sr.getScaledHeight() - height) / 2;
	}

	private int listTop() {
		return y + 62;
	}

	private int listBottom() {
		return y + height - 30;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		NanoVGManager nvg = Glide.getInstance().getNanoVGManager();
		nvg.setupAndDraw(() -> drawNanoVG(nvg, mouseX, mouseY));
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	private void drawNanoVG(NanoVGManager nvg, int mouseX, int mouseY) {

		Glide instance = Glide.getInstance();
		ColorPalette palette = instance.getColorManager().getPalette();
		Color accent = instance.getColorManager().getCurrentColor().getInterpolateColor();
		AltManager altManager = instance.getAltManager();

		// Flat dim backdrop (no blur) so text renders cleanly against it.
		ScaledResolution sr = new ScaledResolution(mc);
		nvg.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), new Color(0, 0, 0, 150));

		nvg.drawShadow(x, y, width, height, 12);
		nvg.drawRoundedRect(x, y, width, height, 10, palette.getBackgroundColor(ColorType.NORMAL));

		// Header
		nvg.drawText("Alt Manager", x + 15, y + 13, palette.getFontColor(ColorType.DARK), 13, Fonts.SEMIBOLD);
		String active = mc.getSession() != null ? mc.getSession().getUsername() : "-";
		nvg.drawText("Playing as: " + active, x + width - 15 - nvg.getTextWidth("Playing as: " + active, 8, Fonts.REGULAR),
				y + 16, palette.getFontColor(ColorType.NORMAL), 8, Fonts.REGULAR);

		// Add button
		float addY = y + 32;
		boolean addHover = !busy && MouseUtils.isInside(mouseX, mouseY, x + 15, addY, width - 30, 22);
		nvg.drawRoundedRect(x + 15, addY, width - 30, 22, 6, alpha(accent, addHover ? 255 : 215));
		nvg.drawCenteredText("+  Add Microsoft Account", x + (width / 2F), addY + 7, Color.WHITE, 9.5F, Fonts.MEDIUM);

		// Account list
		List<Account> accounts = altManager.getAccounts();

		scroll.onScroll();
		scroll.onAnimation();
		int contentHeight = accounts.size() * ROW_STEP;
		int viewHeight = listBottom() - listTop();
		scroll.setMaxScroll(Math.max(0, contentHeight - viewHeight));

		nvg.save();
		nvg.scissor(x + 10, listTop(), width - 20, viewHeight);
		nvg.translate(0, scroll.getValue());

		if (accounts.isEmpty()) {
			nvg.drawCenteredText("No accounts yet - add one above.", x + (width / 2F), listTop() + 24,
					palette.getFontColor(ColorType.NORMAL), 9, Fonts.REGULAR);
		}

		int rowX = x + 15;
		int rowW = width - 30;

		for (int i = 0; i < accounts.size(); i++) {

			Account account = accounts.get(i);
			float rowY = listTop() + i * ROW_STEP;

			boolean isActive = account == altManager.getActiveAccount();
			nvg.drawRoundedRect(rowX, rowY, rowW, ROW_H, 6, palette.getBackgroundColor(ColorType.DARK));

			// avatar placeholder: accent tile + initial
			nvg.drawRoundedRect(rowX + 5, rowY + 4, 20, 20, 5, alpha(accent, 230));
			String initial = account.getProfileName().substring(0, 1).toUpperCase();
			nvg.drawCenteredText(initial, rowX + 15, rowY + 9, Color.WHITE, 10, Fonts.SEMIBOLD);

			nvg.drawText(account.getProfileName(), rowX + 32, rowY + 6, palette.getFontColor(ColorType.DARK), 10, Fonts.MEDIUM);
			nvg.drawText(isActive ? "Active session" : "Saved", rowX + 32, rowY + 17,
					palette.getFontColor(ColorType.NORMAL), 7.5F, Fonts.REGULAR);

			// remove
			float remX = rowX + rowW - 22;
			float remY = rowY + (ROW_H - 16) / 2F;
			boolean remHover = insideScrolled(mouseX, mouseY, remX, remY, 16, 16);
			nvg.drawRoundedRect(remX, remY, 16, 16, 5, palette.getBackgroundColor(ColorType.NORMAL));
			nvg.drawCenteredText("x", remX + 8, remY + 4.5F, remHover ? palette.getMaterialRed() : palette.getFontColor(ColorType.NORMAL), 9, Fonts.MEDIUM);

			// use
			float useW = 46;
			float useX = remX - 6 - useW;
			float useY = rowY + (ROW_H - 16) / 2F;
			boolean useHover = insideScrolled(mouseX, mouseY, useX, useY, useW, 16);
			nvg.drawRoundedRect(useX, useY, useW, 16, 5, isActive ? palette.getBackgroundColor(ColorType.NORMAL) : alpha(accent, useHover ? 255 : 220));
			nvg.drawCenteredText(isActive ? "In use" : "Use", useX + useW / 2F, useY + 4.5F,
					isActive ? palette.getFontColor(ColorType.NORMAL) : Color.WHITE, 8.5F, Fonts.MEDIUM);
		}

		nvg.restore();

		// Footer status line
		if (message != null && !message.isEmpty()) {
			nvg.drawCenteredText(message, x + (width / 2F), y + height - 18, palette.getFontColor(ColorType.NORMAL), 8, Fonts.REGULAR);
		}

		if (showCode) {
			drawCodeOverlay(nvg, palette, accent, mouseX, mouseY);
		}
	}

	private void drawCodeOverlay(NanoVGManager nvg, ColorPalette palette, Color accent, int mouseX, int mouseY) {

		nvg.drawRoundedRect(x, y, width, height, 10, new Color(0, 0, 0, 180));

		float cy = y + 34;
		nvg.drawCenteredText("Sign in to Microsoft", x + (width / 2F), cy, Color.WHITE, 12, Fonts.SEMIBOLD);

		nvg.drawCenteredText("1.  Open this page:", x + (width / 2F), cy + 26, new Color(210, 214, 222), 9, Fonts.REGULAR);
		nvg.drawCenteredText(verificationUri, x + (width / 2F), cy + 38, Color.WHITE, 9.5F, Fonts.MEDIUM);

		nvg.drawCenteredText("2.  Enter this code:", x + (width / 2F), cy + 62, new Color(210, 214, 222), 9, Fonts.REGULAR);
		nvg.drawCenteredText(userCode, x + (width / 2F), cy + 74, accent, 16, Fonts.SEMIBOLD);

		nvg.drawCenteredText(message, x + (width / 2F), cy + 100, new Color(190, 194, 202), 8.5F, Fonts.REGULAR);

		// buttons: Open page / Copy code / Cancel
		float bw = 70, bh = 18, gap = 8;
		float total = bw * 3 + gap * 2;
		float bx = x + (width - total) / 2F;
		float by = y + height - 34;

		drawOverlayButton(nvg, "Open Page", bx, by, bw, bh, alpha(accent, MouseUtils.isInside(mouseX, mouseY, bx, by, bw, bh) ? 255 : 220));
		drawOverlayButton(nvg, "Copy Code", bx + bw + gap, by, bw, bh, palette.getBackgroundColor(ColorType.DARK));
		drawOverlayButton(nvg, "Cancel", bx + (bw + gap) * 2, by, bw, bh, palette.getBackgroundColor(ColorType.DARK));
	}

	private void drawOverlayButton(NanoVGManager nvg, String text, float bx, float by, float bw, float bh, Color color) {
		nvg.drawRoundedRect(bx, by, bw, bh, 5, color);
		nvg.drawCenteredText(text, bx + bw / 2F, by + bh / 2F - 4, Color.WHITE, 8.5F, Fonts.MEDIUM);
	}

	private Color alpha(Color c, int a) {
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
	}

	// hit-test against a region that is drawn translated by the scroll offset
	private boolean insideScrolled(int mouseX, int mouseY, float rx, float ry, float w, float h) {
		float drawnY = ry + scroll.getValue();
		return drawnY + h >= listTop() && drawnY <= listBottom()
				&& MouseUtils.isInside(mouseX, mouseY, rx, drawnY, w, h);
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {

		try {
			super.mouseClicked(mouseX, mouseY, mouseButton);
		} catch (IOException ignored) {}

		if (mouseButton != 0) {
			return;
		}

		AltManager altManager = Glide.getInstance().getAltManager();

		if (showCode) {
			float bw = 70, bh = 18, gap = 8;
			float total = bw * 3 + gap * 2;
			float bx = x + (width - total) / 2F;
			float by = y + height - 34;
			if (MouseUtils.isInside(mouseX, mouseY, bx, by, bw, bh)) {
				openUrl(verificationUri);
			} else if (MouseUtils.isInside(mouseX, mouseY, bx + bw + gap, by, bw, bh)) {
				setClipboardString(userCode);
				message = "Code copied";
			} else if (MouseUtils.isInside(mouseX, mouseY, bx + (bw + gap) * 2, by, bw, bh)) {
				cancelAuth();
			}
			return;
		}

		// Add Microsoft account
		if (!busy && MouseUtils.isInside(mouseX, mouseY, x + 15, y + 32, width - 30, 22)) {
			startAdd();
			return;
		}

		// Account rows
		List<Account> accounts = altManager.getAccounts();
		int rowX = x + 15;
		int rowW = width - 30;

		for (int i = 0; i < accounts.size(); i++) {

			Account account = accounts.get(i);
			float rowY = listTop() + i * ROW_STEP;

			float remX = rowX + rowW - 22;
			float remY = rowY + (ROW_H - 16) / 2F;
			if (insideScrolled(mouseX, mouseY, remX, remY, 16, 16)) {
				altManager.remove(account);
				return;
			}

			float useW = 46;
			float useX = remX - 6 - useW;
			float useY = rowY + (ROW_H - 16) / 2F;
			if (!busy && account != altManager.getActiveAccount() && insideScrolled(mouseX, mouseY, useX, useY, useW, 16)) {
				startLogin(account);
				return;
			}
		}
	}

	private void startAdd() {
		busy = true;
		message = "Starting sign-in...";
		authThread = new Thread(() -> {
			try {
				MicrosoftAuth auth = new MicrosoftAuth();
				MicrosoftAuth.DeviceCode dc = auth.requestDeviceCode();
				userCode = dc.userCode;
				verificationUri = dc.verificationUri;
				showCode = true;
				message = "Waiting for sign-in...";
				Account account = auth.pollAndAuthenticate(dc);
				Glide.getInstance().getAltManager().add(account);
				showCode = false;
				message = "Added " + account.getProfileName();
			} catch (InterruptedException e) {
				showCode = false;
				message = "Sign-in cancelled";
			} catch (Exception e) {
				showCode = false;
				message = e.getMessage() != null ? e.getMessage() : "Sign-in failed";
			} finally {
				busy = false;
			}
		}, "FlaxClient-MSAuth");
		authThread.setDaemon(true);
		authThread.start();
	}

	private void startLogin(Account account) {
		busy = true;
		message = "Signing in as " + account.getProfileName() + "...";
		Thread thread = new Thread(() -> {
			try {
				Glide.getInstance().getAltManager().login(account);
				message = "Now playing as " + account.getProfileName();
			} catch (Exception e) {
				message = e.getMessage() != null ? e.getMessage() : "Sign-in failed";
			} finally {
				busy = false;
			}
		}, "FlaxClient-AltLogin");
		thread.setDaemon(true);
		thread.start();
	}

	private void cancelAuth() {
		if (authThread != null) {
			authThread.interrupt();
		}
		showCode = false;
		busy = false;
		message = "Sign-in cancelled";
	}

	private void openUrl(String url) {
		try {
			org.lwjgl.Sys.openURL(url);
		} catch (Throwable t) {
			message = "Open " + url + " in your browser";
		}
	}

	@Override
	public void keyTyped(char typedChar, int keyCode) {
		if (keyCode == Keyboard.KEY_ESCAPE) {
			if (showCode) {
				cancelAuth();
			} else {
				mc.displayGuiScreen(parent);
			}
		}
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}
