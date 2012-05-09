package cam.androidSpeex;

public interface SpeexDef {
	/** Set enhancement on/off (decoder only) */
	public static final int SPEEX_SET_ENH = 0;
	/** Get enhancement state (decoder only) */
	public static final int SPEEX_GET_ENH = 1;

	/*Would be SPEEX_SET_FRAME_SIZE, but it's (currently) invalid*/
	/** Obtain frame size used by encoder/decoder */
	public static final int SPEEX_GET_FRAME_SIZE = 3;

	/** Set quality value */
	public static final int SPEEX_SET_QUALITY = 4;
	/** Get current quality setting */
	/* #define SPEEX_GET_QUALITY 5 -- Doesn't make much sense, does it? */

	/** Set sub-mode to use */
	public static final int SPEEX_SET_MODE = 6;
	/** Get current sub-mode in use */
	public static final int SPEEX_GET_MODE = 7;

	/** Set low-band sub-mode to use (wideband only)*/
	public static final int SPEEX_SET_LOW_MODE = 8;
	/** Get current low-band mode in use (wideband only)*/
	public static final int SPEEX_GET_LOW_MODE = 9;

	/** Set high-band sub-mode to use (wideband only)*/
	public static final int SPEEX_SET_HIGH_MODE = 10;
	/** Get current high-band mode in use (wideband only)*/
	public static final int SPEEX_GET_HIGH_MODE = 11;

	/** Set VBR on (1) or off (0) */
	public static final int SPEEX_SET_VBR = 12;
	/** Get VBR status (1 for on, 0 for off) */
	public static final int SPEEX_GET_VBR = 13;

	/** Set quality value for VBR encoding (0-10) */
	public static final int SPEEX_SET_VBR_QUALITY = 14;
	/** Get current quality value for VBR encoding (0-10) */
	public static final int SPEEX_GET_VBR_QUALITY = 15;

	/** Set complexity of the encoder (0-10) */
	public static final int SPEEX_SET_COMPLEXITY = 16;
	/** Get current complexity of the encoder (0-10) */
	public static final int SPEEX_GET_COMPLEXITY = 17;

	/** Set bit-rate used by the encoder (or lower) */
	public static final int SPEEX_SET_BITRATE = 18;
	/** Get current bit-rate used by the encoder or decoder */
	public static final int SPEEX_GET_BITRATE = 19;

	/** Define a handler function for in-band Speex request*/
	public static final int SPEEX_SET_HANDLER = 20;

	/** Define a handler function for in-band user-defined request*/
	public static final int SPEEX_SET_USER_HANDLER = 22;

	/** Set sampling rate used in bit-rate computation */
	public static final int SPEEX_SET_SAMPLING_RATE = 24;
	/** Get sampling rate used in bit-rate computation */
	public static final int SPEEX_GET_SAMPLING_RATE = 25;

	/** Reset the encoder/decoder memories to zero*/
	public static final int SPEEX_RESET_STATE = 26;

	/** Get VBR info (mostly used internally) */
	public static final int SPEEX_GET_RELATIVE_QUALITY = 29;

	/** Set VAD status (1 for on, 0 for off) */
	public static final int SPEEX_SET_VAD = 30;

	/** Get VAD status (1 for on, 0 for off) */
	public static final int SPEEX_GET_VAD = 31;

	/** Set Average Bit-Rate (ABR) to n bits per seconds */
	public static final int SPEEX_SET_ABR = 32;
	/** Get Average Bit-Rate (ABR) setting (in bps) */
	public static final int SPEEX_GET_ABR = 33;

	/** Set DTX status (1 for on, 0 for off) */
	public static final int SPEEX_SET_DTX = 34;
	/** Get DTX status (1 for on, 0 for off) */
	public static final int SPEEX_GET_DTX = 35;

	/** Set submode encoding in each frame (1 for yes, 0 for no, setting to no breaks the standard) */
	public static final int SPEEX_SET_SUBMODE_ENCODING = 36;
	/** Get submode encoding in each frame */
	public static final int SPEEX_GET_SUBMODE_ENCODING = 37;

	/*#define SPEEX_SET_LOOKAHEAD 38*/
	/** Returns the lookahead used by Speex */
	public static final int SPEEX_GET_LOOKAHEAD = 39;

	/** Sets tuning for packet-loss concealment (expected loss rate) */
	public static final int SPEEX_SET_PLC_TUNING = 40;
	/** Gets tuning for PLC */
	public static final int SPEEX_GET_PLC_TUNING = 41;

	/** Sets the max bit-rate allowed in VBR mode */
	public static final int SPEEX_SET_VBR_MAX_BITRATE = 42;
	/** Gets the max bit-rate allowed in VBR mode */
	public static final int SPEEX_GET_VBR_MAX_BITRATE = 43;

	/** Turn on/off input/output high-pass filtering */
	public static final int SPEEX_SET_HIGHPASS = 44;
	/** Get status of input/output high-pass filtering */
	public static final int SPEEX_GET_HIGHPASS = 45;

	/** Get "activity level" of the last decoded frame, i.e.
	    how much damage we cause if we remove the frame */
	public static final int SPEEX_GET_ACTIVITY = 47;


	/* Preserving compatibility:*/
	/** Equivalent to SPEEX_SET_ENH */
	public static final int SPEEX_SET_PF = 0;
	/** Equivalent to SPEEX_GET_ENH */
	public static final int SPEEX_GET_PF = 1;




	/* Values allowed for mode queries */
	/** Query the frame size of a mode */
	public static final int SPEEX_MODE_FRAME_SIZE = 0;

	/** Query the size of an encoded frame for a particular sub-mode */
	public static final int SPEEX_SUBMODE_BITS_PER_FRAME = 1;



	/** Get major Speex version */
	public static final int SPEEX_LIB_GET_MAJOR_VERSION = 1;
	/** Get minor Speex version */
	public static final int SPEEX_LIB_GET_MINOR_VERSION = 3;
	/** Get micro Speex version */
	public static final int SPEEX_LIB_GET_MICRO_VERSION = 5;
	/** Get extra Speex version */
	public static final int SPEEX_LIB_GET_EXTRA_VERSION = 7;
	/** Get Speex version string */
	public static final int SPEEX_LIB_GET_VERSION_STRING = 9;

	/*#define SPEEX_LIB_SET_ALLOC_FUNC 10
	public static final int SPEEX_LIB_GET_ALLOC_FUNC = 11;
	public static final int SPEEX_LIB_SET_FREE_FUNC = 12;
	public static final int SPEEX_LIB_GET_FREE_FUNC = 13;

	public static final int SPEEX_LIB_SET_WARNING_FUNC = 14;
	public static final int SPEEX_LIB_GET_WARNING_FUNC = 15;
	public static final int SPEEX_LIB_SET_ERROR_FUNC = 16;
	public static final int SPEEX_LIB_GET_ERROR_FUNC = 17;
	*/

	/** Number of defined modes in Speex */
	public static final int SPEEX_NB_MODES = 3;

	/** modeID for the defined narrowband mode */
	public static final int SPEEX_MODEID_NB = 0;

	/** modeID for the defined wideband mode */
	public static final int SPEEX_MODEID_WB = 1;

	/** modeID for the defined ultra-wideband mode */
	public static final int SPEEX_MODEID_UWB = 2;
}
