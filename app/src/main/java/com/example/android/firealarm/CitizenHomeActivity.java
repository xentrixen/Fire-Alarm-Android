package com.example.android.firealarm;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.android.firealarm.utilities.RequestSingleton;
import com.example.android.firealarm.utilities.Utils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;

public class CitizenHomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    private static final int REQUEST_CODE_GET_IMAGE = 0;
    private static final int REQUEST_CODE_ACCESS_LOCATION = 1;
    private static final int DEFAULT_ZOOM = 9;
    private DrawerLayout mNavigationDrawer;
    private SharedPreferences mSharedPreferences;
    private Context mContext = CitizenHomeActivity.this;
    private ProgressDialog mProgressDialog;
    private GoogleMap mMap;
    private Location mCurrentLocation;
    private boolean mRefreshDialogActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        rejectUnauthenticatedUsers();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_citizen_home);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mNavigationDrawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mNavigationDrawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        mNavigationDrawer.addDrawerListener(toggle);
        toggle.syncState();
        getMenuInflater().inflate(R.menu.menu_citizen, navigationView.getMenu());
        navigationView.setNavigationItemSelectedListener(this);

        getLocationPermission();
    }

    private void rejectUnauthenticatedUsers() {
        String accessToken = mSharedPreferences.getString("access_token", "");
        String type = mSharedPreferences.getString("type", "");
        if (accessToken.equals("") || !type.equals("citizen")) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void showRefreshDialog() {
        if(!mRefreshDialogActive) {
            mRefreshDialogActive = true;
            Dialog refreshDialog = new AlertDialog.Builder(mContext)
                    .setMessage("Refresh application?")
                    .setCancelable(true)
                    .setPositiveButton("Refresh", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                            onMapReady(mMap);
                        }
                    })
                    .create();
            refreshDialog.show();
            refreshDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    mRefreshDialogActive = false;
                }
            });
        }
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(mContext.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            initMap();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_ACCESS_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ACCESS_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initMap();
                } else {
                    Utils.showToast(mContext, "Please accept the necessary location permissions");
                    finish();
                }
        }
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;
        if (Utils.isNetworkAvailable(mContext)) {
            markMapCurrentLocation();
            markFireStationLocations();
        } else {
            Utils.showToast(mContext, "No internet connection");
            showRefreshDialog();
        }
    }

    private void markFireStationLocations() {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + mSharedPreferences.getString("access_token", ""));

        RequestSingleton.addJSONArrayRequest(mContext, Request.Method.GET, Utils.FIRE_STATIONS_URL, null, headers,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        markFireStationLocationsSuccessCallback(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        markFireStationLocationsErrorCallback(error);
                    }
                });
    }

    private void markFireStationLocationsSuccessCallback(JSONArray response) {
        for (int i = 0, length = response.length(); i < length; i++) {
            try {
                JSONObject currentObject = response.getJSONObject(i);
                LatLng position = new LatLng(currentObject.getDouble("latitude"), currentObject.getDouble("longitude"));

                mMap.addMarker(new MarkerOptions().position(position).title("Fire Station").snippet(currentObject.getString("name")));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void markFireStationLocationsErrorCallback(VolleyError error) {
        Utils.showError(mContext, "Could not get the location of fire stations: ", error, Toast.LENGTH_LONG, false);
        showRefreshDialog();
    }

    private void markMapCurrentLocation() {
        FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(mContext);

        try {
            Task<Location> getCurrentLocation = fusedLocationProviderClient.getLastLocation();
            getCurrentLocation.addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful()) {
                        mCurrentLocation = task.getResult();
                        if (mCurrentLocation != null) {
                            moveMapCamera(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
                            mMap.setMyLocationEnabled(true);
                        } else {
                            Utils.showToast(mContext, "Unable to get your current location. Make sure to enable your location service", Toast.LENGTH_LONG, false);
                            showRefreshDialog();
                        }
                    } else {
                        Utils.showToast(mContext, "Unable to get your current location", Toast.LENGTH_LONG, false);
                        showRefreshDialog();
                    }
                }
            });
        } catch (SecurityException e) {
            Utils.showToast(mContext, "Location permissions are not granted", Toast.LENGTH_SHORT, false);
            showRefreshDialog();
            e.printStackTrace();
        }
    }

    private void moveMapCamera(LatLng latLng) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
    }

    @Override
    public void onBackPressed() {
        if (mNavigationDrawer.isDrawerOpen(GravityCompat.START)) {
            mNavigationDrawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int id = menuItem.getItemId();

        switch (id) {
            case R.id.navigation_report:
                requestImage();
                break;
            case R.id.navigation_logout:
                logout();
                break;
        }

        mNavigationDrawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void requestImage() {
        if(mCurrentLocation != null) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                Utils.showToast(mContext, "Device has no camera");
            } else if (intent.resolveActivity(getPackageManager()) == null) {
                Utils.showToast(mContext, "You have no application that can take pictures");
            } else {
                startActivityForResult(intent, REQUEST_CODE_GET_IMAGE);
            }
        } else {
            Utils.showToast(mContext, "Unable to get your current location. Make sure to enable your location service", Toast.LENGTH_LONG, false);
            showRefreshDialog();
        }
    }

    private void logout() {
        if (Utils.isNetworkAvailable(mContext)) {
            mProgressDialog = Utils.makeProgressDialog(mContext, "Logging out", "Please wait...");
            mProgressDialog.show();

            HashMap<String, String> headers = new HashMap<String, String>();
            headers.put("Authorization", "Bearer " + mSharedPreferences.getString("access_token", ""));

            RequestSingleton.addJSONObjectRequest(mContext, Request.Method.POST, Utils.LOGOUT_URL, null, headers,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            logoutSuccessCallback(response);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            logoutErrorCallback();
                        }
                    });

        } else {
            Utils.showToast(mContext, "No internet connection");
        }
    }

    private void logoutSuccessCallback(JSONObject response) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.remove("access_token");
        editor.remove("type");
        editor.commit();

        RequestSingleton.getInstance(mContext).cancelAllRequest();
        mProgressDialog.dismiss();
        try {
            Utils.showToast(mContext, response.getString("message"));
        } catch (JSONException e) {
            Utils.showToast(mContext, "Logout Successful");
            e.printStackTrace();
        }
        startActivity(new Intent(mContext, MainActivity.class));
        finish();
    }

    private void logoutErrorCallback() {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.remove("access_token");
        editor.commit();
        RequestSingleton.getInstance(mContext).cancelAllRequest();
        mProgressDialog.dismiss();
        Utils.showToast(mContext, "Logout Successful");
        startActivity(new Intent(mContext, CitizenLoginActivity.class));
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_GET_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                showSendReportDialog(data);
            }
        }
    }

    private void showSendReportDialog(final Intent data) {
        new AlertDialog.Builder(mContext)
                .setMessage("Continue Sending Report?")
                .setCancelable(true)
                .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        sendReport(data);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }

    private void sendReport(Intent data) {
        if (!Utils.isNetworkAvailable(mContext)) {
            Utils.showToast(mContext, "No internet connection");
        } else {
            mProgressDialog = Utils.makeProgressDialog(mContext, "Sending Report", "Please wait...");
            mProgressDialog.show();

            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            String image = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);

            JSONObject sendData = new JSONObject();
            try {
                sendData.put("latitude", mCurrentLocation.getLatitude());
                sendData.put("longitude", mCurrentLocation.getLongitude());
                sendData.put("image", image);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            HashMap<String, String> headers = new HashMap<String, String>();
            headers.put("Authorization", "Bearer " + mSharedPreferences.getString("access_token", ""));

            RequestSingleton.addJSONObjectRequest(mContext, Request.Method.POST, Utils.FIRE_REPORTS_URL, sendData, headers,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            sendReportSuccessCallback(response);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            sendReportErrorCallback(error);
                        }
                    });
        }
    }

    private void sendReportSuccessCallback(JSONObject response) {
        mProgressDialog.dismiss();
        try {
            Utils.showToast(mContext, response.getString("message"), Toast.LENGTH_LONG);
        } catch (JSONException e) {
            Utils.showToast(mContext, "An Error Has Occurred");
        }
    }

    private void sendReportErrorCallback(VolleyError error) {
        mProgressDialog.dismiss();
        Utils.showError(mContext, error);
    }
}


