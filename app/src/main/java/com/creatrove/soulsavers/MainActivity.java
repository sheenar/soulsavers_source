package com.creatrove.soulsavers;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.List;
import java.util.Locale;


public class MainActivity extends ActionBarActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    protected static final String TAG = "SOULSAVERS_LOG";
    String projectId = "126245700016";
    public static final String REG_ID = "regId";
    private static final String APP_VERSION = "appVersion";
    String loc1 = "", loc2 = "";

    AsyncTask<Void, Void, String> sendTask;
    MessageSender messageSender;
    GoogleCloudMessaging gcm;
    String regId;
    String signUpUser;
    private boolean signupFlag = false;
    Context context;

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    /**
     * Represents a geographical location.
     */
    protected Location mLastLocation;
    private Intent intent;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        messageSender = new MessageSender();
        gcm = GoogleCloudMessaging.getInstance(getApplicationContext());

        intent = new Intent(this, GCMNotificationIntentService.class);
        registerReceiver(broadcastReceiver, new IntentFilter("com.creatrove.soulsavers.sosmessage"));


        /*TO-DO Registration should happen in 1 step - when the user opens the application*/
        /* Register button*/
        final Button register_button = (Button) findViewById(R.id.register_button);
        register_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "Register Button Clicked check play services "+checkPlayServices());

                if (TextUtils.isEmpty(regId)) {
                    regId = registerGCM();
                    Log.d(TAG, "GCM Registration ID : " + regId);
                }


            }
            });

        /*Register with our 3rd XMPP server for SoulSavers app*/
        final Button register_button_2 = (Button) findViewById(R.id.register_button_2);
        register_button_2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "Register with XMPP Button Clicked");

                if(!regId.isEmpty()) {
                    EditText mUserName = (EditText) findViewById(R.id.userName);
                    signUpUser = mUserName.getText().toString();
                    Bundle dataBundle = new Bundle();
                    dataBundle.putString("ACTION", "SIGNUP");
                    dataBundle.putString("USER_NAME", signUpUser);
                    messageSender.sendMessage(dataBundle,gcm);
                    signupFlag = true;
                    Toast.makeText(context,
                            "Sign Up Complete!",
                            Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Sign Up Complete!!");
                } else {
                    Log.d(TAG, "regID Not Available");
                }


            }
        });

        /* SOS button*/
        final Button sos_button = (Button) findViewById(R.id.sos_button);
        sos_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "SOS Button Clicked");

                /*Toast.makeText(getApplicationContext(),
                        "SOS Clicked!",
                        Toast.LENGTH_LONG).show();*/
                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if (mLastLocation != null) {
                    Log.d(TAG, ": Latitude = "+String.valueOf(mLastLocation.getLatitude())+" Longitude = "+String.valueOf(mLastLocation.getLongitude()));
                    loc1 = String.valueOf(mLastLocation.getLatitude());
                    loc2 = String.valueOf(mLastLocation.getLongitude());
                    /*Send location to server*/
                    sendSosMessage(signUpUser, loc1, loc2);
                } else {
                     Toast.makeText(getApplicationContext(), "Location Unknown", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Last location is null");
                    sendSosMessage(signUpUser, "Unkown", "Unkown");
                }


            }
        });

        buildGoogleApiClient();
    }

    /**
     * Builds a GoogleApiClient. Uses the addApi() method to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {



    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }


    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.d(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    private boolean checkPlayServices() {
        int resultCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            return false;
        }
        return true;

    }

    public String registerGCM() {

        gcm = GoogleCloudMessaging.getInstance(this);
        regId = getRegistrationId();

        if (TextUtils.isEmpty(regId)) {

            registerInBackground();

            Log.d(TAG,
                    "Successfully registered with GCM server. Registration ID = "
                            + regId);

        } else {
            Log.d(TAG,
                    "GCM RegID already available: "
                            + regId
            );
            Toast.makeText(context,
                    "GCM RegID already available: "+ regId,
                    Toast.LENGTH_LONG).show();
        }
        return regId;
    }

    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regId = gcm.register(projectId);
                    Log.d("RegisterActivity", "registerInBackground - RegID: "
                            + regId);
                    msg = "Device registered, Registration ID=" + regId;

                    storeRegistrationId(regId);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    Log.e(TAG, "Error: " + msg);
                }
                Log.d(TAG, "AsyncTask completed: " + msg);
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                Log.d(TAG, "Registered with GCM Server." + msg);
            }
        }.execute(null, null, null);
    }

    private boolean sendSosMessage(String user, String location1, String location2){
        //sending gcm message
        Bundle dataBundle = new Bundle();
        dataBundle.putString("ACTION", "CHAT");
        dataBundle.putString("TOUSER", user);
        dataBundle.putString("CHATMESSAGE", "Save Our Souls-"+user+" @ "+location1+";"+location2);
        messageSender.sendMessage(dataBundle,gcm);

        return true;
    }

    /*Receives SOS Message from other devices*/
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String received_message = intent.getStringExtra("CHATMESSAGE");
            int i= 0 ;
            String location[] = new String[2];

            Log.d(TAG, "onReceive: " + received_message);

            Toast.makeText(context,
                    "onReceive: " + received_message,
                    Toast.LENGTH_LONG).show();

            for (String retval: received_message.split("@ ", 2)){
                if(retval.contains(";")) {
                    i=0;
                    for (String retval2:
                            retval.split(";", 2)){
                        System.out.println
                                (retval2);
                        location[i] = retval2;
                        i++;
                    }
                }
                else
                    continue;
            }
            Log.d(TAG, "onReceive: location = "+location[0]+" and "+location[1]);

           /*Get the Address from the lat and long co-ordinates*/
            if(!(location[0].equals("unknown")) && !location[1].equals("Unkown")) {
                Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                try {
                    List<Address> listAddresses = geocoder.getFromLocation(Double.parseDouble(location[0]), Double.parseDouble(location[1]), 1);
                    if (null != listAddresses && listAddresses.size() > 0) {
                        String _location = listAddresses.get(0).getAddressLine(0)+","+listAddresses.get(0).getAddressLine(0)+", "
                                +listAddresses.get(0).getLocality()+", Pin - "+listAddresses.get(0).getPostalCode();

                        Toast.makeText(context,
                                "location of SOS is " /*+ location[0] + " and " + location[1]*/ + _location,
                                Toast.LENGTH_LONG).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else {
                            Toast.makeText(context,
                    "location of SOS is "+location[0]+" and "+location[1],
                    Toast.LENGTH_LONG).show();
            }


//            Toast.makeText(context,
//                    "location of SOS is "+location[0]+" and "+location[1]+,
//                    Toast.LENGTH_LONG).show();

            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLastLocation != null) {
                Log.d(TAG, ": Latitude = "+String.valueOf(mLastLocation.getLatitude())+" Longitude = "+String.valueOf(mLastLocation.getLongitude()));
                //loc1 = String.valueOf(mLastLocation.getLatitude());
                //loc2 = String.valueOf(mLastLocation.getLongitude());
                if((String.valueOf(mLastLocation.getLatitude())).equals("Unknown") || (String.valueOf(mLastLocation.getLatitude())).equals("Unknown") ) {
                    Toast.makeText(getApplicationContext(), "My Location is Unknown", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Receiver's location unknown");
                }
                else {
                    /*Display the location*/
                    Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                    try {
                        List<Address> listAddresses = geocoder.getFromLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude(), 1);
                        if (null != listAddresses && listAddresses.size() > 0) {
                            String _location = listAddresses.get(0).getAddressLine(0)+", "+listAddresses.get(0).getAddressLine(0)+", "
                                    +listAddresses.get(0).getLocality()+", Pin - "+listAddresses.get(0).getPostalCode();

                            Toast.makeText(context,
                                    "My location is " /*+ location[0] + " and " + location[1]*/ + _location,
                                    Toast.LENGTH_LONG).show();
                            Log.e(TAG, "My location is - "+_location);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

               // Toast.makeText(getApplicationContext(), "My Location is : "+String.valueOf(mLastLocation.getLatitude())+" Longitude = "+String.valueOf(mLastLocation.getLongitude()), Toast.LENGTH_LONG).show();

            } else {
                Toast.makeText(getApplicationContext(), "My Location is Unknown", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Receiver's location unknown");

            }

        }


    };

    private String getRegistrationId() {
        final SharedPreferences prefs = getSharedPreferences(
                MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
        String registrationId = prefs.getString(REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.d(TAG, "Registration not found.");
            return "";
        }
        int registeredVersion = prefs.getInt(APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion();
        if (registeredVersion != currentVersion) {
            Log.d(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    private void storeRegistrationId(String regId) {
        final SharedPreferences prefs = getSharedPreferences(
                MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
        int appVersion = getAppVersion();
        Log.d(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(REG_ID, regId);
        editor.putInt(APP_VERSION, appVersion);
        editor.commit();
    }

    private int getAppVersion() {
        try {
            PackageInfo packageInfo;
            packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("RegisterActivity",
                    "I never expected this! Going down, going down!" + e);
            throw new RuntimeException(e);
        }
    }

}
