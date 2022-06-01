package com.example.mynewusample;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class ResetPasswordDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if(getActivity() != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = requireActivity().getLayoutInflater();

            builder.setView(inflater.inflate(R.layout.dialog_password_reset, null));
            TextInputLayout textFieldEmail = ResetPasswordDialogFragment.this.getView().findViewById(R.id.textFieldEmail);
            builder.setCancelable(true);
            builder.setTitle("Reset password").setMessage("Enter your email to receive password reset link.");
            builder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    String email = textFieldEmail.getEditText().getText().toString().trim();
                    if (TextUtils.isEmpty(email)) {
                        textFieldEmail.setErrorEnabled(true);
                        textFieldEmail.setError("Email is required.");
                        return;
                    }
                    FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Toast.makeText(getContext(), "Password reset link has been sent to your email.", Toast.LENGTH_LONG).show();
                            ResetPasswordDialogFragment.this.getDialog().cancel();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    ResetPasswordDialogFragment.this.getDialog().cancel();
                }
            });
            return builder.create();
        }
        return null;
    }
}
