package uk.ac.cam.cl.xf214.blackadderCom.net;

import java.io.IOException;
import java.nio.ByteBuffer;

import de.mjpegsample.NativeJpegLib;

import android.util.Log;

import uk.ac.cam.cl.xf214.blackadderWrapper.BAHelper;
import uk.ac.cam.cl.xf214.blackadderWrapper.ByteHelper;

/* Plan
 * Implement a UDP style protocol to transmit video frames (stream style not fit with current usage)
 * Check OggStreamerExample for implementation reference
 * Each packet will have a header with
 * 		1. granule (int 32bit)
 * 		2. sequence (short 16bit)
 * 		3. timestamp (long 64bit)
 * 		4. checksum (int 32bit)
 * 
 * TOTAL HEADER LENGTH = 16 bytes
 * 
 * Each frame is marked with a frame number, incrementing from 0 to Integer.MAX_VALUE
 * (can represent about 19884 hours streaming time with 30fps video) 
 * Each frame is transmitted using N fragment (Blackadder event), each RTP packet is marked by a sequence number
 * incrementing from 0 to Short.MAX_VALUE
 */

public class BARtpSender {
	public static final String TAG = "BARtpSender";
	public static final int PAYLOAD_SIZE = 1400;
	public static final int HEADER_SIZE = 14; // (4 bytes granule, 2 bytes seq, 8 bytes timestamp)
	public static final int PKT_SIZE = HEADER_SIZE + PAYLOAD_SIZE;
	public static final byte[] FIN_PKT = new byte[0];
	
	private BAPacketSender mSender;
	private byte[] mPktBuf;
	
	private boolean released;
	
	public BARtpSender(BAPacketSender sender) {
		mSender = sender;
		mPktBuf = new byte[PKT_SIZE];
	}
	
	public void send(byte[] data, int granule, long timestamp) throws IOException {
		if (released) {
			throw new IOException("Calling send() after BARtpSender has been released!");
		}
		Log.i(TAG, "Sending payload:\n" + BAHelper.byteToHex(data));
		
		System.arraycopy(ByteHelper.getBytes(granule), 0, mPktBuf, BARtpPacketFragment.GRANULE_POS, 4);
		System.arraycopy(ByteHelper.getBytes(timestamp), 0, mPktBuf, BARtpPacketFragment.TIMESTAMP_POS, 8);
		
		int off = 0;
		int remain = data.length;
		short seq = 0;
		
		while (remain > 0) {
			if (remain > PAYLOAD_SIZE) {
				sendPkt(data, off, PAYLOAD_SIZE, seq);
				off += PAYLOAD_SIZE;
				remain -= PAYLOAD_SIZE;
			} else {
				// last pkt of this frame
				sendPkt(data, off, remain, seq);
				off += remain;
				remain -= remain;	// remain = 0
			}
			seq++;	// increse sequence number
		}
	}
	
	public void sendDirect(ByteBuffer data, int granule, long timestamp) throws IOException {
		if (released) {
			throw new IOException("Calling send() after BARtpSender has been released!");
		}
		
		System.arraycopy(ByteHelper.getBytes(granule), 0, mPktBuf, BARtpPacketFragment.GRANULE_POS, 4);
		System.arraycopy(ByteHelper.getBytes(timestamp), 0, mPktBuf, BARtpPacketFragment.TIMESTAMP_POS, 8);
		short seq = 0;
		int remain;
		
		while ((remain = data.remaining()) > 0) {
			if (remain >= PAYLOAD_SIZE) {
				sendPktDirect(data, PAYLOAD_SIZE, seq++);
			} else {
				sendPktDirect(data, remain, seq++);
			}
		}
	}
	
	public void release() {
		if (!released) {
			released = true;
			mSender.release();
			Log.i(TAG, "BARtpSender released (FIN_PKT sent)!");
		}
	}
	
	private void sendPktDirect(ByteBuffer data, int length, short seq) {
		data.limit(data.position() + length);	// limit the position of source data that can be read
		ByteBuffer buf = NativeJpegLib.allocateNativeBuffer(PKT_SIZE);
		
		// WRITE HEADER
		System.arraycopy(ByteHelper.getBytes(seq), 0, mPktBuf, BARtpPacketFragment.SEQ_POS, 2);	// write seq (2 bytes)
		buf.put(mPktBuf, 0, HEADER_SIZE);	// write header
		
		// ADD PAYLOAD
		buf.put(data);	// fill the pkt with remaining bytes in 'data'
		buf.flip();	// set limit to current position (if remaining bytes cannot fill the pkt)
		
		// SEND PKT
		mSender.sendDirect(buf, 0, buf.remaining());
		NativeJpegLib.freeNativeBuffer(buf);	// TODO: remove this line if the array copy code in native blackadder lib wrapper is remove
	}
	
	private void sendPkt(byte[] data, int off, int length, short seq) {
		// WRITE HEADER
		// granule and timestamp already written
		System.arraycopy(ByteHelper.getBytes(seq), 0, mPktBuf, BARtpPacketFragment.SEQ_POS, 2);	// write seq		(2 bytes)
		
		// ADD PAYLOAD
		System.arraycopy(data, 0, mPktBuf, BARtpPacketFragment.DATA_POS, length);
		
		// SEND PKT
		mSender.send(mPktBuf, HEADER_SIZE + length);
		Log.i(TAG, "Sending pkt " + seq + ", size=" + (HEADER_SIZE + length) + " bytes");
	}
	
}
