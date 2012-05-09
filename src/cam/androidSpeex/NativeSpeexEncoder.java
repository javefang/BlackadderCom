package cam.androidSpeex;

import java.nio.ByteBuffer;

public class NativeSpeexEncoder implements SpeexDef {
	public static final int DEFAULT_SPEEX_MODEID = SPEEX_MODEID_UWB;
	
	private long spxPtr;
	private long bits;
	
	public NativeSpeexEncoder() {
		this(DEFAULT_SPEEX_MODEID);
	}
	
	public NativeSpeexEncoder(int mode) {
		bits = c_init_bits();
		spxPtr = c_init(mode);
	}
	
	public NativeSpeexEncoder(int mode, int quality) {
		spxPtr = c_init(mode);
		c_set(spxPtr, SPEEX_SET_QUALITY, quality);
	}
	
	public int getFrameSize() {
		return c_get(spxPtr, SPEEX_GET_FRAME_SIZE);
	}
	
	public ByteBuffer encode(short[] pcmBuf) {
		return c_encode(spxPtr, bits, pcmBuf, pcmBuf.length);
	}
	
	public void destroy() {
		c_destroy(spxPtr, bits);
	}

	private native long c_init_bits();
	private native long c_init(int mode);
	private native void c_set(long spxPtr, int field, int value);
	private native int c_get(long spxPtr, int field);
	private native ByteBuffer c_encode(long ptr, long bits, short[] pcmBuf, int length);
	private native void c_destroy(long spxPtr, long bits);
}
