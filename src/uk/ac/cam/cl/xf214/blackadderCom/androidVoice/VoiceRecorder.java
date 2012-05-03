package uk.ac.cam.cl.xf214.blackadderCom.androidVoice;

import java.nio.ByteBuffer;

import org.xiph.speex.SpeexEncoder;

import uk.ac.cam.cl.xf214.blackadderCom.androidVoice.VoiceProxy.VoiceCodec;
import uk.ac.cam.cl.xf214.blackadderCom.net.BAPacketSender;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;
import android.util.Log;

public class VoiceRecorder extends Thread {
	public static final String TAG = "AndroidVoiceRecorder";
	
	/* AudioRecorder Settings */
	public static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
	public static final int SAMPLE_RATE = 22050;
	public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
	public static final int CHANNELS = 1;	 // mono
	public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
	
	
	public static final int TARGET_DELAY = 40;	// ms
	public static final int TARGET_BUFFER = (int)(SAMPLE_RATE * 2 * TARGET_DELAY / 1000.0d);
	//public static final int MIN_BUFFER_SIZE_BYTE = 8 * 1024;	// 8 KB
	
	/* Speex Settings */
	public static final int SPX_MODE = 1;	 // 1=WB
	public static final int SPX_QUALITY = 8;	// Q=8
	
	public static final byte[] FIN_PKT = new byte[0];
	
	private int pktSizeByte;
	private int speexFrameSize;
	private boolean released;
	private BAPacketSender sender;
	private AudioRecord mRec;
	
	private VoiceCodec codec = VoiceCodec.SPEEX;
	
	/* Speex Encoder Settings */
	private SpeexEncoder encoder;
	
	public VoiceRecorder(BAPacketSender sender, int pktSizeByte, VoiceCodec codec) {
		Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
		released = false;
		this.sender = sender;
		this.pktSizeByte = pktSizeByte;
		this.codec = codec;
		
		encoder = new SpeexEncoder();
		boolean success = encoder.init(SPX_MODE, SPX_QUALITY, SAMPLE_RATE, CHANNELS);
		speexFrameSize = encoder.getFrameSize();
		if (success) {
			Log.i(TAG, "Speex encoder initialized! Frame size = " + speexFrameSize);
		} else {
			Log.e(TAG, "ERROR: Failed to init Speex encoder!");
		}
	}

	@Override
	public void run() {
		Log.i(TAG, "Recorder started");
		// calculate buffer size
		int minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
		
		// initialise audio recorder and start recording
		mRec = new AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, 
				CHANNEL_CONFIG, AUDIO_FORMAT, 
				Math.max(minBuffer, TARGET_BUFFER));
		mRec.startRecording();
		Log.i(TAG, "Start recording...");
		switch(codec) {
		case PCM:
			recordPCM();
			break;
		case SPEEX:
			recordSpeex();
			break;
		}
		
		// stop audio record when finished
		mRec.stop();
		mRec.release();
		
		Log.i(TAG, "Recorder stopped");
		
		sender.send(FIN_PKT);	// send terminate mark
		sender.release();	// terminate the sender
		Log.i(TAG, "Recorder thread terminated!");
	}
	
	private void recordPCM() {
		Log.i(TAG, "Streaming in PCM format...");
		// packet buffer, size=MTU
		//byte[] pktBuf = new byte[pktSizeByte];
		ByteBuffer pktBuf;
		// start reading pkt continuously
		while (!released) {
			// fill the pktBuf
			pktBuf = ByteBuffer.allocateDirect(pktSizeByte);
			//readFully(pktBuf, pktSizeByte);
			readFully(pktBuf, pktSizeByte);
			sender.send(pktBuf);
		}
	}
	
	private void recordSpeex() {
		Log.i(TAG, "Streaming in Speex format...");
		short[] frame = new short[speexFrameSize];
		//ByteBuffer pktBuf = ByteBuffer.allocateDirect(pktSizeByte);
		
		while (!released) {
			// read frame from AudioRecorder
			readFully(frame, 0, speexFrameSize);
			
			// encode audio data
			boolean success = encoder.processData(frame, 0, speexFrameSize);
			
			if (!success) {
				Log.e(TAG, "SPEEX: encoder annot process data!");
			}
			
			// read encoded data
			int length = encoder.getProcessedDataByteSize();
			if (length > 0) {
				Log.i(TAG, "Encoded size is " + length + " bytes");
				byte[] encodedData = new byte[length];
				encoder.getProcessedData(encodedData, 0);
				sender.send(encodedData);
			} else {
				Log.i(TAG, "ERROR: encoded size is " + length + " byte!");
			}
		}
		
	}
	
	private void readFully(short[] data, int off, int length) {
		int read;
		while (length > 0 && mRec.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
			read = mRec.read(data, off, length);
			length -= read;
			off += read;
		}
	}
	
	private void readFully(ByteBuffer data, int length) {
		int read;
		while (length > 0 && mRec.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
			read = mRec.read(data, length);
			length -= read;
		}
		data.flip();
	}
	
	/* fill the byte[] with recorded audio data */
	private void readFully(byte[] data, int off, int length) {
		int read;
		while (length > 0 && mRec.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
			read = mRec.read(data, off, length);
			length -= read;
			off += read;
		}
	}
	
	/* stop recording */
	public void release() {
		if (!released) {
			released = true;
			interrupt();
			try {
				join(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (this.getState() != Thread.State.TERMINATED) {
				Log.e(TAG, "Failed to terminate recorder thread!");
			}
		}
	}
}
