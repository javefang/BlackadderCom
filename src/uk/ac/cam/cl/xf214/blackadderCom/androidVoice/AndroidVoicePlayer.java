package uk.ac.cam.cl.xf214.blackadderCom.androidVoice;

import java.io.StreamCorruptedException;
import java.util.concurrent.BlockingQueue;

import org.xiph.speex.SpeexDecoder;

import uk.ac.cam.cl.xf214.blackadderCom.androidVoice.AndroidVoiceProxy.VoiceCodec;
import uk.ac.cam.cl.xf214.blackadderCom.net.BAPacketReceiver;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAEvent;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAHelper;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Process;
import android.util.Log;

public class AndroidVoicePlayer extends Thread {
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
	private SpeexDecoder decoder;
	
	private VoiceCodec codec = VoiceCodec.PCM;
	
	//private int hashId; // used for identifying player
	
	public AndroidVoicePlayer(BAPacketReceiver receiver, VoiceCodec codec) {
		Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
		this.receiver = receiver;
		this.recvQueue = receiver.getDataQueue();
		this.codec = codec;
		
		
		//this.hashId = Arrays.hashCode(receiver.getRid());
		decoder = new SpeexDecoder();
		boolean success = decoder.init(SPX_MODE, SAMPLE_RATE, CHANNELS, SPX_ENHANCED);
		if (success) {
			Log.i(TAG, "Speex decoder initialized!");
		} else {
			Log.e(TAG, "ERROR: Failed to init Speex decoder!");
		}
	}
	
	public BAPacketReceiver getReceiver() {
		return receiver;
	}
	
	@Override
	public void run() {
		released = false;
		int minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
		mAudioTrack = new AudioTrack(STREAM_TYPE, SAMPLE_RATE, CHANNEL_CONFIG, 
				AUDIO_FORMAT, Math.max(minBuffer, TARGET_BUFFER), MODE);
		mAudioTrack.play();
		
		switch(codec) {
		case PCM:
			playbackPCM();
			break;
		case SPEEX:
			playbackSpeex();
			break;
		}
		
		mAudioTrack.stop();
		mAudioTrack.release();
		print("AudioTrack stopped and released");
		print("Voice player for stream " + BAHelper.byteToHex(receiver.getRid()) + " terminated!");
	}
	
	private void playbackPCM() {
		Log.i(TAG, "Playback in PCM format...");
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
	}
	
	private void playbackSpeex() {
		Log.i(TAG, "Playback in Speex format...");
		BAEvent event = null;
		byte[] pktBuf;
		//int i = 0;
		while (!released) {
			try {
				//Log.i(TAG, "Waiting for pkt " + i + "...");
				
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
				
				decoder.processData(pktBuf, 0, pktBuf.length);
				int length = decoder.getProcessedDataByteSize();
				Log.i(TAG, "Decoded size is " + length  + " bytes");
				
				byte[] decodedData = new byte[length];
				decoder.getProcessedData(decodedData, 0);
				
				writeFully(decodedData, 0, pktBuf.length);	// write data read from recorder to the buffer				
				event.freeNativeBuffer();
			} catch (InterruptedException e) {
				print("InterruptedException caught");
				break;
			} catch (StreamCorruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}		
	}
	
	private void writeFully(byte[] data, int off, int length) {
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
		}
	}
	
	public void print(String msg) {
		Log.i(TAG, "[Thread #" + getId() + "] " + msg);
	}

}
