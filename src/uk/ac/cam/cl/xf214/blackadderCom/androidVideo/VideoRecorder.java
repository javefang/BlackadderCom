package uk.ac.cam.cl.xf214.blackadderCom.androidVideo;

import java.io.IOException;
import java.util.List;

import de.mjpegsample.MjpegDataOutput;
import de.mjpegsample.OnErrorListener;

import uk.ac.cam.cl.xf214.blackadderCom.net.BAPacketPublisher;
import android.graphics.ImageFormat;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public class VideoRecorder extends Thread {
	public static final String TAG = "VideoRecorder";
	public static final int CAM_ID = 0;
	public static final int FRAME_BUFFER_SIZE = 3;
	
	private Camera mCamera;
	private BAPacketPublisher mSender;
	private MjpegDataOutput mjpegDataOutput;
	private SurfaceView preview;
	private SurfaceHolder mSurfaceHolder;
	private Callback callback;
	private boolean released;
	
	// default video parameters
	private int mHeight;
	private int mWidth;
	private int mQuality;
	private int mFrameRate;
	
	public VideoRecorder(BAPacketPublisher sender, SurfaceView preview, int width, int height, int quality, int frameRate) throws IOException {
		this.mWidth = width;
		this.mHeight = height;
		this.mQuality = quality;
		this.mFrameRate = frameRate;
		this.preview = preview;
		this.mSender = sender;
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
		mCamera = Camera.open(CAM_ID);
		// use camera preview
		Camera.Parameters camParams = mCamera.getParameters();
		camParams.setPreviewFormat(ImageFormat.NV21);
		camParams.setPreviewSize(mWidth, mHeight);
		mCamera.setParameters(camParams);
		// print camera specification
		//printCameraSpec(camParams);
		
		Log.i(TAG, "Setting preview display...");
		try {
			mCamera.setPreviewDisplay(mSurfaceHolder);
		} catch (IOException e1) {
			Log.e(TAG, "ERROR: IOException when setting preview display!");
			return false;
		}
		
		// initialise buffer
		OnErrorListener onErrorListener = new OnErrorListener() {
			@Override
			public void onError() {
				release();
			}
		};
		mjpegDataOutput = new MjpegDataOutput(mSender, mWidth, mHeight, mQuality, FRAME_BUFFER_SIZE, mFrameRate, mCamera, onErrorListener);
		mjpegDataOutput.start();
		
		final YuvImage[] yuvBuffer = mjpegDataOutput.getYuvBuffer();
		for (YuvImage yuv : yuvBuffer) {
			mCamera.addCallbackBuffer(yuv.getYuvData());
		}
		
		mCamera.setPreviewCallbackWithBuffer(new PreviewCallback() {
			public void onPreviewFrame(byte[] data, Camera camera) {
				try {
					//Log.i(TAG, "onPreviewFrame() on " + System.currentTimeMillis() / 1000.0d + "s");
					mjpegDataOutput.addFrame(data);
				} catch (IOException e) {
					Log.e(TAG, "IOException caught: " + e.getMessage());
				}
			}
		});
		
		Log.i(TAG, "Starting camera preview, size=" + mWidth + "x" + mHeight + ", Q=" + mQuality + "...");
		mCamera.startPreview();
		Log.i(TAG, "Camera preview started!");
		return true;
	}
	
	/* change video encoding quality in on-the-fly */
	public void setVideoQuality(int quality) {
		mQuality = quality;
		if (mjpegDataOutput != null) {
			mjpegDataOutput.setVideoQuality(quality);
		}
	}
	
	public synchronized void release() {
		Log.i(TAG, "release()");
		if (released) {
			Log.i(TAG, "already released(), ignoring...");
			return;
		}
		
		released = true;
		releaseCamera();
		
		if (mjpegDataOutput != null) {
			mjpegDataOutput.release();	// sender is also released in wrapped method
		}
		
		if (mSurfaceHolder != null) {
			mSurfaceHolder.removeCallback(callback);
			mSurfaceHolder = null;
		}
		Log.i(TAG, "Video recorder released!");
	}
	
	public void printCameraSpec(Camera.Parameters camParams) {
		// print camera spec
		// 1. supported preview size
		StringBuffer strBuf = new StringBuffer("Supported preview size: ");
		for (Size s : camParams.getSupportedPreviewSizes()) {
			strBuf.append(s.width + "x" + s.height + "; ");
		}
		Log.i(TAG, strBuf.toString());
		// 2. supported preview format
		strBuf = new StringBuffer("Supported preview format: ");
		for (int format : camParams.getSupportedPictureFormats()) {
			String formatStr = "UNKNOWN";
			switch(format) {
			case ImageFormat.JPEG:
				formatStr = "JPEG";
				break;
			case ImageFormat.NV16:
				formatStr = "NV16";
				break;
			case ImageFormat.NV21:
				formatStr = "NV21";
				break;
			case ImageFormat.RGB_565:
				formatStr = "RGB_565";
				break;
			case ImageFormat.YUY2:
				formatStr = "YUV2";
				break;
			case ImageFormat.YV12:
				formatStr = "YV12";
				break;
			default:
				formatStr = "UNKNOWN";
			}
			strBuf.append(formatStr + "; ");
		}
		Log.i(TAG, strBuf.toString());
		// 3. supported preview fps range
		strBuf = new StringBuffer("Supported preview fps range: ");
		List<int[]> fpsRanges = camParams.getSupportedPreviewFpsRange();
		if (fpsRanges != null) {
			for (int[] fps : fpsRanges) {
				strBuf.append(fps[0] + "/" + fps[1] + "; ");
			}
		}
		Log.i(TAG, strBuf.toString());
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
