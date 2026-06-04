package com.example.littlecure;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;

public class LoginActivity extends AppCompatActivity {

    // Common
    private DatabaseHelper dbHelper;
    private static final String PREFS_NAME = "LittleCurePrefs";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_ROLE = "role";
    private static final String KEY_PARENT_NAME = "parentName";
    private static final String KEY_CHILD_NAME = "childName";

    // Login views
    private LinearLayout layoutLogin;
    private TextInputEditText etUsername, etPassword;
    private CheckBox cbRememberMe;
    private TextView tvForgotPassword, tvSwitchToRegister;
    private Button btnSubmit;

    // Register views
    private LinearLayout layoutRegister;
    private TextView layoutBackToLogin;
    private TextInputEditText etRegName, etRegEmail, etRegPhone, etRegPassword;
    private TextInputLayout inputLayoutRegName, inputLayoutRegEmail, inputLayoutRegPhone, inputLayoutRegPassword;
    private Button btnSubmitRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Enforce Light Mode only, ignoring system dark mode settings
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new DatabaseHelper(this);

        // Check if user is already logged in (only auto-redirect if Remember Me is enabled)
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean rememberMe = prefs.getBoolean("remember_me", false);
        if (prefs.contains(KEY_USER_ID) && rememberMe) {
            redirectUser(prefs.getString(KEY_ROLE, ""));
            finish();
            return;
        } else if (prefs.contains(KEY_USER_ID) && !rememberMe) {
            // Clear temporary session on fresh launch
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(KEY_USER_ID);
            editor.remove(KEY_USERNAME);
            editor.remove(KEY_ROLE);
            editor.apply();
        }

        // Initialize login views
        layoutLogin = findViewById(R.id.layoutLogin);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvSwitchToRegister = findViewById(R.id.tvSwitchToRegister);
        btnSubmit = findViewById(R.id.btnSubmit);

        // Pre-fill username if Remember Me is checked
        if (rememberMe) {
            String savedUsername = prefs.getString("saved_username", "");
            etUsername.setText(savedUsername);
            cbRememberMe.setChecked(true);
        }

        // Initialize register views
        layoutRegister = findViewById(R.id.layoutRegister);
        layoutBackToLogin = findViewById(R.id.layoutBackToLogin);
        etRegName = findViewById(R.id.etRegName);
        etRegEmail = findViewById(R.id.etRegEmail);
        etRegPhone = findViewById(R.id.etRegPhone);
        etRegPassword = findViewById(R.id.etRegPassword);
        btnSubmitRegister = findViewById(R.id.btnSubmitRegister);

        // Bind registration input layouts for error handling
        inputLayoutRegName = findViewById(R.id.inputLayoutRegName);
        inputLayoutRegEmail = findViewById(R.id.inputLayoutRegEmail);
        inputLayoutRegPhone = findViewById(R.id.inputLayoutRegPhone);
        inputLayoutRegPassword = findViewById(R.id.inputLayoutRegPassword);

        // Setup reactive focus loss validation listeners
        setupRegistrationValidationListeners();

        // Toggle Switchers
        tvSwitchToRegister.setOnClickListener(v -> showRegisterScreen());
        layoutBackToLogin.setOnClickListener(v -> showLoginScreen());

        // Date picker dialog for Child's DOB removed

        // Submit Listeners
        btnSubmit.setOnClickListener(v -> handleLogin());
        btnSubmitRegister.setOnClickListener(v -> handleRegistration());

        // Forgot password dialog trigger
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        // Developer Quick Bypass Buttons for quick role testing
        Button btnBypassAdmin = findViewById(R.id.btnBypassAdmin);
        Button btnBypassDokter = findViewById(R.id.btnBypassDokter);
        Button btnBypassPasien = findViewById(R.id.btnBypassPasien);

        btnBypassAdmin.setOnClickListener(v -> {
            etUsername.setText("admin");
            etPassword.setText("admin");
            cbRememberMe.setChecked(true);
            handleLogin();
        });

        btnBypassDokter.setOnClickListener(v -> {
            etUsername.setText("dokter");
            etPassword.setText("dokter");
            cbRememberMe.setChecked(true);
            handleLogin();
        });

        btnBypassPasien.setOnClickListener(v -> {
            etUsername.setText("pasien");
            etPassword.setText("pasien");
            cbRememberMe.setChecked(true);
            handleLogin();
        });
    }

    private void showRegisterScreen() {
        layoutLogin.setVisibility(View.GONE);
        layoutRegister.setVisibility(View.VISIBLE);
    }

    private void showLoginScreen() {
        layoutRegister.setVisibility(View.GONE);
        layoutLogin.setVisibility(View.VISIBLE);
    }

    // showChildDobPicker removed

    private void handleLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both username and password!", Toast.LENGTH_SHORT).show();
            return;
        }

        Cursor cursor = dbHelper.checkUserLogin(username, password);
        if (cursor != null && cursor.moveToFirst()) {
            int idIdx = cursor.getColumnIndex(DatabaseHelper.KEY_ID);
            int userIdx = cursor.getColumnIndex(DatabaseHelper.KEY_USER_USERNAME);
            int roleIdx = cursor.getColumnIndex(DatabaseHelper.KEY_USER_ROLE);
            
            int parentIdx = cursor.getColumnIndex(DatabaseHelper.KEY_USER_PARENT_NAME);
            int phoneIdx = cursor.getColumnIndex(DatabaseHelper.KEY_USER_PHONE);
            int childIdx = cursor.getColumnIndex(DatabaseHelper.KEY_USER_CHILD_NAME);
            int dobIdx = cursor.getColumnIndex(DatabaseHelper.KEY_USER_CHILD_DOB);

            if (idIdx >= 0 && userIdx >= 0 && roleIdx >= 0) {
                int userId = cursor.getInt(idIdx);
                String userVal = cursor.getString(userIdx);
                String roleVal = cursor.getString(roleIdx);

                // Save session in SharedPreferences
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putInt(KEY_USER_ID, userId);
                editor.putString(KEY_USERNAME, userVal);
                editor.putString(KEY_ROLE, roleVal);
                
                // Save remember me preference
                boolean remember = cbRememberMe.isChecked();
                editor.putBoolean("remember_me", remember);
                if (remember) {
                    editor.putString("saved_username", username);
                } else {
                    editor.remove("saved_username");
                }
                
                // Cache database profiles in SharedPreferences active session
                if (parentIdx >= 0 && cursor.getString(parentIdx) != null) {
                    editor.putString("parentName_" + userVal, cursor.getString(parentIdx));
                }
                if (phoneIdx >= 0 && cursor.getString(phoneIdx) != null) {
                    editor.putString("parentPhone_" + userVal, cursor.getString(phoneIdx));
                }
                if (childIdx >= 0 && cursor.getString(childIdx) != null) {
                    editor.putString("childName_" + userVal, cursor.getString(childIdx));
                }
                if (dobIdx >= 0 && cursor.getString(dobIdx) != null) {
                    editor.putString("childDob_" + userVal, cursor.getString(dobIdx));
                }

                // If it's a default user, save fallback display names
                if (userVal.equalsIgnoreCase("pasien")) {
                    editor.putString(KEY_PARENT_NAME, "Budi Santoso");
                    editor.putString(KEY_CHILD_NAME, "Andi");
                    editor.putString("parentPhone_pasien", "+62 812-3456-7890");
                    editor.putString("childDob_pasien", "2022-10-15");
                }
                editor.apply();

                Toast.makeText(this, "Welcome, " + userVal + "!", Toast.LENGTH_SHORT).show();
                redirectUser(roleVal);
                finish();
            } else {
                Toast.makeText(this, "Failed to process login session.", Toast.LENGTH_SHORT).show();
            }
            cursor.close();
        } else {
            Toast.makeText(this, "Incorrect Username/Email or Password!", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleRegistration() {
        String name = etRegName.getText().toString().trim();
        String email = etRegEmail.getText().toString().trim();
        String phone = etRegPhone.getText().toString().trim();
        String password = etRegPassword.getText().toString().trim();

        // Reset layouts error state
        inputLayoutRegName.setError(null);
        inputLayoutRegEmail.setError(null);
        inputLayoutRegPhone.setError(null);
        inputLayoutRegPassword.setError(null);

        boolean hasError = false;

        if (name.isEmpty()) {
            inputLayoutRegName.setError("Full Name is required!");
            hasError = true;
        }
        if (email.isEmpty()) {
            inputLayoutRegEmail.setError("Email is required!");
            hasError = true;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputLayoutRegEmail.setError("Invalid Email format (e.g. example@gmail.com)!");
            hasError = true;
        }
        if (phone.isEmpty()) {
            inputLayoutRegPhone.setError("Phone number is required!");
            hasError = true;
        } else if (!phone.matches("^[0-9+]{10,14}$")) {
            inputLayoutRegPhone.setError("Invalid Phone number (must be 10-14 digits)!");
            hasError = true;
        }
        if (password.isEmpty()) {
            inputLayoutRegPassword.setError("Password is required!");
            hasError = true;
        } else if (password.length() < 6) {
            inputLayoutRegPassword.setError("Password must be at least 6 characters!");
            hasError = true;
        }

        if (hasError) {
            Toast.makeText(this, "Please complete all fields correctly!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if username (email) already exists in SQLite
        Cursor checkCursor = dbHelper.getUserByUsername(email);
        if (checkCursor != null && checkCursor.getCount() > 0) {
            inputLayoutRegEmail.setError("Email is already registered! Please use another email.");
            Toast.makeText(this, "Email is already registered!", Toast.LENGTH_SHORT).show();
            checkCursor.close();
            return;
        }
        if (checkCursor != null) {
            checkCursor.close();
        }

        // Check if phone number already exists in SQLite
        if (dbHelper.checkPhoneExists(phone)) {
            inputLayoutRegPhone.setError("Phone number is already registered! Please use another number.");
            Toast.makeText(this, "Phone number is already registered!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Register user with empty fields for child details
        boolean success = dbHelper.registerUser(email, password, "pasien", name, phone, "", "");
        if (success) {
            // Save custom parent info in SharedPreferences for display customization
            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
            editor.putString(KEY_PARENT_NAME + "_" + email, name);
            editor.putString("parentPhone_" + email, phone);
            editor.apply();

            Toast.makeText(this, "Registration successful! Please log in.", Toast.LENGTH_LONG).show();
            
            // Clean registration form
            etRegName.setText("");
            etRegEmail.setText("");
            etRegPhone.setText("");
            etRegPassword.setText("");

            showLoginScreen();
        } else {
            Toast.makeText(this, "Registration failed, please try again.", Toast.LENGTH_SHORT).show();
        }
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

    private void showForgotPasswordDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);
        builder.setView(view);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.show();

        // Step layouts
        LinearLayout layoutForgotStep1 = view.findViewById(R.id.layoutForgotStep1);
        LinearLayout layoutForgotStep2 = view.findViewById(R.id.layoutForgotStep2);
        TextView tvForgotSubtitle = view.findViewById(R.id.tvForgotSubtitle);

        // Inputs & buttons
        EditText etForgotEmail = view.findViewById(R.id.etForgotEmail);
        EditText etForgotNewPassword = view.findViewById(R.id.etForgotNewPassword);
        EditText etForgotConfirmPassword = view.findViewById(R.id.etForgotConfirmPassword);
        
        Button btnForgotCancel = view.findViewById(R.id.btnForgotCancel);
        Button btnForgotCancelStep2 = view.findViewById(R.id.btnForgotCancelStep2);
        Button btnForgotNext = view.findViewById(R.id.btnForgotNext);
        Button btnForgotSave = view.findViewById(R.id.btnForgotSave);

        // Cancel buttons dismiss dialog
        btnForgotCancel.setOnClickListener(v -> dialog.dismiss());
        btnForgotCancelStep2.setOnClickListener(v -> dialog.dismiss());

        // Step 1: Check if user exists in database
        btnForgotNext.setOnClickListener(v -> {
            String email = etForgotEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter your registered email/username!", Toast.LENGTH_SHORT).show();
                return;
            }

            Cursor userCursor = dbHelper.getUserByUsername(email);
            if (userCursor != null && userCursor.moveToFirst()) {
                userCursor.close();
                // User exists, toggle to Step 2
                layoutForgotStep1.setVisibility(View.GONE);
                layoutForgotStep2.setVisibility(View.VISIBLE);
                tvForgotSubtitle.setText("Step 2: Create a new password for account " + email);
            } else {
                if (userCursor != null) userCursor.close();
                Toast.makeText(LoginActivity.this, "Username/Email is not registered!", Toast.LENGTH_SHORT).show();
            }
        });

        // Step 2: Save the new password
        btnForgotSave.setOnClickListener(v -> {
            String email = etForgotEmail.getText().toString().trim();
            String newPassword = etForgotNewPassword.getText().toString().trim();
            String confirmPassword = etForgotConfirmPassword.getText().toString().trim();

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please fill in all password fields!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPassword.length() < 6) {
                Toast.makeText(LoginActivity.this, "Password must be at least 6 characters!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(LoginActivity.this, "Confirmation password does not match!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update password in SQLite
            boolean success = dbHelper.updateUserPassword(email, newPassword);
            if (success) {
                Toast.makeText(LoginActivity.this, "Password successfully changed! Please log in again.", Toast.LENGTH_LONG).show();
                dialog.dismiss();
            } else {
                Toast.makeText(LoginActivity.this, "Failed to change password.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRegistrationValidationListeners() {
        etRegName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String val = etRegName.getText().toString().trim();
                if (val.isEmpty()) {
                    inputLayoutRegName.setError("Full Name is required!");
                } else {
                    inputLayoutRegName.setError(null);
                }
            }
        });

        etRegEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String val = etRegEmail.getText().toString().trim();
                if (val.isEmpty()) {
                    inputLayoutRegEmail.setError("Email is required!");
                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(val).matches()) {
                    inputLayoutRegEmail.setError("Invalid Email format (e.g. example@gmail.com)!");
                } else {
                    Cursor checkCursor = dbHelper.getUserByUsername(val);
                    if (checkCursor != null && checkCursor.getCount() > 0) {
                        inputLayoutRegEmail.setError("Email is already registered! Please use another email.");
                        checkCursor.close();
                    } else {
                        if (checkCursor != null) checkCursor.close();
                        inputLayoutRegEmail.setError(null);
                    }
                }
            }
        });

        etRegPhone.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String val = etRegPhone.getText().toString().trim();
                if (val.isEmpty()) {
                    inputLayoutRegPhone.setError("Phone number is required!");
                } else if (!val.matches("^[0-9+]{10,14}$")) {
                    inputLayoutRegPhone.setError("Invalid Phone number (must be 10-14 digits)!");
                } else if (dbHelper.checkPhoneExists(val)) {
                    inputLayoutRegPhone.setError("Phone number is already registered! Please use another number.");
                } else {
                    inputLayoutRegPhone.setError(null);
                }
            }
        });

        etRegPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String val = etRegPassword.getText().toString().trim();
                if (val.isEmpty()) {
                    inputLayoutRegPassword.setError("Password is required!");
                } else if (val.length() < 6) {
                    inputLayoutRegPassword.setError("Password must be at least 6 characters!");
                } else {
                    inputLayoutRegPassword.setError(null);
                }
            }
        });

        // Child's validation focus listeners removed
    }
}
