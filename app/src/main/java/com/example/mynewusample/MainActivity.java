package com.example.mynewusample;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.mynewusample.model.SampleStructure;
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
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private AlertDialog logOutDialog;

    private FirebaseFirestore mStore;
    private FirebaseAuth mAuth;
    private String userID;
    private FirebaseUser user;
    private StorageReference mStorageRef;
    private ProgressBar progressBar;

    private AlertDialog networkErrorDialog;
    private AlertDialog accessPermissionDialog;

    private boolean emailVerified;

    private RecyclerView recyclerViewSamples;
    private List<SampleStructure> samples;
    private SamplesAdapter samplesAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MaterialToolbar toolbar = (MaterialToolbar) findViewById(R.id.topAppBar);

        setSupportActionBar(toolbar);

        mAuth = FirebaseAuth.getInstance();
        mStore = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();
        userID = user.getUid();
        mStorageRef = FirebaseStorage.getInstance().getReference();

        if (mAuth.getCurrentUser() != null) {
            DocumentReference documentRef = mStore.collection("users").document(userID);
            documentRef.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                    try {
                        toolbar.setTitle(value.getString("nickname") + "'s samples");
                    } catch (NullPointerException e) {

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

        progressBar = findViewById(R.id.progressBar);

        recyclerViewSamples = findViewById(R.id.recyclerViewSamples);
        recyclerViewSamples.setHasFixedSize(true);
        recyclerViewSamples.setLayoutManager(new LinearLayoutManager(this));

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setRefreshing(true);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                newSampleListener();
            }
        });

        samples = new ArrayList<SampleStructure>();
        samplesAdapter = new SamplesAdapter(MainActivity.this, samples);
        recyclerViewSamples.setAdapter(samplesAdapter);

        recyclerViewSamples.addOnItemTouchListener(new RecyclerItemClickListener(MainActivity.this,
                recyclerViewSamples, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Intent intent = new Intent(MainActivity.this, SampleListenActivity.class);
                SampleStructure sample = samples.get(position);
                intent.putExtra("sampleName", sample.getSampleName());
                intent.putExtra("sampleLink", sample.getSampleLink());
                intent.putExtra("fileName", sample.getFileName());
                intent.putExtra("sampleCoverLink", sample.getSampleCoverLink());
                intent.putExtra("note", sample.getNote());
                startActivity(intent);
            }

            @Override
            public void onLongItemClick(View view, int position) {

            }
        }));

        newSampleListener();

        SwipeToDeleteCallback swipeToDeleteCallback = new SwipeToDeleteCallback(this) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
                final int position = viewHolder.getAdapterPosition();
                final SampleStructure item = samplesAdapter.getData().get(position);

                int fullPosition = samplesAdapter.removeItem(position);

                Snackbar snackbar = Snackbar
                        .make(findViewById(R.id.mainContent), "Sample was removed.", Snackbar.LENGTH_LONG);
                snackbar.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.white_tinted));

                snackbar.setAction("Undo", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        samplesAdapter.restoreItem(item, position, fullPosition);
                        recyclerViewSamples.scrollToPosition(position);
                    }
                });

                snackbar.setAnchorView(fab);

                snackbar.addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        if (event != Snackbar.Callback.DISMISS_EVENT_ACTION){
                            DocumentReference documentRef = mStore.collection("users").document(userID)
                                    .collection("samples").document(item.getSampleName());
                            documentRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                final StorageReference sampleRef = mStorageRef.child("users/" + userID + "/samples/" + item.getFileName());
                                if (!item.getSampleCoverLink().equals("NONE")) {
                                    final StorageReference coverRef = mStorageRef.child("users/" + userID + "/covers/" + item.getFileName() + ".jpg");
                                    coverRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            sampleRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void unused) {

                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    onSampleDeleteError(e);
                                                }
                                            });
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            onSampleDeleteError(e);
                                        }
                                    });
                                } else {
                                    sampleRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {

                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            onSampleDeleteError(e);
                                        }
                                    });
                                }
                            }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    onSampleDeleteError(e);
                                }
                            });
                        }
                    }
                    @Override
                    public void onShown(Snackbar snackbar) { }
                });
                snackbar.show();
            }
        };

        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeToDeleteCallback);
        itemTouchhelper.attachToRecyclerView(recyclerViewSamples);

        createNetworkErrorDialog();
        createLogOutDialog();
        createAccessPermissionDialog();
    }

    private void createAccessPermissionDialog() {
        AlertDialog.Builder LogOutBuilder = new AlertDialog.Builder(this);
        LogOutBuilder.setPositiveButton("Give access", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Intent permissionIntent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(permissionIntent);
            }
        });
        LogOutBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        LogOutBuilder.setCancelable(true);
        LogOutBuilder.setTitle("Files access").setMessage("For correct working USample requires all files access.");
        accessPermissionDialog = LogOutBuilder.create();
    }

    private void createLogOutDialog() {
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

    private void createNetworkErrorDialog() {
        AlertDialog.Builder networkErrorBuilder = new AlertDialog.Builder(this);
        networkErrorBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        networkErrorBuilder.setCancelable(true);
        networkErrorBuilder.setTitle("Network error").setMessage("There is a problem with your connection. Check your network settings.");
        networkErrorDialog = networkErrorBuilder.create();
    }

    public void onSampleDeleteError(Exception e) {
        try {
            throw e;
        } catch (FirebaseNetworkException ex) {
            networkErrorDialog.show();
        } catch (FirebaseTooManyRequestsException ex) {
            Toast.makeText(MainActivity.this, "Too many requests.", Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            Toast.makeText(MainActivity.this, "There is an error with deleting the sample."
                    + ex.getMessage() + ex.getClass().toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void newSampleListener() {
        if (mAuth.getCurrentUser() != null) {
            mStore.collection("users").document(userID)
                .collection("samples").orderBy("sampleName", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            swipeRefreshLayout.setRefreshing(false);
                            Log.e("SampleList", "Error: " + error.getMessage() + " " + error.getClass().toString());
                        }
                        if(mAuth.getCurrentUser() != null) {
                            mStore.clearPersistence();
                            boolean dataChanged = false;
                            for (DocumentChange documentChange : value.getDocumentChanges()) {
                                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                                    if (!sampleIsAdded(documentChange.getDocument().toObject(SampleStructure.class))) {
                                        samples.add(documentChange.getDocument().toObject(SampleStructure.class));
                                        dataChanged = true;
                                    }
                                }
                                if (documentChange.getType() == DocumentChange.Type.REMOVED) {
                                    if (sampleIsAdded(documentChange.getDocument().toObject(SampleStructure.class))) {
                                        samples.remove(findSampleInListByName(documentChange.getDocument().toObject(SampleStructure.class).getSampleName()));
                                        dataChanged = true;
                                    }
                                }
                            }
                            if (dataChanged) {
                                samples.sort(new Comparator<SampleStructure>() {
                                    @Override
                                    public int compare(SampleStructure s1, SampleStructure s2) {
                                        return s1.getSampleName().compareTo(s2.getSampleName());
                                    }
                                });
                                samplesAdapter.notifyDataSetChanged();
                                samplesAdapter.updateFullList();
                            }
                        }
                        swipeRefreshLayout.setRefreshing(false);
                    }
            });
        }
    }

    private int findSampleInListByName(String sampleName) {
        for(int i = 0; i < samples.size(); i++){
            if(samples.get(i).getSampleName().equals(sampleName)){
                return i;
            }
        }
        return -1;
    }

    private boolean sampleIsAdded(SampleStructure sample) {
        for (SampleStructure i : samples) {
            if (i.getSampleName().equals(sample.getSampleName())) {
                return true;
            }
        }
        return false;
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
                if (!getIntent().hasExtra("FromRegistration") && !emailVerified) {
                    Snackbar snackbar = Snackbar.make(MainActivity.this.findViewById(R.id.mainContent), "You need to verify your email.", Snackbar.LENGTH_LONG);
                    snackbar.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.white_tinted));

                    View view = snackbar.getView();
                    CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) view.getLayoutParams();
                    params.gravity = Gravity.TOP;
                    int px = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            56,
                            getResources().getDisplayMetrics()
                    );
                    params.topMargin = px;
                    view.setLayoutParams(params);

                    snackbar.setAnchorView(findViewById(R.id.floating_action_button));

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
                                    try {
                                        throw e;
                                    } catch (FirebaseNetworkException ex) {
                                        networkErrorDialog.show();
                                    } catch (FirebaseTooManyRequestsException ex) {
                                        Toast.makeText(MainActivity.this, "Email verification link has already been sent to you.", Toast.LENGTH_LONG).show();
                                    } catch (Exception ex) {
                                        Toast.makeText(MainActivity.this,
                                                "There is an error with sending the email verification."
                                                        + ex.getMessage() + ex.getClass().toString(), Toast.LENGTH_LONG).show();
                                    }
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
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if(!Environment.isExternalStorageManager()){
                accessPermissionDialog.show();
            } else {
                accessPermissionDialog.dismiss();
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View view = getCurrentFocus();
            if (view instanceof EditText) {
                Rect outRect = new Rect();
                view.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    view.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.top_app_bar_main, menu);

        MenuItem menuItemSearch = menu.findItem(R.id.actionSearch);
        SearchView searchView = (SearchView) menuItemSearch.getActionView();

        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);
        searchView.setBackgroundColor(Color.parseColor("#00000000"));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                samplesAdapter.getFilter().filter(s);
                return false;
            }
        });
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