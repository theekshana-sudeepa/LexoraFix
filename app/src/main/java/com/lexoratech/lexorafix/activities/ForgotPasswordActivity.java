package com.lexoratech.lexorafix.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.data.FirebaseAuthHelper;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private TextInputLayout tilEmail;
    private MaterialButton btnReset;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        tilEmail = findViewById(R.id.til_email);
        etEmail = findViewById(R.id.et_email);
        btnReset = findViewById(R.id.btn_reset);
        progressBar = findViewById(R.id.progress_bar);
        ImageView btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());

        btnReset.setOnClickListener(v -> attemptReset());
    }

    private void attemptReset() {
        tilEmail.setError(null);

        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.field_required));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.invalid_email));
            return;
        }

        setLoading(true);

        FirebaseAuthHelper.getInstance().sendPasswordReset(email, new FirebaseAuthHelper.AuthCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Toast.makeText(ForgotPasswordActivity.this,
                        getString(R.string.reset_email_sent), Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnReset.setEnabled(!loading);
    }
}
