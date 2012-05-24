package de.mjpegsample;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import uk.ac.cam.cl.xf214.blackadderWrapper.BAHelper;

import android.util.Log;

// STREAM FORMAT SPECIFICATION
// xx xx xx xx (header, content-length, int)
// FF D8 xx xx xx ... xx xx xx FF D9 (content, JPEG bytes)

/**
 * Stream input module for Mjpeg video stream
 * Deprecated, use MjpegDataInput instead
 * @author jave
 *
 */

@Deprecated
public class MjpegInputStream extends DataInputStream {
	public static final String TAG = "MjpegInputStream";
	
    private final byte[] SOI_MARKER = { (byte) 0xFF, (byte) 0xD8 };
    private final byte[] EOF_MARKER = { (byte) 0xFF, (byte) 0xD9 };
    //private final String CONTENT_LENGTH = "Content-Length";
    private final static int HEADER_MAX_LENGTH = 100;
    private final static int FRAME_MAX_LENGTH = 160000 + HEADER_MAX_LENGTH;
    private int mContentLength = -1;

    
    
    public MjpegInputStream(InputStream in) { 
    	super(new BufferedInputStream(in, FRAME_MAX_LENGTH));
    	
    }

    // return content byte count (EOF inclusive)
    private int getEndOfSeqeunce(DataInputStream in, byte[] sequence) throws IOException {
        int seqIndex = 0;
        byte c;
        for(int i=0; i < FRAME_MAX_LENGTH; i++) {
            c = (byte) in.readUnsignedByte();
            if(c == sequence[seqIndex]) {
                seqIndex++;
                if(seqIndex == sequence.length) return i + 1;
            } else seqIndex = 0;
        }
        return -1;
    }

    // return header byte count (SOI exclusive)
    private int getStartOfSequence(DataInputStream in, byte[] sequence) throws IOException {
        int end = getEndOfSeqeunce(in, sequence);
        return (end < 0) ? (-1) : (end - sequence.length);
    }

    private int parseContentLength(byte[] headerBytes) throws NumberFormatException {
        return Integer.parseInt(BAHelper.byteToText(headerBytes));
    }
    
    public void skipMjpegFrame(int frameToSkip) throws IOException {
    	Log.i(TAG, "Skipping " + frameToSkip + " frame(s)...");
    	while (frameToSkip > 0) {
	    	mark(FRAME_MAX_LENGTH);
	    	int headerLen = getStartOfSequence(this, SOI_MARKER);
	    	reset();
	    	byte[] header = new byte[headerLen];
	        readFully(header);
	        //Log.i(TAG, "reading content length...");
	        try {
	            mContentLength = parseContentLength(header);
	        } catch (NumberFormatException nfe) { 
	        	// count the content length manually if the content-length in the header is broken
	        	Log.e(TAG, "readMjpegFrame(): NumberFormatException,, count content-length manually");
	            mContentLength = getEndOfSeqeunce(this, EOF_MARKER); 
	        }
	        reset();
	        skipBytes(headerLen + mContentLength);
	        frameToSkip--;
    	}
    }

    public byte[] readMjpegFrame() throws IOException {
    	//Log.i(TAG, "reading header...");
        mark(FRAME_MAX_LENGTH);
        int headerLen = getStartOfSequence(this, SOI_MARKER);
        reset();
        byte[] header = new byte[headerLen];
        readFully(header);
        //Log.i(TAG, "reading content length...");
        try {
            mContentLength = parseContentLength(header);
        } catch (NumberFormatException nfe) { 
        	// count the content length manually if the content-length in the header is broken
        	Log.e(TAG, "readMjpegFrame(): NumberFormatException,, count content-length manually");
            mContentLength = getEndOfSeqeunce(this, EOF_MARKER); 
        }
        reset();
        //Log.i(TAG, "reading content (skipping header)...");
        byte[] frameData = new byte[mContentLength];
        skipBytes(headerLen);
        //Log.i(TAG, "reading content...");
        readFully(frameData);
        //Log.i(TAG, "Generating bitmap...");
        return frameData;
    }
}