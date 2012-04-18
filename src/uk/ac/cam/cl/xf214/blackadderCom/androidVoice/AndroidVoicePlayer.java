package uk.ac.cam.cl.xf214.blackadderCom.androidVoice;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

import uk.ac.cam.cl.xf214.blackadderCom.net.BAPacketReceiver;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAEvent;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAHelper;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.util.Log;

public class AndroidVoicePlayer extends Thread {
	public static final String TAG = "AndroidVoicePlayer";
	public static final int STREAM_TYPE = AudioManager.STREAM_MUSIC;
	public static final int SAMPLE_RATE = 44100;
	public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO;
	public static final int CHANNELS = 1;
	public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
	public static final int MODE = AudioTrack.MODE_STREAM;
	
	private BAPacketReceiver receiver;
	private BlockingQueue<BAEvent> recvQueue;
	private AudioTrack mAudioTrack;
	private volatile boolean finished;
	private int bufSize;
	
	private int hashId; // used for identifying player
	
	public AndroidVoicePlayer(BAPacketReceiver receiver, int bufSize) {
		this.receiver = receiver;
		this.recvQueue = receiver.getDataQueue();
		this.bufSize = bufSize;
		
		this.hashId = Arrays.hashCode(receiver.getRid());
	}
	
	public BAPacketReceiver getReceiver() {
		return receiver;
	}
	
	@Override
	public void run() {
		finished = false;
		int minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
		mAudioTrack = new AudioTrack(STREAM_TYPE, SAMPLE_RATE, CHANNEL_CONFIG, 
				AUDIO_FORMAT, Math.max(minBuffer, bufSize), MODE);
		
		mAudioTrack.play();
		BAEvent event = null;
		byte[] pktBuf;
		int i = 0;
		while (!finished) {
			try {
				//Log.i(TAG, "Waiting for pkt " + i + "...");
				event = recvQueue.take();
				pktBuf = event.getData(0, event.getDataLength());
				//Log.i(TAG, "Pkt " + i++ + " received, length=" + pktBuf.length);
				
				// check if terminated
				if (pktBuf.length == 0) {
					// stream terminated
					print("FIN_PKT received: terminating player " + BAHelper.byteToHex(receiver.getRid()));
					event.freeNativeBuffer();
					finish();
					break;
				}
				writeFully(pktBuf, 0, pktBuf.length);
				event.freeNativeBuffer();
			} catch (InterruptedException e) {
				print("InterruptedException caught");
				break;
			}
		}
		mAudioTrack.stop();
		mAudioTrack.release();
		print("AudioTrack stopped and released");
		
		print("Voice player for stream " + BAHelper.byteToHex(receiver.getRid()) + " terminated!");
	}
	
	private void writeFully(byte[] data, int off, int length) {
		int read;
		while (!finished && length > 0 && mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
			read = mAudioTrack.write(data, off, length);
			length -= read;
			off += read;
		}
	}
	
	public void finish() {
		if (!finished) {
			finished = true;	// the player thread will not process the next event after current one being processed
			// if there is no event at the moment, player thread is blocked on recvQueue.take();
			interrupt();	// this ensure thread to exit waiting status (goes to exception)
			// stop receiving new events
			receiver.finish();
		}
	}
	
	public void print(String msg) {
		Log.i(TAG, "[Thread #" + getId() + "] " + msg);
	}

}
