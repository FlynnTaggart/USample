package com.example.mynewusample;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.yalantis.ucrop.UCrop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SampleUploadActivity extends AppCompatActivity {

    private TextInputLayout textFieldName;
    private TextInputLayout textFieldFileName;
    private TextInputLayout textFieldNote;
    private CardView cardViewCover;
    private ImageView imageViewCover;
    private ImageView imageViewCoverIcon;
    private Button buttonChooseFile;
    private Button buttonUpload;
    private ProgressBar progressBar;

    private Uri sampleFile;
    private Uri sampleCoverFile;
    private Bitmap sampleCoverBitmap;

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
        setContentView(R.layout.activity_sample_upload);
        MaterialToolbar toolbar = (MaterialToolbar) findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        textFieldName = findViewById(R.id.textFieldName);
        textFieldFileName = findViewById(R.id.textFieldFileName);
        textFieldNote = findViewById(R.id.textFieldNote);
        cardViewCover = findViewById(R.id.cardViewCover);
        imageViewCover = findViewById(R.id.imageViewCover);
        imageViewCoverIcon = findViewById(R.id.imageViewCoverIcon);
        buttonChooseFile = findViewById(R.id.buttonChooseFile);
        buttonUpload = findViewById(R.id.buttonUpload);
        progressBar = findViewById(R.id.progressBar);

        sampleFile = null;
        sampleCoverFile = null;

        mAuth = FirebaseAuth.getInstance();
        mStore = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();
        if (user != null) {
            userID = user.getUid();
        }
        mStorageRef = FirebaseStorage.getInstance().getReference();

        ActivityResultLauncher<Intent> chooseFileActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Intent data = result.getData();
                            Uri audioUri = data.getData();
                            String fileName = getFileName(audioUri);
                            textFieldFileName.getEditText().setText(fileName);
                            sampleFile = audioUri;
                        }
                    }
                });

        buttonChooseFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");
                chooseFileActivityResultLauncher.launch(intent);
            }
        });

        ActivityResultLauncher<Intent> chooseCoverActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Intent data = result.getData();
                            Uri imageUri = data.getData();
                            sampleCoverFile = imageUri;

                            File cover = new File(getCacheDir(), "Cropped.jpg");
                            if (cover.exists()) {
                                cover.delete();
                            }
                            Uri uri = Uri.fromFile(cover);
                            UCrop.Options options = new UCrop.Options();
                            options.setToolbarColor(ContextCompat.getColor(SampleUploadActivity.this, R.color.black));
                            options.setActiveControlsWidgetColor(ContextCompat.getColor(SampleUploadActivity.this, R.color.orange_500));
                            options.setStatusBarColor(ContextCompat.getColor(SampleUploadActivity.this, R.color.black));
                            options.setDimmedLayerColor(ContextCompat.getColor(SampleUploadActivity.this, R.color.black));
                            options.setToolbarWidgetColor(ContextCompat.getColor(SampleUploadActivity.this, R.color.white));
                            options.setLogoColor(ContextCompat.getColor(SampleUploadActivity.this, R.color.black));
                            UCrop.of(imageUri, uri)
                                    .withAspectRatio(1, 1)
                                    .withMaxResultSize(256, 256)
                                    .withOptions(options)
                                    .start(SampleUploadActivity.this);
                        }
                    }
                });

        cardViewCover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                chooseCoverActivityResultLauncher.launch(intent);
            }
        });


        buttonUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadFileStart();
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

        textFieldName.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                textFieldName.setErrorEnabled(false);
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
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

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index < 0) index = 0;
                    result = cursor.getString(index);
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void uploadFileStart() {
        if (sampleFile == null) {
            Toast.makeText(this, "Please select a sample.", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(textFieldFileName.getEditText().getText().toString().trim())) {
            Toast.makeText(this, "Please select a sample.", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(textFieldName.getEditText().getText().toString().trim())) {
            textFieldName.setErrorEnabled(true);
            textFieldName.setError("Sample name is required.");
        } else if (textFieldName.getEditText().getText().toString().trim().length() > 30) {
            textFieldName.setErrorEnabled(true);
            textFieldName.setError("Sample name is too long.");
        } else if (canLoad) {
            uploadFile();
        }
    }

    private void uploadFile() {
        canLoad = false;
        progressBar.setVisibility(View.VISIBLE);

        String sampleName = simplifySampleName(textFieldName.getEditText().getText().toString().trim());
        String fileName = textFieldFileName.getEditText().getText().toString().trim();
        String note = textFieldNote.getEditText().getText().toString().trim();

        DocumentReference documentRef = mStore.collection("users").document(userID)
                .collection("samples").document(sampleName);
        documentRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    canLoad = true;
                    textFieldName.setErrorEnabled(true);
                    textFieldName.setError("Sample with such name already exists.");
                    progressBar.setVisibility(View.GONE);
                } else {
                    final StorageReference sampleRef = mStorageRef.child("users/" + userID + "/samples/" + fileName);
                    final StorageReference coverRef = mStorageRef.child("users/" + userID + "/covers/" + cutFileExtensionFromFileName(fileName) + ".jpg");
                    StorageTask sampleUploadTask = sampleRef.putFile(sampleFile).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            sampleRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri sampleUri) {
                                    if (sampleCoverBitmap != null && sampleCoverBitmap.getByteCount() != 0) {
                                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                        sampleCoverBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                        byte[] data = baos.toByteArray();
                                        StorageTask coverUploadTask = coverRef.putBytes(data).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                            @Override
                                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                                coverRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                    @Override
                                                    public void onSuccess(Uri coverUri) {
                                                        SampleStructure sample = new SampleStructure(sampleName, sampleUri.toString(),
                                                                fileName, coverUri.toString(), note);
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
                                                                onSampleUploadFailure(e);
                                                            }
                                                        });
                                                    }
                                                });
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                onSampleUploadFailure(e);
                                            }
                                        });
                                    } else {
                                        SampleStructure sample = new SampleStructure(sampleName, sampleUri.toString(), fileName, "", note);
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
                                                onSampleUploadFailure(e);
                                            }
                                        });
                                    }
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    onSampleUploadFailure(e);
                                }
                            });
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            onSampleUploadFailure(e);
                        }
                    });
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                onSampleUploadFailure(e);
            }
        });

    }

    private void showSampleUploadSuccessSnackbar() {
        Snackbar snackbar = Snackbar.make(SampleUploadActivity.this.findViewById(R.id.mainContent),
                "Sample has been successfully uploaded.", Snackbar.LENGTH_LONG);
        snackbar.setTextColor(ContextCompat.getColor(SampleUploadActivity.this, R.color.white_tinted));

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

    public void onSampleUploadFailure(Exception e){
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
                Toast.makeText(SampleUploadActivity.this, "There is an error with uploading the sample", Toast.LENGTH_LONG).show();
                Log.e("SampleUpload", "Error: " + ex.getMessage() + " " + ex.getClass().toString());
            }
        }
        catch (Exception ex) {
            Toast.makeText(SampleUploadActivity.this, "There is an error with uploading the sample", Toast.LENGTH_LONG).show();
            Log.e("SampleUpload", "Error: " + ex.getMessage() + " " + ex.getClass().toString());
        }
        finally {
            canLoad = true;
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            final Uri imageUri = UCrop.getOutput(data);
            sampleCoverFile = imageUri;
            Target target = new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    sampleCoverBitmap = bitmap;
                    imageViewCover.setImageBitmap(sampleCoverBitmap);
                }

                @Override
                public void onBitmapFailed(Exception e, Drawable errorDrawable) {

                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {
                    imageViewCover.setImageDrawable(placeHolderDrawable);
                }
            };
            try {
                Picasso.get().load(imageUri).resize(256, 256).centerCrop().memoryPolicy(MemoryPolicy.NO_CACHE).into(target);
                imageViewCover.setVisibility(View.VISIBLE);
                imageViewCoverIcon.setVisibility(View.GONE);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(SampleUploadActivity.this, "Error with setting the cover.", Toast.LENGTH_LONG).show();
                imageViewCover.setVisibility(View.GONE);
                imageViewCoverIcon.setVisibility(View.VISIBLE);
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
            Toast.makeText(SampleUploadActivity.this, "Error with setting the cover.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static String simplifySampleName(String sampleName) {
        return sampleName.replace("\\s", "_");
    }

    public static String cutFileExtensionFromFileName(String fileName) {
        return fileName.replace("\\.[a-zA-Z0-9]+$", "");
    }
}