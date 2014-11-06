package com.example.connection.gopher.service;

/**
 * Created by ryan on 3/11/2014.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.example.connection.gopher.R;
import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.accessory.SA;
import com.samsung.android.sdk.accessory.SAAgent;
import com.samsung.android.sdk.accessory.SAPeerAgent;
import com.samsung.android.sdk.accessory.SASocket;

import java.io.IOException;
import java.util.HashMap;

public class HelloAccessoryProviderService extends SAAgent {
    public static final String TAG = "HelloAccessoryProviderService";

    public static final int SERVICE_CONNECTION_RESULT_OK = 0;

    public static final int HELLOACCESSORY_CHANNEL_ID = 404;

    HashMap<Integer, HelloAccessoryProviderConnection> mConnectionsMap = null;

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public HelloAccessoryProviderService getService() {
            return HelloAccessoryProviderService.this;
        }
    }

    public HelloAccessoryProviderService() {
        super(TAG, HelloAccessoryProviderConnection.class);
    }

    public class HelloAccessoryProviderConnection extends SASocket {
        private int mConnectionId;

        public HelloAccessoryProviderConnection() {
            super(HelloAccessoryProviderConnection.class.getName());
        }

        @Override
        public void onError(int channelId, String errorString, int error) {
            Log.e(TAG, "Connection is not alive ERROR: " + errorString + "  "
                    + error);
        }

        @Override
        public void onReceive(int channelId, byte[] data) {

            // registering the BroadcastReceiver here ensures you, that the Gear connection has been already established
            GearDataReceiver gearDataReceiver = new GearDataReceiver();
            IntentFilter intentFilter = new IntentFilter("myData");
            registerReceiver(gearDataReceiver, intentFilter);


            // for example
            Toast.makeText(getBaseContext(),
                    new String(data), Toast.LENGTH_LONG)
                    .show();
        }


        public void sendNotification(final String notification) {

            final HelloAccessoryProviderConnection uHandler = mConnectionsMap.get(mConnectionId);

            if(uHandler == null){
                Log.e(TAG,"Error, can not get handler");
                return;
            }

            new Thread(new Runnable() {
                public void run() {
                    try {
                        uHandler.send(HELLOACCESSORY_CHANNEL_ID, notification.getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        }

        @Override
        protected void onServiceConnectionLost(int errorCode) {
            Log.e(TAG, "onServiceConectionLost  for peer = " + mConnectionId
                    + "error code =" + errorCode);

            if (mConnectionsMap != null) {
                mConnectionsMap.remove(mConnectionId);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate of smart view Provider Service");

        SA mAccessory = new SA();
        try {
            mAccessory.initialize(this);
        } catch (SsdkUnsupportedException e) {
            // Error Handling
        } catch (Exception e1) {
            Log.e(TAG, "Cannot initialize Accessory package.");
            e1.printStackTrace();
			/*
			 * Your application can not use Accessory package of Samsung
			 * Mobile SDK. You application should work smoothly without using
			 * this SDK, or you may want to notify user and close your app
			 * gracefully (release resources, stop Service threads, close UI
			 * thread, etc.)
			 */
            stopSelf();
        }

    }

    @Override
    protected void onServiceConnectionRequested(SAPeerAgent peerAgent) {
        acceptServiceConnectionRequest(peerAgent);
    }

    @Override
    protected void onFindPeerAgentResponse(SAPeerAgent arg0, int arg1) {
        // TODO Auto-generated method stub
        Log.d(TAG, "onFindPeerAgentResponse  arg1 =" + arg1);
    }

    @Override
    protected void onServiceConnectionResponse(SASocket thisConnection,
                                               int result) {
        if (result == CONNECTION_SUCCESS) {
            if (thisConnection != null) {
                HelloAccessoryProviderConnection myConnection = (HelloAccessoryProviderConnection) thisConnection;

                if (mConnectionsMap == null) {
                    mConnectionsMap = new HashMap<Integer, HelloAccessoryProviderConnection>();
                }

                myConnection.mConnectionId = (int) (System.currentTimeMillis() & 255);

                Log.d(TAG, "onServiceConnection connectionID = "
                        + myConnection.mConnectionId);

                mConnectionsMap.put(myConnection.mConnectionId, myConnection);

                Toast.makeText(getBaseContext(),
                        "connection established", Toast.LENGTH_LONG)
                        .show();
            } else {
                Log.e(TAG, "SASocket object is null");
            }
        } else if (result == CONNECTION_ALREADY_EXIST) {
            Log.e(TAG, "onServiceConnectionResponse, CONNECTION_ALREADY_EXIST");
        } else {
            Log.e(TAG, "onServiceConnectionResponse result error =" + result);
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    // code below goes to the outer class, which extends SAAgent
    private class GearDataReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals("myData")) {
                String data = intent.getStringExtra("data");


                notifyGear(data);
            }
        }
    }

    public void notifyGear(String notification) {

        for(HelloAccessoryProviderConnection provider : mConnectionsMap.values()) {
            provider.sendNotification(notification);
        }
    }
}