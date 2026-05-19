package me.eldodebug.flax.gui.modmenu;

import me.eldodebug.flax.core.Glide;
import me.eldodebug.flax.management.mods.Mod;
import me.eldodebug.flax.management.mods.ModCategory;
import me.eldodebug.flax.management.mods.StubMod;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlaxModMenuScreen extends Screen {

	private ModCategory selectedCategory = ModCategory.RENDER;
	private List<Mod> visibleMods = List.of();
	private final List<RowEntry> rowEntries = new ArrayList<>();
	private final Map<String, Float> rowAnimations = new HashMap<>();
	private float openAnimation = 0.0F;

	public FlaxModMenuScreen() {
		super(Text.literal("FlaxClient 1.21.11"));
		refreshVisibleMods();
	}

	@Override
	protected void init() {
		clearChildren();

		int panelLeft = Math.max(10, width / 2 - 250);
		int y = 34;
		for (ModCategory category : ModCategory.values()) {
			if (category == ModCategory.ALL) {
				continue;
			}
			ModCategory captured = category;
			addDrawableChild(ButtonWidget.builder(Text.literal(category.name()), button -> {
				selectedCategory = captured;
				refreshVisibleMods();
			}).dimensions(panelLeft + 14, y, 110, 20).build());
			y += 24;
		}

		addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> close())
				.dimensions(panelLeft + 430, 16, 60, 20)
				.build());
	}

	private void refreshVisibleMods() {
		List<Mod> mods = new ArrayList<>();
		for (Mod mod : Glide.getInstance().getModManager().getMods()) {
			if ("internal_settings".equals(mod.getId())) {
				continue;
			}
			if (mod.getCategory() == selectedCategory) {
				mods.add(mod);
			}
		}
		mods.sort(Comparator.comparing(Mod::getName));
		visibleMods = mods;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		openAnimation += (1.0F - openAnimation) * Math.min(0.45F, 0.1F + delta * 0.2F);
		rowEntries.clear();

		context.fill(0, 0, width, height, 0xB3060A10);

		int panelWidth = 500;
		int panelHeight = 330;
		int panelLeft = width / 2 - panelWidth / 2;
		int panelTop = height / 2 - panelHeight / 2;
		int animatedBottom = panelTop + (int) (panelHeight * openAnimation);
		context.fill(panelLeft, panelTop, panelLeft + panelWidth, animatedBottom, 0xD0131722);
		context.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 2, 0xFF2EC4B6);

		context.drawTextWithShadow(textRenderer, Text.literal("FlaxClient 1.21.11"), panelLeft + 14, panelTop + 14, 0xFFFFFFFF);
		context.drawTextWithShadow(
				textRenderer,
				Text.literal("Category: " + selectedCategory.name() + "  |  Modules: " + visibleMods.size()),
				panelLeft + 14,
				panelTop + 26,
				0xFFB0BEC5);

		int implemented = 0;
		for (Mod mod : visibleMods) {
			if (!(mod instanceof StubMod)) {
				implemented++;
			}
		}
		context.drawTextWithShadow(
				textRenderer,
				Text.literal("Ported: " + implemented + " / " + visibleMods.size()),
				panelLeft + 350,
				panelTop + 26,
				0xFF90CAF9);

		int rowX = panelLeft + 136;
		int rowY = panelTop + 52;
		int rowW = panelWidth - 150;
		int rowH = 18;

		for (Mod mod : visibleMods) {
			float state = rowAnimations.getOrDefault(mod.getId(), mod.isToggled() ? 1.0F : 0.0F);
			float target = mod.isToggled() ? 1.0F : 0.0F;
			state += (target - state) * Math.min(0.5F, 0.2F + delta * 0.25F);
			rowAnimations.put(mod.getId(), state);

			int base = blendColor(0x2AFFFFFF, 0x4040E0A0, state);
			context.fill(rowX, rowY, rowX + rowW, rowY + rowH, base);
			context.fill(rowX, rowY, rowX + 2, rowY + rowH, mod.isToggled() ? 0xFF2EC4B6 : 0xFF455A64);

			String stateText = mod.isToggled() ? "ON" : "OFF";
			String text = mod.getName() + " [" + stateText + "]";
			int textColor = mod.isToggled() ? 0xFFD5FFE8 : 0xFFF5F5F5;
			context.drawTextWithShadow(textRenderer, Text.literal(text), rowX + 6, rowY + 5, textColor);

			if (mod instanceof StubMod) {
				context.drawTextWithShadow(textRenderer, Text.literal("stub"), rowX + rowW - 28, rowY + 5, 0xFFFFB74D);
			}

			rowEntries.add(new RowEntry(mod, rowX, rowY, rowX + rowW, rowY + rowH));
			rowY += 21;
			if (rowY > panelTop + panelHeight - 24) {
				break;
			}
		}

		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (super.mouseClicked(click, doubled)) {
			return true;
		}

		double mouseX = click.x();
		double mouseY = click.y();
		for (RowEntry row : rowEntries) {
			if (row.contains(mouseX, mouseY)) {
				row.mod().toggle();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private int blendColor(int from, int to, float progress) {
		progress = Math.max(0.0F, Math.min(1.0F, progress));
		int a1 = (from >>> 24) & 0xFF;
		int r1 = (from >>> 16) & 0xFF;
		int g1 = (from >>> 8) & 0xFF;
		int b1 = from & 0xFF;

		int a2 = (to >>> 24) & 0xFF;
		int r2 = (to >>> 16) & 0xFF;
		int g2 = (to >>> 8) & 0xFF;
		int b2 = to & 0xFF;

		int a = (int) (a1 + (a2 - a1) * progress);
		int r = (int) (r1 + (r2 - r1) * progress);
		int g = (int) (g1 + (g2 - g1) * progress);
		int b = (int) (b1 + (b2 - b1) * progress);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private record RowEntry(Mod mod, int minX, int minY, int maxX, int maxY) {
		private boolean contains(double x, double y) {
			return x >= minX && x <= maxX && y >= minY && y <= maxY;
		}
	}
}
