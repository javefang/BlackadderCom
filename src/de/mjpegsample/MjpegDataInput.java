package de.mjpegsample;

import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import uk.ac.cam.cl.xf214.blackadderCom.net.BARtpPacket;
import uk.ac.cam.cl.xf214.blackadderCom.net.BARtpReceiver;

public class MjpegDataInput {
	public static final String TAG = "MjpegDataInput";
	private BARtpReceiver mReceiver;
	
	public MjpegDataInput(BARtpReceiver receiver) {
		mReceiver = receiver;
	}
	
	public Bitmap readMjpegFrame() throws IOException, InterruptedException {
		// monitor receiver buffer queue size and skip frames if necessary here
		//frameBufferControl();
		BARtpPacket pkt = mReceiver.getBARtpPacket();
		while (pkt == null) {
			pkt = mReceiver.getBARtpPacket();
		}
		if (pkt.getDataLength() != pkt.getData().length) {
			Log.e(TAG, "BARtpPacket data length mismatch " + pkt.getDataLength() + " / " + pkt.getData().length);
		}
		return BitmapFactory.decodeByteArray(pkt.getData(), 0, pkt.getDataLength());
	}
	
	public void frameBufferControl() {
		int queueSize = mReceiver.getQueueSize();
		if (queueSize > 50) {
			mReceiver.skipPacket(10);
		} else if (queueSize > 30) {
			mReceiver.skipPacket(5);
		} else if (queueSize > 20) {
			mReceiver.skipPacket(1);
		}
	}
}
