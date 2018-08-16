package com.example.gryffindor.sos;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.gryffindor.sos.data.SOSContract;
import com.example.gryffindor.sos.data.SOSDbHelper;

public class SMS {

    private final static int MY_PERMISSIONS_REQUEST_SEND_SMS = 2;

    public static boolean emergency=false;
    public static String emrgencyMessage=null;



    public static void  SendSMS(String number,  String message , Activity activity)
    {

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(activity, new String [] {Manifest.permission.SEND_SMS},
                    MY_PERMISSIONS_REQUEST_SEND_SMS);
        }
        else
        {
            SmsManager sms = SmsManager.getDefault();


            sms.sendTextMessage(number, null, message, null, null);
        }

    }



    public static void startSendingSMS(Activity activity)
    {
        try {
            SOSDbHelper dbHelper = new SOSDbHelper(activity);
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            String[] projection = {

                    SOSContract.contactsEntry.COLUMN_CONTACT_PHONE,
            };
            String userID = FirebaseUtil.Auth.getUid();

            String[] selArgs = {userID};

            Cursor c = db.query(SOSContract.contactsEntry.TABLE_NAME, projection,
                    SOSContract.contactsEntry.COLUMN_CONTACT_REFERENCE_TO_USER + "=?", selArgs,
                    null, null, null);

            while (c.moveToNext()) {

                int phoneColumnIndex = c.getColumnIndex(SOSContract.contactsEntry.COLUMN_CONTACT_PHONE);
                String phone = c.getString(phoneColumnIndex);

                //send SMS
                if (phone != null && emrgencyMessage != null)
                    SMS.SendSMS(phone, emrgencyMessage, activity);

            }
            c.close();
        }
        catch (Exception e)
        {
            Toast.makeText(activity, "Sending Interrupted", Toast.LENGTH_LONG).show();
        }
    }

    public static void setEmrgencyMessage(String message)
    {
        if(emrgencyMessage != null)
            emrgencyMessage= message+  "\nI need help as soon as possible" +"\nI am at https://www.google.com/maps/place/"+
                    FirebaseUtil.lastKnownLatitude+"N+"
                    +FirebaseUtil.lastKnownLogitude+"E";
        else
            emrgencyMessage= "I need help as soon as possible" +
                    "\nI am at https://www.google.com/maps/place/"+
                    FirebaseUtil.lastKnownLatitude+"N+"
                    +FirebaseUtil.lastKnownLogitude+"E";

    }



}

