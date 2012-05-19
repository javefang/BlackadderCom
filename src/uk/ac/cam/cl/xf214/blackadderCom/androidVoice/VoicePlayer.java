package uk.ac.cam.cl.xf214.blackadderCom.androidVoice;

import java.util.concurrent.BlockingQueue;

import cam.androidSpeex.NativeSpeexDecoder;

import uk.ac.cam.cl.xf214.blackadderCom.androidVoice.VoiceProxy.VoiceCodec;
import uk.ac.cam.cl.xf214.blackadderCom.net.BAPacketReceiver;
import uk.ac.cam.cl.xf214.blackadderCom.net.StreamFinishedListener;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAEvent;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAHelper;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class VoicePlayer extends Thread {
	public static final String TAG = "AndroidVoicePlayer";
	public static final int STREAM_TYPE = AudioManager.STREAM_MUSIC;
	public static final int SAMPLE_RATE = 22050;
	public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO;
	public static final int CHANNELS = 1;
	public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
	public static final int MODE = AudioTrack.MODE_STREAM;
	
	public static final int RESYNC_THRESHOLD = 10;	// player will resync when queue has 10 unhandled event
	public static final int TARGET_DELAY = 100;	// ms
	public static final int TARGET_BUFFER = (int)(SAMPLE_RATE * 2 * TARGET_DELAY / 1000.0d);

	/* Speex Settings */
	public static final int SPX_MODE = 1;	// 1=WB
	public static final boolean SPX_ENHANCED = true; // perceptual enhancement
	
	private BAPacketReceiver receiver;
	private BlockingQueue<BAEvent> recvQueue;
	private AudioTrack mAudioTrack;
	private volatile boolean released;
	private NativeSpeexDecoder decoder;
	private StreamFinishedListener mStreamFinishedListener;
	
	private VoiceCodec codec = VoiceCodec.PCM;
	private int sampleRate = SAMPLE_RATE;
	private int targetBuffer = TARGET_BUFFER;
	
	//private int hashId; // used for identifying player
	public VoicePlayer(BAPacketReceiver receiver, VoiceCodec codec, StreamFinishedListener streamFinishedListener, int sampleRate) {
		this(receiver, codec, streamFinishedListener);
		this.sampleRate = sampleRate;
		this.targetBuffer = (int)(sampleRate * 2 * TARGET_DELAY / 1000.0d);
	}
	
	@Deprecated
	public VoicePlayer(BAPacketReceiver receiver, VoiceCodec codec, StreamFinishedListener streamFinishedListener) {
		this.receiver = receiver;
		this.mStreamFinishedListener = streamFinishedListener;
		this.recvQueue = receiver.getDataQueue();
		this.codec = codec;
		
		
	}
	
	public BAPacketReceiver getReceiver() {
		return receiver;
	}
	
	@Override
	public void run() {
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		released = false;
		
		switch(codec) {
		case PCM:
			playbackPCM();
			break;
		case SPEEX:
			playbackSpeex();
			break;
		}
		print("Voice player for stream " + BAHelper.byteToHex(receiver.getRid()) + " terminated!");
	}
	
	private void playbackPCM() {
		int minBuffer = AudioTrack.getMinBufferSize(sampleRate, CHANNEL_CONFIG, AUDIO_FORMAT);
		mAudioTrack = new AudioTrack(STREAM_TYPE, sampleRate, CHANNEL_CONFIG, 
				AUDIO_FORMAT, Math.max(minBuffer, targetBuffer), MODE);
		mAudioTrack.play();
		Log.i(TAG, "Playback in PCM format (" + sampleRate + "Hz)...");
		BAEvent event = null;
		byte[] pktBuf;
		//int i = 0;
		while (!released) {
			try {
				//Log.i(TAG, "Waiting for pkt " + i + "...");
				if (recvQueue.size() > RESYNC_THRESHOLD) {
					// when queue size > 10, remove old event to keep audio synced
					Log.i(TAG, "Resync audio");
					receiver.drainDataQueue();
				}
				event = recvQueue.take();
				pktBuf = event.getDataCopy();
				//Log.i(TAG, "Pkt " + i++ + " received, length=" + pktBuf.length);
				// check if terminated
				if (pktBuf.length == 0) {
					// stream terminated
					print("FIN_PKT received: terminating player " + BAHelper.byteToHex(receiver.getRid()));
					event.freeNativeBuffer();
					release();
					break;
				}
				writeFully(pktBuf, 0, pktBuf.length);	// write data read from recorder to the buffer				
				event.freeNativeBuffer();
			} catch (InterruptedException e) {
				print("InterruptedException caught");
				break;
			}
		}
		mAudioTrack.stop();
		mAudioTrack.release();
		print("AudioTrack stopped and released");
	}
	
	private void playbackSpeex() {
		decoder = new NativeSpeexDecoder();
		Log.i(TAG, "Speex decoder initialized!");
		Log.i(TAG, "Playback in Speex format...");
		BAEvent event = null;
		final int frameBatchSize = 10;
		short[] frame;
		while (!released) {
			try {
				//Log.i(TAG, "Waiting for pkt " + i + "...");
				event = recvQueue.take();
				//Log.i(TAG, "Pkt " + i++ + " received, length=" + pktBuf.length);
				// check if terminated
				if (event.getDataLength() == 0) {
					// stream terminated
					print("FIN_PKT received: terminating player " + BAHelper.byteToHex(receiver.getRid()));
					event.freeNativeBuffer();
					release();
					break;
				}
				// TODO: maybe we can send a batch of frames (short[]) for encoding and return encoded bytes in batch
				// TODO: BROKEN, DON"T USE NOW!
				// TODO: need a way to split the event data into "frameBatchSize" chunks for decoding
				frame = decoder.decode(event.getDirectData(), frameBatchSize);
				writeFully(frame, 0, frame.length);	// write data read from recorder to the buffer				
				event.freeNativeBuffer();
			} catch (InterruptedException e) {
				print("InterruptedException caught");
				break;
			}
		}
		decoder.destroy();
	}
	
	private void writeFully(byte[] data, int off, int length) {
		int read;
		while (length > 0 && mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
			read = mAudioTrack.write(data, off, length);
			length -= read;
			off += read;
		}
	}
	
	private void writeFully(short[] data, int off, int length) {
		int read;
		while (length > 0 && mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
			read = mAudioTrack.write(data, off, length);
			length -= read;
			off += read;
		}
	}
	
	public void release() {
		if (!released) {
			released = true;	// the player thread will not process the next event after current one being processed
			// if there is no event at the moment, player thread is blocked on recvQueue.take();
			interrupt();	// this ensure thread to exit waiting status (goes to exception)
			// stop receiving new events
			receiver.release();
			mStreamFinishedListener.streamFinished(receiver.getRid());
		}
	}
	
	public void print(String msg) {
		Log.i(TAG, "[Thread #" + getId() + "] " + msg);
	}

}
