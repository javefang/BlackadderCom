package uk.ac.cam.cl.xf214.blackadderCom.net;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import android.util.Log;

import uk.ac.cam.cl.xf214.blackadderWrapper.BAEvent;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAHelper;
import uk.ac.cam.cl.xf214.blackadderWrapper.callback.BAPushDataEventHandler;
import uk.ac.cam.cl.xf214.blackadderWrapper.callback.HashClassifierCallback;

public class BAPacketReceiver {
	public static final String TAG = "BAPacketReceiver";
	public static final int RESYNC_THRESHOLD = 1000;
	private byte[] rid;
	private ArrayBlockingQueue<BAEvent> dataQueue;
	private HashClassifierCallback classifier;
	private volatile boolean released;
	
	public BAPacketReceiver(HashClassifierCallback classifier, byte[] rid) {
		this.released = false;
		this.classifier = classifier;
		this.rid = Arrays.copyOf(rid, rid.length);
		dataQueue = new ArrayBlockingQueue<BAEvent>(RESYNC_THRESHOLD);
		
		// register queue to wrapper
		classifier.registerDataEventHandler(rid, new BAPushDataEventHandler() {
			@Override
			public void publishedData(BAEvent event) {
				dataQueue.offer(event);	// TODO: offer can fail here if consumer is slower than producer
			}
		});
		// from this point all events will be placed in the dataQueue
		Log.i(TAG, "BAPacketReceiver initialised!");
	}
	
	public BlockingQueue<BAEvent> getDataQueue() {
		return dataQueue;
	}
	
	public byte[] getRid() {
		return rid;
	}
	
	public void release() {
		if (!released) {
			Log.i(TAG, "Finishing BAPacketReceiver for info " + BAHelper.byteToHex(rid));
			released = true;
			classifier.unregisterDataEventHandler(rid);	// further event will not be sent to the data queue
			Log.i(TAG, "Draining data queue...");
			drainDataQueue();
			Log.i(TAG, "BAPacketReceiver released!");
		} else {
			Log.i(TAG, "Receiver already finished");
		}
	}
	
	public void drainDataQueue() {
		// drain the queue. free all memory
		ArrayList<BAEvent> wasteEventQueue = new ArrayList<BAEvent>();
		dataQueue.drainTo(wasteEventQueue);
		for (BAEvent event : wasteEventQueue) {
			event.freeNativeBuffer();
		}
	}
}
