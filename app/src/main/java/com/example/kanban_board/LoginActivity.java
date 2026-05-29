package com.example.kanban_board;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText editName, editEmail, editPassword;
    private Button btnMainAction, btnGoogle;
    private TextView tvTitle, tvToggleMode;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    // Tracker to know which mode the user is currently looking at
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Bind UI Elements
        tvTitle = findViewById(R.id.tvTitle);
        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        btnMainAction = findViewById(R.id.btnMainAction);
        tvToggleMode = findViewById(R.id.tvToggleMode);
        btnGoogle = findViewById(R.id.btnGoogle);

        // 1. UI TOGGLE LOGIC
        tvToggleMode.setOnClickListener(v -> {
            isLoginMode = !isLoginMode; // Flip the switch

            if (isLoginMode) {
                // Switch to Login UI
                tvTitle.setText("Welcome Back");
                editName.setVisibility(View.GONE);
                btnMainAction.setText("Login");
                tvToggleMode.setText("Don't have an account? Sign Up");
            } else {
                // Switch to Sign Up UI
                tvTitle.setText("Create Account");
                editName.setVisibility(View.VISIBLE);
                btnMainAction.setText("Register");
                tvToggleMode.setText("Already have an account? Login");
            }
        });

        // 2. MAIN BUTTON LOGIC (Handles BOTH Login and Register based on mode)
        btnMainAction.setOnClickListener(v -> {
            String email = editEmail.getText().toString().trim();
            String password = editPassword.getText().toString().trim();
            String name = editName.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(LoginActivity.this, "Email and Password are required.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isLoginMode) {
                // --- EXECUTE LOGIN ---
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                navigateToDashboard();
                            } else {
                                Toast.makeText(LoginActivity.this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            } else {
                // --- EXECUTE REGISTRATION ---
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(LoginActivity.this, "Please enter your full name.", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                String uid = mAuth.getCurrentUser().getUid();

                                // Construct database profile using their typed name
                                Map<String, Object> userProfile = new HashMap<>();
                                userProfile.put("email", email);
                                userProfile.put("name", name);
                                userProfile.put("role", "user");

                                db.collection("Users").document(uid).set(userProfile)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(LoginActivity.this, "Welcome, " + name + "!", Toast.LENGTH_SHORT).show();
                                            navigateToDashboard();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(LoginActivity.this, "DB Sync Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            navigateToDashboard();
                                        });
                            } else {
                                Toast.makeText(LoginActivity.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });

        // 3. GOOGLE SIGN IN TRIGGER
        btnGoogle.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account);
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign in failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();

                        db.collection("Users").document(uid).get().addOnCompleteListener(docTask -> {
                            if (docTask.isSuccessful()) {
                                DocumentSnapshot document = docTask.getResult();
                                if (!document.exists()) {
                                    Map<String, Object> userProfile = new HashMap<>();
                                    userProfile.put("email", account.getEmail());
                                    userProfile.put("name", account.getDisplayName() != null ? account.getDisplayName() : account.getEmail());
                                    userProfile.put("role", "user");

                                    db.collection("Users").document(uid).set(userProfile)
                                            .addOnCompleteListener(wTask -> navigateToDashboard());
                                } else {
                                    navigateToDashboard();
                                }
                            } else {
                                navigateToDashboard();
                            }
                        });
                    } else {
                        Toast.makeText(LoginActivity.this, "Firebase Google Sync Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}