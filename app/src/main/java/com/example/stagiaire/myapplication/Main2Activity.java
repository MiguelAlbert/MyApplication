package com.example.stagiaire.myapplication;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttService;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.List;

import static com.example.stagiaire.myapplication.MqttHelper.mqttAndroidClient;

@RequiresApi(api = Build.VERSION_CODES.N)
public class Main2Activity extends AppCompatActivity  {

    //mqtt
    MqttHelper mqttHelper;

    //sharedPreferences
    SharedPreferences sharedPreferences;
    String PREFS_CLIENT_ID;
    String PREFS_TOPIC;

    private static final int PERMISSION_REQUEST_ID = 1999;
    private static final String TAG = MainActivity.class.getSimpleName();
    private String STOCKAGE_PAS_DECONNEXION = "";

    private Context context;
    Button btnSaveQuit;
    Button btnParDefaut;
    Button btClearSharedPrefs;
    Button btnTest;
    Button btnListeBDD, btnExpedierBDD;
    EditText etServeurUri,etIdentifiant,etTopic,etClientID,etMDP,etSecondes,etVitesseStart,etDistanceStart,etCapStart,etDeceleration,etTempsInactif;
    TextView tvEntete;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        context = getApplicationContext();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        sharedPreferences = getBaseContext().getSharedPreferences("PREFS", MODE_PRIVATE);

        etServeurUri = (EditText)findViewById(R.id.etServeurUri);
        etIdentifiant = (EditText)findViewById(R.id.etIdentifiant);
        etTopic = (EditText)findViewById(R.id.etTopic);
        etClientID = (EditText)findViewById(R.id.etClientID);
        etMDP = (EditText)findViewById(R.id.etMDP);
        etSecondes = (EditText)findViewById(R.id.etSecondes);
        etVitesseStart = (EditText)findViewById(R.id.etVitesseStart);
        etDistanceStart = (EditText)findViewById(R.id.etDistanceStart);
        etCapStart = (EditText)findViewById(R.id.etCapStart);
        etDeceleration = (EditText)findViewById(R.id.etDeceleration);
        etTempsInactif = (EditText)findViewById(R.id.etTempsInactif);
        tvEntete = (TextView)findViewById(R.id.tvEntete);

        btnSaveQuit = (Button) findViewById(R.id.btnSaveQuit);
        btnSaveQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Sauvegarde
                sharedPreferences
                        .edit()
                        .putString("PREFS_SERVEUR_URI", etServeurUri.getText().toString())
                        .putString("PREFS_IDENTIFIANT", etIdentifiant.getText().toString())
                        .putString("PREFS_TOPIC", etTopic.getText().toString())
                        .putString("PREFS_CLIENT_ID", etClientID.getText().toString())
                        .putString("PREFS_MDP", etMDP.getText().toString())
                        .putString("PREFS_SECONDES", etSecondes.getText().toString())
                        .putString("PREFS_VITESSE_START", etVitesseStart.getText().toString())
                        .putString("PREFS_DISTANCE_START", etDistanceStart.getText().toString())
                        .putString("PREFS_CAP_START", etCapStart.getText().toString())
                        .putString("PREFS_DECELERATION", etDeceleration.getText().toString())
                        .putString("PREFS_TEMPS_INACTIF", etTempsInactif.getText().toString())
                        .apply();
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
                String val11 = sharedPreferences.getString("PREFS_TEMPS_INACTIF", "");
                if (val1.equals("") || val2.equals("") || val3.equals("") || val4.equals("") || val5.equals("") || val6.equals("")|| val7.equals("")|| val8.equals("")|| val9.equals("")|| val10.equals("")|| val11.equals("")) {
                    Toast.makeText(context, "Veuillez complèter le formulaire", Toast.LENGTH_SHORT).show();
                } else {
                    //Quit
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(new Intent(context, ForegroundLocationService.class));
                    } else{
                        startService(new Intent(context, ForegroundLocationService.class));
                    }
                    new  MqttService();
                    Intent i2 = new Intent(Main2Activity.this, MainActivity.class);
                    startActivity(i2);
                    finish();
                }
            }
        });
        btnParDefaut = (Button)findViewById(R.id.btnParDefaut);
        btnParDefaut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DefautValue();

            }
        });


        btClearSharedPrefs = (Button) findViewById(R.id.btClearSharedPrefs);
        btClearSharedPrefs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(new Intent(context,MqttService.class));
                etServeurUri.setText("");
                etIdentifiant.setText("");
                etTopic.setText("");
                etClientID.setText("");
                etMDP.setText("");
                etSecondes.setText("");
                etVitesseStart.setText("");
                etDistanceStart.setText("");
                etCapStart.setText("");
                etDeceleration.setText("");
                etTempsInactif.setText("");
                clearSharedPrefs();
            }
        });

        btnTest = (Button)findViewById(R.id.btnTest);
        btnTest.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View v) {
                testMqtt();
            }
        });
        btnListeBDD = (Button)findViewById(R.id.btnListeBDD);
        btnListeBDD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String listedeslocs = "";
                BDDmanager m = new BDDmanager(context); // gestionnaire de la table "BDDlocalisation"
                m.open(); // ouverture de la table en lecture/écriture

                // Listing des enregistrements de la table
                Cursor c = m.getLocalisation();
                if (c.moveToFirst())
                {
                    do {
                        Log.d("test",
                                c.getInt(c.getColumnIndex(BDDmanager.KEY_ID_LOCALISATION)) + "," +
                                        c.getString(c.getColumnIndex(BDDmanager.KEY_NOM_LOCALISATION))
                        );
                        listedeslocs = listedeslocs + "\n" + c.getString(c.getColumnIndex(BDDmanager.KEY_NOM_LOCALISATION)) ;
                    }
                    while (c.moveToNext());
                }
                c.close(); // fermeture du curseur
                Toast.makeText(context, listedeslocs, Toast.LENGTH_LONG).show();
                m.close();
            }
        });
        btnExpedierBDD= (Button)findViewById(R.id.btnExpedierBDD);
        btnExpedierBDD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                STOCKAGE_PAS_DECONNEXION = ExpedieLesLocalisations();
                Toast.makeText(context, STOCKAGE_PAS_DECONNEXION, Toast.LENGTH_SHORT).show();
                if(isMyServiceRunning(MqttService.class)){
                    MqttMessage myMess = new MqttMessage(STOCKAGE_PAS_DECONNEXION.getBytes());
                    try {
                        mqttHelper.publish("0/" + PREFS_TOPIC + "/" + PREFS_CLIENT_ID, myMess);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                } else{
                    Toast.makeText(context, "Pas de service", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onBackPressed() { }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    private void clearSharedPrefs() {
        SharedPreferences preferences = getSharedPreferences("PREFS", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.commit();
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

    private void testMqtt() {
        if(isMyServiceRunning(MqttHelper.class)){
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
        }
        sharedPreferences
                .edit()
                .putString("PREFS_SERVEUR_URI", etServeurUri.getText().toString())
                .putString("PREFS_IDENTIFIANT", etIdentifiant.getText().toString())
                .putString("PREFS_TOPIC", etTopic.getText().toString())
                .putString("PREFS_CLIENT_ID", etClientID.getText().toString())
                .putString("PREFS_MDP", etMDP.getText().toString())
                .putString("PREFS_SECONDES", etSecondes.getText().toString())
                .putString("PREFS_VITESSE_START", etVitesseStart.getText().toString())
                .putString("PREFS_DISTANCE_START", etDistanceStart.getText().toString())
                .putString("PREFS_CAP_START", etCapStart.getText().toString())
                .putString("PREFS_DECELERATION", etDeceleration.getText().toString())
                .putString("PREFS_TEMPS_INACTIF", etTempsInactif.getText().toString())
                .apply();
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
        String val11 = sharedPreferences.getString("PREFS_TEMPS_INACTIF", "");
        if (val1.equals("") || val2.equals("") || val3.equals("") || val4.equals("") || val5.equals("") || val6.equals("")|| val7.equals("")|| val8.equals("")|| val9.equals("")|| val10.equals("")|| val11.equals("")) {
            Toast.makeText(context, "Veuillez complèter le formulaire", Toast.LENGTH_SHORT).show();
        } else {
            btnParDefaut.setEnabled(false);
            btnSaveQuit.setEnabled(false);
            btnTest.setEnabled(false);
            btClearSharedPrefs.setEnabled(false);
            tvEntete.setBackgroundColor(Color.YELLOW);
            tvEntete.setText("TEST *** Connexion - 10 secondes");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(new Intent(context, MqttService.class));
            } else{
                startService(new Intent(context,MqttService.class));
            }
            mqttHelper = new MqttHelper(context);
            mqttHelper.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectionLost(Throwable cause) {

                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {

                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }

                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    mqttHelper.subscribeToTopic();
                    try {
                        mqttHelper.publish("0/" + PREFS_TOPIC + "/" + PREFS_CLIENT_ID, new MqttMessage("test".getBytes()));
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
            });
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(ForegroundLocationService.CONNECTED_MQTT){
                        tvEntete.setBackgroundColor(Color.GREEN);
                        tvEntete.setText("Connexion Mqtt");
                    } else{
                        tvEntete.setBackgroundColor(Color.RED);
                        tvEntete.setText("Connexion Mqtt");
                    }
                    btnParDefaut.setEnabled(true);
                    btnSaveQuit.setEnabled(true);
                    btnTest.setEnabled(true);
                    btClearSharedPrefs.setEnabled(true);
                }
            }, 10000);

        }
    }

    private boolean checkSystemPermissions() {

        Log.i(TAG, "Checking for the necessary permissions");

        final int granted = PackageManager.PERMISSION_GRANTED;

        // only for M and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // list of permissions to check
            final String permissionsToCheck[] = new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.WAKE_LOCK
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
                requestPermissions(permissionsStillNeeded.toArray(new String[permissionsStillNeeded.size()]),
                        PERMISSION_REQUEST_ID);
                return false;
            }
        }
        return true;
    }

    private void DefautValue() {
        etServeurUri.setText("tcp://bressuire.strada.fr:6001");
        etIdentifiant.setText("");
        etTopic.setText("tracking");
        etClientID.setText("Miguel");
        etMDP.setText("");
        etSecondes.setText("30");
        etVitesseStart.setText("10");;
        etDistanceStart.setText("1");
        etCapStart.setText("20");
        etDeceleration.setText("20");
        etTempsInactif.setText("60");
        sharedPreferences
                .edit()
                .putString("PREFS_SERVEUR_URI", etServeurUri.getText().toString())
                .putString("PREFS_IDENTIFIANT", etIdentifiant.getText().toString())
                .putString("PREFS_TOPIC", etTopic.getText().toString())
                .putString("PREFS_CLIENT_ID", etClientID.getText().toString())
                .putString("PREFS_MDP", etMDP.getText().toString())
                .putString("PREFS_SECONDES", etSecondes.getText().toString())
                .putString("PREFS_VITESSE_START", etVitesseStart.getText().toString())
                .putString("PREFS_DISTANCE_START", etDistanceStart.getText().toString())
                .putString("PREFS_CAP_START", etCapStart.getText().toString())
                .putString("PREFS_DECELERATION", etDeceleration.getText().toString())
                .putString("PREFS_TEMPS_INACTIF", etTempsInactif.getText().toString())
                .apply();
    }


    @Override
    public void onResume() {

        if (!checkSystemPermissions()) {
            super.onResume();
            return;
        }
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
        String val11 = sharedPreferences.getString("PREFS_TEMPS_INACTIF", "");
        etServeurUri.setText(val1);
        etIdentifiant.setText(val2);
        etTopic.setText(val3);
        etClientID.setText(val4);
        etMDP.setText(val5);
        etSecondes.setText(val6);
        etVitesseStart.setText(val7);
        etDistanceStart.setText(val8);
        etCapStart.setText(val9);
        etDeceleration.setText(val10);
        etTempsInactif.setText(val11);

        if(ForegroundLocationService.CONNECTED_MQTT == false){
            //Toast.makeText(this, "Erreur de connexion", Toast.LENGTH_LONG).show();
            tvEntete.setBackgroundColor(Color.RED);
        } else{
            //Toast.makeText(this, "Connexion OK", Toast.LENGTH_LONG).show();
            tvEntete.setBackgroundColor(Color.GREEN);
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
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
}
