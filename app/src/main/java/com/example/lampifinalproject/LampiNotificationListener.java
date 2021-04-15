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

            Intent intent = new Intent(MainActivity.INTENT_ACTION_NOTIFICATION);//
            intent.putExtras(mNotification.extras);
            Log.v("INFO", extras.toString());
            sendBroadcast(intent);

            Notification.Action[] mActions = mNotification.actions;
            if (mActions!=null){
                for (Notification.Action mAction:mActions){
                    int icon=mAction.icon;
                    CharSequence actionTitle=mAction.title;
                    PendingIntent pendingIntent=mAction.actionIntent;
                }
            }
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        //..............
    }
}
