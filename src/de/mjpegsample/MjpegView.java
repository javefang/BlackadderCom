package de.mjpegsample;

import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MjpegView extends SurfaceView implements SurfaceHolder.Callback {
	public static final String TAG = "MjpegView";
	public static final int POSITION_UPPER_LEFT = 9;
	public static final int POSITION_UPPER_RIGHT = 3;
	public static final int POSITION_LOWER_LEFT = 12;
	public static final int POSITION_LOWER_RIGHT = 6;

	public static final int SIZE_STANDARD = 1;
	public static final int SIZE_BEST_FIT = 4;
	public static final int SIZE_FULLSCREEN = 8;
	
	public static final int FRAME_DROP_THRESHOLD = 3;

	private MjpegViewThread thread;
	private MjpegDataInput mIn = null;
	//private FrameSkipController mFrameSkipCtrl = null;
	private OnErrorListener mOnErrorListener;
	private boolean showFps = false;
	private boolean mRun = false;
	private boolean mPause = false;
	private boolean surfaceDone = false;
	private Paint overlayPaint;
	private int overlayTextColor;
	private int overlayBackgroundColor;
	private int ovlPos;
	private int dispWidth;
	private int dispHeight;
	private int displayMode;

	private Paint noSignalPaint;
	private String noSignalText;
	
	private Object pauseWait;
	
	private int mViewId;

	public class MjpegViewThread extends Thread {
		private SurfaceHolder mSurfaceHolder;
		private int frameCounter = 0;
		private long start;
		private Bitmap ovl;

		public MjpegViewThread(SurfaceHolder surfaceHolder, Context context) {
			mSurfaceHolder = surfaceHolder;
		}

		private Rect destRect(int bmw, int bmh) {
			int tempx;
			int tempy;
			if (displayMode == MjpegView.SIZE_STANDARD) {
				tempx = (dispWidth / 2) - (bmw / 2);
				tempy = (dispHeight / 2) - (bmh / 2);
				return new Rect(tempx, tempy, bmw + tempx, bmh + tempy);
			}
			if (displayMode == MjpegView.SIZE_BEST_FIT) {
				float bmasp = (float) bmw / (float) bmh;
				bmw = dispWidth;
				bmh = (int) (dispWidth / bmasp);
				if (bmh > dispHeight) {
					bmh = dispHeight;
					bmw = (int) (dispHeight * bmasp);
				}
				tempx = (dispWidth / 2) - (bmw / 2);
				tempy = (dispHeight / 2) - (bmh / 2);
				return new Rect(tempx, tempy, bmw + tempx, bmh + tempy);
			}
			if (displayMode == MjpegView.SIZE_FULLSCREEN)
				return new Rect(0, 0, dispWidth, dispHeight);
			return null;
		}

		public void setSurfaceSize(int width, int height) {
			synchronized (mSurfaceHolder) {
				dispWidth = width;
				dispHeight = height;
			}
		}

		private Bitmap makeFpsOverlay(Paint p, String text) {
			Rect b = new Rect();
			p.getTextBounds(text, 0, text.length(), b);
			int bwidth = b.width() + 2;
			int bheight = b.height() + 2;
			Bitmap bm = Bitmap.createBitmap(bwidth, bheight,
					Bitmap.Config.ARGB_8888);
			Canvas c = new Canvas(bm);
			p.setColor(overlayBackgroundColor);
			c.drawRect(0, 0, bwidth, bheight, p);
			p.setColor(overlayTextColor);
			c.drawText(text, -b.left + 1,
					(bheight / 2) - ((p.ascent() + p.descent()) / 2) + 1, p);
			return bm;
		}

		public void run() {
			start = System.currentTimeMillis();
			PorterDuffXfermode mode = new PorterDuffXfermode(
					PorterDuff.Mode.DST_OVER);
			Bitmap bm;
			
			int width;
			int height;
			Rect destRect;
			Canvas c = null;
			Paint p = new Paint();
			String fps = "";
			Log.i(TAG, "Starting playback...");
			//mFrameSkipCtrl.start();
			while (mRun) {
				if (surfaceDone) {
					if (mPause) {
						try {
							doPause();
							continue;
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							break;
						}
					}
					try {
						c = mSurfaceHolder.lockCanvas();
						synchronized (mSurfaceHolder) {
							try {
								//Log.i(TAG, "Waiting on readMjpegFrame()..." + System.currentTimeMillis());
								bm = mIn.readMjpegFrame();
								
								if (bm == null) {
									Log.i(TAG, "Invalid bitmap, skip to next loop");
									continue;
								}
								//Log.i(TAG, "Got bitmap!");
								destRect = destRect(bm.getWidth(),
										bm.getHeight());
								c.drawColor(Color.BLACK);
								//Log.i(TAG, "Drawing frame... " + System.currentTimeMillis());
								c.drawBitmap(bm, null, destRect, p);
								if (showFps) {
									p.setXfermode(mode);
									if (ovl != null) {
										height = ((ovlPos & 1) == 1) ? destRect.top
												: destRect.bottom
														- ovl.getHeight();
										width = ((ovlPos & 8) == 8) ? destRect.left
												: destRect.right
														- ovl.getWidth();
										c.drawBitmap(ovl, width, height, null);
									}
									p.setXfermode(null);
									frameCounter++;
									if ((System.currentTimeMillis() - start) >= 1000) {
										fps = String.valueOf(frameCounter)
												+ "fps";
										frameCounter = 0;
										start = System.currentTimeMillis();
										ovl = makeFpsOverlay(overlayPaint, fps);
									}
								}
								bm.recycle();
							} catch (IOException e) {
								Log.e(TAG, "run(): IOException caught: " + e.getMessage());
								pausePlayback();
							} catch (InterruptedException e) {
								Log.e(TAG, "run(): InterruptedException caught: " + e.getMessage());
								pausePlayback();
							}
						}
					} finally {
						if (c != null)
							mSurfaceHolder.unlockCanvasAndPost(c);
					}
				}
			}
			Log.i(TAG, "MjpegView thread stopped");
		}
	}

	private void init(Context context) {
		pauseWait = new Object();
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		thread = new MjpegViewThread(holder, context);	// call this on startPlayback()
		setFocusable(true);
		overlayPaint = new Paint();
		overlayPaint.setTextAlign(Paint.Align.LEFT);
		overlayPaint.setTextSize(12);
		overlayPaint.setTypeface(Typeface.DEFAULT);
		overlayTextColor = Color.WHITE;
		overlayBackgroundColor = Color.BLACK;
		ovlPos = MjpegView.POSITION_LOWER_RIGHT;
		displayMode = MjpegView.SIZE_STANDARD;
		dispWidth = getWidth();
		dispHeight = getHeight();
		
		// no signal message
		noSignalText = "NO SIGNAL";
		noSignalPaint = new Paint();
		noSignalPaint.setTextSize(30);
		noSignalPaint.setColor(Color.WHITE);
	}

	public void startPlayback() throws RuntimeException {
		if (mIn != null && mOnErrorListener != null) {
			switch (thread.getState()) {
			case NEW:
				Log.i(TAG, "Starting new playback!");
				mRun = true;
				thread.start();
				break;
			case TERMINATED:
				throw new RuntimeException("MjpegView already stopped and destroyed!");
			default:
				if (mPause) {
					Log.i(TAG, "Resuming playback!");
					mPause = false;
					synchronized(pauseWait) {
						pauseWait.notifyAll();
					}
				} else {
					Log.i(TAG, "Already started");
				}
			}
		} else {
			throw new RuntimeException("Source or OnErrorListener not set");
		}
	}
	
	/* normally this method should be called if this view will be further used */
	public void pausePlayback() {
		if (mRun) {
			Log.i(TAG, "pausePlayback()");
			/* pause */
			mPause = true;
		} else {
			Log.i(TAG, "Not started");
		}
	}
	
	private void doPause() throws InterruptedException {
		Log.i(TAG, "doPause()");
		mIn = null;
		// notify videoplayer
		mOnErrorListener.onError();
		mOnErrorListener = null;
		
		// draw no signal
		drawNoSignal();
		
		try {
			synchronized(pauseWait) {
				Log.i(TAG, "drawing thread paused");
				pauseWait.wait();
				Log.i(TAG, "drawing thread resumed!");
			}
		} catch (InterruptedException e) {
			Log.e(TAG, "doPause(): InterruptedException caught");
		}
	}

	/* cannot restart after stop */
	public void stopPlayback() {
		if (mRun) {
			mRun = false;
			boolean retry = true;
			while (retry) {
				mIn = null;
				synchronized(pauseWait) {
					pauseWait.notify();
				}
				try {
					thread.join();
				} catch (InterruptedException e) {
					Log.e(TAG, "stopPlayback(): InterruptedException caught");
				}
				retry = false;
			}
			mIn = null;
			//mFrameSkipCtrl = null;
			thread = null;
			Log.i(TAG, "drawing no signal...");
			drawNoSignal();
			Log.i(TAG, "stopped");
		}
	}
	
	private void drawNoSignal() {
		// draw no signal text
		SurfaceHolder holder = getHolder();
		if (holder != null) {
			Canvas canvas = holder.lockCanvas();
			if (canvas != null) {
				canvas.drawColor(Color.BLACK);
				canvas.drawText(noSignalText, 50, 50, noSignalPaint);
				holder.unlockCanvasAndPost(canvas);
			}
		}
	}

	public MjpegView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public void surfaceChanged(SurfaceHolder holder, int f, int w, int h) {
		thread.setSurfaceSize(w, h);
		drawNoSignal();
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		surfaceDone = false;
		stopPlayback();
	}

	public MjpegView(Context context) {
		super(context);
		init(context);
	}

	public void surfaceCreated(SurfaceHolder holder) {
		surfaceDone = true;
	}

	public void showFps(boolean b) {
		showFps = b;
	}

	public void setSource(MjpegDataInput source) {
		if (source == null) {
			throw new NullPointerException("Source MjpegInputStream cannot be NULL!");
		}
		mIn = source;
		//mFrameSkipCtrl = new FrameSkipController(source);
		//startPlayback();
	}
	
	public void setOnErrorListener(OnErrorListener oel) {
		mOnErrorListener = oel;
	}

	public void setOverlayPaint(Paint p) {
		overlayPaint = p;
	}

	public void setOverlayTextColor(int c) {
		overlayTextColor = c;
	}

	public void setOverlayBackgroundColor(int c) {
		overlayBackgroundColor = c;
	}

	public void setOverlayPosition(int p) {
		ovlPos = p;
	}
	
	public void setNoSignalText(String text) {
		noSignalText = text;
	}

	public void setDisplayMode(int s) {
		displayMode = s;
	}
	
	public void setViewId(int viewId) {
		mViewId = viewId;
	}
	
	public int getViewId() {
		return mViewId;
	}
}