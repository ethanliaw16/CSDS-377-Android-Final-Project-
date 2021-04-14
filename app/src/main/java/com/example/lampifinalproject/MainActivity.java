package com.example.lampifinalproject;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private Context context;
    private RequestQueue myQueue;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        context = getBaseContext();
        myQueue = Volley.newRequestQueue(this);
        super.onCreate(savedInstanceState);
        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        startActivity(intent);
        setContentView(R.layout.activity_main);
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
}