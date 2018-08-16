package com.example.gryffindor.sos;

import android.*;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.LocationCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class FirebaseBackgroundService extends Service {

    private static final String CHANNEL_ID = "SOS";
    private static DatabaseReference CurrentRequests;
    private static DatabaseReference HistoryRequests;
    private static FirebaseAuth Auth;
    private static int notificationId = 0;
    //in km
    public static double MAX_RADIUS_OF_NOTIFICATIONS = 100000;
    public static long MAX_TIME_DIFFERENCE = 15; //minutes

    private static Service thisService;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        thisService = this;
        FirebaseApp.initializeApp(this);
        CurrentRequests = FirebaseDatabase.getInstance().getReference("SOSCurrentRequests");
        HistoryRequests =  FirebaseDatabase.getInstance().getReference("SOSHistoryRequests");
        Auth = FirebaseAuth.getInstance();
        createNotificationChannel();
        CurrentRequests.addChildEventListener(new ChildEventListener() {
           @Override
           public void onChildAdded(@NonNull final DataSnapshot dataSnapshot, @Nullable String s) {
               try {
                   FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(thisService);
                   mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                       @Override
                       public void onSuccess(final Location locationHere) {
                           if(locationHere == null) return;
                           final GeoFire geoFire = new GeoFire(CurrentRequests);

                           geoFire.getLocation(dataSnapshot.getKey(), new LocationCallback() {
                               @Override
                               public void onLocationResult(final String key, final GeoLocation locationReq) {

                                   if(locationReq == null) return;

                                    CurrentRequests.child(key).child("time").addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot2) {

                                            String timeReceived  ="";

                                            try {

                                                timeReceived = dataSnapshot2.getValue(String.class);
                                                if(timeReceived != null)
                                                {
                                                    Date dateRecieved = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(timeReceived);
                                                    Date now = new Date();


                                                    long timeDifference = now.getTime() - dateRecieved.getTime();
                                                    long minutesDiff = TimeUnit.MINUTES.convert(timeDifference, TimeUnit.MILLISECONDS);


                                                    if(minutesDiff > MAX_TIME_DIFFERENCE)
                                                    {
                                                        transferSOSRequestToHistory(key);
                                                    }


                                                    else {
                                                        Location loc = new Location(locationHere);
                                                        loc.setLatitude(locationReq.latitude);
                                                        loc.setLongitude(locationReq.longitude);
                                                        double dist = locationHere.distanceTo(loc);
                                                        if (dist <= MAX_RADIUS_OF_NOTIFICATIONS && !Auth.getUid().equals(key)) {
                                                            postNotif("SOS", "" + locationReq.latitude + " , " + locationReq.longitude);
                                                        }
                                                    }


                                                }

                                                else return;

                                            }

                                            catch (Exception e)
                                            {
                                            }
                                            CurrentRequests.child(key).child("time").removeEventListener(this);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });

                               }

                               @Override
                               public void onCancelled(DatabaseError databaseError) {

                               }
                           });
                       }

                   });

               } catch (SecurityException e) {
               }

               catch (Exception e)
               {
               }
           }

           @Override
           public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

           }

           @Override
           public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

           }

           @Override
           public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

           }

           @Override
           public void onCancelled(@NonNull DatabaseError databaseError) {

           }
       });

    }

    private void postNotif(String textTitle, String textContent) {

        Intent intent = new Intent(this, MapActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);


        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(textTitle)
                .setContentText(textContent)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);


        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

// notificationId is a unique int for each notification that you must define
        notificationManager.notify(notificationId++, mBuilder.build());
    }


    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


    private double distance(double lat1, double lon1, double lat2, double lon2) {
        if(lat1 == lat2 && lon1 == lon2) return 0;
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        dist = dist * 1.609344;

        return (dist);
    }


    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    /*::  This function converts decimal degrees to radians             :*/
    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    /*::  This function converts radians to decimal degrees             :*/
    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    private static double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }




    public static void transferSOSRequestToHistory(final String keyDeleted)
    {
        final DatabaseReference refDeleted = CurrentRequests.child(keyDeleted);

        GeoFire geoFire = new GeoFire(CurrentRequests);

        geoFire.getLocation(keyDeleted, new LocationCallback() {
            @Override
            public void onLocationResult(String key, GeoLocation location) {
                if(location == null)
                {
                    return;
                }

                final Request request = new Request();
                request.setLatitude(location.latitude);
                request.setLongitude(location.longitude);

                CurrentRequests.child(keyDeleted).child("time").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        request.setTime(dataSnapshot.getValue(String.class));

                        CurrentRequests.child(keyDeleted).child("uid").addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                request.setUid(dataSnapshot.getValue(String.class));


                                CurrentRequests.child(keyDeleted).child("message").addValueEventListener(new ValueEventListener() {

                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        request.setMessage(dataSnapshot.getValue(String.class));


                                        refDeleted.removeValue(new DatabaseReference.CompletionListener() {

                                            @Override
                                            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                                                try {

                                                    GeoFire geoFire = new GeoFire(HistoryRequests);
                                                    geoFire.setLocation(generateRandomString(30), new GeoLocation(request.getLatitude(), request.getLongitude()), new GeoFire.CompletionListener() {
                                                        @Override
                                                        public void onComplete(String key, DatabaseError error) {
                                                            Log.d("key", key);
                                                            DatabaseReference newRef = HistoryRequests.child(key);
                                                            newRef.child("message").setValue(request.getMessage());
                                                            newRef.child("uid").setValue(request.getUid());
                                                            newRef.child("time").setValue(request.getTime());


                                                        }
                                                    });




                                                } catch (SecurityException e) {

                                                    Toast.makeText(thisService, "There's a permission problem with sending request", Toast.LENGTH_LONG).show();

                                                } catch (Exception e) {

                                                    Toast.makeText(thisService, "There's a problem with sending request", Toast.LENGTH_LONG).show();

                                                }




                                            }
                                        });

                                        CurrentRequests.child(keyDeleted).child("message").removeEventListener(this);


                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });

                                CurrentRequests.child(keyDeleted).child("uid").removeEventListener(this);


                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });

                        CurrentRequests.child(keyDeleted).child("time").removeEventListener(this);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }


                });



            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }



    private static String generateRandomString(int length) {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 10;
        Random random = new Random();
        StringBuilder buffer = new StringBuilder(targetStringLength);
        for (int i = 0; i < length; i++) {
            int randomLimitedInt = leftLimit + (int)
                    (random.nextFloat() * (rightLimit - leftLimit + 1));
            buffer.append((char) randomLimitedInt);
        }
        String generatedString = buffer.toString();
        return generatedString;
    }

}



