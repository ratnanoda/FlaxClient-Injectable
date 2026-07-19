package me.eldodebug.soar.management.remote.changelog;

import java.util.ArrayList;
import java.util.List;

public class ChangelogManager {

	private final List<Changelog> changelogs = new ArrayList<Changelog>();

	public ChangelogManager() {
		loadChangelog();
	}
	
	private void loadChangelog() {
		changelogs.clear();
		changelogs.add(new Changelog("Releases 1.0: Added the all-in-one Windows launcher", ChangelogType.ADDED));
		changelogs.add(new Changelog("Releases 1.0: Increased the module list height", ChangelogType.ADDED));
		changelogs.add(new Changelog("Releases 1.0: Restored corrupted Japanese translations", ChangelogType.FIXED));
		changelogs.add(new Changelog("Releases 1.0: Fixed UTF-8 BOM translation lookup", ChangelogType.FIXED));
	}

	public List<Changelog> getChangelogs() {
		return changelogs;
	}
}
