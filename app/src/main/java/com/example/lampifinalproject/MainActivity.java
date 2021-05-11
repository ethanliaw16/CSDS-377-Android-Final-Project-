package com.example.lampifinalproject;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
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

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
    private int mInterval = 5000; // 5 seconds by default, can be changed later
    private Handler mWeatherUpdateHandler;
    private String mDeviceId = "";
    protected MyReceiver mReceiver = new MyReceiver();
    protected TextView titleText;
    protected Button deviceIdButton;
    protected TextView text;
    protected TextView subText;
    protected TextView cityText;
    protected TextView cityWeatherText;
    protected Spinner locationPicker;
    private static SurfaceView motionDetectionView = null;
    private static SurfaceHolder motionDetectionViewHolder = null;
    public Switch notificationToggle;
    public Switch weatherToggle;
    public Switch motionDetectionToggle;
    private static Camera camera = null;
    private static boolean inPreview = false;
    private static long mReferenceTime = 0;
    private static MotionDetector detector;
    private static boolean notifyLampOfOnMotion;
    private static volatile AtomicBoolean processing = new AtomicBoolean(false);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        context = getBaseContext();
        detector = new MotionDetector();
        myQueue = Volley.newRequestQueue(this);
        locationFromPicker = new LatLng(41, -81);
        cityLocations.put("Default Location", locationFromPicker);
        cities = cityLocations.keySet().toArray(new String[cityLocations.keySet().size()]);
        super.onCreate(savedInstanceState);
        ComponentName cn = new ComponentName(context, LampiNotificationListener.class);
        String flattenedComponents = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
        final boolean notificationListenerEnabled = flattenedComponents != null && flattenedComponents.contains(cn.flattenToString());

        if(!notificationListenerEnabled){
            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            startActivity(intent);
        }
        
        setContentView(R.layout.activity_main);

        motionDetectionView = (SurfaceView) findViewById(R.id.motiondetectionview);
        motionDetectionViewHolder = motionDetectionView.getHolder();
        motionDetectionViewHolder.addCallback(surfaceCallback);
        //motionDetectionViewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        locationPicker = findViewById(R.id.simpleLocationPicker);

        notificationToggle = (Switch) findViewById(R.id.notificationtoggle);
        notificationToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Intent intent = new Intent(MainActivity.this, LampiNotificationListener.class);
                if (isChecked) {
                    System.out.println("Notifications on");
                    if (mReceiver == null) mReceiver = new MyReceiver();
                    registerReceiver(mReceiver, new IntentFilter(INTENT_ACTION_NOTIFICATION));
                    startService(intent);
                } else {
                    System.out.println("Notifications off");
                    unregisterReceiver(mReceiver);
                    stopService(intent);
                }
            }
        });

        weatherToggle = (Switch) findViewById(R.id.weathertoggle);
        weatherToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    System.out.println("Weather on");
                    startWeatherMode();
                } else {
                    System.out.println("Weather off");
                    stopWeatherMode();
                }
            }
        });

        motionDetectionToggle = (Switch) findViewById(R.id.motionsensortoggle);
        motionDetectionToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(mDeviceId.isEmpty()){
                    Toast.makeText(getBaseContext(), "Device ID not set - Motion Detection may not work properly", Toast.LENGTH_LONG).show();
                }
                notifyLampOfOnMotion = isChecked;
            }
        });

        titleText = findViewById(R.id.titletext);
        deviceIdButton = findViewById(R.id.setdeviceidbutton);
        text = findViewById(R.id.notificationtext);
        subText = findViewById(R.id.notificationsubtext);
        cityText = findViewById(R.id.cityForWeather);
        cityWeatherText = findViewById(R.id.tempAndHumidity);
        deviceIdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDeviceId();
            }
        });
        subText.setText("This is where the notifications be going");
        cityWeatherText.setText("This is where the weather deets be going");

        final PackageManager pm = getPackageManager();
        //get a list of installed apps.
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo packageInfo : packages) {
            //Log.i("Application", "Installed package :" + packageInfo.packageName);
        }

        mWeatherUpdateHandler = new Handler();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //stop weather service
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        savedInstanceState.putBoolean("MyBoolean", true);
        savedInstanceState.putDouble("myDouble", 1.9);
        savedInstanceState.putInt("MyInt", 1);
        savedInstanceState.putString("device", mDeviceId);
        // etc.
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        System.out.println("Device ID restored");
        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.
        boolean myBoolean = savedInstanceState.getBoolean("MyBoolean");
        double myDouble = savedInstanceState.getDouble("myDouble");
        int myInt = savedInstanceState.getInt("MyInt");
        System.out.println(myDouble + " " + myInt);
        mDeviceId= savedInstanceState.getString("device");
    }

    @Override
    public void onPause() {
        super.onPause();
        stopMotionDetection();
        putIntentStuff();
    }

    @Override
    public void onStop(){
        super.onStop();
        putIntentStuff();
    }

    public void putIntentStuff(){
        getIntent().putExtra("device", mDeviceId);
    }

    public void restoreStateFromIntent(){
        Intent previous = getIntent();
        if(previous != null){
            String id = previous.getStringExtra("device");
            if(id != null){
                System.out.println("Got the device, its " + id);
                setDeviceIdTo(id);
            }

            double lat = previous.getDoubleExtra("lat", locationFromPicker.latitude);
            double lon = previous.getDoubleExtra("lon", locationFromPicker.longitude);
            String city = previous.getStringExtra("city");
            String state = previous.getStringExtra("state");
            String key = city + ", " + state;
            locationFromPicker = new LatLng(lat,lon);
            if(!cityLocations.containsKey(key) && city != null && state != null) {

                cityLocations.put(key, locationFromPicker);
                locationQueue.add(city);
                if (locationQueue.size() > 7) {
                    String keyToRemove = locationQueue.remove();
                    cityLocations.remove(keyToRemove);
                }
                cities = cityLocations.keySet().toArray(new String[cityLocations.keySet().size()]);
            }

            ArrayAdapter<String> locationPickerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cities);
            locationPicker.setAdapter(locationPickerAdapter);
            locationPicker.setOnItemSelectedListener(this);
        }


    }


    @Override
    public void onResume() {
        super.onResume();
        startMotionDetection();
        restoreStateFromIntent();
        if(mDeviceId.isEmpty()){
            setDeviceId();
        }
    }

    public void startMotionDetection(){
        if(camera != null){
            System.out.println("Gotta release");
            camera.release();
            camera = null;
        }
        try{
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    android.Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                System.out.println("Opening camera...");
                camera = Camera.open();
                camera.startPreview();

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        1);
            }

        }catch(Exception e){
            System.out.println("Camera error");
            System.out.println(e.getMessage());
        }
    }

    public void stopMotionDetection(){
        if(camera != null){
            camera.setPreviewCallback(null);
            if (inPreview) camera.stopPreview();
            inPreview = false;
            camera.release();
            camera = null;
        }
    }

    public void setDeviceIdTo(String device){
        mDeviceId = device;
        deviceIdButton.setText("Change Device ID");
        titleText.setText("LAMPI " + device);
        System.out.println("showing valid toast");
        Toast.makeText(getBaseContext(), "Device ID Set Successfully", Toast.LENGTH_SHORT).show();
    }

    public void setDeviceId(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter your Lamp Device ID");

        // Set up the input
        final EditText deviceIdInput = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        deviceIdInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_TEXT);
        builder.setView(deviceIdInput);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String inputDeviceId = deviceIdInput.getText().toString();
                if(!inputDeviceId.isEmpty()){
                    System.out.println(inputDeviceId + " was given as input, validating...");
                    makeDeviceValidationRequest(inputDeviceId);
                }

            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();

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
        stopWeatherMode();
        notifyLampOfOnMotion = false;
        startActivity(intent);
    }

    public void makeDeviceValidationRequest(String device){
        System.out.println("validating device " + device);
        String deviceValidateURL = "http://34.226.146.171:8000/verify-device-id";
        JSONObject deviceValidationData = new JSONObject();
        try{
            deviceValidationData.put("device", device);
            JsonObjectRequest deviceValidationRequest = new JsonObjectRequest(deviceValidateURL, deviceValidationData, new Response.Listener<JSONObject>(){
                @Override
                public void onResponse(JSONObject response) {
                    System.out.println("Got a response");
                    System.out.println(response.toString());
                    try{
                        boolean validDevice = response.getBoolean("valid");
                        System.out.println("result boolean: " + validDevice);
                        if(validDevice){
                            setDeviceIdTo(device);
                        }
                        else{
                            System.out.println("showing invalid toast");
                            Toast.makeText(getBaseContext(), "Unable to Validate Device ID", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e){
                        System.out.println("Exception with jsoning");
                        System.out.println(e.getMessage());
                    }

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    System.out.println("Oop response was error");
                }
            });

            myQueue.add(deviceValidationRequest);
        } catch (Exception e){
            System.out.println("Oop something went wrong");
        }

    }

    public void makeNotificationRequest(String app, String sender, String message){
        System.out.println("making notification request to api");
        JSONObject notificationPostData = new JSONObject();
        String notificationURL = "http://34.226.146.171:8000/update-notification";
        try{
            if(mDeviceId.isEmpty()){
                Toast.makeText(getBaseContext(), "Error updating notification data - Please set Device ID.", Toast.LENGTH_LONG).show();
            }
            else{
                notificationPostData.put("app", app);
                notificationPostData.put("sender", sender);
                notificationPostData.put("message",message);
                notificationPostData.put("device", mDeviceId);
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
            }

        } catch (Exception e){
            System.out.println("Oop something went wrong");
        }

    }

    public void makeMotionDetectionRequest(){
        String motionDetectionURL = "http://34.226.146.171:8000/motion-detected";
        JSONObject motionDetectionData = new JSONObject();
        try{
            if(mDeviceId.isEmpty()){
                Toast.makeText(getBaseContext(), "Error - Invalid Device ID", Toast.LENGTH_SHORT).show();
            }
            else{
                motionDetectionData.put("device", mDeviceId);
                JsonObjectRequest motionDetectionRequest = new JsonObjectRequest(motionDetectionURL, motionDetectionData, new Response.Listener<JSONObject>(){
                    @Override
                    public void onResponse(JSONObject response) {
                        System.out.println("Got a response");
                        System.out.println(response.toString());
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        System.out.println("Oop response was error");
                    }
                });

                myQueue.add(motionDetectionRequest);
            }
        } catch (Exception e){
            System.out.println("Oop something went wrong");
        }
    }
    Runnable mWeatherUpdater = new Runnable() {
        @Override
        public void run() {
            try {
                if(mDeviceId.isEmpty()){
                    Toast.makeText(getBaseContext(), "Error updating weather data - Please set Device ID.", Toast.LENGTH_SHORT).show();
                }
                else{
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
                                weatherPostData.put("device", mDeviceId);
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
                    //updateStatus(); //this function can change value of mInterval.
                }

            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mWeatherUpdateHandler.postDelayed(mWeatherUpdater, mInterval);
            }
        }
    };

    void startWeatherMode() {
        mWeatherUpdater.run();
    }

    void stopWeatherMode() {
        mWeatherUpdateHandler.removeCallbacks(mWeatherUpdater);
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

    private PreviewCallback previewCallback = new PreviewCallback() {

        @Override
        public void onPreviewFrame(byte[] data, android.hardware.Camera cam) {
            if (data == null) return;
            Camera.Size size = cam.getParameters().getPreviewSize();
            if (size == null) return;

            DetectionThread thread = new DetectionThread(data, size.width, size.height);
            thread.start();
        }

    };

    private SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                System.out.println("In surfaceholder callback");
                camera.setPreviewDisplay(motionDetectionViewHolder);
                camera.setPreviewCallback(previewCallback);
            } catch (Throwable t) {
                Log.e("surface-callback", "Exception in setPreviewDisplay()", t);
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = getBestPreviewSize(width, height, parameters);
            if (size != null) {
                parameters.setPreviewSize(size.width, size.height);
                Log.d("surface-changed", "Using width=" + size.width + " height=" + size.height);
            }
            camera.setParameters(parameters);
            camera.startPreview();
            inPreview = true;
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            // Ignore
        }
    };

    private static Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea) result = size;
                }
            }
        }

        return result;
    }

    private final class DetectionThread extends Thread {

        private byte[] data;
        private int width;
        private int height;
        private boolean allowOneDiff = false;

        public DetectionThread(byte[] data, int width, int height) {
            this.data = data;
            this.width = width;
            this.height = height;
        }

        @Override
        public void run() {
            if (!processing.compareAndSet(false, true)) return;

            // Log.d(TAG, "BEGIN PROCESSING...");
            try {
                // Previous frame
                int[] pre = null;
                //if (Preferences.SAVE_PREVIOUS) pre = detector.getPrevious();

                // Current frame (with changes)
                // long bConversion = System.currentTimeMillis();
                int[] myImage = null;
                /**
                if (Preferences.USE_RGB) {
                    img = ImageProcessing.decodeYUV420SPtoRGB(data, width, height);
                } else {
                    img = ImageProcessing.decodeYUV420SPtoLuma(data, width, height);
                }**/
                myImage = MotionDetector.decodeYUV420SPtoRGB(data, width, height);
                // long aConversion = System.currentTimeMillis();
                // Log.d(TAG, "Converstion="+(aConversion-bConversion));

                // Current frame (without changes)
                int[] org = null;
                /**if (Preferences.SAVE_ORIGINAL && img != null)**/
                if(myImage != null){
                    org = myImage.clone();
                }


                if (myImage != null && detector.detect(myImage, width, height)) {

                    if(notifyLampOfOnMotion){
                        if(allowOneDiff){
                            allowOneDiff = false;
                        }
                        else{
                            //zToast.makeText(getBaseContext(), "Motion detected from main activity", Toast.LENGTH_SHORT).show();
                            System.out.println("Motion detected from main activity!!!");
                            makeMotionDetectionRequest();
                            Thread.sleep(3000);
                            allowOneDiff = true;
                        }
                    }
                    // The delay is necessary to avoid taking a picture while in
                    // the
                    // middle of taking another. This problem can causes some
                    // phones
                    // to reboot.
                    long now = System.currentTimeMillis();
                    if (now > (mReferenceTime + 10000)) {
                        mReferenceTime = now;

                        Bitmap previous = null;
                        /**
                        if (Preferences.SAVE_PREVIOUS && pre != null) {
                            if (Preferences.USE_RGB) previous = ImageProcessing.rgbToBitmap(pre, width, height);
                            else previous = ImageProcessing.lumaToGreyscale(pre, width, height);
                        }**/

                        Bitmap original = null;
                        if (org != null) /**Preferences.SAVE_ORIGINAL && **/{
                            /**if (Preferences.USE_RGB)**/
                            original = detector.rgbToBitmap(org, width, height);
                        }

                        Bitmap bitmap = null;
                        /**if (Preferences.SAVE_CHANGES)**/
                        bitmap = MotionDetector.rgbToBitmap(myImage, width, height);

                        Log.i("motion-detector", "Saving.. previous=" + previous + " original=" + original + " bitmap=" + bitmap);
                        Looper.prepare();
                        //new SavePhotoTask().execute(previous, original, bitmap);
                    } else {
                        Log.i("motion-detector", "Not taking picture because not enough time has passed since the creation of the Surface");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                processing.set(false);
            }
            // Log.d(TAG, "END PROCESSING...");

            processing.set(false);
        }
    }

    private static final class SavePhotoTask extends AsyncTask<Bitmap, Integer, Integer> {

        /**
         * {@inheritDoc}
         */
        @Override
        protected Integer doInBackground(Bitmap... data) {
            for (int i = 0; i < data.length; i++) {
                Bitmap bitmap = data[i];
                String name = String.valueOf(System.currentTimeMillis());
                if (bitmap != null) save(name, bitmap);
            }
            return 1;
        }

        private void save(String name, Bitmap bitmap) {
            File photo = new File(Environment.getExternalStorageDirectory(), name + ".jpg");
            if (photo.exists()) photo.delete();

            try {
                FileOutputStream fos = new FileOutputStream(photo.getPath());
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();
            } catch (Exception e) {
                Log.e("PictureDemo", "Exception in photoCallback", e);
            }
        }
    }
}