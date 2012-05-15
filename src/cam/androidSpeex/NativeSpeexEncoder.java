package cam.androidSpeex;

import java.nio.ByteBuffer;

import android.util.Log;

public class NativeSpeexEncoder implements SpeexDef {
	public static final String TAG = "libspeex";
	public static final int DEFAULT_SPEEX_MODEID = SPEEX_MODEID_UWB;
	public static final int DEFAULT_SPEEX_QUALITY = 6;
	
	private int mode;
	private long spxPtr;
	private long bits;
	
	public NativeSpeexEncoder() {
		this(DEFAULT_SPEEX_MODEID);
	}
	
	public NativeSpeexEncoder(int mode) {
		this(mode, DEFAULT_SPEEX_QUALITY);
	}
	
	public NativeSpeexEncoder(int mode, int quality) {
		this.mode = mode;
		bits = c_init_bits();
		spxPtr = c_init(mode);
		Log.i(TAG, "SPX_PTR is " + spxPtr);
		c_set(spxPtr, SPEEX_SET_QUALITY, quality);
	}
	
	public int getFrameSize() {
		return c_get(spxPtr, SPEEX_GET_FRAME_SIZE);
	}
	
	public int getEncodedSize() {
		return c_get(spxPtr, SPEEX_SUBMODE_BITS_PER_FRAME);
	}
	
	public ByteBuffer encode(short[] pcmBuf, int frameCount) {
		return c_encode(spxPtr, bits, pcmBuf, frameCount);
	}
	
	public void destroy() {
		c_destroy(spxPtr, bits);
	}

	private native long c_init_bits();
	private native long c_init(int mode);
	private native void c_set(long spxPtr, int field, int value);
	private native int c_get(long spxPtr, int field);
	private native int c_mode_query(int mode, int field);
	private native ByteBuffer c_encode(long ptr, long bits, short[] pcmBuf, int frameCount);
	private native void c_destroy(long spxPtr, long bits);
}
