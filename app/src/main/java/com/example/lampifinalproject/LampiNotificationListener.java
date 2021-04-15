package com.example.lampifinalproject;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.RequiresApi;

@SuppressLint("OverrideAbstract")
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class LampiNotificationListener extends NotificationListenerService {

    @Override
    public void onCreate(){
        System.out.println("Listener - oncreate called.");
        super.onCreate();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Notification mNotification=sbn.getNotification();
        System.out.println("Got a notification on the listener.");
        if (mNotification!=null){
            Bundle extras = mNotification.extras;
            System.out.println(mNotification.extras.toString());
            String notificationTitle = mNotification.extras.getString(Notification.EXTRA_TITLE);
            CharSequence foo = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
            String notificationText = "";
            if(foo != null){
                notificationText = foo.toString();
            }
            CharSequence bar = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
            String notificationSubText = "";
            if(bar != null){
                notificationSubText = bar.toString();
            }
            Intent intent = new Intent(MainActivity.INTENT_ACTION_NOTIFICATION);//
            intent.putExtra("NOTIFICATION_TITLE", notificationTitle);
            intent.putExtra("NOTIFICATION_TEXT", notificationText);
            intent.putExtra("NOTIFICATION_SUB_TEXT", notificationSubText);//, notificationText, notificationSubText);
            sendBroadcast(intent);


        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        //..............
    }
}
