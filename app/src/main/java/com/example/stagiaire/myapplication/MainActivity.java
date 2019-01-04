package com.example.stagiaire.myapplication;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttService;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttDisconnect;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.location.ActivityRecognitionClient;

import static android.graphics.Color.GREEN;
import static android.graphics.Color.RED;
import static com.example.stagiaire.myapplication.MqttHelper.mqttAndroidClient;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    //Ouvre appli en APK
    Button btnOuvreAppli;
    //public static Runnable runnable;

    private static final int REQUEST_ENABLE_BT = 3;
    //Service
    Button  btnStop, btnParam,btnClose,btnTestRedem;
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_ID = 1999;

    //Préférences
    SharedPreferences sharedPreferences;

    private Context context;
    static TextView tvActivity,tvActivity2;

    MyPhoneStateListener MyListener;
    TelephonyManager Tel;

    LocationManager manager;

    private SensorManager sensorMan;
    private Sensor accelerometer;

    private float[] mGravity;
    private double mAccel;
    private double mAccelCurrent;
    private double mAccelLast;

    private boolean sensorRegistered = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        MyListener = new MyPhoneStateListener();

        Tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        Tel.listen(MyListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);


        sensorMan = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;

        sensorMan.registerListener(this, accelerometer,SensorManager.SENSOR_DELAY_NORMAL);
        sensorRegistered = true;

        tvActivity = (TextView)findViewById(R.id.tvActivity);
        tvActivity2= (TextView)findViewById(R.id.tvActivity2);


        manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
// on lance le JOB que pour OREO
            long flexMillis = 15 * 60 * 1000;
// le temps entre chaque réquete (15 minutes)

            JobScheduler jobScheduler = (JobScheduler) getApplicationContext()
                    .getSystemService(JOB_SCHEDULER_SERVICE);

            ComponentName componentName = new ComponentName(this,
                    MyJobService.class);
// on instancie notre classe que nous créerons plus tard

            JobInfo jobInfoObj = new JobInfo.Builder(1, componentName).setPeriodic(flexMillis, flexMillis).setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
// type de réseau nécessaire si connecté ou non.
                    .setRequiresCharging(false)
// si la tache nécessite un smartphone chargé ou non
                    .setPersisted(true).build();
// si la tache doit toujours rester active ou non
            jobScheduler.schedule(jobInfoObj);
        }

        btnStop = (Button) findViewById(R.id.buttonStop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View v) {
                stopService(new Intent(getApplicationContext(), ForegroundLocationService.class));
                //new MqttDisconnect();
                try {
                    MqttHelper.publish("0/state/" + sharedPreferences.getString("PREFS_CLIENT_ID", ""),new MqttMessage("0".getBytes()));
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                try {
                    MqttHelper.disconnect(mqttAndroidClient);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                btnTestRedem.setEnabled(true);
                btnStop.setEnabled(false);
                //android.os.Process.killProcess(android.os.Process.myPid());
                //ActivityCompat.finishAffinity(MainActivity.this);
                //System.exit(0);
            }
        });

        btnTestRedem = (Button)findViewById(R.id.btnTestRedem);
        btnTestRedem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    buildAlertMessageNoGps();
                } else {
                    if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.O) {


                        startForegroundService(new Intent(context, ForegroundLocationService.class));
                    }
                    else {
                        startService(new Intent(context, ForegroundLocationService.class));
                    }
                }
                btnTestRedem.setEnabled(false);
                btnStop.setEnabled(true);
            }
        });

        btnParam = (Button)findViewById(R.id.btnParam);
        btnParam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*if(isMyServiceRunning(MqttHelper.class)){
                    try {
                        MqttHelper.publish("0/state/" + sharedPreferences.getString("PREFS_CLIENT_ID", ""),new MqttMessage("0".getBytes()));
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                    try {
                        MqttHelper.unSubscribe(mqttAndroidClient,"tracking");
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                    try {
                        MqttHelper.unSubscribe(mqttAndroidClient,"file");
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                    try {
                        MqttHelper.disconnect(mqttAndroidClient);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
                stopService(new Intent(getApplicationContext(), ForegroundLocationService.class));
                btnTestRedem.setEnabled(true);
                btnStop.setEnabled(false);
                */
                Intent i = new Intent(MainActivity.this, Main2Activity.class);
                startActivity(i);
                finish();
            }
        });

        btnClose = (Button)findViewById(R.id.btnClose);
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();

            }
        });

        //Ouvre autre appli
        btnOuvreAppli = (Button) findViewById(R.id.btnOuvreAppli);
        btnOuvreAppli.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isPackageExisted("io.cordova.stradapilot")) {
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage("io.cordova.stradapilot");
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (launchIntent != null) {
                        startActivity(launchIntent);//null pointer check in case package name was not found
                        finish();
                    }
                } else{
                    if(isPackageExisted("org.chromium.webapk.a5d468639fd3a2399")) {
                        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("org.chromium.webapk.a5d468639fd3a2399");
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        if (launchIntent != null) {
                            startActivity(launchIntent);//null pointer check in case package name was not found
                            finish();
                        }
                    }else{
                        Intent i = new Intent(MainActivity.this, Main3Activity.class);
                        startActivity(i);
                        finish();
                    }
                }
            }
        });

        sharedPreferences = getSharedPreferences("PREFS", Context.MODE_PRIVATE); // get the set of Preferences labeled "PREFS"
        String val1 = sharedPreferences.getString("PREFS_SERVEUR_URI", "");
        String val2 = sharedPreferences.getString("PREFS_IDENTIFIANT", "");
        String val3 = sharedPreferences.getString("PREFS_TOPIC", "");
        String val4 = sharedPreferences.getString("PREFS_CLIENT_ID", "");
        String val5 = sharedPreferences.getString("PREFS_MDP", "");
        String val6 = sharedPreferences.getString("PREFS_SECONDES", "");
        String val7 = sharedPreferences.getString("PREFS_VITESSE_START", "");
        String val8 = sharedPreferences.getString("PREFS_DISTANCE_START", "");
        String val9 = sharedPreferences.getString("PREFS_CAP_START", "");
        String val10 = sharedPreferences.getString("PREFS_DECELERATION", "");
        if (val1.equals("") || val2.equals("") || val3.equals("") || val4.equals("") || val5.equals("") || val6.equals("")|| val7.equals("")|| val8.equals("")|| val9.equals("")|| val10.equals("")) {
            Intent i = new Intent(MainActivity.this, Main2Activity.class);
            startActivity(i);
            finish();
        } else {
            if (!manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
                buildAlertMessageNoGps();

            } else {
                if (!isMyServiceRunning(ForegroundLocationService.class)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        startForegroundService(new Intent(context, ForegroundLocationService.class));

                        //Intent it = new Intent(context, MyJobSchedular.class);
                        //MyJobSchedular.enqueueWork(context, it);
                    }else {
                        startService(new Intent(context, ForegroundLocationService.class));
                    }
                    Handler handler = new Handler();

                    handler.postDelayed(new Runnable() {
                        public void run() {

                            finish();
                        }
                    }, 100);
                }
            }
        }
        //écran toujours allumé
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Intent intent = new Intent();
        String packageName = this.getPackageName();
        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (pm.isIgnoringBatteryOptimizations(packageName))
                intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            else {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
            }
        }
        this.startActivity(intent);

        if (isMyServiceRunning(ForegroundLocationService.class)) {
            btnTestRedem.setEnabled(false);
            btnStop.setEnabled(true);
        }else{
            btnTestRedem.setEnabled(true);
            btnStop.setEnabled(false);
        }
    }

    @Override
    public void onBackPressed() { }

    private class MyPhoneStateListener extends PhoneStateListener {
        String gsmStrength = "";
        String gsmStrength2 = "";
        String gsmStrength3 = "";
        String gsmStrength4 = "";
        String gsmStrength5 = "";
        String gsmStrength6 = "";
        String gsmStrength7 = "";
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {

            super.onSignalStrengthsChanged(signalStrength);

            if (signalStrength.isGsm()) {
                gsmStrength = String.valueOf(signalStrength.getGsmSignalStrength());
                gsmStrength2 = String.valueOf(signalStrength.getEvdoDbm());
                gsmStrength3 = String.valueOf(signalStrength.getCdmaEcio());
                gsmStrength4 = String.valueOf(signalStrength.getCdmaEcio());
                gsmStrength5 = String.valueOf(signalStrength.getLevel()*100/4 + "%");
                gsmStrength6 = String.valueOf(signalStrength.getGsmBitErrorRate());
                gsmStrength7 = String.valueOf(signalStrength.getGsmSignalStrength());
                tvActivity2.setText(MyListener.getStrength() + " dBm "+MyListener.getStrength2()+" "+MyListener.getStrength3()+" "+MyListener.getStrength5()+" "+MyListener.getStrength4()+" "+MyListener.getStrength6()+" "+MyListener.getStrength7());
                Log.d("SIGNAL STRENGTH", String.valueOf(signalStrength.getGsmSignalStrength()));
            }
        }

        public String getStrength() {
            return gsmStrength;
        }
        public String getStrength2() {
            return gsmStrength2;
        }
        public String getStrength3() { return gsmStrength3; }
        public String getStrength4() {
            return gsmStrength4;
        }
        public String getStrength5() {
            return gsmStrength5;
        }
        public String getStrength6() {
            return gsmStrength6;
        }
        public String getStrength7() {
            return gsmStrength7;
        }
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

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Le GPS n'est pas activé. Souhaitez-vous l'activer?")
                .setCancelable(false)
                .setPositiveButton("Oui", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        Intent enableIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);

                        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.O) {
                            startForegroundService(new Intent(context, MqttService.class));
                            startForegroundService(new Intent(getApplicationContext(), ForegroundLocationService.class).putExtra("tempsSecondes", sharedPreferences.getString("PREFS_SECONDES", "")));
                        }
                        else {
                            startService(new Intent(context, ForegroundLocationService.class).putExtra("tempsSecondes", sharedPreferences.getString("PREFS_SECONDES", "")));
                            startService(new Intent(context, MqttService.class));
                        }


                        btnStop.setEnabled(true);
                    }
                })
                .setNegativeButton("Non", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (manager.isProviderEnabled( LocationManager.GPS_PROVIDER)) {
                    stopService(new Intent(getApplicationContext(),ForegroundLocationService.class));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(new Intent(getApplicationContext(), ForegroundLocationService.class));
                    } else{
                        startService(new Intent(getApplicationContext(), ForegroundLocationService.class));
                    }
                } else {
                    Toast.makeText(this, "not available",Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onResume() {

        if (!checkSystemPermissions()) {
            super.onResume();
            if (!isMyServiceRunning(ForegroundLocationService.class)) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(new Intent(getApplicationContext(), ForegroundLocationService.class));
                } else{
                    startService(new Intent(getApplicationContext(), ForegroundLocationService.class));
                }
            }
            return;
        }
        if (isMyServiceRunning(ForegroundLocationService.class)) {
            btnTestRedem.setEnabled(false);
            btnStop.setEnabled(true);
        }else{
            btnTestRedem.setEnabled(true);
            btnStop.setEnabled(false);
        }
        Tel.listen(MyListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        super.onResume();
    }

    private boolean checkSystemPermissions() {

        Log.i(TAG, "Vérifie les permissions permissions");

        final int granted = PackageManager.PERMISSION_GRANTED;

        // only for M and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // list of permissions to check
            final String permissionsToCheck[] = new String[]{
                    //Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,

            };

            // list of permissions still needed
            final List<String> permissionsStillNeeded = new ArrayList<>();

            // loop through all permissions to check, if any are not granted, add them
            // to the list of permissions that are still needed so they can be requested all
            // at once later
            for (String permission : permissionsToCheck) {

                if (granted != checkSelfPermission(permission)) {
                    permissionsStillNeeded.add(permission);
                }
            }
            // request permission(s), if necessary
            if (permissionsStillNeeded.size() > 0) {
                requestPermissions(permissionsStillNeeded.toArray(new String[permissionsStillNeeded.size()]),PERMISSION_REQUEST_ID);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onPause() {
        Tel.listen(MyListener, PhoneStateListener.LISTEN_NONE);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        new MqttDisconnect();
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        new MqttDisconnect();
        super.onStop();
    }


    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
    private int hitCount = 0;
    private double hitSum = 0;

    

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        final int SAMPLE_SIZE = 10; // 50 change this sample size as you want, higher is more precise but slow measure.
        final double THRESHOLD = 0.08; // 0.2 change this threshold as you want, higher is more spike movement
        double hitResult = 0;
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mGravity = sensorEvent.values.clone();
            // Shake detection
            double x = mGravity[0];
            double y = mGravity[1];
            double z = mGravity[2];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = Math.sqrt(x * x + y * y + z * z);
            double delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta;

            if (hitCount <= SAMPLE_SIZE) {
                hitCount++;
                hitSum += Math.abs(mAccel);
            } else {
                hitResult = hitSum / SAMPLE_SIZE;
                //Log.d(TAG, String.valueOf(hitResult));
                if (hitResult > THRESHOLD) {
                    //Log.d(TAG, "Driving");
                    tvActivity.setText("Driving");
                    tvActivity.setTextColor(GREEN);
                } else {
                    //Log.d(TAG, "Stop Driving");
                    tvActivity.setText("Stop Driving");
                    tvActivity.setTextColor(RED);
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
}
