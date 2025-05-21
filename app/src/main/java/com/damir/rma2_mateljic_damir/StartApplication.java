package com.damir.rma2_mateljic_damir;

import static android.content.ContentValues.TAG;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class StartApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "StartApplication: onCreate");
            initializeFirebase();
           initializeFirebase();
            checkData();
    }

    private void initializeFirebase() {
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
            Log.d(TAG, "Firebase initialized");
        }else{
            Log.d(TAG, "Firebase already initialized");
        }
    }
    private void checkData(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .whereEqualTo("name", "Employee001")
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().isEmpty()) {
                        createEmployee();
                    }else {
                        Log.d(TAG, "Employee already exists");
                    }
                });
    }

    private void createEmployee() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", GuidGenerator.generateGuid());
        userMap.put("name", "Employee001");
        userMap.put("email", "employee001@mediashop.com");
        userMap.put("password", "employee123");
        userMap.put("role", "EMPLOYEE");

        db.collection("users")
                .add(userMap)
                .addOnSuccessListener(documentReference -> {
                    Log.d("StartApplication", "Employee created with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e("StartApplication", "Error creating employee", e);
                    e.printStackTrace();
                });
    }
}