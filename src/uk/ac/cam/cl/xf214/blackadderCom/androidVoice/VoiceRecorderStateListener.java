package uk.ac.cam.cl.xf214.blackadderCom.androidVoice;

public interface VoiceRecorderStateListener {
	public void updateMicRMSVolume(double rms);
	public void recorderTerminated();
}
