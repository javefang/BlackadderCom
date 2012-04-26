package uk.ac.cam.cl.xf214.blackadderCom.androidVideo;

import java.io.IOException;

import uk.ac.cam.cl.xf214.blackadderCom.net.BAPacketSenderSocketAdapter;
import android.media.MediaRecorder;

public class AndroidVideoRecorder extends Thread {
	public static final String TAG = "AndroidVideoRecorder";
	public static final int VIDEO_WIDTH = 176;
	public static final int VIDEO_HEIGHT = 144;
	
	private BAPacketSenderSocketAdapter sender;
	private MediaRecorder mMediaRecorder;
	private boolean released;
	
	public AndroidVideoRecorder(BAPacketSenderSocketAdapter sender) {
		this.sender = sender;
		

	}
	
	@Override
	public void run() {
		released = false;
		
		mMediaRecorder = new MediaRecorder();
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mMediaRecorder.setOutputFile(sender.getFileDescriptor());
		mMediaRecorder.setVideoSize(VIDEO_WIDTH, VIDEO_HEIGHT);
		try {
			mMediaRecorder.prepare();
			mMediaRecorder.start();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public synchronized void release() {
		if (!released) {
			released = true;
			mMediaRecorder.reset();
			sender.release();
			mMediaRecorder.release();
		}
	}
}
