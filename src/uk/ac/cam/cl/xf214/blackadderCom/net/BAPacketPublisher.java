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

public class BAPacketPublisher {
	public static final String TAG = "BAPacketSender";
	public static final byte STRATEGY = Strategy.DOMAIN_LOCAL;
	public static final byte[] FIN_PKT = new byte[0];
	
	private BAWrapperNB wrapper;
	private HashClassifierCallback classifier;
	
	private BAItem item;
	private byte[] fullId;
	private BAPushControlEventHandler eventHandler;
	private boolean canPublish;
	//private int pktCount;
	
	private volatile boolean released;
	
	public BAPacketPublisher(final BAWrapperNB wrapper, final HashClassifierCallback classifier, final BAItem item) {
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
					Log.i(TAG, "Start publishing stream " + item.getIdHex());
					canPublish = true;
				} else {
					Log.e(TAG, "Incorrect scope id for START_PUBLISH event, event id is " + BAHelper.byteToHex(event.getId()));
				}
				event.freeNativeBuffer();
			}
			
			@Override
			public void stopPublish(BAEvent event) {
				Log.i(TAG, "STOP_PUBLISH called");
				if (Arrays.equals(event.getId(), fullId)) {
					Log.i(TAG, "Stop publishing stream " + item.getIdHex());
					canPublish = false;
				} else {
					Log.e(TAG, "Incorrect scope id for STOP_PUBLISH event, event id is " + BAHelper.byteToHex(event.getId()));
				}
				event.freeNativeBuffer();
			}
		};
		
		// register listener for start stop publishing
		Log.i(TAG, "Registering control queue with prefix: " + item.getIdHex());
		classifier.registerControlEventHandler(fullId, eventHandler);
		
		// publish item
		wrapper.publishItem(item.getId(), item.getPrefix(), STRATEGY, null);
		Log.i(TAG, "BAPacketSender initialization complete!");
	}
	
	public void send(byte[] data, int length) {
		if (canPublish) {
			wrapper.publishData(fullId, STRATEGY, null, data, length);
		}
	}
	
	public void sendDirect(ByteBuffer data, int off, int length) {
		if (canPublish) {
			wrapper.publishData(fullId, STRATEGY, null, data, length);
		}
	}
	
	public void release() {
		// unpublish item
		if (!released) {
			send(FIN_PKT, 0);	// termination mark
			released = true;
			wrapper.unpublishItem(item.getId(), item.getPrefix(), STRATEGY, null);
			Log.i(TAG, "Unregistering control queue with prefix: " + item.getIdHex());
			classifier.unregisterControlEventHandler(fullId);
		}
	}
}
