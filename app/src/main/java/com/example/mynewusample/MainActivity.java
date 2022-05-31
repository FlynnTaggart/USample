package com.example.mynewusample;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class MainActivity extends AppCompatActivity {

    private AlertDialog logOutDialog;

    private FirebaseFirestore mStore;
    private FirebaseAuth mAuth;
    private String userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MaterialToolbar toolbar = (MaterialToolbar) findViewById(R.id.topAppBar);;
        setSupportActionBar(toolbar);

        mAuth = FirebaseAuth.getInstance();
        mStore = FirebaseFirestore.getInstance();
        userID = mAuth.getCurrentUser().getUid();

        if(mAuth.getCurrentUser() != null) {
            DocumentReference documentRef = mStore.collection("users").document(userID);
            documentRef.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                    try {
                        toolbar.setTitle(value.getString("nickname") + "'s samples");
                    }
                    catch (NullPointerException e){

                    }
                }
            });
        }

        FloatingActionButton fab = findViewById(R.id.floating_action_button);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SampleUploadActivity.class);
                startActivity(intent);
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton("Sign out", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(MainActivity.this, AuthMainActivity.class);
                startActivity(intent);
                finish();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.setCancelable(true);
        builder.setTitle("Do you really want to sign out?").setMessage("");
        logOutDialog = builder.create();
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.top_app_bar_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionLogout:

                logOutDialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}