use eframe::egui::{self, CentralPanel, Context};
use poll_promise::Promise;
use std::{
    fs,
    io::{Read},
    sync::Arc,
};

#[derive(Default)]
struct Input {
    host_url: String,
    m3u8_path: String,
    destination: String,
}

impl Input {
    pub fn is_valid(&self) -> (bool, String) {
        if self.host_url.len() > 0 {
            return (false, String::from("Host URL is empty."));
        }
        if self.m3u8_path.len() == 0 {
            return (false, String::from("M3U8 path is empty."));
        }
        if self.destination.len() == 0 {
            return (false, String::from("Download destination is empty."));
        }
        (true, String::new())
    }
}

#[derive(Default)]
struct DownloadState {
    is_downloading: bool,
    progress: f32,
}

#[derive(Default)]
struct App {
    input: Input,
    download_state: DownloadState,
    context: Context,
    // promise: Option<Promise<()>>,
}

impl App {
    fn new(input: Input) -> Self {
        App {
            input,
            ..Default::default()
        }
    }

    fn download_m3u8(&mut self) -> (bool, String, Option<Promise<f32>>) {
        let (check_result, message) = self.input.is_valid();
        let mut promise = None;
        let ctx = self.context.clone();
        if check_result {
            self.input.destination.push_str("/out.mp4");
            let mut mp4 = fs::OpenOptions::new()
                .append(true)
                .open(&self.input.destination)
                .unwrap();
            let mut snippet_downloaded_count = 0;
            let m3u8_snippets = self.resolve_m3u8_file();
            let snippets_len = m3u8_snippets.len();
            let temp_promise = promise.get_or_insert_with(|| {
                let (sender, promise) = Promise::new();
                let rc_sender = Arc::new(&sender);
                for snippet in m3u8_snippets {
                    let temp_sender = Arc::clone(&rc_sender);
                    let url = format!("{0}/{1}", self.input.host_url, snippet);
                    let request = ehttp::Request::get(url);
                    // ehttp::fetch(request, |response| {
                    //     if let Ok(response) = response {
                    //         temp_sender.send(snippet_downloaded_count as f32 / snippets_len as f32);
                    //     }
                    // });
                }
                promise
            });
        }
        (check_result, message, promise)
    }

    fn resolve_m3u8_file(&self) -> Vec<String> {
        let mut m3u8_snippets = vec![];
        let mut file: Option<fs::File> = match fs::File::open(&self.input.m3u8_path) {
            Ok(file) => Some(file),
            Err(_) => None,
        };
        if let Some(file) = &mut file {
            let mut buf = String::new();
            file.read_to_string(&mut buf).unwrap();
            let mut i = 0;
            let lines: Vec<&str> = buf.split('\n').collect();
            while i < lines.len() {
                if i >= 6 && (i & 1) == 0 {
                    m3u8_snippets.push(String::from(lines[i]));
                }
                i += 1;
            }
        }
        m3u8_snippets
    }
}

impl eframe::App for App {
    fn update(&mut self, ctx: &egui::Context, _frame: &mut eframe::Frame) {
        self.context = ctx.clone();
        CentralPanel::default().show(&ctx, |ui| {
            ctx.set_visuals(egui::Visuals::light());

            let host_url_label = ui.label("Host URL:");
            ui.text_edit_singleline(&mut self.input.host_url)
                .labelled_by(host_url_label.id);

            ui.label("M3U8 File Path:");
            ui.horizontal(|ui| {
                if ui.button("Open file...").clicked() {
                    if let Some(path_buf) = rfd::FileDialog::new().pick_file() {
                        self.input.m3u8_path = path_buf.display().to_string();
                    }
                }
                if self.input.m3u8_path.len() > 0 {
                    ui.monospace(&self.input.m3u8_path);
                }
            });

            ui.label("Download Destination:");
            ui.horizontal(|ui| {
                if ui.button("Open directory...").clicked() {
                    if let Some(path_buf) = rfd::FileDialog::new().pick_folder() {
                        self.input.destination = path_buf.display().to_string();
                    }
                }
                if self.input.destination.len() > 0 {
                    ui.monospace(&self.input.destination);
                }
            });

            ui.horizontal(|ui| {
                if ui.button("Download").clicked() {
                    let (ok, message, promise) = self.download_m3u8();
                    if !ok {
                        CentralPanel::default().show(&ctx, |ui| ui.monospace(&message));
                    } else {
                        self.download_state.is_downloading = true;
                        if let Some(promise) = promise {
                            match promise.ready() {
                                Some(p) => self.download_state.progress = *p,
                                None => {}
                            }
                        }
                    }
                }
                if self.download_state.is_downloading {
                    ui.monospace(format!(
                        "Downloading: {0}%",
                        self.download_state.progress * 100.0
                    ));
                }
            });
        });
    }
}

fn main() {
    let input = Input::default();

    let options = eframe::NativeOptions {
        initial_window_size: Some(egui::vec2(600.0, 160.0)),
        ..Default::default()
    };

    eframe::run_native(
        "m3u8 downloader",
        options,
        Box::new(|_| Box::new(App::new(input))),
    );
}
