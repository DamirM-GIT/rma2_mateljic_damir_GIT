package com.damir.rma2_mateljic_damir;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class MainActivity extends AppCompatActivity {

    private EditText etName, etPassword;
    private Button btnLogin, btnRegister;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etName = findViewById(R.id.nameEditText);
        etPassword = findViewById(R.id.passwordEditText);
        btnLogin = findViewById(R.id.loginButton);
        btnRegister = findViewById(R.id.registerButton);
        db = FirebaseFirestore.getInstance();

        btnLogin.setOnClickListener(v -> attemptLogin());
        btnRegister.setOnClickListener(v -> showRegistrationDialog());
    }

    private void attemptLogin() {
        String name = etName.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if(name.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Popunite sva polja", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .whereEqualTo("name", name)
                .whereEqualTo("password", password)
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot doc = task.getResult().getDocuments().get(0);
                        String userId = doc.getId();

                        Intent intent = new Intent(MainActivity.this, StoreActivity.class);
                        intent.putExtra("USER_ID", userId);
                        startActivity(intent);
                    } else {
                        Toast.makeText(MainActivity.this, "Pogrešni podaci za prijavu", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showRegistrationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.activity_register, null);

        EditText etRegName = dialogView.findViewById(R.id.etRegName);
        EditText etRegEmail = dialogView.findViewById(R.id.etRegEmail);
        EditText etRegPassword = dialogView.findViewById(R.id.etRegPassword);
        Button btnDialogRegister = dialogView.findViewById(R.id.btnDialogRegister);
        Button btnDialogCancel = dialogView.findViewById(R.id.btnDialogCancel);

        AlertDialog dialog = builder.setView(dialogView).create();

        btnDialogRegister.setOnClickListener(v -> {
            String name = etRegName.getText().toString().trim();
            String email = etRegEmail.getText().toString().trim();
            String password = etRegPassword.getText().toString().trim();

            if(name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Popunite sva polja", Toast.LENGTH_SHORT).show();
                return;
            }

            User newUser = new User(name, email, password, "CUSTOMER");

            db.collection("users")
                    .document(newUser.getId())
                    .set(newUser)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(MainActivity.this, "Registracija uspješna!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MainActivity.this, "Greška pri registraciji", Toast.LENGTH_SHORT).show();
                        Log.e("Registration", "Error: ", e);
                    });
        });

        btnDialogCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}