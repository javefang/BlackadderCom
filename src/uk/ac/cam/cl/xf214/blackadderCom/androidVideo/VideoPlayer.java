package uk.ac.cam.cl.xf214.blackadderCom.androidVideo;

import java.io.IOException;

import de.mjpegsample.MjpegDataInput;
import de.mjpegsample.MjpegView;
import de.mjpegsample.OnErrorListener;
import android.util.Log;
import uk.ac.cam.cl.xf214.blackadderCom.net.BARtpSubscriber;
import uk.ac.cam.cl.xf214.blackadderCom.net.StreamFinishedListener;

public class VideoPlayer implements OnErrorListener {
	public static final String TAG = "VideoPlayer";
	public static final int RESYNC_THRESHOLD = 10;	// player will resync when queue has 10 unhandled event
	
	private BARtpSubscriber mReceiver;
	private MjpegView mView;
	private StreamFinishedListener mStreamFinishedListener;
	private volatile boolean released;
	
	public VideoPlayer(BARtpSubscriber receiver,
			StreamFinishedListener sfLis) {
		mReceiver = receiver;
		mStreamFinishedListener = sfLis;
	}
	
	public BARtpSubscriber getReceiver() {
		return mReceiver;
	}
	
	public MjpegView setView(MjpegView view) {
		// releasing old view first
		MjpegView oldView = mView;
		
		if (oldView != null) {
			Log.i(TAG, "Unassign old view: " + oldView);
			//oldView.pausePlayback();	// TODO: print info when stop playback (change in MjpegView)
			mView = null;
			// pause data receiving (destroy all incoming pkts)
			try {
				mReceiver.setReceive(false);
			} catch (IOException e) {
				Log.e(TAG, e.getMessage());
			}
		}
		
		// assign new view
		mView = view;
		// start playback if the assigned view is not null
		if (mView != null) {
			Log.i(TAG, "Get view: " + mView);
			released = false;
			// create MJPEG input stream
			MjpegDataInput mjpegDataInput = new MjpegDataInput(mReceiver);
			
			// set source
			mView.setSource(mjpegDataInput);
			mView.setOnErrorListener(this);
			// resume data receiving (baPkt->Socket stream->MjpegInputStream->MJpegView)
			
			// settings
			view.showFps(true);
			view.setDisplayMode(MjpegView.SIZE_FULLSCREEN);
			
			// start playback
			view.startPlayback();
			try {
				mReceiver.setReceive(true);
			} catch (IOException e) {
				Log.e(TAG, e.getMessage());
			}
			Log.i(TAG, "startPlayback()");
		}
		
		return oldView;
	}
	
	public void release() {
		if (!released) {
			released = true;
			mReceiver.release();
		}
	}

	@Override
	public void onError() {
		mStreamFinishedListener.streamFinished(mReceiver.getRid());
		Log.i(TAG, "VideoPlayer released!");
	}
}
