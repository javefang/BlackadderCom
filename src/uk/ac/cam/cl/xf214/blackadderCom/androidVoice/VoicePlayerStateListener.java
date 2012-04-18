package uk.ac.cam.cl.xf214.blackadderCom.androidVoice;

public interface VoicePlayerStateListener {
	public void updateRMSVolume(double rms);
	public void playerTerminated();
}
