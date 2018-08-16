package com.example.gryffindor.sos;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.example.gryffindor.sos.data.SOSContract;
import com.example.gryffindor.sos.data.SOSDbHelper;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.LocationCallback;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;

public class FirebaseUtil {


    private static FirebaseUtil firebaseUtil;
    public static FirebaseDatabase Database;

    public static DatabaseReference CurrentRequestsRef;
    public static DatabaseReference HistoryRequestsRef;

    public static FirebaseAuth Auth;
    public static FirebaseAuth.AuthStateListener AuthListener;
    public static Activity caller;
    public static boolean isAdmin;
    public static String Token;
    private static int RC_SIGN_IN = 100;

    public static double lastKnownLatitude;
    public static double lastKnownLogitude;


    private FirebaseUtil()
    {}

    public static void openRefrence(Activity callerActivity)
    {
        if(firebaseUtil == null) {
            firebaseUtil = new FirebaseUtil();
            Database = FirebaseDatabase.getInstance();
            Auth = FirebaseAuth.getInstance();
            caller = callerActivity;
            AuthListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                    if (Auth.getCurrentUser() == null) {
                        FirebaseUtil.signIn();
                    }
                    else
                    {
                        String uid = Auth.getUid();
                        checkAdmin(uid);

                    }
                }
            };
        }

        CurrentRequestsRef = Database.getReference().child(Constants.CURRENT_REQUESTS_NODE_NAME);
        HistoryRequestsRef = Database.getReference().child(Constants.HISTORY_REQUESTS_NODE_NAME);
        if(FirebaseUtil.Auth.getUid()!=null)
            checkSQLite();

    }

    private static void signIn()
    {
        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

// Create and launch sign-in intent
        caller.startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN);


    }

    public static void signOut()
    {
        AuthUI.getInstance()
                .signOut(caller)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    public void onComplete(@NonNull Task<Void> task) {
                        attachListener();
                    }
                });

        FirebaseUtil.detachListener();
        isAdmin = false;
    }

    public static void attachListener()
    {
        Auth.addAuthStateListener(AuthListener);
    }

    public static void detachListener()
    {
        Auth.removeAuthStateListener(AuthListener);
    }

    private static void checkAdmin(String uid)
    {
        DatabaseReference administratorFirebaseRef = Database.getReference().child("administrators").child(uid);
        administratorFirebaseRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                isAdmin = true;

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


    public static void startLocationService()
    {
        Intent serviceIntent = new Intent(caller, FirebaseBackgroundService.class);
        caller.startService(serviceIntent);
    }


    public static void SOSRequestInit(final String message)
    {
        if(Auth.getCurrentUser() != null) {
            try {
                FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(caller);
                mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(final Location location) {

                        if(location == null) return;

                        lastKnownLatitude = location.getLatitude();
                        lastKnownLogitude = location.getLongitude();
                        GeoFire geoFire = new GeoFire(CurrentRequestsRef);
                        geoFire.setLocation(Auth.getUid(), new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {

                                DatabaseReference uidRef = CurrentRequestsRef.child(Auth.getUid());
                                Date currentDate = new Date();
                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                                uidRef.child("message").setValue(message);
                                uidRef.child("uid").setValue(Auth.getUid());
                                uidRef.child("time").setValue(dateFormat.format(currentDate));
                                Toast.makeText(caller, "Your request has been sent, hold on", Toast.LENGTH_LONG).show();
                            }
                        });


                    }
                });

                if(MainActivity.willSendMessages.isChecked()) {
                    SMS.setEmrgencyMessage(MainActivity.txtMessage.getText().toString());
                    SMS.startSendingSMS(MainActivity.thisActivity);
                }



            } catch (SecurityException e) {

                Toast.makeText(caller, "There's a permission problem with sending request", Toast.LENGTH_LONG).show();

            } catch (Exception e) {

                Toast.makeText(caller, "There's a problem with sending request", Toast.LENGTH_LONG).show();

            }

        }
    }


    public static void transferSOSRequestToHistory(final String keyDeleted)
    {
        final DatabaseReference refDeleted = CurrentRequestsRef.child(keyDeleted);

        GeoFire geoFire = new GeoFire(CurrentRequestsRef);

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

                CurrentRequestsRef.child(keyDeleted).child("time").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        request.setTime(dataSnapshot.getValue(String.class));

                        CurrentRequestsRef.child(keyDeleted).child("uid").addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                request.setUid(dataSnapshot.getValue(String.class));


                                CurrentRequestsRef.child(keyDeleted).child("message").addValueEventListener(new ValueEventListener() {

                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        request.setMessage(dataSnapshot.getValue(String.class));


                                        refDeleted.removeValue(new DatabaseReference.CompletionListener() {

                                            @Override
                                            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                                                try {

                                                    GeoFire geoFire = new GeoFire(HistoryRequestsRef);
                                                    geoFire.setLocation(generateRandomString(30), new GeoLocation(request.getLatitude(), request.getLongitude()), new GeoFire.CompletionListener() {
                                                        @Override
                                                        public void onComplete(String key, DatabaseError error) {
                                                            Log.d("key", key);
                                                            DatabaseReference newRef = HistoryRequestsRef.child(key);
                                                            newRef.child("message").setValue(request.getMessage());
                                                            newRef.child("uid").setValue(request.getUid());
                                                            newRef.child("time").setValue(request.getTime());


                                                        }
                                                    });




                                                } catch (SecurityException e) {

                                                    Toast.makeText(caller, "There's a permission problem with sending request", Toast.LENGTH_LONG).show();

                                                } catch (Exception e) {

                                                    Toast.makeText(caller, "There's a problem with sending request", Toast.LENGTH_LONG).show();

                                                }




                                            }
                                        });

                                        CurrentRequestsRef.child(keyDeleted).child("message").removeEventListener(this);


                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });

                                CurrentRequestsRef.child(keyDeleted).child("uid").removeEventListener(this);


                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });

                        CurrentRequestsRef.child(keyDeleted).child("time").removeEventListener(this);

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


    public static void SOSRequestThenTransfer(final int milliseconds, String message)
    {
        SOSRequestInit(message);
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                transferSOSRequestToHistory(Auth.getUid());
                Log.d("Transfered", "");
            }
        }, milliseconds);


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

    private static void checkSQLite(){

        String userID= FirebaseUtil.Auth.getUid();

        SOSDbHelper dbHelper=new SOSDbHelper(caller);
        SQLiteDatabase db=dbHelper.getReadableDatabase();

        String [] projection={
                SOSContract.usersEntry.COLUMN_USER_ID
        };
        String[] selArgs={userID};
        Cursor c=db.query(SOSContract.usersEntry.TABLE_NAME,projection,SOSContract.usersEntry.COLUMN_USER_ID+"=?",selArgs,
                null,null,null);

        if(c!=null) {
            c.close();
            return;
        }
        db=dbHelper.getWritableDatabase();
        ContentValues temp = new ContentValues();
        temp.put(SOSContract.usersEntry.COLUMN_USER_ID, userID);
        temp.put(SOSContract.usersEntry.COLUMN_USER_MESSAGE, "");
        long id=db.insert(SOSContract.usersEntry.TABLE_NAME,null,temp);
        if (id != -1) {
            System.out.println(id);
            //Toast.makeText(caller,"new User was add to the network",Toast.LENGTH_SHORT).show();

        } else {
            //Toast.makeText(caller,"an error occured",Toast.LENGTH_SHORT).show();
        }
    }


}
