package com.example.gryffindor.sos;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gryffindor.sos.data.SOSContract;
import com.example.gryffindor.sos.data.SOSDbHelper;
import com.firebase.ui.auth.data.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class ContactsActivity extends AppCompatActivity {
    public static ArrayList<Contact> contacts;
    private ListView contactsListview;
    private Button addButton;
    public static ContactsAdapter adapter;
    private static final int RESULT_PICK_CONTACT = 1;
    private static Activity a;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        contactsListview=findViewById(R.id.contactsListView);
        a=this;
        addButton=findViewById(R.id.addButton);
        SOSDbHelper dbHelper=new SOSDbHelper(this);
        SQLiteDatabase db=dbHelper.getReadableDatabase();
        contacts=new ArrayList<Contact>();

        String [] projection={
                SOSContract.contactsEntry.COLUMN_CONTACT_NAME,
                SOSContract.contactsEntry.COLUMN_CONTACT_PHONE,
        };
        String userID= FirebaseUtil.Auth.getUid();

        String[] selArgs={userID};

        Cursor c=db.query(SOSContract.contactsEntry.TABLE_NAME,projection,SOSContract.contactsEntry.COLUMN_CONTACT_REFERENCE_TO_USER+"=?",selArgs,
                null,null,null);
        while(c.moveToNext()){
            int nameColumnIndex=c.getColumnIndex(SOSContract.contactsEntry.COLUMN_CONTACT_NAME);
            String name=c.getString(nameColumnIndex);

            int phoneColumnIndex=c.getColumnIndex(SOSContract.contactsEntry.COLUMN_CONTACT_PHONE);
            String phone=c.getString(phoneColumnIndex);
            Contact contact=new Contact(name,phone);
            contacts.add(contact);
        }
        c.close();
        adapter=new ContactsAdapter(this,contacts);
        contactsListview.setAdapter(adapter);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openContacts();
            }
        });
        contactsListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Button deleteButton=adapterView.findViewById(R.id.deleteButton);
                final String name=((TextView)view.findViewById(R.id.nameTextView)).getText().toString();
                final String phone=((TextView)view.findViewById(R.id.phoneTextView)).getText().toString();
                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        deleteContact(name,phone);
                        contacts.remove(contacts.get(contacts.indexOf(new Contact(name,phone))));
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        });

    }
    private void openContacts(){

        Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI);

        startActivityForResult(contactPickerIntent, RESULT_PICK_CONTACT);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // check whether the result is ok
        if (resultCode == RESULT_OK) {
            // Check for the request code, we might be usign multiple startActivityForReslut
            switch (requestCode) {
                case RESULT_PICK_CONTACT:
                    contactPicked(data);

                    break;
            }
        } else {
            Log.e("MainActivity", "Failed to pick contact");
        }
    }
    /**
     * Query the Uri and read contact details. Handle the picked contact data.
     * @param data
     */
    private void contactPicked(Intent data) {
        Cursor cursor = null;
        try {
            String phoneNo = null ;
            String name = null;
            // getData() method will have the Content Uri of the selected contact
            Uri uri = data.getData();
            //Query the content uri
            cursor = getContentResolver().query(uri, null, null, null, null);
            cursor.moveToFirst();
            // column index of the phone number
            int  phoneIndex =cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            // column index of the contact name
            int  nameIndex =cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            phoneNo = cursor.getString(phoneIndex);
            name = cursor.getString(nameIndex);
            saveContact(name,phoneNo);
            // Set the value to the textviews
            //textView1.setText(name);
            //textView2.setText(phoneNo);
        } catch (Exception e) {
            e.printStackTrace();
        }

        adapter.notifyDataSetChanged();
    }
    private void saveContact(String name,String phone){
        if(!ContactNotFound(name,phone))
            Toast.makeText(this,"this is already selected!",Toast.LENGTH_SHORT).show();
        else{
            SOSDbHelper dbHelper = new SOSDbHelper(this);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            String userID = FirebaseUtil.Auth.getUid();
            ContentValues temp = new ContentValues();
            temp.put(SOSContract.contactsEntry.COLUMN_CONTACT_NAME, name);
            temp.put(SOSContract.contactsEntry.COLUMN_CONTACT_PHONE, phone);
            temp.put(SOSContract.contactsEntry.COLUMN_CONTACT_REFERENCE_TO_USER, userID);
            long id = db.insert(SOSContract.contactsEntry.TABLE_NAME, null, temp);

            if (id != -1) {
                System.out.println(id);
                //Toast.makeText(caller,"new User was add to the network",Toast.LENGTH_SHORT).show();

            } else {
                //Toast.makeText(caller,"an error occured",Toast.LENGTH_SHORT).show();
            }
            contacts.add(new Contact(name,phone));
        }
    }
    private boolean ContactNotFound(String name,String phone){
        SOSDbHelper dbHelper=new SOSDbHelper(this);
        SQLiteDatabase db=dbHelper.getReadableDatabase();

        String [] projection={
                SOSContract.contactsEntry.COLUMN_CONTACT_NAME,
                SOSContract.contactsEntry.COLUMN_CONTACT_PHONE,
        };
        String userID= FirebaseUtil.Auth.getUid();

        String[] selArgs={userID};
        Cursor c=db.query(SOSContract.contactsEntry.TABLE_NAME,projection,SOSContract.contactsEntry.COLUMN_CONTACT_REFERENCE_TO_USER+"=?",selArgs,
                null,null,null);
        while(c.moveToNext()){
            int nameColumnIndex=c.getColumnIndex(SOSContract.contactsEntry.COLUMN_CONTACT_NAME);
            String nameTemp=c.getString(nameColumnIndex);

            int phoneColumnIndex=c.getColumnIndex(SOSContract.contactsEntry.COLUMN_CONTACT_PHONE);
            String phoneTemp=c.getString(phoneColumnIndex);
            if(name.equals(nameTemp) && phone.equals(phoneTemp))
                return false;
        }
        return true;
    }
    public static void deleteContact(String name,String phone){
        SOSDbHelper dbHelper=new SOSDbHelper(a);
        SQLiteDatabase db=dbHelper.getWritableDatabase();
        long id=db.delete(SOSContract.contactsEntry.TABLE_NAME, SOSContract.contactsEntry.COLUMN_CONTACT_NAME + "=\"" + name+"\"", null);
    }
}
