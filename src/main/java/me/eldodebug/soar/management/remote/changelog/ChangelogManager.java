package me.eldodebug.soar.management.remote.changelog;

import java.util.concurrent.CopyOnWriteArrayList;

import me.eldodebug.soar.utils.Multithreading;

public class ChangelogManager {

	private CopyOnWriteArrayList<Changelog> changelogs = new CopyOnWriteArrayList<Changelog>();

	public ChangelogManager() {
		Multithreading.runAsync(() -> loadChangelog());
	}
	
	private void loadChangelog() {
		changelogs.clear();
		changelogs.add(new Changelog("dev 1.0: Flax cape and icon updates are now available in-client", ChangelogType.ADDED));
		changelogs.add(new Changelog("dev 1.0: Home feed and version labels now use Flax branding", ChangelogType.FIXED));
		changelogs.add(new Changelog("dev 1.0: Removed legacy Glide release history from the panel", ChangelogType.REMOVED));
	}

	public CopyOnWriteArrayList<Changelog> getChangelogs() {
		return changelogs;
	}
}
