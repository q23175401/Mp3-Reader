package ab;

import java.net.MalformedURLException;

import javafx.animation.Animation.Status;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class WavePainter {
	private double WAVE_CANVAS_WIDTH = 600;
	private Canvas RightWaveCanvas;
	private GraphicsContext rightGc;

	private Canvas LeftWaveCanvas;
	private GraphicsContext leftGc;

	private Canvas SpectrumCanvas;
	private GraphicsContext spectGc;

	private MusicStatusListener StatusLitener = new MusicStatusListener();
	private AudioFile curAudioFile = null;
	private float[] RightWaveData; // 畫畫用的Data
	private float[] LeftWaveData; // 畫畫用的Data

	private MediaPlayer MP;
	private int SPECTRUM_BAND_NUMBER = 256;
	private int AUDIOSPECTRUM_THRESHOLD = -100;

	private Timeline MusicImageConfig = new Timeline();
	private double SecondsPerPixel = 0;

	private double timeTrackerPositionX = -1;
	private double mouseChosenPositionX = -1;
	private double mouseCanvasX = -1;
	private double mouseChosenRegionEndX = -1;

	public void AddMusicEventListener(MusicBtnEventHandler ML) {
		StatusLitener.AddMusicEventListener(ML);
	}

	public WavePainter(Canvas rightWaveCanvas, Canvas leftWaveCanvas, Canvas spectrumCanvas) {
		RightWaveCanvas = rightWaveCanvas;
		rightGc = RightWaveCanvas.getGraphicsContext2D();
		LeftWaveCanvas = leftWaveCanvas;
		leftGc = leftWaveCanvas.getGraphicsContext2D();
		SpectrumCanvas = spectrumCanvas;
		spectGc = spectrumCanvas.getGraphicsContext2D();

		// 右聲道 初始化
		rightGc.clearRect(0, 0, RightWaveCanvas.getWidth(), RightWaveCanvas.getHeight());
		rightGc.setFill(Color.web("#252525"));
		rightGc.fillRect(0, 0, RightWaveCanvas.getWidth(), RightWaveCanvas.getHeight());
		rightGc.setStroke(Color.RED);
		rightGc.strokeLine(0, RightWaveCanvas.getHeight() / 2, RightWaveCanvas.getWidth(),
				RightWaveCanvas.getHeight() / 2);
		rightGc.setStroke(Color.ORANGE);
		// 左聲道 初始化
		leftGc.clearRect(0, 0, LeftWaveCanvas.getWidth(), LeftWaveCanvas.getHeight());
		leftGc.setFill(Color.web("#252525"));
		leftGc.fillRect(0, 0, LeftWaveCanvas.getWidth(), LeftWaveCanvas.getHeight());
		leftGc.setStroke(Color.RED);
		leftGc.strokeLine(0, LeftWaveCanvas.getHeight() / 2, LeftWaveCanvas.getWidth(), LeftWaveCanvas.getHeight() / 2);
		leftGc.setStroke(Color.ORANGE);

		RightWaveCanvas.setOnMousePressed(e -> {
			mouseChosenPositionX = e.getX();
			mouseChosenRegionEndX = -1;

			if (!isPlaying) {
				DrawWave();
			}
		});

		LeftWaveCanvas.setOnMousePressed(e -> {
			mouseChosenPositionX = e.getX();
			mouseChosenRegionEndX = -1;

			if (!isPlaying) {
				DrawWave();
			}
		});

		RightWaveCanvas.setOnMouseReleased(e -> {
			if (Math.abs(Math.abs(SecondsPerPixel * e.getX()) 				// (SecondsPerPixel * mouseChosenPositionX)
				- Math.abs(SecondsPerPixel * mouseChosenPositionX)) < 0.1) {// -(SecondsPerPixel * mouseChosenRegionEndX)>= 0.1
				mouseChosenRegionEndX = -1;
			} else if (e.getX() < mouseChosenPositionX) {
				mouseChosenRegionEndX = mouseChosenPositionX;
				mouseChosenPositionX = e.getX();

			} else if (e.getX() > mouseChosenPositionX) {
				mouseChosenRegionEndX = e.getX();
			}

			if (!isPlaying) {
				DrawWave();
			}
		});

		LeftWaveCanvas.setOnMouseReleased(e -> {
			if (Math.abs(Math.abs(SecondsPerPixel * e.getX()) 				// (SecondsPerPixel * mouseChosenPositionX)
				- Math.abs(SecondsPerPixel * mouseChosenPositionX)) < 0.1) {// -(SecondsPerPixel * mouseChosenRegionEndX)>= 0.1
				mouseChosenRegionEndX = -1;
			} else if (e.getX() < mouseChosenPositionX) {
				mouseChosenRegionEndX = mouseChosenPositionX;
				mouseChosenPositionX = e.getX();

			} else if (e.getX() > mouseChosenPositionX) {
				mouseChosenRegionEndX = e.getX();
			}

			if (!isPlaying) {
				DrawWave();
			}
		});

		RightWaveCanvas.setOnMouseExited(e -> {
			mouseCanvasX = -1;
			if (!isPlaying) {
				DrawWave();
			}
		});
		LeftWaveCanvas.setOnMouseExited(e -> {
			mouseCanvasX = -1;
			if (!isPlaying) {
				DrawWave();
			}
		});

		RightWaveCanvas.setOnMouseMoved(e -> {
			mouseCanvasX = e.getX();
			if (!isPlaying) {
				DrawWave();
			}
		});

		LeftWaveCanvas.setOnMouseMoved(e -> {
			mouseCanvasX = e.getX();
			if (!isPlaying) {
				DrawWave();
			}
		});

		// 頻譜圖初始化
		spectGc.clearRect(0, 0, SpectrumCanvas.getWidth(), SpectrumCanvas.getHeight());
		spectGc.setFill(Color.web("#252525"));
		spectGc.fillRect(0, 0, SpectrumCanvas.getWidth(), SpectrumCanvas.getHeight());
		spectGc.setStroke(Color.BLUE);

		MusicImageConfig.getKeyFrames().add(new KeyFrame( // 每0.1秒畫正在跑的畫面 ->當音樂開始時
				Duration.millis(100), (e) -> DrawPlayingWave()));
		MusicImageConfig.setCycleCount(Timeline.INDEFINITE); // 沒有限制TimeLine結束點

		SetDefault();
		ClearSpectrum();
		DrawWave();
	}

	private void DrawPlayingWave() {
		if (curAudioFile != null) {
			if (isPlaying) { // 確保不會亂開始
				// 先追蹤Time tracker的位子
				if (MP.getCurrentTime().greaterThanOrEqualTo(MP.getStopTime())) {
					System.out.println("到達音訊結尾" + MP.getCurrentTime().toSeconds());
					MusicStop();
				} else {
					timeTrackerPositionX = (MP.getCurrentTime().toSeconds() / SecondsPerPixel);
				}
				// 再畫圖
				DrawWave();
			}
		}
	}

	private boolean isPlaying = false;

	public boolean isPlaying() {
		return isPlaying;
	}

	public void MusicStart() {
		// MusicStop();
		MP.stop();
		// MP.seek(Duration.ZERO);

		StatusLitener.MusicStart();
		isPlaying = true;
		isPausing = false;
		MusicImageConfig.play();

		if (mouseChosenPositionX != -1) {
			MP.setStartTime(new Duration(SecondsPerPixel * mouseChosenPositionX * 1000));
			if (mouseChosenRegionEndX != -1) {
				MP.setStopTime(new Duration(SecondsPerPixel * mouseChosenRegionEndX * 1000));
			} else {
				MP.setStopTime(new Duration(curAudioFile.GetAudioTime() * 1000));
			}
		} else {
			MP.setStartTime(new Duration(0));
			MP.setStopTime(new Duration(curAudioFile.GetAudioTime() * 1000));
		}

		// MP.seek(MP.getStartTime());
		System.out.println("Start: " + MP.getStartTime().toSeconds());
		System.out.println("Current: " + MP.getCurrentTime().toSeconds());
		System.out.println("Stop: " + MP.getStopTime().toSeconds());

		double BugTime = 1.8;
		double Now = MP.getStartTime().toSeconds();
		if (0.5 <= Now && Now <= BugTime) {
			MP.setStartTime(MP.getStartTime().add(Duration.seconds((BugTime - Now) / BugTime * 1.3)));
		}

		MP.play(); // Play 會自動從 Start time 開始
	}

	public void MusicContinue() {
		isPausing = false;
		isPlaying = true;
		MP.setStartTime(new Duration(SecondsPerPixel * timeTrackerPositionX * 1000));

		double BugTime = 1.8;
		double Now = MP.getStartTime().toSeconds();
		if (0.5 <= Now && Now <= BugTime) {
			MP.setStartTime(MP.getStartTime().add(Duration.seconds((BugTime - Now) / BugTime * 1.3)));
		}

		MusicImageConfig.play();
		MP.play();
	}

	private boolean isPausing = false;

	public boolean isPausing() {
		return isPausing;
	}

	// private double pauseTimePositionX = -1;
	public void MusicPause() {
		isPausing = true;
		isPlaying = false;
		MusicImageConfig.pause();
		if (MP != null) {
			// pauseTimePositionX = timeTrackerPositionX;
			MP.stop();
			// MP.pause();
		}
	}

	public void MusicStop() {
		StatusLitener.MusicStop();
		isPlaying = false;
		isPausing = false;
		MusicImageConfig.stop();
		MP.stop();
		MP.seek(MP.getStartTime());
		timeTrackerPositionX = -1;
		ClearSpectrum();
		DrawWave();
	}

	boolean isMute = false;

	public void MusicMute(boolean m) {
		isMute = m;
		if (MP != null) {
			MP.setMute(m);
		}
	}

	public AudioFile GetAudioFile() {
		return curAudioFile;
	}

	private DoubleProperty MusicVolume = null;

	public void SetMusicVolumeProperty(DoubleProperty v) {
		MusicVolume = v;
	}

	public void SetAudioFile(AudioFile af) throws MalformedURLException {
		timeTrackerPositionX = -1;
		curAudioFile = af;

		Media m = new Media(curAudioFile.F.toURI().toString());
		if (MP != null) {
			MusicStop();
			MP = null;
		}

		MP = new MediaPlayer(m);
		MP.setAudioSpectrumInterval(0.1); // 每多少秒得到新的頻譜
		MP.setAudioSpectrumNumBands(SPECTRUM_BAND_NUMBER); // 得到256個 頻率分布圖
		MP.setAudioSpectrumThreshold(AUDIOSPECTRUM_THRESHOLD);// 跟最高的聲音差 (-120db ~ 0db) 最高的為0db
		MP.setMute(isMute);
		if (MusicVolume != null) {
			MP.volumeProperty().bind(MusicVolume);
		}
		// int FREQUENCY_UNIT = (int) ((double)22050 / SPECTRUM_BAND_NUMBER);
		MP.setAudioSpectrumListener(new AudioSpectrumListener() {
			@Override
			public void spectrumDataUpdate(double timestamp, double duration, float[] magnitudes, float[] phases) {
				if (isPlaying) {
					double LINEWIDTH_CONSTANT = 0.25;
					double lineDist = (SpectrumCanvas.getWidth() / SPECTRUM_BAND_NUMBER); // 計算每個Line的距離
					spectGc.setLineWidth(lineDist + LINEWIDTH_CONSTANT); // 根據每條線的距離改變線的寬度
					ClearSpectrum();

					double bandValue = 1;
					for (int f = 0; f < magnitudes.length; f++) {
						bandValue = (AUDIOSPECTRUM_THRESHOLD - magnitudes[f]) / (AUDIOSPECTRUM_THRESHOLD);

						// 畫上 SpectrumCanvas
						spectGc.strokeLine(f * lineDist, SpectrumCanvas.getHeight(), f * lineDist,
								SpectrumCanvas.getHeight() * (1 - bandValue));
					}
				}
			}

		});

		if (curAudioFile == null) {
			SetDefault();
		} else {

			RightWaveCanvas.setWidth(WAVE_CANVAS_WIDTH);
			LeftWaveCanvas.setWidth(WAVE_CANVAS_WIDTH);
			SecondsPerPixel = af.GetAudioTime() / WAVE_CANVAS_WIDTH; // 算出每一個Pixel佔了多少秒

			var rightWaveData = curAudioFile.GetWaveDataFitCanvas("Right", RightWaveCanvas.getWidth());
			var leftWaveData = curAudioFile.GetWaveDataFitCanvas("Left", LeftWaveCanvas.getWidth());
			SetWaveData(rightWaveData, leftWaveData);
		}

		// 把音樂初始化
		mouseChosenRegionEndX = -1;
		mouseChosenPositionX = -1;
		MusicStop();
	}

	private final double WAVE_CANVAS_WIDTH_MAX = 8000;
	private final double WAVE_CANVAS_WIDTH_MIN = 600;
	private double WAVE_CANVAS_WIDTH_STEP = 1000;

	public void SetWaveCanvasWidth(String UPorDown) {
		if (curAudioFile == null) {
			SetDefault();
		} else {
			if (UPorDown.equals("UP")) {
				WAVE_CANVAS_WIDTH = WAVE_CANVAS_WIDTH + WAVE_CANVAS_WIDTH_STEP >= WAVE_CANVAS_WIDTH_MAX
						? WAVE_CANVAS_WIDTH_MAX
						: WAVE_CANVAS_WIDTH + WAVE_CANVAS_WIDTH_STEP;
			} else if (UPorDown.equals("DOWN")) {
				WAVE_CANVAS_WIDTH = WAVE_CANVAS_WIDTH - WAVE_CANVAS_WIDTH_STEP <= WAVE_CANVAS_WIDTH_MIN
						? WAVE_CANVAS_WIDTH_MIN
						: WAVE_CANVAS_WIDTH - WAVE_CANVAS_WIDTH_STEP;
			}
			RightWaveCanvas.setWidth(WAVE_CANVAS_WIDTH);
			LeftWaveCanvas.setWidth(WAVE_CANVAS_WIDTH);
			if (mouseChosenPositionX != -1) {
				mouseChosenPositionX = SecondsPerPixel // 舊的時間
						* mouseChosenPositionX / (curAudioFile.GetAudioTime() / WAVE_CANVAS_WIDTH); // 轉換成新的時間
			}

			if (mouseChosenRegionEndX != -1) {
				mouseChosenRegionEndX = SecondsPerPixel // 舊的時間
						* mouseChosenRegionEndX / (curAudioFile.GetAudioTime() / WAVE_CANVAS_WIDTH); // 轉換成新的時間
			}

			SecondsPerPixel = curAudioFile.GetAudioTime() / WAVE_CANVAS_WIDTH; // 算出每一個Pixel佔了多少秒

			var rightWaveData = curAudioFile.GetWaveDataFitCanvas("Right", RightWaveCanvas.getWidth());
			var leftWaveData = curAudioFile.GetWaveDataFitCanvas("Left", LeftWaveCanvas.getWidth());
			SetWaveData(rightWaveData, leftWaveData);

			if (!isPlaying) {
				DrawWave();
			}
		}
	}

	private void SetDefault() {
		RightWaveData = new float[10000];
		LeftWaveData = new float[10000];

		for (int i = 0; i < RightWaveData.length; i++) {
			RightWaveData[i] = 0.01f;
			LeftWaveData[i] = 0.01f;
		}
	}

	public void SetWaveData(float[] rightWaveData, float[] leftWaveData) {
		if (rightWaveData != null)
			RightWaveData = rightWaveData;
		if (leftWaveData != null)
			LeftWaveData = leftWaveData;
	}

	private void ClearSpectrum() {
		spectGc.fillRect(0, 0, SpectrumCanvas.getWidth(), SpectrumCanvas.getHeight());
	}

	private void ClearCanvas() {
		rightGc.fillRect(0, 0, RightWaveCanvas.getWidth(), RightWaveCanvas.getHeight());

		leftGc.fillRect(0, 0, LeftWaveCanvas.getWidth(), LeftWaveCanvas.getHeight());
	}

	public void DrawWave() {
		ClearCanvas();

		var rightCanvasHeight = RightWaveCanvas.getHeight();
		var leftCanvasHeight = LeftWaveCanvas.getHeight();
		rightGc.setLineWidth(1.2);
		leftGc.setLineWidth(1.2);

		rightGc.setStroke(Color.ORANGE);
		leftGc.setStroke(Color.ORANGE);
		for (int i = 0; i < RightWaveData.length; i++) {
			double value = ((double) RightWaveData[i]) * rightCanvasHeight;
			int y1 = (int) ((rightCanvasHeight / 2 - value / 2));
			int y2 = (int) (y1 + value);
			rightGc.strokeLine(i, y1, i, y2);
		}

		for (int i = 0; i < LeftWaveData.length; i++) {
			double value = ((double) LeftWaveData[i]) * leftCanvasHeight;
			int y1 = (int) ((leftCanvasHeight / 2 - value / 2));
			int y2 = (int) (y1 + value);
			leftGc.strokeLine(i, y1, i, y2);
		}

		if (mouseCanvasX != -1) {
			rightGc.setStroke(Color.LIGHTGREEN);
			leftGc.setStroke(Color.LIGHTGREEN);
			rightGc.strokeLine(mouseCanvasX, 0, mouseCanvasX, rightCanvasHeight);
			leftGc.strokeLine(mouseCanvasX, 0, mouseCanvasX, leftCanvasHeight);
		}

		if (mouseChosenPositionX != -1) {
			rightGc.setStroke(Color.YELLOW);
			leftGc.setStroke(Color.YELLOW);
			rightGc.strokeLine(mouseChosenPositionX, 0, mouseChosenPositionX, rightCanvasHeight);
			leftGc.strokeLine(mouseChosenPositionX, 0, mouseChosenPositionX, leftCanvasHeight);
		}
		if (mouseChosenRegionEndX != -1) {
			for (int i = (int) (mouseChosenPositionX + 1); i < mouseChosenRegionEndX; i++) {
				leftGc.setStroke(Color.web("#06B9B0", 0.3));
				rightGc.setStroke(Color.web("#06B9B0", 0.3));
				rightGc.strokeLine(i, 0, i, RightWaveCanvas.getHeight());
				leftGc.strokeLine(i, 0, i, LeftWaveCanvas.getHeight());
			}
		}

		if (timeTrackerPositionX != -1) {
			leftGc.setStroke(Color.RED);
			rightGc.setStroke(Color.RED);
			rightGc.strokeLine(timeTrackerPositionX, 0, timeTrackerPositionX, RightWaveCanvas.getHeight());
			leftGc.strokeLine(timeTrackerPositionX, 0, timeTrackerPositionX, LeftWaveCanvas.getHeight());
			// System.out.println(timeTrackerPositionX);
		}
	}
}
