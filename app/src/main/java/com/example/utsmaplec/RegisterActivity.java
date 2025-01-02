    package com.example.utsmaplec;

    import android.content.Intent;
    import android.os.Bundle;
    import android.widget.Button;
    import android.widget.EditText;
    import android.widget.Toast;
    import androidx.appcompat.app.AppCompatActivity;
    import com.google.firebase.auth.FirebaseAuth;
    import com.google.firebase.firestore.FirebaseFirestore;

    import java.util.HashMap;
    import java.util.Map;

    public class RegisterActivity extends AppCompatActivity {

        private EditText editTextName, editTextEmail, editTextPassword, editTextConfirmPassword, editTextNumber;
        private Button buttonRegister;
        private FirebaseAuth auth;
        private FirebaseFirestore db;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_register);

            // Initialize views
            editTextName = findViewById(R.id.editTextName);
            editTextEmail = findViewById(R.id.editTextEmail);
            editTextPassword = findViewById(R.id.editTextPassword);
            editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
            editTextNumber = findViewById(R.id.editTextNumber);
            buttonRegister = findViewById(R.id.buttonRegister);

            // Initialize Firebase Auth and Firestore
            auth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();

            // Set onClick listener for the register button
            buttonRegister.setOnClickListener(view -> registerUser());
        }

        private void registerUser() {
            String name = editTextName.getText().toString().trim();
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();
            String confirmPassword = editTextConfirmPassword.getText().toString().trim();
            String phoneNumber = editTextNumber.getText().toString().trim();

            // Check if name, email, and passwords are not empty
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || phoneNumber.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if password and confirm password match
            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            // Register user with Firebase Authentication
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // Get the UID of the newly registered user
                            String uid = auth.getCurrentUser().getUid();

                            // Create a map to store user data
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("name", name);
                            userData.put("email", email);
                            userData.put("phoneNumber", phoneNumber);
                            userData.put("created_at", System.currentTimeMillis());

                            // Save user data to Firestore
                            db.collection("users").document(uid)
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        // Registration and Firestore save successful
                                        Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();

                                        // Redirect to LoginActivity after successful registration
                                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                        startActivity(intent);
                                        finish(); // Close the RegisterActivity to prevent going back to it
                                    })
                                    .addOnFailureListener(e -> {
                                        // Failed to save user data
                                        Toast.makeText(RegisterActivity.this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            // Registration failed
                            Toast.makeText(RegisterActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
