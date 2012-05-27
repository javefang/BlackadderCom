package uk.ac.cam.cl.xf214.blackadderCom.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.util.Log;

import uk.ac.cam.cl.xf214.blackadderWrapper.BAEvent;
import uk.ac.cam.cl.xf214.blackadderWrapper.callback.BAPushDataEventHandler;
import uk.ac.cam.cl.xf214.blackadderWrapper.callback.HashClassifierCallback;

public class BARtpReceiver implements BAPushDataEventHandler {
	public static final String TAG = "BARtpReceiver";
	//public static final int DEFAULT_QUEUE_SIZE = 60;	// 60 frames
	public static final long READ_TIMEOUT_MS = 500;
	
	private HashClassifierCallback mClassifier;
	private byte[] mRid;
	
	private ArrayBlockingQueue<BARtpPacket> dataQueue;
	private boolean released = false;
	private boolean mReceive;
	private int mQueueSize;
	private BARtpPacketHeader mPktHeader;
	
	private ArrayList<BAEvent> curGranuleFragmentQueue;
	private int curRtpDataLen = 0;
	private int curGranule = -1;
	private int curSeq = -1;
	private long curTimestamp = -1;
	
	public BARtpReceiver(HashClassifierCallback classifier, byte[] rid, int queueSize) {
		mQueueSize = queueSize;
		mClassifier = classifier;
		mRid = rid;
		classifier.registerDataEventHandler(rid, this);
		
		dataQueue = new ArrayBlockingQueue<BARtpPacket>(mQueueSize);
		curGranuleFragmentQueue = new ArrayList<BAEvent>();
		mPktHeader = new BARtpPacketHeader();
	}
	
	public BARtpPacket getBARtpPacket() throws InterruptedException, IOException {
		if (released) {
			throw new IOException("Calling getBARtpPacket() after BARtpReceiver has been released!");
		}
		return dataQueue.poll(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * skip and discard N packets
	 * @param count number of packets to discard
	 * @return true if the requested number of packets is discarded, false otherwise
	 */
	public boolean skipPacket(int count) {
		for (int i = 0; i < count; i++) {
			if (dataQueue.poll() == null) {
				return false;
			}
		}
		return true;
	}
	
	public int getQueueSize() {
		return dataQueue.size();
	}
	
	public long getCurrentTimestamp() {
		return curTimestamp;
	}

	@Override
	public void publishedData(BAEvent event) {
		// free memory only if receiver is released
		if (!mReceive) {
			event.freeNativeBuffer();
			return;
		}
		
		// release receiver if FIN_PKT received
		if (event.getDataLength() == 0) {
			Log.i(TAG, "FIN_PKT received, releasing BARtpReceiver...");
			release();
			return;
		}
		
		BARtpPacketHelper.getHeader(event, mPktHeader);
		if (mPktHeader.granule == curGranule) {
			// fragment of the same granule
			if (mPktHeader.seq > curSeq) {
				// pkt in sequence (possible to skip some fragments (but we don't care!)
				// add new fragment to the curGranuleFramentQueue
				appendFragment(event, mPktHeader.seq);
			} else {
				// out of sequence, two possibility
				// 1. seq < curSeq (late arrival, discard)
				// 2. seq == curSeq (duplication, discard)
				// so basically we will ignore out of sequence packet, free memory only
				event.freeNativeBuffer();
			}
		} else if (mPktHeader.granule > curGranule) {
			// current granule complete, generate BARtpPacket if the fragment queue is not empty
			generateBARtpPacket();
			// update current granule and timestamp
			curGranule = mPktHeader.granule;
			curTimestamp = mPktHeader.timestamp;
			// add new fragment to the curGranuleFragmentQueue
			appendFragment(event, mPktHeader.seq);
		} else {
			// granule < curGranule (late arrival granule, discard)
			event.freeNativeBuffer();
		}
	}
	
	public void setReceive(boolean receive) throws IOException {
		if (!released) {
			mReceive = receive;
		} else {
			throw new IOException("Calling setReceive after BARtpReceiver is released!");
		}
	}
	
	public byte[] getRid() {
		return mRid;
	}
	
	/* must unregister this BAPushDataEventHandler first (no new incoming events) */
	public void release() {
		if (!released) {
			mReceive = false;
			released = true;
			// unregistering data event handler
			mClassifier.unregisterDataEventHandler(mRid);
			// clear curGranuleFragmentQueue
			for (BAEvent frag : curGranuleFragmentQueue) {
				frag.freeNativeBuffer();
			}
			curGranuleFragmentQueue.clear();
			// clear dataQueue
			dataQueue.clear();
		}
	}
	
	private void generateBARtpPacket() {
		// TODO: add checksum if we need to ensure integrity of the BARtpPacket
		if (!curGranuleFragmentQueue.isEmpty()) {
			//Log.i(TAG, "Received RTP packet: granule=" + curGranule + ", timestamp=" + curTimestamp);
			byte[] payload = new byte[curRtpDataLen];
			int off = 0;
			
			for (BAEvent frag : curGranuleFragmentQueue) {
				//frag.getRtpPayload().get(payload, off, frag.getDataLength());	// payload: frag -> pkt
				ByteBuffer buf = frag.getDirectData();
				buf.position(BARtpPacketHelper.DATA_POS);
				buf.get(payload, off, frag.getDataLength() - BARtpPacketHelper.HEADER_SIZE);
				off += frag.getDataLength() - BARtpPacketHelper.HEADER_SIZE;
				frag.freeNativeBuffer();	// free native memory
			}
			BARtpPacket pkt = new BARtpPacket(curGranule, curTimestamp, payload, curRtpDataLen);
			dataQueue.offer(pkt);	// TODO: offer can fail here if the queue is full
			// clear curGranuleFragmentQueue, update curGranule & curTimestamp
			curGranuleFragmentQueue.clear();
			curRtpDataLen = 0;
		}
	}
	
	private void appendFragment(BAEvent frag, short seq) {
		// add new fragment to the curGranuleFramentQueue
		//Log.i(TAG, "Recv fragment " + seq + ": size=" + mPktHeader.dataLen);
		curGranuleFragmentQueue.add(frag);
		curRtpDataLen += mPktHeader.dataLen;
		curSeq = seq;
	}
}
