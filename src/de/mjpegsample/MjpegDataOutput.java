package de.mjpegsample;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.Log;
import uk.ac.cam.cl.xf214.blackadderCom.net.BARtpSender;

public class MjpegDataOutput {
	public static final String TAG = "MjpegDataOutput";
	private BARtpSender mSender;
	private int mWidth;
	private int mHeight;
	private int mQuality;
	private Rect mRect;
	private YuvImage[] mYuvBuffer;
	private HashMap<byte[], YuvImage> mYuvBufferIndex;
	private ByteArrayOutputStream mJpegOutputStream;
	
	private int curGranule = 0;
	private long curTimestamp = 0;	// TODO: timestamp not used for the moment (always 0)
	
	public MjpegDataOutput(BARtpSender sender, int width, int height, int quality, int format, int frameBufSize) {
		mSender = sender;
		mWidth = width;
		mHeight = height;
		mQuality = quality;
		mRect = new Rect(0, 0, mWidth, mHeight);
		mYuvBufferIndex = new HashMap<byte[], YuvImage>();
		
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
	
	@Deprecated
	public void addFrameFast(byte[] data, Camera camera) throws IOException {
		// TODO: remove the need of YuvImage by using native YUV420 -> JPEG convert code
		ByteBuffer buf = NativeJpegLib.encode(mWidth, mHeight, mQuality, data);
		mSender.sendDirect(buf, curGranule++, curTimestamp); // TODO: timestamp not used here
		NativeJpegLib.freeNativeBuffer(buf);	// data is copied when packetize
	    camera.addCallbackBuffer(data);
	}
	
	public void addFrameYuvBuf(byte[] data, Camera camera) throws IOException {
		mJpegOutputStream.reset();
		mYuvBufferIndex.get(data).compressToJpeg(mRect, mQuality, mJpegOutputStream);
		Log.i(TAG, "Compressed frame size = " + mJpegOutputStream.size() + " bytes");
		mSender.send(mJpegOutputStream.toByteArray(), curGranule++, curTimestamp);
		camera.addCallbackBuffer(data);
	}
	
	// deprecated due to GC related performance issue
	
	@Deprecated
	public void addFrame(byte[] yuvData, Camera camera) throws IOException {
		YuvImage yuvImage = new YuvImage(yuvData, ImageFormat.NV21, mWidth, mHeight, null);
		ByteArrayOutputStream jpegOutput = new ByteArrayOutputStream(yuvData.length);
		yuvImage.compressToJpeg(mRect, mQuality, jpegOutput);
		mSender.send(jpegOutput.toByteArray(), curGranule++, curTimestamp); // TODO: timestamp not used here
		jpegOutput.close();
	}
	
	
	public YuvImage[] getYuvBuffer() {
		return mYuvBuffer;
	}
	
	/* wrap the "release()" method of the BARtpSender */
	public void release() {
		try {
			mJpegOutputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mSender.release();
	}
}
