package com.example.stagiaire.myapplication;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.icu.util.Calendar;
import android.location.Criteria;

import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import android.os.Handler;
import android.os.IBinder;

import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.List;

import static java.lang.StrictMath.abs;

@RequiresApi(api = Build.VERSION_CODES.N)
public class ForegroundLocationService extends Service implements SensorEventListener {



    //https://stackoverflow.com/questions/28535703/best-way-to-get-user-gps-location-in-background-in-android
    private LocationManager mLocationManager = null;
    private static final float LOCATION_DISTANCE = 0;

    private static final String TAG = ForegroundLocationService.class.getSimpleName();

    // the notification id for the foreground notification
    public static final int GPS_NOTIFICATION = 1;

    // variable qui récupère les valeurs stockées quand la connexion est revenue
    private String STOCKAGE_PAS_DECONNEXION = "";

    //mqtt
    MqttHelper mqttHelper;
    String messageMqtt = "";

    int compteurMessage = 0;

    //sharedPreferences
    SharedPreferences sharedPreferences;
    String PREFS_TOPIC;
    String PREFS_CLIENT_ID;

    //Notifications
    NotificationManager notificationManager;

    final int[] notificationId = {1};
    private static final String CHANNEL_ID = "le_channel_id_message";
    private static final String CHANNEL_ID_LOCATION = "channel_id_localisation";

    float nCurrentBearing = 0;
    float nLastBearing = 0;
    double nCurrentSpeed = 0;
    double nLastSpeed = 0;
    double nCurrentLatitude = 0;
    double nLastLatitude = 0;
    double nCurrentLongitude = 0;
    double nLastLongitude = 0;
    float Distance = 0;
    float angle = 0;

    Location mLastLocation;
    Location lastLocation;
    Location newLocation;
    Date lastDate = null;
    Date currentDate = null;
    long mills = 0;
    static Boolean CONNECTED_GPS = true;
    static Boolean CONNECTED_MQTT = true;

    private int hitCount = 0;
    private double hitSum = 0;
    boolean move = false;

    int countSat = 0;

    Context context;

    Handler handler;

    //LTE

    TelephonyManager telephonyManager ;
    int LTEsignalStrength;

    //Accélléromètre
    private SensorManager sensorMan;
    private Sensor accelerometer;

    private double mAccel;
    private double mAccelCurrent;
    private double mAccelLast;

    boolean sensorRegistered = false;


    @Override
    public void onCreate() {

        handler = new Handler();

        context = getApplicationContext();

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        sharedPreferences = getSharedPreferences("PREFS", Context.MODE_PRIVATE);

        startMqtt();

        sensorMan = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;

        sensorMan.registerListener(this, accelerometer,SensorManager.SENSOR_DELAY_NORMAL);
        sensorRegistered = true;

        Log.e(TAG, "onCreate");

        initializeLocationManager();
        try {
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            criteria.setPowerRequirement(Criteria.POWER_HIGH);
            criteria.setAltitudeRequired(false);
            criteria.setSpeedRequired(true);
            criteria.setCostAllowed(true);
            criteria.setBearingRequired(false);
            //API level 9 and up
            criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
            criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);
            criteria.setBearingAccuracy(Criteria.ACCURACY_HIGH);
            criteria.setSpeedAccuracy(Criteria.ACCURACY_HIGH);

            mLocationManager.requestLocationUpdates(mLocationManager.getBestProvider(criteria, true), 0, LOCATION_DISTANCE, mLocationListeners[0]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }

        lastLocation = new Location(LocationManager.GPS_PROVIDER);
        lastLocation.setLatitude(nLastLatitude);
        lastLocation.setLongitude(nLastLongitude);

        android.icu.text.SimpleDateFormat sdf = new android.icu.text.SimpleDateFormat("hh:mm:ss");
        Date systemDate = Calendar.getInstance().getTime();
        String myDate = sdf.format(systemDate);
        try {
            lastDate = sdf.parse(myDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }


    public Runnable messagePasDeGPS = new Runnable() {
        @Override
        public void run() {
            sharedPreferences = getBaseContext().getSharedPreferences("PREFS", MODE_PRIVATE);
            PREFS_TOPIC = sharedPreferences.getString("PREFS_TOPIC", "");
            PREFS_CLIENT_ID = sharedPreferences.getString("PREFS_CLIENT_ID", "");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String currentDateandTime = sdf.format(new Date());
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(currentDateandTime);
            stringBuilder.append(";");
            stringBuilder.append("");
            stringBuilder.append(";");
            stringBuilder.append("");
            stringBuilder.append(";");
            stringBuilder.append("");
            stringBuilder.append(";");
            stringBuilder.append("");
            stringBuilder.append(";");
            stringBuilder.append("");
            stringBuilder.append(";");
            stringBuilder.append(countSat);
            messageMqtt = stringBuilder.toString();

            compteurMessage ++;
            EnregistreEnBase(messageMqtt);//Stockage en base

            if(NetworkUtils.isNetworkConnected(context)){ // test la connexion
                if(compteurMessage >= 15){
                    STOCKAGE_PAS_DECONNEXION = ExpedieLesLocalisations();
                    MqttMessage myMess = new MqttMessage(STOCKAGE_PAS_DECONNEXION.getBytes());
                    try {
                        mqttHelper.publish("0/" + PREFS_TOPIC + "/" + PREFS_CLIENT_ID, myMess);
                        compteurMessage =0;
                        Log.w(TAG, "Stockage expédié : " + myMess);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
            }else{
                EnregistreEnBase(messageMqtt);//Stockage en base
            }
            handler.postDelayed(messagePasDeGPS,5*60*1000); // 5 minutes
        }
    };

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float[] mGravity = sensorEvent.values.clone();
            // Shake detection
            double x = mGravity[0];
            double y = mGravity[1];
            double z = mGravity[2];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = Math.sqrt(x * x + y * y + z * z);
            double delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta;

            int SAMPLE_SIZE = 10;
            if (hitCount <= SAMPLE_SIZE) {
                hitCount++;
                hitSum += Math.abs(mAccel);
            } else {
                double hitResult = hitSum / SAMPLE_SIZE;
                //Log.d(TAG, String.valueOf(hitResult));
                double THRESHOLD = 0.08;
                if (hitResult > THRESHOLD) {
                    //Log.d(TAG, "Walking");
                    move = true;
                } else {
                    //Log.d(TAG, "Stop Walking");
                    move = false;
                }

                hitCount = 0;
                hitSum = 0;
                hitResult = 0;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }



    private class LocationListener implements android.location.LocationListener {
        public LocationListener(String provider) {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(LocationManager.GPS_PROVIDER);
        }

        @Override
        public void onLocationChanged(Location location) {

            currentDate = Calendar.getInstance().getTime();
            nCurrentLatitude = location.getLatitude();
            nCurrentLongitude = location.getLongitude();
            nCurrentBearing = location.getBearing();
            nCurrentSpeed = location.getSpeed()*3.6;
            newLocation = new Location("newlocation");
            newLocation.setLatitude(nCurrentLatitude);
            newLocation.setLongitude(nCurrentLongitude);
            lastLocation.setLatitude(nLastLatitude);
            lastLocation.setLongitude(nLastLongitude);
            Distance = lastLocation.distanceTo(newLocation) / 1000; // in km

            if (abs(nLastBearing - nCurrentBearing) > 180) {
                angle = 360 - (abs(nLastBearing - nCurrentBearing));
            } else {
                angle = (abs(nLastBearing - nCurrentBearing));
            }
            try {
                android.icu.text.SimpleDateFormat sdf1 = new android.icu.text.SimpleDateFormat("hh:mm:ss");
                Date systemDate = Calendar.getInstance().getTime();
                String myDate = sdf1.format(systemDate);
                currentDate = sdf1.parse(myDate);
                mills = currentDate.getTime() - lastDate.getTime();

            } catch (Exception e) {
                e.printStackTrace();
            }

            if ((nCurrentSpeed >= Integer.parseInt(sharedPreferences.getString("PREFS_VITESSE_START", "")))&& (move == true )) { // && (nCurrentSpeed <= 180)test accelerometre et vitesse minimale
                if (
                        (angle > Integer.parseInt(sharedPreferences.getString("PREFS_CAP_START", ""))) // angle de cap
                                ||
                        ((nCurrentSpeed < nLastSpeed) && ((nLastSpeed - nCurrentSpeed) >= Integer.parseInt(sharedPreferences.getString("PREFS_DECELERATION", ""))))  // Décélération
                                ||
                        (Distance >= Integer.parseInt(sharedPreferences.getString("PREFS_DISTANCE_START", ""))) //Distance entre 2 points
                                ||
                        (mills == (Integer.parseInt(sharedPreferences.getString("PREFS_SECONDES", "")) * 1000))
                        )
                {
                    DistribueLeMessage(location);
                    mills = 0;
                    lastDate = currentDate;
                    nLastBearing = nCurrentBearing;
                    nLastLatitude = nCurrentLatitude;
                    nLastLongitude = nCurrentLongitude;
                    nLastSpeed = nCurrentSpeed;
                }
            } else {
                if (mills == (1000 * 60 * Integer.parseInt(sharedPreferences.getString("PREFS_TEMPS_INACTIF", "")))) { // si le téléphone ne bouge pas une toute les heures
                    DistribueLeMessage(location);
                    mills = 0;
                    lastDate = currentDate;
                    nLastBearing = nCurrentBearing;
                    nLastLatitude = nCurrentLatitude;
                    nLastLongitude = nCurrentLongitude;
                    nLastSpeed = nCurrentSpeed;
                }
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e(TAG, "onProviderDisabled: " + provider);
            CONNECTED_GPS = false;
            startForeground(ForegroundLocationService.GPS_NOTIFICATION, notifyUserThatLocationServiceStoped());
            try {
                CONNECTED_GPS = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            } catch(Exception ex) {}

            messagePasDeGPS.run();
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.e(TAG, "onProviderEnabled: " + provider);
            CONNECTED_GPS = true;
            try {
                CONNECTED_GPS = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            } catch(Exception ex) {}

            if (CONNECTED_MQTT ) {
                startForeground(ForegroundLocationService.GPS_NOTIFICATION, notifyUserThatLocationServiceStarted());

            }else{
               startForeground(ForegroundLocationService.GPS_NOTIFICATION, notifyUserThatConnectionLosted());
            }
            handler.removeCallbacks(messagePasDeGPS);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            //Log.e(TAG, "onStatusChanged: " + provider);
        }
    }


    public void DistribueLeMessage(Location location) {

        sharedPreferences = getBaseContext().getSharedPreferences("PREFS", MODE_PRIVATE);
        PREFS_TOPIC = sharedPreferences.getString("PREFS_TOPIC", "");
        PREFS_CLIENT_ID = sharedPreferences.getString("PREFS_CLIENT_ID", "");

        Log.e(TAG, "onLocationChanged: " + location);
        mLastLocation.set(location);
        Double fusedspeed = (location.getSpeed()*3.6);

        countSat = location.getExtras().getInt("satellites");

        Double fusedAltitude = location.getAltitude();

        DecimalFormat df2 = new DecimalFormat("#0");
        DecimalFormatSymbols dfs2 = new DecimalFormatSymbols();
        dfs2.setDecimalSeparator('.'); // mettre point ou virgule
        df2.setDecimalFormatSymbols(dfs2);
        df2.setRoundingMode(RoundingMode.UP);

        DecimalFormat df = new DecimalFormat("#0.00");
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setDecimalSeparator('.'); // mettre point ou virgule
        df.setDecimalFormatSymbols(dfs);
        df.setRoundingMode(RoundingMode.UP);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String currentDateandTime = sdf.format(new Date());

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(currentDateandTime);
        stringBuilder.append(";");
        stringBuilder.append(location.getLatitude());
        stringBuilder.append(";");
        stringBuilder.append(location.getLongitude());
        stringBuilder.append(";");
        stringBuilder.append(df.format(fusedspeed));
        stringBuilder.append(";");
        stringBuilder.append(df2.format(fusedAltitude));
        stringBuilder.append(";");
        stringBuilder.append(LTEsignalStrength);
        stringBuilder.append(";");
        stringBuilder.append(countSat);
        messageMqtt = stringBuilder.toString();

        compteurMessage ++;
        EnregistreEnBase(messageMqtt);//Stockage en base

        if(NetworkUtils.isNetworkConnected(context)){ // test la connexion
            if(compteurMessage >= 15){
                STOCKAGE_PAS_DECONNEXION = ExpedieLesLocalisations();
                MqttMessage myMess = new MqttMessage(STOCKAGE_PAS_DECONNEXION.getBytes());
                try {
                    mqttHelper.publish("0/" + PREFS_TOPIC + "/" + PREFS_CLIENT_ID, myMess);
                    compteurMessage =0;
                    Log.w(TAG, "Stockage expédié : " + myMess);
                } catch (MqttException e) {
                    e.printStackTrace();
                }

            }
            }else{
            EnregistreEnBase(messageMqtt);//Stockage en base
            }

        }

        LocationListener[] mLocationListeners = new LocationListener[] {
            new LocationListener(LocationManager.GPS_PROVIDER)
        };


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        PhoneStateListener psl =new PhoneStateListener() {
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                super.onSignalStrengthsChanged(signalStrength);
                // Modification du niveau de signal
                LTEsignalStrength=signalStrength.getGsmBitErrorRate();
            }
        };
        telephonyManager.listen(psl, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);


        try {
            CONNECTED_GPS = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}


        if (CONNECTED_GPS  && CONNECTED_MQTT ) {
            startForeground(ForegroundLocationService.GPS_NOTIFICATION, notifyUserThatLocationServiceStarted());

        }else{if (!CONNECTED_MQTT  && CONNECTED_GPS ){
            startForeground(ForegroundLocationService.GPS_NOTIFICATION, notifyUserThatConnectionLosted());

        }else {if (CONNECTED_MQTT   &&  !CONNECTED_GPS){
            startForeground(ForegroundLocationService.GPS_NOTIFICATION, notifyUserThatLocationServiceStoped());

        }else {
            startForeground(ForegroundLocationService.GPS_NOTIFICATION, notifyUserThatConnectionLosted());
        }
        }
        }
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        sensorMan.registerListener(this,sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_NORMAL);

        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        Intent broadcastIntent = new Intent(".SensorRestarterBroadcastReceiver");
        sendBroadcast(broadcastIntent);
        mLocationManager.removeUpdates(mLocationListeners[0]);
        sensorMan.unregisterListener(this);
    }

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        }
    }

    public void startMqtt() {
        sharedPreferences = getBaseContext().getSharedPreferences("PREFS", MODE_PRIVATE);
        PREFS_TOPIC = sharedPreferences.getString("PREFS_TOPIC", "");
        PREFS_CLIENT_ID = sharedPreferences.getString("PREFS_CLIENT_ID", "");

        mqttHelper = new MqttHelper(context);

        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                Log.w("Mqtt","Connect complete ");
                CONNECTED_MQTT = true;
                try {
                    CONNECTED_GPS = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                } catch(Exception ex) {}
                if (CONNECTED_GPS) {
                    startForeground(ForegroundLocationService.GPS_NOTIFICATION, notifyUserThatLocationServiceStarted());

                }else {if (!CONNECTED_GPS){
                    startForeground(ForegroundLocationService.GPS_NOTIFICATION, notifyUserThatLocationServiceStoped());
                }
                }
                mqttHelper.subscribeToTopic();
                try {
                    mqttHelper.publish("0/state/" + PREFS_CLIENT_ID,new MqttMessage("1".getBytes()));
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                STOCKAGE_PAS_DECONNEXION = ExpedieLesLocalisations();
                if (!STOCKAGE_PAS_DECONNEXION.equals("")){
                    MqttMessage myMess = new MqttMessage(STOCKAGE_PAS_DECONNEXION.getBytes());
                    try {
                        mqttHelper.publish("0/" + PREFS_TOPIC + "/" + PREFS_CLIENT_ID, myMess);
                        //VideLaBase();
                        Log.w(TAG, "Stockage expédié : " + myMess);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
                Log.w("Debug", "MQTT CONNECTED");
            }

            @Override
            public void connectionLost(Throwable throwable) {
                Log.w("Debug", "MQTT CONNECTION LOST");
                CONNECTED_MQTT = false;
                if (CONNECTED_GPS) {
                    startForeground(ForegroundLocationService.GPS_NOTIFICATION, notifyUserThatConnectionLosted());
                }else{if (!CONNECTED_GPS){
                    startForeground(ForegroundLocationService.GPS_NOTIFICATION, notifyUserThatLocationServiceStoped());
                }
                }
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                if(mqttMessage.toString().equals("OK")){
                    Log.w("Debug", "J'ai reçu " + mqttMessage.toString() + " comme confirmation");
                } else {
                    if(mqttMessage.toString().equals("123456")){
                        Log.w("Debug","j'ai reçu : " + mqttMessage.toString());
                        Toast.makeText(context, "Service Stop", Toast.LENGTH_LONG).show();
                        stopService(new Intent(context, ForegroundLocationService.class));
                        MqttMessage myMess = new MqttMessage("OK".getBytes());
                        mqttHelper.publish("debug1/" + PREFS_TOPIC + "/" + PREFS_CLIENT_ID, myMess);
                    }else {
                        if(mqttMessage.toString().equals("1234567")){
                            Log.w("Debug","j'ai reçu : " + mqttMessage.toString());
                            Toast.makeText(context, "Service Start", Toast.LENGTH_LONG).show();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(new Intent(context, ForegroundLocationService.class));
                            }else {
                                startService(new Intent(context, ForegroundLocationService.class));
                            }
                            MqttMessage myMess = new MqttMessage("OK".getBytes());
                            mqttHelper.publish("debug1/" + PREFS_TOPIC + "/" + PREFS_CLIENT_ID, myMess);
                        }else {
                            if(mqttMessage.toString().equals("restart")){
                                stopService(new Intent(getApplicationContext(), ForegroundLocationService.class));
                                Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(i);
                            } else{
                                Log.w("Debug","j'ai reçu : " + mqttMessage.toString());
                                MqttMessage myMess = new MqttMessage("OK".getBytes());
                                mqttHelper.publish("debug1/" + PREFS_TOPIC + "/" + PREFS_CLIENT_ID, myMess);
                                setMessageNotification(topic, new String(mqttMessage.getPayload()));
                            }
                        }
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                Log.w("Debug", "MQTT DELIVERY COMPLETE!");
                VideLaBase();
            }
        });
    }

    public boolean isPackageExisted(String targetPackage){
        PackageManager pm=getPackageManager();
        try {
            PackageInfo info=pm.getPackageInfo(targetPackage,PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }
    private void setMessageNotification(String topic, String msg) {
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

        if(isPackageExisted("io.cordova.stradapilot")){
            // ouvre STRADApilot en APK si installé
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage("io.cordova.stradapilot");
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_name_message)
                    .setContentTitle("topic : "+topic)
                    .setContentText("text : "+msg)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(msg + "\n" + "De : " +  topic))
                    .setAutoCancel(true)
                    .setVibrate(new long[]{0, 1000, 500, 1000})
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    ;

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            // notificationId is a unique int for each notification that you must define
            notificationManager.notify(notificationId[0], mBuilder.build());
        }else{
            if(isPackageExisted("org.chromium.webapk.a5d468639fd3a2399")){
            // ouvre STRADApilot en APK si installé
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage("org.chromium.webapk.a5d468639fd3a2399");
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_name_message)
                    .setContentTitle("topic : "+topic)
                    .setContentText("text : "+msg)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(msg + "\n" + "De : " +  topic))
                    .setAutoCancel(true)
                    .setVibrate(new long[]{0, 1000, 500, 1000})
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    ;

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            // notificationId is a unique int for each notification that you must define
            notificationManager.notify(notificationId[0], mBuilder.build());
        }else{
            // ouvre STRADApilot en webView
            Intent notificationIntent = new Intent(context, Main3Activity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_name_message)
                    .setContentTitle("topic : "+topic)
                    .setContentText("text : "+msg)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(msg + "\n" + "De : " +  topic))
                    .setAutoCancel(true)
                    .setVibrate(new long[]{0, 1000, 500, 1000})
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            // notificationId is a unique int for each notification that you must define
            notificationManager.notify(notificationId[0], mBuilder.build());
            }
        }
    }


    protected Notification notifyUserThatLocationServiceStarted() {

        Intent notificationIntent = new Intent(this, MainActivity.class);

        // Construct a task stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity.class);

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent);

        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        // Define the notification settings.
        builder
                .setSmallIcon(R.drawable.ic_strada_location_on)
                .setContentTitle(getString(R.string.foreground_location_service))
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),R.mipmap.ic_launcher_strada_round))
                .setContentText(getString(R.string.service_is_running))
                .setContentIntent(pendingIntent);

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true);

        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.app_name);
            // Create the channel for the notification
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW);

            // Set the Notification Channel for the Notification Manager.
            mNotificationManager.createNotificationChannel(mChannel);

            // Channel ID
            builder.setChannelId(CHANNEL_ID);
        }
        final Notification notification;
        if (Build.VERSION.SDK_INT < 16) {
            notification = builder.getNotification();
        } else {
            notification = builder.build();
        }
        return notification;
    }

    private Notification notifyUserThatConnectionLosted() {
        Intent notificationIntent = new Intent(context, MainActivity.class);

        // Construct a task stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity.class);

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent);

        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        // Define the notification settings.
        builder
                .setSmallIcon(R.drawable.ic_stat_no_reseau)
                .setContentTitle(getString(R.string.foreground_location_service))
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),R.mipmap.ic_launcher_strada_round))
                .setColor(Color.RED)
                .setContentText(getString(R.string.connexion_is_lost))
                .setContentIntent(pendingIntent);

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true);

        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.app_name);
            // Create the channel for the notification
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW);
            // Set the Notification Channel for the Notification Manager.
            mNotificationManager.createNotificationChannel(mChannel);

            // Channel ID
            builder.setChannelId(CHANNEL_ID);
        }

        // Issue the notification
       //mNotificationManager.notify(0, builder.build());
        final Notification notification;
        if (Build.VERSION.SDK_INT < 16) {
            notification = builder.getNotification();
        } else {
            notification = builder.build();
        }
        return notification;
    }


    private Notification notifyUserThatLocationServiceStoped() {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.putExtra("from_notification", true);
        // Construct a task stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity.class);

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent);

        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        // Define the notification settings.
        builder
                .setSmallIcon(R.drawable.ic_action_location_off)
                .setContentTitle(getString(R.string.foreground_location_service))
                .setContentText(getString(R.string.service_is_stop))
                .setContentIntent(pendingIntent)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),R.mipmap.ic_launcher_strada_round))
                .setColor(Color.RED)
                .setContentIntent(pendingIntent);

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true);

        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.app_name);
            // Create the channel for the notification
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW);
            // Set the Notification Channel for the Notification Manager.
            mNotificationManager.createNotificationChannel(mChannel);

            // Channel ID
            builder.setChannelId(CHANNEL_ID);
        }

        // Issue the notification
        //mNotificationManager.notify(0, builder.build());
        final Notification notification;
        if (Build.VERSION.SDK_INT < 16) {
            notification = builder.getNotification();
        } else {
            notification = builder.build();
        }
        return notification;
    }

    private void EnregistreEnBase(String LocEnBase) {
        BDDmanager m = new BDDmanager(context); // gestionnaire de la table "BDDlocalisation"
        m.open(); // ouverture de la table en lecture/écriture
        // insertion. L'id sera attribué automatiquement par incrément
        m.addLocalisation(new BDDlocalisation(0,LocEnBase));
        // fermeture du gestionnaire
        m.close();
    }

    private void VideLaBase() {
        BDDmanager m = new BDDmanager(context); // gestionnaire de la table "BDDlocalisation"
        // ouverture de la table en lecture/écriture
        m.open();
        // suppression
        m.deleteAll();
    }

    public String ExpedieLesLocalisations(){
        String LocalisationsAExpedier = "";
        // gestionnaire de la table "BDDlocalisation"
        BDDmanager m = new BDDmanager(context);
        // ouverture de la table en lecture/écriture
        m.open();

        // Listing des enregistrements de la table
        Cursor c = m.getLocalisation();
        if (c.moveToFirst())
        {
            do {
                Log.e("test",
                        c.getInt(c.getColumnIndex(BDDmanager.KEY_ID_LOCALISATION)) + "," +
                                c.getString(c.getColumnIndex(BDDmanager.KEY_NOM_LOCALISATION))
                );
                LocalisationsAExpedier = LocalisationsAExpedier + "\n" + c.getString(c.getColumnIndex(BDDmanager.KEY_NOM_LOCALISATION)) ;
            }
            while (c.moveToNext());
        }
        c.close(); // fermeture du curseur
        m.close();// fermeture de la base en lecture/écriture
        return LocalisationsAExpedier;
    }

    public boolean isForeground(String myPackage) {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfo = manager.getRunningTasks(1);
        ComponentName componentInfo = runningTaskInfo.get(0).topActivity;
        return componentInfo.getPackageName().equals(myPackage);
    }
}