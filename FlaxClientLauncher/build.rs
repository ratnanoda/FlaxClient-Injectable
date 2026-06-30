use std::env;
use std::fs;
use std::path::PathBuf;

fn main() {
    println!("cargo:rerun-if-env-changed=FLAX_EMBED_JAR");
    println!("cargo:rerun-if-changed=FlaxClient-Release.jar");
    println!("cargo:rerun-if-changed=../build/libs/FlaxClient-Release.jar");

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
}
