package uk.ac.cam.cl.xf214.blackadderCom.net;

import java.nio.ByteBuffer;

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
	public static final int PAYLOAD_SIZE = 1400;
	public static final int HEADER_SIZE = 14; // (4 bytes granule, 2 bytes seq, 8 bytes timestamp)
	public static final int PKT_SIZE = HEADER_SIZE + PAYLOAD_SIZE;
	
	private BAPacketSender mSender;
	private boolean released;
	
	private byte[] curGranuleBytes;
	private byte[] curTimestampBytes;
	
	public BARtpSender(BAPacketSender sender) {
		mSender = sender;
	}
	
	public void send(byte[] data, int granule, long timestamp) {
		if (released) {
			return;
		}
		
		curGranuleBytes = ByteHelper.getBytes(granule);
		curTimestampBytes = ByteHelper.getBytes(timestamp);
		
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
	
	public void release() {
		if (!released) {
			released = true;
			mSender.release();
		}
	}
	
	private void sendPkt(byte[] data, int dataOff, int dataLen, short seq) {
		// WRITE HEADER
		ByteBuffer buf = ByteBuffer.allocateDirect(PKT_SIZE);
		buf.put(curGranuleBytes);			// write granule	(4 bytes)
		buf.put(ByteHelper.getBytes(seq));	// write seq		(2 bytes)
		buf.put(curTimestampBytes);			// write timestamp	(8 bytes)
		
		// ADD PAYLOAD
		buf.put(data, dataOff, dataLen);
		buf.flip();
		
		// SEND PKT
		mSender.sendDirect(buf);
	}
	
}
