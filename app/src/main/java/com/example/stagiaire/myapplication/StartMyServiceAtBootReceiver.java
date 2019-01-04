package com.example.stagiaire.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.eclipse.paho.android.service.MqttService;

public class StartMyServiceAtBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i=new Intent(context, ForegroundLocationService.class);
        Intent i2=new Intent(context, MqttService.class);

        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.O) {
            context.startForegroundService(i);
            context.startForegroundService(i2);
        }
        else {
            context.startService(i);
            context.startService(i2);
        }
    }

}

