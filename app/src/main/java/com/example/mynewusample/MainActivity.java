package com.example.mynewusample;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
    private FirebaseUser user;
    private AlertDialog networkErrorDialog;

    private boolean emailVerified;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MaterialToolbar toolbar = (MaterialToolbar) findViewById(R.id.topAppBar);;
        setSupportActionBar(toolbar);

        mAuth = FirebaseAuth.getInstance();
        mStore = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();
        userID = user.getUid();

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

        Task<Void> userTask = mAuth.getCurrentUser().reload();
        userTask.addOnSuccessListener(new OnSuccessListener() {
            @Override
            public void onSuccess(Object o) {
                user = mAuth.getCurrentUser();
                emailVerified = user.isEmailVerified();
            }
        });

        AlertDialog.Builder networkErrorBuilder = new AlertDialog.Builder(this);
        networkErrorBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        networkErrorBuilder.setCancelable(true);
        networkErrorBuilder.setTitle("Network error").setMessage("There is a problem with your connection. Check your network settings.");
        networkErrorDialog = networkErrorBuilder.create();

        AlertDialog.Builder LogOutBuilder = new AlertDialog.Builder(this);
        LogOutBuilder.setPositiveButton("Sign out", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(MainActivity.this, AuthMainActivity.class);
                startActivity(intent);
                finish();
            }
        });
        LogOutBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        LogOutBuilder.setCancelable(true);
        LogOutBuilder.setTitle("Do you really want to sign out?").setMessage("");
        logOutDialog = LogOutBuilder.create();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Task<Void> userTask = mAuth.getCurrentUser().reload();
        userTask.addOnSuccessListener(new OnSuccessListener() {
            @Override
            public void onSuccess(Object o) {
                user = mAuth.getCurrentUser();
                emailVerified = user.isEmailVerified();
                if(!getIntent().hasExtra("FromRegistration") && !emailVerified){
                    Snackbar snackbar = Snackbar.make(getWindow().getDecorView(), "You need to verify your email.", Snackbar.LENGTH_LONG);
                    snackbar.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.white_tinted));
                    View view = snackbar.getView();
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();

                    Rect rectangle = new Rect();
                    Window window = getWindow();
                    window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
                    int statusBarHeight = rectangle.top;
                    params.gravity = Gravity.TOP;
                    int px = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            56,
                            getResources().getDisplayMetrics()
                    );
                    params.topMargin = px + statusBarHeight;

                    view.setLayoutParams(params);
                    snackbar.setAction(R.string.snackbar_action_name, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            user.sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    Toast.makeText(MainActivity.this, "Email verification link has been sent to you.", Toast.LENGTH_LONG).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    try{
                                        throw e;
                                    }
                                    catch (FirebaseNetworkException ex){
                                        networkErrorDialog.show();
                                    }
                                    catch (FirebaseTooManyRequestsException ex){
                                        Toast.makeText(MainActivity.this, "Email verification link has already been sent to you.", Toast.LENGTH_LONG).show();
                                    }
                                    catch (Exception ex){
                                        Toast.makeText(MainActivity.this, "There is an error with sending the email verification." + ex.getMessage() + ex.getClass().toString(), Toast.LENGTH_LONG).show();                                    }
                                }
                            });
                            snackbar.dismiss();
                        }
                    }).show();
                }
            }
        });
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