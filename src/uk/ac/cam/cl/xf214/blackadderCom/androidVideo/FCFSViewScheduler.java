package uk.ac.cam.cl.xf214.blackadderCom.androidVideo;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Comparator;

import android.util.Log;

import de.mjpegsample.MjpegView;

/**
 * FCFSViewScheduler
 * Available views will be allocated in a first-come-first-served principle. 
 * A VideoPlayer not be allocated a view will be placed into a wait queue,
 * the waiting VideoPlayer will be allocated a view as long as there is any becomes available
 * 
 * @author Xinghong Fang
 *
 */
public class FCFSViewScheduler implements ViewScheduler {
	public static final String TAG = "FCFSViewScheduler";
	private LinkedList<VideoPlayer> mPlayerWaitQueue;
	//private LinkedList<MjpegView> mViewQueue;
	private PriorityQueue<MjpegView> mViewQueue;
	
	private HashSet<VideoPlayer> mAllPlayer;
	private MjpegView[] mViews;
	
	public FCFSViewScheduler(MjpegView[] views) {
		mViews = views;
		//mViewQueue = new LinkedList<MjpegView>();
		mViewQueue = new PriorityQueue<MjpegView>(views.length, new Comparator<MjpegView>() {
			public int compare(MjpegView lhs, MjpegView rhs) {
				return lhs.getViewId() - rhs.getViewId();
			}
		});
		initViewQueue();
		mPlayerWaitQueue = new LinkedList<VideoPlayer>();
		mAllPlayer = new HashSet<VideoPlayer>();
	}
	
	@Override
	public synchronized void addStream(VideoPlayer player) {
		Log.i(TAG, "addStream(): " + player);
		if (!mAllPlayer.contains(player)) {
			// add player to the set if it has not been previously added
			Log.i(TAG, "Add player to set");
			mAllPlayer.add(player);
			if (!mViewQueue.isEmpty()) {
				// views are available, assign one to the player
				Log.i(TAG, "Assign new view to player!");
				assignNewView(mViewQueue.poll(), player);
			} else {
				// no available view yet, add player to wait queue
				Log.i(TAG, "No views available, add player to wait queue");
				mPlayerWaitQueue.add(player);
			}
		} else {
			Log.i(TAG, "Cannot addStream(), player already added: " + player);
		}
		Log.i(TAG, "stream added");
	}

	@Override
	public synchronized void removeStream(VideoPlayer player) {
		if (mAllPlayer.contains(player)) {
			mAllPlayer.remove(player);
			assignNewView(null, player);
			if (!mPlayerWaitQueue.isEmpty() && !mViewQueue.isEmpty()) {
				// if there is player waiting for a view and there is available view to use
				assignNewView(mViewQueue.poll(), mPlayerWaitQueue.poll());
			}
		} else {
			Log.i(TAG, "Player not exists: " + player);
		}
	}

	@Override
	@Deprecated
	public synchronized void clear() {
		// unassign views from all players
		for (VideoPlayer vp : mAllPlayer) {
			assignNewView(null, vp);
		}
		mPlayerWaitQueue.clear();
		mAllPlayer.clear();
		initViewQueue();
	}
	
	public synchronized void release() {
		clear();
		for (MjpegView m : mViews) {
			m.stopPlayback();
		}
	}
	
	private void initViewQueue() {
		for (MjpegView view : mViews) {
			mViewQueue.offer(view);
		}
	}
	
	private void assignNewView(MjpegView newView, VideoPlayer player) {
		Log.i(TAG, "Assigning " + newView + " to player " + player);
		MjpegView oldView = player.setView(newView);
		// recycle old view if not null
		if (oldView != null) {
			mViewQueue.offer(oldView);
			Log.i(TAG, "Recycling view " + oldView);
		}
	}

}
