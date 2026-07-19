use crate::auth;
use crate::config::{
    Account, FLAX_VERSION, GameSession, LauncherConfig, app_dir, bundled_java_path_for_version,
    java_runtime_manifest_key, minecraft_dir,
};
use crate::events::WorkerEvent;
use crate::minecraft::{
    Artifact, AssetIndexJson, JavaRuntimeFileManifest, Library, ResolvedVersion, VersionJson,
    VersionManifest, library_allowed, resolve_argument_entries,
};
use anyhow::{Context, Result, anyhow, bail};
use reqwest::blocking::Client;
use sha1::{Digest, Sha1};
use std::collections::HashSet;
use std::fs::{self, File};
use std::io::{self, Cursor, Read, Write};
use std::path::{Path, PathBuf};
use std::process::{Command, Stdio};
use std::sync::mpsc::Sender;
use std::thread;
use std::time::Duration;
use zip::ZipArchive;

#[cfg(windows)]
use std::os::windows::process::CommandExt;

const FLAX_VERSION_JSON_1_8_9: &str = include_str!("../FlaxClient.json");
const EMBEDDED_FLAX_CLIENT_JAR: &[u8] =
    include_bytes!(concat!(env!("OUT_DIR"), "/FlaxClient-Release.jar"));
const EMBEDDED_YT_DLP: &[u8] = include_bytes!(concat!(env!("OUT_DIR"), "/yt-dlp-embedded"));
const EMBEDDED_FFMPEG_ARCHIVE: &[u8] =
    include_bytes!(concat!(env!("OUT_DIR"), "/ffmpeg-embedded.zip"));
const EMBEDDED_FLAX_CLIENT_COORDINATE: &str = "me.eldodebug:FlaxClient:Release";
const VERSION_MANIFEST_URL: &str =
    "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";
const LIBRARIES_BASE_URL: &str = "https://libraries.minecraft.net/";
const ASSET_BASE_URL: &str = "https://resources.download.minecraft.net/";
const JAVA_RUNTIME_ALL_URL: &str = "https://launchermeta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json";
#[cfg(windows)]
const CREATE_NO_WINDOW: u32 = 0x08000000;

struct FeatureRuntime {
    music_dir: PathBuf,
    yt_dlp: PathBuf,
    ffmpeg: PathBuf,
}

pub fn prepare_and_launch(
    mut config: LauncherConfig,
    tx: Sender<WorkerEvent>,
) -> Result<Option<Account>> {
    fs::create_dir_all(app_dir()).context("failed to create .flaxclient")?;
    ensure_resourcepacks_link(&tx)?;

    let updated_account = if let Some(account) = &config.account {
        let refreshed = auth::refresh_account(&config.microsoft_client_id, account, &tx)?;
        if refreshed.expires_at() != account.expires_at() {
            config.account = Some(refreshed.clone());
            let _ = tx.send(WorkerEvent::AccountUpdated(refreshed.clone()));
            Some(refreshed)
        } else {
            None
        }
    } else {
        None
    };

    let client = Client::builder()
        .user_agent("FlaxClientLauncher/1.0")
        .build()
        .context("failed to build HTTP client")?;

    let resolved = prepare_distribution(&client, &config, &tx)?;
    if config.use_bundled_java {
        let java = ensure_bundled_java(
            &client,
            &config.version,
            resolved.java_component.as_deref(),
            &tx,
        )?;
        config.java_path = java.to_string_lossy().to_string();
    } else if config.java_path.trim().is_empty() {
        config.java_path = crate::config::find_default_java_for_version(&config.version);
    }

    let feature_runtime = ensure_feature_runtime(&tx)?;

    let session = config.active_session();
    let pid = launch_game(&resolved, &session, &config, &feature_runtime, &tx)?;
    let _ = tx.send(WorkerEvent::LaunchStarted(pid));

    Ok(updated_account)
}

pub fn prepare_only(mut config: LauncherConfig, tx: Sender<WorkerEvent>) -> Result<()> {
    fs::create_dir_all(app_dir()).context("failed to create .flaxclient")?;
    ensure_resourcepacks_link(&tx)?;
    let client = Client::builder()
        .user_agent("FlaxClientLauncher/1.0")
        .build()
        .context("failed to build HTTP client")?;

    let resolved = prepare_distribution(&client, &config, &tx)?;
    if config.use_bundled_java {
        let java = ensure_bundled_java(
            &client,
            &config.version,
            resolved.java_component.as_deref(),
            &tx,
        )?;
        config.java_path = java.to_string_lossy().to_string();
    }
    ensure_feature_runtime(&tx)?;
    let _ = tx.send(WorkerEvent::Finished("Prepare complete.".to_owned()));
    Ok(())
}

fn prepare_distribution(
    client: &Client,
    _config: &LauncherConfig,
    tx: &Sender<WorkerEvent>,
) -> Result<ResolvedVersion> {
    let version_id = "FlaxClient";
    let effective_json = apply_version_json_overrides(FLAX_VERSION_JSON_1_8_9)?;

    let versions_dir = app_dir().join("versions");
    let version_dir = versions_dir.join(version_id);
    fs::create_dir_all(&version_dir)?;
    fs::write(
        version_dir.join(format!("{}.json", version_id)),
        &effective_json,
    )
    .context("failed to write FlaxClient version json")?;

    ensure_embedded_flax_client_library(tx)?;

    let flax_json: VersionJson =
        serde_json::from_str(&effective_json).context("failed to parse FlaxClient.json")?;
    let parent_id = flax_json
        .inherits_from
        .clone()
        .unwrap_or_else(|| FLAX_VERSION.to_owned());
    let parent_json = load_or_download_version_json(client, &parent_id, tx)?;
    let resolved = ResolvedVersion::from_parent_and_child(parent_json, flax_json)?;

    ensure_client_jar(client, &resolved, tx)?;
    ensure_libraries(client, &resolved, tx)?;
    ensure_assets(client, &resolved, tx)?;

    Ok(resolved)
}

fn apply_version_json_overrides(json_str: &str) -> Result<String> {
    let value: serde_json::Value =
        serde_json::from_str(json_str).context("failed to parse embedded FlaxClient JSON")?;

    serde_json::to_string_pretty(&value).context("failed to serialize FlaxClient JSON")
}

fn load_or_download_version_json(
    client: &Client,
    version_id: &str,
    tx: &Sender<WorkerEvent>,
) -> Result<VersionJson> {
    let version_dir = app_dir().join("versions").join(version_id);
    let json_path = version_dir.join(format!("{version_id}.json"));
    if json_path.exists() {
        let text = fs::read_to_string(&json_path)
            .with_context(|| format!("failed to read {}", json_path.display()))?;
        return serde_json::from_str(&text)
            .with_context(|| format!("failed to parse {}", json_path.display()));
    }

    let _ = tx.send(WorkerEvent::Log(format!(
        "Downloading metadata for {version_id}..."
    )));

    let manifest = client
        .get(VERSION_MANIFEST_URL)
        .send()
        .context("failed to download Minecraft version manifest")?
        .error_for_status()
        .context("Minecraft version manifest request failed")?
        .json::<VersionManifest>()
        .context("failed to parse Minecraft version manifest")?;

    let entry = manifest
        .versions
        .into_iter()
        .find(|entry| entry.id == version_id)
        .ok_or_else(|| anyhow!("Minecraft version {version_id} was not found"))?;

    fs::create_dir_all(&version_dir)?;
    let text = client
        .get(&entry.url)
        .send()
        .with_context(|| format!("failed to download {version_id} metadata"))?
        .error_for_status()
        .with_context(|| format!("metadata request failed for {version_id}"))?
        .text()
        .context("failed to read version metadata")?;

    if let Some(expected) = entry.sha1 {
        verify_text_sha1(&text, &expected)
            .with_context(|| format!("sha1 check failed for {version_id}.json"))?;
    }

    fs::write(&json_path, &text)
        .with_context(|| format!("failed to save {}", json_path.display()))?;
    serde_json::from_str(&text).context("failed to parse downloaded version json")
}

fn ensure_client_jar(
    client: &Client,
    resolved: &ResolvedVersion,
    tx: &Sender<WorkerEvent>,
) -> Result<()> {
    let jar_path = app_dir()
        .join("versions")
        .join(&resolved.jar_id)
        .join(format!("{}.jar", resolved.jar_id));
    let artifact = resolved.client_download.clone();
    let url = artifact
        .url
        .as_deref()
        .ok_or_else(|| anyhow!("client jar url was missing"))?;
    download_if_needed(
        client,
        url,
        &jar_path,
        artifact.sha1.as_deref(),
        tx,
        "Minecraft client jar",
    )
}

fn ensure_libraries(
    client: &Client,
    resolved: &ResolvedVersion,
    tx: &Sender<WorkerEvent>,
) -> Result<()> {
    let mut seen = HashSet::new();
    let libraries: Vec<&Library> = resolved
        .libraries
        .iter()
        .filter(|library| library_allowed(library))
        .collect();
    let total = libraries.len() as u64;

    for (index, library) in libraries.into_iter().enumerate() {
        let _ = tx.send(WorkerEvent::Progress {
            label: format!("Library: {}", library.name),
            current: index as u64 + 1,
            total,
        });

        if let Some(artifact) = normal_artifact(library)? {
            let path = library_path(&artifact, library)?;
            if seen.insert(path.clone()) {
                let url = artifact_url(&artifact, library)?;
                download_if_needed(
                    client,
                    &url,
                    &app_dir().join("libraries").join(&path),
                    artifact.sha1.as_deref(),
                    tx,
                    &library.name,
                )?;
            }
        }

        if let Some((classifier, artifact)) = native_artifact(library)? {
            let path = library_path(&artifact, library)?;
            let url = artifact_url(&artifact, library)?;
            let dest = app_dir().join("libraries").join(&path);
            download_if_needed(
                client,
                &url,
                &dest,
                artifact.sha1.as_deref(),
                tx,
                &format!("{} ({classifier})", library.name),
            )?;
            extract_natives(&dest, &resolved.jar_id, library, tx)?;
        }
    }

    Ok(())
}

fn ensure_assets(
    client: &Client,
    resolved: &ResolvedVersion,
    tx: &Sender<WorkerEvent>,
) -> Result<()> {
    let assets_dir = app_dir().join("assets");
    let indexes_dir = assets_dir.join("indexes");
    fs::create_dir_all(&indexes_dir)?;

    let index_path = indexes_dir.join(format!("{}.json", resolved.asset_index.id));
    download_if_needed(
        client,
        &resolved.asset_index.url,
        &index_path,
        resolved.asset_index.sha1.as_deref(),
        tx,
        "Asset index",
    )?;

    let text = fs::read_to_string(&index_path).context("failed to read asset index")?;
    let index: AssetIndexJson =
        serde_json::from_str(&text).context("failed to parse asset index")?;
    let total = index.objects.len() as u64;

    for (done, (name, object)) in index.objects.iter().enumerate() {
        if done % 25 == 0 || done + 1 == index.objects.len() {
            let _ = tx.send(WorkerEvent::Progress {
                label: "Syncing assets".to_owned(),
                current: done as u64 + 1,
                total,
            });
        }

        let prefix = object
            .hash
            .get(0..2)
            .ok_or_else(|| anyhow!("invalid asset hash"))?;
        let object_path = assets_dir.join("objects").join(prefix).join(&object.hash);
        let url = format!("{ASSET_BASE_URL}{prefix}/{}", object.hash);
        download_if_needed(
            client,
            &url,
            &object_path,
            Some(&object.hash),
            tx,
            "Asset object",
        )?;

        if index.virtual_assets {
            copy_asset_to_named_path(
                &object_path,
                &assets_dir.join("virtual").join("legacy"),
                name,
            )?;
        }

        if index.map_to_resources {
            copy_asset_to_named_path(&object_path, &app_dir().join("resources"), name)?;
        }
    }

    Ok(())
}

fn ensure_bundled_java(
    client: &Client,
    version: &str,
    java_component: Option<&str>,
    tx: &Sender<WorkerEvent>,
) -> Result<PathBuf> {
    let java = bundled_java_path_for_version(version);
    if java.exists() {
        ensure_executable(&java)?;
        return Ok(java);
    }

    let _ = version;
    let component = java_component.unwrap_or("jre-legacy");
    let runtime_name = component;
    let runtime_key = java_runtime_manifest_key();
    let progress_label = format!("Installing bundled {component}");

    let _ = tx.send(WorkerEvent::Log(format!(
        "Downloading Mojang Java runtime ({component} / {runtime_key})..."
    )));
    let all = client
        .get(JAVA_RUNTIME_ALL_URL)
        .send()
        .context("failed to download Java runtime index")?
        .error_for_status()
        .context("Java runtime index request failed")?
        .json::<serde_json::Value>()
        .context("failed to parse Java runtime index")?;

    let manifest = all
        .get(runtime_key)
        .and_then(|entry| entry.get(component))
        .and_then(|entry| entry.get(0))
        .and_then(|entry| entry.get("manifest"))
        .ok_or_else(|| anyhow!("Java runtime manifest was missing for {runtime_key}/{component}"))?;
    let manifest_url = manifest["url"]
        .as_str()
        .ok_or_else(|| anyhow!("Java runtime manifest URL was missing for {component}"))?;
    let manifest_sha1 = manifest["sha1"].as_str();
    let manifest_text = client
        .get(manifest_url)
        .send()
        .context("failed to download Java runtime manifest")?
        .error_for_status()
        .context("Java runtime manifest request failed")?
        .text()
        .context("failed to read Java runtime manifest")?;
    if let Some(expected) = manifest_sha1 {
        verify_text_sha1(&manifest_text, expected).context("Java runtime manifest sha1 check failed")?;
    }

    let manifest: JavaRuntimeFileManifest =
        serde_json::from_str(&manifest_text).context("failed to parse Java runtime manifest")?;
    let root = app_dir()
        .join("runtime")
        .join(runtime_name)
        .join(runtime_key)
        .join(runtime_name);
    let total = manifest.files.len() as u64;

    for (index, (relative, file)) in manifest.files.iter().enumerate() {
        let dest = root.join(relative);
        match file.kind.as_str() {
            "directory" => {
                fs::create_dir_all(&dest)?;
            }
            "file" => {
                let artifact = file
                    .downloads
                    .as_ref()
                    .and_then(|downloads| downloads.raw.as_ref())
                    .ok_or_else(|| anyhow!("Java runtime file has no raw download: {relative}"))?;
                if index % 8 == 0 || index + 1 == manifest.files.len() {
                    let _ = tx.send(WorkerEvent::Progress {
                        label: progress_label.clone(),
                        current: index as u64 + 1,
                        total,
                    });
                }
                download_if_needed(
                    client,
                    &artifact.url,
                    &dest,
                    artifact.sha1.as_deref(),
                    tx,
                    "Java runtime file",
                )?;
                if file.executable {
                    ensure_executable(&dest)?;
                }
            }
            other => {
                let _ = tx.send(WorkerEvent::Log(format!(
                    "Skipping Java runtime entry {relative} ({other})."
                )));
            }
        }
    }

    if !java.exists() {
        bail!(
            "bundled Java was downloaded, but {} was not found",
            java.display()
        );
    }

    ensure_executable(&java)?;
    Ok(java)
}

fn copy_asset_to_named_path(source: &Path, root: &Path, name: &str) -> Result<()> {
    let dest = root.join(name);
    if dest.exists() {
        return Ok(());
    }

    if let Some(parent) = dest.parent() {
        fs::create_dir_all(parent)?;
    }
    fs::copy(source, dest)?;
    Ok(())
}

fn ensure_feature_runtime(tx: &Sender<WorkerEvent>) -> Result<FeatureRuntime> {
    let music_dir = std::env::var_os("FLAX_MUSIC_DIR")
        .map(PathBuf::from)
        .unwrap_or_else(|| app_dir().join("Musics"));
    fs::create_dir_all(&music_dir)
        .with_context(|| format!("failed to create {}", music_dir.display()))?;
    fs::create_dir_all(app_dir().join("glide/cache/custom-cape"))?;

    let tools_dir = app_dir().join("tools");
    fs::create_dir_all(&tools_dir)?;

    let yt_dlp = if let Some(path) = std::env::var_os("FLAX_YTDLP") {
        PathBuf::from(path)
    } else {
        let path = tools_dir.join(if cfg!(windows) { "yt-dlp.exe" } else { "yt-dlp" });
        ensure_embedded_tool(EMBEDDED_YT_DLP, &path, "yt-dlp", "--version", tx)?;
        path
    };

    let ffmpeg = if let Some(path) = std::env::var_os("FLAX_FFMPEG") {
        PathBuf::from(path)
    } else {
        let path = tools_dir.join(if cfg!(windows) { "ffmpeg.exe" } else { "ffmpeg" });
        ensure_ffmpeg(&path, tx)?;
        path
    };

    let _ = tx.send(WorkerEvent::Log(format!(
        "Music, capes and YouTube tools are ready in {}.",
        app_dir().display()
    )));
    Ok(FeatureRuntime { music_dir, yt_dlp, ffmpeg })
}

fn ensure_embedded_tool(
    bytes: &[u8],
    path: &Path,
    label: &str,
    version_argument: &str,
    tx: &Sender<WorkerEvent>,
) -> Result<()> {
    let embedded_hash = sha1_bytes(bytes);
    let needs_install = !path.exists()
        || sha1_file(path)
            .map(|hash| hash != embedded_hash)
            .unwrap_or(true);
    if needs_install {
        if let Some(parent) = path.parent() {
            fs::create_dir_all(parent)?;
        }
        fs::write(path, bytes).with_context(|| format!("failed to install {label}"))?;
        let _ = tx.send(WorkerEvent::Log(format!("Installed embedded {label}.")));
    }
    ensure_executable(path)?;
    if !tool_runs(path, version_argument) {
        bail!("downloaded {label} could not be started: {}", path.display());
    }
    Ok(())
}

fn ensure_ffmpeg(ffmpeg_path: &Path, tx: &Sender<WorkerEvent>) -> Result<()> {
    if !ffmpeg_path.exists() || !tool_runs(ffmpeg_path, "-version") {
        if ffmpeg_path.exists() {
            fs::remove_file(ffmpeg_path)?;
        }
        let mut archive = ZipArchive::new(Cursor::new(EMBEDDED_FFMPEG_ARCHIVE))
            .context("failed to read embedded ffmpeg archive")?;
        let expected_name = if cfg!(windows) { "ffmpeg.exe" } else { "ffmpeg" };
        let mut extracted = false;
        for index in 0..archive.len() {
            let mut entry = archive.by_index(index)?;
            if entry.is_dir() || Path::new(entry.name()).file_name().and_then(|name| name.to_str()) != Some(expected_name) {
                continue;
            }
            let mut output = File::create(ffmpeg_path)
                .with_context(|| format!("failed to create {}", ffmpeg_path.display()))?;
            io::copy(&mut entry, &mut output)?;
            output.flush()?;
            extracted = true;
            break;
        }
        if !extracted {
            bail!("ffmpeg was not found inside the embedded archive");
        }
        let _ = tx.send(WorkerEvent::Log("Installed embedded ffmpeg.".to_owned()));
    }

    ensure_executable(ffmpeg_path)?;
    if !tool_runs(ffmpeg_path, "-version") {
        bail!("downloaded ffmpeg could not be started: {}", ffmpeg_path.display());
    }
    Ok(())
}

fn tool_runs(path: &Path, version_argument: &str) -> bool {
    let mut command = Command::new(path);
    command
        .arg(version_argument)
        .stdout(Stdio::null())
        .stderr(Stdio::null());
    #[cfg(windows)]
    command.creation_flags(CREATE_NO_WINDOW);
    matches!(command.status(), Ok(status) if status.success())
}

fn launch_game(
    resolved: &ResolvedVersion,
    session: &GameSession,
    config: &LauncherConfig,
    feature_runtime: &FeatureRuntime,
    tx: &Sender<WorkerEvent>,
) -> Result<u32> {
    let game_dir = app_dir();
    let assets_dir = game_dir.join("assets");
    let natives_dir = game_dir
        .join("versions")
        .join(&resolved.jar_id)
        .join("natives");
    let logs_dir = game_dir.join("logs");
    fs::create_dir_all(&logs_dir)?;
    let launch_log = logs_dir.join("latest-launch.log");

    let classpath = build_classpath(resolved)?;
    let mut args = Vec::new();

    if resolved.uses_modern_arguments() {
        let jvm_args = resolve_argument_entries(&resolved.jvm_arguments)
            .into_iter()
            .map(|arg| {
                substitute_launch_tokens(
                    &arg,
                    resolved,
                    session,
                    &game_dir,
                    &assets_dir,
                    &natives_dir,
                    &classpath,
                )
            })
            .collect::<Vec<_>>();
        args.extend(normalize_modern_jvm_args(jvm_args));
    } else {
        args.push(format!(
            "-Djava.library.path={}",
            natives_dir.to_string_lossy()
        ));
        args.push("-Dminecraft.launcher.brand=FlaxLauncher".to_owned());
        args.push("-Dminecraft.launcher.version=0.2.0".to_owned());
        args.push("-cp".to_owned());
        args.push(classpath.clone());
    }

    args.push(format!("-Xms{}M", config.memory_mb.min(1024)));
    args.push(format!("-Xmx{}M", config.memory_mb));
    if !args
        .iter()
        .any(|arg| arg.starts_with("-Djava.library.path"))
    {
        args.push(format!(
            "-Djava.library.path={}",
            natives_dir.to_string_lossy()
        ));
    }
    let game_args = sanitize_game_arguments(collect_game_arguments(
        resolved,
        session,
        &game_dir,
        &assets_dir,
        &natives_dir,
        &classpath,
    ));
    if !args.iter().any(|arg| arg == "-cp") {
        args.push("-cp".to_owned());
        args.push(classpath);
    }
    args.push(resolved.main_class.clone());
    args.extend(game_args);

    let java = config.java_path.trim();
    if java.is_empty() {
        bail!("Java path is empty");
    }

    let stdout = File::create(&launch_log)
        .with_context(|| format!("failed to create {}", launch_log.display()))?;
    let stderr = stdout
        .try_clone()
        .context("failed to clone launch log handle")?;

    let _ = tx.send(WorkerEvent::Log(format!(
        "Starting FlaxClient with {} MB memory.",
        config.memory_mb
    )));
    let _ = tx.send(WorkerEvent::Log(format!(
        "Java: {}",
        Path::new(java).display()
    )));

    let mut command = Command::new(java);
    command
        .args(args)
        .current_dir(&game_dir)
        .env("FLAX_MUSIC_DIR", &feature_runtime.music_dir)
        .env("FLAX_YTDLP", &feature_runtime.yt_dlp)
        .env("FLAX_FFMPEG", &feature_runtime.ffmpeg)
        .stdout(Stdio::from(stdout))
        .stderr(Stdio::from(stderr));

    #[cfg(windows)]
    command.creation_flags(CREATE_NO_WINDOW);

    let mut child = command
        .spawn()
        .with_context(|| format!("failed to start Java: {java}"))?;

    thread::sleep(Duration::from_secs(3));
    if let Some(status) = child
        .try_wait()
        .context("failed to inspect Minecraft process")?
    {
        let tail = read_log_tail(&launch_log, 5000).unwrap_or_default();
        bail!(
            "Minecraft closed immediately with status {status}. Log: {}\n{}",
            launch_log.display(),
            tail
        );
    }

    Ok(child.id())
}

fn build_classpath(resolved: &ResolvedVersion) -> Result<String> {
    let mut paths = Vec::new();
    let mut seen = HashSet::new();

    for library in resolved
        .libraries
        .iter()
        .filter(|library| library_allowed(library))
    {
        if let Some(artifact) = normal_artifact(library)? {
            let path = app_dir()
                .join("libraries")
                .join(library_path(&artifact, library)?);
            if seen.insert(path.clone()) {
                paths.push(path);
            }
        }
    }

    paths.push(
        app_dir()
            .join("versions")
            .join(&resolved.jar_id)
            .join(format!("{}.jar", resolved.jar_id)),
    );

    let separator = if cfg!(windows) { ";" } else { ":" };
    Ok(paths
        .iter()
        .map(|path| path.to_string_lossy().to_string())
        .collect::<Vec<_>>()
        .join(separator))
}

fn collect_game_arguments(
    resolved: &ResolvedVersion,
    session: &GameSession,
    game_dir: &Path,
    assets_dir: &Path,
    natives_dir: &Path,
    classpath: &str,
) -> Vec<String> {
    if resolved.uses_modern_arguments() {
        return resolve_argument_entries(&resolved.game_arguments)
            .into_iter()
            .map(|arg| {
                substitute_launch_tokens(
                    &arg,
                    resolved,
                    session,
                    game_dir,
                    assets_dir,
                    natives_dir,
                    classpath,
                )
            })
            .collect();
    }

    resolved
        .minecraft_arguments
        .as_deref()
        .unwrap_or_default()
        .split_whitespace()
        .map(|token| {
            substitute_launch_tokens(
                token,
                resolved,
                session,
                game_dir,
                assets_dir,
                natives_dir,
                classpath,
            )
        })
        .collect()
}

fn normalize_modern_jvm_args(args: Vec<String>) -> Vec<String> {
    let mut normalized = Vec::new();
    let mut index = 0;
    while index < args.len() {
        let arg = args[index].as_str();
        if arg == "-cp" {
            index += 2;
            continue;
        }
        if arg.contains("${classpath}") {
            index += 1;
            continue;
        }
        normalized.push(args[index].clone());
        index += 1;
    }
    normalized
}

fn sanitize_game_arguments(args: Vec<String>) -> Vec<String> {
    let mut sanitized = Vec::new();
    let mut index = 0;
    while index < args.len() {
        let flag = &args[index];
        if flag.starts_with("--quickPlay") {
            index += 2;
            continue;
        }
        if flag.starts_with("--") {
            let value = args.get(index + 1);
            if value.is_some_and(|value| value.is_empty()) {
                index += 2;
                continue;
            }
            if let Some(value) = value {
                sanitized.push(flag.clone());
                sanitized.push(value.clone());
                index += 2;
                continue;
            }
        }
        sanitized.push(flag.clone());
        index += 1;
    }
    sanitized
}

fn substitute_launch_tokens(
    token: &str,
    resolved: &ResolvedVersion,
    session: &GameSession,
    game_dir: &Path,
    assets_dir: &Path,
    natives_dir: &Path,
    classpath: &str,
) -> String {
    token
        .replace("${auth_player_name}", &session.username)
        .replace("${version_name}", &resolved.id)
        .replace("${game_directory}", &game_dir.to_string_lossy())
        .replace("${assets_root}", &assets_dir.to_string_lossy())
        .replace("${asset_index_name}", &resolved.assets_id)
        .replace("${assets_index_name}", &resolved.assets_id)
        .replace("${auth_uuid}", &session.uuid)
        .replace("${auth_access_token}", &session.access_token)
        .replace("${user_properties}", &session.user_properties)
        .replace("${user_type}", &session.user_type)
        .replace("${version_type}", "release")
        .replace("${clientid}", crate::config::DEFAULT_CLIENT_ID)
        .replace("${auth_xuid}", "")
        .replace("${natives_directory}", &natives_dir.to_string_lossy())
        .replace("${classpath}", classpath)
        .replace("${launcher_name}", "FlaxLauncher")
        .replace("${launcher_version}", "0.2.0")
        .replace("${quickPlayPath}", "")
        .replace("${quickPlaySingleplayer}", "")
        .replace("${quickPlayMultiplayer}", "")
        .replace("${quickPlayRealms}", "")
        .replace("${resolution_width}", "854")
        .replace("${resolution_height}", "480")
}

fn normal_artifact(library: &Library) -> Result<Option<Artifact>> {
    if let Some(downloads) = &library.downloads {
        if let Some(artifact) = &downloads.artifact {
            return Ok(Some(artifact.clone()));
        }
    }

    if library.natives.is_some() {
        return Ok(None);
    }

    Ok(Some(Artifact {
        path: Some(maven_path(&library.name, None)?),
        sha1: library.sha1.clone(),
        url: Some(format!(
            "{}{}",
            library.url.as_deref().unwrap_or(LIBRARIES_BASE_URL),
            maven_path(&library.name, None)?
        )),
    }))
}

fn native_artifact(library: &Library) -> Result<Option<(String, Artifact)>> {
    let Some(natives) = &library.natives else {
        return Ok(None);
    };

    let Some(classifier_template) = natives.get(current_native_os_key()) else {
        return Ok(None);
    };

    let classifier = classifier_template.replace(
        "${arch}",
        if cfg!(target_pointer_width = "64") {
            "64"
        } else {
            "32"
        },
    );

    if let Some(downloads) = &library.downloads {
        if let Some(classifiers) = &downloads.classifiers {
            if let Some(artifact) = classifiers.get(&classifier) {
                return Ok(Some((classifier, artifact.clone())));
            }
        }
    }

    let path = maven_path(&library.name, Some(&classifier))?;
    Ok(Some((
        classifier,
        Artifact {
            path: Some(path.clone()),
            sha1: None,
            url: Some(format!(
                "{}{}",
                library.url.as_deref().unwrap_or(LIBRARIES_BASE_URL),
                path
            )),
        },
    )))
}

fn current_native_os_key() -> &'static str {
    if cfg!(target_os = "windows") {
        "windows"
    } else if cfg!(target_os = "linux") {
        "linux"
    } else {
        "osx"
    }
}

fn library_path(artifact: &Artifact, library: &Library) -> Result<String> {
    if let Some(path) = &artifact.path {
        return Ok(path.replace('\\', "/"));
    }

    maven_path(&library.name, None)
}

fn artifact_url(artifact: &Artifact, library: &Library) -> Result<String> {
    if let Some(url) = &artifact.url {
        if !url.is_empty() {
            return Ok(url.clone());
        }
    }

    Ok(format!(
        "{}{}",
        library.url.as_deref().unwrap_or(LIBRARIES_BASE_URL),
        library_path(artifact, library)?
    ))
}

fn maven_path(name: &str, classifier: Option<&str>) -> Result<String> {
    let parts: Vec<&str> = name.split(':').collect();
    if parts.len() < 3 {
        bail!("invalid Maven coordinate: {name}");
    }

    let group = parts[0].replace('.', "/");
    let artifact = parts[1];
    let version = parts[2];
    let classifier = classifier
        .or_else(|| parts.get(3).copied())
        .map(|value| format!("-{value}"))
        .unwrap_or_default();

    Ok(format!(
        "{group}/{artifact}/{version}/{artifact}-{version}{classifier}.jar"
    ))
}

fn ensure_embedded_flax_client_library(tx: &Sender<WorkerEvent>) -> Result<()> {
    let target = app_dir()
        .join("libraries")
        .join(maven_path(EMBEDDED_FLAX_CLIENT_COORDINATE, None)?);

    if let Some(parent) = target.parent() {
        fs::create_dir_all(parent)?;
    }

    let embedded_hash = sha1_bytes(EMBEDDED_FLAX_CLIENT_JAR);
    let should_write = if target.exists() {
        sha1_file(&target)
            .map(|existing_hash| existing_hash != embedded_hash)
            .unwrap_or(true)
    } else {
        true
    };

    if should_write {
        fs::write(&target, EMBEDDED_FLAX_CLIENT_JAR)
            .with_context(|| format!("failed to write {}", target.display()))?;
        let _ = tx.send(WorkerEvent::Log(format!(
            "Installed bundled FlaxClient 1.8.9 jar to {}",
            target.display()
        )));
    } else {
        let _ = tx.send(WorkerEvent::Log(format!(
            "Using bundled FlaxClient 1.8.9 jar: {}",
            target.display()
        )));
    }

    Ok(())
}

fn download_if_needed(
    client: &Client,
    url: &str,
    dest: &Path,
    sha1: Option<&str>,
    tx: &Sender<WorkerEvent>,
    label: &str,
) -> Result<()> {
    if dest.exists() {
        if let Some(expected) = sha1 {
            if sha1_file(dest)
                .map(|actual| actual == expected)
                .unwrap_or(false)
            {
                return Ok(());
            }
        } else {
            return Ok(());
        }
    }

    if let Some(parent) = dest.parent() {
        fs::create_dir_all(parent)?;
    }

    let _ = tx.send(WorkerEvent::Log(format!("Downloading {label}...")));
    let mut response = client
        .get(url)
        .send()
        .with_context(|| format!("failed to download {url}"))?
        .error_for_status()
        .with_context(|| format!("download request failed: {url}"))?;

    let temp = dest.with_extension("download");
    {
        let mut file =
            File::create(&temp).with_context(|| format!("failed to create {}", temp.display()))?;
        io::copy(&mut response, &mut file).context("failed to write downloaded file")?;
        file.flush()?;
    }

    if let Some(expected) = sha1 {
        let actual = sha1_file(&temp)?;
        if actual != expected {
            let _ = fs::remove_file(&temp);
            bail!("sha1 mismatch for {label}: expected {expected}, got {actual}");
        }
    }

    if dest.exists() {
        fs::remove_file(dest).with_context(|| format!("failed to replace {}", dest.display()))?;
    }
    fs::rename(&temp, dest).with_context(|| format!("failed to move {}", dest.display()))?;
    Ok(())
}

fn sha1_file(path: &Path) -> Result<String> {
    let mut file =
        File::open(path).with_context(|| format!("failed to open {}", path.display()))?;
    let mut hasher = Sha1::new();
    let mut buffer = [0_u8; 8192];

    loop {
        let read = file.read(&mut buffer)?;
        if read == 0 {
            break;
        }
        hasher.update(&buffer[..read]);
    }

    Ok(hex::encode(hasher.finalize()))
}

fn sha1_bytes(bytes: &[u8]) -> String {
    let mut hasher = Sha1::new();
    hasher.update(bytes);
    hex::encode(hasher.finalize())
}

fn verify_text_sha1(text: &str, expected: &str) -> Result<()> {
    let mut hasher = Sha1::new();
    hasher.update(text.as_bytes());
    let actual = hex::encode(hasher.finalize());
    if actual == expected {
        Ok(())
    } else {
        bail!("expected {expected}, got {actual}")
    }
}

fn read_log_tail(path: &Path, max_chars: usize) -> Result<String> {
    let text =
        fs::read_to_string(path).with_context(|| format!("failed to read {}", path.display()))?;
    if text.len() <= max_chars {
        return Ok(text);
    }

    let start = text
        .char_indices()
        .rev()
        .nth(max_chars)
        .map(|(index, _)| index)
        .unwrap_or(0);
    Ok(format!("...{}", &text[start..]))
}

fn extract_natives(
    jar_path: &Path,
    jar_id: &str,
    library: &Library,
    tx: &Sender<WorkerEvent>,
) -> Result<()> {
    let natives_dir = app_dir().join("versions").join(jar_id).join("natives");
    fs::create_dir_all(&natives_dir)?;

    let file = File::open(jar_path)
        .with_context(|| format!("failed to open native jar {}", jar_path.display()))?;
    let mut archive = ZipArchive::new(file).context("failed to read native jar")?;
    let excludes = library
        .extract
        .as_ref()
        .map(|extract| extract.exclude.clone())
        .unwrap_or_else(|| vec!["META-INF/".to_owned()]);

    for index in 0..archive.len() {
        let mut entry = archive.by_index(index)?;
        let name = entry.name().replace('\\', "/");
        if entry.is_dir() || excludes.iter().any(|exclude| name.starts_with(exclude)) {
            continue;
        }

        let Some(enclosed) = entry.enclosed_name().map(|path| path.to_owned()) else {
            continue;
        };
        let dest = natives_dir.join(enclosed);
        if let Some(parent) = dest.parent() {
            fs::create_dir_all(parent)?;
        }
        let mut out = File::create(&dest)?;
        io::copy(&mut entry, &mut out)?;
    }

    let _ = tx.send(WorkerEvent::Log(format!(
        "Extracted natives for {}.",
        library.name
    )));
    Ok(())
}

fn ensure_resourcepacks_link(tx: &Sender<WorkerEvent>) -> Result<()> {
    let game_dir = app_dir();
    fs::create_dir_all(&game_dir)?;

    let target = minecraft_dir().join("resourcepacks");
    fs::create_dir_all(&target)
        .with_context(|| format!("failed to create {}", target.display()))?;

    let link = game_dir.join("resourcepacks");
    if fs::symlink_metadata(&link).is_ok() {
        let _ = tx.send(WorkerEvent::Log(format!(
            "Resource packs link: {}",
            link.display()
        )));
        return Ok(());
    }

    #[cfg(windows)]
    {
        if std::os::windows::fs::symlink_dir(&target, &link).is_ok() {
            let _ = tx.send(WorkerEvent::Log(
                "Created resource pack symlink.".to_owned(),
            ));
            return Ok(());
        }

        let status = Command::new("cmd")
            .arg("/C")
            .arg("mklink")
            .arg("/J")
            .arg(&link)
            .arg(&target)
            .status();

        if matches!(status, Ok(status) if status.success()) {
            let _ = tx.send(WorkerEvent::Log(
                "Created resource pack junction.".to_owned(),
            ));
            return Ok(());
        }
    }

    #[cfg(not(windows))]
    {
        if std::os::unix::fs::symlink(&target, &link).is_ok() {
            return Ok(());
        }
    }

    fs::create_dir_all(&link)?;
    let _ = tx.send(WorkerEvent::Log(format!(
        "Could not create a resource pack link; using {}.",
        link.display()
    )));
    Ok(())
}

#[cfg(unix)]
fn ensure_executable(path: &Path) -> Result<()> {
    use std::os::unix::fs::PermissionsExt;

    let metadata =
        fs::metadata(path).with_context(|| format!("failed to inspect {}", path.display()))?;
    let mut permissions = metadata.permissions();
    let mode = permissions.mode();
    let executable_mode = mode | 0o755;
    if executable_mode != mode {
        permissions.set_mode(executable_mode);
        fs::set_permissions(path, permissions)
            .with_context(|| format!("failed to set executable bit on {}", path.display()))?;
    }
    Ok(())
}

#[cfg(not(unix))]
fn ensure_executable(_path: &Path) -> Result<()> {
    Ok(())
}
