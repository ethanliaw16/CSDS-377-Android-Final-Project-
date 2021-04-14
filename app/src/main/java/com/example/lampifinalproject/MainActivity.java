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
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class MainActivity extends AppCompatActivity {
    private Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        context = getBaseContext();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void makeWeatherRequest(View view){
        Toast.makeText(context, "Makine request for weather", Toast.LENGTH_SHORT);
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="https://www.google.com";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        Toast.makeText(context, "Response is: "+ response.substring(0,500), Toast.LENGTH_SHORT);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(context, "That didnt work :(", Toast.LENGTH_SHORT);
            }
        });
        queue.add(stringRequest);
    }
}