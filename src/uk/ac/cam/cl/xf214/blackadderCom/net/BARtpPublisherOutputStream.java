package uk.ac.cam.cl.xf214.blackadderCom.net;

import java.io.IOException;
import java.io.OutputStream;

import android.util.Log;

import uk.ac.cam.cl.xf214.blackadderWrapper.ByteHelper;

/* Plan
 * Implement a UDP style protocol to transmit video frames (stream style not fit with current usage)
 * Check OggStreamerExample for implementation reference
 * Each packet will have a header with
 * 		1. granule (int 32bit)
 * 		2. sequence (short 16bit)
 * 		3. timestamp (long 64bit)
 * 		#*. checksum (int 32bit) (not implemented)
 * 
 * TOTAL HEADER LENGTH = 16 bytes
 * 
 * Each frame is marked with a frame number, incrementing from 0 to Integer.MAX_VALUE
 * (can represent about 19884 hours streaming time with 30fps video) 
 * Each frame is transmitted using N fragment (Blackadder event), each RTP packet is marked by a sequence number
 * incrementing from 0 to Short.MAX_VALUE
 */

public class BARtpPublisherOutputStream extends OutputStream {
	public static final String TAG = "BARtpSenderOutputStream";
	public static final int PAYLOAD_SIZE = 1400;
	public static final int HEADER_SIZE = 14; // (4 bytes granule, 2 bytes seq, 8 bytes timestamp)
	public static final int PKT_SIZE = HEADER_SIZE + PAYLOAD_SIZE;
	public static final byte[] FIN_PKT = new byte[0];
	
	private BAPacketPublisher mSender;
	private byte[] mPktBuf;
	private int bufPtr = HEADER_SIZE;	// data starts from position HEADER_SIZE
	private short seq = 0;
	
	private boolean released;
	
	public BARtpPublisherOutputStream(BAPacketPublisher sender) {
		mSender = sender;
		mPktBuf = new byte[PKT_SIZE];
	}
	
	private void sendPkt(byte[] data, int length, short seq) {
		// WRITE HEADER (granule and timestamp already written)
		ByteHelper.getBytes(seq, mPktBuf, BARtpPacketHelper.SEQ_POS);
		// send pkt 
		mSender.send(mPktBuf, length);
		bufPtr = HEADER_SIZE;	// reset bufPtr
	}
	
	public void createNewRtpPkt(int granule, long timestamp) {
		//Log.i(TAG, "Creating RTP pkt granule=" + granule + ", timestamp=" + timestamp);
		ByteHelper.getBytes(granule, mPktBuf, BARtpPacketHelper.GRANULE_POS);
		ByteHelper.getBytes(timestamp, mPktBuf, BARtpPacketHelper.TIMESTAMP_POS);
		seq = 0;	// reset seq
	}

	@Override
	public void close() throws IOException {
		super.close();
		if (!released) {
			released = true;
			mSender.release();
		}
	}

	@Override
	public void flush() throws IOException {
		// DO NOTHING, manual flush only using forceSend()
		//Log.i(TAG, "OutputStream flush() " + System.currentTimeMillis());
	}
	
	public void forceSend() throws IOException {
		if (bufPtr > HEADER_SIZE) {
			//Log.i(TAG, "forceSendPkt(): size = " + bufPtr);
			// there is at least one byte in the payload
			sendPkt(mPktBuf, bufPtr, seq++);
		}
	}
	
	@Override
	public void write(int oneByte) throws IOException {
		mPktBuf[bufPtr++] = (byte)oneByte;
		if (bufPtr == PKT_SIZE) {
			sendPkt(mPktBuf, PKT_SIZE, seq++);
		}
	}

	@Override
	public void write(byte[] buffer, int offset, int count) throws IOException {
		int remain;
		while (count > 0) {
			remain = PKT_SIZE - bufPtr;
			if (count >= remain) {
				// fill remaining bytes in mPktBuf first and sendPkt()
				System.arraycopy(buffer, offset, mPktBuf, bufPtr, remain);
				bufPtr += remain;
				sendPkt(mPktBuf, PKT_SIZE, seq++);
				offset += remain;
				count -= remain;
			} else {
				// fill the mPktBuf with rest bytes from buffer 
				// (this section should be called only once and the is the last loop)
				System.arraycopy(buffer, offset, mPktBuf, bufPtr, count);
				bufPtr += count;
				offset += count;
				count -= count;
			}
		}
	}

	@Override
	public void write(byte[] buffer) throws IOException {
		this.write(buffer, 0, buffer.length);
	}
	
	/*
	public static void main(String[] args) {
		BARtpSenderOutputStream os = new BARtpSenderOutputStream(null);
		byte[] payloadA = new byte[5000];
		for (int i = 0; i < 5; i++) {
			Arrays.fill(payloadA, i*1000, (i+1)*1000, (byte)i);
		}
		int granuleA = 2878123;
		long timestampA = System.currentTimeMillis();
		
		byte[] sampleResultA = new byte[5000 + HEADER_SIZE * 4];
		for (int p = 0; p < sampleResultA.length; p += PKT_SIZE) {
			System.arraycopy(src, srcPos, dst, dstPos, length)
		}
		
		byte[] payloadB = new byte[5000];
		for (int i = 0; i < 5; i++) {
			Arrays.fill(payloadB, i*1000, (i+1)*1000, (byte)(5-i));
		}
		int granuleB = 12389981;
		long timestampB = System.currentTimeMillis();
	}*/
}

