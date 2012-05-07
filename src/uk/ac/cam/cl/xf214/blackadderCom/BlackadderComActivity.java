package uk.ac.cam.cl.xf214.blackadderCom;

import de.mjpegsample.MjpegView;
import uk.ac.cam.cl.xf214.DebugTool.LocalDebugger;
import uk.ac.cam.cl.xf214.blackadderCom.androidVideo.VideoProxy;
import uk.ac.cam.cl.xf214.blackadderCom.androidVoice.VoiceProxy;
import uk.ac.cam.cl.xf214.blackadderCom.androidVoice.VoiceProxy.VoiceCodec;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAHelper;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAWrapperShared;
import uk.ac.cam.cl.xf214.blackadderWrapper.data.BAObject;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
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
	
	static {
		loadJNILibraries();
	}
	
	private MjpegView[] views;
	private SurfaceView preview;
	
	private Node node;
	private VoiceProxy voiceProxy;
	private VideoProxy videoProxy;
	private WakeLock wakeLock;
	//private boolean finished;
	private EditText userIdInput;
	private Button btnInit;
	private ToggleButton tbSend;
	private ToggleButton tbRecv;
	private RadioGroup codecSelect;
	private ImageButton btnPTT;
	private ToggleButton tbSendVideo;
	private ToggleButton tbRecvVideo;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LocalDebugger.setDebugger(new AndroidDebugger());	// set debugger
        int pid= android.os.Process.myPid();
        Log.i(TAG, "onCreate() pid=" + pid);
        setContentView(R.layout.main_landscape);
        initVoiceUI(); 
        initVideoUI();
        setUIEnabled(btnInit, false);
        
        //runTest();
    }
    
    private boolean connect(String roomIdHex, String clientIdHex) {
    	byte[] roomId = BAHelper.hexToByte(roomIdHex);
    	byte[] clientId = BAHelper.hexToByte(clientIdHex);
        if (BAHelper.isValidId(clientId, BAObject.getScopeIdLength())) {
        	PowerManager powMan = (PowerManager)getSystemService(Context.POWER_SERVICE);
        	wakeLock = powMan.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "BlackadderComWL");
        	wakeLock.setReferenceCounted(true);
        	node = new Node(roomId, clientId, wakeLock);
        	voiceProxy = new VoiceProxy(node);
        	videoProxy = new VideoProxy(node, views, preview);
        	setUIEnabled(btnInit, true);
        	return true;
        } else {
        	Toast.makeText(getApplicationContext(), "Invalid user ID", Toast.LENGTH_SHORT);
        	return false;
        }
       
    }
    
    private void setUIEnabled(View v, final boolean enabled) {
		// disable buttons
    	v.post(new Runnable() {
    		public void run() {
    			userIdInput.setEnabled(!enabled);
	        	btnInit.setEnabled(!enabled);
	        	tbSend.setEnabled(enabled);
	        	tbRecv.setEnabled(enabled);
	        	btnPTT.setEnabled(enabled);
	        	
	        	tbSendVideo.setEnabled(enabled);
	        	tbRecvVideo.setEnabled(enabled);
	        	
	        	/* KEEP DISABLED */
	        	for(int i = 0; i < codecSelect.getChildCount(); i++){
	        	    ((RadioButton)codecSelect.getChildAt(i)).setEnabled(false);
	        	}
    		}
    	});
    }
    
    
    private void initVideoUI() {
    	views = new MjpegView[3];
    	int[] viewId = {R.id.video1, R.id.video2, R.id.video3};
    	
    	for (int i = 0; i < views.length; i++) {	
    		views[i] = (MjpegView)findViewById(viewId[i]);
    		SurfaceHolder holder = views[i].getHolder();
    	}
    	
    	preview = (SurfaceView)findViewById(R.id.preview);
    	preview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);// only the last one is set as PUSH_BUFFERS 
    	preview.setZOrderMediaOverlay(true);
    	
    	tbSendVideo = (ToggleButton)findViewById(R.id.tb_sendVideo);
    	tbRecvVideo = (ToggleButton)findViewById(R.id.tb_recvVideo);
    	
    	tbSendVideo.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				videoProxy.setSend(isChecked);
			}
    	});
    	
    	tbRecvVideo.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				videoProxy.setReceive(isChecked);
			}
    	});
    }
    
    private void initVoiceUI() {
    	userIdInput = (EditText)findViewById(R.id.clientIdInput);
    	btnInit = (Button)findViewById(R.id.btn_init);
    	tbSend = (ToggleButton)findViewById(R.id.tb_send);
    	tbRecv = (ToggleButton)findViewById(R.id.tb_recv);
    	codecSelect = (RadioGroup)findViewById(R.id.rg_codec);
    	btnPTT = (ImageButton)findViewById(R.id.btn_ptt);    	
    	
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
		if (videoProxy != null) {
			videoProxy.release();
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
	
	private static void loadJNILibraries() {
    	String sharedObjPath = "/data/data/uk.ac.cam.cl.xf214.blackadderCom/lib/";
		System.load(sharedObjPath + "libgnustl_shared.so");
		System.load(sharedObjPath + "libblackadder.so");

		BAWrapperShared.configureObjectFile(sharedObjPath
				+ "libuk_ac_cam_cl_xf214_blackadderWrapper.so");
		Log.i(TAG, "JNI shared libraries loaded");
    }
}
