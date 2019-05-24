package com.example.android.firealarm;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.android.firealarm.utilities.RequestSingleton;
import com.example.android.firealarm.utilities.Utils;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class FireReportDetailActivity extends AppCompatActivity {

    private Context mContext = FireReportDetailActivity.this;
    private SharedPreferences mSharedPreferences;
    private ProgressDialog mProgressDialog;
    private String mFireReportId;
    private TextView mLevelOfFireTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fire_report_detail);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        JSONObject data = null;
        String levelOfFire = null;
        String image = null;
        String latitude = null;
        String longitude = null;
        String reporterName = null;
        String reporterEmail = null;
        String reportedOn = null;
        try {
            data = new JSONObject(getIntent().getStringExtra("data"));
            image = data.getString("image");
            mFireReportId = data.getString("id");
            levelOfFire = data.getString("level_of_fire");
            latitude = data.getString("latitude") + "°";
            longitude = data.getString("longitude") + "°";
            reporterName = data.getString("reporter_name");
            reporterEmail = data.getString("reporter_email");
            reportedOn = data.getString("created_at");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ImageView imageView = findViewById(R.id.image);
        mLevelOfFireTextView = findViewById(R.id.level_of_fire);
        TextView latitudeTextView = findViewById(R.id.latitude);
        TextView longitudeTextView = findViewById(R.id.longitude);
        TextView reporterNameTextView = findViewById(R.id.reporter_name);
        TextView reporterEmailTextView = findViewById(R.id.reporter_email);
        TextView reportedOnTextView = findViewById(R.id.reported_on);

        Picasso.get().load(image).into(imageView);
        mLevelOfFireTextView.setText(levelOfFire);
        latitudeTextView.setText(latitude);
        longitudeTextView.setText(longitude);
        reporterNameTextView.setText(reporterName);
        reporterEmailTextView.setText(reporterEmail);
        reportedOnTextView.setText(reportedOn);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_fire_report_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        } else if (id == R.id.set_level_of_fire) {
            showSetLevelOfFireDialog();
        } else if (id == R.id.delete) {
            showDeleteFireReportDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSetLevelOfFireDialog() {
        final String[] levelOfFires = {"First Alarm", "Second Alarm", "Third Alarm", "General Alarm"};
        new AlertDialog.Builder(mContext)
                .setTitle("Set level of fire")
                .setItems(levelOfFires, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        setLevelOfFire(levelOfFires[which]);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }

    private void setLevelOfFire(final String levelOfFire) {
        mProgressDialog = Utils.makeProgressDialog(mContext, "Updating level of fire", "Please wait...");
        mProgressDialog.show();

        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + mSharedPreferences.getString("access_token", ""));

        JSONObject sendData = new JSONObject();
        try {
            sendData.put("level_of_fire", levelOfFire);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestSingleton.addJSONObjectRequest(mContext, Request.Method.PUT, Utils.FIRE_REPORTS_URL + "/" + mFireReportId, sendData, headers,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        setLevelOfFireSuccessCallback(response, levelOfFire);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        setLevelOfFireErrorCallback(error);
                    }
                });
    }

    private void setLevelOfFireSuccessCallback(JSONObject response, String levelOfFire) {
        mProgressDialog.dismiss();
        try {
            Utils.showToast(mContext, response.getString("message"), Toast.LENGTH_LONG);
            mLevelOfFireTextView.setText(levelOfFire);
        } catch (JSONException e) {
            Utils.showToast(mContext, "An Error Has Occurred");
        }
    }

    private void setLevelOfFireErrorCallback(VolleyError error) {
        mProgressDialog.dismiss();
        Utils.showError(mContext, error);
    }

    private void showDeleteFireReportDialog() {
        new AlertDialog.Builder(mContext)
                .setMessage("Confirm delete?")
                .setCancelable(true)
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        deleteFireReport();
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

    private void deleteFireReport() {
        mProgressDialog = Utils.makeProgressDialog(mContext, "Deleting fire report", "Please wait...");
        mProgressDialog.show();

        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + mSharedPreferences.getString("access_token", ""));

        RequestSingleton.addJSONObjectRequest(mContext, Request.Method.DELETE, Utils.FIRE_REPORTS_URL + "/" + mFireReportId, null, headers,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        deleteFireReportSuccessCallback(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        deleteFireReportErrorCallback(error);
                    }
                });
    }

    private void deleteFireReportSuccessCallback(JSONObject response) {
        mProgressDialog.dismiss();
        try {
            Utils.showToast(mContext, response.getString("message"), Toast.LENGTH_LONG);
            finish();
        } catch (JSONException e) {
            Utils.showToast(mContext, "An Error Has Occurred");
        }
    }

    private void deleteFireReportErrorCallback(VolleyError error) {
        mProgressDialog.dismiss();
        Utils.showError(mContext, error);
    }
}
