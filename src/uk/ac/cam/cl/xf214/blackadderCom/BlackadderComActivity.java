package uk.ac.cam.cl.xf214.blackadderCom;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import uk.ac.cam.cl.xf214.DebugTool.LocalDebugger;
import uk.ac.cam.cl.xf214.blackadderCom.androidVoice.AndroidVoiceProxy;
import uk.ac.cam.cl.xf214.blackadderCom.net.BAPacketSenderSocketAdapter;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAEvent;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAHelper;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAWrapper;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAWrapperNB;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAWrapperShared;
import uk.ac.cam.cl.xf214.blackadderWrapper.Strategy;
import uk.ac.cam.cl.xf214.blackadderWrapper.callback.BlockingQueueCallback;
import uk.ac.cam.cl.xf214.blackadderWrapper.callback.HashClassifierCallback;
import uk.ac.cam.cl.xf214.blackadderWrapper.data.BAItem;
import uk.ac.cam.cl.xf214.blackadderWrapper.data.BAObject;
import uk.ac.cam.cl.xf214.blackadderWrapper.data.BAScope;
import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Toast;

public class BlackadderComActivity extends Activity {
	public static final String TAG = "BlackadderComActivity";
	
	private Node node;
	private AndroidVoiceProxy voiceProxy;
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

	public void runTest() {
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
			sender.finish();
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
    	final CheckBox chkSend = (CheckBox)findViewById(R.id.chk_send);
    	final CheckBox chkReceive = (CheckBox)findViewById(R.id.chk_receive);
    	btnInit.setEnabled(false);
    	chkSend.setEnabled(false);
    	chkReceive.setEnabled(false);
    	
    	userIdInput.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				if (s.length() != 16) {
					btnInit.setEnabled(false);
				} else {
					btnInit.setEnabled(true);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// TODO Auto-generated method stub
				
			}
    		
    	});
    	
    	btnInit.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				byte[] roomId = BAHelper.hexToByte("0000000000000000");
				String clientIdHex = userIdInput.getText().toString();
				if (clientIdHex.length() != 16) {
					return;
				}
		        byte[] clientId = BAHelper.hexToByte(clientIdHex);
		        if (BAHelper.isValidId(clientId, BAObject.getScopeIdLength())) {
		        	node = new Node(roomId, clientId);
		        	voiceProxy = node.getVoiceProxy();
		        	
		        	// disable buttons
		        	userIdInput.post(new Runnable() {
		        		public void run() {
		        			userIdInput.setEnabled(false);
				        	btnInit.setEnabled(false);
				        	chkSend.setEnabled(true);
				        	chkReceive.setEnabled(true);
		        		}
		        	});
		        	
		        } else {
		        	Toast.makeText(getApplicationContext(), "Invalid user ID", Toast.LENGTH_SHORT);
		        }
			}
    	});
    	
    	chkSend.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				voiceProxy.setSend(isChecked);
			}
    	});
    	
    	chkReceive.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				voiceProxy.setReceive(isChecked);
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



/*
// test code
    	final Button btnTest = (Button)findViewById(R.id.btnTest);
    	btnTest.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				btnTest.post(new Runnable() {
					public void run() {
						btnTest.setEnabled(false);
					}
				});
				
				final String TAG = "BATest";
				final byte[] scope = BAHelper.hexToByte("0000000000000000");
				final byte[] item = BAHelper.hexToByte("1111111111111111");
				final byte strat = Strategy.DOMAIN_LOCAL;
				final BAWrapper wrapper = BAWrapper.getWrapper();
				final byte[] payload = new byte[1400];
				Arrays.fill(payload, (byte)5);
				
				Thread handler = new Thread(new Runnable() {
					public void run() {
						BAEvent event;
						while (!Thread.currentThread().isInterrupted()) {
							event = wrapper.getNextEventDirect();
							switch (event.getType()) {
							case SCOPE_PUBLISHED:
								break;
							case SCOPE_UNPUBLISHED:
								break;
							case START_PUBLISH:
								Log.i(TAG, "START_PUBLISH");
								for (int i = 0; i < 5; i++) {
									wrapper.publishData(scope, strat, null, payload	);
									Log.i(TAG, "Item published, length=" + payload.length);
								}
								break;
							case STOP_PUBLISH:
								Log.i(TAG, "STOP_PUBLISH");
								break;
							case PUBLISHED_DATA:
								Log.i(TAG, "Data event received, length=" + event.getDataLength());
								event.freeNativeBuffer();
								break;
							}
						}
					}
				});
				handler.start();
				
				wrapper.publishScope(scope, new byte[0], strat, null);
				wrapper.publishItem(item, scope, strat, null);
				wrapper.subscribeScope(scope, new byte[0], strat, null);
				
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				wrapper.unsubscribeScope(scope, new byte[0], strat, null);
				wrapper.unpublishItem(item, scope, strat, null);
				wrapper.unpublishScope(scope, new byte[0], strat, null);
				
				Log.i(TAG, "Stopping event handler thread");
				handler.interrupt();
				try {
					handler.join(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (handler.getState() == Thread.State.TERMINATED) {
					Log.i(TAG, "Event handler stopped");
				} else {
					Log.e(TAG, "Failed to stop event handler");
				}
				
				Log.i(TAG, "Disconnecting wrapper");
				wrapper.disconnect();
			}
    	});
*/
