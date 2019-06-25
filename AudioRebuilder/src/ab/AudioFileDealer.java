package ab;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFormat.Encoding;

import ws.schild.jave.AudioAttributes;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.EncoderProgressListener;
import ws.schild.jave.EncodingAttributes;
import ws.schild.jave.MultimediaInfo;
import ws.schild.jave.MultimediaObject;

public class AudioFileDealer {
	private ConvertProgressListener listener = new ConvertProgressListener();
	private static final CopyOption[] options = new CopyOption[]{ COPY_ATTRIBUTES , REPLACE_EXISTING };
	private Encoder encoder;
	
	public void ProcessAudioFile(AudioFile AF) throws UnsupportedAudioFileException, IOException, EncoderException {
		if(isMp3File(AF.F)) {
			DealMp3File(AF);
		}else if(isWavFile(AF.F)) {
			DealWavFile(AF, AF.F);
		}else {
			System.out.println("不能處理的檔案類型");
		}
	}

	private boolean isMp3File(File f) {
		String fileName = f.getName();
		int startIndex = fileName.lastIndexOf((int)'.')+1;
		String fileType = fileName.substring(startIndex, fileName.length());
		
		try {
			if(fileType.toLowerCase().equals("mp3")) {
				return true;
			}
		}catch(Exception e) {
			System.out.println("get format failed");
		}
		return false;
	}
	
	private boolean isWavFile(File f) {
		String fileName = f.getName();
		int startIndex = fileName.lastIndexOf((int)'.')+1;
		String fileType = fileName.substring(startIndex, fileName.length());

		try {
			if(fileType.toLowerCase().equals("wav")) {
				return true;
			}
		}catch(Exception e) {
			System.out.println("get format failed");
		}
		return false;
	}
	
	public void DealMp3File(AudioFile originAF) throws IOException , UnsupportedAudioFileException , EncoderException {

		//創造暫時的檔案
		File temporalDecodedFile = File.createTempFile("tem_decoded_Audio_File" , ".wav");
		File temporalCopiedFile = File.createTempFile("tem_original_Audio_File" , ".mp3");
		
		//程式關閉會自動刪除
		temporalDecodedFile.deleteOnExit();
		temporalCopiedFile.deleteOnExit();
		
		//把原始的檔案複製避免改到
		Files.copy(originAF.F.toPath(), temporalCopiedFile.toPath(), options);
		
		//Mp3 轉成 Wav
		transcodeToWav(temporalCopiedFile, temporalDecodedFile);
		
		//把Mp3的振幅拿出來
		DealWavFile(originAF, temporalDecodedFile);
		
		//做完事就把暫時的檔案刪掉
		temporalDecodedFile.delete();
		temporalCopiedFile.delete();

	}
	
	public void transcodeToWav(File sourceFile , File destinationFile) throws EncoderException {
		//Attributes atters = DefaultAttributes.WAV_PCM_S16LE_STEREO_44KHZ.getAttributes()
		try {
			//創造WAV的檔案Attribute
			AudioAttributes wavAttribute = new AudioAttributes();
			wavAttribute.setCodec("pcm_s16le");
			wavAttribute.setChannels(2);
			wavAttribute.setSamplingRate(44100);
			
			//根據這個Attribute創造Encoding的Attribute
			EncodingAttributes encodeingAttribute = new EncodingAttributes();
			encodeingAttribute.setFormat("wav");
			encodeingAttribute.setAudioAttributes(wavAttribute);
			
			//Encode
			encoder = encoder != null ? encoder : new Encoder();
			encoder.encode(new MultimediaObject(sourceFile), destinationFile, encodeingAttribute, listener);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void DealWavFile(AudioFile AF, File targetProcessFile) throws UnsupportedAudioFileException , IOException {
		//System.out.println("Calculting amplitudes");
		
		//Get Audio input stream
		try (AudioInputStream input = AudioSystem.getAudioInputStream(targetProcessFile)) {
			AudioFormat baseFormat = input.getFormat();
			AudioFormat audioFormat = getOutFormat(baseFormat);
			int totalBytes = input.available() / 2;
			
			long framesCount = input.getFrameLength(); //總共有幾個FRAME
			float AudioTIme = framesCount / audioFormat.getFrameRate();
			AF.SetAudioTime(AudioTIme);
			
			long dataLength = framesCount * (audioFormat.getSampleSizeInBits() / 8) * audioFormat.getChannels() ;
			System.out.println("檔案總大小: "+dataLength);
			
			//Get the PCM Decoded Audio Input Stream
			try (AudioInputStream pcmDecodedInput = AudioSystem.getAudioInputStream(audioFormat, input)) {
				final int BUFFER_SIZE = 4096 ;//this is actually bytes
				
				//Create a buffer
				byte[] buffer = new byte[BUFFER_SIZE];
				
				//Now get the average to a smaller array
				int maximumArrayLength = 200000;
				int[] rightSampledAmps = new int[maximumArrayLength];
				int[] leftSampledAmps = new int[maximumArrayLength];
				int ampsSampleAmount = totalBytes / maximumArrayLength;
				
				//Variables to calculate finalAmplitudes array
				int currentAmpSampleNumber = 0;
				int sampledAmpsI = 0;
				
				float curRightSumUpValue = 0.0f;
				float curLeftSumUpValue = 0.0f;
				
				//Variables for the loop
				float rightAmpValue = 0;
				float leftAmpValue = 0;
				int rightOneAmp = 0;
				int leftOneAmp = 0;
				
		        float TO_FIT_CANVAS_HEIGHT_CONSTANT = 2.9f;
				while (pcmDecodedInput.readNBytes(buffer, 0, BUFFER_SIZE) > 0) {
					for (int i = 0; i < buffer.length - 3; i += 4) {
						
						//Calculate the value
						leftOneAmp = (int) ( ( ( buffer[i + 1] << 8 ) | buffer[i] & 0xff ) << 16 );
						leftAmpValue =  ( ( (float) leftOneAmp / 32767 ) * TO_FIT_CANVAS_HEIGHT_CONSTANT );
						
						rightOneAmp = (int) ( ( ( buffer[i + 3] << 8 ) | buffer[i + 2] & 0xff ) << 16 );
						rightAmpValue = ( ( (float) rightOneAmp / 32767 ) * TO_FIT_CANVAS_HEIGHT_CONSTANT );
						
						//當還沒達到可以算平均的數量的時候
						if (currentAmpSampleNumber < ampsSampleAmount) {
							currentAmpSampleNumber++;
							curRightSumUpValue += Math.abs(rightAmpValue);
							curLeftSumUpValue  += Math.abs(leftAmpValue);
						} else { //算每個SAMPLE 的平均
							//不要超出ARRAY的長度
							if (sampledAmpsI != maximumArrayLength)
								rightSampledAmps[sampledAmpsI] = (int) (curRightSumUpValue / ampsSampleAmount);
								leftSampledAmps[sampledAmpsI]  = (int) (curLeftSumUpValue  / ampsSampleAmount);
							
							//數字們歸零
							currentAmpSampleNumber = 0;
							curRightSumUpValue = 0;
							curLeftSumUpValue = 0;
							sampledAmpsI += 1;
						}
					}
				}
				
				AF.SetSampledAmplitudes(new int[][] {rightSampledAmps, leftSampledAmps}, sampledAmpsI);
				return;
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		AF.SetSampledAmplitudes(new int[][] {new int[]{1}, new int[]{1}} ,1);
	}
	

	public float[] processAmplitudes(int[] sourcePcmData, int dataLength,double targetCanvasWidth) {
		//System.out.println("Processing "+ sourcePcmData.length + " amplitudes");
		
		int pixels = (int) targetCanvasWidth;//取得這個Canvas有幾個Pixel可以畫畫 -> 一個width = pixel

		float[] waveData = new float[pixels];
		
		//計算Canvas總共可以容納幾筆資料(Width)，總Pcm的數量去切分每一筆資料要包含多少PCMdata
		float samplesPerPixel = (float)dataLength / pixels;
		//Calculate
		float curSumUpValue=0;
		
		for (int w = 0; w < pixels; w++) {
			
			//計算每筆資料相對應的PCMdata的位子
			int c = (int)(w * samplesPerPixel);
			curSumUpValue = 0.0f;
			
			//每筆資料涵蓋的pixel值加總起來
			for (int s = 0; s < samplesPerPixel; s++) {
				curSumUpValue += ( Math.abs(sourcePcmData[c + s]) / 65536.0f );
			}
			//每筆data是這些pixel的振幅的平均
			waveData[w] = curSumUpValue / samplesPerPixel;
		}
		//System.out.println("Finished Processing amplitudes");
		return waveData;
	}

	
	private AudioFormat getOutFormat(AudioFormat inFormat) {
        final int ch = inFormat.getChannels();
        final float rate = inFormat.getSampleRate();
        
        return new AudioFormat(Encoding.PCM_SIGNED, rate, 16, ch, ch * 2, rate, false);
	}
	
	public class ConvertProgressListener implements EncoderProgressListener {
		int current = 1;
		
		public ConvertProgressListener() {
		}
		
		public void message(String m) {
		}
		
		public void progress(int p) {
			
			//double progress = p / 1000.00;
			//System.out.println(progress);
			
		}
		
		public void sourceInfo(MultimediaInfo m) {
		}
	}
	
}
