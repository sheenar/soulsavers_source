package com.creatrove.soulsavers;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;


public class GcmBroadcastReceiver extends WakefulBroadcastReceiver {

    protected static final String TAG = "SOULSAVERS_LOG";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG,
                "onReceive: notification received.");
        ComponentName comp = new ComponentName(context.getPackageName(),
                GCMNotificationIntentService.class.getName());
        startWakefulService(context, (intent.setComponent(comp)));
        setResultCode(Activity.RESULT_OK);
    }
}
