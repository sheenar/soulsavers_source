package com.creatrove.soulsavers;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;


public class GCMNotificationIntentService extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    public GCMNotificationIntentService() {
        super("GcmIntentService");
    }

    public static final String TAG = "GCMNotificationIntentService";

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent "/*+intent.getDataString()*/);
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);

        String messageType = gcm.getMessageType(intent);

        Log.d(TAG, "onHandleIntent message type = "+messageType);
        if (extras != null) {
            if (!extras.isEmpty()) {
                if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR
                        .equals(messageType)) {
                    sendNotification("Send error: " + extras.toString());
                } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED
                        .equals(messageType)) {
                    sendNotification("Deleted messages on server: "
                            + extras.toString());
                } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE
                        .equals(messageType)) {
                     if("CHAT".equals(extras.get("SM"))){
                        Log.d(TAG, "onHandleIntent - CHAT ");
                        Intent chatIntent = new Intent("com.creatrove.soulsavers.sosmessage");
                        chatIntent.putExtra("CHATMESSAGE",extras.get("CHATMESSAGE").toString());
                        sendBroadcast(chatIntent);
                        sendNotification(extras.get("CHATMESSAGE").toString()); //sheena:added
                    }
                    Log.d(TAG, "SERVER_MESSAGE: " + extras.toString());

                }
            }
            else
            {
                Log.d(TAG, "onHandleIntent extras empty");
            }
        }
        else {
            Log.d(TAG, "onHandleIntent extras null");
        }
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void sendNotification(String msg) {
        Log.d(TAG, "Preparing to send notification...: " + msg);
        mNotificationManager = (NotificationManager) this
                .getSystemService(Context.NOTIFICATION_SERVICE);

        Intent notifyIntent = new Intent(this, NotificationActivity.class);
        notifyIntent.putExtra("title_notification", msg);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                this).setSmallIcon(R.drawable.gcm_cloud)
                .setContentTitle("SOS Notification Received")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(msg))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                .setContentText(msg)
                .setTicker("SOS !!!!");

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
        Log.d(TAG, "Notification sent successfully.");
    }


}

