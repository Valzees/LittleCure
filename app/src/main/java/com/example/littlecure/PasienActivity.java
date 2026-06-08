package com.example.littlecure;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import android.graphics.pdf.PdfDocument;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PasienActivity extends AppCompatActivity {

    private TextView tvWelcomePatient;
    private TextView tvParentAvatar;

    // Containers (3 tabs)
    private NestedScrollView containerDashboard, containerRiwayat, containerProfile;
    private BottomNavigationView bottomNavigation;

    // Form 1 Toggle
    private View btnBookAppointmentToggle;

    // Active Card & Status
    private View layoutActiveStepper;
    private View cardActiveQueue;
    private TextView tvActiveChildName;
    private TextView tvActiveQueueNumber;
    private TextView tvActiveStatus, tvActiveDate, tvActiveComplaint, tvActivePayment, tvEmptyStatus;
    private LinearLayout layoutMedicalResult;
    private TextView tvActiveDiagnosis, tvActivePrescription;
    private TextView step1Circle, step2Circle, step3Circle, step4Circle;
    private Button btnActivePay;
    private TextView tvPaymentSubmittedStatus;

    // History Lists
    private RecyclerView rvMedicalHistoryPreview, rvMedicalHistory;
    private TextView tvEmptyHistoryPreview, tvEmptyHistory;
    private MedicalHistoryAdapter historyAdapter;

    // Profile Fields
    private TextView tvProfileParentName, tvProfileEmail, tvProfilePhone;
    private Button btnProfileLogout;

    private DatabaseHelper dbHelper;
    private int currentUserId;
    private String currentUsername;

    private static final String PREFS_NAME = "LittleCurePrefs";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Enforce Light Mode only, ignoring system dark mode settings
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pasien);

        dbHelper = new DatabaseHelper(this);

        // Get user session
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        currentUserId = prefs.getInt(KEY_USER_ID, -1);
        currentUsername = prefs.getString(KEY_USERNAME, "Pasien");

        if (currentUserId == -1) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Fetch personalized names
        String parentDisplayName = prefs.getString("parentName_" + currentUsername, "");
        if (parentDisplayName.isEmpty()) {
            parentDisplayName = prefs.getString("parentName", currentUsername);
        }

        String childDisplayName = prefs.getString("childName_" + currentUsername, "");
        if (childDisplayName.isEmpty()) {
            childDisplayName = prefs.getString("childName", "");
        }

        // Init Header
        tvWelcomePatient = findViewById(R.id.tvWelcomePatient);
        tvWelcomePatient.setText("Hello, " + parentDisplayName + "!");
        tvParentAvatar = findViewById(R.id.tvParentAvatar);
        tvParentAvatar.setText(getInitials(parentDisplayName));

        // Init Containers (3 tabs)
        containerDashboard = findViewById(R.id.containerDashboard);
        containerRiwayat = findViewById(R.id.containerRiwayat);
        containerProfile = findViewById(R.id.containerProfile);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Book appointment CTA toggle triggers custom pop-up dialog
        btnBookAppointmentToggle = findViewById(R.id.btnBookAppointmentToggle);
        btnBookAppointmentToggle.setOnClickListener(v -> showBookAppointmentDialog());

        // Init Active Queue components
        layoutActiveStepper = findViewById(R.id.layoutActiveStepper);
        cardActiveQueue = findViewById(R.id.cardActiveQueue);
        tvActiveChildName = findViewById(R.id.tvActiveChildName);
        tvActiveQueueNumber = findViewById(R.id.tvActiveQueueNumber);
        tvActiveStatus = findViewById(R.id.tvActiveStatus);
        tvActiveDate = findViewById(R.id.tvActiveDate);
        tvActiveComplaint = findViewById(R.id.tvActiveComplaint);
        tvActivePayment = findViewById(R.id.tvActivePayment);
        tvEmptyStatus = findViewById(R.id.tvEmptyStatus);
        layoutMedicalResult = findViewById(R.id.layoutMedicalResult);
        tvActiveDiagnosis = findViewById(R.id.tvActiveDiagnosis);
        tvActivePrescription = findViewById(R.id.tvActivePrescription);
        btnActivePay = findViewById(R.id.btnActivePay);
        tvPaymentSubmittedStatus = findViewById(R.id.tvPaymentSubmittedStatus);

        // Init Stepper views
        step1Circle = findViewById(R.id.step1Circle);
        step2Circle = findViewById(R.id.step2Circle);
        step3Circle = findViewById(R.id.step3Circle);
        step4Circle = findViewById(R.id.step4Circle);

        // Init History list views
        rvMedicalHistoryPreview = findViewById(R.id.rvMedicalHistoryPreview);
        tvEmptyHistoryPreview = findViewById(R.id.tvEmptyHistoryPreview);
        rvMedicalHistory = findViewById(R.id.rvMedicalHistory);
        tvEmptyHistory = findViewById(R.id.tvEmptyHistory);

        rvMedicalHistoryPreview.setLayoutManager(new LinearLayoutManager(this));
        rvMedicalHistory.setLayoutManager(new LinearLayoutManager(this));

        // View All History Text trigger
        TextView tvViewAllHistory = findViewById(R.id.tvViewAllHistory);
        tvViewAllHistory.setOnClickListener(v -> bottomNavigation.setSelectedItemId(R.id.nav_history));

        // Profile tab views
        tvProfileParentName = findViewById(R.id.tvProfileParentName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfilePhone = findViewById(R.id.tvProfilePhone);
        btnProfileLogout = findViewById(R.id.btnProfileLogout);

        btnProfileLogout.setOnClickListener(v -> handleLogout());

        // Bottom Navigation Controller (3 tabs)
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_dashboard) {
                containerDashboard.setVisibility(View.VISIBLE);
                containerRiwayat.setVisibility(View.GONE);
                containerProfile.setVisibility(View.GONE);
                loadActiveQueue();
                loadMedicalHistory();
                return true;
            } else if (itemId == R.id.nav_history) {
                containerDashboard.setVisibility(View.GONE);
                containerRiwayat.setVisibility(View.VISIBLE);
                containerProfile.setVisibility(View.GONE);
                loadMedicalHistory();
                return true;
            } else if (itemId == R.id.nav_profile) {
                containerDashboard.setVisibility(View.GONE);
                containerRiwayat.setVisibility(View.GONE);
                containerProfile.setVisibility(View.VISIBLE);
                loadProfileData();
                return true;
            }
            return false;
        });

        // Load Initial State
        loadActiveQueue();
        loadMedicalHistory();
    }

    @Override
    public void onBackPressed() {
        handleLogoutSilent();
    }

    private void handleLogoutSilent() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();

        Toast.makeText(this, "Returning to Login", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void handleLogout() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();

        Toast.makeText(this, "You have logged out.", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void showDatePicker(EditText et) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String formattedMonth = String.format("%02d", (monthOfYear + 1));
                    String formattedDay = String.format("%02d", dayOfMonth);
                    et.setText(year1 + "-" + formattedMonth + "-" + formattedDay);
                }, year, month, day);
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void showBookAppointmentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_book_appointment, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);

        // Bind dialog views
        EditText etDialogVisitDate = dialogView.findViewById(R.id.etDialogVisitDate);
        Button btnDialogDatePicker = dialogView.findViewById(R.id.btnDialogDatePicker);
        EditText etDialogChildName = dialogView.findViewById(R.id.etDialogChildName);
        EditText etDialogChildAge = dialogView.findViewById(R.id.etDialogChildAge);
        EditText etDialogComplaint = dialogView.findViewById(R.id.etDialogComplaint);
        RadioGroup rgDialogPayment = dialogView.findViewById(R.id.rgDialogPayment);
        RadioButton rbDialogUmum = dialogView.findViewById(R.id.rbDialogUmum);
        Button btnDialogCancel = dialogView.findViewById(R.id.btnDialogCancel);
        Button btnDialogSubmit = dialogView.findViewById(R.id.btnDialogSubmit);

        // Pre-fill child name and age/DOB from SQLite/prefs if available as fallback, but keep editable
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String childDisplayName = prefs.getString("childName_" + currentUsername, "");
        if (childDisplayName.isEmpty()) {
            childDisplayName = prefs.getString("childName", "");
        }
        etDialogChildName.setText(childDisplayName);

        String childDisplayAge = prefs.getString("childDob_" + currentUsername, "");
        etDialogChildAge.setText(childDisplayAge);

        // Setup date picker
        btnDialogDatePicker.setOnClickListener(v -> showDatePicker(etDialogVisitDate));
        etDialogVisitDate.setOnClickListener(v -> showDatePicker(etDialogVisitDate));

        // Cancel confirmation
        btnDialogCancel.setOnClickListener(v -> {
            String childNameInput = etDialogChildName.getText().toString().trim();
            String childAgeInput = etDialogChildAge.getText().toString().trim();
            String complaintInput = etDialogComplaint.getText().toString().trim();

            if (!childNameInput.isEmpty() || !childAgeInput.isEmpty() || !complaintInput.isEmpty()) {
                new AlertDialog.Builder(this)
                        .setTitle("Cancel Registration?")
                        .setMessage("Are you sure you want to cancel? All entered data will be lost.")
                        .setPositiveButton("Yes", (dialogInterface, i) -> dialog.dismiss())
                        .setNegativeButton("No", null)
                        .show();
            } else {
                dialog.dismiss();
            }
        });

        // Submit registration
        btnDialogSubmit.setOnClickListener(v -> {
            String visitDate = etDialogVisitDate.getText().toString().trim();
            String childName = etDialogChildName.getText().toString().trim();
            String childAge = etDialogChildAge.getText().toString().trim();
            String complaint = etDialogComplaint.getText().toString().trim();
            String payment = rbDialogUmum.isChecked() ? "Cash" : "Insurance";

            if (visitDate.isEmpty()) {
                etDialogVisitDate.setError("Please select a visit date!");
                Toast.makeText(this, "Please select a visit date!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (childName.isEmpty()) {
                etDialogChildName.setError("Child's name is required!");
                return;
            }
            if (childAge.isEmpty()) {
                etDialogChildAge.setError("Child's age is required!");
                return;
            }
            if (complaint.isEmpty()) {
                etDialogComplaint.setError("Child's complaint is required!");
                return;
            }

            // Database concatenation
            String finalComplaint = childName + " (" + childAge + ") - " + complaint;

            // Update user child details permanently in database and SharedPreferences session
            dbHelper.updateUserChildDetails(currentUsername, childName, childAge);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("childName_" + currentUsername, childName);
            editor.putString("childDob_" + currentUsername, childAge);
            editor.apply();

            boolean success = dbHelper.addPendaftaran(currentUserId, visitDate, finalComplaint, payment);
            if (success) {
                Toast.makeText(this, "Registration submitted successfully!", Toast.LENGTH_LONG).show();
                dialog.dismiss();
                loadActiveQueue();
                loadMedicalHistory();
            } else {
                Toast.makeText(this, "Registration failed, please try again.", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "PA";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        } else {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
    }

    private String translatePaymentMethod(String rawPayment) {
        if (rawPayment == null) return "-";
        if (rawPayment.equalsIgnoreCase("Umum")) return "Cash";
        if (rawPayment.equalsIgnoreCase("Asuransi")) return "Insurance";
        return rawPayment;
    }

    private void updateStepperStatus(String status) {
        if (status.equalsIgnoreCase(DatabaseHelper.STATUS_WAITING)) {
            step1Circle.setBackgroundResource(R.drawable.stepper_circle_active);
            step2Circle.setBackgroundResource(R.drawable.stepper_circle_inactive);
            step3Circle.setBackgroundResource(R.drawable.stepper_circle_inactive);
            step4Circle.setBackgroundResource(R.drawable.stepper_circle_inactive);
        } else if (status.equalsIgnoreCase(DatabaseHelper.STATUS_VERIFIED)) {
            step1Circle.setBackgroundResource(R.drawable.stepper_circle_completed);
            step2Circle.setBackgroundResource(R.drawable.stepper_circle_active);
            step3Circle.setBackgroundResource(R.drawable.stepper_circle_inactive);
            step4Circle.setBackgroundResource(R.drawable.stepper_circle_inactive);
        } else if (status.equalsIgnoreCase(DatabaseHelper.STATUS_CHECKING)) {
            step1Circle.setBackgroundResource(R.drawable.stepper_circle_completed);
            step2Circle.setBackgroundResource(R.drawable.stepper_circle_completed);
            step3Circle.setBackgroundResource(R.drawable.stepper_circle_active);
            step4Circle.setBackgroundResource(R.drawable.stepper_circle_inactive);
        } else if (status.equalsIgnoreCase(DatabaseHelper.STATUS_EXAM_DONE) || status.equalsIgnoreCase(DatabaseHelper.STATUS_PAID_PENDING)) {
            step1Circle.setBackgroundResource(R.drawable.stepper_circle_completed);
            step2Circle.setBackgroundResource(R.drawable.stepper_circle_completed);
            step3Circle.setBackgroundResource(R.drawable.stepper_circle_completed);
            step4Circle.setBackgroundResource(R.drawable.stepper_circle_active);
        } else if (status.equalsIgnoreCase(DatabaseHelper.STATUS_COMPLETED)) {
            step1Circle.setBackgroundResource(R.drawable.stepper_circle_completed);
            step2Circle.setBackgroundResource(R.drawable.stepper_circle_completed);
            step3Circle.setBackgroundResource(R.drawable.stepper_circle_completed);
            step4Circle.setBackgroundResource(R.drawable.stepper_circle_completed);
        }
    }

    private void loadActiveQueue() {
        Cursor cursor = dbHelper.getActivePendaftaran(currentUserId);
        if (cursor != null && cursor.moveToFirst()) {
            int statusIndex = cursor.getColumnIndex(DatabaseHelper.KEY_PEND_STATUS);
            int dateIndex = cursor.getColumnIndex(DatabaseHelper.KEY_PEND_TANGGAL);
            int complaintIndex = cursor.getColumnIndex(DatabaseHelper.KEY_PEND_KELUHAN);
            int paymentIndex = cursor.getColumnIndex(DatabaseHelper.KEY_PEND_JENIS_BAYAR);
            int idIndex = cursor.getColumnIndex(DatabaseHelper.KEY_ID);
            int noAntreanIndex = cursor.getColumnIndex(DatabaseHelper.KEY_PEND_NO_ANTREAN);

            if (statusIndex >= 0 && dateIndex >= 0 && complaintIndex >= 0 && paymentIndex >= 0 && idIndex >= 0) {
                String status = cursor.getString(statusIndex);
                String date = cursor.getString(dateIndex);
                String complaint = cursor.getString(complaintIndex);
                String payment = cursor.getString(paymentIndex);
                int pendaftaranId = cursor.getInt(idIndex);

                // If the latest pendaftaran is fully complete, treat it as no active queue
                if (status.equalsIgnoreCase(DatabaseHelper.STATUS_COMPLETED)) {
                    layoutActiveStepper.setVisibility(View.GONE);
                    tvEmptyStatus.setVisibility(View.VISIBLE);
                } else {
                    layoutActiveStepper.setVisibility(View.VISIBLE);
                    cardActiveQueue.setVisibility(View.VISIBLE);
                    tvEmptyStatus.setVisibility(View.GONE);

                    tvActiveDate.setText("Visit Date: " + date);
                    tvActiveComplaint.setText("Complaint: " + complaint);
                    tvActivePayment.setText("Payment Method: " + translatePaymentMethod(payment));

                    // Set child's name in active queue details card
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    String childName = prefs.getString("childName_" + currentUsername, "");
                    if (childName.isEmpty()) {
                        childName = prefs.getString("childName", "Active Patient");
                    }
                    if (childName.isEmpty()) {
                        childName = "Active Patient";
                    }
                    tvActiveChildName.setText(childName);

                    // Display assigned queue number
                    if (noAntreanIndex >= 0) {
                        String noAntrean = cursor.getString(noAntreanIndex);
                        if (noAntrean != null && !noAntrean.trim().isEmpty()) {
                            tvActiveQueueNumber.setText("Queue No: " + noAntrean);
                            tvActiveQueueNumber.setVisibility(View.VISIBLE);
                        } else {
                            tvActiveQueueNumber.setVisibility(View.GONE);
                        }
                    } else {
                        tvActiveQueueNumber.setVisibility(View.GONE);
                    }

                    // Map status display to mockup friendly string
                    if (status.equalsIgnoreCase(DatabaseHelper.STATUS_WAITING)) {
                        tvActiveStatus.setText("Pending Verification");
                    } else if (status.equalsIgnoreCase(DatabaseHelper.STATUS_VERIFIED)) {
                        tvActiveStatus.setText("Verified / In Queue");
                    } else if (status.equalsIgnoreCase(DatabaseHelper.STATUS_CHECKING)) {
                        tvActiveStatus.setText("In Examination");
                    } else if (status.equalsIgnoreCase(DatabaseHelper.STATUS_EXAM_DONE)) {
                        tvActiveStatus.setText("Pending Payment");
                    } else if (status.equalsIgnoreCase(DatabaseHelper.STATUS_PAID_PENDING)) {
                        tvActiveStatus.setText("Payment Verification");
                    } else {
                        tvActiveStatus.setText(status);
                    }

                    // Style the status badge
                    styleStatusBadge(tvActiveStatus, status);

                    // Update Stepper circles drawable
                    updateStepperStatus(status);

                    // Prescription is hidden before Admin confirms payment completion
                    layoutMedicalResult.setVisibility(View.GONE);

                    // Simulated Payment Button and status text view visibility handling
                    if (status.equalsIgnoreCase(DatabaseHelper.STATUS_EXAM_DONE)) {
                        btnActivePay.setVisibility(View.VISIBLE);
                        tvPaymentSubmittedStatus.setVisibility(View.GONE);

                        final String finalTicket = (noAntreanIndex >= 0) ? cursor.getString(noAntreanIndex) : "";
                        btnActivePay.setOnClickListener(v -> {
                            int grandTotal = 150000; // Consultation fee
                            Cursor rmCursor = dbHelper.getRekamMedisByPendaftaran(pendaftaranId);
                            if (rmCursor != null && rmCursor.moveToFirst()) {
                                int rxIdx = rmCursor.getColumnIndex(DatabaseHelper.KEY_RM_RESEP_OBAT);
                                if (rxIdx >= 0) {
                                    String rawRx = rmCursor.getString(rxIdx);
                                    if (rawRx != null && !rawRx.trim().isEmpty() && !rawRx.equals("-")) {
                                        String[] meds = rawRx.split(",\\s*");
                                        for (String med : meds) {
                                            String name = med.trim();
                                            int qty = 1;
                                            if (name.contains(" (") && name.endsWith("x)")) {
                                                int openParen = name.lastIndexOf(" (");
                                                String qtyStr = name.substring(openParen + 2, name.length() - 2);
                                                try { qty = Integer.parseInt(qtyStr); } catch (Exception e) {}
                                                name = name.substring(0, openParen).trim();
                                            }
                                            grandTotal += dbHelper.getObatPrice(name) * qty;
                                        }
                                    }
                                }
                                rmCursor.close();
                            }

                            final String displayTicket = (finalTicket == null || finalTicket.trim().isEmpty()) ? ("A-" + String.format("%03d", pendaftaranId)) : finalTicket;
                            final int finalGrandTotal = grandTotal;

                            new AlertDialog.Builder(PasienActivity.this)
                                    .setTitle("Simulated Payment")
                                    .setMessage("Confirm payment of Rp " + String.format("%,d", finalGrandTotal) + " for Ticket " + displayTicket + "?")
                                    .setPositiveButton("Pay", (dialogInterface, i) -> {
                                        boolean ok = dbHelper.updatePendaftaranStatus(pendaftaranId, DatabaseHelper.STATUS_PAID_PENDING);
                                        if (ok) {
                                            Toast.makeText(PasienActivity.this, "Payment Successful! Awaiting Admin confirmation.", Toast.LENGTH_LONG).show();
                                            loadActiveQueue();
                                        } else {
                                            Toast.makeText(PasienActivity.this, "Payment failed.", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        });
                    } else if (status.equalsIgnoreCase(DatabaseHelper.STATUS_PAID_PENDING)) {
                        btnActivePay.setVisibility(View.GONE);
                        tvPaymentSubmittedStatus.setVisibility(View.VISIBLE);
                    } else {
                        btnActivePay.setVisibility(View.GONE);
                        tvPaymentSubmittedStatus.setVisibility(View.GONE);
                    }
                }
            }
            cursor.close();
        } else {
            layoutActiveStepper.setVisibility(View.GONE);
            tvEmptyStatus.setVisibility(View.VISIBLE);
        }
    }

    private void styleStatusBadge(TextView tv, String status) {
        if (status.equalsIgnoreCase(DatabaseHelper.STATUS_WAITING)) {
            tv.setBackgroundResource(R.color.status_waiting_bg);
            tv.setTextColor(getResources().getColor(R.color.status_waiting_text));
        } else if (status.equalsIgnoreCase(DatabaseHelper.STATUS_VERIFIED)) {
            tv.setBackgroundResource(R.color.status_verified_bg);
            tv.setTextColor(getResources().getColor(R.color.status_verified_text));
        } else if (status.equalsIgnoreCase(DatabaseHelper.STATUS_CHECKING)) {
            tv.setBackgroundResource(R.color.status_checking_bg);
            tv.setTextColor(getResources().getColor(R.color.status_checking_text));
        } else if (status.equalsIgnoreCase(DatabaseHelper.STATUS_EXAM_DONE)) {
            tv.setBackgroundResource(R.color.status_exam_done_bg);
            tv.setTextColor(getResources().getColor(R.color.status_exam_done_text));
        } else if (status.equalsIgnoreCase(DatabaseHelper.STATUS_PAID_PENDING)) {
            tv.setBackgroundResource(R.color.status_completed_bg);
            tv.setTextColor(getResources().getColor(R.color.status_completed_text));
        } else {
            tv.setBackgroundResource(R.color.status_completed_bg);
            tv.setTextColor(getResources().getColor(R.color.status_completed_text));
        }
    }

    private void loadMedicalHistory() {
        ArrayList<HashMap<String, String>> historyList = new ArrayList<>();
        Cursor cursor = dbHelper.getMedicalHistory(currentUserId);

        if (cursor != null) {
            int idIdx = cursor.getColumnIndex(DatabaseHelper.KEY_ID);
            int dateIdx = cursor.getColumnIndex(DatabaseHelper.KEY_PEND_TANGGAL);
            int complaintIdx = cursor.getColumnIndex(DatabaseHelper.KEY_PEND_KELUHAN);
            int paymentIdx = cursor.getColumnIndex(DatabaseHelper.KEY_PEND_JENIS_BAYAR);
            int statusIdx = cursor.getColumnIndex(DatabaseHelper.KEY_PEND_STATUS);
            int antreanIdx = cursor.getColumnIndex(DatabaseHelper.KEY_PEND_NO_ANTREAN);
            int diagIdx = cursor.getColumnIndex(DatabaseHelper.KEY_RM_DIAGNOSA);
            int rxIdx = cursor.getColumnIndex(DatabaseHelper.KEY_RM_RESEP_OBAT);

            if (dateIdx >= 0 && complaintIdx >= 0 && paymentIdx >= 0 && statusIdx >= 0 && diagIdx >= 0 && rxIdx >= 0) {
                while (cursor.moveToNext()) {
                    HashMap<String, String> map = new HashMap<>();
                    map.put("id", idIdx >= 0 ? String.valueOf(cursor.getInt(idIdx)) : "");
                    map.put("date", cursor.getString(dateIdx));
                    map.put("complaint", cursor.getString(complaintIdx));
                    map.put("payment", cursor.getString(paymentIdx));
                    map.put("status", cursor.getString(statusIdx));
                    map.put("no_antrean", antreanIdx >= 0 ? cursor.getString(antreanIdx) : "");
                    map.put("diagnosis", cursor.getString(diagIdx));
                    map.put("prescription", cursor.getString(rxIdx));
                    historyList.add(map);
                }
            }
            cursor.close();
        }

        if (historyList.isEmpty()) {
            tvEmptyHistory.setVisibility(View.VISIBLE);
            rvMedicalHistory.setVisibility(View.GONE);
            tvEmptyHistoryPreview.setVisibility(View.VISIBLE);
            rvMedicalHistoryPreview.setVisibility(View.GONE);
        } else {
            tvEmptyHistory.setVisibility(View.GONE);
            rvMedicalHistory.setVisibility(View.VISIBLE);
            tvEmptyHistoryPreview.setVisibility(View.GONE);
            rvMedicalHistoryPreview.setVisibility(View.VISIBLE);

            // Full list adapter
            historyAdapter = new MedicalHistoryAdapter(historyList);
            rvMedicalHistory.setAdapter(historyAdapter);

            // Preview list (limit to 3 latest items to avoid clutter on main Dashboard)
            ArrayList<HashMap<String, String>> previewList = new ArrayList<>();
            for (int i = 0; i < Math.min(3, historyList.size()); i++) {
                previewList.add(historyList.get(i));
            }
            MedicalHistoryAdapter previewAdapter = new MedicalHistoryAdapter(previewList);
            rvMedicalHistoryPreview.setAdapter(previewAdapter);
        }
    }

    private void showMedicalHistoryDetailDialog(HashMap<String, String> item) {
        final AppCompatDialog dialog = new AppCompatDialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_medical_history_detail);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);

        // Styling dialog to occupy full width
        android.view.WindowManager.LayoutParams lp = new android.view.WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = android.view.WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setAttributes(lp);

        TextView tvReceiptMeta = dialog.findViewById(R.id.tvReceiptMeta);
        TextView tvReceiptChildName = dialog.findViewById(R.id.tvReceiptChildName);
        TextView tvReceiptChildAge = dialog.findViewById(R.id.tvReceiptChildAge);
        TextView tvReceiptParentName = dialog.findViewById(R.id.tvReceiptParentName);
        TextView tvReceiptParentPhone = dialog.findViewById(R.id.tvReceiptParentPhone);
        TextView tvReceiptComplaint = dialog.findViewById(R.id.tvReceiptComplaint);
        TextView tvReceiptDiagnosis = dialog.findViewById(R.id.tvReceiptDiagnosis);
        LinearLayout layoutReceiptMedicines = dialog.findViewById(R.id.layoutReceiptMedicines);
        TextView tvReceiptTotalBill = dialog.findViewById(R.id.tvReceiptTotalBill);
        TextView tvReceiptPaymentStatus = dialog.findViewById(R.id.tvReceiptPaymentStatus);
        Button btnReceiptPay = dialog.findViewById(R.id.btnReceiptPay);
        Button btnReceiptDownload = dialog.findViewById(R.id.btnReceiptDownload);
        Button btnReceiptClose = dialog.findViewById(R.id.btnReceiptClose);

        // Parse meta info
        String date = item.get("date");
        String ticket = item.get("no_antrean");
        if (ticket == null || ticket.trim().isEmpty()) {
            ticket = "A-" + String.format("%03d", Integer.parseInt(item.get("id")));
        }
        tvReceiptMeta.setText("Date: " + date + " | Ticket: " + ticket);

        // Fetch parent details from prefs
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String parentDisplayName = prefs.getString("parentName_" + currentUsername, "");
        if (parentDisplayName.isEmpty()) {
            parentDisplayName = prefs.getString("parentName", currentUsername);
        }
        String parentPhone = prefs.getString("parentPhone_" + currentUsername, "");
        if (parentPhone.isEmpty()) {
            parentPhone = prefs.getString("parentPhone_pasien", "+62 812-3456-7890");
        }
        tvReceiptParentName.setText("Parent/Guardian: " + parentDisplayName);
        tvReceiptParentPhone.setText("Phone: " + parentPhone);

        // Chief Complaint format: "ChildName (Age) - ComplaintText"
        String fullComplaint = item.get("complaint");
        String childName = "-";
        String childAge = "-";
        String chiefComplaint = fullComplaint;

        if (fullComplaint != null && fullComplaint.contains(" - ")) {
            String[] parts = fullComplaint.split(" - ", 2);
            chiefComplaint = parts[1];
            String nameAge = parts[0];
            if (nameAge.contains(" (") && nameAge.endsWith(")")) {
                int openParen = nameAge.indexOf(" (");
                childName = nameAge.substring(0, openParen);
                childAge = nameAge.substring(openParen + 2, nameAge.length() - 1);
            } else {
                childName = nameAge;
            }
        }

        // Override childName & childAge with current user profile if available to reflect updates immediately
        String currentChildName = prefs.getString("childName_" + currentUsername, "");
        if (!currentChildName.isEmpty()) {
            childName = currentChildName;
        }
        String currentChildAge = prefs.getString("childDob_" + currentUsername, "");
        if (!currentChildAge.isEmpty()) {
            childAge = currentChildAge;
        }
        tvReceiptChildName.setText("Child Name: " + childName);
        tvReceiptChildAge.setText("Child Age: " + childAge);
        tvReceiptComplaint.setText("Chief Complaint: " + chiefComplaint);
        String status = item.get("status");
        boolean isCompleted = DatabaseHelper.STATUS_COMPLETED.equalsIgnoreCase(status);
        tvReceiptDiagnosis.setText("Diagnosis: " + (item.get("diagnosis") != null ? item.get("diagnosis") : "-"));
        tvReceiptDiagnosis.setVisibility(View.VISIBLE);

        // Billing Calculations
        int consultationFee = 150000;
        int medicinesTotal = 0;

        layoutReceiptMedicines.removeAllViews();

        String rawPrescription = item.get("prescription");
        if (rawPrescription != null && !rawPrescription.trim().isEmpty() && !rawPrescription.equals("-")) {
            String[] meds = rawPrescription.split(",\\s*");
            for (String med : meds) {
                if (med.trim().isEmpty()) continue;

                // Parse quantity e.g. "Paracetamol Syrup (2x)"
                String name = med.trim();
                int qty = 1;
                if (name.contains(" (") && name.endsWith("x)")) {
                    int openParen = name.lastIndexOf(" (");
                    String qtyStr = name.substring(openParen + 2, name.length() - 2);
                    try {
                        qty = Integer.parseInt(qtyStr);
                    } catch (NumberFormatException e) {
                        qty = 1;
                    }
                    name = name.substring(0, openParen).trim();
                }

                // Query price from SQLite
                int unitPrice = dbHelper.getObatPrice(name);
                int cost = unitPrice * qty;
                medicinesTotal += cost;

                if (isCompleted) {
                    // Add itemized row programmatically
                    View rowView = getLayoutInflater().inflate(R.layout.item_receipt_medicine, layoutReceiptMedicines, false);
                    TextView tvMedName = rowView.findViewById(R.id.tvReceiptMedName);
                    TextView tvMedCost = rowView.findViewById(R.id.tvReceiptMedCost);

                    tvMedName.setText("- " + name + " (" + qty + "x) @ Rp " + String.format("%,d", unitPrice));
                    tvMedCost.setText("Rp " + String.format("%,d", cost));

                    layoutReceiptMedicines.addView(rowView);
                }
            }

            if (!isCompleted && medicinesTotal > 0) {
                View rowView = getLayoutInflater().inflate(R.layout.item_receipt_medicine, layoutReceiptMedicines, false);
                TextView tvMedName = rowView.findViewById(R.id.tvReceiptMedName);
                TextView tvMedCost = rowView.findViewById(R.id.tvReceiptMedCost);

                tvMedName.setText("- Medicines Cost");
                tvMedCost.setText("Rp " + String.format("%,d", medicinesTotal));

                layoutReceiptMedicines.addView(rowView);
            }
        } else {
            // No medicines row
            TextView tvNoMeds = new TextView(this);
            tvNoMeds.setText("No medicines prescribed.");
            tvNoMeds.setTextColor(getResources().getColor(R.color.colorTextLight));
            tvNoMeds.setTextSize(13);
            layoutReceiptMedicines.addView(tvNoMeds);
        }

        int grandTotal = consultationFee + medicinesTotal;
        tvReceiptTotalBill.setText("Rp " + String.format("%,d", grandTotal));

        // Payment status & actions handling
        String payment = translatePaymentMethod(item.get("payment"));
        final String displayTicket = ticket;
        final int finalGrandTotal = grandTotal;
        final int pendaftaranId = Integer.parseInt(item.get("id"));

        if (isCompleted) {
            tvReceiptPaymentStatus.setText("STATUS: PAID (" + payment + ")");
            tvReceiptPaymentStatus.setBackgroundResource(R.color.status_completed_bg);
            tvReceiptPaymentStatus.setTextColor(getResources().getColor(R.color.status_completed_text));
            btnReceiptPay.setVisibility(View.GONE);
            btnReceiptDownload.setVisibility(View.VISIBLE);
        } else if (DatabaseHelper.STATUS_PAID_PENDING.equalsIgnoreCase(status)) {
            tvReceiptPaymentStatus.setText("STATUS: PENDING VERIFICATION (" + payment + ")");
            tvReceiptPaymentStatus.setBackgroundResource(R.color.status_waiting_bg);
            tvReceiptPaymentStatus.setTextColor(getResources().getColor(R.color.status_waiting_text));
            btnReceiptPay.setVisibility(View.GONE);
            btnReceiptDownload.setVisibility(View.GONE);
        } else {
            tvReceiptPaymentStatus.setText("STATUS: UNPAID (" + payment + ")");
            tvReceiptPaymentStatus.setBackgroundResource(R.color.status_waiting_bg);
            tvReceiptPaymentStatus.setTextColor(getResources().getColor(R.color.status_waiting_text));
            btnReceiptPay.setVisibility(View.VISIBLE);
            btnReceiptDownload.setVisibility(View.GONE);

            btnReceiptPay.setOnClickListener(v -> {
                new AlertDialog.Builder(PasienActivity.this)
                        .setTitle("Simulated Payment")
                        .setMessage("Confirm payment of Rp " + String.format("%,d", finalGrandTotal) + " for Ticket " + displayTicket + "?")
                        .setPositiveButton("Pay", (dialogInterface, i) -> {
                            boolean ok = dbHelper.updatePendaftaranStatus(pendaftaranId, DatabaseHelper.STATUS_PAID_PENDING);
                            if (ok) {
                                Toast.makeText(PasienActivity.this, "Payment Successful! Awaiting Admin confirmation.", Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                                loadActiveQueue();
                                loadMedicalHistory();
                            } else {
                                Toast.makeText(PasienActivity.this, "Payment failed.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        final String finalTicket = ticket;
        final String finalChildName = childName;
        final String finalChildAge = childAge;
        final String finalChiefComplaint = chiefComplaint;
        final String finalParentDisplayName = parentDisplayName;
        final String finalParentPhone = parentPhone;

        btnReceiptDownload.setOnClickListener(v -> {
            generateReceiptPdf(item, finalChildName, finalChildAge, finalChiefComplaint, finalParentDisplayName, finalParentPhone, finalTicket, consultationFee);
        });

        btnReceiptClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void generateReceiptPdf(Map<String, String> item, String childName, String childAge, 
                                    String chiefComplaint, String parentDisplayName, String parentPhone, 
                                    String ticket, int consultationFee) {
        String date = item.get("date");
        String payment = translatePaymentMethod(item.get("payment"));
        String diagnosis = item.get("diagnosis") != null ? item.get("diagnosis") : "-";
        String rawPrescription = item.get("prescription");

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.parseColor("#4F46E5")); // Sleek Indigo
        titlePaint.setTextSize(22);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setAntiAlias(true);

        Paint subTitlePaint = new Paint();
        subTitlePaint.setColor(Color.GRAY);
        subTitlePaint.setTextSize(10);
        subTitlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        subTitlePaint.setAntiAlias(true);

        Paint headerPaint = new Paint();
        headerPaint.setColor(Color.parseColor("#1F2937")); // Slate gray
        headerPaint.setTextSize(14);
        headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        headerPaint.setAntiAlias(true);

        Paint normalPaint = new Paint();
        normalPaint.setColor(Color.BLACK);
        normalPaint.setTextSize(11);
        normalPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        normalPaint.setAntiAlias(true);

        Paint linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#E5E7EB")); // Light Gray border line
        linePaint.setStrokeWidth(1.5f);

        int y = 60;
        canvas.drawText("LITTLE CURE CLINIC", 50, y, titlePaint);
        y += 18;
        canvas.drawText("Layanan Kesehatan Anak Terpercaya", 50, y, subTitlePaint);
        y += 20;

        canvas.drawLine(50, y, 545, y, linePaint);
        y += 25;

        canvas.drawText("STRUK PEMBAYARAN & REKAM MEDIS", 50, y, headerPaint);
        y += 25;

        canvas.drawText("Tanggal Kunjungan: " + date, 50, y, normalPaint);
        canvas.drawText("Nomor Antrean: " + ticket, 350, y, normalPaint);
        y += 20;

        canvas.drawText("Metode Pembayaran: " + payment, 50, y, normalPaint);
        canvas.drawText("Status: LUNAS", 350, y, normalPaint);
        y += 30;

        canvas.drawLine(50, y, 545, y, linePaint);
        y += 25;

        canvas.drawText("INFORMASI PASIEN", 50, y, headerPaint);
        y += 20;
        canvas.drawText("Nama Anak: " + childName, 50, y, normalPaint);
        canvas.drawText("Umur Anak: " + childAge, 350, y, normalPaint);
        y += 20;
        canvas.drawText("Orang Tua/Wali: " + parentDisplayName, 50, y, normalPaint);
        canvas.drawText("No. Telepon: " + parentPhone, 350, y, normalPaint);
        y += 30;

        canvas.drawLine(50, y, 545, y, linePaint);
        y += 25;

        canvas.drawText("DIAGNOSIS KLINIS", 50, y, headerPaint);
        y += 20;
        canvas.drawText(diagnosis, 50, y, normalPaint);
        y += 30;

        canvas.drawLine(50, y, 545, y, linePaint);
        y += 25;

        canvas.drawText("RINCIAN BIAYA & OBAT", 50, y, headerPaint);
        y += 25;

        canvas.drawText("Biaya Konsultasi Dokter", 50, y, normalPaint);
        canvas.drawText("Rp " + String.format("%,d", consultationFee), 420, y, normalPaint);
        y += 25;

        int medicinesTotal = 0;
        if (rawPrescription != null && !rawPrescription.trim().isEmpty() && !rawPrescription.equals("-")) {
            String[] meds = rawPrescription.split(",\\s*");
            for (String med : meds) {
                if (med.trim().isEmpty()) continue;

                String name = med.trim();
                int qty = 1;
                if (name.contains(" (") && name.endsWith("x)")) {
                    int openParen = name.lastIndexOf(" (");
                    String qtyStr = name.substring(openParen + 2, name.length() - 2);
                    try {
                        qty = Integer.parseInt(qtyStr);
                    } catch (NumberFormatException e) {
                        qty = 1;
                    }
                    name = name.substring(0, openParen).trim();
                }

                int unitPrice = dbHelper.getObatPrice(name);
                int cost = unitPrice * qty;
                medicinesTotal += cost;

                canvas.drawText("- " + name + " (" + qty + "x)", 50, y, normalPaint);
                canvas.drawText("Rp " + String.format("%,d", cost), 420, y, normalPaint);
                y += 20;
            }
        } else {
            canvas.drawText("Tidak ada resep obat.", 50, y, normalPaint);
            y += 20;
        }

        y += 10;
        canvas.drawLine(50, y, 545, y, linePaint);
        y += 25;

        int grandTotal = consultationFee + medicinesTotal;
        Paint boldPaint = new Paint();
        boldPaint.setColor(Color.BLACK);
        boldPaint.setTextSize(12);
        boldPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        boldPaint.setAntiAlias(true);

        canvas.drawText("TOTAL PEMBAYARAN", 50, y, boldPaint);
        canvas.drawText("Rp " + String.format("%,d", grandTotal), 420, y, boldPaint);
        y += 40;

        canvas.drawLine(50, y, 545, y, linePaint);
        y += 25;

        Paint footerPaint = new Paint();
        footerPaint.setColor(Color.GRAY);
        footerPaint.setTextSize(9);
        footerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        footerPaint.setAntiAlias(true);

        canvas.drawText("Terima kasih telah memercayakan kesehatan putra-putri Anda pada Little Cure.", 50, y, footerPaint);
        y += 12;
        canvas.drawText("Struk ini adalah bukti pembayaran yang sah.", 50, y, footerPaint);

        document.finishPage(page);

        // Save file to app's internal cache folder
        File pdfFile = new File(getCacheDir(), "Receipt_" + ticket + ".pdf");

        try {
            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            fos.close();
            document.close();

            // Share/open the PDF via sharesheet
            shareReceiptPdf(pdfFile);

            Toast.makeText(this, "PDF berhasil dibuat!", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("PasienActivity", "Error saving PDF: " + e.getMessage());
            Toast.makeText(this, "Gagal membuat PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            document.close();
        }
    }

    private void shareReceiptPdf(File pdfFile) {
        try {
            Uri pdfUri = androidx.core.content.FileProvider.getUriForFile(
                this, 
                "com.example.littlecure.fileprovider", 
                pdfFile
            );

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(intent, "Download / Cetak Struk PDF"));
        } catch (Exception e) {
            Log.e("PasienActivity", "Error sharing PDF: " + e.getMessage());
            Toast.makeText(this, "Gagal membagikan PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProfileData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String parentDisplayName = prefs.getString("parentName_" + currentUsername, "");
        if (parentDisplayName.isEmpty()) {
            parentDisplayName = prefs.getString("parentName", currentUsername);
        }

        String parentPhone = prefs.getString("parentPhone_" + currentUsername, "");
        if (parentPhone.isEmpty()) {
            parentPhone = prefs.getString("parentPhone_pasien", "+62 812-3456-7890");
        }

        tvProfileParentName.setText(parentDisplayName);
        tvProfileEmail.setText(currentUsername.contains("@") ? currentUsername : currentUsername + "@littlecure.com");
        tvProfilePhone.setText(parentPhone);
    }

    // RecyclerView Adapter for Medical History
    private class MedicalHistoryAdapter extends RecyclerView.Adapter<MedicalHistoryAdapter.ViewHolder> {

        private final ArrayList<HashMap<String, String>> list;

        public MedicalHistoryAdapter(ArrayList<HashMap<String, String>> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medical_history, parent, false);
            return new ViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HashMap<String, String> item = list.get(position);
            holder.tvHistoryDate.setText("Visit Date: " + item.get("date"));
            holder.tvHistoryComplaint.setText("Complaint: " + item.get("complaint"));

            String payment = translatePaymentMethod(item.get("payment"));
            String status = item.get("status");

            String displayDiagnosis = item.get("diagnosis");
            if (displayDiagnosis == null || displayDiagnosis.trim().isEmpty()) {
                displayDiagnosis = "-";
            }
            String displayPrescription;
            if (DatabaseHelper.STATUS_COMPLETED.equalsIgnoreCase(status)) {
                displayPrescription = item.get("prescription");
                holder.tvHistoryPayment.setText("Payment: " + payment + " (Paid)");
                holder.tvHistoryPayment.setBackgroundResource(R.color.status_completed_bg);
                holder.tvHistoryPayment.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.status_completed_text));
            } else if (DatabaseHelper.STATUS_PAID_PENDING.equalsIgnoreCase(status)) {
                displayPrescription = "-";
                holder.tvHistoryPayment.setText("Payment: " + payment + " (Pending Confirmation)");
                holder.tvHistoryPayment.setBackgroundResource(R.color.status_waiting_bg);
                holder.tvHistoryPayment.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.status_waiting_text));
            } else {
                displayPrescription = "-";
                holder.tvHistoryPayment.setText("Payment: " + payment + " (Unpaid)");
                holder.tvHistoryPayment.setBackgroundResource(R.color.status_waiting_bg);
                holder.tvHistoryPayment.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.status_waiting_text));
            }

            holder.tvHistoryDiagnosis.setText("Diagnosis: " + displayDiagnosis);
            holder.tvHistoryPrescription.setText("Prescription: " + displayPrescription);

            // Click card to open dynamic detailed invoice receipt dialog
            holder.itemView.setOnClickListener(v -> {
                showMedicalHistoryDetailDialog(item);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvHistoryDate, tvHistoryPayment, tvHistoryComplaint, tvHistoryDiagnosis, tvHistoryPrescription;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvHistoryDate = itemView.findViewById(R.id.tvHistoryDate);
                tvHistoryPayment = itemView.findViewById(R.id.tvHistoryPayment);
                tvHistoryComplaint = itemView.findViewById(R.id.tvHistoryComplaint);
                tvHistoryDiagnosis = itemView.findViewById(R.id.tvHistoryDiagnosis);
                tvHistoryPrescription = itemView.findViewById(R.id.tvHistoryPrescription);
            }
        }
    }
}
