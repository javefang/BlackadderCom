package de.mjpegsample;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.Log;
import uk.ac.cam.cl.xf214.blackadderCom.net.BAPacketSender;
import uk.ac.cam.cl.xf214.blackadderCom.net.BARtpSenderOutputStream;

public class MjpegDataOutput extends Thread {
	public static final String TAG = "MjpegDataOutput";

	private long mFrameStartTime;
	private long mTimeRemain;
	
	private long mFrameInterval;
	
	//private BARtpSender mSender;
	private int mWidth;
	private int mHeight;
	private int mQuality;
	private Rect mRect;
	private YuvImage[] mYuvBuffer;
	private HashMap<byte[], YuvImage> mYuvBufferIndex;
	private BARtpSenderOutputStream mJpegOutputStream;
	private Camera mCam;
	private OnErrorListener mOnErrorListener;
	
	private ArrayBlockingQueue<byte[]> encodeQueue;
	private boolean released = false;
	
	private int curGranule = 0;
	
	public MjpegDataOutput(BAPacketSender sender, int width, int height, int quality, int frameBufSize, int frameRate, Camera cam, OnErrorListener onErrorListener) {
		//mSender = sender;
		mWidth = width;
		mHeight = height;
		mQuality = quality;
		//mFrameRate = frameRate;
		mFrameInterval = (long)(1000 / (double)frameRate);
		mCam = cam;
		mOnErrorListener = onErrorListener;
		mRect = new Rect(0, 0, mWidth, mHeight);
		mYuvBufferIndex = new HashMap<byte[], YuvImage>();
		encodeQueue = new ArrayBlockingQueue<byte[]>(frameBufSize);
		int dataBufSize=(int)(mHeight * mWidth * (ImageFormat.getBitsPerPixel(ImageFormat.NV21)/8.0));
		Log.i(TAG, "JPEG output buffer size=" + dataBufSize + " bytes");
		
		mYuvBuffer = new YuvImage[frameBufSize];
		for (int i = 0; i < frameBufSize; i++) {
			byte[] data = new byte[dataBufSize];
			mYuvBuffer[i] = new YuvImage(data, ImageFormat.NV21, mWidth, mHeight, null);	// NV21
			mYuvBufferIndex.put(data, mYuvBuffer[i]);
		}
		
		mJpegOutputStream = new BARtpSenderOutputStream(sender);
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
				mOnErrorListener.onError();	// notify VideoRecorder to release
			}
		}
	}
	
	private void encodeFrame(byte[] data) throws IOException {
		mFrameStartTime = System.currentTimeMillis();	// record frame start time
		
		mJpegOutputStream.createNewRtpPkt(curGranule++, mFrameStartTime);
		mYuvBufferIndex.get(data).compressToJpeg(mRect, mQuality, mJpegOutputStream);
		mJpegOutputStream.forceSend();
		
		// sleep for the remaining of the frame interval before adding the buffer back to camera
		mTimeRemain = mFrameInterval - (System.currentTimeMillis() - mFrameStartTime);
		if (mTimeRemain > 0) {
			try {
				//Log.i(TAG, "Sleeping for " + mTimeRemain + "ms");
				Thread.sleep(mTimeRemain);
			} catch (InterruptedException e) {
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
				Log.e(TAG, "IOException caught when closing BARtpSenderOutputStream: " + e.getMessage());
			}
			interrupt();
		}
		
	}
}
