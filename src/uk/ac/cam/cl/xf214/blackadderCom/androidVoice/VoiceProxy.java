package uk.ac.cam.cl.xf214.blackadderCom.androidVoice;

import java.util.Arrays;
import java.util.HashMap;

import android.os.PowerManager.WakeLock;
import android.util.Log;

import uk.ac.cam.cl.xf214.blackadderCom.BANode;
import uk.ac.cam.cl.xf214.blackadderCom.net.BAPacketReceiver;
import uk.ac.cam.cl.xf214.blackadderCom.net.BAPacketSender;
import uk.ac.cam.cl.xf214.blackadderCom.net.StreamFinishedListener;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAEvent;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAHelper;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAWrapperNB;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAWrapperShared;
import uk.ac.cam.cl.xf214.blackadderWrapper.Strategy;
import uk.ac.cam.cl.xf214.blackadderWrapper.callback.BAPushControlEventAdapter;
import uk.ac.cam.cl.xf214.blackadderWrapper.callback.BAPushControlEventHandler;
import uk.ac.cam.cl.xf214.blackadderWrapper.callback.HashClassifierCallback;
import uk.ac.cam.cl.xf214.blackadderWrapper.data.BAItem;
import uk.ac.cam.cl.xf214.blackadderWrapper.data.BAScope;

public class VoiceProxy {
	public static enum VoiceCodec {PCM, SPEEX};
	public static final String TAG = "VoiceProxy";
	public static final byte STRATEGY = Strategy.DOMAIN_LOCAL;
	public static final int DEFAULT_SAMPLE_RATE = 22050; 
	public static final byte[] VOICE_SCOPE_ID = BAHelper.hexToByte("2222222222222222");
	
	//public static final int DEFAULT_BUF_SIZE = 8 * 1024;		// 8K
	
	private VoiceCodec codec = VoiceCodec.PCM;
	
	private BAScope scope;
	private BAItem item;
	private byte[] clientId;
	private BAPushControlEventHandler eventHandler;
	
	private BAWrapperNB wrapper;
	private HashClassifierCallback classifier;
	//private BAPacketSender sender;
	private WakeLock wakeLock;
	
	private VoiceRecorder recorder;
	private HashMap<Integer, VoicePlayer> streamMap;
	
	private int bufSize;
	private int sampleRate = DEFAULT_SAMPLE_RATE;

	private boolean send;
	private boolean receive;
	private boolean released;
	
	public VoiceProxy(BANode node) {
		this.wrapper = node.getWrapper();
		this.classifier = node.getClassifier();
		this.wakeLock = node.getWakeLock();
		this.clientId = node.getClientId();
		streamMap = new HashMap<Integer, VoicePlayer>();
		
		// process scope id
		this.scope = BAScope.createBAScope(VOICE_SCOPE_ID, node.getRoomScope());
		this.item = BAItem.createBAItem(clientId, scope);
		
		this.eventHandler = new BAPushControlEventAdapter() {
			@Override
			public void newData(BAEvent event) {
				int idHash = Arrays.hashCode(event.getId());	// TODO: fixing hashing behaviour (need revise)
				synchronized(streamMap) {
					if (!streamMap.containsKey(idHash)) {
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
		StreamFinishedListener sfl = new StreamFinishedListener() {
			public void streamFinished(byte[] rid) {
				int idHash = Arrays.hashCode(rid);
				synchronized(streamMap) {
					if (streamMap.containsKey(idHash)) {
						streamMap.remove(idHash);
					}
				}
			}
		};
		BAPacketReceiver receiver = new BAPacketReceiver(classifier, id);
		VoicePlayer player = new VoicePlayer(receiver, codec, sfl, sampleRate);
		streamMap.put(idHash, player);
		player.start();
	}
	
	public synchronized void setSend(boolean enabled) {
		if (enabled == send) {
			// no need to disable(enable) twice
			return;
		}
		send = enabled;
		if (enabled) {	// start streaming
			wakeLock.acquire();
			BAPacketSender sender = new BAPacketSender(wrapper, classifier, item);
			recorder = new VoiceRecorder(sender, BAWrapperShared.DEFAULT_PKT_SIZE, codec, sampleRate);
			recorder.start();
		} else {	// stop streaming
			if (recorder != null) {
				recorder.release();
				recorder = null;
			}
			wakeLock.release();
		}
	}
	
	public synchronized void setCodec(VoiceCodec codec) {
		this.codec = codec;
		Log.i(TAG, "Set codec = " + codec);
	}

	public synchronized void setReceive(boolean enabled) {
		if (enabled == receive) {
			// no need to disable(enable) twice
			return;
		}
		receive = enabled;
		if (enabled) {
			wakeLock.acquire();
			Log.i(TAG, "CURRENT ACTIVE THREAD: " + Thread.activeCount());
			// NOTE: the order is important
			// Step 1 registers control queue so further events can be forwarded to the proxy
			// Step 2 subscribe to voice scope, starting receiving events about the scope
			
			Log.i(TAG, "Registering control queue with prefix: " + scope.getIdHex());
			// 1. register control queue
			classifier.registerControlEventHandler(scope.getFullId(), eventHandler);
			// from now proxy is able to receive events about unmapped new stream (so new streams can be created)
			
			// 2. subscribe voice scope
			wrapper.subscribeScope(scope.getId(), scope.getPrefix(), STRATEGY, null);
			// scope subscribed, from now proxy will receive events about the voice scope
		} else {
			// NOTE: the order is important
			// Step 1 first ensures no further events about the scope will arrive
			// Step 2 unregister the control queue
			// Step 3 terminates all active player
			
			// 1. unsubscribe voice channel
			wrapper.unsubscribeScope(scope.getId(), scope.getPrefix(), STRATEGY, null);
			// from now proxy will not receive any new events about the voice scope
			
			// 2. unregister control queue
			Log.i(TAG, "Unregistering control queue with prefix: " + scope.getIdHex());
			// from now the proxy stops to receive any new control events
			classifier.unregisterControlEventHandler(scope.getFullId());
			
			// 3. terminate all active player
			VoicePlayer[] playerList = null;
			synchronized(streamMap) {
				playerList = new VoicePlayer[streamMap.size()];
				if (streamMap.size() > 0) {
					streamMap.values().toArray(playerList);
				}
			}
			for (VoicePlayer player : playerList) {
				Log.i(TAG, "Terminating player " + BAHelper.byteToHex(player.getReceiver().getRid()) + "...");
				player.release();
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
	
	public int getBufSize() {
		return bufSize;
	}

	public void setBufSize(int bufSize) {
		this.bufSize = bufSize;
	}
	
	public void setSampleRate(int sampleRate) {
		Log.i(TAG, "Setting sample rate to " + sampleRate + " Hz");
		this.sampleRate = sampleRate;
	}
	
	public int getSampleRate() {
		return sampleRate;
	}
}
