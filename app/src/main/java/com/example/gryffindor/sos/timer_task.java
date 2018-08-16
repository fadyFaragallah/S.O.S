package com.example.gryffindor.sos;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import java.util.Calendar;

import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.TimerTask;
import java.util.Timer;

public class timer_task extends AppCompatActivity {
    static Timer timer;
    TimerTask timerTask;
    static long time;
    TextView txtHours;
    TextView txtMinutes;
    TextView txtSeconds;
    static TextView txtShowTime;
    Button btnStart;
    Button btnCancel;

    static int notificationId = 0;
    static String CHANNEL_ID = "SOS_TIMER";



    final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        createNotificationChannel();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer_task);

         txtHours = findViewById(R.id.txtHours);
         txtMinutes =  findViewById(R.id.txtMinutes);
         txtSeconds =  findViewById(R.id.txtSeconds);
         txtShowTime =  findViewById(R.id.time_text_view);
         btnStart = findViewById(R.id.btnStart);

         btnStart.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 try
                 {
                     if(timer != null) stoptimertask();
                     showElements(false);
                     String hoursText = txtHours.getText().toString();
                     String minutesText = txtMinutes.getText().toString();
                     String secondsText = txtSeconds.getText().toString();
                     if(hoursText == null || hoursText.trim().equals(""))
                         hoursText = "0";

                     if(minutesText == null || minutesText.trim().equals(""))
                         minutesText = "0";

                     if(secondsText == null || secondsText.trim().equals(""))
                         secondsText = "0";

                     if(hoursText.equals("0") && minutesText.equals("0") && secondsText.equals("0"))
                         time = 600;

                     else {

                         int hours = Integer.parseInt(hoursText);
                         int minutes = Integer.parseInt(minutesText);
                         int seconds = Integer.parseInt(secondsText);
                         time = seconds + minutes * 60 + hours * 3600;
                     }

                     startTimer(0, 1000);

                     startService(new Intent(getBaseContext(), BackGround.class));


                 }

                 catch (Exception e)
                 {
                     showElements(true);
                     Toast.makeText(view.getContext(), "Enter the time correctly", Toast.LENGTH_LONG).show();
                 }
             }
         });

         btnCancel = findViewById(R.id.btnCancel);

         btnCancel.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 stoptimertask();
             }
         });

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void startTimer(int delay, int timeSlice) {

        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
        timer.schedule(timerTask, delay, timeSlice); //

    }

    public void stoptimertask() {

        //stop the timer, if it's not already null

        if (timer != null) {
            timer.cancel();

            timer = null;
        }
        txtShowTime.setText("00 : 00 : 00");
        time = 0;
        showElements(true);
        BackGround.thisService.stopForeground(true);
        BackGround.thisService.stopSelf();

    }

    public void initializeTimerTask() {
        timerTask = new TimerTask() {

            public void run() {
                //use a handler to run a toast that shows the current timestamp

                handler.post(new Runnable() {

                    public void run() {

                        time--;

                        if(time<=0) {
                            stoptimertask();
                            FirebaseUtil.SOSRequestInit("Help");
                        }
                            long tempTime = time;
                        long seconds = tempTime %60; tempTime/=60;
                        long minutes =  tempTime %60;  tempTime/=60;
                        long hours =tempTime %60;


                        txtShowTime.setText(hours + " : " + minutes + " : " + seconds );



                    }

                });

            }

        };
    }


    private void showElements(boolean willShow)
    {
        if(willShow) {
            txtHours.setVisibility(View.VISIBLE);
            txtMinutes.setVisibility(View.VISIBLE);
            txtSeconds.setVisibility(View.VISIBLE);
        }

        else
        {
            txtHours.setVisibility(View.GONE);
            txtMinutes.setVisibility(View.GONE);
            txtSeconds.setVisibility(View.GONE);
        }
    }


    private void postNotif(String textTitle, String textContent) {

        Intent intent = new Intent(this, timer_task.class);
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
        notificationManager.notify(notificationId, mBuilder.build());
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



}



