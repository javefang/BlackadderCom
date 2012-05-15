package cam.androidSpeex;

import java.nio.ByteBuffer;


	
public class NativeSpeexDecoder implements SpeexDef {
	public static final int DEFAULT_SPEEX_MODEID = SPEEX_MODEID_UWB;
	
	private long spxPtr;
	private long bits;
	private int frameSize;
	
	public NativeSpeexDecoder() {
		this(DEFAULT_SPEEX_MODEID);
	}
	
	public NativeSpeexDecoder(int mode) {
		bits = c_init_bits();
		spxPtr = c_init(mode);
		frameSize = getFrameSize();
	}
	
	public int getFrameSize() {
		return c_get(spxPtr, SPEEX_GET_FRAME_SIZE);
	}
	
	public void setEnhancerEnabled(boolean enabled) {
		c_set(spxPtr, SPEEX_SET_ENH, enabled ? 1 : 0);
	}
	
	public boolean isEnhancerEnabled() {
		return c_get(spxPtr, SPEEX_GET_ENH) == 1;
	}
	
	public short[] decode(ByteBuffer spxBuf, int frameCount) {
		return c_decode(spxPtr, bits, spxBuf, spxBuf.capacity(), frameSize, frameCount);
	}
	
	public void destroy() {
		c_destroy(spxPtr, bits);
	}

	private native long c_init_bits();
	private native long c_init(int mode);
	private native void c_set(long spxPtr, int field, int value);
	private native int c_get(long spxPtr, int field);
	private native int c_mode_query(int mode, int field);
	private native short[] c_decode(long spxPtr, long bits, ByteBuffer spxBuf, int dataLen, int frameSize, int frameCount);
	private native void c_destroy(long spxPtr, long bits);
}
