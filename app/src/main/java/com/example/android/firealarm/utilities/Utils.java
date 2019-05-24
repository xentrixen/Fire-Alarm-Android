package com.example.android.firealarm.utilities;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

public class Utils {
    public static final String API_URL = "https://fire-alarm-api.herokuapp.com/";
    public static final String LOGIN_URL = API_URL + "auth/login";
    public static final String LOGOUT_URL = API_URL + "auth/logout";
    public static final String REGISTER_URL = API_URL + "auth/signup";
    public static final String FIRE_STATIONS_URL = API_URL + "fire-stations";
    public static final String GET_CURRENT_USER_URL = API_URL + "auth/user";
    public static final String FIRE_REPORTS_URL = API_URL + "fire-reports";
    public static final String FIRE_HYDRANTS_URL = API_URL + "fire-hydrants";
    public static final String BROADCAST_AUTHORIZER_URL = API_URL + "broadcasting/auth";
    private static Toast mToast;

    public static ProgressDialog makeProgressDialog(Context context, String title, String message, boolean cancelable) {
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setTitle(title);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(cancelable);
        return progressDialog;
    }

    public static ProgressDialog makeProgressDialog(Context context, String title, String message) {
        return makeProgressDialog(context, title, message, false);
    }

    public static void showToast(Context context, String message, int length, boolean override) {
        if (override && mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(context.getApplicationContext(), message, length);
        mToast.show();
    }

    public static void showToast(Context context, String message, int length) {
        showToast(context, message, length, true);
    }

    public static void showToast(Context context, String message) {
        showToast(context, message, Toast.LENGTH_SHORT);
    }

    public static void showError(Context context, String prepend, VolleyError error, int length, boolean override) {
        NetworkResponse networkResponse = error.networkResponse;
        if (networkResponse != null && networkResponse.data != null) {
            JSONObject jsonError = null;
            try {
                jsonError = new JSONObject(new String(networkResponse.data));
                showToast(context, prepend + jsonError.getString("message"), Toast.LENGTH_LONG, false);
            } catch (JSONException e) {
                showToast(context, prepend + "An Error Has Occurred", length, false);
                e.printStackTrace();
            }
        } else if (error instanceof NoConnectionError) {
            showToast(context, prepend + "No Connection Error", length, false);
        } else if (error instanceof TimeoutError) {
            showToast(context, prepend + "Timeout Error", length, false);
        } else if (error instanceof AuthFailureError) {
            showToast(context, prepend + "Auth Failure Error", length, false);
        } else if (error instanceof ServerError) {
            showToast(context, prepend + "Server Error", length, false);
        } else if (error instanceof NetworkError) {
            showToast(context, prepend + "Network Error", length, false);
        } else if (error instanceof ParseError) {
            showToast(context, prepend + "Parse Error", length, false);
        } else {
            showToast(context, prepend + "An Error Has Occurred", length);
        }
    }

    public static void showError(Context context, VolleyError error) {
        showError(context, "", error, Toast.LENGTH_SHORT, true);
    }

    public static void showError(Context context, VolleyError error, OnResponseHasErrors callback) {
        NetworkResponse networkResponse = error.networkResponse;
        if (networkResponse != null && networkResponse.data != null) {
            JSONObject jsonError = null;
            try {
                jsonError = new JSONObject(new String(networkResponse.data));
                JSONObject errors = jsonError.optJSONObject("errors");
                if (errors != null) {
                    callback.onResponseHasErrorsCallback(errors);
                } else {
                    showToast(context, jsonError.getString("message"));
                }
            } catch (JSONException e) {
                showToast(context, "An Error Has Occurred");
                e.printStackTrace();
            }
        } else if (error instanceof NoConnectionError) {
            showToast(context, "No Connection Error");
        } else if (error instanceof TimeoutError) {
            showToast(context, "Timeout Error");
        } else if (error instanceof AuthFailureError) {
            showToast(context, "Auth Failure Error");
        } else if (error instanceof ServerError) {
            showToast(context, "Server Error");
        } else if (error instanceof NetworkError) {
            showToast(context, "Network Error");
        } else if (error instanceof ParseError) {
            showToast(context, "Parse Error");
        } else {
            showToast(context, "An Error Has Occurred");
        }
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = null;
        if (connectivityManager != null) {
            activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        }
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public interface OnResponseHasErrors {
        void onResponseHasErrorsCallback(JSONObject errors);
    }
}
