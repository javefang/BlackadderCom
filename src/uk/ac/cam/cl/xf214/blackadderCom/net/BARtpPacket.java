package uk.ac.cam.cl.xf214.blackadderCom.net;

public class BARtpPacket {
	private byte[] mPayload;
	private int mGranule;
	private long mTimestamp;
	private int mDataLen;
	
	public BARtpPacket(int granule, long timestamp, byte[] payload, int dataLen) {
		mPayload = payload;
		mGranule = granule;
		mTimestamp = timestamp;
		mDataLen = dataLen;
		
		
		
	}
	
	public int getGranule() {
		return mGranule;
	}
	
	public long getTimestamp() {
		return mTimestamp;
	}
	
	public byte[] getData() {
		return mPayload;
	}
	
	public int getDataLength() {
		return mDataLen;
	}
}
