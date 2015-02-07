package com.iglesiaintermedia.mobmuplat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Observable;

import org.apache.http.conn.util.InetAddressUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.iglesiaintermedia.LANdini.LANdiniLANManager;
import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;
import com.illposed.osc.OSCPortOut;

public class NetworkController extends Observable{

	//	private static NetworkController mInstance = null;
	public MainActivity delegate; //TODO make interface?
	private OSCPortIn receiver;
	private OSCPortOut sender;
	//private OSCPortIn landiniPortIn;
	//private OSCPortOut landiniPortOut;
	public AsyncExceptionListener asyncExceptionListener = null;
	//MOre for landini!

	public String outputIPAddressString;
	//public String multicastGroupAddressString;
	public int outputPortNumber;
	public int inputPortNumber;

	static public int DEFAULT_PORT_NUMBER = 54321;

	public LANdiniLANManager landiniManager;
	private Activity _activity;
	
	//private String _ssid;

	final Handler mHandler = new Handler() { //make un-anonymous
    	@Override
    	public void handleMessage(Message msg) {
			//Log.d("HANDLE", String.format("Handler.handleMessage(): msg=%s", msg));
    		delegate.receiveOSCMessage((OSCMessage)msg.obj);
    	}
    };
    
	public OSCListener oscListener = new OSCListener() {
		//@Override
		public void acceptMessage(java.util.Date time, OSCMessage message) {
			/*System.out.println("Message received2! instance"+ this);
    		System.out.println(message.getAddress());
    		Object[] args = message.getArguments();
    		for(int i=0;i<args.length;i++)
    			System.out.print(" "+args[i]);//type Integer, Float*/
			/*for(MMPController controller:controllerArrayList){
        		controller.receiveMessage(message);
        	}*/
			//Log.i("NETWORK", "receive osc!");
			if (delegate != null){
				//delegate.receiveOSCMessage(message);
				Message msg = Message.obtain();
				msg.obj = message;
				mHandler.sendMessage(msg);
			}
		}
	};

	public NetworkController(Activity activity) {
		super();
		_activity = activity;
		setupOSC();
		landiniManager = new LANdiniLANManager(this);
		
	}

	private void setupOSC() {
		/*outputPortNumber = 54321;
		inputPortNumber = 54322;
		outputIPAddressString = "224.0.0.1";
		*/
		// get user pref numbers
		SharedPreferences sp = _activity.getPreferences(Activity.MODE_PRIVATE);	
		outputIPAddressString = sp.getString("outputIPAddress", "224.0.0.1");
		outputPortNumber = sp.getInt("outputPortNumber", 54321);
		inputPortNumber = sp.getInt("inputPortNumber", 54322);
		
		
		resetOutput();
		resetInput();	
	}
	/*
	private void showBadHostAlert(String string) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(string);
		builder.setCancelable(false);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
		  {
		    public void onClick(DialogInterface dialog, int id)
		    {
		      dialog.dismiss();

		    }
		  });
		AlertDialog alert = builder.create();
		alert.show();
	}*/

	/*public void setMulticastGroupAddress(String newIP) {
		multicastGroupAddressString = newIP;
		resetInput();
	}*/

	public void setOutputIPAddress(String newIP) {
		outputIPAddressString = newIP;
		resetOutput();
		
		SharedPreferences settings = _activity.getPreferences(Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("outputIPAddress", outputIPAddressString);
		editor.commit();
	}

	
	public void setOutputPortNumber(int number) {
		/*if (number < 1000 || number > 65535) {
			return;
		}*/
		outputPortNumber = number;
		resetOutput();
		resetInput();
		
		SharedPreferences settings = _activity.getPreferences(Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt("outputPortNumber", outputPortNumber);
		editor.commit();
	}
	public void setInputPortNumber(int number) {
		/*if (number < 1000 || number > 65535) {
			return;
		}*/
		inputPortNumber = number;
		resetOutput();
		resetInput();
		
		SharedPreferences settings = _activity.getPreferences(Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt("inputPortNumber", inputPortNumber);
		editor.commit();
		
	}
	

	public void resetOutput(){
		new SetupOSCTask().execute();
	}

	public void resetInput() {
		//if (receiver!=null)receiver.close();
		try{
			receiver = new OSCPortIn(inputPortNumber); //added to multicast group 224.0.0.1
			receiver.addListener(".*", oscListener); //pattern no longer matters, hacked class to send everything to all listeners
			receiver.startListening();

		}catch(SocketException e){//not called with multicastsocket
			if(MainActivity.VERBOSE)Log.e("NETWORK","receiver socket exception");	
			if(asyncExceptionListener != null) {
				asyncExceptionListener.receiveException(e, "Unable to listen on port "+inputPortNumber+". Perhaps another application is using this port (or you are not connected to a wifi network).", "port");
			}
		} catch (IOException e) {
			if(MainActivity.VERBOSE)Log.e("NETWORK","receiver IO exception from multi");
			if(asyncExceptionListener != null) {
				asyncExceptionListener.receiveException(e, "Multicast receiver IO error. Perhaps your hardware does not support multicast.", "ip");
			}
		} catch (IllegalArgumentException e){
			if(asyncExceptionListener != null) {
				asyncExceptionListener.receiveException(e, "Bad port number, try a value between 1000 to 65535", "port");
			}
		}
	}

	public void sendMessage(Object[] args){
		new SendOSCTask().execute(args);
	}

	public static String getIPAddress(boolean useIPv4) { 
		try {
			List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces()); //lots of allocaiton here :(
			for (NetworkInterface intf : interfaces) {
				List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
				for (InetAddress addr : addrs) {
					if (!addr.isLoopbackAddress()) {
						String sAddr = addr.getHostAddress().toUpperCase();
						boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr); 
						if (useIPv4) {
							if (isIPv4) 
								return sAddr;
						} else {
							if (!isIPv4) {
								int delim = sAddr.indexOf('%'); // drop ip6 port suffix
								return delim<0 ? sAddr : sAddr.substring(0, delim);
							}
						}
					}
				}
			}
		} catch (Exception ex) {
			if(MainActivity.VERBOSE)Log.e("NETWORK", "failed to get my ip");
		} // for now eat exceptions
		return null;
	}

	public static String getDeviceName() {
		String manufacturer = Build.MANUFACTURER;
		String model = Build.MODEL;
		String serial = Build.SERIAL;
		if (model.startsWith(manufacturer)) {
			return model+" "+serial;
		} else {
			return manufacturer + " " + model+" "+serial;
		}
	}

	public void handlePDMessage(Object[] args) {
		if (args.length == 0 )return;
		//
		//look for LANdini - this clause looks for /send, /send/GD, /send/OGD
		//String address = (String)args[0];//check for isntanceof string first
		if(args[0].equals("/send") || args[0].equals("/send/GD") || args[0].equals("/send/OGD")) {
			if (landiniManager.isEnabled()) {
				//send directly, not through localhost!
				OSCMessage msg = LANdiniLANManager.OSCMessageFromList(Arrays.asList(args));
				landiniManager.oscListener.acceptMessage(null, msg);
				//[outPortToLANdini sendThisPacket:[OSCPacket createWithContent:[ViewController oscMessageFromList:list]]];

			}
			/*else {
            //landini disabled: remake message without the first 2 landini elements and send out normal port
            if([list count]>2){
             NSArray* newList = [list subarrayWithRange:NSMakeRange(2, [list count]-2)];
             [outPort sendThisPacket:[OSCPacket createWithContent:[ViewController oscMessageFromList:newList]]];
            }
        }*/
		}
		//other landini messages, keep passing to landini
		else if (args[0].equals("/networkTime") ||
				args[0].equals("/numUsers") ||
				args[0].equals("/userNames") ||
				args[0].equals("/myName") ){

        //[outPortToLANdini sendThisPacket:[OSCPacket createWithContent:[ViewController oscMessageFromList:list]]];
			OSCMessage msg = LANdiniLANManager.OSCMessageFromList(Arrays.asList(args));
			landiniManager.oscListener.acceptMessage(null, msg); //DANGEROUS! responders might not be set up...
    }
		//not for landini - send out regular!
		else{
			sendMessage(args);
			//[outPort sendThisPacket:[OSCPacket createWithContent:[ViewController oscMessageFromList:list]]];
		}
	}

	public void newSSIDData(){
		setChanged();
		notifyObservers();
	}
	
	public String getSSID() {
		WifiManager wifiManager = (WifiManager) delegate.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return wifiInfo.getSSID();
	}

	private class SetupOSCTask extends AsyncTask<Void, Void, Void> {
		UnknownHostException uhe = null;
		SocketException se = null;
		@Override
		protected Void doInBackground(Void... values) {
			if (sender!=null)sender.close();
			try{
				InetAddress outputIPAddress = InetAddress.getByName(outputIPAddressString);
				sender = new OSCPortOut(outputIPAddress, outputPortNumber);	
			}catch(UnknownHostException e){
				if(MainActivity.VERBOSE)Log.e("NETWORK","unknown host exception");
				uhe = e;
			}catch(SocketException e){
				if(MainActivity.VERBOSE)Log.e("NETWORK","sender socket exception");	
				se = e;
				//JOptionPane.showMessageDialog(null, "Unable to create OSC sender on port 54300. \nI won't be able to receive messages from PD. \nPerhaps another application, or instance of this editor, is on this port.");			
			}

			return null;

		}

		@Override
		protected void onPostExecute(Void value) {
			//Log.i("NETWORK", "completed setup");
			if (uhe!=null) {
				if(asyncExceptionListener != null) {
					asyncExceptionListener.receiveException(uhe, "Unknown host. This IP address is invalid", "ip");
				}
			}
			if (se!=null) {
				if(asyncExceptionListener != null) {
					asyncExceptionListener.receiveException(se, "Unable to send on port "+outputPortNumber+".", "port");
				}
			}
		}
	}

	private class SendOSCTask extends AsyncTask<Object, Void, Void> {
		@Override
		protected Void doInBackground(Object... args) {

			if (args.length == 0 || !(args[0] instanceof String) || sender == null) return null;
			try{
				String address = (String)args[0];
				Object args2[] = Arrays.copyOfRange(args, 1, args.length); //last arg is exclusive
				OSCMessage msg = new OSCMessage(address, args2);
				sender.send(msg);
			}
			catch (IOException e) {
				Log.e("NETWORK", "Couldn't send OSC message,  "+e.getMessage());
			}

			return null;
		}

		/*
		@Override
		protected void onPostExecute(Void value) {
			
		}*/
	}
}
