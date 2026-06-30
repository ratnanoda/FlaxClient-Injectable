use serde::Deserialize;
use std::collections::HashMap;

#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct VersionManifest {
    pub versions: Vec<ManifestVersion>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct ManifestVersion {
    pub id: String,
    pub url: String,
    pub sha1: Option<String>,
}

#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct VersionJson {
    pub id: String,
    #[serde(default, rename = "inheritsFrom")]
    pub inherits_from: Option<String>,
    #[serde(default)]
    pub jar: Option<String>,
    #[serde(default)]
    pub assets: Option<String>,
    #[serde(default)]
    pub asset_index: Option<AssetIndexInfo>,
    #[serde(default)]
    pub downloads: Option<HashMap<String, Artifact>>,
    #[serde(default)]
    pub libraries: Vec<Library>,
    #[serde(default, rename = "mainClass")]
    pub main_class: Option<String>,
    #[serde(default, rename = "minecraftArguments")]
    pub minecraft_arguments: Option<String>,
    #[serde(default)]
    pub arguments: Option<LaunchArguments>,
    #[serde(default, rename = "javaVersion")]
    pub java_version: Option<JavaVersionInfo>,
}

#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct JavaVersionInfo {
    pub component: String,
    pub _major_version: u32,
}

#[derive(Debug, Clone, Deserialize)]
pub struct LaunchArguments {
    #[serde(default)]
    pub game: Vec<ArgumentEntry>,
    #[serde(default)]
    pub jvm: Vec<ArgumentEntry>,
}

#[derive(Debug, Clone, Deserialize)]
#[serde(untagged)]
pub enum ArgumentEntry {
    Plain(String),
    Conditional {
        rules: Vec<Rule>,
        value: ArgumentValues,
    },
}

#[derive(Debug, Clone, Deserialize)]
#[serde(untagged)]
pub enum ArgumentValues {
    Single(String),
    Many(Vec<String>),
}

impl ArgumentValues {
    pub fn into_strings(self) -> Vec<String> {
        match self {
            Self::Single(value) => vec![value],
            Self::Many(values) => values,
        }
    }
}

#[derive(Debug, Clone, Deserialize)]
pub struct AssetIndexInfo {
    pub id: String,
    pub url: String,
    #[serde(default)]
    pub sha1: Option<String>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct Library {
    pub name: String,
    #[serde(default)]
    pub url: Option<String>,
    #[serde(default)]
    pub sha1: Option<String>,
    #[serde(default)]
    pub downloads: Option<LibraryDownloads>,
    #[serde(default)]
    pub natives: Option<HashMap<String, String>>,
    #[serde(default)]
    pub rules: Option<Vec<Rule>>,
    #[serde(default)]
    pub extract: Option<ExtractRule>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct LibraryDownloads {
    #[serde(default)]
    pub artifact: Option<Artifact>,
    #[serde(default)]
    pub classifiers: Option<HashMap<String, Artifact>>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct Artifact {
    #[serde(default)]
    pub path: Option<String>,
    #[serde(default)]
    pub sha1: Option<String>,
    #[serde(default)]
    pub url: Option<String>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct Rule {
    pub action: String,
    #[serde(default)]
    pub os: Option<RuleOs>,
    #[serde(default)]
    pub features: Option<serde_json::Map<String, serde_json::Value>>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct RuleOs {
    #[serde(default)]
    pub name: Option<String>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct ExtractRule {
    #[serde(default)]
    pub exclude: Vec<String>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct AssetIndexJson {
    #[serde(default)]
    pub objects: HashMap<String, AssetObject>,
    #[serde(default, rename = "virtual")]
    pub virtual_assets: bool,
    #[serde(default)]
    pub map_to_resources: bool,
}

#[derive(Debug, Clone, Deserialize)]
pub struct AssetObject {
    pub hash: String,
    #[serde(default, rename = "size")]
    pub _size: Option<u64>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct JavaRuntimeFileManifest {
    #[serde(default)]
    pub files: HashMap<String, JavaRuntimeFile>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct JavaRuntimeFile {
    #[serde(rename = "type")]
    pub kind: String,
    #[serde(default)]
    pub executable: bool,
    #[serde(default)]
    pub downloads: Option<JavaRuntimeDownloads>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct JavaRuntimeDownloads {
    #[serde(default)]
    pub raw: Option<JavaRuntimeArtifact>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct JavaRuntimeArtifact {
    pub url: String,
    #[serde(default)]
    pub sha1: Option<String>,
}

#[derive(Debug, Clone)]
pub struct ResolvedVersion {
    pub id: String,
    pub jar_id: String,
    pub assets_id: String,
    pub asset_index: AssetIndexInfo,
    pub client_download: Artifact,
    pub libraries: Vec<Library>,
    pub main_class: String,
    pub minecraft_arguments: Option<String>,
    pub jvm_arguments: Vec<ArgumentEntry>,
    pub game_arguments: Vec<ArgumentEntry>,
    pub java_component: Option<String>,
}

impl ResolvedVersion {
    pub fn from_parent_and_child(parent: VersionJson, child: VersionJson) -> anyhow::Result<Self> {
        let mut libraries = parent.libraries;
        libraries.extend(child.libraries);

        let asset_index = child
            .asset_index
            .or(parent.asset_index)
            .ok_or_else(|| anyhow::anyhow!("asset index was missing"))?;

        let client_download = parent
            .downloads
            .as_ref()
            .and_then(|downloads| downloads.get("client"))
            .cloned()
            .ok_or_else(|| anyhow::anyhow!("client jar download was missing"))?;

        let assets_id = child
            .assets
            .or(parent.assets)
            .unwrap_or_else(|| asset_index.id.clone());

        let merged_arguments =
            merge_launch_arguments(parent.arguments.as_ref(), child.arguments.as_ref());
        let (jvm_arguments, game_arguments) = match merged_arguments {
            Some(args) => (args.jvm, args.game),
            None => (Vec::new(), Vec::new()),
        };

        let minecraft_arguments = child.minecraft_arguments.or(parent.minecraft_arguments);

        if minecraft_arguments.is_none() && jvm_arguments.is_empty() && game_arguments.is_empty() {
            anyhow::bail!("minecraft arguments were missing");
        }

        let java_component = child
            .java_version
            .or(parent.java_version)
            .map(|info| info.component);

        Ok(Self {
            id: child.id.clone(),
            jar_id: child.jar.unwrap_or(parent.id),
            assets_id,
            asset_index,
            client_download,
            libraries,
            main_class: child
                .main_class
                .or(parent.main_class)
                .ok_or_else(|| anyhow::anyhow!("main class was missing"))?,
            minecraft_arguments,
            jvm_arguments,
            game_arguments,
            java_component,
        })
    }

    pub fn uses_modern_arguments(&self) -> bool {
        !self.jvm_arguments.is_empty() || !self.game_arguments.is_empty()
    }
}

fn merge_launch_arguments(
    parent: Option<&LaunchArguments>,
    child: Option<&LaunchArguments>,
) -> Option<LaunchArguments> {
    match (parent, child) {
        (None, None) => None,
        (Some(parent), None) => Some(parent.clone()),
        (None, Some(child)) => Some(child.clone()),
        (Some(parent), Some(child)) => Some(LaunchArguments {
            jvm: parent.jvm.iter().chain(child.jvm.iter()).cloned().collect(),
            game: parent
                .game
                .iter()
                .chain(child.game.iter())
                .cloned()
                .collect(),
        }),
    }
}

pub fn resolve_argument_entries(entries: &[ArgumentEntry]) -> Vec<String> {
    let mut resolved = Vec::new();
    for entry in entries {
        match entry {
            ArgumentEntry::Plain(value) => resolved.push(value.clone()),
            ArgumentEntry::Conditional { rules, value } => {
                if rule_list_allows(rules) {
                    resolved.extend(value.clone().into_strings());
                }
            }
        }
    }
    resolved
}

pub fn library_allowed(library: &Library) -> bool {
    let Some(rules) = &library.rules else {
        return true;
    };

    let mut allowed = false;
    for rule in rules {
        if rule_matches(rule) {
            allowed = rule.action == "allow";
        }
    }

    allowed
}

pub fn rule_list_allows(rules: &[Rule]) -> bool {
    let mut allowed = false;
    for rule in rules {
        if rule_matches(rule) {
            allowed = rule.action == "allow";
        }
    }
    allowed
}

fn rule_matches(rule: &Rule) -> bool {
    if rule
        .features
        .as_ref()
        .is_some_and(|features| !features.is_empty())
    {
        // Launcher feature flags (quick play, demo, custom resolution, etc.) are not
        // supported yet — skip those conditional argument blocks.
        return false;
    }

    let Some(os) = &rule.os else {
        return true;
    };

    match os.name.as_deref() {
        Some("windows") => cfg!(target_os = "windows"),
        Some("linux") => cfg!(target_os = "linux"),
        Some("osx") => cfg!(target_os = "macos"),
        Some(_) => false,
        None => true,
    }
}
