package com.example.lampifinalproject;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;
import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity {
    private Context context;
    private RequestQueue myQueue;
    private boolean notificationsOn = false;
    public static String INTENT_ACTION_NOTIFICATION = "com.example.lampifinalproject.notification";
    public static String NOTIFICATION_APP = "app";
    public static String NOTIFICATION_SENDER = "sender";
    public static String NOTIFICATION_MESSAGE = "message";
    private String[] allowedApps = {"Gmail","Messenger","Snapchat"};
    protected MyReceiver mReceiver = new MyReceiver();
    protected TextView text;
    protected TextView subText;
    public Switch notificationToggle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        context = getBaseContext();
        myQueue = Volley.newRequestQueue(this);
        super.onCreate(savedInstanceState);

        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        startActivity(intent);

        setContentView(R.layout.activity_main);

        notificationToggle = (Switch) findViewById(R.id.notificationtoggle);
        notificationToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    System.out.println("Notifications on");
                    if (mReceiver == null) mReceiver = new MyReceiver();
                    registerReceiver(mReceiver, new IntentFilter(INTENT_ACTION_NOTIFICATION));
                } else {
                    System.out.println("Notifications off");
                    unregisterReceiver(mReceiver);
                }
            }
        });
        text = (TextView) findViewById(R.id.notificationtext);
        subText = (TextView) findViewById(R.id.notificationsubtext);
        text.setText("This is where the notifications be going");
    }

    @Override
    protected void OnDestroy(){
        
    }


    public void makeWeatherRequest(View view){
        Toast.makeText(getBaseContext(), "Makine request for weather", Toast.LENGTH_SHORT);
        // Instantiate the RequestQueue.
        System.out.println("Making request...");
        //RequestQueue queue = Volley.newRequestQueue(this);
        String url ="https://api.openweathermap.org/data/2.5/weather?q=Cleveland&appid=44f4936e6e18098c789b1ada6e7c6fbd";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try{
                    //System.out.println("Temperature: " + response.getJSONObject("main").getDouble("temp"));
                    //System.out.println("Humidity: " + response.getJSONObject("main").getInt("humidity"));
                    double temp = response.getJSONObject("main").getDouble("temp");
                    temp -= 273.15;
                    temp *= (9.0/5.0);
                    temp += 32;
                    int humidity = response.getJSONObject("main").getInt("humidity");
                    String ec2Url = "http://34.226.146.171:8000/update-weather";
                    JSONObject weatherPostData = new JSONObject();
                    System.out.println("Adding " + temp + " as temp");
                    weatherPostData.put("temp", temp);
                    weatherPostData.put("humidity", humidity);
                    JsonObjectRequest weatherPostRequest = new JsonObjectRequest(ec2Url, weatherPostData, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            System.out.println(response);
                        }
                        }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // TODO: Handle error
                            System.out.println("ERROR :( " + error.getMessage());
                        }
                    });
                    myQueue.add(weatherPostRequest);
                }
                catch (Exception e){
                    System.out.println(e.getMessage());
                }
            }
            }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO: Handle error
                System.out.println("ERROR :( " + error.getMessage());
            }
        });
        myQueue.add(jsonObjectRequest);
    }

    public void makeNotificationRequest(String app, String sender, String message){
        System.out.println("making notification request to api");
        JSONObject notificationPostData = new JSONObject();
        String notificationURL = "http://34.226.146.171:8000/update-notification";
        try{
            notificationPostData.put("app", app);
            notificationPostData.put("sender", sender);
            notificationPostData.put("message",message);
            JsonObjectRequest notificationRequest = new JsonObjectRequest(notificationURL, notificationPostData, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            });

            myQueue.add(notificationRequest);
        } catch (Exception e){
            System.out.println("Oop something went wrong");
        }

    }

    public class MyReceiver extends BroadcastReceiver {
        private String lastMessage = "";
        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("Broadcast Receiver - got notification");
            if (intent != null) {
                System.out.println("Broadcast Receiver - intent wasn't null");
                Bundle extras = intent.getExtras();
                System.out.println("Extras: " + intent.getExtras());
                String notificationApp = extras.getString(NOTIFICATION_APP);
                String sender = extras.getString(NOTIFICATION_SENDER);
                String message = extras.getString(NOTIFICATION_MESSAGE);

                //System.out.println("Title: " + notificationApp);
                //System.out.println("Content: " + sender);
                //System.out.println("SubContent: " + message);
                text.setText(sender);
                subText.setText(message);
                for(String app : allowedApps){
                    if(app.equals(notificationApp)){
                        //System.out.println("Last message was " + lastMessage + ", this message is " + message);
                        makeNotificationRequest(notificationApp, sender, message);
                    }
                }
                lastMessage = message;
            }

        }
    }
}