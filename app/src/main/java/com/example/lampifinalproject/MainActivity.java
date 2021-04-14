package com.example.lampifinalproject;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;

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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        context = getBaseContext();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void makeWeatherRequest(View view){
        Toast.makeText(getBaseContext(), "Makine request for weather", Toast.LENGTH_SHORT);
        // Instantiate the RequestQueue.
        System.out.println("Making request...");
        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="https://api.openweathermap.org/data/2.5/weather?q=Cleveland&appid=44f4936e6e18098c789b1ada6e7c6fbd";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try{
                            System.out.println("Temperature: " + response.getJSONObject("main").getDouble("temp"));
                            System.out.println("Humidity: " + response.getJSONObject("main").getInt("humidity"));
                            double temp = response.getJSONObject("main").getDouble("temp");
                            int humidity = response.getJSONObject("main").getInt("humidity");
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
        queue.add(jsonObjectRequest);
    }
}