use crate::auth;
use crate::config::{FLAX_VERSION, RELEASE_LABEL, LauncherConfig, app_dir, clean_player_name, minecraft_dir};
use crate::events::WorkerEvent;
use crate::launcher;
use eframe::egui::{
    self, Align, Color32, ColorImage, Context, FontData, FontDefinitions, FontFamily, Frame,
    Layout, Pos2, Rect, RichText, Sense, Stroke, StrokeKind, TextureHandle, Ui, Vec2,
    ViewportCommand,
};
use std::fs;
use std::collections::VecDeque;
use std::path::PathBuf;
use std::sync::mpsc::{self, Receiver, Sender};
use std::thread;
use std::time::Duration;

const INTER_FONT_BYTES: &[u8] =
    include_bytes!("../../src/main/resources/assets/minecraft/soar/fonts/inter/Inter-Regular.ttf");
const FALLBACK_FONT_BYTES: &[u8] =
    include_bytes!("../../src/main/resources/assets/minecraft/soar/fonts/fallback.ttf");

pub struct FlaxLauncherApp {
    config: LauncherConfig,
    events: Option<Receiver<WorkerEvent>>,
    logs: VecDeque<String>,
    progress_label: String,
    progress_current: u64,
    progress_total: u64,
    busy: bool,
    login_in_progress: bool,
    login_cancel_tx: Option<Sender<()>>,
    device_code: Option<DeviceCodeState>,
    icon: TextureHandle,
    java_edit: String,
    show_settings: bool,
    show_logs: bool,
    memory_limit_mb: u32,
    native_window_ready: bool,
}

#[derive(Debug, Clone)]
struct DeviceCodeState {
    verification_uri: String,
    user_code: String,
    message: String,
}

impl FlaxLauncherApp {
    pub fn new(cc: &eframe::CreationContext<'_>, icon_bytes: &[u8]) -> Self {
        install_fonts(&cc.egui_ctx);
        install_theme(&cc.egui_ctx);

        let mut config = LauncherConfig::load();
        let memory_limit_mb = detect_memory_limit_mb();
        config.memory_mb = normalize_memory_mb(config.memory_mb, memory_limit_mb);

        let icon = load_icon_texture(&cc.egui_ctx, icon_bytes);
        let java_edit = config.java_path.clone();
        let mut logs = VecDeque::new();
        logs.push_back("Ready.".to_owned());

        Self {
            config,
            events: None,
            logs,
            progress_label: "Ready".to_owned(),
            progress_current: 0,
            progress_total: 1,
            busy: false,
            login_in_progress: false,
            login_cancel_tx: None,
            device_code: None,
            icon,
            java_edit,
            show_settings: false,
            show_logs: false,
            memory_limit_mb,
            native_window_ready: false,
        }
    }

    fn start_launch(&mut self) {
        if self.busy {
            return;
        }

        self.commit_edits();
        let (tx, rx) = mpsc::channel();
        let config = self.config.clone();
        self.events = Some(rx);
        self.busy = true;
        self.login_in_progress = false;
        self.login_cancel_tx = None;
        self.device_code = None;
        self.progress_label = "Preparing".to_owned();
        self.progress_current = 0;
        self.progress_total = 1;
        self.push_log("Launch requested.");

        thread::spawn(move || {
            let result = launcher::prepare_and_launch(config, tx.clone());
            match result {
                Ok(updated) => {
                    if let Some(account) = updated {
                        let _ = tx.send(WorkerEvent::AccountUpdated(account));
                    }
                    let _ = tx.send(WorkerEvent::Finished("Launch started.".to_owned()));
                }
                Err(error) => {
                    let _ = tx.send(WorkerEvent::Failed(format!("{error:#}")));
                }
            }
        });
    }

    fn start_login(&mut self) {
        if self.busy {
            return;
        }

        self.commit_edits();
        let (tx, rx) = mpsc::channel();
        let (cancel_tx, cancel_rx) = mpsc::channel();
        let client_id = self.config.microsoft_client_id.clone();
        self.events = Some(rx);
        self.busy = true;
        self.login_in_progress = true;
        self.login_cancel_tx = Some(cancel_tx);
        self.device_code = None;
        self.show_settings = true;
        self.progress_label = "Microsoft login".to_owned();
        self.progress_current = 0;
        self.progress_total = 1;
        self.push_log("Microsoft login requested.");

        thread::spawn(move || {
            let result = auth::device_login(&client_id, &tx, cancel_rx);
            match result {
                Ok(account) => {
                    let _ = tx.send(WorkerEvent::Authenticated(account));
                    let _ = tx.send(WorkerEvent::Finished(
                        "Microsoft login complete.".to_owned(),
                    ));
                }
                Err(error) => {
                    let message = format!("{error:#}");
                    if is_cancelled_message(&message) {
                        let _ = tx.send(WorkerEvent::Finished(
                            "Microsoft login cancelled.".to_owned(),
                        ));
                    } else {
                        let _ = tx.send(WorkerEvent::Failed(message));
                    }
                }
            }
        });
    }

    fn cancel_login(&mut self) {
        if !self.login_in_progress {
            return;
        }

        if let Some(cancel_tx) = self.login_cancel_tx.take() {
            let _ = cancel_tx.send(());
        }
        self.progress_label = "Cancelling login...".to_owned();
        self.push_log("Cancelling Microsoft login...");
    }

    fn poll_events(&mut self) {
        let Some(rx) = self.events.take() else {
            return;
        };

        let mut keep_receiver = true;
        while let Ok(event) = rx.try_recv() {
            match event {
                WorkerEvent::Log(message) => self.push_log(message),
                WorkerEvent::Progress {
                    label,
                    current,
                    total,
                } => {
                    self.progress_label = label;
                    self.progress_current = current;
                    self.progress_total = total.max(1);
                }
                WorkerEvent::DeviceCode {
                    verification_uri,
                    user_code,
                    message,
                } => {
                    self.device_code = Some(DeviceCodeState {
                        verification_uri,
                        user_code,
                        message,
                    });
                    self.progress_label = "Enter code".to_owned();
                    self.show_settings = true;
                }
                WorkerEvent::Authenticated(account) | WorkerEvent::AccountUpdated(account) => {
                    self.config.account = Some(account);
                    if let Err(error) = self.config.save() {
                        self.push_log(format!("Failed to save settings: {error:#}"));
                    }
                }
                WorkerEvent::LaunchStarted(pid) => {
                    self.progress_label = format!("Running ({pid})");
                    self.push_log(format!("Minecraft started. PID {pid}."));
                }
                WorkerEvent::Finished(message) => {
                    self.finish_busy_state();
                    self.progress_current = self.progress_total;
                    self.push_log(message);
                    keep_receiver = false;
                }
                WorkerEvent::Failed(message) => {
                    self.finish_busy_state();
                    self.progress_label = "Failed".to_owned();
                    self.show_settings = true;
                    self.show_logs = true;
                    self.push_log(format!("ERROR: {message}"));
                    keep_receiver = false;
                }
            }
        }

        if keep_receiver {
            self.events = Some(rx);
        }
    }

    fn finish_busy_state(&mut self) {
        self.busy = false;
        self.login_in_progress = false;
        self.login_cancel_tx = None;
        self.device_code = None;
    }

    fn commit_edits(&mut self) {
        self.config.offline_name = clean_player_name(&self.config.offline_name);
        self.config.memory_mb = normalize_memory_mb(self.config.memory_mb, self.memory_limit_mb);
        self.config.java_path = self.java_edit.trim().to_owned();
        if let Err(error) = self.config.save() {
            self.push_log(format!("Failed to save settings: {error:#}"));
        }
    }

    fn push_log(&mut self, message: impl Into<String>) {
        while self.logs.len() > 120 {
            self.logs.pop_front();
        }
        self.logs.push_back(message.into());
    }

    fn progress_value(&self) -> f32 {
        (self.progress_current as f32 / self.progress_total.max(1) as f32).clamp(0.0, 1.0)
    }

    fn ensure_native_window_style(&mut self) {
        if self.native_window_ready {
            return;
        }

        #[cfg(windows)]
        {
            use windows_sys::Win32::Graphics::Dwm::{
                DWMWA_WINDOW_CORNER_PREFERENCE, DWMWCP_ROUND, DwmSetWindowAttribute,
            };
            use windows_sys::Win32::UI::WindowsAndMessaging::GetForegroundWindow;

            let hwnd = unsafe { GetForegroundWindow() };
            if !hwnd.is_null() {
                let preference = DWMWCP_ROUND;
                let _ = unsafe {
                    DwmSetWindowAttribute(
                        hwnd,
                        DWMWA_WINDOW_CORNER_PREFERENCE as u32,
                        &preference as *const _ as *const core::ffi::c_void,
                        std::mem::size_of_val(&preference) as u32,
                    )
                };
                self.native_window_ready = true;
            }
        }

        #[cfg(not(windows))]
        {
            self.native_window_ready = true;
        }
    }
}

impl eframe::App for FlaxLauncherApp {
    fn clear_color(&self, _visuals: &egui::Visuals) -> [f32; 4] {
        [0.05, 0.06, 0.08, 1.0]
    }

    fn update(&mut self, ctx: &Context, _frame: &mut eframe::Frame) {
        if self.busy {
            ctx.request_repaint_after(Duration::from_millis(200));
        }

        install_theme(ctx);
        self.ensure_native_window_style();
        self.poll_events();

        egui::CentralPanel::default()
            .frame(Frame::new().fill(BG))
            .show(ctx, |ui| {
                let rect = ui.max_rect().shrink(1.0);
                paint_background(ui, rect);
                self.title_bar(ctx, ui, rect);

                let content_rect = Rect::from_min_max(
                    Pos2::new(rect.left() + 24.0, rect.top() + 54.0),
                    Pos2::new(rect.right() - 24.0, rect.bottom() - 20.0),
                );
                self.sliding_content(ctx, ui, content_rect);
            });
    }
}

impl FlaxLauncherApp {
    fn title_bar(&mut self, ctx: &Context, ui: &mut Ui, rect: Rect) {
        let bar = Rect::from_min_size(rect.min, Vec2::new(rect.width(), 36.0));
        ui.painter().line_segment(
            [
                Pos2::new(bar.left() + 1.0, bar.bottom()),
                Pos2::new(bar.right() - 1.0, bar.bottom()),
            ],
            Stroke::new(1.0, BORDER),
        );

        let drag_rect = Rect::from_min_max(bar.min, Pos2::new(bar.right() - 80.0, bar.bottom()));
        let drag = ui.interact(
            drag_rect,
            ui.id().with("title-drag"),
            Sense::click_and_drag(),
        );
        if drag.drag_started() {
            ctx.send_viewport_cmd(ViewportCommand::StartDrag);
        }

        ui.scope_builder(egui::UiBuilder::new().max_rect(bar.shrink(8.0)), |ui| {
            ui.horizontal(|ui| {
                ui.image((self.icon.id(), Vec2::splat(16.0)));
                ui.label(
                    RichText::new(format!("Flax Launcher - {RELEASE_LABEL}"))
                        .size(13.0)
                        .strong()
                        .color(TEXT),
                );
                ui.with_layout(Layout::right_to_left(Align::Center), |ui| {
                    if traffic_button(ui, CLOSE).clicked() {
                        ctx.send_viewport_cmd(ViewportCommand::Close);
                    }
                    if traffic_button(ui, MINIMIZE).clicked() {
                        ctx.send_viewport_cmd(ViewportCommand::Minimized(true));
                    }
                });
            });
        });
    }

    fn sliding_content(&mut self, ctx: &Context, ui: &mut Ui, rect: Rect) {
        let t =
            ctx.animate_bool_with_time(ui.id().with("settings-slide"), self.show_settings, 0.18);
        let distance = rect.width() + 52.0;
        let home_rect = rect.translate(Vec2::new(-distance * t, 0.0));
        let settings_rect = rect.translate(Vec2::new(distance * (1.0 - t), 0.0));

        if !self.show_settings || t < 0.999 {
            ui.scope_builder(
                egui::UiBuilder::new()
                    .id_salt("home-screen")
                    .max_rect(home_rect),
                |ui| {
                    ui.shrink_clip_rect(rect);
                    self.home(ui);
                },
            );
        }

        if self.show_settings || t > 0.001 {
            ui.scope_builder(
                egui::UiBuilder::new()
                    .id_salt("settings-screen")
                    .max_rect(settings_rect),
                |ui| {
                    ui.shrink_clip_rect(rect);
                    self.settings_screen(ui);
                },
            );
        }
    }

    fn home(&mut self, ui: &mut Ui) {
        let rect = ui.max_rect();
        let controls_height = 126.0;
        let controls_rect = Rect::from_min_max(
            Pos2::new(rect.left(), rect.bottom() - controls_height),
            rect.right_bottom(),
        );
        let main_rect = Rect::from_min_max(
            rect.min,
            Pos2::new(rect.right(), controls_rect.top() - 18.0),
        );

        ui.scope_builder(egui::UiBuilder::new().max_rect(main_rect), |ui| {
            let hero_height = 242.0;
            let status_height = 116.0;
            let gap = 16.0;

            let hero_rect = Rect::from_min_max(
                main_rect.min,
                Pos2::new(main_rect.right(), main_rect.top() + hero_height),
            );
            let status_rect = Rect::from_min_max(
                Pos2::new(main_rect.left(), hero_rect.bottom() + gap),
                Pos2::new(main_rect.right(), hero_rect.bottom() + gap + status_height),
            );
            let stats_rect = Rect::from_min_max(
                Pos2::new(main_rect.left(), status_rect.bottom() + gap),
                main_rect.right_bottom(),
            );

            ui.scope_builder(egui::UiBuilder::new().max_rect(hero_rect), |ui| {
                ui.vertical_centered_justified(|ui| {
                    ui.add_space(8.0);
                    ui.image((self.icon.id(), Vec2::splat(132.0)));
                    ui.add_space(18.0);
                    ui.label(
                        RichText::new("FlaxClient")
                            .size(36.0)
                            .strong()
                            .color(TEXT)
                            .extra_letter_spacing(0.8),
                    );
                    ui.add_space(8.0);
                    ui.label(muted(self.account_line()));
                });
            });

            ui.scope_builder(egui::UiBuilder::new().max_rect(status_rect), |ui| {
                home_panel(ui, |ui| {
                    ui.label(muted(self.progress_label.clone()));
                    ui.add_space(10.0);
                    draw_progress_bar(ui, ui.available_width(), self.progress_value());
                    ui.add_space(12.0);

                    let last = self
                        .logs
                        .back()
                        .cloned()
                        .unwrap_or_else(|| "Ready.".to_owned());
                    ui.add(
                        egui::Label::new(code(last))
                            .truncate()
                            .selectable(false),
                    );
                });
            });

            ui.scope_builder(egui::UiBuilder::new().max_rect(stats_rect), |ui| {
                home_panel(ui, |ui| {
                    simple_stat(ui, "Memory", &format!("{} MB", self.config.memory_mb));
                    simple_stat(
                        ui,
                        "Java",
                        if self.config.use_bundled_java {
                            "Bundled"
                        } else {
                            "Custom"
                        },
                    );
                    simple_stat(
                        ui,
                        "Mode",
                        if self.config.account.is_some() {
                            "Online"
                        } else {
                            "Offline"
                        },
                    );
                });
            });
        });

        ui.scope_builder(
            egui::UiBuilder::new().max_rect(controls_rect.shrink2(Vec2::new(24.0, 0.0))),
            |ui| {
                ui.with_layout(Layout::left_to_right(Align::Center), |ui| {
                    if hamburger_button(ui, Vec2::new(62.0, 62.0)).clicked() {
                        self.show_settings = true;
                    }
                    ui.add_space(12.0);

                    let play_text = if self.busy {
                        "Working..."
                    } else {
                        "LAUNCH CLIENT"
                    };

                    let play = egui::Button::new(
                        RichText::new(play_text)
                            .size(18.0)
                            .strong()
                            .color(Color32::from_rgb(12, 12, 14)),
                    )
                    .fill(if self.busy { ACTION_DISABLED } else { ACTION })
                    .stroke(Stroke::new(1.0, ACTION_EDGE))
                    .corner_radius(10.0)
                    .min_size(Vec2::new(ui.available_width(), 62.0));

                    if ui.add_enabled(!self.busy, play).clicked() {
                        self.start_launch();
                    }
                });
                ui.add_space(8.0);
                ui.vertical_centered(|ui| {
                    ui.label(RichText::new("FlaxLauncher").size(10.0).color(MUTED_DIM));
                });
            },
        );
    }

    fn settings_screen(&mut self, ui: &mut Ui) {
        home_panel(ui, |ui| {
            ui.horizontal(|ui| {
                if ui
                    .add_sized([78.0, 36.0], secondary_button("Back"))
                    .clicked()
                {
                    self.show_settings = false;
                }
                ui.add_space(12.0);
                ui.vertical(|ui| {
                    ui.label(RichText::new("Settings").size(18.0).strong().color(TEXT));
                    ui.label(muted("Launcher configuration and runtime options"));
                });
            });
        });
        ui.add_space(16.0);

        egui::ScrollArea::vertical()
            .auto_shrink([true, false])
            .scroll_bar_visibility(egui::scroll_area::ScrollBarVisibility::AlwaysHidden)
            .show(ui, |ui| {
                ui.set_width(ui.available_width());

                settings_card(ui, "Account", "Microsoft login and offline identity", |ui| {
                    self.account_settings(ui);
                });

                ui.add_space(16.0);
                settings_card(ui, "Runtime", "Memory budget and Java selection", |ui| {
                    self.runtime_settings(ui);
                });

                ui.add_space(16.0);
                settings_card(ui, "Folders", "Open the launcher and Minecraft directories", |ui| {
                    self.folder_settings(ui);
                });

                ui.add_space(16.0);
                settings_card(ui, "System Logs", "Recent launcher output and diagnostics", |ui| {
                    self.log_settings(ui);
                });
            });
    }

    fn account_line(&self) -> String {
        match &self.config.account {
            Some(account) => format!("Signed in as {}", account.username()),
            None => format!(
                "Offline as {}",
                clean_player_name(&self.config.offline_name)
            ),
        }
    }

    fn account_settings(&mut self, ui: &mut Ui) {
        ui.horizontal(|ui| {
            ui.with_layout(Layout::right_to_left(Align::Center), |ui| {
                if self.login_in_progress {
                    if ui.add_sized([130.0, 36.0], warn_button("Cancel")).clicked() {
                        self.cancel_login();
                    }
                } else {
                    let login_button = primary_button("Login as Microsoft Account")
                        .min_size(Vec2::new(230.0, 36.0));
                    if ui.add_enabled(!self.busy, login_button).clicked() {
                        self.start_login();
                    }
                }
            });
        });
        ui.add_space(12.0);

        match &self.config.account {
            Some(account) => {
                let username = account.username().to_owned();
                inset_panel(ui, |ui| {
                    ui.label(muted("Current account"));
                    ui.add_space(6.0);
                    ui.label(
                        RichText::new(username)
                            .size(18.0)
                            .strong()
                            .color(TEXT),
                    );
                    ui.add_space(12.0);
                    if ui
                        .add_enabled(
                            !self.busy,
                            secondary_button("Sign out").min_size(Vec2::new(110.0, 36.0)),
                        )
                        .clicked()
                    {
                        self.config.account = None;
                        let _ = self.config.save();
                        self.push_log("Signed out.");
                    }
                });
            }
            None => {
                inset_panel(ui, |ui| {
                    ui.label(muted("Offline name"));
                    ui.add_space(6.0);
                    if underlined_text_edit(ui, &mut self.config.offline_name, "Player name")
                        .lost_focus()
                    {
                        self.config.offline_name = clean_player_name(&self.config.offline_name);
                        let _ = self.config.save();
                    }
                });
            }
        }

        if let Some(code_state) = self.device_code.clone() {
            ui.add_space(10.0);
            inset_panel(ui, |ui| {
                ui.label(muted("Enter this code at microsoft.com/link"));
                ui.add_space(6.0);
                ui.label(
                    RichText::new(&code_state.user_code)
                        .size(28.0)
                        .monospace()
                        .strong()
                        .color(ACCENT),
                );
                ui.add_space(10.0);

                ui.horizontal(|ui| {
                    if ui.add_sized([122.0, 36.0], primary_button("Open URL")).clicked() {
                        ui.ctx().copy_text(code_state.user_code.clone());
                        let _ = open::that(&code_state.verification_uri);
                    }

                    if ui.add_sized([100.0, 36.0], secondary_button("Cancel")).clicked() {
                        self.cancel_login();
                    }
                });

                ui.add_space(10.0);
                ui.add(egui::Label::new(code(&code_state.message)).wrap());
            });
        }
    }

    fn runtime_settings(&mut self, ui: &mut Ui) {
        inset_panel(ui, |ui| {
            ui.horizontal(|ui| {
                ui.label(muted("Allocated memory"));
                ui.with_layout(Layout::right_to_left(Align::Center), |ui| {
                    ui.label(
                        RichText::new(format!(
                            "{} / {} MB",
                            self.config.memory_mb, self.memory_limit_mb
                        ))
                        .size(14.0)
                        .strong()
                        .color(TEXT),
                    );
                });
            });
            ui.add_space(6.0);
            ui.label(muted(format!(
                "Recommended for {}: {} MB",
                FLAX_VERSION,
                crate::config::recommended_memory_mb(FLAX_VERSION)
            )));
            ui.add_space(10.0);

            if thin_slider(ui, &mut self.config.memory_mb, self.memory_limit_mb).changed() {
                self.config.memory_mb =
                    normalize_memory_mb(self.config.memory_mb, self.memory_limit_mb);
                let _ = self.config.save();
            }
        });
        ui.add_space(12.0);

        inset_panel(ui, |ui| {
            let bundled_java_label = self.config.bundled_java_label();
            if ui
                .checkbox(&mut self.config.use_bundled_java, bundled_java_label)
                .changed()
            {
                let _ = self.config.save();
            }

            ui.add_space(10.0);
            ui.add_enabled_ui(!self.config.use_bundled_java, |ui| {
                ui.label(muted("Custom Java binary"));
                ui.add_space(6.0);
                ui.horizontal(|ui| {
                    let browse_width = 92.0;
                    let field_width = (ui.available_width() - browse_width - 10.0).max(120.0);

                    let response = ui.add_sized(
                        [field_width, 24.0],
                        egui::TextEdit::singleline(&mut self.java_edit)
                            .frame(false)
                            .margin(Vec2::ZERO)
                            .desired_width(field_width)
                            .hint_text(crate::config::java_binary_hint()),
                    );

                    let underline_color = if response.has_focus() { ACCENT } else { BORDER };
                    let underline_y = response.rect.bottom() + 2.0;
                    ui.painter().line_segment(
                        [
                            Pos2::new(response.rect.left(), underline_y),
                            Pos2::new(response.rect.right(), underline_y),
                        ],
                        Stroke::new(1.0, underline_color),
                    );

                    if response.lost_focus() {
                        self.commit_edits();
                    }

                    ui.add_space(10.0);

                    if ui.add_sized([browse_width, 36.0], secondary_button("Browse")).clicked() {
                        if let Some(path) = pick_java_binary() {
                            self.java_edit = path.to_string_lossy().to_string();
                            self.commit_edits();
                        }
                    }
                });
            });
        });
    }

    fn folder_settings(&mut self, ui: &mut Ui) {
        inset_panel(ui, |ui| {
            ui.label(muted("Quick links"));
            ui.add_space(10.0);
            let spacing = ui.spacing().item_spacing.x;
            let width = ((ui.available_width() - spacing * 2.0) / 3.0).max(0.0);
            ui.horizontal(|ui| {
                if ui
                    .add_sized([width, 38.0], secondary_button("Data"))
                    .clicked()
                {
                    let _ = open::that(app_dir());
                }
                if ui
                    .add_sized([width, 38.0], secondary_button("Packs"))
                    .clicked()
                {
                    let _ = open::that(minecraft_dir().join("resourcepacks"));
                }
                if ui
                    .add_sized([width, 38.0], secondary_button("Logs"))
                    .clicked()
                {
                    let _ = open::that(app_dir().join("logs"));
                }
            });
        });
    }

    fn log_settings(&mut self, ui: &mut Ui) {
        inset_panel(ui, |ui| {
            ui.horizontal(|ui| {
                ui.label(muted("Latest event"));
                ui.with_layout(Layout::right_to_left(Align::Center), |ui| {
                    if ui
                        .add_sized(
                            [116.0, 36.0],
                            secondary_button(if self.show_logs {
                                "Hide log"
                            } else {
                                "Show log"
                            }),
                        )
                        .clicked()
                    {
                        self.show_logs = !self.show_logs;
                    }
                });
            });
            ui.add_space(10.0);

            let last = self
                .logs
                .back()
                .cloned()
                .unwrap_or_else(|| "Ready.".to_owned());
            ui.add(egui::Label::new(code(last)).wrap());

            if self.show_logs {
                ui.add_space(12.0);
                inset_panel(ui, |ui| {
                    egui::ScrollArea::vertical()
                        .stick_to_bottom(true)
                        .max_height(138.0)
                        .show(ui, |ui| {
                            for entry in &self.logs {
                                ui.add(egui::Label::new(code(entry)).wrap());
                            }
                        });
                });
            }
        });
    }
}

const WINDOW_RADIUS: f32 = 12.0;
const BG: Color32 = Color32::from_rgb(12, 16, 24);
const ACCENT: Color32 = Color32::from_rgb(0, 199, 255);
const ACTION: Color32 = Color32::from_rgb(0, 180, 240);
const ACTION_DISABLED: Color32 = Color32::from_rgb(60, 60, 65);
const ACTION_EDGE: Color32 = Color32::from_rgb(70, 120, 145);
const TEXT: Color32 = Color32::from_rgb(245, 247, 251);
const MUTED: Color32 = Color32::from_rgb(156, 164, 178);
const MUTED_DIM: Color32 = Color32::from_rgb(108, 114, 124);
const BORDER: Color32 = Color32::from_rgb(36, 42, 54);
const MINIMIZE: Color32 = Color32::from_rgb(242, 199, 76);
const CLOSE: Color32 = Color32::from_rgb(241, 97, 66);

fn paint_background(ui: &mut Ui, rect: Rect) {
    let painter = ui.painter();
    painter.rect(
        rect,
        WINDOW_RADIUS,
        BG,
        Stroke::new(1.0, BORDER),
        StrokeKind::Inside,
    );
}

fn divider(ui: &mut Ui) {
    let width = ui.available_width().min(300.0);
    rule(ui, width, BORDER);
}

fn home_panel<R>(ui: &mut Ui, add_contents: impl FnOnce(&mut Ui) -> R) {
    Frame::new()
        .fill(Color32::from_rgb(15, 20, 30))
        .stroke(Stroke::new(1.0, BORDER))
        .corner_radius(14.0)
        .inner_margin(16.0)
        .show(ui, |ui| {
            ui.set_width(ui.available_width());
            add_contents(ui);
        });
}

fn simple_stat(ui: &mut Ui, label: &str, value: &str) {
    ui.horizontal(|ui| {
        ui.add_space(8.0);
        ui.label(RichText::new(label).size(12.0).color(MUTED));
        ui.with_layout(Layout::right_to_left(Align::Center), |ui| {
            ui.label(RichText::new(value).size(12.0).strong().color(TEXT));
        });
    });
    ui.add_space(8.0);
    divider(ui);
    ui.add_space(8.0);
}

fn draw_progress_bar(ui: &mut Ui, width: f32, progress: f32) {
    let (rect, _) = ui.allocate_exact_size(Vec2::new(width, 10.0), Sense::hover());
    let painter = ui.painter();

    painter.rect(
        rect,
        999.0,
        Color32::from_rgb(28, 32, 40),
        Stroke::new(1.0, BORDER),
        StrokeKind::Inside,
    );

    let fill_width = (rect.width() * progress).clamp(0.0, rect.width());
    if fill_width > 0.0 {
        let fill_rect = Rect::from_min_max(
            rect.min,
            Pos2::new((rect.left() + fill_width).min(rect.right()), rect.bottom()),
        );
        painter.rect_filled(fill_rect, 999.0, ACTION);
    }
}

fn settings_card<R>(
    ui: &mut Ui,
    title: &str,
    subtitle: &str,
    add_contents: impl FnOnce(&mut Ui) -> R,
) {
    home_panel(ui, |ui| {
        ui.label(RichText::new(title).size(16.0).strong().color(TEXT));
        ui.add_space(2.0);
        ui.label(muted(subtitle));
        ui.add_space(12.0);
        divider(ui);
        ui.add_space(14.0);
        add_contents(ui);
    });
}

fn inset_panel<R>(ui: &mut Ui, add_contents: impl FnOnce(&mut Ui) -> R) {
    Frame::new()
        .fill(Color32::from_rgb(20, 24, 34))
        .stroke(Stroke::new(1.0, BORDER))
        .corner_radius(12.0)
        .inner_margin(14.0)
        .show(ui, |ui| {
            ui.set_width(ui.available_width());
            add_contents(ui);
        });
}

fn rule(ui: &mut Ui, width: f32, color: Color32) {
    let (rect, _) = ui.allocate_exact_size(Vec2::new(width, 1.0), Sense::hover());
    ui.painter().line_segment(
        [rect.left_center(), rect.right_center()],
        Stroke::new(1.0, color),
    );
}

fn underlined_text_edit(ui: &mut Ui, value: &mut String, hint: &str) -> egui::Response {
    ui.vertical(|ui| {
        let response = ui.add(
            egui::TextEdit::singleline(value)
                .frame(false)
                .margin(Vec2::ZERO)
                .desired_width(ui.available_width())
                .hint_text(hint),
        );
        ui.add_space(4.0);
        rule(
            ui,
            ui.available_width(),
            if response.has_focus() { ACCENT } else { BORDER },
        );
        response
    })
    .inner
}

fn thin_slider(ui: &mut Ui, value: &mut u32, max_value: u32) -> egui::Response {
    let desired_size = Vec2::new(ui.available_width(), 16.0);
    let (rect, mut response) = ui.allocate_exact_size(desired_size, Sense::click_and_drag());
    let line_rect = Rect::from_center_size(rect.center(), Vec2::new(rect.width(), 2.0));

    let min_value = 512_u32;
    let max_value = max_value.max(min_value);
    let range = (max_value - min_value).max(1) as f32;

    if let Some(pointer_pos) = response.interact_pointer_pos() {
        if response.dragged() || response.clicked() {
            let t = ((pointer_pos.x - line_rect.left()) / line_rect.width()).clamp(0.0, 1.0);
            let raw = min_value as f32 + t * range;
            let stepped = (((raw / 256.0).round() as u32).max(2)) * 256;
            let next = stepped.clamp(min_value, max_value);
            if next != *value {
                *value = next;
                response.mark_changed();
            }
        }
    }

    ui.painter()
        .rect_filled(line_rect, 0.0, Color32::from_rgb(26, 30, 39));

    let progress = (*value - min_value) as f32 / range;
    let fill_width = (line_rect.width() * progress).clamp(0.0, line_rect.width());
    if fill_width > 0.0 {
        let fill_rect = Rect::from_min_max(
            line_rect.min,
            Pos2::new(line_rect.left() + fill_width, line_rect.bottom()),
        );
        ui.painter().rect_filled(fill_rect, 0.0, ACCENT);
    }

    if response.hovered() || response.has_focus() {
        ui.painter().line_segment(
            [
                Pos2::new(line_rect.left(), line_rect.bottom() + 3.0),
                Pos2::new(line_rect.right(), line_rect.bottom() + 3.0),
            ],
            Stroke::new(1.0, BORDER),
        );
    }

    response
}

fn traffic_button(ui: &mut Ui, color: Color32) -> egui::Response {
    let (rect, response) = ui.allocate_exact_size(Vec2::splat(18.0), Sense::click());
    let draw_color = if response.hovered() {
        lighten(color, 20)
    } else {
        color
    };
    ui.painter().circle_filled(rect.center(), 8.0, draw_color);
    response
}

fn hamburger_button(ui: &mut Ui, size: Vec2) -> egui::Response {
    let (rect, response) = ui.allocate_exact_size(size, Sense::click());
    let fill = if response.hovered() {
        Color32::from_rgb(28, 34, 44)
    } else {
        Color32::from_rgb(22, 26, 34)
    };
    ui.painter().rect(
        rect,
        0.0,
        fill,
        Stroke::new(1.0, BORDER),
        StrokeKind::Inside,
    );

    let left = rect.left() + 16.0;
    let right = rect.right() - 16.0;
    for offset in [-8.0_f32, 0.0, 8.0] {
        let y = rect.center().y + offset;
        ui.painter().line_segment(
            [Pos2::new(left, y), Pos2::new(right, y)],
            Stroke::new(1.6, TEXT),
        );
    }

    response
}

fn primary_button(text: &str) -> egui::Button<'_> {
    egui::Button::new(
        RichText::new(text)
            .size(12.0)
            .strong()
            .color(Color32::from_rgb(16, 18, 22)),
    )
    .fill(Color32::from_rgb(128, 186, 222))
    .stroke(Stroke::new(1.0, Color32::from_rgb(110, 150, 180)))
    .corner_radius(10.0)
    .min_size(Vec2::new(0.0, 36.0))
}

fn secondary_button(text: &str) -> egui::Button<'_> {
    egui::Button::new(RichText::new(text).size(12.0).color(TEXT))
        .fill(Color32::from_rgb(24, 28, 36))
        .stroke(Stroke::new(1.0, BORDER))
        .corner_radius(10.0)
        .min_size(Vec2::new(0.0, 36.0))
}

fn warn_button(text: &str) -> egui::Button<'_> {
    egui::Button::new(RichText::new(text).size(12.0).strong().color(TEXT))
        .fill(Color32::from_rgb(96, 42, 36))
        .stroke(Stroke::new(1.0, Color32::from_rgb(125, 72, 60)))
        .corner_radius(10.0)
        .min_size(Vec2::new(0.0, 36.0))
}

fn muted(text: impl Into<String>) -> RichText {
    RichText::new(text).size(12.0).color(MUTED)
}

fn code(text: impl Into<String>) -> RichText {
    RichText::new(text).size(11.0).monospace().color(MUTED)
}

fn lighten(color: Color32, amount: u8) -> Color32 {
    Color32::from_rgba_unmultiplied(
        color.r().saturating_add(amount),
        color.g().saturating_add(amount),
        color.b().saturating_add(amount),
        color.a(),
    )
}

fn normalize_memory_mb(value: u32, max_value: u32) -> u32 {
    let max_value = max_value.max(512);
    let clamped = value.clamp(512, max_value);
    let stepped = (clamped / 256).max(2) * 256;
    stepped.min(max_value)
}

fn detect_memory_limit_mb() -> u32 {
    let total_mb = detect_total_memory_mb().unwrap_or(8192);
    let rounded = (total_mb / 256).max(2) * 256;
    rounded.clamp(1024, 65536)
}

#[cfg(windows)]
fn detect_total_memory_mb() -> Option<u32> {
    use windows_sys::Win32::System::SystemInformation::{GlobalMemoryStatusEx, MEMORYSTATUSEX};

    let mut status: MEMORYSTATUSEX = unsafe { std::mem::zeroed() };
    status.dwLength = std::mem::size_of::<MEMORYSTATUSEX>() as u32;

    let ok = unsafe { GlobalMemoryStatusEx(&mut status as *mut MEMORYSTATUSEX) };
    if ok == 0 {
        return None;
    }

    u32::try_from(status.ullTotalPhys / 1024 / 1024).ok()
}

#[cfg(target_os = "linux")]
fn detect_total_memory_mb() -> Option<u32> {
    let meminfo = fs::read_to_string("/proc/meminfo").ok()?;
    let kilobytes = meminfo.lines().find_map(|line| {
        let rest = line.strip_prefix("MemTotal:")?;
        rest.split_whitespace().next()?.parse::<u64>().ok()
    })?;
    u32::try_from(kilobytes / 1024).ok()
}

#[cfg(all(not(windows), not(target_os = "linux")))]
fn detect_total_memory_mb() -> Option<u32> {
    None
}

fn is_cancelled_message(message: &str) -> bool {
    message.to_ascii_lowercase().contains("cancel")
}

fn install_fonts(ctx: &Context) {
    let mut fonts = FontDefinitions::default();
    fonts.font_data.insert(
        "inter".to_owned(),
        FontData::from_static(INTER_FONT_BYTES).into(),
    );
    fonts.font_data.insert(
        "fallback".to_owned(),
        FontData::from_static(FALLBACK_FONT_BYTES).into(),
    );
    fonts
        .families
        .entry(FontFamily::Proportional)
        .or_default()
        .insert(0, "inter".to_owned());
    fonts
        .families
        .entry(FontFamily::Proportional)
        .or_default()
        .insert(1, "fallback".to_owned());
    fonts
        .families
        .entry(FontFamily::Monospace)
        .or_default()
        .insert(0, "fallback".to_owned());
    fonts
        .families
        .entry(FontFamily::Monospace)
        .or_default()
        .insert(1, "inter".to_owned());
    ctx.set_fonts(fonts);
}

#[cfg(windows)]
fn pick_java_binary() -> Option<PathBuf> {
    rfd::FileDialog::new()
        .add_filter("Java executable", &["exe"])
        .pick_file()
}

#[cfg(not(windows))]
fn pick_java_binary() -> Option<PathBuf> {
    None
}

fn install_theme(ctx: &Context) {
    let mut visuals = egui::Visuals::dark();
    visuals.override_text_color = Some(TEXT);
    visuals.panel_fill = BG;
    visuals.window_fill = BG;
    visuals.extreme_bg_color = BG;
    visuals.faint_bg_color = Color32::from_rgb(22, 26, 34);
    visuals.code_bg_color = Color32::TRANSPARENT;
    visuals.widgets.noninteractive.bg_fill = Color32::TRANSPARENT;
    visuals.widgets.noninteractive.weak_bg_fill = Color32::TRANSPARENT;
    visuals.widgets.noninteractive.bg_stroke = Stroke::new(1.0, BORDER);
    visuals.widgets.inactive.bg_fill = Color32::TRANSPARENT;
    visuals.widgets.inactive.weak_bg_fill = Color32::TRANSPARENT;
    visuals.widgets.inactive.bg_stroke = Stroke::new(1.0, BORDER);
    visuals.widgets.inactive.fg_stroke = Stroke::new(1.0, TEXT);
    visuals.widgets.hovered.bg_fill = Color32::from_rgb(20, 24, 31);
    visuals.widgets.hovered.weak_bg_fill = Color32::from_rgb(20, 24, 31);
    visuals.widgets.hovered.bg_stroke = Stroke::new(1.0, ACCENT);
    visuals.widgets.hovered.fg_stroke = Stroke::new(1.0, TEXT);
    visuals.widgets.active.bg_fill = Color32::from_rgb(20, 24, 31);
    visuals.widgets.active.weak_bg_fill = Color32::from_rgb(20, 24, 31);
    visuals.widgets.active.bg_stroke = Stroke::new(1.0, ACCENT);
    visuals.widgets.active.fg_stroke = Stroke::new(1.0, TEXT);
    visuals.widgets.open.bg_fill = Color32::TRANSPARENT;
    visuals.widgets.open.weak_bg_fill = Color32::TRANSPARENT;
    visuals.widgets.open.bg_stroke = Stroke::new(1.0, ACCENT);
    visuals.selection.bg_fill = ACTION;
    visuals.selection.stroke = Stroke::new(1.0, ACTION);
    visuals.hyperlink_color = ACCENT;
    visuals.text_cursor.stroke = Stroke::new(1.0, TEXT);
    ctx.set_visuals(visuals);
}

fn load_icon_texture(ctx: &Context, bytes: &[u8]) -> TextureHandle {
    let image = image::load_from_memory(bytes)
        .expect("flaxicon.png must be valid")
        .into_rgba8();
    let size = [image.width() as usize, image.height() as usize];
    let pixels = image.into_raw();
    ctx.load_texture(
        "flax-icon",
        ColorImage::from_rgba_unmultiplied(size, &pixels),
        egui::TextureOptions::LINEAR,
    )
}
