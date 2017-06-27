package com.example.dylanblok.acopagps2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    // Keys for restoring savedInstanceState
    static final String DURATION = "duration";
    static final String DISTANCE = "distance";
    static final String POLYLINE = "polyline";
    static final String BOTTOMSHEET = "bottomsheet";
    static final String LATITUDE = "latitude";
    static final String LONGITUDE = "longitude";
    static final String CAMERA = "camera";
    static final String SCROLLING = "scrolling";

    // Variables for map functionality
    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    private ArrayList<LatLng> points;
    Polyline line;
    CameraPosition startPosition;
    private Double totalDistance;
    private Location currentLocation;
    private final int DEFAULT_ZOOM = 14;
    public static boolean mMapIsScrolling;

    // Variables for bottom sheet functionality
    public static boolean mMapIsTouched;
    private int bottomSheetState = BottomSheetBehavior.STATE_COLLAPSED;

    // Variables for timer functionality
    TextView duration;
    long millis;
    long startTime = 0;
    private long savedTime = 0;


    /*** APP CREATION ***/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        MySupportMapFragment mapFragment = (MySupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        duration = (TextView) findViewById(R.id.duration);
        points = new ArrayList<LatLng>();



        if (savedInstanceState != null) {
            savedTime = savedInstanceState.getLong(DURATION);
            totalDistance = savedInstanceState.getDouble(DISTANCE);
            points = savedInstanceState.getParcelableArrayList(POLYLINE);
            startPosition = savedInstanceState.getParcelable(CAMERA);
            mMapIsScrolling = savedInstanceState.getBoolean(SCROLLING);
        }

        startTime = System.currentTimeMillis();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        timerHandler.postDelayed(timerRunnable, 0);


        /*** BOTTOM SHEET FUNCTIONALITY ***/

        View bottomSheet = findViewById(R.id.design_bottom_sheet);
        final BottomSheetBehavior behavior = BottomSheetBehavior.from(bottomSheet);

        behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                bottomSheetState = newState;
                switch (newState) {
                    case BottomSheetBehavior.STATE_DRAGGING:
                        if (mMapIsTouched) {
                            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                        } else {
                            Log.i("BottomSheetCallback", "BottomSheetBehavior.STATE_DRAGGING");
                        }
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        Log.i("BottomSheetCallback", "BottomSheetBehavior.STATE_SETTLING");
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        Log.i("BottomSheetCallback", "BottomSheetBehavior.STATE_EXPANDED");
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        Log.i("BottomSheetCallback", "BottomSheetBehavior.STATE_COLLAPSED");
                        break;
                    case BottomSheetBehavior.STATE_HIDDEN:
                        Log.i("BottomSheetCallback", "BottomSheetBehavior.STATE_HIDDEN");
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                Log.i("BottomSheetCallback", "slideOffset: " + slideOffset);
            }
        });
    }


    /*** To maintain state on rotation ***/

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putDouble(DISTANCE, totalDistance);
        savedInstanceState.putLong(DURATION, millis);
        savedInstanceState.putParcelableArrayList(POLYLINE, points);
        savedInstanceState.putInt(BOTTOMSHEET, bottomSheetState);
        savedInstanceState.putDouble(LATITUDE, currentLocation.getLatitude());
        savedInstanceState.putDouble(LONGITUDE, currentLocation.getLongitude());
        savedInstanceState.putBoolean(SCROLLING, mMapIsScrolling);
        CameraPosition savedPosition = CameraPosition
                .builder(
                        mMap.getCameraPosition()
                )
                .build();
        savedInstanceState.putParcelable(CAMERA, savedPosition);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        totalDistance = savedInstanceState.getDouble(DISTANCE, 0);
        savedTime = savedInstanceState.getLong(DURATION);
        points = savedInstanceState.getParcelableArrayList(POLYLINE);
        bottomSheetState = savedInstanceState.getInt(BOTTOMSHEET);
        double latitude = savedInstanceState.getDouble(LATITUDE);
        double longitude = savedInstanceState.getDouble(LONGITUDE);
        currentLocation = new Location("gps");
        currentLocation.setLatitude(latitude);
        currentLocation.setLongitude(longitude);

        View bottomSheet = findViewById(R.id.design_bottom_sheet);
        final BottomSheetBehavior behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setState(bottomSheetState);
    }

    /*** MAP FUNCTIONALITY ***/

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Enable location
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        }
        else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }


        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener()
        {
            @Override
            public boolean onMyLocationButtonClick()
            {
                LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM);
                mMap.animateCamera(cameraUpdate);
                mMapIsScrolling = false;
                return true;
            }
        });

    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }

    }

    @Override
    public void onLocationChanged(Location location) {

        currentLocation = location;
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        LatLng latLng = new LatLng(latitude, longitude);

        points.add(latLng);

        addDistance();

        drawLine();

        if(!mMapIsScrolling) {
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM);
            mMap.animateCamera(cameraUpdate);
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted. Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other permissions this app might request.
            // You can add here other case statements according to your requirement.
        }
    }

    /*** ROUTE TRACE ***/

    public void drawLine() {

        mMap.clear();

        PolylineOptions options = new PolylineOptions()
                .width(10)
                .color(Color.BLUE)
                .geodesic(true);
        for (int i = 0; i < points.size(); i++) {
            LatLng point = points.get(i);
            options.add(point);
        }

        line = mMap.addPolyline(options);

    }

    /*** DISTANCE TRACKER ***/

    public void addDistance() {
        totalDistance = 0.0;
        for (int i=1; i < points.size(); i++) {
            LatLng point1 = points.get(i-1);
            LatLng point2 = points.get(i);
            double distance = distance(point1.latitude, point1.longitude, point2.latitude, point2.longitude);
            totalDistance += distance;
        }
        String distanceString = String.format("%.2f",totalDistance) + " km";
        TextView distanceTraveled = (TextView) findViewById(R.id.distance);
        distanceTraveled.setText(distanceString);
    }

    public double distance (double lat_a, double lng_a, double lat_b, double lng_b )
    {
        double earthRadius = 3958.75;
        double latDiff = Math.toRadians(lat_b-lat_a);
        double lngDiff = Math.toRadians(lng_b-lng_a);
        double a = Math.sin(latDiff /2) * Math.sin(latDiff /2) +
                Math.cos(Math.toRadians(lat_a)) * Math.cos(Math.toRadians(lat_b)) *
                        Math.sin(lngDiff /2) * Math.sin(lngDiff /2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double distance = earthRadius * c;

        double kilometerConversion = 1.609;

        return distance * kilometerConversion;
    }

    /*** TIMER ***/

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            millis = System.currentTimeMillis() - startTime + savedTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            int hours = minutes / 60;
            minutes = minutes % 60;
            seconds = seconds % 60;


            duration.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));

            timerHandler.postDelayed(this, 500);
        }
    };


}
