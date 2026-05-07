package me.eldodebug.soar.gui.mainmenu.impl.welcome;

import java.awt.Color;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.gui.mainmenu.GuiGlideMainMenu;
import me.eldodebug.soar.gui.mainmenu.MainMenuScene;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.utils.TimerUtils;
import me.eldodebug.soar.utils.animation.normal.Animation;
import me.eldodebug.soar.utils.animation.normal.Direction;
import me.eldodebug.soar.utils.animation.normal.other.DecelerateAnimation;
import me.eldodebug.soar.utils.render.BlurUtils;
import net.minecraft.client.gui.ScaledResolution;

public class WelcomeMessageScene extends MainMenuScene {

	// Todo: translate text

	private Animation fadeAnimation;
	private int step;
	private String message;
	
	private TimerUtils timer = new TimerUtils();
	
	public WelcomeMessageScene(GuiGlideMainMenu parent) {
		super(parent);
		
		step = 0;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		
		ScaledResolution sr = new ScaledResolution(mc);
		NanoVGManager nvg = Glide.getInstance().getNanoVGManager();
		String hello = "Hello!";
		String welcomeMessage = "Welcome to FlaxClient";
		String setupMessage = "An Updated Version of";
		String setupMessageLine2 = "Soar / Glide Client";
		String setupMessage2 = "Time to setup Glide.";
		
		BlurUtils.drawBlurScreen(14);
		
		if(fadeAnimation == null && this.getParent().isDoneBackgroundAnimation()) {
			fadeAnimation = new DecelerateAnimation(800, 1);
			fadeAnimation.setDirection(Direction.FORWARDS);
			fadeAnimation.reset();
			timer.reset();
		}
		
		if(fadeAnimation != null) {
			
			switch(step) {
				case 0:
					message = hello;
					break;
				case 1:
					message = welcomeMessage;
					break;
				case 2:
					message = setupMessage;
					break;
				case 3:
					message = setupMessage2;
					break;
			}
			
			nvg.setupAndDraw(() -> {
				Color textColor = new Color(255, 255, 255, (int) (fadeAnimation.getValueFloat() * 255));
				float centerX = sr.getScaledWidth() / 2F;
				float centerY = sr.getScaledHeight() / 2F;
				
				if(step == 2) {
					nvg.drawCenteredText(setupMessage, centerX, centerY - 18, textColor, 26, Fonts.REGULAR);
					nvg.drawCenteredText(setupMessageLine2, centerX, centerY + 10, textColor, 26, Fonts.REGULAR);
				}else {
					nvg.drawCenteredText(message, centerX,
							centerY - (nvg.getTextHeight(message, 26, Fonts.REGULAR) / 2),
							textColor, 26, Fonts.REGULAR);
				}
			});
			
			if(timer.delay(2500) && fadeAnimation.getDirection().equals(Direction.FORWARDS)) {
				fadeAnimation.setDirection(Direction.BACKWARDS);
				timer.reset();
			}
			
			if(fadeAnimation.isDone(Direction.BACKWARDS)) {
				
				if(step == 3) {
					this.setCurrentScene(this.getSceneByClass(LanguageSelectScene.class));
					return;
				}
				
				step++;
				fadeAnimation.setDirection(Direction.FORWARDS);
			}
		}
	}
}
