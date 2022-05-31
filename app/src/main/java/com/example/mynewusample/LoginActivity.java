package com.example.mynewusample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.transition.Slide;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout textFieldEmail;
    private TextInputLayout textFieldPassword;
    private Button buttonCommit;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        getWindow().setExitTransition(new Slide(Gravity.LEFT));
        setContentView(R.layout.activity_login);
        MaterialToolbar toolbar = (MaterialToolbar) findViewById(R.id.topAppBar);;
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        textFieldEmail = findViewById(R.id.textFieldEmail);
        textFieldPassword = findViewById(R.id.textFieldPassword);
        buttonCommit = findViewById(R.id.buttonCommit);
        progressBar = findViewById(R.id.progressBar);

        mAuth = FirebaseAuth.getInstance();

        buttonCommit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = textFieldEmail.getEditText().getText().toString().trim();
                String password = textFieldPassword.getEditText().getText().toString();

                if(TextUtils.isEmpty(email)){
                    textFieldEmail.setErrorEnabled(true);
                    textFieldEmail.setError("Email is required.");
                    return;
                }
                if(TextUtils.isEmpty(password)){
                    textFieldPassword.setErrorEnabled(true);
                    textFieldPassword.setError("Password is required.");
                    return;
                }
                if(password.length() < 8){
                    textFieldPassword.setErrorEnabled(true);
                    textFieldPassword.setError("Min. 8 characters required.");
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);

                mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                        } else {
                            try{
                                throw task.getException();
                            }
                            catch(FirebaseAuthInvalidCredentialsException e){
                                if(e.getErrorCode().equals("ERROR_INVALID_EMAIL")){
                                    textFieldEmail.setErrorEnabled(true);
                                    textFieldEmail.setError("Wrong email format.");
                                }
                                if(e.getErrorCode().equals("ERROR_WRONG_PASSWORD")){
                                    textFieldPassword.setErrorEnabled(true);
                                    textFieldPassword.setError("Wrong password.");
                                }
                            }
                            catch(FirebaseAuthInvalidUserException e){
                                textFieldEmail.setErrorEnabled(true);
                                textFieldEmail.setError("There is no user with such email.");
                            }
                            catch (Exception e){
                                Toast.makeText(LoginActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                            finally {
                                progressBar.setVisibility(View.GONE);
                            }
                        }
                    }
                });
            }
        });

        textFieldEmail.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                textFieldEmail.setErrorEnabled(false);
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void afterTextChanged(Editable editable) { }
        });
        textFieldPassword.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                textFieldPassword.setErrorEnabled(false);
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void afterTextChanged(Editable editable) { }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.transition.slide_in_left, R.transition.slide_out_right);
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
}