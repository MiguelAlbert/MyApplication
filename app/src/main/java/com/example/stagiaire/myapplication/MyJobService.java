package com.example.stagiaire.myapplication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MyJobService extends JobService {
    Context ctx;
    NotificationManager notificationManager;
    final int[] notificationId = {1};
    private static final String CHANNEL_ID = "le_channel_id";

    @Override
    public boolean onStartJob(final JobParameters params) {
        ctx = getBaseContext();
        //le contexte pour afficher le toast
        // ici mettre votre travail a faire en tache répétitive en exemple ici un toast
        try {
        // moi je rajoute souvent un TRY en cas de problème sur une opération risqué
            Log.i("JOBB","OK");
        //simple log dans le logcat
            Toast.makeText(ctx, "test ok" ,
                    Toast.LENGTH_LONG).show();
            setMessageNotification();
        // on affiche "test ok "
        } catch (Exception x) {
        // rien en cas d\'echec dans la condition TRY
        }
        return false;
    }

    private void setMessageNotification() {
        notificationId[0]++;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID+notificationId[0], "Strada Notifications", NotificationManager.IMPORTANCE_HIGH);
            // Configure the notification channel.
            notificationChannel.setDescription("Channel description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0, 700, 500, 700});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_name_message)
                .setContentTitle("topic : ")
                .setContentText("text : ")
                .setStyle(new NotificationCompat.BigTextStyle().bigText("" + "\n" + "De : "))
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 1000, 500, 1000})
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                ;
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(ctx);
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(notificationId[0], mBuilder.build());
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        //  ici a faire en cas d\'echec du JOB Service
        return false;
    }
}