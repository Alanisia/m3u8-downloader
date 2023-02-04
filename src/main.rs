use eframe::egui;

struct Input {
    host_url: String,
    m3u8_path: String,
    destination: String,
}

impl Input {
    pub fn new(host_url: String, m3u8_path: String, destination: String) -> Input {
        Input {
            host_url,
            m3u8_path,
            destination,
        }
    }
}

struct App {
    input: Input,
}

impl App {
    fn new(input: Input) -> Self {
        App { input }
    }

    fn download_m3u8(&self) {
        // reqwest::get(self.input.m3u8_path.clone())
        //     .await
        //     .unwrap()
        //     .bytes()
        //     .await
        //     .unwrap();
    }
}

impl eframe::App for App {
    fn update(&mut self, ctx: &egui::Context, _frame: &mut eframe::Frame) {
        egui::CentralPanel::default().show(&ctx, |ui| {
            ctx.set_visuals(egui::Visuals::light());

            let host_url_label = ui.label("Host URL:");
            ui.text_edit_singleline(&mut self.input.host_url)
                .labelled_by(host_url_label.id);

            let m3u8_path_label = ui.label("M3U8 File Path:");
            ui.text_edit_singleline(&mut self.input.m3u8_path)
                .labelled_by(m3u8_path_label.id);

            let destination_label = ui.label("Download Destination:");
            ui.text_edit_singleline(&mut self.input.destination)
                .labelled_by(destination_label.id);

            if ui.button("Download").clicked() {
                self.download_m3u8();
            }
        });
    }
}

fn main() {
    let input = Input::new(String::new(), String::new(), String::new());

    let options = eframe::NativeOptions {
        initial_window_size: Some(egui::vec2(300.0, 160.0)),
        ..Default::default()
    };

    eframe::run_native(
        "m3u8 downloader",
        options,
        Box::new(|_| Box::new(App::new(input))),
    );
}
