package com.example.gryffindor.sos;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

public class BackGround extends IntentService {

    static CountDownTimer cdt;
    static Notification notification;
    static IntentService thisService;
    public BackGround() {
        super("BackGround");
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        // Gets data from the incoming Intent
        String dataString = workIntent.getDataString();

        // Do work here, based on the contents of dataString

    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        thisService = this;
        Intent notificationIntent = new Intent(this, timer_task.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Timer")
                .setContentText("SOS timer is working..")
                .setContentIntent(pendingIntent).build();


        startForeground(1337, notification);

        return START_STICKY;
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
