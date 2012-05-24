package uk.ac.cam.cl.xf214.blackadderCom.net;

import java.nio.ByteBuffer;

import uk.ac.cam.cl.xf214.blackadderWrapper.BAEvent;

/*
 * Used only when there are out of order packets
 */

@Deprecated
public class BARtpPacketFragment {
	public static final int GRANULE_POS = 0;
	public static final int SEQ_POS = 4;
	public static final int TIMESTAMP_POS = 6;
	public static final int DATA_POS = 14;
	
	private BAEvent mEvent;
	private int mGranule;
	private short mSeq;
	private long mTimestamp;
	private int mRtpDataLength;
	private ByteBuffer mRtpData;
	
	public static BARtpPacketFragment wrap(BAEvent event) {
		return new BARtpPacketFragment(event);
	}
	
	private BARtpPacketFragment(BAEvent event) {
		mEvent = event;
		mRtpData = event.getDirectData();
		mGranule = mRtpData.getInt();
		mSeq = mRtpData.getShort();
		mTimestamp = mRtpData.getLong();
		mRtpDataLength = mRtpData.remaining();
		// position is now on 14, which is DATA_POS
	}
	
	public int getGranule() {
		return mGranule;
	}
	
	public short getSeq() {
		return mSeq;
	}
	
	public long getTimestamp() {
		return mTimestamp;
	}
	
	public int getDataLength() {
		return mRtpDataLength;
	}
	
	public ByteBuffer getRtpPayload() {
		mRtpData.position(DATA_POS);
		// ByteBuffer mRtpData now positions at 14, which is DATA_POS
		return mRtpData;
	}
	
	/* just a wrapper to the BAEvent */
	public void freeNativeMemory() {
		mEvent.freeNativeBuffer();
	}
}
