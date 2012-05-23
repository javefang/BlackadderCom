package de.mjpegsample;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.Log;
import uk.ac.cam.cl.xf214.blackadderCom.net.BARtpSender;

public class MjpegDataOutput extends Thread {
	public static final String TAG = "MjpegDataOutput";

	public static final int PREVIEW_FPS = 15;
	public static final long FRAME_INTERVAL = (int)(1000 / (double)PREVIEW_FPS);
	private long mFrameStartTime;
	private long mTimeRemain;
	
	private BARtpSender mSender;
	private int mWidth;
	private int mHeight;
	private int mQuality;
	private Rect mRect;
	private YuvImage[] mYuvBuffer;
	private HashMap<byte[], YuvImage> mYuvBufferIndex;
	private ByteArrayOutputStream mJpegOutputStream;
	private Camera mCam;
	private OnErrorListener mOnErrorListener;
	
	private ArrayBlockingQueue<byte[]> encodeQueue;
	private boolean released = false;
	
	private int curGranule = 0;
	private long curTimestamp = 0;	// TODO: timestamp not used for the moment (always 0)
	
	public MjpegDataOutput(BARtpSender sender, int width, int height, int quality, int format, int frameBufSize, Camera cam, OnErrorListener onErrorListener) {
		mSender = sender;
		mWidth = width;
		mHeight = height;
		mQuality = quality;
		mRect = new Rect(0, 0, mWidth, mHeight);
		mCam = cam;
		mOnErrorListener = onErrorListener;
		mYuvBufferIndex = new HashMap<byte[], YuvImage>();
		
		encodeQueue = new ArrayBlockingQueue<byte[]>(frameBufSize);
		
		// initialize frame buffer
		/*
		if (format != ImageFormat.NV21) {
			Log.e(TAG, "Preview foramt is not NV21 : is " + format);
		}
		if (ImageFormat.getBitsPerPixel(format) != ImageFormat.getBitsPerPixel(ImageFormat.NV21)) {
			Log.e(TAG, "bits per pixel mismatch");
		}
		*/
		
		int dataBufSize=(int)(mHeight * mWidth * (ImageFormat.getBitsPerPixel(format)/8.0));
		Log.i(TAG, "JPEG output buffer size=" + dataBufSize + " bytes");
		
		mYuvBuffer = new YuvImage[frameBufSize];
		for (int i = 0; i < frameBufSize; i++) {
			byte[] data = new byte[dataBufSize];
			mYuvBuffer[i] = new YuvImage(data, format, mWidth, mHeight, null);	// NV21
			mYuvBufferIndex.put(data, mYuvBuffer[i]);
		}
		
		mJpegOutputStream = new ByteArrayOutputStream(dataBufSize);
	}
	
	public void run() {
		Log.i(TAG, "Async frame processing thread started!");
		byte[] frameData;
		while (!released) {
			synchronized(encodeQueue) {
				if (encodeQueue.size() == 0) {
					try {
						//Log.i(TAG, "Frame queue empty, go sleep...");
						encodeQueue.wait();
						if (encodeQueue.size() == 0) {
							continue;
						}
					} catch (InterruptedException e) {
						Log.e(TAG, "InterruptedException caught, exiting MjepgDataOutput...");
						break;
					}
				}
			}
			// notifyed, new incoming frame to encode
			frameData = encodeQueue.poll();
			try {
				encodeFrame(frameData);
			} catch (IOException e) {
				// notify VideoRecorder to release
				mOnErrorListener.onError();
			}
		}
	}
	
	/*
	@Deprecated
	public void addFrameFast(byte[] data, Camera camera) throws IOException {
		// TODO: remove the need of YuvImage by using native YUV420 -> JPEG convert code
		ByteBuffer buf = NativeJpegLib.encode(mWidth, mHeight, mQuality, data);
		mSender.sendDirect(buf, curGranule++, curTimestamp); // TODO: timestamp not used here
		NativeJpegLib.freeNativeBuffer(buf);	// data is copied when packetize
	    camera.addCallbackBuffer(data);
	}*/
	
	private void encodeFrame(byte[] data) throws IOException {
		mFrameStartTime = System.currentTimeMillis();	// record frame start time
		
		mJpegOutputStream.reset();
		mYuvBufferIndex.get(data).compressToJpeg(mRect, mQuality, mJpegOutputStream);
		//Log.i(TAG, "Compressed frame size = " + mJpegOutputStream.size() + " bytes");
		mSender.send(mJpegOutputStream.toByteArray(), curGranule++, curTimestamp);
		
		// sleep for the remaining of the frame interval before adding the buffer back to camera
		mTimeRemain = FRAME_INTERVAL - (System.currentTimeMillis() - mFrameStartTime);
		if (mTimeRemain > 0) {
			try {
				//Log.i(TAG, "Sleeping for " + mTimeRemain + "ms");
				Thread.sleep(mTimeRemain);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		mCam.addCallbackBuffer(data);
	}
	
	public void addFrame(byte[] data) throws IOException {
		if (released) {
			throw new IOException("Calling addFrame() after MjepgDataOutput is released!");
		}
		if (!encodeQueue.offer(data)) {
			// consumer only add buffer back after a frame is processed, should not be problem to offer data
			throw new IOException("Failed to offer frame to encode queue!");
		}
		synchronized(encodeQueue) {
			encodeQueue.notify();
		}
	}
	
	// deprecated due to GC related performance issue
	/*
	@Deprecated
	public void addFrame(byte[] yuvData, Camera camera) throws IOException {
		YuvImage yuvImage = new YuvImage(yuvData, ImageFormat.NV21, mWidth, mHeight, null);
		ByteArrayOutputStream jpegOutput = new ByteArrayOutputStream(yuvData.length);
		yuvImage.compressToJpeg(mRect, mQuality, jpegOutput);
		mSender.send(jpegOutput.toByteArray(), curGranule++, curTimestamp); // TODO: timestamp not used here
		jpegOutput.close();
	}
	*/
	
	public YuvImage[] getYuvBuffer() {
		return mYuvBuffer;
	}
	
	/* wrap the "release()" method of the BARtpSender */
	public void release() {
		if (!released) {
			released = true;
			try {
				mJpegOutputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mSender.release();
			interrupt();
		}
		
	}
}
