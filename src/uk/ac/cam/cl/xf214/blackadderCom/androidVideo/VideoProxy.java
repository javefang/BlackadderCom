package uk.ac.cam.cl.xf214.blackadderCom.androidVideo;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import de.mjpegsample.MjpegView;

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
	
	private HashMap<Integer, VideoPlayer> mStreamMap;
	private FCFSViewScheduler mViewSched;
	private SurfaceView preview;
	
	private VideoRecorder recorder;
	
	
	private boolean send;
	private boolean receive;
	private boolean released;
	
	public VideoProxy(Node node, MjpegView[] views, SurfaceView preview) {
		this.released = false;
		this.wrapper = node.getWrapper();
		this.classifier = node.getClassifier();
		this.mViewSched = new FCFSViewScheduler(views);
		this.preview = preview;
		this.wakeLock = node.getWakeLock();
		this.clientId = node.getClientId();
		mStreamMap = new HashMap<Integer, VideoPlayer>(); 
		
		// process scope id
		this.scope = BAScope.createBAScope(VIDEO_SCOPE_ID, node.getRoomScope());
		this.item = BAItem.createBAItem(clientId, scope);
		
		// controller to use when receiving new stream
		this.eventHandler = new BAPushControlEventAdapter() {
			@Override
			public void newData(BAEvent event) {
				int idHash = Arrays.hashCode(event.getId());
				synchronized(mStreamMap) {
					if (!mStreamMap.containsKey(idHash)) {
						// creating new stream if not previously added
						initStream(event.getId(), idHash);
					}		
				}
				event.freeNativeBuffer();
			}
		};
		
		wrapper.publishScope(scope.getId(), scope.getPrefix(), STRATEGY, null);
	}
	
	
	private void initStream(byte[] id, int idHash) {
		Log.i(TAG, "Creating new stream " + BAHelper.byteToHex(id));
		// create BAPacketReceiver and AndroidVoicePlayer
		
		try {
			StreamFinishedListener sfLis = new StreamFinishedListener() {
				public void streamFinished(byte[] rid) {
					Log.i(TAG, "StreamFinishedListener called");
					int idHash = Arrays.hashCode(rid);
					//Log.i(TAG, "streamFinished(): Acquiring mutex on mStreamMap");
					synchronized(mStreamMap) {
						if (mStreamMap.containsKey(idHash)) {
							// remove stream
							Log.i(TAG, "Removing stream from ViewScheduler and streamMap...");
							VideoPlayer player = mStreamMap.get(idHash);
							mViewSched.removeStream(player);
							mStreamMap.remove(idHash);
						}
					}
					//Log.i(TAG, "streamFinished(): mutex on mStreamMap released");
				}
			};
			Log.i(TAG, "Creating BAPacketReceiverSocketAdapter...");
			BAPacketReceiverSocketAdapter receiver = new BAPacketReceiverSocketAdapter(classifier, id);
			Log.i(TAG, "Creating VideoPlayer...");
			VideoPlayer player = new VideoPlayer(receiver, sfLis);
			Log.i(TAG, "add to mStreamMap");
			mStreamMap.put(idHash, player);
			Log.i(TAG, "ViewScheduler addStream()");
			mViewSched.addStream(player);
			Log.i(TAG, "Stream initialized!");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.e(TAG, "ERROR: IOException caught!");
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
				recorder = new VideoRecorder(sender, preview);
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
			synchronized(mStreamMap) {
				playerList = new VideoPlayer[mStreamMap.size()];
				if (mStreamMap.size() > 0) {
					mStreamMap.values().toArray(playerList);
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
