package com.example.android.firealarm.utilities;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RequestSingleton {
    private static RequestSingleton mInstance;
    private static Context mContext;
    private RequestQueue mRequestQueue;

    private RequestSingleton(Context context) {
        mContext = context;
        mRequestQueue = getRequestQueue();
    }

    public static synchronized RequestSingleton getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new RequestSingleton(context);
        }
        return mInstance;
    }


    public static void addJSONObjectRequest(final Context context, int method, String url, JSONObject data, final HashMap<String, String> headers, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        JsonObjectRequest request = new JsonObjectRequest(method, url, data,
                listener,
                errorListener) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Accept", "application/json");
                if(headers != null) {
                    params.putAll(headers);
                }
                return params;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        getInstance(context).addToRequestQueue(request);
    }

    public static void addJSONObjectRequest(final Context context, int method, String url, JSONObject data, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        addJSONObjectRequest(context, method, url, data, null, listener, errorListener);
    }

    public static void addJSONArrayRequest(final Context context, int method, String url, JSONArray data, final HashMap<String, String> headers, Response.Listener<JSONArray> listener, Response.ErrorListener errorListener) {
        JsonArrayRequest request = new JsonArrayRequest(method, url, data,
                listener,
                errorListener) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Accept", "application/json");
                if(headers != null) {
                    params.putAll(headers);
                }
                return params;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        getInstance(context).addToRequestQueue(request);
    }

    private RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(mContext.getApplicationContext());
        }
        return mRequestQueue;
    }

    private <T> void addToRequestQueue(Request<T> request) {
        getRequestQueue().add(request);
    }

    public void cancelAllRequest() {
        getRequestQueue().cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
    }
}
