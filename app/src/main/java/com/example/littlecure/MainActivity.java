package com.example.littlecure;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "LittleCurePrefs";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_ROLE = "role";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Enforce Light Mode only, ignoring system dark mode settings
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        
        // Simple routing logic based on session (only auto-redirect if Remember Me is checked)
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean rememberMe = prefs.getBoolean("remember_me", false);
        if (prefs.contains(KEY_USER_ID) && rememberMe) {
            String role = prefs.getString(KEY_ROLE, "");
            redirectUser(role);
        } else {
            // Clear temporary session if not remembered
            if (prefs.contains(KEY_USER_ID)) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove(KEY_USER_ID);
                editor.remove(KEY_USERNAME);
                editor.remove(KEY_ROLE);
                editor.apply();
            }
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
    }

    private void redirectUser(String role) {
        Intent intent;
        switch (role) {
            case "admin":
                intent = new Intent(this, AdminActivity.class);
                break;
            case "dokter":
                intent = new Intent(this, DoctorActivity.class);
                break;
            case "pasien":
            default:
                intent = new Intent(this, PasienActivity.class);
                break;
        }
        startActivity(intent);
    }
}