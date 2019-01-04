package com.example.stagiaire.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.JobIntentService;
import android.util.Log;

import org.eclipse.paho.android.service.MqttService;

public class MyJobSchedular extends JobIntentService {

    static final int JOB_ID = 1000;
    static final String TAG =MyJobSchedular.class.getSimpleName();

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, MyJobSchedular.class, JOB_ID, work);
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onHandleWork(Intent intent) {
        Log.i(TAG, "Started onHandleWork");
        startForegroundService(new Intent(getApplicationContext(), ForegroundLocationService.class));
        startForegroundService(new Intent(getApplicationContext(), MqttService.class));
    }
}
