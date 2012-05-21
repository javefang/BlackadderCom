package uk.ac.cam.cl.xf214.blackadderCom.net;

import java.nio.ByteBuffer;
import java.util.Vector;

public class BARtpPacket {
	private ByteBuffer mPayload;
	private int mGranule;
	private long mTimestamp;
	private int mDataLen;
	
	public BARtpPacket(int granule, long timestamp, Vector<BARtpPacketFragment> frags, int dataLen) {
		mPayload = ByteBuffer.allocate(dataLen);
		mGranule = granule;
		mTimestamp = timestamp;
		mDataLen = dataLen;
		
		for (BARtpPacketFragment frag : frags) {
			mPayload.put(frag.getRtpPayload());
			frag.freeNativeMemory();	// free native memory
		}
	}
	
	public int getGranule() {
		return mGranule;
	}
	
	public long getTimestamp() {
		return mTimestamp;
	}
	
	public byte[] getData() {
		return mPayload.array();
	}
	
	public int getDataLength() {
		return mDataLen;
	}
}
