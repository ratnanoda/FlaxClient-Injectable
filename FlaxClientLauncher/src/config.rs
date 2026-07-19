use anyhow::{Context, Result};
use serde::{Deserialize, Serialize};
use std::fs;
use std::path::PathBuf;
use std::time::{SystemTime, UNIX_EPOCH};
use uuid::Uuid;

pub const APP_DIR_NAME: &str = ".flaxclient";
pub const DEFAULT_CLIENT_ID: &str = "00000000402b5328";
pub const FLAX_VERSION: &str = "1.8.9";
pub const RELEASE_LABEL: &str = "Releases 1.0";

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LauncherConfig {
    pub memory_mb: u32,
    #[serde(default = "default_use_bundled_java")]
    pub use_bundled_java: bool,
    pub java_path: String,
    pub offline_name: String,
    pub microsoft_client_id: String,
    pub account: Option<Account>,
    #[serde(default = "default_version")]
    pub version: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "kind")]
pub enum Account {
    Microsoft {
        username: String,
        uuid: String,
        access_token: String,
        refresh_token: String,
        expires_at: u64,
    },
}

#[derive(Debug, Clone)]
pub struct GameSession {
    pub username: String,
    pub uuid: String,
    pub access_token: String,
    pub user_type: String,
    pub user_properties: String,
}

impl Default for LauncherConfig {
    fn default() -> Self {
        let version = default_version();
        Self {
            memory_mb: default_memory_mb(&version),
            use_bundled_java: true,
            java_path: find_default_java_for_version(&version),
            offline_name: "Player".to_owned(),
            microsoft_client_id: DEFAULT_CLIENT_ID.to_owned(),
            account: None,
            version,
        }
    }
}

impl LauncherConfig {
    pub fn load() -> Self {
        let path = config_path();
        let mut config = match fs::read_to_string(&path) {
            Ok(text) => {
                let sanitized = text.trim_start_matches('\u{feff}');
                serde_json::from_str(sanitized).unwrap_or_default()
            }
            Err(_) => Self::default(),
        };
        config.version = normalize_version(&config.version);
        config.memory_mb = config.memory_mb.max(512);
        config.memory_mb = config.memory_mb.max(recommended_memory_mb(&config.version));
        if config.use_bundled_java || config.java_path.trim().is_empty() {
            config.java_path = find_default_java_for_version(&config.version);
        }
        config
    }

    pub fn bundled_java_label(&self) -> &'static str {
        "Bundled Java 8"
    }

    pub fn save(&self) -> Result<()> {
        fs::create_dir_all(app_dir()).context("failed to create launcher data directory")?;
        let text = serde_json::to_string_pretty(self).context("failed to serialize config")?;
        fs::write(config_path(), text).context("failed to write launcher config")
    }

    pub fn active_session(&self) -> GameSession {
        if let Some(Account::Microsoft {
            username,
            uuid,
            access_token,
            ..
        }) = &self.account
        {
            return GameSession {
                username: username.clone(),
                uuid: uuid.clone(),
                access_token: access_token.clone(),
                user_type: "msa".to_owned(),
                user_properties: "{}".to_owned(),
            };
        }

        let username = clean_player_name(&self.offline_name);
        GameSession {
            uuid: offline_uuid(&username),
            username,
            access_token: "0".to_owned(),
            user_type: "legacy".to_owned(),
            user_properties: "{}".to_owned(),
        }
    }
}

impl Account {
    pub fn username(&self) -> &str {
        match self {
            Self::Microsoft { username, .. } => username,
        }
    }

    pub fn expires_at(&self) -> u64 {
        match self {
            Self::Microsoft { expires_at, .. } => *expires_at,
        }
    }

    pub fn refresh_token(&self) -> &str {
        match self {
            Self::Microsoft { refresh_token, .. } => refresh_token,
        }
    }

    pub fn is_fresh(&self) -> bool {
        self.expires_at() > now_unix() + 300
    }
}

pub fn app_dir() -> PathBuf {
    dirs::data_dir()
        .unwrap_or_else(|| std::env::current_dir().unwrap_or_else(|_| PathBuf::from(".")))
        .join(APP_DIR_NAME)
}

pub fn minecraft_dir() -> PathBuf {
    #[cfg(windows)]
    {
        return dirs::data_dir()
            .unwrap_or_else(|| std::env::current_dir().unwrap_or_else(|_| PathBuf::from(".")))
            .join(".minecraft");
    }

    #[cfg(not(windows))]
    {
        return dirs::home_dir()
            .unwrap_or_else(|| std::env::current_dir().unwrap_or_else(|_| PathBuf::from(".")))
            .join(".minecraft");
    }
}

pub fn config_path() -> PathBuf {
    app_dir().join("launcher_config.json")
}

pub fn bundled_javaw_path_for_version(version: &str) -> PathBuf {
    let _ = version;
    bundled_java8_home()
        .join("bin")
        .join(preferred_java_binary_name())
}

pub fn bundled_java_path_for_version(version: &str) -> PathBuf {
    let _ = version;
    bundled_java8_home()
        .join("bin")
        .join(launch_java_binary_name())
}

fn bundled_java8_home() -> PathBuf {
    app_dir()
        .join("runtime")
        .join("jre-legacy")
        .join(java_runtime_manifest_key())
        .join("jre-legacy")
}

pub fn java_runtime_manifest_key() -> &'static str {
    #[cfg(all(target_os = "windows", target_pointer_width = "64"))]
    {
        return "windows-x64";
    }

    #[cfg(all(target_os = "windows", target_pointer_width = "32"))]
    {
        return "windows-x86";
    }

    #[cfg(all(target_os = "linux", target_pointer_width = "64"))]
    {
        return "linux";
    }

    #[cfg(all(target_os = "linux", target_pointer_width = "32"))]
    {
        return "linux-i386";
    }

    #[cfg(all(target_os = "macos", target_arch = "aarch64"))]
    {
        return "mac-os-arm64";
    }

    #[cfg(all(target_os = "macos", not(target_arch = "aarch64")))]
    {
        return "mac-os";
    }

    #[allow(unreachable_code)]
    "windows-x64"
}

pub fn java_binary_hint() -> &'static str {
    preferred_java_binary_name()
}

pub fn recommended_memory_mb(version: &str) -> u32 {
    let _ = version;
    2048
}

pub fn default_memory_mb(version: &str) -> u32 {
    recommended_memory_mb(version)
}

pub fn normalize_version(version: &str) -> String {
    let _ = version;
    FLAX_VERSION.to_owned()
}

pub fn now_unix() -> u64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|duration| duration.as_secs())
        .unwrap_or_default()
}

pub fn clean_player_name(name: &str) -> String {
    let cleaned: String = name
        .chars()
        .filter(|c| c.is_ascii_alphanumeric() || *c == '_')
        .take(16)
        .collect();

    if cleaned.is_empty() {
        "Player".to_owned()
    } else {
        cleaned
    }
}

fn offline_uuid(name: &str) -> String {
    let digest = md5::compute(format!("OfflinePlayer:{name}").as_bytes());
    let mut bytes = digest.0;
    bytes[6] = (bytes[6] & 0x0f) | 0x30;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;
    Uuid::from_bytes(bytes).simple().to_string()
}

pub fn find_default_java_for_version(version: &str) -> String {
    let mut candidates = Vec::new();

    candidates.push(bundled_javaw_path_for_version(version));
    candidates.push(bundled_java_path_for_version(version));

    if let Ok(java_home) = std::env::var("JAVA_HOME") {
        candidates.push(
            PathBuf::from(&java_home)
                .join("bin")
                .join(preferred_java_binary_name()),
        );
        candidates.push(
            PathBuf::from(java_home)
                .join("bin")
                .join(launch_java_binary_name()),
        );
    }

    candidates.push(
        minecraft_dir()
            .join("runtime")
            .join("jre-legacy")
            .join(java_runtime_manifest_key())
            .join("jre-legacy")
            .join("bin")
            .join(preferred_java_binary_name()),
    );
    candidates.push(
        minecraft_dir()
            .join("runtime")
            .join("jre-legacy")
            .join(java_runtime_manifest_key())
            .join("jre-legacy")
            .join("bin")
            .join(launch_java_binary_name()),
    );

    #[cfg(windows)]
    {
        if let Some(program_files) = std::env::var_os("ProgramFiles") {
            let java_dir = PathBuf::from(&program_files).join("Java");
            collect_java_candidates(&java_dir, &mut candidates, version);
        }

        if let Some(program_files_x86) = std::env::var_os("ProgramFiles(x86)") {
            let java_dir = PathBuf::from(program_files_x86).join("Java");
            collect_java_candidates(&java_dir, &mut candidates, version);
        }
    }

    #[cfg(not(windows))]
    {
        for root in ["/usr/lib/jvm", "/usr/java"] {
            collect_java_candidates(&PathBuf::from(root), &mut candidates, version);
        }

        candidates.push(PathBuf::from("/usr/bin/java"));
        candidates.push(PathBuf::from("/usr/local/bin/java"));
    }

    for candidate in candidates {
        if candidate.exists() {
            return candidate.to_string_lossy().to_string();
        }
    }

    "java".to_owned()
}

fn default_use_bundled_java() -> bool {
    true
}

fn default_version() -> String {
    FLAX_VERSION.to_owned()
}

fn collect_java_candidates(root: &PathBuf, candidates: &mut Vec<PathBuf>, version: &str) {
    let Ok(entries) = fs::read_dir(root) else {
        return;
    };

    for entry in entries.flatten() {
        let name = entry.file_name().to_string_lossy().to_lowercase();
        let _ = version;
        let matches = name.contains("1.8")
            || name.contains("8")
            || name.contains("jre1.8")
            || name.contains("java-8")
            || name == "jdk1.8.0";
        if matches {
            candidates.push(entry.path().join("bin").join(preferred_java_binary_name()));
            candidates.push(entry.path().join("bin").join(launch_java_binary_name()));

            #[cfg(target_os = "macos")]
            {
                candidates.push(
                    entry
                        .path()
                        .join("Contents")
                        .join("Home")
                        .join("bin")
                        .join(preferred_java_binary_name()),
                );
                candidates.push(
                    entry
                        .path()
                        .join("Contents")
                        .join("Home")
                        .join("bin")
                        .join(launch_java_binary_name()),
                );
            }
        }
    }
}

fn preferred_java_binary_name() -> &'static str {
    if cfg!(windows) {
        "javaw.exe"
    } else {
        "java"
    }
}

fn launch_java_binary_name() -> &'static str {
    if cfg!(windows) {
        "java.exe"
    } else {
        "java"
    }
}
