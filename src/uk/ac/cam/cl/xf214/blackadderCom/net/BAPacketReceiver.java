package uk.ac.cam.cl.xf214.blackadderCom.net;

import java.util.Arrays;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.util.Log;

import uk.ac.cam.cl.xf214.blackadderWrapper.BAEvent;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAHelper;
import uk.ac.cam.cl.xf214.blackadderWrapper.callback.HashClassifierCallback;

public class BAPacketReceiver {
	public static final String TAG = "BAPacketReceiver";
	private byte[] rid;
	private BlockingQueue<BAEvent> dataQueue;
	private HashClassifierCallback classifier;
	private StreamFinishedListener streamFinishedListener;
	private volatile boolean finished;
	
	public BAPacketReceiver(HashClassifierCallback classifier, byte[] rid, StreamFinishedListener streamFinishedListener) {
		this.finished = false;
		this.classifier = classifier;
		this.rid = Arrays.copyOf(rid, rid.length);
		this.streamFinishedListener = streamFinishedListener;
		dataQueue = new LinkedBlockingQueue<BAEvent>();
		
		// register queue to wrapper
		classifier.registerDataQueue(rid, dataQueue);
		// from this point all events will be placed in the dataQueue
	}
	
	public BlockingQueue<BAEvent> getDataQueue() {
		return dataQueue;
	}
	
	public byte[] getRid() {
		return rid;
	}
	
	public void finish() {
		if (!finished) {
			Log.i(TAG, "Finishing BAPacketReceiver for info " + BAHelper.byteToHex(rid));
			finished = true;
			classifier.unregisterDataQueue(rid);	// further event will not be sent to the data queue
			// now events are sent to AndroidVoiceProxy, but will be ignored as the stream hasdID of this stream still hasn't been unregistered from the proxy
			streamFinishedListener.streamFinished(rid);	// unregister stream hashID from the proxy
			// now events are sent to AndroidVoiceProxy, which may create new receiver and player thread
			Log.i(TAG, "Draining data queue...");
			// drain the queue. free all memory
			Vector<BAEvent> wasteEventQueue = new Vector<BAEvent>();
			dataQueue.drainTo(wasteEventQueue);
			for (int i = 0; i < wasteEventQueue.size(); i++) {
				wasteEventQueue.get(i).freeNativeBuffer();
			}
			Log.i(TAG, "Receiver finished!");
		} else {
			Log.i(TAG, "Receiver already finished");
		}
	}
}
