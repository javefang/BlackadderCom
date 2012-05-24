package uk.ac.cam.cl.xf214.blackadderCom.net;

import java.nio.ByteBuffer;

import uk.ac.cam.cl.xf214.blackadderWrapper.BAEvent;
import uk.ac.cam.cl.xf214.blackadderWrapper.ByteHelper;

public class BARtpPacketHelper {
	public static final int HEADER_SIZE = 14;
	
	public static final int GRANULE_POS = 0;
	public static final int SEQ_POS = 4;
	public static final int TIMESTAMP_POS = 6;
	public static final int DATA_POS = 14;
	
	private static byte[] headerBytes = new byte[HEADER_SIZE];
	
	public static void getHeader(BAEvent event, BARtpPacketHeader header) {
		ByteBuffer buf = event.getDirectData();
		buf.position(0);
		buf.get(headerBytes);
		header.granule = ByteHelper.getInt(headerBytes, GRANULE_POS);
		header.seq = ByteHelper.getShort(headerBytes, SEQ_POS);
		header.timestamp = ByteHelper.getLong(headerBytes, TIMESTAMP_POS);
		header.dataLen = buf.remaining();
	}
}

class BARtpPacketHeader {
	int granule;
	short seq;
	long timestamp;
	int dataLen;
}
