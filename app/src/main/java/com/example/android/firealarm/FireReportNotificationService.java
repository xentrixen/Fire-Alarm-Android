package com.example.android.firealarm;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.example.android.firealarm.utilities.NotificationUtils;
import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.Channel;
import com.pusher.client.channel.SubscriptionEventListener;

import org.json.JSONException;
import org.json.JSONObject;

public class FireReportNotificationService extends Service implements SubscriptionEventListener {

    private Pusher mPusher;

    @Override
    public void onCreate() {
        PusherOptions options = new PusherOptions().setCluster("ap1");
        mPusher = new Pusher("b98c896342b90b17345b", options);
        mPusher.connect();

        Channel channel = mPusher.subscribe("fire-reports");

        channel.bind("fire-report-event", this);
    }

    @Override
    public void onEvent(String s, String s1, final String s2) {
        try {
            JSONObject data = new JSONObject(s2);
            final String event = data.getString("eventType");

            switch (event) {
                case "created":
                    NotificationUtils.notify(this, "Fire Alert", "A fire report has been submitted");
                    break;
                case "deleted":
                    NotificationUtils.notify(this, "Fire Alert", "A fire report has been deleted");
                    break;
                case "updated":
                    NotificationUtils.notify(this, "Fire Alert", "A fire report has been updated");
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPusher.disconnect();
    }
}
