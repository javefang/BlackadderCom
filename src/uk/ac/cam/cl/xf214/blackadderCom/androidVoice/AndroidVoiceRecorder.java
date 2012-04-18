package uk.ac.cam.cl.xf214.blackadderCom.androidVoice;

import uk.ac.cam.cl.xf214.blackadderCom.net.BAPacketSender;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class AndroidVoiceRecorder extends Thread {
	public static final String TAG = "AndroidVoiceRecorder";
	public static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
	public static final int SAMPLE_RATE = 44100;
	public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
	public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
	public static final int MIN_BUFFER_SIZE_BYTE = 16 * 1024;	// 16 KB
	public static final byte[] FIN_PKT = new byte[0];
	
	private int pktSizeByte;
	private boolean finished;
	private BAPacketSender sender;
	private AudioRecord mRec;
	
	public AndroidVoiceRecorder(BAPacketSender sender, int pktSizeByte) {
		finished = false;
		this.sender = sender;
		this.pktSizeByte = pktSizeByte;
	}

	@Override
	public void run() {
		Log.i(TAG, "Recorder started");
		// calculate buffer size
		int minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
		
		// initialise audio recorder and start recording
		mRec = new AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, 
				CHANNEL_CONFIG, AUDIO_FORMAT, 
				Math.max(minBuffer, MIN_BUFFER_SIZE_BYTE));
		mRec.startRecording();
		Log.i(TAG, "Start recording...");
		
		// packet buffer, size=MTU
		byte[] pktBuf = new byte[pktSizeByte];
		// start reading pkt continuously
		while (!finished) {
			// fill the pktBuf
			readFully(pktBuf, 0, pktBuf.length);
			sender.send(pktBuf);
		}
		// stop audio record when finished
		mRec.stop();
		mRec.release();
		
		Log.i(TAG, "Recorder stopped");
		
		sender.send(FIN_PKT);	// send terminate mark
		sender.finish();	// terminate the sender
		Log.i(TAG, "Recorder thread terminated!");
	}
	
	/* fill the byte[] with recorded audio data */
	private void readFully(byte[] data, int off, int length) {
		int read;
		while (length > 0) {
			read = mRec.read(data, off, length);
			length -= read;
			off += read;
		}
	}
	
	/* stop recording */
	public void finish() {
		if (!finished) {
			finished = true;
			interrupt();
			try {
				join(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (this.getState() != Thread.State.TERMINATED) {
				Log.e(TAG, "Failed to terminate recorder thread!");
			}
		}
	}
}
