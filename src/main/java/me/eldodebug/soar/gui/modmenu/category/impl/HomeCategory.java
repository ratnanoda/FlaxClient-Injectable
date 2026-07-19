package me.eldodebug.soar.gui.modmenu.category.impl;

import java.awt.Color;
import java.util.List;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.gui.modmenu.GuiModMenu;
import me.eldodebug.soar.gui.modmenu.category.Category;
import me.eldodebug.soar.management.remote.changelog.Changelog;
import me.eldodebug.soar.management.remote.changelog.ChangelogManager;
import me.eldodebug.soar.management.color.AccentColor;
import me.eldodebug.soar.management.color.ColorManager;
import me.eldodebug.soar.management.color.palette.ColorPalette;
import me.eldodebug.soar.management.color.palette.ColorType;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.management.nanovg.font.LegacyIcon;
import me.eldodebug.soar.management.remote.news.News;
import me.eldodebug.soar.management.remote.news.NewsManager;
import me.eldodebug.soar.utils.mouse.MouseUtils;
import me.eldodebug.soar.utils.mouse.Scroll;

public class HomeCategory extends Category {

	public HomeCategory(GuiModMenu parent) {
		super(parent, TranslateText.HOME, LegacyIcon.HOME, false, false);
	}
	private Scroll changelogScroll = new Scroll();
	private Scroll newsScroll = new Scroll();
	private float[] newsTitleHeights = new float[0];
	private float[] newsSubtitleHeights = new float[0];
	private float[] newsBodyHeights = new float[0];
	private float[] devlogTextHeights = new float[0];

	@Override
	public void initGui() {
		changelogScroll.resetAll();
		newsScroll.resetAll();
	}

	Color onlineColour = new Color(85, 155, 89, 255);
	Color noColour = new Color(0, 0, 0, 0);

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		
		Glide instance = Glide.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		ColorManager colorManager = instance.getColorManager();
		ColorPalette palette = colorManager.getPalette();
		AccentColor currentColor = colorManager.getCurrentColor();
		ChangelogManager changelogManager = instance.getChangelogManager();
		NewsManager newsManager = instance.getNewsManager();
		List<News> newsItems = newsManager.getNews();
		List<Changelog> devlogItems = changelogManager.getChangelogs();
		ensureLayoutCache(nvg, newsItems, devlogItems);
		int outerPadding = 15;

		// news
		int offsetNewsY = 0;
		nvg.drawRoundedRect(this.getX() + outerPadding, this.getY() + outerPadding, 200, 250, 8, palette.getBackgroundColor(ColorType.DARK));
		nvg.drawText(TranslateText.NEWS.getText(), this.getX() + outerPadding + 8, this.getY() + 15 + 8, palette.getFontColor(ColorType.DARK), 11F, Fonts.SEMIBOLD);

		nvg.save();
		nvg.scissor(this.getX() + outerPadding, this.getY() + outerPadding + 20, 200, 230);
		nvg.translate(0, newsScroll.getValue());

		for(int i = 0; i < newsItems.size(); i++) {
			News n = newsItems.get(i);
			float titleSize = newsTitleHeights[i];
			nvg.drawTextBox(n.getTitle(), this.getX() + outerPadding + 8, this.getY() + 43F + offsetNewsY, 180, palette.getFontColor(ColorType.DARK), 10, Fonts.SEMIBOLD);
			offsetNewsY += (int) (titleSize);
			float subTitleSize = newsSubtitleHeights[i];
			nvg.drawTextBox(n.getSubTitle(), this.getX() + outerPadding + 8, this.getY() + 43F + offsetNewsY, 180, palette.getFontColor(ColorType.DARK), 8.5F, Fonts.MEDIUM);
			offsetNewsY += (int) (subTitleSize + 1);
			float bodySize = newsBodyHeights[i];
			nvg.drawTextBox(n.getBody(), this.getX() + outerPadding + 8, this.getY() + 43F + offsetNewsY, 180, palette.getFontColor(ColorType.DARK), 8, Fonts.REGULAR);
			offsetNewsY += (int) (bodySize + 9);
		}
		nvg.restore();

		if(MouseUtils.isInside(mouseX, mouseY,this.getX() + outerPadding, this.getY() + outerPadding, 200, 250)) {newsScroll.onScroll();}
		newsScroll.onAnimation();
		newsScroll.setMaxScroll(Math.max(offsetNewsY - 225, 0));

		// shadow
		nvg.drawVerticalGradientRect(this.getX() + outerPadding + 8, this.getY() + outerPadding + 20, 200 - 16, 8, palette.getBackgroundColor(ColorType.DARK), noColour);
		nvg.drawVerticalGradientRect(this.getX() + outerPadding + 8, this.getY() + outerPadding +  250 - 8, 200 - 16, 8, noColour, palette.getBackgroundColor(ColorType.DARK));


		// Devlog

		int offsetChangelogY = 0;

		nvg.drawRoundedRect(this.getX() + 230, this.getY() + outerPadding, 174, 250, 8, palette.getBackgroundColor(ColorType.DARK));
		nvg.drawText("Devlog", this.getX() + 230 + 8, this.getY() + 15 + 8,
				palette.getFontColor(ColorType.DARK), 11F, Fonts.SEMIBOLD);

		nvg.save();
		nvg.scissor(this.getX() + 230, this.getY() + outerPadding + 20, 174, 230);
		nvg.translate(0, changelogScroll.getValue());

		for(int i = 0; i < devlogItems.size(); i++) {
			Changelog c = devlogItems.get(i);
			float tbSize = devlogTextHeights[i];
			nvg.drawRoundedRect(this.getX() + 230 + 8, this.getY() + 40 + offsetChangelogY + ((tbSize/2)-4), 13, 13, 7F, c.getType().getColor());
			nvg.drawCenteredText(c.getType().getText(), this.getX() + 230 + 8 + (13 / 2), this.getY() + 42F + offsetChangelogY + ((tbSize/2)-3), Color.WHITE, 7, Fonts.LEGACYICON);
			nvg.drawTextBox(c.getText(), this.getX() + 230 + 25, this.getY() + 43F + offsetChangelogY, 174 - 33, palette.getFontColor(ColorType.DARK), 8, Fonts.MEDIUM);
			offsetChangelogY+= (int) (tbSize + 9);
		}
		nvg.restore();
		if(MouseUtils.isInside(mouseX, mouseY,this.getX() + 230, this.getY() + outerPadding, 174, 250)) {changelogScroll.onScroll();}
		changelogScroll.onAnimation();
		changelogScroll.setMaxScroll(Math.max(offsetChangelogY - 225, 0));

		nvg.drawVerticalGradientRect(this.getX() + 230 + 8, this.getY() + outerPadding + 20, 174 - 16, 8, palette.getBackgroundColor(ColorType.DARK), noColour);
		nvg.drawVerticalGradientRect(this.getX() + 230 + 8, this.getY() + outerPadding +  250 - 8, 174 - 16, 8, noColour, palette.getBackgroundColor(ColorType.DARK));

	}

	private void ensureLayoutCache(NanoVGManager nvg, List<News> newsItems, List<Changelog> devlogItems) {
		if(newsTitleHeights.length != newsItems.size()) {
			newsTitleHeights = new float[newsItems.size()];
			newsSubtitleHeights = new float[newsItems.size()];
			newsBodyHeights = new float[newsItems.size()];
			for(int i = 0; i < newsItems.size(); i++) {
				News news = newsItems.get(i);
				newsTitleHeights[i] = nvg.getTextBoxHeight(news.getTitle(), 10, Fonts.SEMIBOLD, 180);
				newsSubtitleHeights[i] = nvg.getTextBoxHeight(news.getSubTitle(), 8.5F, Fonts.MEDIUM, 180);
				newsBodyHeights[i] = nvg.getTextBoxHeight(news.getBody(), 8, Fonts.REGULAR, 180);
			}
		}
		if(devlogTextHeights.length != devlogItems.size()) {
			devlogTextHeights = new float[devlogItems.size()];
			for(int i = 0; i < devlogItems.size(); i++) {
				devlogTextHeights[i] = nvg.getTextBoxHeight(devlogItems.get(i).getText(), 8, Fonts.MEDIUM, 174 - 33);
			}
		}
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
	}
}
