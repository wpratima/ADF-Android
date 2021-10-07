package com.example.accessibilityservicecheck;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private Button allowPermission;
    Timer foregroundTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        allowPermission = findViewById(R.id.button);
        allowPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });

        foregroundTimer = new Timer();
        //SupportedBrowserConfig finalBrowserConfig = browserConfig;
//        foregroundTimer.scheduleAtFixedRate(
//                new TimerTask() {
//                    @Override
//                    public void run() {
//                        Log.e("MainActivity", "Foreground App: " + MyService.foregroundAppName);
//                    }
//                }, 3000,
//                5000);
    }
}