package me.eldodebug.soar.attach.modern;

public enum ModernCategory {
    HUD("HUD"),
    PLAYER("Player"),
    RENDER("Render"),
    GHOST("Ghost"),
    BLATANT("Blatant"),
    WORLD("World"),
    OTHER("Other");

    private final String displayName;

    ModernCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
