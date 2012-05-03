package uk.ac.cam.cl.xf214.blackadderCom.androidVideo;

import java.io.IOException;

import de.mjpegsample.MjpegOutputStream;


import uk.ac.cam.cl.xf214.blackadderCom.net.BAPacketSenderSocketAdapter;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public class VideoRecorder extends Thread {
	public static final String TAG = "AndroidVideoRecorder";
	public static final int VIDEO_WIDTH = 176;
	public static final int VIDEO_HEIGHT = 144;
	
	private BAPacketSenderSocketAdapter sender;
	private MediaRecorder mMediaRecorder;
	private Camera mCamera;
	private MjpegOutputStream mjpegOutputStream;
	private SurfaceView preview;
	private SurfaceHolder mSurfaceHolder;
	private Callback callback;
	private boolean released;
	private boolean recording;
	
	// default video parameters
	private int mHeight = 144;
	private int mWidth = 176;
	private int mQuality = 50;
	
	public VideoRecorder(BAPacketSenderSocketAdapter sender, SurfaceView preview) throws IOException {
		this.sender = sender;
		this.preview = preview;
		this.mjpegOutputStream = new MjpegOutputStream(sender.getOutputStream(), mWidth, mHeight, mQuality);
	}
	
	private boolean initVideo() throws IOException {
		recording = true;
		if (mSurfaceHolder == null) {
			Log.i(TAG, "ERROR: cannot init video, surface is null!");
			return false;
		}
		/*
		if (mMediaRecorder == null) {
			mMediaRecorder = new MediaRecorder();
		} else {
			mMediaRecorder.reset();
		}*/
		
		// release camera first if previously opened
		releaseCamera();
		
		// init camera
		mCamera = Camera.open();
		mCamera.unlock();
		Camera.Parameters camParams = mCamera.getParameters();
		camParams.setPreviewFormat(ImageFormat.YV12);
		camParams.setPreviewFpsRange(5, 20);
		camParams.setPreviewSize(mWidth, mHeight);
		mCamera.setParameters(camParams);
		mCamera.setPreviewDisplay(mSurfaceHolder);
		mCamera.setPreviewCallback(new PreviewCallback() {
			public void onPreviewFrame(byte[] data, Camera camera) {
				try {
					mjpegOutputStream.addFrame(data);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		
		// start capturing video frames (frame data will be push to MjpegOutputStream)
		mCamera.startPreview();
		
		/*
		mMediaRecorder.setCamera(mCamera);
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		mMediaRecorder.setOutputFile(sender.getFileDescriptor());
		mMediaRecorder.setVideoSize(VIDEO_WIDTH, VIDEO_HEIGHT);
		mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263);
		mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
		mMediaRecorder.prepare();
		mMediaRecorder.setOnErrorListener(new OnErrorListener() {
			@Override
			public void onError(MediaRecorder mr, int what, int extra) {
				release();
			}
		});
		Log.i(TAG, "Start recording!");
		mMediaRecorder.start();
		*/
		
		return true;
	}
	
	@Override
	public void run() {
		released = false;
		mSurfaceHolder = preview.getHolder();
		try {
			initVideo();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		this.callback = new Callback() {
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
				Log.i(TAG, "surfaceChanged()");
				try {
					if (!recording) initVideo();
				} catch (IOException e) {
					release();
					e.printStackTrace();
				}
			}
			public void surfaceDestroyed(SurfaceHolder holder) {
				Log.i(TAG, "surfaceDestroyed()");
				mSurfaceHolder = null;
			}
			public void surfaceCreated(SurfaceHolder holder) {
				Log.i(TAG, "surfaceCreated()");
				mSurfaceHolder = holder;
				
			}
		};
		SurfaceHolder holder = preview.getHolder();
		holder.addCallback(callback);
	}
	
	public synchronized void release() {
		if (!released) {
			released = true;
			
			/*
			if (mMediaRecorder != null) {
				recording = false;
				mMediaRecorder.stop();
				mMediaRecorder.reset();
				mMediaRecorder.release();
			}
			*/
			releaseCamera();
			try {
				mjpegOutputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			sender.release();
			
			if (mSurfaceHolder != null) {
				mSurfaceHolder.removeCallback(callback);
			}
			Log.i(TAG, "Video recorder released!");
		}
	}
	
	private void releaseCamera() {
		if (mCamera != null) {
			try {
				mCamera.stopPreview();
				mCamera.reconnect();
				mCamera.release();
				mCamera = null;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}