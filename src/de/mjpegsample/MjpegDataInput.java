package de.mjpegsample;

import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import uk.ac.cam.cl.xf214.blackadderCom.net.BARtpPacket;
import uk.ac.cam.cl.xf214.blackadderCom.net.BARtpReceiver;

public class MjpegDataInput {
	public static final String TAG = "MjpegDataInput";
	public static final long DEFAULT_FRAME_TIMEOUT = 1500;
	private BARtpReceiver mReceiver;
	
	public MjpegDataInput(BARtpReceiver receiver) {
		mReceiver = receiver;
	}
	
	public Bitmap readMjpegFrame() throws IOException, InterruptedException {
		// monitor receiver buffer queue size and skip frames if necessary here
		//frameBufferControl();
		BARtpPacket pkt = mReceiver.getBARtpPacket();
		int timeoutPktCount = 0;
		while (pkt == null) {	// || mReceiver.getCurrentTimestamp() - pkt.getTimestamp() > DEFAULT_FRAME_TIMEOUT
			pkt = mReceiver.getBARtpPacket();
			timeoutPktCount++;
		}
		if (timeoutPktCount > 0) {
			Log.e(TAG, "Timeout packet detected, skipping " + timeoutPktCount + " pkts");
		}
		/*
		if (pkt.getDataLength() != pkt.getData().length) {
			Log.e(TAG, "BARtpPacket data length mismatch " + pkt.getDataLength() + " / " + pkt.getData().length);
		}
		*/
		return BitmapFactory.decodeByteArray(pkt.getData(), 0, pkt.getDataLength());
	}
	
	public void frameBufferControl() {
		int queueSize = mReceiver.getQueueSize();
		if (queueSize > 50) {
			Log.i(TAG, "Skipping 10 frames");
			mReceiver.skipPacket(10);
		} else if (queueSize > 30) {
			Log.i(TAG, "Skipping 5 frames");
			mReceiver.skipPacket(5);
		} else if (queueSize > 10) {
			Log.i(TAG, "Skipping 3 frame");
			mReceiver.skipPacket(3);
		}
	}
}
