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
	//private StreamFinishedListener streamFinishedListener;
	private volatile boolean released;
	
	public BAPacketReceiver(HashClassifierCallback classifier, byte[] rid) {
		this.released = false;
		this.classifier = classifier;
		this.rid = Arrays.copyOf(rid, rid.length);
		//this.streamFinishedListener = streamFinishedListener;
		dataQueue = new LinkedBlockingQueue<BAEvent>();
		
		// register queue to wrapper
		classifier.registerDataQueue(rid, dataQueue);
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
			classifier.unregisterDataQueue(rid);	// further event will not be sent to the data queue
			// now events are sent to AndroidVoiceProxy, but will be ignored as the stream hasdID of this stream still hasn't been unregistered from the proxy
			//streamFinishedListener.streamFinished(rid);	// unregister stream hashID from the proxy
			// now events are sent to AndroidVoiceProxy, which may create new receiver and player thread
			Log.i(TAG, "Draining data queue...");
			drainDataQueue();
			Log.i(TAG, "BAPacketReceiver released!");
		} else {
			Log.i(TAG, "Receiver already finished");
		}
	}
	
	public void drainDataQueue() {
		// drain the queue. free all memory
		Vector<BAEvent> wasteEventQueue = new Vector<BAEvent>();
		dataQueue.drainTo(wasteEventQueue);
		for (BAEvent event : wasteEventQueue) {
			event.freeNativeBuffer();
		}
	}
}
