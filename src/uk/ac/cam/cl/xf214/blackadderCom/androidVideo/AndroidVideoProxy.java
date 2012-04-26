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

public class AndroidVideoProxy {
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
	
	private HashMap<Integer, AndroidVideoPlayer> streamMap;
	private HashMap<Integer, SurfaceView> viewMap;
	private LinkedList<SurfaceView> viewQueue;
	
	private AndroidVideoRecorder recorder;
	
	private boolean send;
	private boolean receive;
	private boolean released;
	
	public AndroidVideoProxy(Node node, SurfaceView[] views) {
		this.wrapper = node.getWrapper();
		this.classifier = node.getClassifier();
		this.views = views;
		this.wakeLock = node.getWakeLock();
		this.clientId = node.getClientId();
		
		this.viewQueue = new LinkedList<SurfaceView>();
		for (SurfaceView sv : views) {
			viewQueue.add(sv);
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
						Log.i(TAG, "Creating new stream " + BAHelper.byteToHex(event.getId()));
						// create BAPacketReceiver and AndroidVoicePlayer
						
						try {
							BAPacketReceiverSocketAdapter receiver = new BAPacketReceiverSocketAdapter(classifier, event.getId(), new StreamFinishedListener() {
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
							SurfaceView view = viewQueue.poll();
							AndroidVideoPlayer player = new AndroidVideoPlayer(receiver, view);
							streamMap.put(idHash, player);
							viewMap.put(idHash, view);
							player.start();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}		
				}
				event.freeNativeBuffer();
			}
		};
		
		wrapper.publishScope(scope.getId(), scope.getPrefix(), STRATEGY, null);
		streamMap = new HashMap<Integer, AndroidVideoPlayer>();
		viewMap = new HashMap<Integer, SurfaceView>();
	}
	
	public synchronized void setSend(boolean enabled) {
		if (enabled == send) {
			return;
		}
		
		send = enabled;
		if (enabled) {	// start streaming
			try {
				BAPacketSenderSocketAdapter sender = new BAPacketSenderSocketAdapter(wrapper, classifier, item);
				recorder = new AndroidVideoRecorder(sender);
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
			// TODO: release wakelock
		}
	}
	
	public synchronized void setReceive(boolean enabled) {
		if (enabled == receive) {
			return;
		}
		
		receive = enabled;
		if (enabled) {
			// TODO: acquire wakelock
			Log.i(TAG, "Registering control queue with prefix: " + scope.getIdHex());
			classifier.registerControlQueue(scope.getFullId(), eventHandler);
			wrapper.subscribeScope(scope.getId(), scope.getPrefix(), STRATEGY, null);
		} else {
			wrapper.unpublishScope(scope.getId(), scope.getPrefix(), STRATEGY, null);
			Log.i(TAG, "Unregistering control queue with prefix: " + scope.getIdHex());
			classifier.unregisterControlQueue(scope.getFullId());
			AndroidVideoPlayer[] playerList = null;
			synchronized(streamMap) {
				playerList = new AndroidVideoPlayer[streamMap.size()];
				if (streamMap.size() > 0) {
					streamMap.values().toArray(playerList);
				}
			}
			
			for (AndroidVideoPlayer player : playerList) {
				Log.i(TAG, "Terminating player " + BAHelper.byteToHex(player.getReceiver().getRid()) + "...");
				player.release();
				
			}
			// TODO: release wakelock
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
