package de.mjpegsample;

import java.nio.ByteBuffer;

public class NativeJpegLib {
	public static ByteBuffer encode(int width, int height, int quality, ByteBuffer bmpBuf) {
		return ByteBuffer.allocateDirect(10);
		//return c_compress_jpeg(width, height, quality, bmpBuf);
	}
	
	public static ByteBuffer decode(int width, int height, ByteBuffer jpegBuf) {
		return ByteBuffer.allocateDirect(10);
		//return c_decompress_jpeg(width, height, jpegBuf);
	}
	
	private static native ByteBuffer c_compress_jpeg(int width, int height, int quality, ByteBuffer in);
	private static native ByteBuffer c_decompress_jpeg(int width, int height, ByteBuffer out);
}
