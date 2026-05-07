package me.eldodebug.soar.management.remote.news;

import me.eldodebug.soar.utils.Multithreading;

import java.util.concurrent.CopyOnWriteArrayList;

public class NewsManager {

	private final CopyOnWriteArrayList<News> news = new CopyOnWriteArrayList<News>();
	
	public NewsManager() {
		Multithreading.runAsync(this::loadNews);
	}
	
	private void loadNews() {
		news.clear();
		news.add(new News(
				"Flax Client dev 1.0",
				"Development release",
				"This is the dev 1.0 build. Branding has been refreshed for Flax, legacy feed content was removed, and the client is now aligned to local Flax release notes."
		));
	}

	public CopyOnWriteArrayList<News> getNews() {
		return news;
	}
}
