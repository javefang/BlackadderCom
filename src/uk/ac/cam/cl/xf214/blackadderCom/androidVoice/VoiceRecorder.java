package uk.ac.cam.cl.xf214.blackadderCom.androidVoice;

import cam.androidSpeex.NativeSpeexEncoder;

import uk.ac.cam.cl.xf214.blackadderCom.androidVoice.VoiceProxy.VoiceCodec;
import uk.ac.cam.cl.xf214.blackadderCom.net.BAPacketSender;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class VoiceRecorder extends Thread {
	public static final String TAG = "VoiceRecorder";
	
	/* AudioRecorder Settings */
	public static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
	public static final int SAMPLE_RATE = 22050;
	public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
	public static final int CHANNELS = 1;	 // mono
	public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
	
	
	public static final int TARGET_DELAY = 40;	// ms
	public static final int TARGET_BUFFER = (int)(SAMPLE_RATE * 2 * TARGET_DELAY / 1000.0d);
	//public static final int MIN_BUFFER_SIZE_BYTE = 8 * 1024;	// 8 KB
	
	private int pktSizeByte;
	private int speexFrameSize;
	private boolean released;
	private BAPacketSender sender;
	private AudioRecord mRec;
	
	private VoiceCodec codec = VoiceCodec.SPEEX;
	private int sampleRate = SAMPLE_RATE;
	private int targetBuffer = TARGET_BUFFER;
	
	/* Speex Encoder Settings */
	private NativeSpeexEncoder encoder;
	
	public VoiceRecorder(BAPacketSender sender, int pktSizeByte, VoiceCodec codec, int sampleRate) {
		this(sender, pktSizeByte, codec);
		this.sampleRate = sampleRate;
		this.targetBuffer = (int)(sampleRate * 2 * TARGET_DELAY / 1000.0d); 
	}
	
	@Deprecated
	public VoiceRecorder(BAPacketSender sender, int pktSizeByte, VoiceCodec codec) {
		released = false;
		this.sender = sender;
		this.pktSizeByte = pktSizeByte;
		this.codec = codec;
		
		
	}

	@Override
	public void run() {
		Log.i(TAG, "Recorder started");
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		
		Log.i(TAG, "Start recording (" + sampleRate + "Hz)...");
		switch(codec) {
		case PCM:
			recordPCM();
			break;
		case SPEEX:
			//recordSpeex();
			break;
		}
		
		sender.release();	// terminate the sender
		Log.i(TAG, "Recorder thread terminated!");
	}
	
	private void recordPCM() {
		// calculate buffer size
		int minBuffer = AudioRecord.getMinBufferSize(sampleRate, CHANNEL_CONFIG, AUDIO_FORMAT);
		// initialise audio recorder and start recording
		mRec = new AudioRecord(AUDIO_SOURCE, sampleRate, 
				CHANNEL_CONFIG, AUDIO_FORMAT, 
				Math.max(minBuffer, targetBuffer));
		mRec.startRecording();
		Log.i(TAG, "Streaming in PCM format...");
		byte[] pktBuf = new byte[pktSizeByte];
		// start reading pkt continuously
		while (!released) {
			// fill the pktBuf
			readFully(pktBuf, 0, pktSizeByte);
			sender.send(pktBuf, pktBuf.length);
		}
		// stop audio record when finished
		mRec.stop();
		mRec.release();
		
		Log.i(TAG, "Recorder stopped and released");
	}
	
	/*
	private void recordSpeex() {
		encoder = new NativeSpeexEncoder();
		speexFrameSize = encoder.getFrameSize();
		Log.i(TAG, "Speex encoder initialized! Frame size = " + speexFrameSize);
		
		Log.i(TAG, "Streaming in Speex format...");
		short[] frame = new short[speexFrameSize];
		//ByteBuffer pktBuf = ByteBuffer.allocateDirect(pktSizeByte);
		final int frameBatchSize = 10;
		
		while (!released) {
			ByteBuffer buf = ByteBuffer.allocateDirect(1400);
			
			while (!released) {
				// read frame from AudioRecorder
				readFully(frame, 0, speexFrameSize * frameBatchSize);
				buf.put(encoder.encode(frame, frameBatchSize));
			}
			sender.send(buf);
		}
		encoder.destroy();
		Log.i(TAG, "NativeSpeexEncoder released!");
	}
	
	
	private void readFully(short[] data, int off, int length) {
		int read;
		while (length > 0 && mRec.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
			read = mRec.read(data, off, length);
			length -= read;
			off += read;
		}
	}
	*/
	
	private void readFully(byte[] data, int off, int length) {
		int read;
		while (length > 0 && mRec.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
			read = mRec.read(data, off, length);
			length -= read;
			off += read;
		}
	}
	
	/*
	private void readFully(ByteBuffer data, int length) {
		int read;
		while (length > 0 && mRec.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
			read = mRec.read(data, length);
			length -= read;
		}
		data.flip();
	}
	*/
	
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
