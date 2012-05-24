package de.mjpegsample;

import java.nio.ByteBuffer;

public class NativeJpegLib {
	public static ByteBuffer encode(int width, int height, int quality, byte[] yuv420data) {
		return c_compress_jpeg(width, height, quality, yuv420data);
	}
	
	public static ByteBuffer decode(int width, int height, byte[] jpegBuf) {
		// TODO: stub method for the momement
		return ByteBuffer.allocateDirect(10);
		//return c_decompress_jpeg(width, height, jpegBuf);
	}
	
	public static native ByteBuffer allocateNativeBuffer(int size);
	public static native void freeNativeBuffer(ByteBuffer buf);
	
	private static native ByteBuffer c_compress_jpeg(int width, int height, int quality, byte[] in);
	private static native ByteBuffer c_decompress_jpeg(int width, int height, byte[] in);
	
}
