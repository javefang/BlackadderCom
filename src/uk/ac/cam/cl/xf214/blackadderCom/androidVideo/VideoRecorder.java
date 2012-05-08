package uk.ac.cam.cl.xf214.blackadderCom.androidVideo;

import java.io.IOException;

import de.mjpegsample.MjpegOutputStream;

import uk.ac.cam.cl.xf214.blackadderCom.net.BAPacketSenderSocketAdapter;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public class VideoRecorder extends Thread {
	public static final String TAG = "AndroidVideoRecorder";
	public static enum PreviewMode { GB, ICS };
	public static final int DEFAULT_VIDEO_WIDTH = 176;
	public static final int DEFAULT_VIDEO_HEIGHT = 144;
	
	/*
	private static PreviewMode previewMode;
	static {
		int sdk = Build.VERSION.SDK_INT;
		if (sdk < 11) {
			Log.i(TAG, "Using GB preview mode");
			previewMode = PreviewMode.GB;
		} else {
			Log.i(TAG, "Using ICS preview mode");
			previewMode = PreviewMode.ICS;
		}
	}
	*/
	
	private BAPacketSenderSocketAdapter sender;
	private Camera mCamera;
	private MjpegOutputStream mjpegOutputStream;
	private SurfaceView preview;
	private SurfaceHolder mSurfaceHolder;
	private Callback callback;
	private boolean released;
	
	// default video parameters
	private int mHeight = DEFAULT_VIDEO_HEIGHT;
	private int mWidth = DEFAULT_VIDEO_WIDTH;
	private int mQuality = 50;
	
	public VideoRecorder(BAPacketSenderSocketAdapter sender, SurfaceView preview) throws IOException {
		this.sender = sender;
		this.preview = preview;
		this.mjpegOutputStream = new MjpegOutputStream(sender.getOutputStream(), mWidth, mHeight, mQuality);
	}
	
	@Override
	public void run() {
		released = false;
		mSurfaceHolder = preview.getHolder();
		this.callback = new Callback() {
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
				Log.i(TAG, "surfaceChanged(), w=" + width + " h=" + height);
				mSurfaceHolder = holder;
				initPreview();
				
			}
			public void surfaceDestroyed(SurfaceHolder holder) {
				Log.i(TAG, "surfaceDestroyed()");
				mSurfaceHolder = null;
				release();
			}
			public void surfaceCreated(SurfaceHolder holder) {
				Log.i(TAG, "surfaceCreated()");
				mSurfaceHolder = holder;				
			}
		};
		SurfaceHolder holder = preview.getHolder();
		holder.addCallback(callback);
		Log.i(TAG, "Surface callback set!");
		
		initPreview();
	}
	
	private boolean initPreview() {
		mSurfaceHolder = preview.getHolder();
		if (mSurfaceHolder == null) {
			Log.i(TAG, "ERROR: cannot init video, surface is null!");
			return false;
		}
		
		if (mCamera != null) {
			releaseCamera();
		}
		
		Log.i(TAG, "Initialising camera preview...");
		mCamera = Camera.open();
		// use camera preview
		Camera.Parameters camParams = mCamera.getParameters();
		camParams.setPreviewFormat(ImageFormat.NV21);
		camParams.setPreviewFpsRange(15, 25);
		camParams.setPreviewSize(mWidth, mHeight);
		mCamera.setParameters(camParams);
		
		Log.i(TAG, "Setting preview display...");
		try {
			mCamera.setPreviewDisplay(mSurfaceHolder);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			Log.e(TAG, "ERROR: IOException when setting preview display!");
			e1.printStackTrace();
			return false;
		}
		mCamera.setPreviewCallback(new PreviewCallback() {
			public void onPreviewFrame(byte[] data, Camera camera) {
				try {
					//Log.i(TAG, "onPreviewFrame() @ " + System.currentTimeMillis());
					mjpegOutputStream.addFrame(data, data.length);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		Log.i(TAG, "Starting camera preview...");
		mCamera.startPreview();
		Log.i(TAG, "Camera preview started!");
		return true;
	}
	
	public synchronized void release() {
		Log.i(TAG, "release()");
		if (released) {
			Log.i(TAG, "already released(), ignoring...");
			return;
		}
		
		released = true;
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
			mSurfaceHolder = null;
		}
		Log.i(TAG, "Video recorder released!");
	}
	
	private void releaseCamera() {
		if (mCamera != null) {
			Log.i(TAG, "Releasing camera...");
			mCamera.stopPreview();
			mCamera.setPreviewCallback(null);
			mCamera.release();
			mCamera = null;
		}
	}
}
