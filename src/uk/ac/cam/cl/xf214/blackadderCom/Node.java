package uk.ac.cam.cl.xf214.blackadderCom;

import java.util.Arrays;

import android.os.PowerManager.WakeLock;

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
	
	public Node(byte[] _roomId, byte[] _clientId, WakeLock _wakeLock) {
		this.roomId = Arrays.copyOf(_roomId, _roomId.length);
		this.clientId = Arrays.copyOf(_clientId, _clientId.length);
		this.wakeLock = _wakeLock;
		this.wrapper = BAWrapperNB.getWrapper();
		this.classifier = new HashClassifierCallback();
		BAWrapperNB.setCallback(classifier);
		this.roomScope = BAScope.createBAScope(_roomId);
		
		wrapper.publishScope(roomScope.getId(), roomScope.getPrefix(), STRATEGY, null);
	}
	
	
	public BAWrapperNB getWrapper() {
		return wrapper;
	}

	public HashClassifierCallback getClassifier() {
		return classifier;
	}

	public BAScope getRoomScope() {
		return roomScope;
	}

	public byte[] getRoomId() {
		return roomId;
	}

	public byte[] getClientId() {
		return clientId;
	}

	public WakeLock getWakeLock() {
		return wakeLock;
	}
	
	public void release() {
		wrapper.unpublishScope(roomScope.getId(), roomScope.getPrefix(), STRATEGY, null);
	}
}
