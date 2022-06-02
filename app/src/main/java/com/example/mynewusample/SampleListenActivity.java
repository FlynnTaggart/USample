package com.example.mynewusample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;

public class SampleListenActivity extends AppCompatActivity {

    private TextView textViewSampleName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample_listen);
        MaterialToolbar toolbar = (MaterialToolbar) findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        textViewSampleName = findViewById(R.id.textViewSampleName);

        Intent intent = getIntent();
        if(intent != null){
            if(intent.hasExtra("sampleName")){
                textViewSampleName.setText(intent.getStringExtra("sampleName"));
            }
        }
    }
}