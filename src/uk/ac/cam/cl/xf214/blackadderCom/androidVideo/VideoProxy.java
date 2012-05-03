package uk.ac.cam.cl.xf214.blackadderCom.androidVideo;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.SurfaceView;
import uk.ac.cam.cl.xf214.blackadderCom.Node;
import uk.ac.cam.cl.xf214.blackadderCom.net.BAPacketReceiverSocketAdapter;
import uk.ac.cam.cl.xf214.blackadderCom.net.BAPacketSenderSocketAdapter;
import uk.ac.cam.cl.xf214.blackadderCom.net.StreamFinishedListener;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAEvent;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAHelper;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAWrapperNB;
import uk.ac.cam.cl.xf214.blackadderWrapper.Strategy;
import uk.ac.cam.cl.xf214.blackadderWrapper.callback.BAPushControlEventAdapter;
import uk.ac.cam.cl.xf214.blackadderWrapper.callback.BAPushControlEventHandler;
import uk.ac.cam.cl.xf214.blackadderWrapper.callback.HashClassifierCallback;
import uk.ac.cam.cl.xf214.blackadderWrapper.data.BAItem;
import uk.ac.cam.cl.xf214.blackadderWrapper.data.BAScope;

public class VideoProxy {
	public static final String TAG = "AndroidVideoProxy";
	public static final byte STRATEGY = Strategy.DOMAIN_LOCAL;
	public static final byte[] VIDEO_SCOPE_ID = BAHelper.hexToByte("3333333333333333");
	
	private BAScope scope;
	private byte[] clientId;
	private BAItem item;
	private BAPushControlEventHandler eventHandler;
	
	private BAWrapperNB wrapper;
	private HashClassifierCallback classifier;
	//private BAPacketSenderSocketAdapter sender;
	private WakeLock wakeLock;
	private SurfaceView[] views;
	
	private HashMap<Integer, VideoPlayer> streamMap;
	private HashMap<Integer, SurfaceView> viewMap;
	private LinkedList<SurfaceView> viewQueue;
	
	private VideoRecorder recorder;
	
	private boolean send;
	private boolean receive;
	private boolean released;
	
	public VideoProxy(Node node, SurfaceView[] views) {
		this.wrapper = node.getWrapper();
		this.classifier = node.getClassifier();
		this.views = views;
		this.wakeLock = node.getWakeLock();
		this.clientId = node.getClientId();
		
		this.viewQueue = new LinkedList<SurfaceView>();
		for (int i = 0; i < views.length - 1; i++) {
			viewQueue.add(views[i]);
		}
		
		// process scope id
		this.scope = BAScope.createBAScope(VIDEO_SCOPE_ID, node.getRoomScope());
		this.item = BAItem.createBAItem(clientId, scope);
		this.eventHandler = new BAPushControlEventAdapter() {
			@Override
			public void newData(BAEvent event) {
				int idHash = event.getId().hashCode();
				synchronized(streamMap) {
					if (!streamMap.containsKey(idHash) && !viewQueue.isEmpty()) {
						// TODO: creating new stream
						initiateStream(event.getId(), idHash);
					}		
				}
				event.freeNativeBuffer();
			}
		};
		
		wrapper.publishScope(scope.getId(), scope.getPrefix(), STRATEGY, null);
		streamMap = new HashMap<Integer, VideoPlayer>();
		viewMap = new HashMap<Integer, SurfaceView>();
	}
	
	private void initiateStream(byte[] id, int idHash) {
		Log.i(TAG, "Creating new stream " + BAHelper.byteToHex(id));
		// create BAPacketReceiver and AndroidVoicePlayer
		
		try {
			BAPacketReceiverSocketAdapter receiver = new BAPacketReceiverSocketAdapter(classifier, id, new StreamFinishedListener() {
				public void streamFinished(byte[] rid) {
					int idHash = Arrays.hashCode(rid);
					synchronized(streamMap) {
						if (streamMap.containsKey(idHash)) {
							// remove stream
							streamMap.remove(idHash);
							// recycle SurfaceView
							viewQueue.offer(viewMap.get(idHash));
						}
					}
				}
			});
			Log.i(TAG, "Retrieving SurfaceView...");
			SurfaceView view = viewQueue.poll();
			Log.i(TAG, "Get!");
			VideoPlayer player = new VideoPlayer(receiver, view);
			streamMap.put(idHash, player);
			viewMap.put(idHash, view);
			player.start();
			Log.i(TAG, "Player.start() called");	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public synchronized void setSend(boolean enabled) {
		if (enabled == send) {
			return;
		}
		
		send = enabled;
		if (enabled) {	// start streaming
			wakeLock.acquire();
			try {
				BAPacketSenderSocketAdapter sender = new BAPacketSenderSocketAdapter(wrapper, classifier, item);
				Log.i(TAG, "Starting video recorder...");
				recorder = new VideoRecorder(sender, views[views.length-1]);
				recorder.start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.e(TAG, "ERROR: failed to initialise sender: " + e.getMessage());
				e.printStackTrace();
				send = false;
				return;
			}
		} else {	// stop streaming
			if (recorder != null) {
				recorder.release();
				recorder = null;
			}
			wakeLock.release();
		}
	}
	
	public synchronized void setReceive(boolean enabled) {
		if (enabled == receive) {
			return;
		}
		
		receive = enabled;
		if (enabled) {
			wakeLock.acquire();
			Log.i(TAG, "Registering control queue with prefix: " + scope.getIdHex());
			classifier.registerControlQueue(scope.getFullId(), eventHandler);
			wrapper.subscribeScope(scope.getId(), scope.getPrefix(), STRATEGY, null);
		} else {
			wrapper.unsubscribeScope(scope.getId(), scope.getPrefix(), STRATEGY, null);
			Log.i(TAG, "Unregistering control queue with prefix: " + scope.getIdHex());
			classifier.unregisterControlQueue(scope.getFullId());
			VideoPlayer[] playerList = null;
			synchronized(streamMap) {
				playerList = new VideoPlayer[streamMap.size()];
				if (streamMap.size() > 0) {
					streamMap.values().toArray(playerList);
				}
			}
			
			for (VideoPlayer player : playerList) {
				Log.i(TAG, "Terminating player " + BAHelper.byteToHex(player.getReceiver().getRid()) + "...");
				player.release();
				
			}
			wakeLock.release();
		}
	}
	
	public synchronized void release() {
		if (!released) {
			released = true;
			setSend(false);
			setReceive(false);
		}
		wrapper.unpublishScope(scope.getId(), scope.getPrefix(), STRATEGY, null);
	}
}
