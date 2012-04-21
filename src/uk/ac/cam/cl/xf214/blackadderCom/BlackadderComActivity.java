package uk.ac.cam.cl.xf214.blackadderCom;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import uk.ac.cam.cl.xf214.DebugTool.LocalDebugger;
import uk.ac.cam.cl.xf214.blackadderCom.androidVoice.AndroidVoiceProxy;
import uk.ac.cam.cl.xf214.blackadderCom.androidVoice.AndroidVoiceProxy.VoiceCodec;
import uk.ac.cam.cl.xf214.blackadderCom.net.BAPacketSenderSocketAdapter;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAHelper;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAWrapperNB;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAWrapperShared;
import uk.ac.cam.cl.xf214.blackadderWrapper.Strategy;
import uk.ac.cam.cl.xf214.blackadderWrapper.callback.HashClassifierCallback;
import uk.ac.cam.cl.xf214.blackadderWrapper.data.BAItem;
import uk.ac.cam.cl.xf214.blackadderWrapper.data.BAObject;
import uk.ac.cam.cl.xf214.blackadderWrapper.data.BAScope;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
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
	
	private Node node;
	private AndroidVoiceProxy voiceProxy;
	private WakeLock wakeLock;
	//private boolean finished;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocalDebugger.setDebugger(new AndroidDebugger());	// set debugger
        int pid= android.os.Process.myPid();
        Log.i(TAG, "onCreate() pid=" + pid);
        setContentView(R.layout.main);
        
        loadJNILibraries();
        initUI(); 
        //runTest();
    }
    
    private String getClientId() {
    	TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
    	String clientIdStr = (telephonyManager.getDeviceId() + "C").substring(0, 16);	// ensure clientId is 16 char long
    	Log.i(TAG, "Client ID is " + clientIdStr);
    	return clientIdStr;
    }
    
    @Override
	protected void onPause() {
		super.onPause();
		Log.i(TAG, "onPause()");
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume()");
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.i(TAG, "onStart()");
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.i(TAG, "onStop()");
	}

	private void runTest() {
    	LocalDebugger.setDebugger(new AndroidDebugger());
    	LocalDebugger.print(TAG, "LocalDebugger set to AndroidDebugger");
    	
    	Log.i(TAG, "BA TEST BEGIN");
    	BAWrapperNB wrapper = BAWrapperNB.getWrapper();
    	String scopeHex = "0000000000000000";
    	String itemHex = "1111111111111111";
    	
    	//Log.i(TAG, "Init and set HashClassifierCallback");
    	//HashClassifierCallback callback = new HashClassifierCallback();
    	Log.i(TAG, "Init and set HashClassifierCallback");
    	final HashClassifierCallback callback = new HashClassifierCallback();
    	
    	BAWrapperNB.setCallback(callback);
    		
    	byte strat = Strategy.DOMAIN_LOCAL;
    	byte[] scope = BAHelper.hexToByte(scopeHex);
    	byte[] item = BAHelper.hexToByte(itemHex);
    	BAScope baScope = BAScope.createBAScope(scope);
    	BAItem baItem = BAItem.createBAItem(item, baScope);
    	
    	Log.i(TAG, "Publishing scope and item");
    	wrapper.publishScope(scope, new byte[0], strat, null);
    	wrapper.publishItem(item, scope, strat, null);
    	
    	try {
			BAPacketSenderSocketAdapter sender = new BAPacketSenderSocketAdapter(wrapper, callback, baItem);
			Log.i(TAG, "Waiting 3sec for sender to initialize");
			Thread.sleep(3000);
			OutputStream out = sender.getOutputStream();
			byte[] payload = new byte[1400];
			Arrays.fill(payload, (byte)5);
			for (int i = 0; i < 5; i++) {
				Log.i(TAG, "Sending payload " + i);
				out.write(payload);
				out.flush();
			}
			Log.i(TAG, "Sleep 3sec for sending to finish");
			Thread.sleep(3000);
			sender.release();
			Log.i(TAG, "Test complete!");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.e(TAG, "IOException caught: " + e.getMessage());
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			Log.e(TAG, "InterruptedException caught");
			e.printStackTrace();
		}
    		
    	Log.i(TAG, "Unpublishing item and scope");
    	wrapper.unpublishItem(item, scope, strat, null);
    	wrapper.unpublishScope(scope, new byte[0], strat, null);
    }
    
    private void initUI() {
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
				byte[] roomId = BAHelper.hexToByte("0000000000000000");
				String clientIdHex = userIdInput.getText().toString();
				if (clientIdHex.length() != 16) {
					return;
				}
		        byte[] clientId = BAHelper.hexToByte(clientIdHex);
		        if (BAHelper.isValidId(clientId, BAObject.getScopeIdLength())) {
		        	PowerManager powMan = (PowerManager)getSystemService(Context.POWER_SERVICE);
		        	wakeLock = powMan.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "BlackadderComWL");
		        	wakeLock.setReferenceCounted(true);
		        	node = new Node(roomId, clientId, wakeLock);
		        	voiceProxy = node.getVoiceProxy();
		        	
		        	// disable buttons
		        	userIdInput.post(new Runnable() {
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
		        	
		        } else {
		        	Toast.makeText(getApplicationContext(), "Invalid user ID", Toast.LENGTH_SHORT);
		        }
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
		if (node != null) {
			node.finish();
		}
		
		if (wakeLock != null && wakeLock.isHeld()) {
			wakeLock.setReferenceCounted(false);
			wakeLock.release();
			Log.i(TAG, "All wakelocks released!");
		}
		// TODO: do not disconnect (delete NB_Blackadder), process and VM is not shutdown onDestroy(),  
		//BAWrapperNB.getWrapper().disconnect();
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
