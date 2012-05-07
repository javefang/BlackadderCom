package uk.ac.cam.cl.xf214.blackadderCom.androidVideo;

public interface ViewScheduler {
	public void addStream(VideoPlayer player);
	public void removeStream(VideoPlayer player);
	public void clear();
}
