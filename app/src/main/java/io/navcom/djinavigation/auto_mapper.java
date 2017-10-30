package io.navcom.djinavigation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dji.sdk.FlightController.DJIFlightController;
import dji.sdk.FlightController.DJIFlightControllerDataType;
import dji.sdk.MissionManager.DJIMission;
import dji.sdk.MissionManager.DJIMissionManager;
import dji.sdk.MissionManager.DJIWaypoint;
import dji.sdk.MissionManager.DJIWaypointMission;
import dji.sdk.Products.DJIAircraft;
import dji.sdk.SDKManager.DJISDKManager;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIError;
import dji.sdk.base.DJISDKError;

public class auto_mapper extends FragmentActivity implements OnMapReadyCallback {
    LocationManager locationManager;
    LocationListener locationListener;
    private GoogleMap mMap;
    private final int MY_PERMISSIONS_REQUEST_READ_STATE = 0;
    List<LatLng> directions = new ArrayList<LatLng>();
    List<Circle> points_circle = new ArrayList<Circle>();
    private FloatingActionButton fab, more_fab;
    private Button btn_clear, btn_submit_flight_plan, btn_startWayPoint, btn_stopWayPoint;
    private boolean is_ready = false, can_add_point = false;
    private static final String TAG = auto_mapper.class.getName();
    public static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";
    private static DJIBaseProduct mProduct;
    private Handler mHandler, updateHandle;
    private DJIFlightController mFlightController;
    private RelativeLayout lay;
    Circle aircraft;
    private TextView gps_display, alt_display, battery_display;
    private EditText alt_input;
    private float altitude = 3f;
    private float mSpeed = 2.0f;
    private DJIWaypointMission.DJIWaypointMissionFinishedAction mFinishedAction = DJIWaypointMission.DJIWaypointMissionFinishedAction.NoAction;
    private DJIWaypointMission.DJIWaypointMissionHeadingMode mHeadingMode = DJIWaypointMission.DJIWaypointMissionHeadingMode.UsingInitialDirection;
    private DJIWaypointMission mWaypointMission;
    private DJIMissionManager mMissionManager;
    public boolean mission_complete = false;
    MediaPlayer mp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_mapper);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        fab = (FloatingActionButton) findViewById(R.id.addWaypointBtn);
        more_fab = (FloatingActionButton)findViewById(R.id.moreDrone);
        btn_clear = (Button) findViewById(R.id.clear_btn);
        lay = (RelativeLayout)findViewById(R.id.relLayoutInfo);

        gps_display = (TextView)findViewById(R.id.gpsOnline);
        alt_display = (TextView)findViewById(R.id.atti);
        battery_display = (TextView)findViewById(R.id.battery);
        btn_submit_flight_plan = (Button)findViewById(R.id.sfp);
        btn_startWayPoint = (Button)findViewById(R.id.StartWaype);
        btn_stopWayPoint = (Button)findViewById(R.id.stopWaypoint);
        alt_input = (EditText)findViewById(R.id.alt_input_i);


        mHandler = new Handler(Looper.getMainLooper());
        DJISDKManager.getInstance().initSDKManager(this, mDJISDKManagerCallback);

        IntentFilter filter = new IntentFilter();
        filter.addAction(FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);
        updateHandle = new Handler();



        btn_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("BEN", "Clearing");
                clear_waypoints();
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("NewApi")
            @Override
            public void onClick(View v) {
                Log.e("Ben", "Click add");
                if(is_ready){
                    Log.e("Ben", "Map detected ready");
                    fab.setSelected(!fab.isSelected());
                    if(fab.isSelected()){
                        fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_clear_black_24dp, getApplicationContext().getTheme()));
                        can_add_point = true;
                        btn_clear.setVisibility(View.VISIBLE);
                    }else if(!fab.isSelected()){
                        can_add_point = false;
                        btn_clear.setVisibility(View.GONE);
                        fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_add_black_24dp, getApplicationContext().getTheme()));

                    }

                }else {
                    Toast.makeText(auto_mapper.this, "Not ready", Toast.LENGTH_SHORT).show();
                }
            }
        });
        more_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mp = MediaPlayer.create(getApplicationContext() , R.raw.autopilot_disconnect);
                Log.e("BEN", "MORE");
                more_fab.setSelected(!more_fab.isSelected());
                if(more_fab.isSelected()){
                    Log.e("BEN", "ENABLE");
                    lay.setVisibility(View.VISIBLE);
                }else if(!more_fab.isSelected()){
                    Log.e("BEN", "DISABLE");
                    lay.setVisibility(View.GONE);
                }


            }
        });
        btn_submit_flight_plan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("BEN", "CLick");
                btn_stopWayPoint.setVisibility(View.VISIBLE);
                btn_startWayPoint.setVisibility(View.VISIBLE);

                configWayPointMission();
                prepareWayPointMission();
            }
        });
        btn_stopWayPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopWaypointMission();
                Log.e("BEN", "\nSTOP MISSION\n");
            }
        });
        btn_startWayPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("BEN", "\nSTART MISSION\n");
                mission_complete = false;
                startWaypointMission();
                Toast.makeText(auto_mapper.this, "Start", Toast.LENGTH_SHORT).show();
            }
        });

    }
    public void clear_waypoints(){
        if(points_circle.size() == 0){
            Log.e("Ben", "Cant");
            return;
        }
        for(int x = 0; x < points_circle.size(); x++){
            points_circle.get(x).remove();
        }
        directions.clear();
        points_circle.clear();
        if(mWaypointMission != null){
            mWaypointMission.removeAllWaypoints();
        }
        mMap.clear();
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
            //Toast.makeText(getApplicationContext(), "Not null and connected", Toast.LENGTH_LONG).show();
            if (product instanceof DJIAircraft) {
                //Toast.makeText(getApplicationContext(), "Is aircraft", Toast.LENGTH_LONG).show();
                mFlightController = ((DJIAircraft) product).getFlightController();
            }
        }
        if (mFlightController != null) {
            //Aircraft Online
            initMissionManager();
            enable_More_info();
            updateAircraftInfo();
            //Toast.makeText(getApplicationContext(), "Should call", Toast.LENGTH_LONG).show();

        }
    }

    //DJI app registration stuff
    private DJISDKManager.DJISDKManagerCallback mDJISDKManagerCallback = new DJISDKManager.DJISDKManagerCallback() {
        @Override
        public void onGetRegisteredResult(DJIError error) {
            Log.d(TAG, error == null ? "success" : error.getDescription());
            if(error == DJISDKError.REGISTRATION_SUCCESS) {
                DJISDKManager.getInstance().startConnectionToProduct();
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Register App Successful", Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Register App Failed! Please enter your App Key and check the network.", Toast.LENGTH_LONG).show();
                    }
                });
            }
            Log.e("TAG", error.toString());
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
    //End app registration
    Runnable r = new Runnable() {

        @Override
        public void run() {
            try{
                DJIFlightControllerDataType.DJIFlightControllerCurrentState curr_state = mFlightController.getCurrentState();
                Log.e("BEN", "Update");
                if(aircraft == null){
                    LatLng aircraft_pos = new LatLng(curr_state.getAircraftLocation().getLatitude(), curr_state.getAircraftLocation().getLongitude());
                    aircraft = mMap.addCircle(new CircleOptions().center(aircraft_pos).radius(0.62).strokeColor(0x75ff5c33).fillColor(0x75ff5c33));
                    alt_display.setText(curr_state.getAircraftLocation().getAltitude() + " ft");
                    gps_display.setText(curr_state.getGpsSignalStatus().toString());
                    if(curr_state.getGpsSignalStatus() == DJIFlightControllerDataType.DJIGPSSignalStatus.Level5 || curr_state.getGpsSignalStatus() == DJIFlightControllerDataType.DJIGPSSignalStatus.Level4){
                        gps_display.setTextColor(Color.GREEN);
                    }else if(curr_state.getGpsSignalStatus() == DJIFlightControllerDataType.DJIGPSSignalStatus.Level3 || curr_state.getGpsSignalStatus() == DJIFlightControllerDataType.DJIGPSSignalStatus.Level2){
                        gps_display.setTextColor(Color.YELLOW);
                    }else if(curr_state.getGpsSignalStatus() == DJIFlightControllerDataType.DJIGPSSignalStatus.Level1 || curr_state.getGpsSignalStatus() == DJIFlightControllerDataType.DJIGPSSignalStatus.Level0){
                        gps_display.setTextColor(Color.RED);
                    }
                    battery_display.setText(curr_state.getRemainingBattery().toString());
                    if(curr_state.getRemainingBattery() == DJIFlightControllerDataType.DJIAircraftRemainingBatteryState.Normal){
                        battery_display.setTextColor(Color.GREEN);
                    }else if(curr_state.getRemainingBattery() == DJIFlightControllerDataType.DJIAircraftRemainingBatteryState.Low || curr_state.getRemainingBattery() == DJIFlightControllerDataType.DJIAircraftRemainingBatteryState.VeryLow){
                        battery_display.setTextColor(Color.RED);
                    }else if(curr_state.getRemainingBattery() == DJIFlightControllerDataType.DJIAircraftRemainingBatteryState.Reserved){
                        battery_display.setTextColor(Color.RED);
                    }
                }else {
                    aircraft.remove();
                    LatLng aircraft_pos = new LatLng(curr_state.getAircraftLocation().getLatitude(), curr_state.getAircraftLocation().getLongitude());

                    aircraft = mMap.addCircle(new CircleOptions().center(aircraft_pos).radius(0.62).strokeColor(0x75ff5c33).fillColor(0x75ff5c33));
                    alt_display.setText(curr_state.getAircraftLocation().getAltitude() + " ft");
                    gps_display.setText(curr_state.getGpsSignalStatus().toString());
                    if(curr_state.getGpsSignalStatus() == DJIFlightControllerDataType.DJIGPSSignalStatus.Level5 || curr_state.getGpsSignalStatus() == DJIFlightControllerDataType.DJIGPSSignalStatus.Level4){
                        gps_display.setTextColor(Color.GREEN);
                    }else if(curr_state.getGpsSignalStatus() == DJIFlightControllerDataType.DJIGPSSignalStatus.Level3 || curr_state.getGpsSignalStatus() == DJIFlightControllerDataType.DJIGPSSignalStatus.Level2){
                        gps_display.setTextColor(Color.YELLOW);
                    }else if(curr_state.getGpsSignalStatus() == DJIFlightControllerDataType.DJIGPSSignalStatus.Level1 || curr_state.getGpsSignalStatus() == DJIFlightControllerDataType.DJIGPSSignalStatus.Level0){
                        gps_display.setTextColor(Color.RED);
                    }
                    battery_display.setText(curr_state.getRemainingBattery().toString());
                    if(curr_state.getRemainingBattery() == DJIFlightControllerDataType.DJIAircraftRemainingBatteryState.Normal){
                        battery_display.setTextColor(Color.GREEN);
                    }else if(curr_state.getRemainingBattery() == DJIFlightControllerDataType.DJIAircraftRemainingBatteryState.Low || curr_state.getRemainingBattery() == DJIFlightControllerDataType.DJIAircraftRemainingBatteryState.VeryLow){
                        battery_display.setTextColor(Color.RED);
                    }else if(curr_state.getRemainingBattery() == DJIFlightControllerDataType.DJIAircraftRemainingBatteryState.Reserved){
                        battery_display.setTextColor(Color.RED);
                    }
                }

            }finally {
                updateHandle.postDelayed(r, 1000);
            }
        }

    };

    public void updateAircraftInfo(){
        r.run();
    }

    public void enable_More_info(){
        more_fab.setVisibility(View.VISIBLE);
    }
    public void disable_More_info(){
        more_fab.setVisibility(View.GONE);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.removeUpdates(locationListener);
        updateHandle.removeCallbacks(r);
        mission_complete = true;
        stopWaypointMission();
        //mHandler.removeCallbacks(Looper.getMainLooper().getThSread());
    }


    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        if (getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_READ_STATE);
        }else if(getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            Log.e("Ben", "On map ready");
            locationListener = new LocationListener() {
                int counter = 0;
                public void onLocationChanged(Location location) {
                    if(counter == 0){
                        Log.e("Ben", "Update");
                        LatLng here = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(here, 19));
                        //CircleOptions ops = new CircleOptions().center(here).fillColor(0x9500e673).strokeColor(0x9500e673).radius(0.6d).strokeWidth(0.6f);
                        //mMap.addCircle(ops);
                        is_ready = true;
                        counter++;
                    }
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                public void onProviderEnabled(String provider) {
                }

                public void onProviderDisabled(String provider) {
                }
            };


            //noinspection MissingPermission
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            int clicks = 0;
            @Override
            public void onMapClick(LatLng point) {

                if(can_add_point){
                    clicks++;
                    Circle circle =  mMap.addCircle(new CircleOptions().center(point).radius(0.7).strokeWidth(0.5f).fillColor(0x751ad1ff).strokeColor(0x751ad1ff));
                    directions.add(point);
                    points_circle.add(circle);
                    if(directions.size() > 1){
                        Log.e("BEN", String.valueOf(directions.size()));//"Old point: " + directions.get(directions.size() - 2).toString());
                        Polyline line = mMap.addPolyline(new PolylineOptions()
                                .add(directions.get(directions.size() - 2), point)
                                .width(1.6f)
                                .color(0x8033ff33));
                    }
                    DJIWaypoint mWaypoint = new DJIWaypoint(point.latitude, point.longitude, altitude);
                    if (mWaypointMission != null) {
                        mWaypointMission.addWaypoint(mWaypoint);
                        Toast.makeText(getApplicationContext(), "Added Waypoint", Toast.LENGTH_SHORT).show();
                    }
                    Log.e("List of points", directions.toString());
                    //mMap.addMarker(new MarkerOptions().position(point).title(Integer.toString(clicks)).draggable(true));
                }
            }
        });
    }
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
    private void configWayPointMission(){
        altitude = Float.valueOf(alt_input.getText().toString());
        if (mWaypointMission != null){
            mWaypointMission.finishedAction = mFinishedAction;
            mWaypointMission.headingMode = mHeadingMode;
            mWaypointMission.autoFlightSpeed = mSpeed;
            if (mWaypointMission.waypointsList.size() > 0){
                for (int i=0; i< mWaypointMission.waypointsList.size(); i++){
                    mWaypointMission.getWaypointAtIndex(i).altitude = altitude;
                }
                Toast.makeText(getApplicationContext(), "Configured mission", Toast.LENGTH_SHORT).show();
            }
        }
        Log.e("BEN", "Submitted alt is: " + altitude + " ft");
    }
    private void initMissionManager() {
        DJIBaseProduct product = mProduct;
        if (product == null || !product.isConnected()) {
            Log.e("BEN", "IMM has a probelm");
            mMissionManager = null;
            return;
        } else {
            Log.e("BEN", "IMM online");
            mMissionManager = product.getMissionManager();
            mMissionManager.setMissionProgressStatusCallback(new DJIMissionManager.MissionProgressStatusCallback() {
                @Override
                public void missionProgressStatus(DJIMission.DJIMissionProgressStatus djiMissionProgressStatus) {
                    //oast.makeText(getApplicationContext(), "PSU", Toast.LENGTH_SHORT).show();
                }
            });
            mMissionManager.setMissionExecutionFinishedCallback(new DJIBaseComponent.DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    Toast.makeText(getApplicationContext(), "Mission finished", Toast.LENGTH_SHORT).show();
                    mission_complete = true;
                    Log.e("BEN", "Execution finished: " + (djiError == null ? "Success" : djiError.getDescription()));
                }
            });
        }
        mWaypointMission = new DJIWaypointMission();
    }
    @Override
    protected void onResume(){
        super.onResume();
        initFlightController();
        initMissionManager();
    }
    private void prepareWayPointMission(){
        if (mMissionManager != null && mWaypointMission != null) {
            DJIMission.DJIMissionProgressHandler progressHandler = new DJIMission.DJIMissionProgressHandler() {
                @Override
                public void onProgress(DJIMission.DJIProgressType type, float progress) {
                }
            };
            mMissionManager.prepareMission(mWaypointMission, progressHandler, new DJIBaseComponent.DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    Log.e("BEN", error == null ? "Success Prep" : error.getDescription());
                }
            });
        }
    }
    private void startWaypointMission(){
        if (mMissionManager != null) {
            mMissionManager.startMissionExecution(new DJIBaseComponent.DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    Toast.makeText(auto_mapper.this, (error == null ? "Success" : error.getDescription()), Toast.LENGTH_SHORT).show();
                    Log.e("BEN", "Start: " + (error == null ? "Success" : error.getDescription()));
                }
            });
        }
    }
    private void stopWaypointMission(){
        if (mMissionManager != null) {
            mMissionManager.stopMissionExecution(new DJIBaseComponent.DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    Log.e("BEN", "Stop: " + (error == null ? "Success" : error.getDescription()));
                }
            });
            if (mWaypointMission != null){
                clear_waypoints();
            }
            btn_startWayPoint.setVisibility(View.GONE);
            btn_stopWayPoint.setVisibility(View.GONE);
            if(mission_complete == false){
                //Play sound
                mp.start();
            }
        }
    }
}
