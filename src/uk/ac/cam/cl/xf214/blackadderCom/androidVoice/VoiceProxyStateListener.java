package uk.ac.cam.cl.xf214.blackadderCom.androidVoice;

import uk.ac.cam.cl.xf214.blackadderWrapper.data.BAScope;

public interface VoiceProxyStateListener {
	public void playerCreated(AndroidVoicePlayer player);
	public void recorderCreated(AndroidVoiceRecorder recorder);
}
