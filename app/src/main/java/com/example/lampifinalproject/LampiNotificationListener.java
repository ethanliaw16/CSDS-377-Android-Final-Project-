package com.example.lampifinalproject;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.annotation.RequiresApi;

@SuppressLint("OverrideAbstract")
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class LampiNotificationListener extends NotificationListenerService {
    private String GOOGLE_APP_INFO = "com.google.android.gm";
    private String FB_APP_INFO = "com.facebook";
    private String SNAP_APP_INFO = "com.snapchat";
    @Override
    public void onCreate(){
        System.out.println("Listener - oncreate called.");
        super.onCreate();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String[] payloadFromGoogleNotification(Notification notification){
        String[] payload = new String[3];
        payload[0] = "Gmail";
        payload[1] = notification.extras.getString(Notification.EXTRA_TITLE);
        CharSequence message = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        payload[2] = "";
        if(message != null){
            payload[2] = message.toString();
        }
        return payload;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String[] payloadFromFbNotification(Notification notification){
        String[] payload = new String[3];
        payload[0] = "Messenger";
        payload[1] = notification.extras.getString(Notification.EXTRA_TITLE);
        CharSequence message = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
        payload[2] = "";
        if(message != null){
            payload[2] = message.toString();
        }
        return payload;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String[] payloadFromSnapNotification(Notification notification){
        String[] payload = new String[3];
        payload[0] = "Snapchat";
        payload[1] = notification.extras.getString(Notification.EXTRA_TITLE);
        payload[2] = "Open Snapchat to view snap";
        return payload;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Notification mNotification=sbn.getNotification();
        System.out.println("Got a notification on the listener.");
        if (mNotification!=null){
            Bundle extras = mNotification.extras;
            String allExtras = mNotification.extras.toString();
            String[] payload = {"Unknown Application", "","Open your phone to see notification"};
            if(allExtras.contains(GOOGLE_APP_INFO)){
                System.out.println("got google");
                payload = payloadFromGoogleNotification(mNotification);
            }
            else if (allExtras.contains(FB_APP_INFO)){
                System.out.println("got facebook");
                payload = payloadFromFbNotification(mNotification);
            }
            else if (allExtras.contains(SNAP_APP_INFO)){
                System.out.println("got snapchat");
                payload = payloadFromSnapNotification(mNotification);
            }
            System.out.println(allExtras);

            Intent intent = new Intent(MainActivity.INTENT_ACTION_NOTIFICATION);//
            intent.putExtra(MainActivity.NOTIFICATION_APP, payload[0]);
            intent.putExtra(MainActivity.NOTIFICATION_SENDER, payload[1]);
            intent.putExtra(MainActivity.NOTIFICATION_MESSAGE, payload[2]);//, notificationText, notificationSubText);
            sendBroadcast(intent);


        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        //..............
    }
}
