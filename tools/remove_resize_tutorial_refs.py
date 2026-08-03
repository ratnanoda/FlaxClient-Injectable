from pathlib import Path

path = Path("src/main/java/me/eldodebug/soar/gui/modmenu/GuiModMenu.java")
text = path.read_text(encoding="utf-8")

old_draw = "\t\tboolean resizeTutorial = currentCategory instanceof ModuleCategory\n\t\t\t\t&& ((ModuleCategory) currentCategory).isResizeTutorialVisible();\n\t\tif(!resizeTutorial) beginnerGuide.draw(mouseX, mouseY, partialTicks);"
new_draw = "\t\tbeginnerGuide.draw(mouseX, mouseY, partialTicks);"
if old_draw not in text:
    raise RuntimeError("Resize tutorial draw reference was not found")
text = text.replace(old_draw, new_draw, 1)

old_click = "\t\tboolean resizeTutorial = currentCategory instanceof ModuleCategory\n\t\t\t\t&& ((ModuleCategory) currentCategory).isResizeTutorialVisible();\n\t\tif(!resizeTutorial && beginnerGuide.mouseClicked(mouseX, mouseY, mouseButton)) return;"
new_click = "\t\tif(beginnerGuide.mouseClicked(mouseX, mouseY, mouseButton)) return;"
if old_click not in text:
    raise RuntimeError("Resize tutorial click reference was not found")
text = text.replace(old_click, new_click, 1)

path.write_text(text, encoding="utf-8")
print("Removed GuiModMenu resize tutorial references")
