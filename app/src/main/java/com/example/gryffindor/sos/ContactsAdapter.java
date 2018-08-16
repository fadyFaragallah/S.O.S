package com.example.gryffindor.sos;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.example.gryffindor.sos.data.SOSContract;
import com.example.gryffindor.sos.data.SOSDbHelper;

import java.util.ArrayList;

public class ContactsAdapter extends ArrayAdapter<Contact> {

    private  static  ContactsAdapter thisAdapter;
    public ContactsAdapter(Activity context, ArrayList<Contact> artists)
    {
        super(context,0,artists);
        thisAdapter = this;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        final Contact contact=getItem(position);
        View listItemView=convertView;

        if(listItemView==null)
        {
            listItemView= LayoutInflater.from(getContext()).inflate(R.layout.contact_item,parent,false);
        }

        TextView name=listItemView.findViewById(R.id.nameTextView);
        name.setText(contact.getName());

        TextView genre=listItemView.findViewById(R.id.phoneTextView);
        genre.setText(contact.getNumber());


        Button deleteButton=listItemView.findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteContact(contact.getName(),contact.getNumber());
                ContactsActivity.contacts.remove
                        (contact);
                thisAdapter.notifyDataSetChanged();
            }
        });
        return listItemView;
    }

    private void deleteContact(String name,String phone){
        ContactsActivity.deleteContact(name,phone);
    }
}
