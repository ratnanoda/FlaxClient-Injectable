from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def require_change(before, after, label):
    if before == after:
        raise RuntimeError("No change made for: " + label)
    return after


module_path = ROOT / "src/main/java/me/eldodebug/soar/gui/modmenu/category/impl/ModuleCategory.java"
module = module_path.read_text(encoding="utf-8")
original_module = module

module = module.replace("\tprivate static boolean resizeTutorialClaimed;\n", "")
module = module.replace(
    "\tprivate final SimpleAnimation resizeTutorialAnimation = new SimpleAnimation();\n"
    "\tprivate boolean resizeTutorialActive;\n",
    "",
)

module = re.sub(
    r"\t@Override\n\tpublic void initCategory\(\) \{\n\t\tresetScene\(\);\n"
    r"\t\tresizeTutorialActive = isResizableLayout\(\).*?\n\t\}\n",
    "\t@Override\n\tpublic void initCategory() {\n\t\tresetScene();\n\t}\n",
    module,
    count=1,
    flags=re.S,
)

module = module.replace(
    "\t\tdrawDropdowns(nvg, palette, accentColor, mouseX, mouseY, partialTicks);\n"
    "\t\tdrawResizeTutorial(nvg, palette, accentColor);\n",
    "\t\tdrawDropdowns(nvg, palette, accentColor, mouseX, mouseY, partialTicks);\n",
)

handle_pattern = re.compile(
    r"\tprivate void drawResizeHandles\(NanoVGManager nvg, float x, float y, float width, float height,\n"
    r"\t\t\tint mouseX, int mouseY\) \{.*?\n\t\}\n\n\tprivate int getResizeEdges",
    re.S,
)
new_handle = r'''	private void drawResizeHandles(NanoVGManager nvg, float x, float y, float width, float height,
			int mouseX, int mouseY) {
		int edges = getResizeEdges(mouseX, mouseY, x, y, width, height);
		Color edge = new Color(255, 255, 255, edges == 0 ? 28 : 82);
		nvg.drawRect(x + width - 10.0F, y + height - 2.0F, 8.0F, 1.0F, edge);
		nvg.drawRect(x + width - 2.0F, y + height - 10.0F, 1.0F, 8.0F, edge);
	}

	private int getResizeEdges'''
module, handle_count = handle_pattern.subn(new_handle, module, count=1)
if handle_count != 1:
    raise RuntimeError("Resize handle method was not found")

module = re.sub(
    r"\n\tpublic boolean isResizeTutorialVisible\(\) \{.*?\n\tprivate Color translucent",
    "\n\tprivate Color translucent",
    module,
    count=1,
    flags=re.S,
)

module = module.replace(
    "\t\tif(mouseButton == 0 && isResizeTutorialVisible()) {\n"
    "\t\t\tdismissResizeTutorial();\n"
    "\t\t\treturn;\n"
    "\t\t}\n",
    "",
)

module = require_change(original_module, module, "ModuleCategory tutorial removal")
module_path.write_text(module, encoding="utf-8")


guide_path = ROOT / "src/main/java/me/eldodebug/soar/gui/modmenu/BeginnerGuideOverlay.java"
guide = guide_path.read_text(encoding="utf-8")
original_guide = guide

guide = guide.replace(
    '            "Finding Your Way Around",\n'
    '            "YouTube: Video and Music",\n'
    '            "Useful Tips"',
    '            "Finding Your Way Around",\n'
    '            "Resizing the Ghost List",\n'
    '            "YouTube: Video and Music",\n'
    '            "Useful Tips"',
    1,
)

guide = guide.replace(
    '            "Paste a supported YouTube URL into the YouTube page and select Download. FlaxClient includes yt-dlp and FFmpeg, so no separate tools are required.\\n\\n"\n'
    '                    + "Video mode uses the normal picture-in-picture player. Music mode plays the same downloaded media as audio and shows the compact player in the bottom-right corner. You can switch modes before or during playback.",',
    '            "The Ghost module list works like a small desktop window.\\n\\n"\n'
    '                    + "Drag its left, right, top, or bottom edge to resize one direction. Drag a corner to change width and height together.\\n\\n"\n'
    '                    + "The two small lines in the bottom-right corner show that the list is resizable. Use Reset in the title bar to restore the default size, position, open state, and scroll position.",\n'
    '            "Paste a supported YouTube URL into the YouTube page and select Download. FlaxClient includes yt-dlp and FFmpeg, so no separate tools are required.\\n\\n"\n'
    '                    + "Video mode uses the normal picture-in-picture player. Music mode plays the same downloaded media as audio and shows the compact player in the bottom-right corner. You can switch modes before or during playback.",',
    1,
)

guide = require_change(original_guide, guide, "Beginner guide resize page")
guide_path.write_text(guide, encoding="utf-8")

print("Removed the Ghost spotlight tutorial, restored line resize handles, and added resize instructions to the beginner guide")
