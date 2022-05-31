package com.example.mynewusample;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;

public class AuthMainActivity extends AppCompatActivity {

    private Button buttonRegister;
    private Button buttonLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_main);

        buttonRegister = findViewById(R.id.buttonRegister);
        buttonLogin = findViewById(R.id.buttonLogin);

        if(FirebaseAuth.getInstance().getCurrentUser() != null){
            Intent intent = new Intent(AuthMainActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }

        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AuthMainActivity.this, RegisterActivity.class);
                startActivity(intent);
                overridePendingTransition(R.transition.slide_in_right, R.transition.slide_out_left);
            }
        });

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AuthMainActivity.this, LoginActivity.class);
                startActivity(intent);
                overridePendingTransition(R.transition.slide_in_right, R.transition.slide_out_left);
            }
        });
    }
}