package com.example.gryffindor.sos;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

public class MainActivity extends AppCompatActivity {
    private DrawerLayout mDrawerLayout;
    private Button getHelpButton;
    public static Activity thisActivity;
    public static CheckBox willSendMessages;
    public static EditText txtMessage;
    public static Button btnBurger;
    private Button btnClose;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        thisActivity = this;

        getHelpButton = findViewById(R.id.help_button);

        mDrawerLayout = findViewById(R.id.drawer_layout);

        willSendMessages = findViewById(R.id.willSendMessages);

        txtMessage = findViewById(R.id.txtMessage);

        btnBurger = findViewById(R.id.burger_button);

        btnClose = findViewById(R.id.btnClose);

        FirebaseUtil.openRefrence(this);


        getHelpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String Message = txtMessage.getText().toString();
                if(Message==null)  FirebaseUtil.SOSRequestInit("");
                else
                FirebaseUtil.SOSRequestInit(Message);
            }
        });

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(FirebaseUtil.Auth.getCurrentUser() != null)
                FirebaseUtil.transferSOSRequestToHistory(FirebaseUtil.Auth.getUid());
            }
        });




        NavigationView navigationView = findViewById(R.id.nav_view);
        FirebaseUtil.startLocationService();
        startService(new Intent(getApplicationContext(), LockService.class));
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                item.setChecked(true);
                mDrawerLayout.closeDrawers();
                switch(item.getItemId())
                {
                    case R.id.nav_add_contacts:
                        startActivity(new Intent(getApplicationContext(),ContactsActivity.class));
                        break;
                    case R.id.nav_set_timer:
                        Intent intent = new Intent(MainActivity.this, timer_task.class);
                        startActivity(intent);
                        break;
                    case R.id.nav_map:
                            Intent intent2 = new Intent(MainActivity.this, HistoryActivity.class);
                            startActivity(intent2);
                        break;

                    case R.id.current_map:
                        Intent intent3 = new Intent(MainActivity.this, MapActivity.class);
                        startActivity(intent3);
                        break;

                    case R.id.logout_drawer_option:
                        FirebaseUtil.signOut();
                        break;
                }
                return true;
            }
        });

        btnBurger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDrawerLayout.openDrawer(Gravity.START);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.logout_menu_option:
                FirebaseUtil.signOut();
                return true;

                default:
                    return super.onOptionsItemSelected(item);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        FirebaseUtil.detachListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUtil.attachListener();

        checkLocationPermission();

    }

    public void checkLocationPermission() {

        if (FirebaseUtil.Auth.getCurrentUser() != null) {

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "You need to grant the app location permission to work correctly", Toast.LENGTH_LONG).show();
                return;
            }

        }
    }



}
