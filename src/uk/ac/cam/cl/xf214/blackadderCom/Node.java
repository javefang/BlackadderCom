package uk.ac.cam.cl.xf214.blackadderCom;

import java.util.Arrays;

import android.os.PowerManager.WakeLock;

import uk.ac.cam.cl.xf214.blackadderCom.androidVoice.AndroidVoiceProxy;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAWrapperNB;
import uk.ac.cam.cl.xf214.blackadderWrapper.Strategy;
import uk.ac.cam.cl.xf214.blackadderWrapper.callback.HashClassifierCallback;
import uk.ac.cam.cl.xf214.blackadderWrapper.data.BAScope;


public class Node {
	public static final byte STRATEGY = Strategy.DOMAIN_LOCAL;
	
	private BAWrapperNB wrapper;
	private HashClassifierCallback classifier;
	
	private BAScope roomScope;
	private byte[] roomId;
	private byte[] clientId;
	private WakeLock wakeLock;
	
	private AndroidVoiceProxy voiceProxy;
	
	public Node(byte[] _roomId, byte[] _clientId, WakeLock _wakeLock) {
		this.roomId = Arrays.copyOf(_roomId, _roomId.length);
		this.clientId = Arrays.copyOf(_clientId, _clientId.length);
		this.wakeLock = _wakeLock;
		
		wrapper = BAWrapperNB.getWrapper();
		classifier = new HashClassifierCallback();
		
		BAWrapperNB.setCallback(classifier);
		this.clientId = Arrays.copyOf(_clientId, _clientId.length);
		
		roomScope = BAScope.createBAScope(_roomId);
		wrapper.publishScope(roomScope.getId(), roomScope.getPrefix(), STRATEGY, null);
		
		voiceProxy = new AndroidVoiceProxy(wrapper, classifier, _roomId, clientId, wakeLock);
	}
	
	public AndroidVoiceProxy getVoiceProxy() {
		return voiceProxy;
	}
	
	public void finish() {
		voiceProxy.release();
		wrapper.unpublishScope(roomScope.getId(), roomScope.getPrefix(), STRATEGY, null);
	}
}
