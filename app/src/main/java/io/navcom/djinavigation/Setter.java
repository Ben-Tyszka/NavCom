package io.navcom.djinavigation;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import dji.sdk.FlightController.DJIFlightController;
import dji.sdk.FlightController.DJIFlightControllerDataType;
import dji.sdk.Products.DJIAircraft;
import dji.sdk.SDKManager.DJISDKManager;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIError;
import dji.sdk.base.DJISDKError;


public class Setter extends Activity {
    private static final String TAG = Setter.class.getName();
    public static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";
    private static DJIBaseProduct mProduct;
    private Handler mHandler, updateHandle;
    private final int MY_PERMISSIONS_REQUEST_READ_STATE = 0;
    private DJIFlightController mFlightController;
    private double mlat, mlong;
    TextView latlong_display, gps_available_display, heading_display, statlite_num_display, alti_display;
    Button btn;
    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        //AssetManager am = this.getApplicationContext().getAssets();
        //Typeface typeface = Typeface.createFromAsset(am, String.format(Locale.US, "fonts/%s", "OpenSans-Light.ttf"));
        latlong_display = (TextView)findViewById(R.id.latlongview);
        gps_available_display = (TextView)findViewById(R.id.gps_view);
        heading_display = (TextView)findViewById(R.id.heading_view);
        statlite_num_display = (TextView)findViewById(R.id.sat_num);
        alti_display = (TextView)findViewById(R.id.alti);
        btn = (Button)findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMapsAct();
            }
        });
        //Init
        if (getApplicationContext().checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    MY_PERMISSIONS_REQUEST_READ_STATE);
        }else if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED){
            mHandler = new Handler(Looper.getMainLooper());
            updateHandle = new Handler();
            DJISDKManager.getInstance().initSDKManager(this, mDJISDKManagerCallback);

            //tx.setTypeface(typeface);
            IntentFilter filter = new IntentFilter();
            filter.addAction(FLAG_CONNECTION_CHANGE);
            registerReceiver(mReceiver, filter);
        }

    }
    public void startMapsAct(){
        Intent i = new Intent(this, auto_mapper.class);
        startActivity(i);
        finish();

    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(mReceiver);
        updateHandle.removeCallbacks(r);
        mHandler.removeCallbacks(Looper.getMainLooper().getThread());

    }
    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onProductConnectionChange();
        }
    };
    private void onProductConnectionChange()
    {
        initFlightController();
    }
    private void initFlightController() {
        //Toast.makeText(getApplicationContext(), "ifc", Toast.LENGTH_SHORT).show();
        DJIBaseProduct product = mProduct;
        if (product != null && product.isConnected()) {
            Toast.makeText(getApplicationContext(), "Not null and connected", Toast.LENGTH_LONG).show();
            if (product instanceof DJIAircraft) {
                //Toast.makeText(getApplicationContext(), "Is aircraft", Toast.LENGTH_LONG).show();
                mFlightController = ((DJIAircraft) product).getFlightController();
            }
        }
        if (mFlightController != null) {
            //Toast.makeText(getApplicationContext(), "Should call", Toast.LENGTH_LONG).show();
            Log.e("BEN", "HIT ME UP");
            updateDroneInfo();
          /*  mFlightController.setUpdateSystemStateCallback(new DJIFlightControllerDelegate.FlightControllerUpdateSystemStateCallback() {
                @Override
                public void onResult(DJIFlightControllerDataType.DJIFlightControllerCurrentState state) {
                    mlat = state.getAircraftLocation().getLatitude();
                    mlong = state.getAircraftLocation().getLongitude();

                    tx.setText("Lat: " + mlat + " Long: " + mlong);
                    //updateDroneLocation();
                }
            });*/
        }
    }
    public void updateDroneInfo(){
        //Toast.makeText(getApplicationContext(), "r call", Toast.LENGTH_LONG).show();
        r.run();
    }
    Runnable r = new Runnable() {

        @Override
        public void run() {
            //Toast.makeText(getApplicationContext(), "Running", Toast.LENGTH_LONG).show();
            try{
                DJIFlightControllerDataType.DJIFlightControllerCurrentState curr_state = mFlightController.getCurrentState();

                latlong_display.setText("Lat: " + curr_state.getAircraftLocation().getLatitude() + "\nLong: " + curr_state.getAircraftLocation().getLongitude());
                latlong_display.setTextColor(Color.WHITE);
                heading_display.setText(Double.toString(mFlightController.getCompass().getHeading()));
                heading_display.setTextColor(Color.WHITE);
                statlite_num_display.setText("Satellites Connected: " + (int)curr_state.getSatelliteCount());
                alti_display.setText(curr_state.getAircraftLocation().getAltitude() + " ft");
                if (curr_state.getSatelliteCount() > 5) {
                    statlite_num_display.setTextColor(Color.WHITE);
                } else if (curr_state.getSatelliteCount() > 5) {
                    statlite_num_display.setTextColor(Color.RED);
                }
                DJIFlightControllerDataType.DJIGPSSignalStatus gps_stat = curr_state.getGpsSignalStatus();
                if (gps_stat == DJIFlightControllerDataType.DJIGPSSignalStatus.Level0 || gps_stat == DJIFlightControllerDataType.DJIGPSSignalStatus.None) {
                    gps_available_display.setText("GPS Not available " + gps_stat);
                    gps_available_display.setTextColor(Color.RED);
                } else if (gps_stat == DJIFlightControllerDataType.DJIGPSSignalStatus.Level5 || gps_stat == DJIFlightControllerDataType.DJIGPSSignalStatus.Level4 || gps_stat == DJIFlightControllerDataType.DJIGPSSignalStatus.Level3) {
                    gps_available_display.setText("GPS Online " + gps_stat);
                    gps_available_display.setTextColor(Color.WHITE);
                } else if (gps_stat == DJIFlightControllerDataType.DJIGPSSignalStatus.Level2 || gps_stat == DJIFlightControllerDataType.DJIGPSSignalStatus.Level3) {
                    gps_available_display.setText("GPS Weak " + gps_stat);
                    gps_available_display.setTextColor(Color.YELLOW);
                }
            }finally {
                updateHandle.postDelayed(r, 1250);
            }

        }
    };
    private DJISDKManager.DJISDKManagerCallback mDJISDKManagerCallback = new DJISDKManager.DJISDKManagerCallback() {
        @Override
        public void onGetRegisteredResult(DJIError error) {
            Log.d(TAG, error == null ? "success" : error.getDescription());
            if(error == DJISDKError.REGISTRATION_SUCCESS) {
                Log.e("Ben", "Suc1");
                DJISDKManager.getInstance().startConnectionToProduct();
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("Ben", "Suc");
                        Toast.makeText(getApplicationContext(), "Register App Successful", Toast.LENGTH_SHORT).show();
                        //initFlightController();
                    }
                });
            } else {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("Ben", "Fai");
                        Toast.makeText(getApplicationContext(), "Register App Failed! Please enter your App Key and check the network.", Toast.LENGTH_LONG).show();
                    }
                });
            }
            Log.e("TAG", "lmao"+error.toString());
        }
        @Override
        public void onProductChanged(DJIBaseProduct oldProduct, DJIBaseProduct newProduct) {
            mProduct = newProduct;
            if(mProduct != null) {
                mProduct.setDJIBaseProductListener(mDJIBaseProductListener);
            }
            notifyStatusChange();
        }
    };
    private DJIBaseProduct.DJIBaseProductListener mDJIBaseProductListener = new DJIBaseProduct.DJIBaseProductListener() {
        @Override
        public void onComponentChange(DJIBaseProduct.DJIComponentKey key, DJIBaseComponent oldComponent, DJIBaseComponent newComponent) {
            if(newComponent != null) {
                newComponent.setDJIComponentListener(mDJIComponentListener);
            }
            notifyStatusChange();
        }
        @Override
        public void onProductConnectivityChanged(boolean isConnected) {
            notifyStatusChange();
        }
    };
    private DJIBaseComponent.DJIComponentListener mDJIComponentListener = new DJIBaseComponent.DJIComponentListener() {
        @Override
        public void onComponentConnectivityChanged(boolean isConnected) {
            notifyStatusChange();
        }
    };
    private void notifyStatusChange() {
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable, 500);
    }
    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            sendBroadcast(intent);
        }
    };




    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_STATE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e("Ben", "Granted!");
                    mHandler = new Handler(Looper.getMainLooper());
                    DJISDKManager.getInstance().initSDKManager(this, mDJISDKManagerCallback);
                    IntentFilter filter = new IntentFilter();
                    filter.addAction(FLAG_CONNECTION_CHANGE);
                    registerReceiver(mReceiver, filter);
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {
                    Log.e("Ben", "Stoppp");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}
