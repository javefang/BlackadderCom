package uk.ac.cam.cl.xf214.blackadderCom.androidVideo;

import java.io.IOException;

import de.mjpegsample.MjpegInputStream;
import de.mjpegsample.MjpegView;
import de.mjpegsample.OnErrorListener;
import android.util.Log;
import uk.ac.cam.cl.xf214.blackadderCom.net.BAPacketReceiverSocketAdapter;
import uk.ac.cam.cl.xf214.blackadderCom.net.StreamFinishedListener;

public class VideoPlayer implements OnErrorListener {
	public static final String TAG = "AndroidVideoPlayer";
	public static final int RESYNC_THRESHOLD = 10;	// player will resync when queue has 10 unhandled event
	
	private BAPacketReceiverSocketAdapter mReceiver;
	private MjpegView mView;
	private StreamFinishedListener mStreamFinishedListener;
	private volatile boolean released;
	
	public VideoPlayer(BAPacketReceiverSocketAdapter receiver,
			StreamFinishedListener sfLis) {
		mReceiver = receiver;
		mStreamFinishedListener = sfLis;
	}
	
	public BAPacketReceiverSocketAdapter getReceiver() {
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
			mReceiver.setReceive(false);
		}
		
		// assign new view
		mView = view;
		// start playback if the assigned view is not null
		if (mView != null) {
			Log.i(TAG, "Get view: " + mView);
			released = false;
			// create MJPEG input stream
			MjpegInputStream mjpegInputStream;
			try {
				mjpegInputStream = new MjpegInputStream(mReceiver.getInputStream());
				mReceiver.setCloseable(mjpegInputStream);
			} catch (IOException e) {
				// TODO: need to add error handling here
				e.printStackTrace();
				// reverting setView() : use old view
				mView = oldView;
				return view;
			}
			
			// set source
			mView.setSource(mjpegInputStream);
			mView.setOnErrorListener(this);
			// resume data receiving (baPkt->Socket stream->MjpegInputStream->MJpegView)
			
			// settings
			view.showFps(true);
			view.setDisplayMode(MjpegView.SIZE_FULLSCREEN);
			
			// start playback
			view.startPlayback();
			mReceiver.setReceive(true);
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
