package com.example.gryffindor.sos.data;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SOSDbHelper extends SQLiteOpenHelper {
    public static final String LOG_TAG = SOSDbHelper.class.getSimpleName();

    /** Name of the database file */
    private static final String DATABASE_NAME = "users.db";

    /**
     * Database version. If you change the database schema, you must increment the database version.
     */
    private static final int DATABASE_VERSION = 1;


    public SOSDbHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * This is called when the database is created for the first time.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create a String that contains the SQL statement to create the users table
        String SQL_CREATE_USERS_TABLE =  "CREATE TABLE " + SOSContract.usersEntry.TABLE_NAME + " ("
                + SOSContract.usersEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + SOSContract.usersEntry.COLUMN_USER_ID + " TEXT NOT NULL, "
                + SOSContract.usersEntry.COLUMN_USER_MESSAGE + " TEXT NOT NULL );";

        String SQL_CREATE_CONTACTS_TABLE =  "CREATE TABLE " + SOSContract.contactsEntry.TABLE_NAME + " ("
                + SOSContract.contactsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + SOSContract.contactsEntry.COLUMN_CONTACT_PHONE + " TEXT NOT NULL, "
                + SOSContract.contactsEntry.COLUMN_CONTACT_NAME + " TEXT NOT NULL, "
                + SOSContract.contactsEntry.COLUMN_CONTACT_REFERENCE_TO_USER + " TEXT NOT NULL, "
                + "FOREIGN KEY("+SOSContract.contactsEntry.COLUMN_CONTACT_REFERENCE_TO_USER+")"
                + " REFERENCES " +SOSContract.usersEntry.TABLE_NAME+"("+SOSContract.usersEntry.COLUMN_USER_ID+")" +");";



        // Execute the SQL statement
        db.execSQL(SQL_CREATE_USERS_TABLE);
        db.execSQL(SQL_CREATE_CONTACTS_TABLE);
    }

    /**
     * This is called when the database needs to be upgraded.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // The database is still at version 1, so there's nothing to do be done here.

    }
}
