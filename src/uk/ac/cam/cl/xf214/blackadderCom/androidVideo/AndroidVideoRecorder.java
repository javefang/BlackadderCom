package uk.ac.cam.cl.xf214.blackadderCom.androidVideo;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import uk.ac.cam.cl.xf214.blackadderCom.net.BAPacketSenderSocketAdapter;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAWrapperShared;
import android.media.MediaRecorder;
import android.util.Log;

public class AndroidVideoRecorder extends Thread {
	public static final String TAG = "AndroidVideoRecorder";
	
	private BAPacketSenderSocketAdapter sender;
	private MediaRecorder mMediaRecorder;
	
	public AndroidVideoRecorder(BAPacketSenderSocketAdapter sender) {
		this.sender = sender;
		
		/*
		mMediaRecorder = new MediaRecorder();
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mMediaRecorder.setOutputFile(sender.getFileDescriptor());
        */
	}
	
	@Override
	public void run() {
		byte[] dummyPayload = new byte[BAWrapperShared.DEFAULT_PKT_SIZE];
		Arrays.fill(dummyPayload, (byte)5);
		
		try {
			OutputStream out = sender.getOutputStream();
			for (int i = 0; i < 10; i++) {
				Log.i(TAG, "Writting dummy payload " + i);
				out.write(dummyPayload);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public synchronized void finish() {
		
	}
}
