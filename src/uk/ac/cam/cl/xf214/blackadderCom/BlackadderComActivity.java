package uk.ac.cam.cl.xf214.blackadderCom;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.mjpegsample.MjpegView;
import de.mjpegsample.NativeJpegLib;
import uk.ac.cam.cl.xf214.DebugTool.LocalDebugger;
import uk.ac.cam.cl.xf214.blackadderCom.androidVideo.VideoProxy;
import uk.ac.cam.cl.xf214.blackadderCom.androidVideo.VideoRecorder;
import uk.ac.cam.cl.xf214.blackadderCom.androidVoice.VoiceProxy;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAHelper;
import uk.ac.cam.cl.xf214.blackadderWrapper.BAWrapperShared;
import uk.ac.cam.cl.xf214.blackadderWrapper.data.BAObject;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
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
	
	private BANode node;
	private VoiceProxy voiceProxy;
	private VideoProxy videoProxy;
	//private SpeedTestProxy speedProxy;
	private WakeLock wakeLock;
	//private boolean finished;
	private EditText userIdInput;
	private Button btnInit;
	private ToggleButton tbSend;
	private ToggleButton tbRecv;
	//private RadioGroup codecSelect;
	private Spinner sampleRateSelect;
	private ImageButton btnPTT;
	private ToggleButton tbSendVideo;
	private ToggleButton tbRecvVideo;
	private Spinner videoSizeSelect;
	private SeekBar videoQualityBar;
	
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
        //initSpeedUI();
        setUIEnabled(btnInit, false);
        
        runTest(false);
    }
    
    private boolean connect(String roomIdHex, String clientIdHex) {
    	byte[] roomId = BAHelper.hexToByte(roomIdHex);
    	byte[] clientId = BAHelper.hexToByte(clientIdHex);
        if (BAHelper.isValidId(clientId, BAObject.getScopeIdLength())) {
        	PowerManager powMan = (PowerManager)getSystemService(Context.POWER_SERVICE);
        	wakeLock = powMan.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "BlackadderComWL");
        	wakeLock.setReferenceCounted(true);
        	node = new BANode(roomId, clientId, wakeLock);
        	voiceProxy = new VoiceProxy(node);
        	videoProxy = new VideoProxy(node, views, preview);
        	//speedProxy = new SpeedTestProxy(node);
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
	        	sampleRateSelect.setEnabled(enabled);
	        	btnPTT.setEnabled(enabled);
	        	
	        	tbSendVideo.setEnabled(enabled);
	        	tbRecvVideo.setEnabled(enabled);
	        	videoSizeSelect.setEnabled(enabled);
	        	videoQualityBar.setEnabled(enabled);
	        	/* KEEP DISABLED */
	        	/*
	        	for(int i = 0; i < codecSelect.getChildCount(); i++){
	        	    ((RadioButton)codecSelect.getChildAt(i)).setEnabled(false);
	        	}
	        	*/
    		}
    	});
    }
    
    /*
    private void initSpeedUI() {
    	ToggleButton tbPubSpeed = (ToggleButton)findViewById(R.id.tb_pubSpeed);
    	ToggleButton tbSubSpeed = (ToggleButton)findViewById(R.id.tb_subSpeed);
    	
    	tbPubSpeed.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				speedProxy.setPublish(isChecked);
			}
    	});
    	
    	tbSubSpeed.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				speedProxy.setSubscribe(isChecked);
			}
    	});
    }
    */
    
    private void initVideoUI() {
    	views = new MjpegView[3];
    	int[] viewId = {R.id.video1, R.id.video2, R.id.video3};
    	
    	for (int i = 0; i < views.length; i++) {	
    		views[i] = (MjpegView)findViewById(viewId[i]);
    		views[i].setViewId(i+1);
    	}
    	
    	preview = (SurfaceView)findViewById(R.id.preview);
    	preview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);// only the last one is set as PUSH_BUFFERS 
    	preview.setZOrderMediaOverlay(true);
    	
    	tbSendVideo = (ToggleButton)findViewById(R.id.tb_sendVideo);
    	tbRecvVideo = (ToggleButton)findViewById(R.id.tb_recvVideo);
    	
    	videoSizeSelect = (Spinner)findViewById(R.id.sp_videoSize);
    	videoQualityBar = (SeekBar)findViewById(R.id.sb_videoQuality);
    	
    	tbSendVideo.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				videoProxy.setSend(isChecked);
				videoSizeSelect.setEnabled(!isChecked);	// disable video_size selector when recording started
			}
    	});
    	
    	tbRecvVideo.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				videoProxy.setReceive(isChecked);
			}
    	});
    	
    	// get camera spec
    	Camera camera = Camera.open(VideoRecorder.CAM_ID);	// opening camera
        Camera.Parameters cameraParameters = camera.getParameters();
        List<Camera.Size> camSupportedSize = cameraParameters.getSupportedPreviewSizes();
        Collections.sort(camSupportedSize, new Comparator<Camera.Size>() {
			@Override
			public int compare(Size lhs, Size rhs) {
				return lhs.width - rhs.width;
			}
        });
    	ArrayList<String> camSupportedSizeStr = new ArrayList<String>(camSupportedSize.size());
    	for (Camera.Size size : camSupportedSize) {
    		camSupportedSizeStr.add(size.width + "x" + size.height);
    	}
    	camera.release();	// releasing camera
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, camSupportedSizeStr);
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	
    	videoSizeSelect.setAdapter(adapter);
    	
    	videoSizeSelect.setSelection(0);	// 176x144
    	videoSizeSelect.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int position, long arg3) {
				if (videoProxy != null) {
					videoProxy.setVideoSize(videoSizeSelect.getSelectedItem().toString());
				}
			}

			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}
    	});
    	
    	final int MIN_SIZE = 1;
    	videoQualityBar.setProgress(5);
    	videoQualityBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				/*
				progress = ((int)Math.round(progress/STEP_SIZE))*STEP_SIZE;
				*/
				if (progress == 0) {
					progress = MIN_SIZE;
				}
			    seekBar.setProgress(progress);
			    
				videoProxy.setVideoQuality(progress*10);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
    	});
    }
    
    private void initVoiceUI() {
    	userIdInput = (EditText)findViewById(R.id.clientIdInput);
    	btnInit = (Button)findViewById(R.id.btn_init);
    	tbSend = (ToggleButton)findViewById(R.id.tb_send);
    	tbRecv = (ToggleButton)findViewById(R.id.tb_recv);
    	//codecSelect = (RadioGroup)findViewById(R.id.rg_codec);
    	sampleRateSelect = (Spinner)findViewById(R.id.sp_sampleRate);
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
				sampleRateSelect.setEnabled(!isChecked);	// disable sample_rate selector when recording started
			}
    	});
    	
    	tbRecv.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				voiceProxy.setReceive(isChecked);
			}
    	});
    	
    	/*
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
    	*/
    	
    	ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
            this, R.array.sample_rate, android.R.layout.simple_spinner_item);
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	sampleRateSelect.setAdapter(adapter);
    	final int[] sampleRateValue = {16000, 22050, 32000, 44100, 48000};
    	sampleRateSelect.setSelection(1);	// 22050
    	sampleRateSelect.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int position, long arg3) {
				if (voiceProxy != null) {
					voiceProxy.setSampleRate(sampleRateValue[position]);
				}
			}

			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
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
	
	private void runTest(boolean run) {
		if (!run) {
			return;
		}
		
		// start test
		AssetManager assetManager = getAssets();
		try {
			String[] imgs = assetManager.list("img");
			for (String img : imgs) {
				Log.i(TAG, "Img file: " + img);
			}
			
			// read yuv file as byte[]
			Log.i(TAG, "Reading yuv file into byte[]...");
			InputStream is = assetManager.open("img/still_frame_0.yuv");
			byte[] yuvBytes = new byte[is.available()];
			is.read(yuvBytes);
			is.close();
			
			// is native library loaded?
			BAWrapperShared.c_hex_to_char("test");
			
			// native byte buffer allocation test
			ByteBuffer testBuf = NativeJpegLib.allocateNativeBuffer(1000);
			NativeJpegLib.freeNativeBuffer(testBuf);
			
			// convert yuv -> jpeg
			Log.i(TAG, "Converting yuv -> jpeg");
			ByteBuffer buf = NativeJpegLib.encode(2048, 1536, 100, yuvBytes);
			
			// save jpeg into file
			Log.i(TAG, "Saving jpeg files...");
			String extPath = Environment.getExternalStorageDirectory().toString();
			File file = new File(extPath, "still.jpg");
			FileOutputStream fos = new FileOutputStream(file);
			byte[] jpgBuf = new byte[buf.capacity()];
			buf.get(jpgBuf);
			fos.write(jpgBuf);
			fos.flush();
			fos.close();
			Log.i(TAG, "JPEG saved to: " + file.getAbsolutePath());
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void loadJNILibraries() {
    	String sharedObjPath = "/data/data/uk.ac.cam.cl.xf214.blackadderCom/lib/";
		// load gnustl
    	System.load(sharedObjPath + "libgnustl_shared.so");
    	// load blackadder
		System.load(sharedObjPath + "libblackadder.so");
		// load speex
		System.load(sharedObjPath + "libspeex.so");
		// load libjpeg
		System.load(sharedObjPath + "libjpeg-mod.so");

		BAWrapperShared.configureObjectFile(sharedObjPath
				+ "libuk_ac_cam_cl_xf214_blackadderWrapper.so");
		Log.i(TAG, "JNI shared libraries loaded");
    }
}
