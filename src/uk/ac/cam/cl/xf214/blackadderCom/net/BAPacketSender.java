package uk.ac.cam.cl.xf214.blackadderCom.net;

import java.nio.ByteBuffer;
import java.util.Arrays;

import android.util.Log;

import uk.ac.cam.cl.xf214.blackadderWrapper.BAEvent;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAHelper;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAWrapperNB;
import uk.ac.cam.cl.xf214.blackadderWrapper.Strategy;
import uk.ac.cam.cl.xf214.blackadderWrapper.callback.BAPushControlEventAdapter;
import uk.ac.cam.cl.xf214.blackadderWrapper.callback.BAPushControlEventHandler;
import uk.ac.cam.cl.xf214.blackadderWrapper.callback.HashClassifierCallback;
import uk.ac.cam.cl.xf214.blackadderWrapper.data.BAItem;

public class BAPacketSender {
	public static final String TAG = "BAPacketSender";
	public static final byte STRATEGY = Strategy.DOMAIN_LOCAL;
	public static final byte[] FIN_PKT = new byte[0];
	
	private BAWrapperNB wrapper;
	private HashClassifierCallback classifier;
	
	private BAItem item;
	private byte[] fullId;
	private BAPushControlEventHandler eventHandler;
	//private int pktCount;
	
	private volatile boolean canPublish;
	private volatile boolean released;
	
	public BAPacketSender(final BAWrapperNB wrapper, final HashClassifierCallback classifier, final BAItem item) {
		this.released = false;
		this.wrapper = wrapper;
		this.classifier = classifier;
		this.item = item;
		this.fullId = item.getFullId();
		this.eventHandler = new BAPushControlEventAdapter() {
			@Override
			public void startPublish(BAEvent event) {
				Log.i(TAG, "START_PUBLISH called");
				if (Arrays.equals(event.getId(), fullId)) {
					canPublish = true;
					Log.i(TAG, "Start publishing stream " + item.getIdHex());
				} else {
					Log.e(TAG, "Incorrect scope id for START_PUBLISH event, event id is " + BAHelper.byteToHex(event.getId()));
				}
				event.freeNativeBuffer();
			}
			
			@Override
			public void stopPublish(BAEvent event) {
				Log.i(TAG, "STOP_PUBLISH called");
				if (Arrays.equals(event.getId(), fullId)) {
					canPublish = false;
					Log.i(TAG, "Stop publishing stream " + item.getIdHex());
				} else {
					Log.e(TAG, "Incorrect scope id for STOP_PUBLISH event, event id is " + BAHelper.byteToHex(event.getId()));
				}
				event.freeNativeBuffer();
			}
		};
		
		// register listener for start stop publishing
		Log.i(TAG, "Registering control queue with prefix: " + item.getIdHex());
		classifier.registerControlQueue(fullId, eventHandler);
		
		// publish item
		wrapper.publishItem(item.getId(), item.getPrefix(), STRATEGY, null);
		Log.i(TAG, "BAPacketSender initialization complete!");
	}
	
	public void send(byte[] data) {
		if (canPublish) {
			//Log.i(TAG, "Sending pkt (length=" + data.length + ")...");
			final ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);
			buffer.put(data);
			buffer.flip();
			wrapper.publishData(fullId, STRATEGY, null, buffer);
			//Log.i(TAG, "Pkt " + pktCount++ + " sent (length=" + data.length + ")");
		}
	}
	
	public void send(ByteBuffer data) {
		if (canPublish) {
			wrapper.publishData(fullId, STRATEGY, null, data);
		}
	}
	
	public void release() {
		// unpublish item
		if (!released) {
			send(FIN_PKT);	// termination mark
			released = true;
			canPublish = false;	// prevent further send operation
			wrapper.unpublishItem(item.getId(), item.getPrefix(), STRATEGY, null);
			Log.i(TAG, "Unregistering control queue with prefix: " + item.getIdHex());
			classifier.unregisterControlQueue(fullId);
		}
	}
}
