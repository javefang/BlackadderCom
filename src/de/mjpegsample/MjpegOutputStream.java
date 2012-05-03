package de.mjpegsample;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import uk.ac.cam.cl.xf214.blackadderWrapper.BAHelper;

public class MjpegOutputStream extends DataOutputStream {
	private final byte[] SOI_MARKER = { (byte) 0xFF, (byte) 0xD8 };
    private final byte[] EOF_MARKER = { (byte) 0xFF, (byte) 0xD9 };
	
    private int mQuality;
    private int mWidth;
    private int mHeight;
    
	public MjpegOutputStream(OutputStream os, int width, int height, int quality) {
		super(new BufferedOutputStream(os));
		this.mWidth = width;
		this.mHeight = height;
		this.mQuality = quality;
	}
	
	public boolean addFrame(byte[] frameData) throws IOException {
		// prepare frame data
		ByteBuffer bmpBuf = ByteBuffer.allocateDirect(frameData.length);
		bmpBuf.put(frameData);
		bmpBuf.flip();
		
		// compress video frame to JPEG
		ByteBuffer jpegBuf = NativeJpegLib.encode(mWidth, mHeight, mQuality, bmpBuf);
		int dataLen = jpegBuf.capacity();
		
		// write result to stream
		// 1. content length
		write(BAHelper.textToByte(dataLen + ""));
		// 2. jpeg bytes (starts with SOI and ends with EOF)
		write(getData(jpegBuf, 0, dataLen));
		flush();
		return true;
	}
	
	private static byte[] getData(ByteBuffer buf, int offset, int length) {
		byte [] retval = new byte[length];
		buf.position(offset);
		buf.get(retval);
		buf.position(0);
		return retval;
	}
	
	
}
