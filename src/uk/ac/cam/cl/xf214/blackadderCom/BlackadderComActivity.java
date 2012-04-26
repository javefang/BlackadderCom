package uk.ac.cam.cl.xf214.blackadderCom;

import uk.ac.cam.cl.xf214.DebugTool.LocalDebugger;
import uk.ac.cam.cl.xf214.blackadderCom.androidVideo.AndroidVideoProxy;
import uk.ac.cam.cl.xf214.blackadderCom.androidVoice.AndroidVoiceProxy;
import uk.ac.cam.cl.xf214.blackadderCom.androidVoice.AndroidVoiceProxy.VoiceCodec;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAHelper;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAWrapperShared;
import uk.ac.cam.cl.xf214.blackadderWrapper.data.BAObject;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.view.ViewPager.LayoutParams;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.ToggleButton;

public class BlackadderComActivity extends Activity {
	public static final String TAG = "BlackadderComActivity";
	public static final String DEFAULT_ROOM_ID = "0000000000000000";
	
	private SurfaceView[] views;
	
	private Node node;
	private AndroidVoiceProxy voiceProxy;
	private AndroidVideoProxy videoProxy;
	private WakeLock wakeLock;
	//private boolean finished;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LocalDebugger.setDebugger(new AndroidDebugger());	// set debugger
        int pid= android.os.Process.myPid();
        Log.i(TAG, "onCreate() pid=" + pid);
        setContentView(R.layout.main);
        loadJNILibraries();
        initVoiceUI(); 
        initVideoUI();
    }
    
    private boolean connect(String roomIdHex, String clientIdHex) {
    	byte[] roomId = BAHelper.hexToByte(roomIdHex);
    	byte[] clientId = BAHelper.hexToByte(clientIdHex);
        if (BAHelper.isValidId(clientId, BAObject.getScopeIdLength())) {
        	PowerManager powMan = (PowerManager)getSystemService(Context.POWER_SERVICE);
        	wakeLock = powMan.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "BlackadderComWL");
        	wakeLock.setReferenceCounted(true);
        	node = new Node(roomId, clientId, wakeLock);
        	voiceProxy = new AndroidVoiceProxy(node);
        	videoProxy = new AndroidVideoProxy(node, views);
        	return true;
        } else {
        	Toast.makeText(getApplicationContext(), "Invalid user ID", Toast.LENGTH_SHORT);
        	return false;
        }
    }
    
    
    private void initVideoUI() {
    	views = new SurfaceView[4];
    	int[] viewId = {R.id.view1, R.id.view2, R.id.view3, R.id.view4};
    	for (int i = 0; i < 4; i++) {
    		views[i] = (SurfaceView)findViewById(viewId[i]);
    		views[i].getHolder().addCallback(new Callback() {
				public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
				public void surfaceDestroyed(SurfaceHolder holder) {}
				public void surfaceCreated(SurfaceHolder holder) {
					holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
				}
    		});
    	}
    	
    	
    	/*
    	for (int i = 0; i < views.length; i++) {
    		final SurfaceView sv = views[i];
    		Log.i(TAG, "Starting video " + i);
    		views[i].getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    		views[i].getHolder().addCallback(new Callback() {
				@Override
				public void surfaceChanged(SurfaceHolder holder, int format,
						int width, int height) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void surfaceCreated(SurfaceHolder holder) {
					MediaPlayer mp = new MediaPlayer();
	    			try {
						mp.setDataSource("/sdcard/media/sample.mp4");
						mp.setDisplay(holder);
		    			mp.prepare();
		    			mp.start();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				@Override
				public void surfaceDestroyed(SurfaceHolder holder) {
					// TODO Auto-generated method stub
					
				}
    			
    		});
    	}
    	*/
    	
    	
    }
    
    private void initVoiceUI() {
    	final EditText userIdInput = (EditText)findViewById(R.id.clientIdInput);
    	final Button btnInit = (Button)findViewById(R.id.btn_init);
    	final ToggleButton tbSend = (ToggleButton)findViewById(R.id.tb_send);
    	final ToggleButton tbRecv = (ToggleButton)findViewById(R.id.tb_recv);
    	final RadioGroup codecSelect = (RadioGroup)findViewById(R.id.rg_codec);
    	final ImageButton btnPTT = (ImageButton)findViewById(R.id.btn_ptt);
    	
    	btnInit.setEnabled(false);
    	tbSend.setEnabled(false);
    	tbRecv.setEnabled(false);
    	btnPTT.setEnabled(false);
    	for(int i = 0; i < codecSelect.getChildCount(); i++){
    	    ((RadioButton)codecSelect.getChildAt(i)).setEnabled(false);
    	}
    	
    	
    	userIdInput.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
				if (s.length() != 16) {
					btnInit.setEnabled(false);
				} else {
					btnInit.setEnabled(true);
				}
			}
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
    	});
    	
    	userIdInput.setText(getClientId());
    	
    	btnInit.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String clientIdHex = userIdInput.getText().toString();
				if (clientIdHex.length() != 16) {
					return;
				}
		        connect(DEFAULT_ROOM_ID, clientIdHex);
		        
		        // disable buttons
	        	v.post(new Runnable() {
	        		public void run() {
	        			userIdInput.setEnabled(false);
			        	btnInit.setEnabled(false);
			        	tbSend.setEnabled(true);
			        	tbRecv.setEnabled(true);
			        	btnPTT.setEnabled(true);
			        	for(int i = 0; i < codecSelect.getChildCount(); i++){
			        	    ((RadioButton)codecSelect.getChildAt(i)).setEnabled(true);
			        	}
	        		}
	        	});
			}
    	});
    	
    	tbSend.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				voiceProxy.setSend(isChecked);
			}
    	});
    	
    	tbRecv.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				voiceProxy.setReceive(isChecked);
			}
    	});
    	
    	codecSelect.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				switch (checkedId) {
				case R.id.rb_pcm:
					voiceProxy.setCodec(VoiceCodec.PCM);
					break;
				case R.id.rb_speex:
					voiceProxy.setCodec(VoiceCodec.SPEEX);
					break;
				}
			}
    	});
    	
    	btnPTT.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					Log.i(TAG, "TALKING...");
					tbSend.setChecked(true);
					break;
				case MotionEvent.ACTION_UP:
					Log.i(TAG, "STOP TALKING!");
					tbSend.setChecked(false);
					break;
				}
				return true;
			}
    	});
    }
    
    @Override
	protected void onDestroy() {
		super.onDestroy();
		//finished = true;
		Log.i(TAG, "onDestroy()");
		if (voiceProxy != null) {
			voiceProxy.release();
		}
		if (node != null) {
			node.release();
		}
		
		if (wakeLock != null) {
			wakeLock.setReferenceCounted(false);
			if (wakeLock != null && wakeLock.isHeld()) {
				wakeLock.release();
				Log.i(TAG, "All wakelocks released!");
			}
		}
		// TODO: do not disconnect (delete NB_Blackadder), process and VM is not shutdown onDestroy(),  
		//BAWrapperNB.getWrapper().disconnect();
	}
    
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
		Log.i(TAG, "onConfigurationChanged()");
	}

	private String getClientId() {
    	TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
    	String clientIdStr = (telephonyManager.getDeviceId() + "C").substring(0, 16);	// ensure clientId is 16 char long
    	Log.i(TAG, "Client ID is " + clientIdStr);
    	return clientIdStr;
    }
	
	private void loadJNILibraries() {
    	String sharedObjPath = "/data/data/uk.ac.cam.cl.xf214.blackadderCom/lib/";
		System.load(sharedObjPath + "libgnustl_shared.so");
		System.load(sharedObjPath + "libblackadder.so");

		BAWrapperShared.configureObjectFile(sharedObjPath
				+ "libuk_ac_cam_cl_xf214_blackadderWrapper.so");
		Log.i(TAG, "JNI shared libraries loaded");
    }
}
