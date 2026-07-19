use std::env;
use std::fs;
use std::path::PathBuf;

fn main() {
    println!("cargo:rerun-if-env-changed=FLAX_EMBED_JAR");
    println!("cargo:rerun-if-changed=FlaxClient-Release.jar");
    println!("cargo:rerun-if-changed=../build/libs/FlaxClient-Release.jar");
    println!("cargo:rerun-if-changed=assets/tools/yt-dlp-linux-x86_64");
    println!("cargo:rerun-if-changed=assets/tools/yt-dlp-windows-x86_64.exe");
    println!("cargo:rerun-if-changed=assets/tools/yt-dlp-macos");
    println!("cargo:rerun-if-changed=assets/tools/ffmpeg-linux-x86_64.zip");
    println!("cargo:rerun-if-changed=assets/tools/ffmpeg-windows-x86_64.zip");
    println!("cargo:rerun-if-changed=assets/tools/ffmpeg-macos-x86_64.zip");

    let manifest_dir = PathBuf::from(env::var_os("CARGO_MANIFEST_DIR").expect("manifest dir"));
    let env_override = env::var_os("FLAX_EMBED_JAR").map(PathBuf::from);
    let candidates = [
        env_override,
        Some(manifest_dir.join("FlaxClient-Release.jar")),
        Some(
            manifest_dir
                .join("..")
                .join("build")
                .join("libs")
                .join("FlaxClient-Release.jar"),
        ),
    ];

    let source = candidates
        .into_iter()
        .flatten()
        .find(|path| path.exists())
        .unwrap_or_else(|| {
            panic!(
                "FlaxClient-Release.jar was not found. Build the 1.8.9 client first with `../gradlew build` \
(or `..\\\\gradlew.bat build` on Windows), or place the jar next to Cargo.toml, then rerun `cargo build --release`."
            )
        });

    let out_dir = PathBuf::from(env::var_os("OUT_DIR").expect("OUT_DIR"));
    let dest = out_dir.join("FlaxClient-Release.jar");
    fs::copy(&source, &dest).unwrap_or_else(|error| {
        panic!(
            "failed to copy embedded FlaxClient jar from {} to {}: {error}",
            source.display(),
            dest.display()
        )
    });

    let target_os = env::var("CARGO_CFG_TARGET_OS").expect("target os");
    let tool_name = match target_os.as_str() {
        "windows" => "yt-dlp-windows-x86_64.exe",
        "macos" => "yt-dlp-macos",
        _ => "yt-dlp-linux-x86_64",
    };
    let tool_source = manifest_dir.join("assets").join("tools").join(tool_name);
    fs::copy(&tool_source, out_dir.join("yt-dlp-embedded")).unwrap_or_else(|error| {
        panic!("failed to copy {}: {error}", tool_source.display())
    });

    let ffmpeg_name = match target_os.as_str() {
        "windows" => "ffmpeg-windows-x86_64.zip",
        "macos" => "ffmpeg-macos-x86_64.zip",
        _ => "ffmpeg-linux-x86_64.zip",
    };
    let ffmpeg_source = manifest_dir
        .join("assets")
        .join("tools")
        .join(ffmpeg_name);
    fs::copy(&ffmpeg_source, out_dir.join("ffmpeg-embedded.zip")).unwrap_or_else(|error| {
        panic!("failed to copy {}: {error}", ffmpeg_source.display())
    });
}
