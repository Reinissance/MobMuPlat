package com.iglesiaintermedia.mobmuplat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.InputDevice.MotionRange;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdPreferences;
import org.puredata.android.service.PdService;
import org.puredata.core.PdBase;
import org.puredata.core.PdReceiver;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi.MessageListener;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.iglesiaintermedia.mobmuplat.PatchFragment.CanvasType;
import com.iglesiaintermedia.mobmuplat.controls.*;
import com.illposed.osc.OSCMessage;
import com.example.inputmanagercompat.InputManagerCompat;
import com.example.inputmanagercompat.InputManagerCompat.InputDeviceListener;

public class MainActivity extends FragmentActivity implements LocationListener, SensorEventListener, AudioDelegate, InputDeviceListener, OnBackStackChangedListener {
	private static final String TAG = "MobMuPlat MainActivity";
	public static final boolean VERBOSE = false;
	//
	private FrameLayout _topLayout;
	public CanvasType hardwareScreenType;
	// Bookmark it between config change
	private String _fileToLoad;

	//Pd
	private PdService pdService = null;
	private int openPDFileHandle;

	//HID
	private InputManagerCompat _inputManager;
	private SparseArray<InputDeviceState> _inputDeviceStates;

	//
	//public final static float VERSION = 1.6f; //necc?

	// important public!
	public UsbMidiController usbMidiController;
	public NetworkController networkController;

	// new arch!
	private PatchFragment _patchFragment;
	private DocumentsFragment _documentsFragment;
	private AudioMidiFragment _audioMidiFragment;
	private NetworkFragment _networkFragment;
	private ConsoleFragment _consoleFragment;
	private HIDFragment _hidFragment;
	private Fragment _lastFrag;
	private String _lastFragTitle;

	public FlashlightController flashlightController;
	private BroadcastReceiver _bc;
	boolean _stopAudioWhilePaused = true; 

	private LocationManager locationManagerA, locationManagerB;

	//sensor
	float[] _rawAccelArray;
	float[] _cookedAccelArray; 
	Object[] _tiltsMsgArray; //addr+2 FL
	Object[] _accelMsgArray; //addr+3 FL
	Object[] _gyroMsgArray;
	Object[] _rotationMsgArray;
	Object[] _compassMsgArray; 
	private boolean _shouldSwapAxes = false;
	
	// wear
    WorkerThread wt;
    private GoogleApiClient mGoogleApiClient;
	private static final long CONNECTION_TIME_OUT_MS = 100;
	private String nodeId;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		//getActionBar().setDisplayShowTitleEnabled(true);
		getActionBar().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
		getActionBar().hide(); //necc on L, not necc on kitkat

		//layout view 
		setContentView(R.layout.activity_main);

		processIntent();

		_inputManager = InputManagerCompat.Factory.getInputManager(this);
		_inputManager.registerInputDeviceListener(this, null);
		_inputDeviceStates = new SparseArray<InputDeviceState>();

		//device type (just for syncing docs)

		hardwareScreenType = CanvasType.canvasTypeWidePhone; //default
		CharSequence deviceFromValues = getResources().getText(R.string.screen_type);
		
		//derive screen ratio
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int width = size.x;
		int height = size.y;
		float aspect = (float)height/width;
		
		if (deviceFromValues.equals("phone")) {
			if (aspect > 1.6375) { // 
				hardwareScreenType = CanvasType.canvasTypeTallPhone; 
			} else {
				hardwareScreenType = CanvasType.canvasTypeWidePhone; 
			}
		} else if (deviceFromValues.equals("7inch") || 
				   deviceFromValues.equals("10inch")) {
			if (aspect > 1.42) { // 
				hardwareScreenType = CanvasType.canvasTypeTallTablet; 
			} else {
				hardwareScreenType = CanvasType.canvasTypeWideTablet; 
			}
		} 

		//version
		boolean shouldCopyDocs = false;
		int versionCode = 0;
		PackageInfo packageInfo;
		try {
			packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			versionCode = packageInfo.versionCode;
			SharedPreferences sp = getPreferences(Activity.MODE_PRIVATE);
			int lastOpenedVersionCode = sp.getInt("lastOpenedVersionCode", 0);
			if (versionCode > lastOpenedVersionCode) {
				shouldCopyDocs = true;
			}
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		//temp
		//shouldCopyDocs = true;
		//copy
		if(shouldCopyDocs) {//!alreadyStartedOnVersion || [alreadyStartedOnVersion boolValue] == NO) {
			List<String> defaultPatchesList;
			if(hardwareScreenType == CanvasType.canvasTypeWidePhone || hardwareScreenType == CanvasType.canvasTypeTallTablet){
				defaultPatchesList=Arrays.asList("MMPTutorial0-HelloSine.mmp", "MMPTutorial1-GUI.mmp", "MMPTutorial2-Input.mmp", "MMPTutorial3-Hardware.mmp", "MMPTutorial4-Networking.mmp","MMPTutorial5-Files.mmp","MMPExamples-Vocoder.mmp", "MMPExamples-Motion.mmp", "MMPExamples-Sequencer.mmp", "MMPExamples-GPS.mmp", "MMPTutorial6-2DGraphics.mmp", "MMPExamples-LANdini.mmp", "MMPExamples-Arp.mmp", "MMPExamples-TableGlitch.mmp", "MMPExamples-HID.mmp", "MMPExamples-Watch.mmp");
			}
			else if (hardwareScreenType==CanvasType.canvasTypeTallPhone){
				defaultPatchesList=Arrays.asList("MMPTutorial0-HelloSine-ip5.mmp", "MMPTutorial1-GUI-ip5.mmp", "MMPTutorial2-Input-ip5.mmp", "MMPTutorial3-Hardware-ip5.mmp", "MMPTutorial4-Networking-ip5.mmp","MMPTutorial5-Files-ip5.mmp", "MMPExamples-Vocoder-ip5.mmp", "MMPExamples-Motion-ip5.mmp", "MMPExamples-Sequencer-ip5.mmp","MMPExamples-GPS-ip5.mmp", "MMPTutorial6-2DGraphics-ip5.mmp", "MMPExamples-LANdini-ip5.mmp", "MMPExamples-Arp-ip5.mmp",  "MMPExamples-TableGlitch-ip5.mmp", "MMPExamples-HID-ip5.mmp");
			}
			else{//wide tablet/pad
				defaultPatchesList=Arrays.asList("MMPTutorial0-HelloSine-Pad.mmp", "MMPTutorial1-GUI-Pad.mmp", "MMPTutorial2-Input-Pad.mmp", "MMPTutorial3-Hardware-Pad.mmp", "MMPTutorial4-Networking-Pad.mmp","MMPTutorial5-Files-Pad.mmp", "MMPExamples-Vocoder-Pad.mmp", "MMPExamples-Motion-Pad.mmp", "MMPExamples-Sequencer-Pad.mmp","MMPExamples-GPS-Pad.mmp", "MMPTutorial6-2DGraphics-Pad.mmp", "MMPExamples-LANdini-Pad.mmp", "MMPExamples-Arp-Pad.mmp",  "MMPExamples-TableGlitch-Pad.mmp", "MMPExamples-HID-Pad.mmp");
			}

			List<String> commonFilesList = Arrays.asList("MMPTutorial0-HelloSine.pd","MMPTutorial1-GUI.pd", "MMPTutorial2-Input.pd", "MMPTutorial3-Hardware.pd", "MMPTutorial4-Networking.pd","MMPTutorial5-Files.pd","cats1.jpg", "cats2.jpg","cats3.jpg","clap.wav","Welcome.pd",  "MMPExamples-Vocoder.pd", "vocod_channel.pd", "MMPExamples-Motion.pd", "MMPExamples-Sequencer.pd", "MMPExamples-GPS.pd", "MMPTutorial6-2DGraphics.pd", "MMPExamples-LANdini.pd", "MMPExamples-Arp.pd", "MMPExamples-TableGlitch.pd", "anderson1.wav", "MMPExamples-HID.pd", "MMPExamples-Watch.pd");

			//defaultPatches = [defaultPatches arrayByAddingObjectsFromArray:commonFiles];

			for (String filename : defaultPatchesList) {
				copyAsset(filename);
			}
			for (String filename : commonFilesList) {
				copyAsset(filename);
			}

			//assuming success, write to user preference
			if (versionCode > 0) {
				SharedPreferences settings = getPreferences(Activity.MODE_PRIVATE);
				SharedPreferences.Editor editor = settings.edit();
				editor.putInt("lastOpenedVersionCode", versionCode);
				editor.commit();
			}

		}

		//
		AudioParameters.init(this);
		PdPreferences.initPreferences(getApplicationContext());
		initSensors();
		initLocation();
		usbMidiController = new UsbMidiController(this); //matched close in onDestroy...move closer in?

		//allow multicast
		WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		if(wifi != null){
			WifiManager.MulticastLock lock = wifi.createMulticastLock("MulticastLockTag");
			lock.acquire();
		}  //Automatically released on app exit/crash.

		networkController = new NetworkController(this);
		networkController.delegate = this;

		bindService(new Intent(this, PdService.class), pdConnection, BIND_AUTO_CREATE);
		
		//bindService(new Intent(this, ListenerService.class), wearConnection, BIND_AUTO_CREATE);
	    mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                //.addConnectionCallbacks(this)
                .build();
		mGoogleApiClient.connect();
		retrieveDeviceNode(); //TODO on connection?
		// message send thread
        wt = new WorkerThread();
        wt.start();
        
		Wearable.MessageApi.addListener(mGoogleApiClient, new MessageListener() {
			@Override
			public void onMessageReceived(MessageEvent messageEvent) {
				
				String path = messageEvent.getPath();
				try {
					String dataString = new String(messageEvent.getData(), "UTF-8");
					String[] messageStringArray = dataString.split(" ");
					List<Object> objList = new ArrayList<Object>();
					objList.add(path); //add address first, then rest of list
					for (String token : messageStringArray) {
						try {
							Float f = Float.valueOf(token);
							objList.add(f);
						} catch (NumberFormatException e) {
							// not a number, add as string.
							objList.add(token);
						}
					}
					PdBase.sendList("fromGUI", objList.toArray());
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		});
		
		//wifi
		_bc = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) { //having trouble with this, reports "0x", doesn't repond to supplicant stuff
				networkController.newSSIDData();
			}	
		};
		IntentFilter intentFilter = new IntentFilter();
		//intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
		intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) ;

		registerReceiver(_bc, intentFilter);



		//fragments
		_patchFragment = new PatchFragment();
		_documentsFragment = new DocumentsFragment();
		_networkFragment = new NetworkFragment();
		_consoleFragment = new ConsoleFragment();
		_hidFragment = new HIDFragment();
		_audioMidiFragment = new AudioMidiFragment();
		//_audioMidiFragment.audioDelegate = this;
		// bookmark for launch from info button
		_lastFrag = _documentsFragment;
		_lastFragTitle = "Documents";


		_topLayout = (FrameLayout)findViewById(R.id.container);

		// Go full screen and lay out to top of screen.
		// Only on SDK >= 16. This means that patch will not go truly fullscreen on ICS. Separate layouts for 14+ vs 16+ for the fragment margins.
		_topLayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

		//Flashlight TODO make black
		SurfaceView surfaceView = (SurfaceView)findViewById(R.id.surfaceView);
		flashlightController = new FlashlightController(surfaceView);
		flashlightController.startCamera();

		// axes for table
		if (getDeviceNaturalOrientation() == Configuration.ORIENTATION_LANDSCAPE) _shouldSwapAxes = true;

		// set action bar title on fragment stack changes
		getSupportFragmentManager().addOnBackStackChangedListener(this);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		if (savedInstanceState == null) {
			launchSplash();
		}
	}

	 /**
     * Connects to the GoogleApiClient and retrieves the connected device's Node ID. If there are
     * multiple connected devices, the first Node ID is returned.
     */
    private void retrieveDeviceNode() {
        new Thread(new Runnable() {
            @Override
            public void run() {
            	mGoogleApiClient.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
                NodeApi.GetConnectedNodesResult result =
                        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                List<Node> nodes = result.getNodes();
                if (nodes.size() > 0) {
                    nodeId = nodes.get(0).getId();
                    //Log.i("WEAR", "gotNODE");
                }
                mGoogleApiClient.disconnect();
            }
        }).start();
    }
    
    class WorkerThread extends Thread {
        public Handler mHandler;
        public void run() {	 
            Looper.prepare();
           mHandler = new Handler();/* {
                public void handleMessage(Message msg) {
                    // process incoming messages here
                	String message = (String) msg.obj;
                	Wearable.MessageApi.sendMessage(client, nodeId, message, null);
                	Log.i("WEAR", "client = "+client.isConnected());
                }
            };*/
            Looper.loop();
        }
    }
    
    public void sendWearMessage(String inPath, byte[] inData) {
        if (nodeId != null) {
        	final String path = inPath;
        	final byte[] data = inData;
        	wt.mHandler.post(new Runnable() {
  			  @Override
  			  public void run() {  
  				  //String message = (String) msg.obj;
  				if (mGoogleApiClient.isConnected()==false)mGoogleApiClient.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
                	Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, path, data);
                	//Log.i("WEAR", "client = "+client.isConnected());
  			  }
  		});
            /*new Thread(new Runnable() {
                @Override
                public void run() {
                	mGoogleApiClient.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
                    Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, path, data);
                    mGoogleApiClient.disconnect();
                }
            }).start();*/
        }
    }
    
	@Override
	public void onBackStackChanged() {
		int count = getSupportFragmentManager().getBackStackEntryCount();//testing
		String title = "MobMuPlat";
		if (count > 0) {
			title = getSupportFragmentManager().getBackStackEntryAt(count - 1).getName();
		}
		getActionBar().setTitle(title);
		if (count == 0 && getActionBar().isShowing()) getActionBar().hide();
		else if (count > 0 && !getActionBar().isShowing())getActionBar().show();
	}

	@Override
	protected void onNewIntent(Intent intent) { //called by open with file
		super.onNewIntent(intent);
		setIntent(intent);
		processIntent();
	}

	private void processIntent() {
		Intent i = getIntent();
		if(i!=null) {
			String action = i.getAction();

			/*if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
		        WifiManager manager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		        NetworkInfo networkInfo = i.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
		        NetworkInfo.State state = networkInfo.getState();

		        if(state == NetworkInfo.State.CONNECTED)
		        {
		            String connectingToSsid = manager.getConnectionInfo().getSSID();//.replace("\"", "");
		           // WifiStateHistory.recordConnectedSsid(connectingToSsid);
		//connected
		        }

		        if(state == NetworkInfo.State.DISCONNECTED)
		        {
		            if(manager.isWifiEnabled())
		            {
		                //String disconnectedFromSsid = WifiStateHistory.getLastConnectedSsid();
		//disconnected
		            }
		        }
		    }*/
			/*if(i.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
		        UsbDevice device  = (UsbDevice)i.getParcelableExtra(UsbManager.EXTRA_DEVICE);
		        //TODO double check that on app-start processIntent is called after setting up usbmidicontroller
		        //usbMidiController.onDeviceAttached(device);
		        //here: set intent filter not to open mmp, but still get intents if open???
		        //getActionBar().setBackgroundDrawable(new ColorDrawable(Color.RED));

			}*/
			//file
			if (action.equals(Intent.ACTION_VIEW)){
				Uri dataUri = i.getData();
				if(dataUri!=null){
					if(VERBOSE)Log.i(TAG, "receive intent data " + dataUri.toString());

					if (i.getScheme().equals("content")) { //received via email attachment
						try {
							InputStream attachment = getContentResolver().openInputStream(getIntent().getData());
							if (attachment == null) {
								if(VERBOSE)Log.e("onCreate", "cannot access mail attachment");
								showAlert("Cannot access mail attachment.");
							} else {
								String attachmentFileName = null;
								//try to get file name
								Cursor c = getContentResolver().query(i.getData(), null, null, null, null);
								c.moveToFirst();
								final int fileNameColumnId = c.getColumnIndex(
										MediaStore.MediaColumns.DISPLAY_NAME);
								if (fileNameColumnId >= 0) {
									attachmentFileName = c.getString(fileNameColumnId);
									String type = i.getType(); //mime type
									if (type.equals("application/zip")) {
										unpackZipInputStream(attachment, attachmentFileName); 
									} else {
										copyInputStream(attachment, attachmentFileName, true);
									}
								} else {
									showAlert("Cannot get filename for attachment.");
								}
							}
						}catch(Exception e) {
							showAlert("Cannot access mail attachment.");
						}

					} else {//"file"

						String filename = dataUri.getLastPathSegment();
						String fullPath = dataUri.getPath();
						int lastSlashPos = fullPath.lastIndexOf('/');

						String parentPath = fullPath.substring(0, lastSlashPos+1);
						String suffix = filename.substring(filename.lastIndexOf('.'));
						if (suffix.equals(".zip")) { 
							unpackZip(parentPath, filename);
						}
						else {
							copyUri(dataUri);
						}
					}
				}
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		flashlightController.stopCamera();
	}

	@Override
	protected void onStop() {
		super.onStop();
		if(_stopAudioWhilePaused) {
			stopAudio();
		}
		flashlightController.stopCamera();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		if(pdService!=null && !pdService.isRunning()) {
			pdService.startAudio();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		flashlightController.startCamera();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		// start wear activity
		sendWearMessage("/startActivity", null);
	}

	public void loadScene(String filenameToLoad) {
		getActionBar().hide();

		_fileToLoad = filenameToLoad;
		String fileJSON = readMMPToString(filenameToLoad);
		boolean requestedChange = setOrientationOfMMP(fileJSON);

		if(!requestedChange){
			_patchFragment.loadSceneFromJSON(fileJSON);
			_fileToLoad = null;
		}
		//otherwise is being loaded in onConfigChange
	}

	public void loadScenePatchOnly(String filenameToLoad) {
		//don't bother with rotation for now...
		stopLocationUpdates();
		
		getActionBar().hide();

		//TODO RESET ports?
		_patchFragment.loadScenePatchOnly(filenameToLoad);
		loadPdFile(filenameToLoad);
	}

	public void loadPdFile(String pdFilename) {
		// load pd patch
		if(openPDFileHandle != 0)PdBase.closePatch(openPDFileHandle); 
		//open
		// File patchFile = null;
		if(pdFilename!=null) {
			try {
				File pdFile = new File(MainActivity.getDocumentsFolderPath(), pdFilename);
				openPDFileHandle = PdBase.openPatch(pdFile);
			} catch (FileNotFoundException e) {
				showAlert("PD file "+pdFilename+" not found.");
			} catch (IOException e) {
				showAlert("I/O error on loading PD file "+pdFilename+".");
			}
		}
	}

	static public String readMMPToString(String filename) {
		if (filename == null) return null;
		File file = new File(MainActivity.getDocumentsFolderPath(),filename);
		if (!file.exists())return null;

		Writer writer = new StringWriter();
		char[] buffer = new char[1024];
		try {
			InputStream is = new FileInputStream(file);
			Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			int n;
			while ((n = reader.read(buffer)) != -1) {
				writer.write(buffer, 0, n);
			}
			try {
				is.close();
			} catch (Exception e) {

			}
		} catch (Exception e) {
			//TODO - ioexception
		} finally {

		}

		String jsonString = writer.toString();

		return jsonString;
	}

	static public String readMMPAssetToString(InputStream is) {
		if (is == null) return null;
		//File file = new File(MainActivity.getDocumentsFolderPath(),filename);

		Writer writer = new StringWriter();
		char[] buffer = new char[1024];
		try {
			Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			int n;
			while ((n = reader.read(buffer)) != -1) {
				writer.write(buffer, 0, n);
			}
			try {
				is.close();
			} catch (Exception e) {

			}
		} catch (Exception e) {
			//TODO - ioexception
		} finally {

		}

		String jsonString = writer.toString();

		return jsonString;
	}

	private void initLocation() {
		locationManagerA = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		//locationManagerA.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 5, this);
		locationManagerB = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		//locationManagerB.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 5, this);
	}

	public void startLocationUpdates() {
		Location lastKnownLocation = null;
		if (locationManagerA!=null){
			locationManagerA.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 5, this);
			// send initial val from this
			lastKnownLocation = locationManagerA.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			if (lastKnownLocation != null)onLocationChanged(lastKnownLocation);
		}
		if (locationManagerB!=null){
			locationManagerB.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 5, this);
			//if initial didn't work, try here
			if (lastKnownLocation==null) {
				lastKnownLocation = locationManagerB.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				if (lastKnownLocation != null)onLocationChanged(lastKnownLocation);
			}
		}
		
	}

	public void stopLocationUpdates() {
		if (locationManagerA!=null)locationManagerA.removeUpdates(this);
		if (locationManagerB!=null)locationManagerB.removeUpdates(this);
	}

	private void initSensors() { //TODO allow sensors on default thread for low-power devices (or just shutoff)

		//_camera = Camera.open();
		_rawAccelArray = new float[3];
		_cookedAccelArray = new float[3];
		_tiltsMsgArray = new Object[3];
		_tiltsMsgArray[0] = "/tilts";
		_accelMsgArray = new Object[4];
		_accelMsgArray[0] = "/accel";

		_gyroMsgArray = new Object[4];
		_gyroMsgArray[0] = "/gyro";
		_rotationMsgArray = new Object[4];
		_rotationMsgArray[0] = "/motion";
		_compassMsgArray = new Object[2];
		_compassMsgArray[0] = "/compass";

		SensorManager sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		//
		// onSensorChanged is now called on a background thread.
		HandlerThread handlerThread = new HandlerThread("sensorThread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
		handlerThread.start();
		Handler handler = new Handler(handlerThread.getLooper());

		Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorManager.registerListener(this, accel,SensorManager.SENSOR_DELAY_GAME, handler);
		Sensor gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		sensorManager.registerListener(this,  gyro, SensorManager.SENSOR_DELAY_GAME, handler);//TODO rate
		Sensor rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
		sensorManager.registerListener(this,  rotation, SensorManager.SENSOR_DELAY_GAME, handler);//TODO rate
		Sensor compass = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		sensorManager.registerListener(this,  compass, SensorManager.SENSOR_DELAY_GAME, handler);

		//

		/*Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorManager.registerListener(this,  accel, SensorManager.SENSOR_DELAY_GAME);//TODO rate
		Sensor gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		sensorManager.registerListener(this,  gyro, SensorManager.SENSOR_DELAY_GAME);//TODO rate
		Sensor rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
		sensorManager.registerListener(this,  rotation, SensorManager.SENSOR_DELAY_GAME);//TODO rate
		Sensor compass = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		sensorManager.registerListener(this,  compass, SensorManager.SENSOR_DELAY_GAME);//TODO rate
		 */
	}

	//insane: 10" tablets can have a "natural" (Surface.ROTATION_0) at landscape, not portrait. Determine
	// the "natural" orientation so that the setOrientation() logic works as expected...
	public int getDeviceNaturalOrientation() {

		WindowManager windowManager =  (WindowManager) getSystemService(WINDOW_SERVICE);

		Configuration config = getResources().getConfiguration();

		int rotation = windowManager.getDefaultDisplay().getRotation();

		if ( ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) &&
				config.orientation == Configuration.ORIENTATION_LANDSCAPE)
				|| ((rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) &&    
						config.orientation == Configuration.ORIENTATION_PORTRAIT)) {
			return Configuration.ORIENTATION_LANDSCAPE;
		} else { 
			return Configuration.ORIENTATION_PORTRAIT;
		}
	}

	private boolean setOrientationOfMMP(String jsonString){ //returns whether there was a change
		JsonParser parser = new JsonParser();
		try {
			JsonObject topDict = parser.parse(jsonString).getAsJsonObject();//top dict
			int screenOrientation = this.getWindow().getWindowManager().getDefaultDisplay().getRotation();// on tablet rotation_0 = "natural"= landscape
			int mmpOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
			if(topDict.get("isOrientationLandscape")!=null) {
				boolean isOrientationLandscape = topDict.get("isOrientationLandscape").getAsBoolean();
				if (isOrientationLandscape)mmpOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
			}

			int naturalOrientation = getDeviceNaturalOrientation();
			if (naturalOrientation == Configuration.ORIENTATION_PORTRAIT) { //phones, 7" tablets
				if ((mmpOrientation==ActivityInfo.SCREEN_ORIENTATION_PORTRAIT && screenOrientation!=Surface.ROTATION_0) ||
						(mmpOrientation==ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE && screenOrientation!=Surface.ROTATION_90)) {
					if(VERBOSE)Log.i(TAG, "requesting orientation...surface = "+screenOrientation);
					this.setRequestedOrientation(mmpOrientation);
					return true;
				}
			}
			//"natural" = landscape = big tablet
			else if (naturalOrientation == Configuration.ORIENTATION_LANDSCAPE) {
				if ((mmpOrientation==ActivityInfo.SCREEN_ORIENTATION_PORTRAIT && screenOrientation!=Surface.ROTATION_270) || //weird that it thinks that rotation 270 is portrait
						(mmpOrientation==ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE && screenOrientation!=Surface.ROTATION_0)) {
					if(VERBOSE)Log.i(TAG, "requesting orientation...surface = "+screenOrientation);
					this.setRequestedOrientation(mmpOrientation);
					return true;
				}
			}
			return false;
		} catch(JsonParseException e) {
			showAlert("Unable to parse interface file.");
		}	
		return false;
	}

	/*
	private void copyAllDocuments() { //doesn't work with linked files!
		//AssetManager am = getAssets();
		String [] list;
		try {
			list = getAssets().list("");
			if (list.length > 0) {
				// This is a folder
				for (String filename : list) {
					//if (!listAssetFiles(path + "/" + file))
					Log.i(TAG, "file "+filename); 
					//getAssets().open(fileName;)
					copyAsset(filename);
				}
			} else {
				Log.i(TAG, "file single ");
			}//path is a file
		} catch (IOException e) {

		}
	}*/

	private void copyInputStream(InputStream in, String filename, boolean showAlert) {
		File file = new File(MainActivity.getDocumentsFolderPath(), filename);
		try {
			OutputStream out = new FileOutputStream(file);
			byte[] data = new byte[in.available()];
			in.read(data);
			out.write(data);
			in.close();
			out.close();

			if(showAlert)showAlert("File "+filename+" copied to MobMuPlat Documents");
		} catch (FileNotFoundException e) {
			Log.i(TAG, "Unable to copy file: "+e.getMessage());
		} catch (IOException e) {
			Log.i(TAG, "Unable to copy file: "+e.getMessage());
		}
	}

	public void copyUri(Uri uri){
		String sourcePath = uri.getPath();
		String sourceFilename = uri.getLastPathSegment();
		try {
			InputStream in = new FileInputStream(sourcePath);
			copyInputStream(in, sourceFilename, true);
		} catch (FileNotFoundException e) {
			Log.i(TAG, "Unable to copy file: "+e.getMessage());
		}
	}

	public void copyAsset(String assetFilename){
		AssetManager assetManager = getAssets();

		try {
			InputStream in = assetManager.open(assetFilename);
			copyInputStream(in, assetFilename, false);
			//Log.i(TAG, "Copied file: "+assetFilename);
		} catch (IOException e) {
			Log.i(TAG, "Unable to copy file: "+e.getMessage());
		}
	}

	//TODO consolidate this with version in fragment
	private void showAlert(String string) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(string);
		builder.setCancelable(false);
		builder.setPositiveButton("OK", null);
		AlertDialog alert = builder.create();
		alert.show();
	}

	// on orientation changed
	public void onConfigurationChanged (Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Log.i(TAG, "ACT on config changed - file to load "+_fileToLoad);
		//if(_fileToLoad!=null)initGui(_fileToLoad);
		//initGui();
		ViewTreeObserver observer = _topLayout.getViewTreeObserver();
		observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

			@Override
			public void onGlobalLayout() {
				_topLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				if(_fileToLoad!=null) {	
					_patchFragment.loadSceneFromJSON(readMMPToString(_fileToLoad));
					_fileToLoad = null;
				}
			}
		});
	}

	private final ServiceConnection pdConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.i(TAG, "Service connected");
			pdService = ((PdService.PdBinder)service).getService();
			//initPd();
			initAndStartAudio(); 
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// this method will never be called
		}
	};

	private void initAndStartAudio() {
		try {
			//Simulator breaks on audio input, so here's a little flag
			boolean amOnSimulator = false;
			pdService.initAudio(-1, amOnSimulator? 0 : -1, -1, -1);   // negative values will be replaced with defaults/preferences
			pdService.startAudio();
		} catch (IOException e) {
			Log.e(TAG, "Audio init error: "+e.toString());
		}
	}

	private void stopAudio() {
		if(pdService!=null)pdService.stopAudio();
	}

	private void cleanup() {
		try {
			unbindService(pdConnection);
		} catch (IllegalArgumentException e) {
			// already unbound
			pdService = null;
		}
		flashlightController.stopCamera();
	}

	protected void onDestroy() {
		usbMidiController.close(); //solves "Intent Receiver Leaked: ... are you missing a call to unregisterReceiver()?"
		unregisterReceiver(_bc);
		super.onDestroy();
		cleanup();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void launchSplash(){
		final SplashFragment sf = new SplashFragment();
		final FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.add(R.id.container, sf);
		fragmentTransaction.commit(); 

		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			public void run() {
				if(this!=null && !isFinishing()){
					fragmentManager.beginTransaction().remove(sf).commitAllowingStateLoss(); //When just commit(), would get java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
					getSupportFragmentManager().beginTransaction().add(R.id.container, _patchFragment).commitAllowingStateLoss();	
				}
			}
		}, 4000);
	}

	public void launchFragment(Fragment frag, String title) {
		FragmentManager fragmentManager = getSupportFragmentManager();

		// if already what we are looking at, return (otherwise it pops but isn't re-added)
		if (fragmentManager.getBackStackEntryCount()==1 && fragmentManager.getBackStackEntryAt(0).getName() == title) {
			return;
		}
		// if something else is on the stack above patch, pop it, but remove listener to not remove/readd action bar.
		if (fragmentManager.getBackStackEntryCount() > 0) {
			fragmentManager.removeOnBackStackChangedListener(this);
			fragmentManager.popBackStackImmediate();
			fragmentManager.addOnBackStackChangedListener(this);
		}

		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.add(R.id.container, frag);
		fragmentTransaction.addToBackStack(title);
		fragmentTransaction.commit(); 
		fragmentManager.executePendingTransactions(); //Do it immediately, not async.
	}

	public void launchSettings() { //launch the last settings frag we were looking at.
		getActionBar().show();
		launchFragment(_lastFrag, _lastFragTitle);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		if (id == R.id.action_documents) {
			launchFragment(_documentsFragment, "Documents");
			_lastFrag = _documentsFragment;
			_lastFragTitle = "Documents";
			return true;
		} else if (id == R.id.action_audiomidi) {
			launchFragment(_audioMidiFragment, "Audio & Midi");
			_lastFrag = _audioMidiFragment;
			_lastFragTitle = "Audio & Midi";
			return true;
		} else if (id == R.id.action_network) {
			launchFragment(_networkFragment, "Networking");
			_lastFrag = _networkFragment;
			_lastFragTitle = "Networking";
			return true;
		} else if (id == R.id.action_console) {
			launchFragment(_consoleFragment, "Console");
			_lastFrag = _consoleFragment;
			_lastFragTitle = "Console";
			return true;
		} else if (id == R.id.action_HID) {
			launchFragment(_hidFragment, "Human Interface Device");
			_lastFrag = _hidFragment;
			_lastFragTitle = "Human Interface Device";
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void cookAccel(float[] rawAccel, float outputAccel[]) { //input is -10 to 10, inverted on x
		//assumes in 3, out 2
		float cookedX = rawAccel[0];
		float cookedY = rawAccel[1];
		float accelZ = rawAccel[2];
		// cook it via Z accel to see when we have tipped it beyond 90 degrees

		if(cookedX>0 && accelZ>0) cookedX=(2-cookedX); //tip towards long side
		else if(cookedX<0 && accelZ>0) cookedX=(-2-cookedX); //tip away long side

		if(cookedY>0 && accelZ>0) cookedY=(2-cookedY); //tip right
		else if(cookedY<0 && accelZ>0) cookedY=(-2-cookedY); //tip left

		//clip 
		if(cookedX<-1)cookedX=-1;
		else if(cookedX>1)cookedX=1;
		if(cookedY<-1)cookedY=-1;
		else if(cookedY>1)cookedY=1;
		//return new float[]{cookedX, cookedY};
		outputAccel[0] = cookedX;
		outputAccel[1] = cookedY;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			for (int i=0 ; i < 3; i++) {
				_rawAccelArray[i] = event.values[i];
			}
			//invert and scale to match ios
			for(int i=0;i<3;i++)_rawAccelArray[i] = -_rawAccelArray[i]/10.0f;
			if (_shouldSwapAxes) _rawAccelArray[0] *= -1; //for big tablets, will become y axis, needs to be flipped
			cookAccel(_rawAccelArray, _cookedAccelArray);
			_tiltsMsgArray[1]=Float.valueOf(_cookedAccelArray[_shouldSwapAxes ? 1 : 0]); //{"/tilts", Float.valueOf(cookedAccel[0]), Float.valueOf(cookedAccel[1]) }; 
			_tiltsMsgArray[2]=Float.valueOf(_cookedAccelArray[_shouldSwapAxes ? 0 : 1]);
			PdBase.sendList("fromSystem", _tiltsMsgArray);
			//Log.v(TAG, "accel "+event.values[0]+" "+event.values[1]+" "+event.values[2] ); //-10 to 10
			//_accelArray = //{"/accel", Float.valueOf(rawAccel[0]), Float.valueOf(rawAccel[1]), Float.valueOf(rawAccel[2]) }; 
			_accelMsgArray[1] = Float.valueOf(_rawAccelArray[_shouldSwapAxes ? 1 : 0]);
			_accelMsgArray[2] = Float.valueOf(_rawAccelArray[_shouldSwapAxes ? 0 : 1]);
			_accelMsgArray[3] = Float.valueOf(_rawAccelArray[2]);
			PdBase.sendList("fromSystem", _accelMsgArray);
		} else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
			//Log.v(TAG, "gyro "+event.values[0]+" "+event.values[1]+" "+event.values[2] );// diff axes than ios?
			//Object[] msgArray = {"/gyro", Float.valueOf(event.values[0]), Float.valueOf(event.values[1]), Float.valueOf(event.values[2]) }; 
			_gyroMsgArray[1] = Float.valueOf(event.values[0]);
			float yVal = event.values[1]; if (_shouldSwapAxes) yVal *= -1;
			_gyroMsgArray[2] = Float.valueOf(yVal); 
			_gyroMsgArray[3] = Float.valueOf(event.values[2]);
			PdBase.sendList("fromSystem", _gyroMsgArray);
		} else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
			//Log.v(TAG, "rotation "+event.values[0]+" "+event.values[1]+" "+event.values[2]+" "+event.values[3] );//-1 to 1 scale to +/- pi and invert pitch
			//Object[] msgArray = {"/motion", Float.valueOf(event.values[0]*(float)Math.PI), Float.valueOf(-event.values[1]*(float)Math.PI), Float.valueOf(event.values[2]*(float)Math.PI) }; 
			_rotationMsgArray[1] = Float.valueOf(event.values[0]) * Math.PI;
			_rotationMsgArray[2] = Float.valueOf(event.values[1]) * Math.PI;
			_rotationMsgArray[3] = Float.valueOf(event.values[2]) * Math.PI;
			PdBase.sendList("fromSystem", _rotationMsgArray);
		} else if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
			//Log.i(TAG, "comp "+event.values[0]+" "+event.values[1]+" "+event.values[2]);
			//Object[] msgArray = {"/compass", Float.valueOf(event.values[0])}; 
			_compassMsgArray[1]=Float.valueOf(event.values[0]);
			PdBase.sendList("fromSystem", _compassMsgArray);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
	}

	public static String getDocumentsFolderPath() {
		File fileDir = new File(Environment.getExternalStorageDirectory(), "MobMuPlat");//device/sdcard aka simulated storage
		fileDir.mkdir(); // make mobmuplat dir if not there
		return fileDir.getAbsolutePath();
	}

	// OSC
	public void receiveOSCMessage(OSCMessage message) {
		// message should have proper object types, but need to put address back in front of array
		Object[] messageArgs = message.getArguments();
		Object[] newArgs = new Object[messageArgs.length + 1];
		newArgs[0] = message.getAddress();
		for (int i=0;i<messageArgs.length;i++) {
			newArgs[i+1] = messageArgs[i];
		}
		PdBase.sendList("fromNetwork", newArgs);
	}

	@Override
	public void onLocationChanged(Location location) {
		//Log.i(TAG, "loc "+location.getLatitude()+" "+location.getLongitude()+" "+location.getAltitude());

		int latRough = (int)( location.getLatitude()*1000);
		int longRough = (int)(location.getLongitude()*1000);
		int latFine = (int)Math.abs((location.getLatitude() % .001)*1000000);
		int longFine = (int)Math.abs(( location.getLongitude() % .001)*1000000);

		Object[] msgArray = {"/location", 
				Float.valueOf((float)location.getLatitude()), 
				Float.valueOf((float)location.getLongitude()),
				Float.valueOf((float)location.getAltitude()), 
				Float.valueOf((float)location.getAccuracy()),
				Float.valueOf((float)location.getAccuracy()),//repeat since no separat call for vertical accuracy 
				Integer.valueOf(latRough),
				Integer.valueOf(longRough),
				Integer.valueOf(latFine),
				Integer.valueOf(longFine)				
		};
		PdBase.sendList("fromSystem", msgArray);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
	}

	@Override
	public int getSampleRate() {
		if(pdService!=null)return pdService.getSampleRate();
		else return 0;
	}

	@Override
	public void setBackgroundAudioEnabled(boolean backgroundAudioEnabled) {
		_stopAudioWhilePaused = !backgroundAudioEnabled;
	}

	private boolean unpackZipInputStream(InputStream is, String zipname) {
		ZipInputStream zis;
		try {
			String filename;

			zis = new ZipInputStream(new BufferedInputStream(is));          
			ZipEntry ze;
			byte[] buffer = new byte[1024];
			int count;

			while ((ze = zis.getNextEntry()) != null) {

				filename = ze.getName();
				Log.i("ZIP", "opening "+filename);

				// Need to create directories if doesn't exist.
				if (ze.isDirectory()) {
					File fmd = new File(MainActivity.getDocumentsFolderPath(),  filename);
					fmd.mkdirs();
					continue;
				}

				File outFile = new File(MainActivity.getDocumentsFolderPath(), filename);
				if(VERBOSE)Log.i(TAG, "zip writes to: "+outFile.getAbsolutePath());
				FileOutputStream fout = new FileOutputStream(outFile);

				while ((count = zis.read(buffer)) != -1) {
					fout.write(buffer, 0, count);             
				}

				fout.close();               
				zis.closeEntry();
				if(VERBOSE)Log.i(TAG, "zip wrote "+filename);
			}

			zis.close();
			//Log.i("ZIP", "complete");
			showAlert("Unzipped contents of "+zipname+" into Documents folder.");
		} 
		catch(Exception e) {
			e.printStackTrace();
			showAlert("Error unzipping contents of "+zipname);
			return false;
		} 

		return true;
	}

	private boolean unpackZip(String path, String zipname) {    
		//Log.i("ZIP", "unzipping "+path+" "+zipname);
		try {
			InputStream is = new FileInputStream(path + zipname);
			return unpackZipInputStream(is, zipname);
		} catch(Exception e) {
			e.printStackTrace();
			showAlert("Error unzipping contents of "+zipname);
			return false;
		} 
	}

	// HID
	@Override
	public void onInputDeviceAdded(int deviceId) {
		// TODO Auto-generated method stub
		Toast.makeText(this, "input device added!", Toast.LENGTH_SHORT).show();;

	}

	@Override
	public void onInputDeviceChanged(int deviceId) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onInputDeviceRemoved(int deviceId) {
		// TODO Auto-generated method stub
	}

	public void refreshMenuFragment(MMPMenu menu) { //TODO move
		FragmentManager fragmentManager = getSupportFragmentManager();
		if(fragmentManager.getBackStackEntryCount()==1 && 
				(fragmentManager.getBackStackEntryAt(0) instanceof MenuFragment) &&
				((MenuFragment)fragmentManager.getBackStackEntryAt(0)).getMenu() == menu) {
			((MenuFragment)fragmentManager.getBackStackEntryAt(0)).refresh();
		}
	}

	private InputDeviceState getInputDeviceState(InputEvent event) {
		final int deviceId = event.getDeviceId();
		InputDeviceState state = _inputDeviceStates.get(deviceId);
		if (state == null) {
			final InputDevice device = event.getDevice();
			if (device == null) {
				return null;
			}
			state = new InputDeviceState(device);
			_inputDeviceStates.put(deviceId, state);

			if(VERBOSE)Log.i(TAG, device.toString());
		}
		return state;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (event.getSource()==InputDevice.SOURCE_KEYBOARD) {
			Object[] args = new Object[]{"/key", Integer.valueOf(keyCode)};
			PdBase.sendList("fromSystem", args);
			//return false; //don't consum
		} else { //HID
			InputDeviceState state = getInputDeviceState(event);
			if (state != null && state.onKeyDown(event)) { //pd message sent in state.onKeyUp/Down
				if (_hidFragment!=null && _hidFragment.isVisible()) {
					_hidFragment.show(state);
				}
				//return true;   
			}
			//return super.onKeyDown(keyCode, event);
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (event.getSource()==InputDevice.SOURCE_KEYBOARD) {
			Object[] args = new Object[]{"/keyUp", Integer.valueOf(keyCode)};
			PdBase.sendList("fromSystem", args);
			//return true;
		} else { //HID
		InputDeviceState state = getInputDeviceState(event);
		if (state != null && state.onKeyUp(event)) { //pd message sent in state.onKeyUp/Down
			if (_hidFragment!=null && _hidFragment.isVisible()) {
				_hidFragment.show(state);
			}
			//return true;
		}    
			//return super.onKeyUp(keyCode, event);
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		_inputManager.onGenericMotionEvent(event);

		// Check that the event came from a joystick or gamepad since a generic
		// motion event could be almost anything. API level 18 adds the useful
		// event.isFromSource() helper function.
		int eventSource = event.getSource();
		if ((((eventSource & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) ||
				((eventSource & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK))
				&& event.getAction() == MotionEvent.ACTION_MOVE) {
			//int id = event.getDeviceId();
			InputDeviceState state = getInputDeviceState(event);
			if (state != null && state.onJoystickMotion(event)) { //pd message sent in state.onJoystickMotion
				if (_hidFragment!=null && _hidFragment.isVisible()) {
					_hidFragment.show(state);
				}
				return true;
			}
		}
		return super.onGenericMotionEvent(event);
	}
}



