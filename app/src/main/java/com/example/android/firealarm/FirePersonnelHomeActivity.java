package com.example.android.firealarm;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.android.firealarm.utilities.NotificationUtils;
import com.example.android.firealarm.utilities.RequestSingleton;
import com.example.android.firealarm.utilities.Utils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.Channel;
import com.pusher.client.channel.PrivateChannel;
import com.pusher.client.channel.PrivateChannelEventListener;
import com.pusher.client.channel.SubscriptionEventListener;
import com.pusher.client.util.HttpAuthorizer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FirePersonnelHomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback, SubscriptionEventListener {

    private static final int DEFAULT_ZOOM = 8;
    private DrawerLayout mNavigationDrawer;
    private SharedPreferences mSharedPreferences;
    private Context mContext = FirePersonnelHomeActivity.this;
    private ProgressDialog mProgressDialog;
    private GoogleMap mMap;
    private boolean mRefreshDialogActive = false;
    private List<Marker> mFireReportMarkers = new ArrayList<>();
    private Pusher mPusher;

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
        getMenuInflater().inflate(R.menu.menu_fire_personnel, navigationView.getMenu());
        navigationView.setNavigationItemSelectedListener(this);

        initMap();
    }

    private void rejectUnauthenticatedUsers() {
        String accessToken = mSharedPreferences.getString("access_token", "");
        String type = mSharedPreferences.getString("type", "");
        if (accessToken.equals("") || !type.equals("fire-personnel")) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void showRefreshDialog() {
        if (!mRefreshDialogActive) {
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

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (Utils.isNetworkAvailable(mContext)) {
            markFireStationLocations();
            markFireReportLocations();
            markFireHydrantLocations();
            listenForFireReportChanges();
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
                    public void onResponse(JSONArray fireStations) {
                        markFireStationLocationsSuccessCallback(fireStations);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        markFireStationLocationsErrorCallback(error);
                    }
                });
    }

    private void markFireStationLocationsSuccessCallback(final JSONArray fireStations) {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + mSharedPreferences.getString("access_token", ""));

        RequestSingleton.addJSONObjectRequest(mContext, Request.Method.GET, Utils.GET_CURRENT_USER_URL, null, headers,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject currentFireStation) {
                        markFireStationLocationsSuccessCallback(fireStations, currentFireStation);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        markFireStationLocationsErrorCallback(error);
                    }
                });
    }

    private void markFireStationLocationsErrorCallback(VolleyError error) {
        Utils.showError(mContext, "Could not get the location of fire stations: ", error, Toast.LENGTH_LONG, false);
        showRefreshDialog();
    }

    private void markFireStationLocationsSuccessCallback(JSONArray fireStations, JSONObject currentFireStation) {
        for (int i = 0, length = fireStations.length(); i < length; i++) {
            try {
                JSONObject currentObject = fireStations.getJSONObject(i);
                LatLng position = new LatLng(currentObject.getDouble("latitude"), currentObject.getDouble("longitude"));
                int currentFireStationId = currentObject.getInt("id");
                int thisFireStationId = currentFireStation.getInt("id");

                if (currentFireStationId != thisFireStationId) {
                    mMap.addMarker(new MarkerOptions()
                            .position(position)
                            .title("Fire Station")
                            .snippet(currentObject.getString("name")));
                } else {
                    mMap.addMarker(new MarkerOptions()
                            .position(position)
                            .title("Fire Station")
                            .snippet(currentObject.getString("name"))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                    moveMapCamera(position);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void markFireReportLocations() {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + mSharedPreferences.getString("access_token", ""));

        RequestSingleton.addJSONArrayRequest(mContext, Request.Method.GET, Utils.FIRE_REPORTS_URL, null, headers,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        markFireReportLocationsSuccessCallback(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        markFireReportLocationsErrorCallback(error);
                    }
                });
    }

    private void markFireReportLocationsSuccessCallback(JSONArray response) {
        for (int i = 0, length = response.length(); i < length; i++) {
            try {
                JSONObject currentObject = response.getJSONObject(i);
                LatLng position = new LatLng(currentObject.getDouble("latitude"), currentObject.getDouble("longitude"));

                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(position)
                        .title("Fire Report")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                marker.setTag(currentObject);

                mFireReportMarkers.add(marker);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                JSONObject data = (JSONObject) marker.getTag();
                if (data != null) {
                    Intent intent = new Intent(mContext, FireReportDetailActivity.class);
                    intent.putExtra("data", data.toString());
                    startActivity(intent);
                }
                return false;
            }
        });
    }

    private void markFireReportLocationsErrorCallback(VolleyError error) {
        Utils.showError(mContext, "Could not get the location of fire reports: ", error, Toast.LENGTH_LONG, false);
        showRefreshDialog();
    }

    private void markFireHydrantLocations() {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + mSharedPreferences.getString("access_token", ""));

        RequestSingleton.addJSONArrayRequest(mContext, Request.Method.GET, Utils.FIRE_HYDRANTS_URL, null, headers,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        markFireHydrantLocationsSuccessCallback(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        markFireHydrantLocationsErrorCallback(error);
                    }
                });
    }

    private void markFireHydrantLocationsSuccessCallback(JSONArray response) {
        for (int i = 0, length = response.length(); i < length; i++) {
            try {
                JSONObject currentObject = response.getJSONObject(i);
                LatLng position = new LatLng(currentObject.getDouble("latitude"), currentObject.getDouble("longitude"));

                mMap.addMarker(new MarkerOptions()
                        .position(position)
                        .title("Fire Hydrant")
                        .snippet(currentObject.getString("name"))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void markFireHydrantLocationsErrorCallback(VolleyError error) {
        Utils.showError(mContext, "Could not get the location of fire hydrants: ", error, Toast.LENGTH_LONG, false);
        showRefreshDialog();
    }

    private void moveMapCamera(LatLng latLng) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
    }

    private void listenForFireReportChanges() {
        PusherOptions options = new PusherOptions().setCluster("ap1");
        mPusher = new Pusher("b98c896342b90b17345b", options);
        mPusher.connect();

        Channel channel = mPusher.subscribe("fire-reports");

        channel.bind("fire-report-event", this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(mContext, FireReportNotificationService.class));
        } else {
            startService(new Intent(mContext, FireReportNotificationService.class));
        }
    }

    @Override
    public void onEvent(String s, String s1, String s2) {
        try {
            JSONObject data = new JSONObject(s2);
            final String event = data.getString("eventType");
            final JSONObject fireReport = data.getJSONObject("fireReport");

            this.runOnUiThread(new Runnable(){
                public void run(){
                    switch (event) {
                        case "created":
                            try {
                                LatLng position = new LatLng(fireReport.getDouble("latitude"), fireReport.getDouble("longitude"));

                                Marker marker = mMap.addMarker(new MarkerOptions()
                                        .position(position)
                                        .title("Fire Report")
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                                marker.setTag(fireReport);
                                mFireReportMarkers.add(marker);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            break;
                        case "deleted":
                            for (Marker fireReportMarker : mFireReportMarkers) {
                                JSONObject tag = (JSONObject) fireReportMarker.getTag();
                                if (tag.optInt("id") == fireReport.optInt("id")) {
                                    fireReportMarker.remove();
                                    mFireReportMarkers.remove(fireReportMarker);
                                    break;
                                }
                            }
                            break;
                        case "updated":
                            for (Marker fireReportMarker : mFireReportMarkers) {
                                JSONObject tag = (JSONObject) fireReportMarker.getTag();
                                if (tag.optInt("id") == fireReport.optInt("id")) {
                                    fireReportMarker.setTag(fireReport);
                                    break;
                                }
                            }
                            break;
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
            case R.id.navigation_logout:
                logout();
                break;
        }

        mNavigationDrawer.closeDrawer(GravityCompat.START);
        return true;
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
        stopService(new Intent(mContext, FireReportNotificationService.class));

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
        stopService(new Intent(mContext, FireReportNotificationService.class));

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
    protected void onDestroy() {
        super.onDestroy();
        mPusher.disconnect();
    }
}
