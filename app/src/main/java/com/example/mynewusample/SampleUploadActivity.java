package com.example.mynewusample;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SampleUploadActivity extends AppCompatActivity {

    private TextInputLayout textInputName;
    private TextInputLayout textInputFileName;
    private TextInputLayout textInputNote;
    private CardView cardViewCover;
    private ImageView imageViewCover;
    private ImageView imageViewCoverIcon;
    private Button buttonChooseFile;
    private Button buttonUpload;
    private ProgressBar progressBar;

    private Uri songFile;
    private Uri albumCoverFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample_upload);
        MaterialToolbar toolbar = (MaterialToolbar) findViewById(R.id.topAppBar);;
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        textInputName = findViewById(R.id.textFieldName);
        textInputFileName = findViewById(R.id.textFieldFileName);
        textInputNote = findViewById(R.id.textFieldNote);
        cardViewCover = findViewById(R.id.cardViewCover);
        imageViewCover = findViewById(R.id.imageViewCover);
        imageViewCoverIcon = findViewById(R.id.imageViewCoverIcon);
        buttonChooseFile = findViewById(R.id.buttonChooseFile);
        buttonUpload = findViewById(R.id.buttonUpload);
        progressBar = findViewById(R.id.progressBar);


        ActivityResultLauncher<Intent> chooseFileActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Intent data = result.getData();
                            Uri audioUri = data.getData();
                            String fileName = getFileName(audioUri);
                            textInputFileName.getEditText().setText(fileName);
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
                            try {
                                Picasso.get().load(imageUri).resize(256, 256).centerCrop().into(imageViewCover);
//                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                                imageViewCover.setVisibility(View.VISIBLE);
                                imageViewCoverIcon.setVisibility(View.GONE);
//                                imageViewCover.setImageBitmap(bitmap);

//                                Bitmap newImage = ((BitmapDrawable) imageViewCover.getDrawable()).getBitmap();
//                                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
//                                newImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
//                                String path = MediaStore.Images.Media.insertImage(getContentResolver(), newImage, "AlbumCover", null);
//                                albumCoverFile = Uri.parse(path);
                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(SampleUploadActivity.this, "Error with setting the cover", Toast.LENGTH_LONG).show();
                                imageViewCover.setVisibility(View.GONE);
                                imageViewCoverIcon.setVisibility(View.VISIBLE);
                            }
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
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View view = getCurrentFocus();
            if (view instanceof EditText) {
                Rect outRect = new Rect();
                view.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    view.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent( event );
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if(index < 0) index = 0;
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

    private void uploadFile(){
        progressBar.setVisibility(View.VISIBLE);
    }

    private void uploadFileStart(){

    }
}