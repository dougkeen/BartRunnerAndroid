package com.dougkeen.bart.activities;

import android.support.v7.app.AppCompatActivity;

import com.dougkeen.bart.BartRunnerApplication;

import java.util.concurrent.TimeUnit;

public abstract class AbstractViewActivity extends AppCompatActivity {

    private static final int MAXIMUM_IDLE_HOURS = 3;

    @Override
    protected void onStart() {
        super.onStart();
        BartRunnerApplication application = (BartRunnerApplication) getApplication();
        long lastActivity = application.getActivityTimestamp();
        long currentTime = System.currentTimeMillis();
        long timeDifference = currentTime - lastActivity;
        if (TimeUnit.MILLISECONDS.toHours(timeDifference) >= MAXIMUM_IDLE_HOURS) {
            finish();
        }
    }
}
