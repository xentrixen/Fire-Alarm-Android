package com.example.android.firealarm;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.android.firealarm.utilities.RequestSingleton;
import com.example.android.firealarm.utilities.Utils;

import org.json.JSONException;
import org.json.JSONObject;

public class FirePersonnelLoginActivity extends AppCompatActivity {

    private EditText mUsernameEditText, mPasswordEditText;
    private ProgressDialog mProgressDialog;
    private Context mContext = FirePersonnelLoginActivity.this;
    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        redirectIfAuthenticated();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fire_personnel_login);

        Button loginButton = findViewById(R.id.login);
        mUsernameEditText = findViewById(R.id.username);
        mPasswordEditText = findViewById(R.id.password);
        mProgressDialog = Utils.makeProgressDialog(mContext, "Logging In", "Please wait...");

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
    }

    private void redirectIfAuthenticated() {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        if(!mSharedPreferences.getString("access_token", "").equals("")) {
            String type = mSharedPreferences.getString("type", "");

            if(type.equals("citizen")) {
                startActivity(new Intent(mContext, CitizenHomeActivity.class));
            } else if(type.equals("fire-personnel")) {
                startActivity(new Intent(mContext, FirePersonnelHomeActivity.class));
            }
            finish();
        }
    }

    private void login() {
        if (TextUtils.isEmpty(mUsernameEditText.getText()) || TextUtils.isEmpty(mPasswordEditText.getText())) {
            Utils.showToast(mContext, "All fields are required");
        } else if (!Utils.isNetworkAvailable(mContext)) {
            Utils.showToast(mContext, "No internet connection");
        } else {
            mProgressDialog.show();
            JSONObject data = new JSONObject();
            try {
                data.put("username", mUsernameEditText.getText());
                data.put("password", mPasswordEditText.getText());
                data.put("type", "fire-personnel");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            RequestSingleton.addJSONObjectRequest(mContext, Request.Method.POST, Utils.LOGIN_URL, data,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            loginSuccessCallback(response);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            loginErrorCallback(error);
                        }
                    });
        }
    }

    private void loginSuccessCallback(JSONObject response) {
        mProgressDialog.dismiss();
        try {
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putString("access_token", response.getString("access_token"));
            editor.putString("type", "fire-personnel");
            if(editor.commit()) {
                mProgressDialog.dismiss();
                Utils.showToast(mContext, "Login Successful");
                startActivity(new Intent(mContext, MainActivity.class));
                finish();
            }
        } catch (JSONException e) {
            mProgressDialog.dismiss();
            Utils.showToast(mContext, "An Error Has Occurred");
            e.printStackTrace();
        }
    }

    private void loginErrorCallback(VolleyError error) {
        mProgressDialog.dismiss();
        mPasswordEditText.setText("");
        mPasswordEditText.requestFocus();
        Utils.showError(mContext, error);
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(mContext, MainActivity.class));
        finish();
    }
}
