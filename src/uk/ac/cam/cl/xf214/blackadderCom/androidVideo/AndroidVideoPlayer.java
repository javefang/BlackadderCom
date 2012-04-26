package uk.ac.cam.cl.xf214.blackadderCom.androidVideo;

import android.media.MediaPlayer;
import android.util.Log;
import android.view.SurfaceView;
import uk.ac.cam.cl.xf214.blackadderCom.net.BAPacketReceiverSocketAdapter;

public class AndroidVideoPlayer extends Thread {
	public static final String TAG = "AndroidVideoPlayer";
	public static final int RESYNC_THRESHOLD = 10;	// player will resync when queue has 10 unhandled event
	
	private BAPacketReceiverSocketAdapter receiver;
	private MediaPlayer mPlayer;
	private SurfaceView view;
	private volatile boolean released;
	
	public AndroidVideoPlayer(BAPacketReceiverSocketAdapter receiver, SurfaceView view) {
		this.receiver = receiver;
		this.view = view;
		
	}
	
	public BAPacketReceiverSocketAdapter getReceiver() {
		return receiver;
	}
	
	@Override
	public void run() {
		released = false;
		mPlayer = new MediaPlayer();
		try {
			mPlayer.setDataSource(receiver.getFileDescriptor());
			mPlayer.setDisplay(view.getHolder());
			mPlayer.prepare();
		} catch (Exception e) {
			Log.e(TAG, "ERROR: Exception while initialising video player!");
			e.printStackTrace();
		}
		mPlayer.start();
	}
	
	public void release() {
		if (!released) {
			released = true;
			interrupt();
			if (mPlayer != null) {
				mPlayer.release();
			}
			receiver.release();
		}
	}
}
