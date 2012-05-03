package uk.ac.cam.cl.xf214.blackadderCom.androidVideo;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import uk.ac.cam.cl.xf214.blackadderCom.net.BAPacketReceiverSocketAdapter;

public class VideoPlayer extends Thread {
	public static final String TAG = "AndroidVideoPlayer";
	public static final int RESYNC_THRESHOLD = 10;	// player will resync when queue has 10 unhandled event
	
	private BAPacketReceiverSocketAdapter receiver;
	private MediaPlayer mPlayer;
	private SurfaceView view;
	private Callback callback;
	private volatile boolean released;
	
	public VideoPlayer(BAPacketReceiverSocketAdapter receiver, SurfaceView view) {
		this.receiver = receiver;
		this.view = view;
		this.callback = new Callback() {
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
			public void surfaceDestroyed(SurfaceHolder holder) {}
			public void surfaceCreated(SurfaceHolder holder) {
				Log.i(TAG, "surfaceCreated()");
				mPlayer.setDisplay(holder);
				
				try {
					mPlayer.prepare();
					mPlayer.start();
					Log.i(TAG, "MediaPlayer started");
				} catch (Exception e) {
					e.printStackTrace();
					Log.e(TAG, "ERROR: Exception while preparing video player: " + e);
					release();
				}
				
			}
		};
		
	}
	
	public BAPacketReceiverSocketAdapter getReceiver() {
		return receiver;
	}
	
	@Override
	public void run() {
		released = false;
		mPlayer = new MediaPlayer();
		try {
			//mPlayer.setDataSource("rtsp://192.168.1.102:8086/");
			mPlayer.setDataSource(receiver.getFileDescriptor());
		} catch (Exception e) {
			Log.e(TAG, "ERROR: Exception while setting FD: " + e);
			e.printStackTrace();
			release();
			return;
		}
		view.getHolder().addCallback(callback);
		Log.i(TAG, "MediaPlayer data source set");
	}
	
	public void release() {
		if (!released) {
			view.getHolder().removeCallback(callback);
			released = true;
			interrupt();
			if (mPlayer != null) {
				mPlayer.release();
			}
			receiver.release();
		}
	}
}
