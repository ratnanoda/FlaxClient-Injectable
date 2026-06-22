package me.eldodebug.soar.management.mods.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.injection.interfaces.IMixinPlayerControllerMP;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventRender2D;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.BooleanSetting;
import me.eldodebug.soar.management.mods.settings.impl.ColorSetting;
import me.eldodebug.soar.management.mods.settings.impl.ComboSetting;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import me.eldodebug.soar.management.mods.settings.impl.combo.Option;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.utils.animation.simple.SimpleAnimation;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class BreakProgressMod extends Mod {

	private final ComboSetting styleSetting = new ComboSetting(TranslateText.STYLE, this, TranslateText.BAR, new ArrayList<Option>(Arrays.asList(
			new Option(TranslateText.BAR), new Option(TranslateText.CIRCLE), new Option(TranslateText.TEXT))));
	private final ColorSetting colorSetting = new ColorSetting(TranslateText.COLOR, this, new Color(0, 255, 170), false);
	private final NumberSetting widthSetting = new NumberSetting(TranslateText.WIDTH, this, 80, 40, 160, true);
	private final NumberSetting heightSetting = new NumberSetting(TranslateText.HEIGHT, this, 5, 2, 12, true);
	private final NumberSetting offsetSetting = new NumberSetting(TranslateText.OFFSET, this, 20, 5, 80, true);
	private final BooleanSetting textSetting = new BooleanSetting(TranslateText.TEXT, this, true);

	private final SimpleAnimation fadeAnimation = new SimpleAnimation();
	private final SimpleAnimation progressAnimation = new SimpleAnimation();
	private boolean wasBreaking;
	private float lastProgress;
	private BlockPos minedPos;

	public BreakProgressMod() {
		super(TranslateText.BREAK_PROGRESS, TranslateText.BREAK_PROGRESS_DESCRIPTION, ModCategory.GHOST);
	}

	@Override
	public void onEnable() {
		super.onEnable();
		fadeAnimation.setValue(0.0F);
		progressAnimation.setValue(0.0F);
		wasBreaking = false;
		lastProgress = 0.0F;
	}

	@EventTarget
	public void onRender2D(EventRender2D event) {

		if(mc.thePlayer == null || mc.theWorld == null) {
			return;
		}

		float progress = getBreakProgress();
		boolean breaking = progress > 0.0F;

		if(breaking) {
			// Start each break from an empty bar so the fill animates in sync.
			if(!wasBreaking) {
				progressAnimation.setValue(0.0F);
			}
			progressAnimation.setAnimation(progress, 26);
			fadeAnimation.setAnimation(1.0F, 24);
			lastProgress = progress;

			if(mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK) {
				minedPos = mc.objectMouseOver.getBlockPos();
			}
		} else {
			// Only complete the bar when the block was actually destroyed (the
			// mined position is now air). If the player merely stopped mining,
			// fade out at the current value instead of jumping to 100%.
			if(wasBreaking && lastProgress >= 0.5F && minedPos != null && mc.theWorld.isAirBlock(minedPos)) {
				progressAnimation.setValue(1.0F);
			}
			fadeAnimation.setAnimation(0.0F, 16);
		}

		wasBreaking = breaking;

		final float fade = fadeAnimation.getValue();

		if(fade <= 0.01F) {
			progressAnimation.setValue(progress);
			lastProgress = 0.0F;
			minedPos = null;
			return;
		}

		final float drawnProgress = MathHelper.clamp_float(progressAnimation.getValue(), 0.0F, 1.0F);

		NanoVGManager nvg = Glide.getInstance().getNanoVGManager();
		nvg.setupAndDraw(() -> draw(nvg, fade, drawnProgress));
	}

	private void draw(NanoVGManager nvg, float fade, float progress) {

		ScaledResolution sr = new ScaledResolution(mc);
		float centerX = sr.getScaledWidth() / 2.0F;
		// Pure opacity fade - no positional slide.
		float baseY = sr.getScaledHeight() / 2.0F + offsetSetting.getValueFloat();

		if(styleSetting.getOption().getTranslate().equals(TranslateText.CIRCLE)) {
			drawCircle(nvg, centerX, baseY, fade, progress);
		} else if(styleSetting.getOption().getTranslate().equals(TranslateText.TEXT)) {
			drawText(nvg, centerX, baseY, fade, progress);
		} else {
			drawBar(nvg, centerX, baseY, fade, progress);
		}
	}

	private void drawBar(NanoVGManager nvg, float centerX, float y, float fade, float progress) {

		float barWidth = widthSetting.getValueFloat();
		float barHeight = heightSetting.getValueFloat();
		float radius = barHeight / 2.0F;
		float x = centerX - barWidth / 2.0F;

		int alpha = (int) (fade * 255);
		Color base = colorSetting.getColor();
		Color background = new Color(0, 0, 0, (int) (fade * 140));
		Color fill = new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);

		nvg.drawRoundedRect(x, y, barWidth, barHeight, radius, background);

		float fillWidth = barWidth * progress;
		if(fillWidth > 0.0F) {
			nvg.drawRoundedRect(x, y, Math.max(fillWidth, barHeight), barHeight, radius, fill);
		}

		if(textSetting.isToggled()) {
			nvg.drawCenteredText(percent(progress), centerX, y - 11.0F, new Color(255, 255, 255, alpha), 8.0F, Fonts.REGULAR);
		}
	}

	private void drawCircle(NanoVGManager nvg, float centerX, float topY, float fade, float progress) {

		float radius = 11.0F;
		float stroke = Math.max(2.0F, heightSetting.getValueFloat() * 0.7F);
		float cy = topY + radius;

		int alpha = (int) (fade * 255);
		Color base = colorSetting.getColor();
		Color background = new Color(255, 255, 255, (int) (fade * 60));
		Color fill = new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);

		// Full background ring, then a clockwise arc starting from the top.
		nvg.drawArc(centerX, cy, radius, -90.0F, 270.0F, stroke, background);
		if(progress > 0.0F) {
			nvg.drawArc(centerX, cy, radius, -90.0F, -90.0F + (360.0F * progress), stroke, fill);
		}

		if(textSetting.isToggled()) {
			nvg.drawCenteredText(percent(progress), centerX, cy - 3.5F, new Color(255, 255, 255, alpha), 7.0F, Fonts.REGULAR);
		}
	}

	private void drawText(NanoVGManager nvg, float centerX, float y, float fade, float progress) {

		int alpha = (int) (fade * 255);
		Color base = colorSetting.getColor();
		Color textColor = new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);

		nvg.drawCenteredText(percent(progress), centerX, y, textColor, 16.0F, Fonts.SEMIBOLD);
	}

	private String percent(float progress) {
		return Math.round(progress * 100.0F) + "%";
	}

	private float getBreakProgress() {

		if(mc.playerController == null || !(mc.playerController instanceof IMixinPlayerControllerMP)) {
			return 0.0F;
		}

		float damage = ((IMixinPlayerControllerMP) mc.playerController).getCurBlockDamageMP();
		return MathHelper.clamp_float(damage, 0.0F, 1.0F);
	}
}
