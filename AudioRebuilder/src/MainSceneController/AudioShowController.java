package MainSceneController;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import ab.AudioFile;
import ab.AudioSelector;
import ab.MusicBtnEventHandler;
import ab.WavePainter;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;


public class AudioShowController implements Initializable{
	@FXML
	public ScrollPane CanvasHolder;
	@FXML
	public VBox ContentHolder;
	@FXML
	public Canvas WaveCanvas;
	@FXML
	public Canvas WaveCanvas2;
	@FXML
	public Canvas SpectrumCanvas;
	@FXML
	public Button PlayBtn;
	@FXML
	public Button PauseBtn;
	@FXML
	public Button StopBtn;
	@FXML
	public StackPane SpcetrumHolder;
	@FXML
	public Slider VolumeController;
	@FXML
	public CheckBox MuteBox;
	
	public WavePainter Painter;
	
	public static void println(Object line) { System.out.println(line.toString()); }
	public static void print(Object line) { System.out.print(line.toString() ); }
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1){
		AudioSelector.MainStage.maxWidthProperty().bind(WaveCanvas.widthProperty());
		MusicBtnEventHandler MusicEventListener = new MusicBtnEventHandler();
		MusicEventListener.SetPauseBtn(PauseBtn);
		MusicEventListener.GetListenBtns().add(StopBtn);
		Painter = new WavePainter(WaveCanvas, WaveCanvas2, SpectrumCanvas);
		Painter.AddMusicEventListener(MusicEventListener);
		Painter.SetMusicVolumeProperty(VolumeController.valueProperty());
		SpectrumCanvas.widthProperty().bind(ContentHolder.widthProperty());
	}
	public void OnSetMute() {
		Painter.MusicMute(MuteBox.isSelected());
	}
	
	public void OnWaveCanvasStepUP() {
		Painter.SetWaveCanvasWidth("UP");
	}
	
	public void OnWaveCanvasStepDOWN() {
		Painter.SetWaveCanvasWidth("DOWN");
	}
	
	public void OnMusicStart() {
		if(Painter.GetAudioFile()!=null) {
			Painter.MusicStart();
		}
	}
	
	public void OnMusicPauseOrContinue() {
		if(Painter.GetAudioFile()!=null) {
			if(Painter.isPlaying()) {
				PauseBtn.setText("> Continue");
				Painter.MusicPause();
			}else {
				PauseBtn.setText("¡ü   Pause");
				Painter.MusicContinue();
			}
		}
	}
	
	public void OnMusicStop(){
		if(Painter.GetAudioFile()!=null) {
			Painter.MusicStop();
		}
	}
	
	public void OnOpenFile() {
		if(Painter.GetAudioFile()!=null) {
			if(Painter.isPlaying()) {
				Painter.MusicPause();
			}
		}
		
		FileChooser FC = new FileChooser();
		ExtensionFilter mp3Filter = new ExtensionFilter("Mp3","*.mp3");
		ExtensionFilter wavFilter = new ExtensionFilter("Wav","*.wav");
		ExtensionFilter musicFilter = new ExtensionFilter("Music","*.mp3","*.wav");
		ExtensionFilter allFilter = new ExtensionFilter("All File","*.*");
		FC.getExtensionFilters().add(musicFilter);
		FC.getExtensionFilters().add(mp3Filter);
		FC.getExtensionFilters().add(wavFilter);
		FC.getExtensionFilters().add(allFilter);
		
		File f = FC.showOpenDialog(AudioSelector.MainStage);
		if (f!=null) {
			OpenAudioFile(f);
		}else {
			if(Painter.GetAudioFile()!=null) {
				if(Painter.isPausing()) {
					Painter.MusicContinue();
				}
			}
		}
	}
	
	private void OpenAudioFile(File f) {
		try {
        	if(f.exists()) {
        		AudioFile AF = new AudioFile(f);
        		Painter.SetAudioFile(AF);
        	}
        	else {
        		System.out.println("File not exist.");
        	}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
}
