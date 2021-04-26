package com.example.lampifinalproject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private Context context;
    private RequestQueue myQueue;
    private boolean notificationsOn = false;
    private LatLng locationFromPicker;
    public static String INTENT_ACTION_NOTIFICATION = "com.example.lampifinalproject.notification";
    public static String NOTIFICATION_APP = "app";
    public static String NOTIFICATION_SENDER = "sender";
    public static String NOTIFICATION_MESSAGE = "message";
    public static final String WEATHER_API_TOKEN = "44f4936e6e18098c789b1ada6e7c6fbd";
    public static final String WEATHER_API_BASE_URL = "https://api.openweathermap.org/data/2.5/weather?lat=";
    private String[] allowedApps = {"Gmail","Messenger","Snapchat"};
    String[] cities = new String[]{"Cleveland,Ohio","Anchorage,Alaska","Lupin,Nunavut","Houston,Texas"};
    HashMap<String, LatLng> cityLocations = new HashMap<>();
    Queue<String> locationQueue = new ArrayDeque<>();
    private int currentlySelectedCity = 0;
    private List<String> allowedCities = new ArrayList<String>(Arrays.asList(cities));
    protected MyReceiver mReceiver = new MyReceiver();
    protected TextView text;
    protected TextView subText;
    protected TextView cityText;
    protected TextView cityWeatherText;
    protected Spinner locationPicker;
    public Switch notificationToggle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        context = getBaseContext();
        myQueue = Volley.newRequestQueue(this);
        locationFromPicker = new LatLng(41, -81);
        cityLocations.put("Default Location", locationFromPicker);
        cities = cityLocations.keySet().toArray(new String[cityLocations.keySet().size()]);
        super.onCreate(savedInstanceState);

        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        startActivity(intent);

        setContentView(R.layout.activity_main);
        Intent intentFromLocationPicker = getIntent();
        if(intentFromLocationPicker != null){
            double lat = intentFromLocationPicker.getDoubleExtra("lat", locationFromPicker.latitude);
            double lon = intentFromLocationPicker.getDoubleExtra("lon", locationFromPicker.longitude);
            String city = intentFromLocationPicker.getStringExtra("city");
            String state = intentFromLocationPicker.getStringExtra("state");
            String key = city + ", " + state;
            locationFromPicker = new LatLng(lat,lon);
            if(!cityLocations.containsKey(key) && city != null && state != null){

                cityLocations.put(key, locationFromPicker);
                locationQueue.add(city);
                if(locationQueue.size() > 7){
                    String keyToRemove = locationQueue.remove();
                    cityLocations.remove(keyToRemove);
                }
                cities = cityLocations.keySet().toArray(new String[cityLocations.keySet().size()]);
            }
            Log.i("Location", "New location from location picker is " + locationFromPicker.toString());
        }
        locationPicker = findViewById(R.id.simpleLocationPicker);
        ArrayAdapter<String> locationPickerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cities);
        locationPicker.setAdapter(locationPickerAdapter);
        locationPicker.setOnItemSelectedListener(this);

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

        text = findViewById(R.id.notificationtext);
        subText = findViewById(R.id.notificationsubtext);
        cityText = findViewById(R.id.cityForWeather);
        cityWeatherText = findViewById(R.id.tempAndHumidity);
        text.setText("This is where the notifications be going");
        cityWeatherText.setText("This is where the weather deets be going");
    }

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        Log.i("Location picker", "Picked " + parent.getItemAtPosition(pos).toString());
        currentlySelectedCity = pos;
        cityText.setText(cities[currentlySelectedCity]);
        // parent.getItemAtPosition(pos)
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Log.i("Location picker", "Nothing Selected");
    }

    public void goToLocationPicker(View view){
        Intent intent = new Intent(context, LocationPickerActivity.class);
        startActivity(intent);
    }

    public void makeWeatherRequest(View view){
        Toast.makeText(getBaseContext(), "Makine request for weather", Toast.LENGTH_SHORT);
        // Instantiate the RequestQueue.
        System.out.println("Making request for " + cities[currentlySelectedCity]);
        LatLng selectedLocation = cityLocations.get(cities[currentlySelectedCity]);

        //RequestQueue queue = Volley.newRequestQueue(this);
        String url =WEATHER_API_BASE_URL + selectedLocation.latitude + "&lon=" + selectedLocation.longitude + "&appid=" + WEATHER_API_TOKEN;
        //"https://api.openweathermap.org/data/2.5/weather?q=Cleveland&appid=44f4936e6e18098c789b1ada6e7c6fbd";
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
                    cityWeatherText.setText("Temperature: " + temp + " Humidity: " + humidity);
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
                if(message.length() > 140){
                    message = message.substring(0,140) + "...";
                }
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