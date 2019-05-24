package com.example.android.firealarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

public class FireReportNotificationServiceAutoStart extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        String accessToken = sharedPreferences.getString("access_token", "");
        String type = sharedPreferences.getString("type", "");

        if (!accessToken.equals("") && type.equals("fire-personnel")) {
            Intent startServiceIntent = new Intent(context, FireReportNotificationService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startServiceIntent);
            } else {
                context.startService(startServiceIntent);
            }
        }
    }
}
