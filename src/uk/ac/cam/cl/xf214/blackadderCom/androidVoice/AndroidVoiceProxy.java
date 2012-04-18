package uk.ac.cam.cl.xf214.blackadderCom.androidVoice;

import java.util.Arrays;
import java.util.HashMap;

import android.util.Log;

import uk.ac.cam.cl.xf214.blackadderCom.net.BAPacketReceiver;
import uk.ac.cam.cl.xf214.blackadderCom.net.BAPacketSender;
import uk.ac.cam.cl.xf214.blackadderCom.net.StreamFinishedListener;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAEvent;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAHelper;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAWrapperNB;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAWrapperShared;
import uk.ac.cam.cl.xf214.blackadderWrapper.Strategy;
import uk.ac.cam.cl.xf214.blackadderWrapper.callback.BAPushControlEventAdapter;
import uk.ac.cam.cl.xf214.blackadderWrapper.callback.HashClassifierCallback;
import uk.ac.cam.cl.xf214.blackadderWrapper.data.BAItem;
import uk.ac.cam.cl.xf214.blackadderWrapper.data.BAPrefix;
import uk.ac.cam.cl.xf214.blackadderWrapper.data.BAScope;

public class AndroidVoiceProxy {
	public static final String TAG = "AndroidVoiceProxy";
	public static final byte STRATEGY = Strategy.DOMAIN_LOCAL;
	public static final byte[] VOICE_SCOPE_ID = BAHelper.hexToByte("2222222222222222");
	public static final long CLEANUP_TIME = 500;
	public static final int DEFAULT_BUF_SIZE = 32000;		// 32K
	
	private BAScope voiceScope;
	private byte[] clientId;
	private BAPrefix voicePrefix;
	
	private BAWrapperNB wrapper;
	private HashClassifierCallback classifier;
	private BAPacketSender sender;
	
	private AndroidVoiceRecorder recorder;
	private HashMap<Integer, AndroidVoicePlayer> streamMap;
	
	private int bufSize;

	private boolean finished;
	
	public AndroidVoiceProxy(final BAWrapperNB wrapper, final HashClassifierCallback classifier, byte[] roomId, byte[] clientId) {
		this.wrapper = wrapper;
		this.classifier = classifier;
		this.clientId = Arrays.copyOf(clientId, clientId.length);
		this.bufSize = DEFAULT_BUF_SIZE;
		// process scope id
		BAScope roomScope = BAScope.createBAScope(roomId);
		this.voiceScope = BAScope.createBAScope(VOICE_SCOPE_ID, roomScope);
		
		this.voicePrefix = new BAPrefix(voiceScope.getFullId(), new BAPushControlEventAdapter() {
			@Override
			public void newData(BAEvent event) {
				int idHash = event.getId().hashCode();
				synchronized(streamMap) {
					if (!streamMap.containsKey(idHash)) {
						Log.i(TAG, "Creating new stream " + BAHelper.byteToHex(event.getId()));
						// create BAPacketReceiver and AndroidVoicePlayer
						BAPacketReceiver receiver = new BAPacketReceiver(classifier, event.getId(), new StreamFinishedListener() {
							public void streamFinished(byte[] rid) {
								int idHash = Arrays.hashCode(rid);
								synchronized(streamMap) {
									if (streamMap.containsKey(idHash)) {
										streamMap.remove(idHash);
									}
								}
							}
						});
						AndroidVoicePlayer player = new AndroidVoicePlayer(receiver, bufSize);
						streamMap.put(Arrays.hashCode(event.getId()), player);
						player.start();
					}		
				}
				event.freeNativeBuffer();
			}
		});
		wrapper.publishScope(voiceScope.getId(), voiceScope.getPrefix(), STRATEGY, null);
		streamMap = new HashMap<Integer, AndroidVoicePlayer>();
	}
	
	public synchronized void setSend(boolean enabled) {
		if (enabled) {
			BAItem item = BAItem.createBAItem(clientId, voiceScope);
			sender = new BAPacketSender(wrapper, classifier, item);
			recorder = new AndroidVoiceRecorder(sender, BAWrapperShared.DEFAULT_PKT_SIZE);
			recorder.start();
		} else {	// disabled
			if (recorder != null) {
				recorder.finish();
				recorder = null;
			}
		}
	}

	public synchronized void setReceive(boolean enabled) {
		if (enabled) {
			// NOTE: the order is important
			// Step 1 registers control queue so further events can be forwarded to the proxy
			// Step 2 subscribe to voice scope, starting receiving events about the scope
			
			Log.i(TAG, "Registering control queue with prefix: " + voiceScope.getIdHex());
			// 1. register control queue
			classifier.registerControlQueue(voicePrefix);
			// from now proxy is able to receive events about unmapped new stream (so new streams can be created)
			
			// 2. subscribe voice scope
			wrapper.subscribeScope(voiceScope.getId(), voiceScope.getPrefix(), STRATEGY, null);
			// scope subscribed, from now proxy will receive events about the voice scope
		} else {
			// NOTE: the order is important
			// Step 1 first ensures no further events about the scope will arrive
			// Step 2 unregister the control queue
			// Step 3 terminates all active player
			
			// 1. unsubscribe voice channel
			wrapper.unsubscribeScope(voiceScope.getId(), voiceScope.getPrefix(), STRATEGY, null);
			// from now proxy will not receive any new events about the voice scope
			
			// 2. unregister control queue
			Log.i(TAG, "Unregistering control queue with prefix: " + voiceScope.getIdHex());
			// from now the proxy stops to receive any new control events
			classifier.unregisterControlQueue(voicePrefix);
			
			// 3. terminate all active player
			AndroidVoicePlayer[] playerList = new AndroidVoicePlayer[streamMap.size()];
			synchronized(streamMap) {
				if (streamMap.size() > 0) {
					streamMap.values().toArray(playerList);
				}
			}
			for (AndroidVoicePlayer player : playerList) {
				Log.i(TAG, "Terminating player " + BAHelper.byteToHex(player.getReceiver().getRid()) + "...");
				player.finish();
				//cleanupThread(player);
				try {
					player.join();
				} catch (InterruptedException e) {
					Log.e(TAG, "Interrupted exception caught while waiting for player thread to die: " + e.getMessage());
				}
			}
			if (!streamMap.isEmpty()) {
				Log.i(TAG, "ERROR:  streamMap should be empty after all players are terminted (they remove themselves from the map)");
				streamMap.clear();
			}
		}
	}
	
	public synchronized void finish() {
		if (!finished) {
			finished = true;
			setSend(false);
			setReceive(false);
		}
		wrapper.unpublishScope(voiceScope.getId(), voiceScope.getPrefix(), STRATEGY, null);
	}
	
	@Deprecated
	private boolean cleanupThread(Thread t) {
		try {
			t.join(CLEANUP_TIME);
			// interrupt the thread if not terminated after 0.5 sec
			if (t.getState() != Thread.State.TERMINATED) {
				t.interrupt();
				return false;
			} else {
				return true;
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	public int getBufSize() {
		return bufSize;
	}

	public void setBufSize(int bufSize) {
		this.bufSize = bufSize;
	}
}
