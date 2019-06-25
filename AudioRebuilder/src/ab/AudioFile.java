package ab;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import ws.schild.jave.EncoderException;

public class AudioFile {
	private int[][] SampledAmplitudes;
	private int SampleLength;
	private float AudioTime;
	private AudioFileDealer AD;
	public File F;
	
	public AudioFile(File f) throws UnsupportedAudioFileException, IOException, EncoderException{
		F = f;
		AD =new AudioFileDealer();
		AD.ProcessAudioFile(this);
	}

	public float[] GetWaveDataFitCanvas(String channel, double canvasWidth) {
		float [] WaveData = new float[]{ 0.1f, 0.1f};
		if(channel.equals("Right")) {
			WaveData = AD.processAmplitudes(SampledAmplitudes[0], SampleLength, canvasWidth);
		}else if(channel.equals("Left")) {
			WaveData = AD.processAmplitudes(SampledAmplitudes[1], SampleLength, canvasWidth);
		}else {
			System.out.println("不知道要哪一個聲道的資料");
		}
		
		return WaveData;
	}
	
	public void SetSampledAmplitudes(int [][] samples, int sampleLength) {
		SampledAmplitudes = samples;
		SampleLength = sampleLength;
	}

	public int[][] GetSampledAmplitudes() {
		return SampledAmplitudes;
	}
	
	public int GetSampleLength() {
		return SampleLength;
	}
	public void SetAudioTime(float time) {
		AudioTime = time;
	}
	
	public float GetAudioTime() {
		return AudioTime;
	}
}
