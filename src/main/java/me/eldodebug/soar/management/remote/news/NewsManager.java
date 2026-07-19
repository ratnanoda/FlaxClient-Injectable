package me.eldodebug.soar.management.remote.news;

import java.util.ArrayList;
import java.util.List;

public class NewsManager {

	private final List<News> news = new ArrayList<News>();
	
	public NewsManager() {
		loadNews();
	}
	
	private void loadNews() {
		news.clear();
		news.add(new News(
				"Flax Client Releases 1.0",
				"Release build",
				"Japanese text rendering has been repaired, module lists are taller, and the Windows launcher now ships as one client-embedded executable."
		));
		news.add(new News(
				"Smoother client experience",
				"Performance pass",
				"Repeated module filtering and sorting are now cached, off-screen ClickGUI rows are skipped, and static home data no longer starts unnecessary worker threads."
		));
	}

	public List<News> getNews() {
		return news;
	}
}
