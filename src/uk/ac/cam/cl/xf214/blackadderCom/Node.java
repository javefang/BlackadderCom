package uk.ac.cam.cl.xf214.blackadderCom;

import java.util.Arrays;

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
	private byte[] clientId;
	
	private AndroidVoiceProxy voiceProxy;
	
	public Node(byte[] _roomId, byte[] _clientId) {
		wrapper = BAWrapperNB.getWrapper();
		classifier = new HashClassifierCallback();
		
		BAWrapperNB.setCallback(classifier);
		this.clientId = Arrays.copyOf(_clientId, _clientId.length);
		
		roomScope = BAScope.createBAScope(_roomId);
		wrapper.publishScope(roomScope.getId(), roomScope.getPrefix(), STRATEGY, null);
		
		voiceProxy = new AndroidVoiceProxy(wrapper, classifier, _roomId, clientId);
	}
	
	public AndroidVoiceProxy getVoiceProxy() {
		return voiceProxy;
	}
	
	public void finish() {
		voiceProxy.finish();
		wrapper.unpublishScope(roomScope.getId(), roomScope.getPrefix(), STRATEGY, null);
	}
}
