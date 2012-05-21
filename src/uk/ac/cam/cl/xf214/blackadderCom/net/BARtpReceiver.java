package uk.ac.cam.cl.xf214.blackadderCom.net;

import java.io.IOException;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;

import uk.ac.cam.cl.xf214.blackadderWrapper.BAEvent;
import uk.ac.cam.cl.xf214.blackadderWrapper.callback.BAPushDataEventHandler;

public class BARtpReceiver implements BAPushDataEventHandler {
	public static final int DEFAULT_QUEUE_SIZE = 60;	// 60 frames
	
	private ArrayBlockingQueue<BARtpPacket> dataQueue;
	private boolean released = false;
	
	private Vector<BARtpPacketFragment> curGranuleFragmentQueue;
	private int curRtpDataLen = 0;
	private int curGranule = -1;
	private int curSeq = -1;
	private long curTimestamp = -1;
	
	public BARtpReceiver() {
		dataQueue = new ArrayBlockingQueue<BARtpPacket>(DEFAULT_QUEUE_SIZE);
		curGranuleFragmentQueue = new Vector<BARtpPacketFragment>();
	}
	
	public BARtpPacket getBARtpPacket() throws InterruptedException, IOException {
		if (!released) {
			return dataQueue.take();
		} else {
			throw new IOException("BARtpReceiver already released!");
		}
	}

	@Override
	public void publishedData(BAEvent event) {
		if (released) {
			// free memory only if already released
			event.freeNativeBuffer();
			return;
		}
		
		BARtpPacketFragment frag = BARtpPacketFragment.wrap(event);
		if (frag.getGranule() == curGranule) {
			// fragment of the same granule
			if (frag.getSeq() > curSeq) {
				// pkt in sequence (possible to skip some fragments (but we don't care!)
				// add new fragment to the curGranuleFramentQueue
				appendFragment(frag);
			} else {
				// out of sequence, two possibility
				// 1. seq < curSeq (late arrival, discard)
				// 2. seq == curSeq (duplication, discard)
				// so basically we will ignore out of sequence packet, free memory only
				frag.freeNativeMemory();
			}
		} else if (frag.getGranule() > curGranule) {
			// current granule complete, generate BARtpPacket if the fragment queue is not empty
			generateBARtpPacket();
			// update current granule and timestamp
			curGranule = frag.getGranule();
			curTimestamp = frag.getTimestamp();
			// add new fragment to the curGranuleFragmentQueue
			appendFragment(frag);
		} else {
			// granule < curGranule (late arrival granule, discard)
			frag.freeNativeMemory();
		}
	}
	
	/* must unregister this BAPushDataEventHandler first (no new incoming events) */
	public void release() {
		if (!released) {
			released = true;
			// clear curGranuleFragmentQueue
			for (BARtpPacketFragment frag : curGranuleFragmentQueue) {
				frag.freeNativeMemory();
			}
			curGranuleFragmentQueue.clear();
			
			// clear dataQueue
			dataQueue.clear();
		}
	}
	
	private void generateBARtpPacket() {
		// TODO: add checksum if we need to ensure integrity of the BARtpPacket
		if (!curGranuleFragmentQueue.isEmpty()) {
			BARtpPacket pkt = new BARtpPacket(curGranule, curTimestamp, curGranuleFragmentQueue, curRtpDataLen);
			dataQueue.offer(pkt);	// TODO: offer can fail here if the queue is full
			// clear curGranuleFragmentQueue, update curGranule & curTimestamp
			curGranuleFragmentQueue.clear();
			curRtpDataLen = 0;
			// TEST BARtp BRANCH
		}		
	}
	
	private void appendFragment(BARtpPacketFragment frag) {
		// add new fragment to the curGranuleFramentQueue
		curGranuleFragmentQueue.add(frag);
		curRtpDataLen += frag.getDataLength();
		curSeq = frag.getSeq();
	}
}
