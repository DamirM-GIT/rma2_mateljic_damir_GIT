package com.damir.rma2_mateljic_damir;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StoreActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private Button btnStore, btnShoppingCart, btnRegisterEmployee, btnLogout, btnHistory;
    private FirebaseFirestore db;
    private String userRole;
    private String userId;
    private Map<String, Integer> cart = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);

        tvWelcome = findViewById(R.id.tvWelcome);
        btnStore = findViewById(R.id.btnStore);
        btnLogout = findViewById(R.id.btnLogout);
        btnShoppingCart = findViewById(R.id.btnShoppingCart);
        btnRegisterEmployee = findViewById(R.id.btnRegisterEmployee);
        btnHistory = findViewById(R.id.btnHistory);

        db = FirebaseFirestore.getInstance();
        String userId = getIntent().getStringExtra("USER_ID");
        this.userId = userId;

        if (userId != null && !userId.isEmpty()) {
            fetchUserData(userId);
        } else {
            Toast.makeText(this, "Greška: Korisnički ID nije pronađen", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void fetchUserData(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userName = documentSnapshot.getString("name");
                        userRole = documentSnapshot.getString("role");
                        tvWelcome.setText("Dobrodošli, " + userName + "!");
                        setupModulesByRole();
                    } else {
                        Toast.makeText(this, "Korisnik nije pronađen", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Greška pri dohvatanju podataka", Toast.LENGTH_SHORT).show();
                    Log.e("StoreActivity", "Error fetching user", e);
                    finish();
                });
    }

    private void setupModulesByRole() {
        if ("EMPLOYEE".equals(userRole)) {
            btnStore.setVisibility(View.VISIBLE);
            btnRegisterEmployee.setVisibility(View.VISIBLE);
            btnShoppingCart.setVisibility(View.GONE);
            btnHistory.setVisibility(View.GONE);
        } else if ("CUSTOMER".equals(userRole)) {
            btnStore.setVisibility(View.VISIBLE);
            btnRegisterEmployee.setVisibility(View.GONE);
            btnShoppingCart.setVisibility(View.VISIBLE);
            btnHistory.setVisibility(View.VISIBLE);
        } else {
            btnStore.setVisibility(View.GONE);
            btnRegisterEmployee.setVisibility(View.GONE);
            btnShoppingCart.setVisibility(View.GONE);
            btnHistory.setVisibility(View.GONE);
        }
        btnStore.setOnClickListener(v -> onStoreClicked());
        btnHistory.setOnClickListener(v -> showHistoryDialog());
        btnShoppingCart.setOnClickListener(v -> onCartClicked());
        btnRegisterEmployee.setOnClickListener(v -> onRegisterClicked());
        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(StoreActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void onStoreClicked() {
        if ("EMPLOYEE".equals(userRole)) {
            showAddProductDialog();
        } else {
            showCustomerStoreDialog();
        }
    }

    private void onCartClicked() {
        showCartDialog();
    }

    private void onRegisterClicked() {
        showRegisterDialog();
    }

    private void showAddProductDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Dodaj artikal");

        final EditText input = new EditText(this);
        input.setHint("Naziv artikla");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Dodaj", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Unesite naziv artikla", Toast.LENGTH_SHORT).show();
                return;
            }
            db.collection("products").document(name)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            long qty = doc.getLong("quantity");
                            doc.getReference().update("quantity", qty + 1)
                                    .addOnSuccessListener(a -> Toast.makeText(this, "Povećana količina za " + name, Toast.LENGTH_SHORT).show());
                        } else {
                            Map<String, Object> prod = new HashMap<>();
                            prod.put("name", name);
                            prod.put("quantity", 1);
                            db.collection("products").document(name)
                                    .set(prod)
                                    .addOnSuccessListener(a -> Toast.makeText(this, "Artikal " + name + " dodat", Toast.LENGTH_SHORT).show());
                        }
                    });
        });
        builder.setNegativeButton("Otkaži", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showCustomerStoreDialog() {
        db.collection("products")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Artikli");

                    ScrollView scroll = new ScrollView(this);
                    LinearLayout layout = new LinearLayout(this);
                    layout.setOrientation(LinearLayout.VERTICAL);
                    scroll.addView(layout);

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String name = doc.getString("name");
                        long qty = doc.getLong("quantity");

                        LinearLayout row = new LinearLayout(this);
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        row.setPadding(8, 8, 8, 8);

                        TextView tv = new TextView(this);
                        tv.setText(name + " (" + qty + " kom)");
                        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

                        Button btnBuy = new Button(this);
                        btnBuy.setText("Kupi");
                        btnBuy.setOnClickListener(v -> showQuantityDialog(name, qty));

                        row.addView(tv);
                        row.addView(btnBuy);
                        layout.addView(row);
                    }

                    builder.setView(scroll);
                    builder.setNegativeButton("Zatvori", (d, w) -> d.dismiss());
                    builder.show();
                });
    }

    private void showQuantityDialog(String name, long available) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Kupi: " + name);

        final EditText input = new EditText(this);
        input.setHint("Količina (max " + available + ")");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String s = input.getText().toString().trim();
            if (s.isEmpty()) return;
            int qty = Integer.parseInt(s);
            if (qty < 1 || qty > available) {
                Toast.makeText(this, "Nevažeća količina", Toast.LENGTH_SHORT).show();
                return;
            }
            int prev = cart.containsKey(name) ? cart.get(name) : 0;
            cart.put(name, prev + qty);
            Toast.makeText(this, "Dodano u korpu: " + name + " x" + qty, Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", (d, w) -> d.dismiss());
        builder.show();
    }

    private void showCartDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Korpa");

        if (cart.isEmpty()) {
            builder.setMessage("Korpa je prazna");
            builder.setPositiveButton("OK", (d, w) -> d.dismiss());
            builder.show();
            return;
        }

        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(layout);

        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            TextView tv = new TextView(this);
            tv.setText(entry.getKey() + " x" + entry.getValue());
            layout.addView(tv);
        }

        builder.setView(scroll);
        builder.setPositiveButton("Plati", (d, w) -> processPayment());
        builder.setNegativeButton("Zatvori", (d, w) -> d.dismiss());
        builder.show();
    }

    private void processPayment() {
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            String name = entry.getKey();
            int qtyToBuy = entry.getValue();
            db.collection("products").document(name)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            long available = doc.getLong("quantity");
                            long updated = available - qtyToBuy;
                            if (updated < 0) updated = 0;
                            doc.getReference().update("quantity", updated);
                        }
                    });
        }
        Map<String, Object> hist = new HashMap<>();
        hist.put("user_id", userId);
        hist.put("items", cart);
        hist.put("date", Timestamp.now());
        db.collection("history").add(hist);
        cart.clear();
        Toast.makeText(this, "Kupovina uspješna!", Toast.LENGTH_SHORT).show();
    }

    private void showRegisterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Registracija zaposlenika");

        View view = LayoutInflater.from(this).inflate(R.layout.activity_register, null);
        builder.setView(view);

        EditText etName = view.findViewById(R.id.etRegName);
        EditText etEmail = view.findViewById(R.id.etRegEmail);
        EditText etPassword = view.findViewById(R.id.etRegPassword);
        Button btnReg = view.findViewById(R.id.btnDialogRegister);
        Button btnCancel = view.findViewById(R.id.btnDialogCancel);

        AlertDialog dialog = builder.create();

        btnReg.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Sva polja su obavezna", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Object> user = new HashMap<>();
            user.put("name", name);
            user.put("email", email);
            user.put("password", password);
            user.put("role", "EMPLOYEE");
            db.collection("users")
                    .add(user)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(this, "Zaposlenik registriran", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Greška pri registraciji", Toast.LENGTH_SHORT).show());
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showHistoryDialog() {
        db.collection("history")
                .whereEqualTo("user_id", userId)
                .get()
                .addOnSuccessListener((com.google.firebase.firestore.QuerySnapshot querySnapshot) -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("History of Purchase");

                    List<DocumentSnapshot> docs = querySnapshot.getDocuments();
                    Collections.sort(docs, (d1, d2) -> {
                        com.google.firebase.Timestamp t1 = d1.getTimestamp("date");
                        com.google.firebase.Timestamp t2 = d2.getTimestamp("date");
                        return t1.compareTo(t2);
                    });

                    ScrollView scroll = new ScrollView(this);
                    LinearLayout layout = new LinearLayout(this);
                    layout.setOrientation(LinearLayout.VERTICAL);

                    DateFormat df = DateFormat.getDateTimeInstance();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : docs) {
                        if (doc.contains("date") && doc.contains("items")) {
                            String dateStr = df.format(doc.getTimestamp("date").toDate());
                            @SuppressWarnings("unchecked")
                            Map<String, Long> items = (Map<String, Long>) doc.get("items");
                            TextView tv = new TextView(this);
                            tv.setText(dateStr + ": " + items.toString());
                            layout.addView(tv);
                        }
                    }

                    scroll.addView(layout);
                    builder.setView(scroll);
                    builder.setPositiveButton("OK", (d, w) -> d.dismiss());
                    AlertDialog dialog = builder.create();
                    dialog.show();
                })
                .addOnFailureListener(e -> {
                    Log.e("StoreActivity", "Error fetching history", e);
                    Toast.makeText(this, "Greška pri učitavanju istorije", Toast.LENGTH_SHORT).show();
                });
    }
}
