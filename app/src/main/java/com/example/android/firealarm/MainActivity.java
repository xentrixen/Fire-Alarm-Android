package com.example.android.firealarm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        redirectIfAuthenticated();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button citizenLoginTextView = findViewById(R.id.citizen_login);
        Button personnelLoginTextView = findViewById(R.id.personnel_login);

        citizenLoginTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCitizenLoginActivity();
            }
        });

        personnelLoginTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFirePersonnelLoginActivity();
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

    private void openCitizenLoginActivity() {
        startActivity(new Intent(this, CitizenLoginActivity.class));
        finish();
    }

    private void openFirePersonnelLoginActivity() {
        startActivity(new Intent(this, FirePersonnelLoginActivity.class));
        finish();
    }
}
