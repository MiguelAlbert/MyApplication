package com.example.stagiaire.myapplication;



import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import static android.content.ContentValues.TAG;

public class MqttHelper {

    private Context context;
    static MqttAndroidClient mqttAndroidClient;

    private String serverUri;
    private String clientId;
    private String subscriptionTopic;
    private String username;
    private String password;


    @RequiresApi(api = Build.VERSION_CODES.N)
    public MqttHelper(final Context context){
        this.context=context;

        SharedPreferences sharedPreferences = context.getSharedPreferences("PREFS", Context.MODE_PRIVATE);
        serverUri = sharedPreferences.getString("PREFS_SERVEUR_URI", "");
        clientId = sharedPreferences.getString("PREFS_IDENTIFIANT", "");
        subscriptionTopic = sharedPreferences.getString("PREFS_TOPIC", "");
        username = sharedPreferences.getString("PREFS_CLIENT_ID", "");
        password = sharedPreferences.getString("PREFS_MDP", "");

        mqttAndroidClient = new MqttAndroidClient(context, serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {

            @Override
            public void connectComplete(boolean b, String s) {
                Log.w("mqtt", s);
                //Toast.makeText(context, "connexion complète", Toast.LENGTH_LONG).show();
                ForegroundLocationService.CONNECTED_MQTT = true;
            }


            @Override
            public void connectionLost(Throwable throwable) {
                Log.w("Mqtt","Connection Lost");
                ForegroundLocationService.CONNECTED_MQTT = false;
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                Log.w("Mqtt", mqttMessage.toString());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
        connect();
    }

    public void setCallback(MqttCallbackExtended callback) {
        mqttAndroidClient.setCallback(callback);
    }

    public static void publish(String sTopic, MqttMessage sMessage) throws MqttException {
        mqttAndroidClient.publish(sTopic,sMessage);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void connect(){
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
        mqttConnectOptions.setUserName(username);
        mqttConnectOptions.setPassword(password.toCharArray());
        mqttConnectOptions.setWill("0/state/" + username ,"0".getBytes(),0,false ) ;

        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    //disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    MqttConnectOptions authen = new MqttConnectOptions();
                    authen.setConnectionTimeout(3);
                    Log.w("Mqtt","On succes Connect");
                    ForegroundLocationService.CONNECTED_MQTT = true;
                    }


                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.w("Mqtt", "Failed to connect to: " + serverUri + " " + exception.toString());
                    ForegroundLocationService.CONNECTED_MQTT = false;
                    }
            });

        } catch (MqttException ex){
            ex.printStackTrace();
        }
    }

    public static void disconnect(@NonNull MqttAndroidClient client)throws MqttException {
        IMqttToken mqttToken = client.disconnect();
        mqttToken.setActionCallback(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken iMqttToken) {
                Log.d(TAG, "Successfully disconnected");
            }
            @Override
            public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                Log.d(TAG, "Failed to disconnected " + throwable.toString());

            }
        });
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void subscribeToTopic() {
        try {
            mqttAndroidClient.subscribe(clientId + "/" + subscriptionTopic + "/+" , 0, null, new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.w("Mqtt","Subscribed! to "+subscriptionTopic);
                    ForegroundLocationService.CONNECTED_MQTT = true;
                }


                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.w("Mqtt", "Subscribed fail!");
                    Toast.makeText(context, "Impossible de souscrire au topic", Toast.LENGTH_SHORT).show();
                    ForegroundLocationService.CONNECTED_MQTT = false;
                }
            });

        } catch (MqttException ex) {
            System.err.println("Exceptionst subscribing");
            ex.printStackTrace();
        }
        try {
            mqttAndroidClient.subscribe(clientId + "/" + "file" + "/+" , 0, null, new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.w("Mqtt","Subscribed! to "+" file de Damien");
                    ForegroundLocationService.CONNECTED_MQTT = true;
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.w("Mqtt", "Subscribed fail!");
                    Toast.makeText(context, "Impossible de souscrire au topic", Toast.LENGTH_SHORT).show();
                    ForegroundLocationService.CONNECTED_MQTT = false;
                }
            });

        } catch (MqttException ex) {
            System.err.println("Exceptionst subscribing");
            ex.printStackTrace();
        }
    }

    public static void unSubscribe(@NonNull MqttAndroidClient client,
                                   @NonNull final String topic) throws MqttException {

        IMqttToken token = mqttAndroidClient.unsubscribe(topic);
        token.setActionCallback(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken iMqttToken) {
                Log.d(TAG, "UnSubscribe Successfully " + topic);
            }

            @Override
            public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                Log.e(TAG, "UnSubscribe Failed " + topic);
            }
        });
    }
}
