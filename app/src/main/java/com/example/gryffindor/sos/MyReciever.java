package com.example.gryffindor.sos;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MyReciever extends BroadcastReceiver {
    private static DatabaseReference CurrentRequests;
    private static DatabaseReference HistoryRequests;
    private static FirebaseAuth Auth;
    public static Service caller;
    static int counter;

    public static boolean wasScreenOn = true;

    public MyReciever() {

    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        FirebaseApp.initializeApp(context);
        CurrentRequests = FirebaseDatabase.getInstance().getReference("SOSCurrentRequests");
        HistoryRequests =  FirebaseDatabase.getInstance().getReference("SOSHistoryRequests");
        Auth = FirebaseAuth.getInstance();
        Log.e("LOB","onReceive");
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            // do whatever you need to do here
            wasScreenOn = false;
            Log.e("LOB","wasScreenOn"+wasScreenOn);
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {

            TimerTask timer = new TimerTask() {
                @Override
                public void run() {
                    MyReciever.counter = 2;
                }
            };

            Timer timer1 = new Timer();

            timer1.schedule(timer, 2000);

            counter--;

            if(counter<=0)
            SOSRequestInit("Help needed as soon as possible (Critical)");

            wasScreenOn = true;

        }else if(intent.getAction().equals(Intent.ACTION_USER_PRESENT)){
            Log.e("LOB","userpresent");
            Log.e("LOB","wasScreenOn"+wasScreenOn);
        }
    }


    public void SOSRequestInit(final String message)
    {
        if(Auth.getCurrentUser() != null) {
            try {
                FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(caller);
                mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(final Location location) {

                        if(location == null) return;


                        GeoFire geoFire = new GeoFire(CurrentRequests);
                        geoFire.setLocation(Auth.getUid(), new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {

                                DatabaseReference uidRef = CurrentRequests.child(Auth.getUid());
                                Date currentDate = new Date();
                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                                uidRef.child("message").setValue(message);
                                uidRef.child("uid").setValue(Auth.getUid());
                                uidRef.child("time").setValue(dateFormat.format(currentDate));
                            }
                        });


                    }
                });

                if(MainActivity.willSendMessages.isChecked()) {
                    SMS.setEmrgencyMessage(MainActivity.txtMessage.getText().toString());
                    SMS.startSendingSMS(MainActivity.thisActivity);
                }



            } catch (SecurityException e) {


            } catch (Exception e) {


            }

        }
    }


}