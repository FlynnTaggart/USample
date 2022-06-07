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
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mynewusample.model.SampleStructure;
import com.example.mynewusample.player.SoundPlayer;
import com.example.mynewusample.player.SoundPlayerOnCompleteListener;
import com.example.mynewusample.player.SoundPlayerOnDurationProgressListener;
import com.example.mynewusample.player.SoundPlayerOnPauseListener;
import com.example.mynewusample.player.SoundPlayerOnPlayListener;
import com.example.mynewusample.player.SoundPlayerOnPreparedListener;
import com.example.mynewusample.player.SoundPlayerOnSeekToListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;
import com.masoudss.lib.WaveformSeekBar;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jp.wasabeef.picasso.transformations.BlurTransformation;
import jp.wasabeef.picasso.transformations.GrayscaleTransformation;
import jp.wasabeef.picasso.transformations.gpu.BrightnessFilterTransformation;
import jp.wasabeef.picasso.transformations.gpu.VignetteFilterTransformation;

public class SampleListenActivity extends AppCompatActivity implements SoundPlayerOnCompleteListener,
        SoundPlayerOnDurationProgressListener,
        SoundPlayerOnPauseListener,
        SoundPlayerOnPlayListener,
        SoundPlayerOnPreparedListener,
        SoundPlayerOnSeekToListener {

    private EditText textFieldName;
    private TextInputLayout textFieldNote;
    private ImageView imageViewSampleCover;
    private Button buttonPlay;
    private ProgressBar progressBar;
    private WaveformSeekBar waveformSeekBar;
    private TextView textViewCurrentTime;
    private TextView textViewOverallTime;

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
    private AlertDialog sampleLoadingDialog;
    private AlertDialog sampleErrorDialog;

    private SoundPlayer soundPlayer = new SoundPlayer();
    private File sampleLocalFile;

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
        progressBar = findViewById(R.id.progressBar);
        findViewById(R.id.appBarLayout).setOutlineProvider(null);
        waveformSeekBar = findViewById(R.id.waveformSeekBar);
        buttonPlay = findViewById(R.id.buttonPlay);
        textViewCurrentTime = findViewById(R.id.textViewCurrentTime);
        textViewOverallTime = findViewById(R.id.textViewOverallTime);

        createNetworkErrorDialog();
        createSampleLoadingDialog();
        createSampleErrorDialog();

        mAuth = FirebaseAuth.getInstance();
        mStore = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();
        if (user != null) {
            userID = user.getUid();
        }
        mStorageRef = FirebaseStorage.getInstance().getReference();

        soundPlayer.setOnCompleteListener(this)
                .setOnDurationProgressListener(this)
                .setOnPauseListener(this)
                .setOnPlayListener(this)
                .setOnPreparedListener(this)
                .setOnSeekToListener(this);

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("sampleName")) {
                sampleName = intent.getStringExtra("sampleName");
                textFieldName.setText(sampleName);
            }
            if (intent.hasExtra("note")) {
                note = intent.getStringExtra("note");
                textFieldNote.getEditText().setText(note);
            }
            if (intent.hasExtra("sampleCoverLink")) {
                sampleCoverLink = intent.getStringExtra("sampleCoverLink");
                Picasso.get().load(sampleCoverLink)
                        .resize(256, 256)
                        .centerCrop(Gravity.BOTTOM)
                        .transform(new BlurTransformation(SampleListenActivity.this, 5, 1))
                        .transform(new GrayscaleTransformation())
                        .transform(new BrightnessFilterTransformation(this, -0.1f))
                        .transform(new VignetteFilterTransformation(this, new PointF(0.5f, 0.5f), new float[]{0.f, 0.f, 0.f}, 0, 0.5f))
                        .error(R.drawable.default_sample_cover_bw)
                        .placeholder(R.drawable.default_sample_cover_bw)
                        .into(imageViewSampleCover);
            }
            if (intent.hasExtra("fileName")) {
                fileName = intent.getStringExtra("fileName");
            }
            if (intent.hasExtra("sampleLink")) {
                sampleLink = intent.getStringExtra("sampleLink");
                if (!isExternalStorageWritable() || !isExternalStorageReadable()) {
                    sampleErrorDialog.show();
                }
                File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "/USample/");
                if (!directory.exists()) {
                    directory.mkdir();
                }
                sampleLocalFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "/USample/" + fileName);
                if (!sampleLocalFile.exists()) {
                    if (!isNetworkAvailable()) {
                        sampleErrorDialog.show();
                    } else {
                        sampleLocalFile.delete();
                        try {
                            sampleLocalFile.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        sampleLoadingDialog.show();
                        final StorageReference sampleRef = mStorageRef.child("users/" + userID + "/samples/" + fileName);
                        sampleRef.getFile(sampleLocalFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                sampleLoadingDialog.dismiss();
                                Toast.makeText(SampleListenActivity.this, "Your sample has been saved to " + directory.getAbsolutePath(), Toast.LENGTH_LONG).show();
                                try {
                                    new getSampleWaveform().execute(sampleLocalFile);
                                    soundPlayer.setAudioSource(sampleLocalFile.getAbsolutePath());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                onSampleDownloadFailure(e);
                            }
                        });
                    }
                } else {
                    try {
                        new getSampleWaveform().execute(sampleLocalFile);
                        soundPlayer.setAudioSource(sampleLocalFile.getAbsolutePath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        textFieldName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    textFieldName.setSelectAllOnFocus(false);
                    view.setFocusable(false);
                    view.setFocusableInTouchMode(false);
                }
            }
        });

        buttonPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                soundPlayer.toggle();
            }
        });

        waveformSeekBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                double newTime = Math.max(0.0, Math.min(1.0, motionEvent.getX() / waveformSeekBar.getWidth()));
                soundPlayer.seekTo((long) Math.ceil(newTime * soundPlayer.getDuration()));
                waveformSeekBar.setProgress((float) Math.ceil(newTime * 100));
                return true;
            }
        });
    }

    private void createSampleLoadingDialog() {
        AlertDialog.Builder sampleLoadingBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        sampleLoadingBuilder.setView(inflater.inflate(R.layout.dialog_loading_sample, null));
        sampleLoadingBuilder.setCancelable(false);
        sampleLoadingBuilder.setTitle("Please wait...");
        sampleLoadingDialog = sampleLoadingBuilder.create();
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

    private void createSampleErrorDialog() {
        AlertDialog.Builder sampleErrorBuilder = new AlertDialog.Builder(this);
        sampleErrorBuilder.setPositiveButton("Go back", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                onBackPressed();
            }
        });
        sampleErrorBuilder.setCancelable(true);
        sampleErrorBuilder.setTitle("Error").setMessage("There is a problem with downloading the sample.");
        sampleErrorDialog = sampleErrorBuilder.create();
        sampleErrorDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                dialogInterface.cancel();
                onBackPressed();
            }
        });
        sampleErrorDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                dialogInterface.cancel();
                onBackPressed();
            }
        });
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public void onComplete(SoundPlayer player) {
        waveformSeekBar.setProgress(0);
        buttonPlay.setBackground(getDrawable(R.drawable.ic_round_play_arrow_24));
    }

    @Override
    public void onDurationProgress(SoundPlayer player, Long duration, Long currentTimestamp) {
        waveformSeekBar.setProgress(100 * currentTimestamp / (float) duration);
        textViewCurrentTime.setText(SoundPlayer.convertFromMSecToMinSec(currentTimestamp));
    }

    @Override
    public void onPause(SoundPlayer player) {
        buttonPlay.setBackground(getDrawable(R.drawable.ic_round_play_arrow_24));
    }

    @Override
    public void onPlay(SoundPlayer player) {
        buttonPlay.setBackground(getDrawable(R.drawable.ic_round_pause_24));
    }

    @Override
    public void onPrepared(SoundPlayer player) {
        textViewOverallTime.setText(SoundPlayer.convertFromMSecToMinSec(player.getDuration()));
    }

    @Override
    public void onSeekTo(SoundPlayer player, long msec) {
        textViewCurrentTime.setText(SoundPlayer.convertFromMSecToMinSec(msec));
    }

    class getSampleWaveform extends AsyncTask<File, Void, Void> {

        @Override
        protected Void doInBackground(File... files) {
            try {
                waveformSeekBar.setSampleFrom(files[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
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
    protected void onStop() {
        buttonPlay.setBackground(getDrawable(R.drawable.ic_round_play_arrow_24));
        try {
            soundPlayer.pause();
        } catch (Exception e) {
            e.printStackTrace();
        }
        soundPlayer.release();
        super.onStop();
    }

    @Override
    protected void onResume() {
        soundPlayer = new SoundPlayer();
        soundPlayer.setOnCompleteListener(this)
                .setOnDurationProgressListener(this)
                .setOnPauseListener(this)
                .setOnPlayListener(this)
                .setOnPreparedListener(this)
                .setOnSeekToListener(this);
        try {
            soundPlayer.setAudioSource(sampleLocalFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onResume();
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
                if (canLoad) {
                    canLoad = false;
                    progressBar.setVisibility(View.VISIBLE);
                    DocumentReference documentRef = mStore.collection("users").document(userID)
                            .collection("samples").document(sampleName);
                    if (!sampleName.equals(textFieldName.getText().toString().trim())) {
                        if (textFieldName.getText().toString().trim().length() > 30) {
                            Toast.makeText(SampleListenActivity.this, "Sample name must not be longer than 30 characters.",
                                    Toast.LENGTH_SHORT).show();
                            canLoad = true;
                            textFieldName.setText(sampleName);
                            progressBar.setVisibility(View.GONE);
                            return true;
                        }
                        DocumentReference documentRefCheck = mStore.collection("users").document(userID)
                                .collection("samples").document(textFieldName.getText().toString().trim());
                        documentRefCheck.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                if (documentSnapshot.exists()) {
                                    canLoad = true;
                                    Toast.makeText(SampleListenActivity.this, "Sample with such name already exists.", Toast.LENGTH_SHORT).show();
                                    textFieldName.setText(sampleName);
                                    progressBar.setVisibility(View.GONE);
                                } else {
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
                                }
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
                        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.toggleSoftInputFromWindow(
                        findViewById(R.id.mainContent).getApplicationWindowToken(),
                        InputMethodManager.SHOW_FORCED, 0);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public void onSampleUpdateFailure(Exception e) {
        try {
            throw e;
        } catch (FirebaseNetworkException ex) {
            networkErrorDialog.show();
        } catch (FirebaseFirestoreException ex) {
            if (ex.getCode() == FirebaseFirestoreException.Code.UNAVAILABLE) {
                networkErrorDialog.show();
            } else {
                Toast.makeText(SampleListenActivity.this, "There is an error with updating the sample info.", Toast.LENGTH_LONG).show();
                Log.e("SampleUpload", "Error: " + ex.getMessage() + " " + ex.getClass().toString());
            }
        } catch (Exception ex) {
            Toast.makeText(SampleListenActivity.this, "There is an error with updating the sample info.", Toast.LENGTH_LONG).show();
            Log.e("SampleUpload", "Error: " + ex.getMessage() + " " + ex.getClass().toString());
        } finally {
            canLoad = true;
            progressBar.setVisibility(View.GONE);
        }
    }

    public void onSampleDownloadFailure(Exception e) {
        try {
            throw e;
        } catch (FirebaseNetworkException ex) {
            sampleErrorDialog.show();
        } catch (StorageException ex) {
            if(ex.getCause().getClass().equals(FileNotFoundException.class)){
                Toast.makeText(SampleListenActivity.this, "There is an error with downloading the sample.", Toast.LENGTH_LONG).show();
                Log.e("SampleDownload", "Error: " + ex.getCause().getMessage() + " " + ex.getCause().getClass().toString());
            } else {
                Toast.makeText(SampleListenActivity.this, "There is an error with downloading the sample.", Toast.LENGTH_LONG).show();
                Log.e("SampleDownload", "Error: " + ex.getMessage() + " " + ex.getClass().toString());
            }
        } catch (FirebaseFirestoreException ex) {
            if (ex.getCode() == FirebaseFirestoreException.Code.UNAVAILABLE) {
                sampleErrorDialog.show();
            } else {
                Toast.makeText(SampleListenActivity.this, "There is an error with downloading the sample.", Toast.LENGTH_LONG).show();
                Log.e("SampleDownload", "Error: " + ex.getMessage() + " " + ex.getClass().toString());
            }
        } catch (Exception ex) {
            Toast.makeText(SampleListenActivity.this, "There is an error with downloading the sample.", Toast.LENGTH_LONG).show();
            Log.e("SampleDownload", "Error: " + ex.getMessage() + " " + ex.getClass().toString());
        } finally {
            sampleLoadingDialog.dismiss();
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
        Resources r = getResources();
        params.topMargin = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                56,
                r.getDisplayMetrics()
        );
        view.setLayoutParams(params);

        snackbar.show();
    }
}