package com.example.gryffindor.sos;

import android.content.BroadcastReceiver;

        import android.content.BroadcastReceiver;
        import android.content.Context;
        import android.content.Intent;

public class StartFirebaseAtBoot extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, FirebaseBackgroundService.class);
        context.startService(serviceIntent);
    }
}