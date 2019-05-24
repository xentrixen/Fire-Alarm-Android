package com.example.android.firealarm;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.android.firealarm.utilities.RequestSingleton;
import com.example.android.firealarm.utilities.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity implements Utils.OnResponseHasErrors {

    private EditText mNameEditText, mEmailEditText, mPasswordEditText, mConfirmPasswordEditText;
    private Button mRegisterButton;
    private Context mContext = RegisterActivity.this;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        redirectIfAuthenticated();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Button registerButton = findViewById(R.id.register);
        Button cancelButton = findViewById(R.id.cancel);
        mNameEditText = findViewById(R.id.name);
        mEmailEditText = findViewById(R.id.username);
        mPasswordEditText = findViewById(R.id.password);
        mConfirmPasswordEditText = findViewById(R.id.confirm_password);
        mProgressDialog = Utils.makeProgressDialog(mContext, "Processing Registration", "Please wait...");

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register();
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void redirectIfAuthenticated() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if(!sharedPreferences.getString("access_token", "").equals("")) {
            String type = sharedPreferences.getString("type", "");

            if(type.equals("citizen")) {
                startActivity(new Intent(this, CitizenHomeActivity.class));
            } else if(type.equals("fire-personnel")) {
                startActivity(new Intent(this, FirePersonnelHomeActivity.class));
            }
            finish();
        }
    }

    private void register() {
        if (TextUtils.isEmpty(mNameEditText.getText()) || TextUtils.isEmpty(mEmailEditText.getText()) || TextUtils.isEmpty(mPasswordEditText.getText()) ||
                TextUtils.isEmpty(mConfirmPasswordEditText.getText())) {
            Utils.showToast(mContext, "All fields are required");
        } else if (!Utils.isNetworkAvailable(mContext)) {
            Utils.showToast(mContext, "No internet connection");
        } else {
            mProgressDialog.show();
            JSONObject data = new JSONObject();
            try {
                data.put("name", mNameEditText.getText());
                data.put("email", mEmailEditText.getText());
                data.put("password", mPasswordEditText.getText());
                data.put("password_confirmation", mConfirmPasswordEditText.getText());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            RequestSingleton.addJSONObjectRequest(mContext, Request.Method.POST, Utils.REGISTER_URL, data,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            registerSuccessCallback(response);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            registerErrorCallback(error);
                        }
                    });
        }
    }

    private void registerSuccessCallback(JSONObject response) {
        mProgressDialog.dismiss();
        try {
            Utils.showToast(mContext, response.getString("message"), Toast.LENGTH_LONG);
            finish();
        } catch (JSONException e) {
            Utils.showToast(mContext, "An Error Has Occurred");
            e.printStackTrace();
        }
    }

    private void registerErrorCallback(VolleyError error) {
        mProgressDialog.dismiss();
        Utils.showError(mContext, error, this);
    }

    @Override
    public void onResponseHasErrorsCallback(JSONObject errors) {
        mPasswordEditText.setText("");
        mConfirmPasswordEditText.setText("");

        final EditText[] editTexts = {mNameEditText, mEmailEditText, mPasswordEditText, mConfirmPasswordEditText};
        String[] keys = {"name", "email", "password", "password_confirmation"};

        int firstErrorIndex = -1;
        for(int i = 0, length = keys.length; i < length; i++) {
            JSONArray errorsArray = errors.optJSONArray(keys[i]);
            if(errorsArray != null) {
                if(firstErrorIndex == -1) {
                    firstErrorIndex = i;
                }

                final EditText currentEditText = editTexts[i];
                currentEditText.setError(errorsArray.optString(0));
                currentEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        currentEditText.setError(null);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        currentEditText.setError(null);
                    }
                });
            }
        }

        editTexts[firstErrorIndex].requestFocus();
    }
}
