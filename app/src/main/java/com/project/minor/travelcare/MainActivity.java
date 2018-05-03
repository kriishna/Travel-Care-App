package com.project.minor.travelcare;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener, View.OnClickListener,
        NavigationView.OnNavigationItemSelectedListener {

    private static final int LOCATION_UPDATE_INTERVAL = 15000;
    private static final int LOCATION_UPDATE_FASTEST_INTERVAL = 10000;
    private static final int ZOOM_LEVEL = 14;

    /**
     * Constant used in the location settings dialog.
     */
    private static final int REQUEST_CHECK_SETTINGS = 0x1;

    /**
     * Code used in requesting runtime permissions.
     */
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    PlaceAutocompleteFragment autocompleteFragment;
    View mapView;
    //Directions
    double latitude, longitude;
    double end_latitude, end_longitude;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastKnownLocation;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firebaseFirestore;
    private GoogleMap mMap;
    private Boolean exit = false;
    private GoogleApiClient client;
    private LocationRequest locationRequest;
    //Service
    // The BroadcastReceiver used to listen from broadcasts from the service.
    private MyReceiver myReceiver;
    // A reference to the service used to get location updates.
    private LocationService mService = null;
    // Tracks the bound state of the service.
    private boolean mBound = false;
    // Monitors the state of the connection to the service.

    //NavBar
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        // Called when a connection  to the Service has been established, with the
        // IBinder of the communication channel to the Service.
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }

    };
    private EditText mileage;
    private EditText fuel;
    private Double possibleDistance;
    private String milOfVehicle;
    private String fuelAmt;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapView = mapFragment.getView();


        mapFragment.getMapAsync(this);

        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable("location");
        }

        mapSetup();
        firebaseSetup();
        setNavDraw();
    }

    private void setNavDraw() {

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, null, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        View view = navigationView.getHeaderView(0);
        mileage = view.findViewById(R.id.mileage);
        fuel = view.findViewById(R.id.fuel);

        navigationView.setNavigationItemSelectedListener(this);


    }

    public void firebaseSetup() {

        mAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
    }

    @TargetApi(Build.VERSION_CODES.O)
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void mapSetup() {

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        ImageView locationPin = findViewById(R.id.location_pin);
        locationPin.setOnClickListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Retrieve the PlaceAutocompleteFragment.
        autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        EditText etPlace = Objects.requireNonNull(autocompleteFragment.getView()).findViewById(R.id.place_autocomplete_search_input);
        etPlace.setTextColor(Color.parseColor("#ffffff"));

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                LatLng coordinate;
                coordinate = place.getLatLng();
                CameraUpdate location = CameraUpdateFactory.newLatLngZoom(
                        coordinate, 14);

                mMap.animateCamera(location);
            }

            @Override
            public void onError(Status status) {
                Toast.makeText(getApplicationContext(), "Place selection failed: " + status.getStatusMessage(),
                        Toast.LENGTH_SHORT).show();
                Log.i("Main", "An error occurred: " + status);
            }

        });
    }


    private void getDeviceLocation() {
        try {
            mFusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(MainActivity.this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {

                            if (location != null) {
                                mLastKnownLocation = location;
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(mLastKnownLocation.getLatitude(),
                                                mLastKnownLocation.getLongitude()), 1));
                            }

                        }
                    });
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        myReceiver = new MyReceiver();
        // Bind to the service. If the service is in foreground mode, this signals to the service
        // that since this activity is in the foreground, the service can exit foreground mode.

        if (checkPermissions()) {
            bindService(new Intent(this, LocationService.class), mServiceConnection,
                    Context.BIND_AUTO_CREATE);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        requestPermissions();
                    }
                }, 3000);

            }
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {

            sendToLoginPage();

        } else {

            String current_user_id = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
            firebaseFirestore.collection("Users").document(current_user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        if (!task.getResult().exists()) {

                            Intent setupIntent = new Intent(MainActivity.this, SetupActivity.class);
                            startActivity(setupIntent);
                            finish();

                        }
                    } else {
                        String error_message = Objects.requireNonNull(task.getException()).getMessage();
                        Toast.makeText(MainActivity.this, "Firestore error: " + error_message, Toast.LENGTH_LONG).show();
                    }
                }
            });


        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!checkPermissions()) {

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    requestPermissions();
                }
            }, 300);

        } else {
            LocalBroadcastManager.getInstance(this).registerReceiver(myReceiver,
                    new IntentFilter(LocationService.ACTION_BROADCAST));
        }
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver);
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (mBound) {
            // Unbind from the service. This signals to the service that this activity is no longer
            // in the foreground, and the service can respond by promoting itself to a foreground
            // service.
            unbindService(mServiceConnection);
            mBound = false;
        }
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i("LOG:", "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Snackbar.make(
                        findViewById(android.R.id.content),
                        "Permission granted!",
                        Snackbar.LENGTH_SHORT)
                        .show();

                bindService(new Intent(this, LocationService.class), mServiceConnection,
                        Context.BIND_AUTO_CREATE);
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    if (client == null) {
                        buildGoogleApiClient();
                    }
                    mMap.setMyLocationEnabled(true);
                }

            }

        }

    }

    private void sendToLoginPage() {

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                Snackbar.make(
                        findViewById(android.R.id.content),
                        "Current location!",
                        Snackbar.LENGTH_SHORT)
                        .show();
                return false;
            }
        });

        mMap.setOnMyLocationClickListener(new GoogleMap.OnMyLocationClickListener() {
            @Override
            public void onMyLocationClick(@NonNull Location location) {
                double lat, lng;
                int acc;

                lat = location.getLatitude();
                lng = location.getLongitude();
                acc = (int) location.getAccuracy();

                Geocoder myLocation = new Geocoder(MainActivity.this, Locale.getDefault());
                List<Address> myList = null;
                try {
                    myList = myLocation.getFromLocation(Double.parseDouble(String.valueOf(lat)), Double.parseDouble(String.valueOf(lng)), 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                assert myList != null;
                Address address = myList.get(0);
                String addressStr = "";
                addressStr += address.getAddressLine(0);
                if (address.getAddressLine(1) != null) {
                    addressStr += ", " + address.getAddressLine(1);
                }
                if (address.getAddressLine(2) != null) {
                    addressStr += ", " + address.getAddressLine(2);
                }

                Snackbar.make(
                        findViewById(android.R.id.content),
                        "Accuracy: " + acc + " m    (" + lat + ", " + lng + ")\nAddress: " + addressStr,
                        Snackbar.LENGTH_SHORT)
                        .show();
            }
        });

        if (mapView != null && mapView.findViewById(Integer.parseInt("1")) != null) {
            // Get the button view
            View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
            // and next place it, on bottom right (as Google Maps app)
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                    locationButton.getLayoutParams();
            // position on right bottom
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            layoutParams.setMargins(0, 0, 50, 50);
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
            getDeviceLocation();
        }

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.flag));
                markerOptions.title(latLng.latitude + " : " + latLng.longitude);
                markerOptions.draggable(true);
                mMap.clear();
                mMap.addMarker(markerOptions);

                // Animating to the touched position
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.setMaxZoomPreference(mMap.getMaxZoomLevel());
            }
        });

    }

    protected synchronized void buildGoogleApiClient() {
        //Google API Client Created
        client = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        client.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        requestLocationUpdate();

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(client, locationRequest, this);
        }

        // For dialog Box that ask for enabling GPS
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        Task<LocationSettingsResponse> task =
                LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());

        task.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    // All location settings are satisfied. The client can initialize location
                    // requests here.


                } catch (ApiException exception) {
                    switch (exception.getStatusCode()) {

                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied. But could be fixed by showing the
                            // user a dialog.
                            try {
                                // Cast to a resolvable exception.
                                ResolvableApiException resolvable = (ResolvableApiException) exception;
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                resolvable.startResolutionForResult(
                                        MainActivity.this,
                                        REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            String errorMessage = "Location settings are inadequate, and cannot be " +
                                    "fixed here. Fix in Settings.";
                            Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();

                            break;
                    }
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i("LOG:", "User agreed to make required location settings changes.");
                        // Nothing to do. startLocationupdates() gets called in onResume again.
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i("LOG", "User chose not to make required location settings changes.");
                        break;
                }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

        latitude = location.getLatitude();
        longitude = location.getLongitude();

        //Get lat and lng of new location
        LatLng locChangedCoordinates = new LatLng(location.getLatitude(), location.getLongitude());

        //Set properties to the marker ie position, icon, title.
        mMap.moveCamera(CameraUpdateFactory.newLatLng(locChangedCoordinates));
        mMap.animateCamera(CameraUpdateFactory.zoomBy(ZOOM_LEVEL));

        if (client != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(client, this);
        }

    }

    private boolean checkPermissions() {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION);
    }


    private void requestLocationUpdate() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(LOCATION_UPDATE_INTERVAL);
        locationRequest.setFastestInterval(LOCATION_UPDATE_FASTEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(client, locationRequest, this);
        }
    }

    public void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i("LOG", "Displaying permission rationale to provide additional context.");

            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i("LOG", "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }

    }

    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    @Override
    public void onBackPressed() {

        if (exit) {
            finish(); // finish activity
        } else {
            Snackbar.make(
                    findViewById(android.R.id.content), "Press Back again to Exit.",
                    Snackbar.LENGTH_SHORT)
                    .show();
            exit = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    exit = false;
                }
            }, 3 * 1000);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.location_pin) {

            if (!TextUtils.isEmpty(mileage.getText().toString()) && !TextUtils.isEmpty(fuel.getText().toString())) {

                final LatLng target = mMap.getCameraPosition().target;
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                @SuppressLint("InflateParams") View dialogView =
                        LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_box, null, false);

                ((TextView) dialogView.findViewById(R.id.place_address)).setText("Coordinate");
                ((TextView) dialogView.findViewById(R.id.checkpoint_lat_tv)).setText("Latitude : " +
                        String.valueOf(target.latitude));
                ((TextView) dialogView.findViewById(R.id.checkpoint_long_tv)).setText("Longitude : " +
                        String.valueOf(target.longitude));

                end_latitude = target.latitude;
                end_longitude = target.longitude;

                final EditText nameEditText = dialogView.findViewById(R.id.checkpoint_name_tv);
                final AlertDialog alertDialog = builder.setView(dialogView).show();
                alertDialog.show();

                Button done = alertDialog.findViewById(R.id.dialogbox_done_btn);
                assert done != null;
                done.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        Object dataTransfer[];
                        String enteredText = nameEditText.getText().toString();

                        if (enteredText.length() >= 3) {

                            MarkerOptions markerOptions = new MarkerOptions();
                            markerOptions.position(target);
                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.flag));
                            markerOptions.title(target.latitude + " : " + target.longitude);
                            markerOptions.draggable(true);
                            mMap.clear();
                            mMap.addMarker(markerOptions);

                            if (!checkPermissions()) {
                                requestPermissions();
                            } else {
                                mService.requestLocationUpdates();
                            }

                            mMap.animateCamera(CameraUpdateFactory.newLatLng(target));
                            mMap.setMaxZoomPreference(mMap.getMaxZoomLevel());

                            dataTransfer = new Object[3];
                            String url = getDirectionsUrl();
                            final GetDirectionsData getDirectionsData = new GetDirectionsData();
                            dataTransfer[0] = mMap;
                            dataTransfer[1] = url;
                            dataTransfer[2] = new LatLng(end_latitude, end_longitude);
                            getDirectionsData.execute(dataTransfer);

                            alertDialog.dismiss();
                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    final Snackbar snackbar = Snackbar.make(
                                            findViewById(android.R.id.content), "Distance: " + getDirectionsData.getDistance()
                                                    + "\nDuration: " + getDirectionsData.getDuration(),
                                            Snackbar.LENGTH_INDEFINITE);
                                    snackbar.setAction("Dismiss", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            snackbar.dismiss();
                                        }
                                    });
                                    snackbar.show();

                                }
                            }, 3000);


                        } else {

                            nameEditText.setError("Name should have minimum of 4 characters.");

                        }
                    }
                });

                Button cancel = alertDialog.findViewById(R.id.dialogbox_cancel_btn);
                assert cancel != null;
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        alertDialog.dismiss();
                    }
                });
            } else {
                Snackbar.make(
                        findViewById(android.R.id.content),
                        "Enter mileage and fuel!",
                        Snackbar.LENGTH_SHORT)
                        .show();

                DrawerLayout drawer = findViewById(R.id.drawer_layout);
                drawer.openDrawer(Gravity.START);
            }
        }
    }

    private String getDirectionsUrl() {

        return "https://maps.googleapis.com/maps/api/directions/json?" + "origin=" + latitude + "," + longitude +
                "&destination=" + end_latitude + "," + end_longitude +
                "&key=" + "AIzaSyCi-cOwNdjflGxFXwbLIO3vhgp6kZn1KQ8";
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        DrawerLayout drawer;

        switch (id) {
            case R.id.logout:

                mAuth.signOut();

                sendToLoginPage();
                drawer = findViewById(R.id.drawer_layout);
                drawer.closeDrawers();

                return false;

            case R.id.save:
                if (!TextUtils.isEmpty(mileage.getText().toString()) && !TextUtils.isEmpty(fuel.getText().toString())) {
                    milOfVehicle = mileage.getText().toString();
                    fuelAmt = fuel.getText().toString();

                    possibleDistance = Double.valueOf(milOfVehicle) * Double.valueOf(fuelAmt);
                    Log.v("Dist", String.valueOf(possibleDistance));
                    Snackbar.make(
                            findViewById(android.R.id.content),
                            "Possible Distance: " + possibleDistance + " KM\nSettings saved !",
                            Snackbar.LENGTH_SHORT)
                            .show();

                    drawer = findViewById(R.id.drawer_layout);
                    drawer.closeDrawers();

                } else {
                    Snackbar.make(
                            findViewById(android.R.id.content),
                            "Enter mileage and fuel!",
                            Snackbar.LENGTH_SHORT)
                            .show();
                }
                return false;
            case R.id.acc_setting:
                Intent settingIntent = new Intent(MainActivity.this, SetupActivity.class);
                startActivity(settingIntent);
                drawer = findViewById(R.id.drawer_layout);
                drawer.closeDrawers();
                return false;

            default:
                return false;
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }


    /**
     * +     * Receiver for broadcasts sent by LocationService.
     * +
     */
    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location location = intent.getParcelableExtra(LocationService.EXTRA_LOCATION);
            if (location != null) {
                Toast.makeText(MainActivity.this, Utils.getLocationText(location),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

}
