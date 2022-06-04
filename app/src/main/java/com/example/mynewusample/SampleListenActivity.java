package com.example.mynewusample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.mynewusample.model.SampleStructure;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

import jp.wasabeef.picasso.transformations.BlurTransformation;

public class SampleListenActivity extends AppCompatActivity {

    private EditText textFieldName;
    private TextInputLayout textFieldNote;
    private ImageView imageViewSampleCover;
    private ImageView imageViewEditName;
    private ProgressBar progressBar;

    private String sampleName = "";
    private String sampleLink = "";
    private String fileName = "";
    private String sampleCoverLink = "";
    private String note = "";

    private FirebaseFirestore mStore;
    private FirebaseAuth mAuth;
    private String userID;
    private FirebaseUser user;
    private StorageReference mStorageRef;
    private boolean canLoad = true;

    private AlertDialog networkErrorDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample_listen);
        MaterialToolbar toolbar = (MaterialToolbar) findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        textFieldName = findViewById(R.id.textFieldName);
        textFieldNote = findViewById(R.id.textFieldNote);
        imageViewSampleCover = findViewById(R.id.imageViewSampleCover);
//        imageViewEditName = findViewById(R.id.imageViewEditName);
        progressBar = findViewById(R.id.progressBar);

        mAuth = FirebaseAuth.getInstance();
        mStore = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();
        if (user != null) {
            userID = user.getUid();
        }
        mStorageRef = FirebaseStorage.getInstance().getReference();

        Intent intent = getIntent();
        if(intent != null){
            if(intent.hasExtra("sampleName")){
                sampleName = intent.getStringExtra("sampleName");
                textFieldName.setText(sampleName);
            }
            if(intent.hasExtra("note")){
                note = intent.getStringExtra("note");
                textFieldNote.getEditText().setText(note);
            }
            if(intent.hasExtra("sampleCoverLink")){
                sampleCoverLink = intent.getStringExtra("sampleCoverLink");
                Picasso.get().load(sampleCoverLink)
                        .transform(new BlurTransformation(SampleListenActivity.this, 5, 1))
                        .error(R.drawable.default_sample_cover_01)
                        .placeholder(R.drawable.default_sample_cover_01)
                        .into(imageViewSampleCover);
            }
            if(intent.hasExtra("sampleLink")){
                sampleLink = intent.getStringExtra("sampleLink");
            }
            if(intent.hasExtra("fileName")){
                fileName = intent.getStringExtra("fileName");
            }
        }

        textFieldName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if(!hasFocus){
                    textFieldName.setSelectAllOnFocus(false);
                    view.setFocusable(false);
                    view.setFocusableInTouchMode(false);
                }
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
        inflater.inflate(R.menu.top_app_bar_listen, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionUpdate:
                if(canLoad){
                    canLoad = false;
                    progressBar.setVisibility(View.VISIBLE);
                    DocumentReference documentRef = mStore.collection("users").document(userID)
                            .collection("samples").document(sampleName);
                    if(!sampleName.equals(textFieldName.getText().toString().trim())) {
                        documentRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                sampleName = textFieldName.getText().toString().trim();
                                note = textFieldNote.getEditText().getText().toString().trim();
                                SampleStructure sample = new SampleStructure(sampleName, sampleLink,
                                        fileName, sampleCoverLink, note);
                                DocumentReference documentRef = mStore.collection("users").document(userID)
                                        .collection("samples").document(sampleName);
                                documentRef.set(sample).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        showSampleUploadSuccessSnackbar();

                                        canLoad = true;
                                        progressBar.setVisibility(View.GONE);
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        onSampleUpdateFailure(e);
                                    }
                                });
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                onSampleUpdateFailure(e);
                            }
                        });
                    } else {
                        note = textFieldNote.getEditText().getText().toString().trim();
                        Map<String, Object> sample = new HashMap<>();
                        sample.put("note", note);
                        documentRef.update(sample).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                showSampleUploadSuccessSnackbar();
                                canLoad = true;
                                progressBar.setVisibility(View.GONE);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                onSampleUpdateFailure(e);
                            }
                        });
                    }
                }
                return true;
            case R.id.actionEdit:
                textFieldName.setSelectAllOnFocus(true);
                textFieldName.setFocusable(true);
                textFieldName.setFocusableInTouchMode(true);
                textFieldName.requestFocus();
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                InputMethodManager inputMethodManager =
                        (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.toggleSoftInputFromWindow(
                        findViewById(R.id.mainContent).getApplicationWindowToken(),
                        InputMethodManager.SHOW_FORCED, 0);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public void onSampleUpdateFailure(Exception e){
        try{
            throw e;
        }
        catch (FirebaseNetworkException ex){
            networkErrorDialog.show();
        }
        catch (FirebaseFirestoreException ex){
            if(ex.getCode() == FirebaseFirestoreException.Code.UNAVAILABLE){
                networkErrorDialog.show();
            } else {
                Toast.makeText(SampleListenActivity.this, "There is an error with updating the sample info", Toast.LENGTH_LONG).show();
                Log.e("SampleUpload", "Error: " + ex.getMessage() + " " + ex.getClass().toString());
            }
        }
        catch (Exception ex) {
            Toast.makeText(SampleListenActivity.this, "There is an error with updating the sample info", Toast.LENGTH_LONG).show();
            Log.e("SampleUpload", "Error: " + ex.getMessage() + " " + ex.getClass().toString());
        }
        finally {
            canLoad = true;
            progressBar.setVisibility(View.GONE);
        }
    }

    private void showSampleUploadSuccessSnackbar() {
        Snackbar snackbar = Snackbar.make(SampleListenActivity.this.findViewById(R.id.mainContent),
                "Sample info has been successfully updated.", Snackbar.LENGTH_LONG);
        snackbar.setTextColor(ContextCompat.getColor(SampleListenActivity.this, R.color.white_tinted));

        View view = snackbar.getView();
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) view.getLayoutParams();
        params.gravity = Gravity.TOP;
        Rect rectangle = new Rect();
        Window window = getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
        int statusBarHeight = rectangle.top;
        Resources r = getResources();
        int px = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                56,
                r.getDisplayMetrics()
        );
        params.topMargin = px;
        view.setLayoutParams(params);

        snackbar.show();
    }
}