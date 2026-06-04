package com.example.littlecure;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.widget.ProgressBar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashMap;

public class AdminActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private static final String PREFS_NAME = "LittleCurePrefs";

    // Containers
    private View containerAdminDashboard;
    private View containerAdminAppointments;
    private View containerAdminHistory;
    private View containerAdminProfile;
    private BottomNavigationView bottomNavigationAdmin;

    // Stats
    private TextView tvStatTotal;
    private TextView tvStatWaiting;
    private TextView tvStatDoneExam;
    private ProgressBar pbPending;
    private ProgressBar pbCompleted;

    // Filters (Dashboard)
    private ChipGroup chipGroupAdminFilters;
    private Chip chipAll;
    private Chip chipWaiting;
    private Chip chipPayment;

    // Recyclers
    private RecyclerView rvAdminQueue;
    private TextView tvAdminEmptyQueue;
    private RecyclerView rvAdminQueueApp;
    private TextView tvAdminEmptyQueueApp;
    private RecyclerView rvAdminQueueHist;
    private TextView tvAdminEmptyQueueHist;

    private Button btnAdminProfileLogout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Enforce Light Mode only, ignoring system dark mode settings
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        dbHelper = new DatabaseHelper(this);



        // Containers
        containerAdminDashboard = findViewById(R.id.containerAdminDashboard);
        containerAdminAppointments = findViewById(R.id.containerAdminAppointments);
        containerAdminHistory = findViewById(R.id.containerAdminHistory);
        containerAdminProfile = findViewById(R.id.containerAdminProfile);
        bottomNavigationAdmin = findViewById(R.id.bottomNavigationAdmin);

        // Stats
        tvStatTotal = findViewById(R.id.tvStatTotal);
        tvStatWaiting = findViewById(R.id.tvStatWaiting);
        tvStatDoneExam = findViewById(R.id.tvStatDoneExam);
        pbPending = findViewById(R.id.pbPending);
        pbCompleted = findViewById(R.id.pbCompleted);

        // Filters (Dashboard)
        chipGroupAdminFilters = findViewById(R.id.chipGroupAdminFilters);
        chipAll = findViewById(R.id.chipAll);
        chipWaiting = findViewById(R.id.chipWaiting);
        chipPayment = findViewById(R.id.chipPayment);

        // Recyclers
        rvAdminQueue = findViewById(R.id.rvAdminQueue);
        tvAdminEmptyQueue = findViewById(R.id.tvAdminEmptyQueue);
        rvAdminQueue.setLayoutManager(new LinearLayoutManager(this));

        rvAdminQueueApp = findViewById(R.id.rvAdminQueueApp);
        tvAdminEmptyQueueApp = findViewById(R.id.tvAdminEmptyQueueApp);
        rvAdminQueueApp.setLayoutManager(new LinearLayoutManager(this));

        rvAdminQueueHist = findViewById(R.id.rvAdminQueueHist);
        tvAdminEmptyQueueHist = findViewById(R.id.tvAdminEmptyQueueHist);
        rvAdminQueueHist.setLayoutManager(new LinearLayoutManager(this));

        // Profile tab views
        btnAdminProfileLogout = findViewById(R.id.btnAdminProfileLogout);
        btnAdminProfileLogout.setOnClickListener(v -> handleLogout());

        // Filter Actions (Dashboard list)
        chipGroupAdminFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            loadQueueData();
        });

        // Bottom Navigation Controller (4 tabs)
        bottomNavigationAdmin.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_admin_dashboard) {
                containerAdminDashboard.setVisibility(View.VISIBLE);
                containerAdminAppointments.setVisibility(View.GONE);
                containerAdminHistory.setVisibility(View.GONE);
                containerAdminProfile.setVisibility(View.GONE);
                loadStats();
                loadQueueData();
                return true;
            } else if (itemId == R.id.nav_admin_appointments) {
                containerAdminDashboard.setVisibility(View.GONE);
                containerAdminAppointments.setVisibility(View.VISIBLE);
                containerAdminHistory.setVisibility(View.GONE);
                containerAdminProfile.setVisibility(View.GONE);
                loadQueueData();
                return true;
            } else if (itemId == R.id.nav_admin_history) {
                containerAdminDashboard.setVisibility(View.GONE);
                containerAdminAppointments.setVisibility(View.GONE);
                containerAdminHistory.setVisibility(View.VISIBLE);
                containerAdminProfile.setVisibility(View.GONE);
                loadQueueData();
                return true;
            } else if (itemId == R.id.nav_admin_profile) {
                containerAdminDashboard.setVisibility(View.GONE);
                containerAdminAppointments.setVisibility(View.GONE);
                containerAdminHistory.setVisibility(View.GONE);
                containerAdminProfile.setVisibility(View.VISIBLE);
                return true;
            }
            return false;
        });

        // Initial load
        loadStats();
        loadQueueData();
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

        Toast.makeText(this, "Admin logged out.", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }



    private void loadStats() {
        // Query database stats counts for pending and completed sessions
        Cursor pendingCur = dbHelper.getPendaftaranByStatus(DatabaseHelper.STATUS_WAITING);
        int pending = pendingCur != null ? pendingCur.getCount() : 0;
        if (pendingCur != null) pendingCur.close();

        Cursor completedCur = dbHelper.getPendaftaranByStatus(DatabaseHelper.STATUS_COMPLETED);
        int completed = completedCur != null ? completedCur.getCount() : 0;
        if (completedCur != null) completedCur.close();

        // Update stats views
        tvStatWaiting.setText(String.valueOf(pending));
        tvStatTotal.setText(String.valueOf(completed));

        // Update horizontal progress bars
        int total = pending + completed;
        if (total > 0) {
            pbPending.setProgress(pending * 100 / total);
            pbCompleted.setProgress(completed * 100 / total);
        } else {
            pbPending.setProgress(0);
            pbCompleted.setProgress(0);
        }
        
        // Dynamic weekly growth calculation based on SQLite records
        String growthStr = dbHelper.getWeeklyGrowthString();
        tvStatDoneExam.setText(growthStr);
    }

    private void loadQueueData() {
        // Tab 1: Dashboard Recycler
        // Dashboard displays pending actions (Waiting verification + Selesai Pemeriksaan / Pending Payment)
        ArrayList<HashMap<String, String>> dashboardList = new ArrayList<>();
        Cursor cursor = null;
        if (chipWaiting.isChecked()) {
            cursor = dbHelper.getPendaftaranByStatus(DatabaseHelper.STATUS_WAITING);
        } else if (chipPayment.isChecked()) {
            cursor = dbHelper.getPendaftaranByStatus(DatabaseHelper.STATUS_PAID_PENDING);
        } else {
            cursor = dbHelper.getPendaftaranByStatusOr(DatabaseHelper.STATUS_WAITING, DatabaseHelper.STATUS_PAID_PENDING);
        }
        populateList(cursor, dashboardList);
        setupRecycler(rvAdminQueue, tvAdminEmptyQueue, dashboardList);

        // Tab 2: Appointments Recycler
        // Appointments tab shows active queues currently inside clinic (Verified + Checking)
        ArrayList<HashMap<String, String>> appList = new ArrayList<>();
        Cursor appCursor = dbHelper.getPendaftaranByStatusOr(DatabaseHelper.STATUS_VERIFIED, DatabaseHelper.STATUS_CHECKING);
        populateList(appCursor, appList);
        setupRecycler(rvAdminQueueApp, tvAdminEmptyQueueApp, appList);

        // Tab 3: History Recycler
        // History tab shows fully completed visits (Completed)
        ArrayList<HashMap<String, String>> histList = new ArrayList<>();
        Cursor histCursor = dbHelper.getPendaftaranByStatus(DatabaseHelper.STATUS_COMPLETED);
        populateList(histCursor, histList);
        setupRecycler(rvAdminQueueHist, tvAdminEmptyQueueHist, histList);
    }

    private void populateList(Cursor cursor, ArrayList<HashMap<String, String>> targetList) {
        if (cursor != null) {
            int idIdx = cursor.getColumnIndex(DatabaseHelper.KEY_ID);
            int userIdx = cursor.getColumnIndex(DatabaseHelper.KEY_USER_USERNAME);
            int dateIdx = cursor.getColumnIndex(DatabaseHelper.KEY_PEND_TANGGAL);
            int complaintIdx = cursor.getColumnIndex(DatabaseHelper.KEY_PEND_KELUHAN);
            int paymentIdx = cursor.getColumnIndex(DatabaseHelper.KEY_PEND_JENIS_BAYAR);
            int statusIdx = cursor.getColumnIndex(DatabaseHelper.KEY_PEND_STATUS);

            // Fetch profile indices
            int parentIdx = cursor.getColumnIndex(DatabaseHelper.KEY_USER_PARENT_NAME);
            int phoneIdx = cursor.getColumnIndex(DatabaseHelper.KEY_USER_PHONE);
            int childIdx = cursor.getColumnIndex(DatabaseHelper.KEY_USER_CHILD_NAME);
            int dobIdx = cursor.getColumnIndex(DatabaseHelper.KEY_USER_CHILD_DOB);
            int antreanIdx = cursor.getColumnIndex(DatabaseHelper.KEY_PEND_NO_ANTREAN);

            if (idIdx >= 0 && userIdx >= 0 && dateIdx >= 0 && complaintIdx >= 0 && paymentIdx >= 0 && statusIdx >= 0) {
                while (cursor.moveToNext()) {
                    HashMap<String, String> map = new HashMap<>();
                    map.put("id", String.valueOf(cursor.getInt(idIdx)));
                    map.put("username", cursor.getString(userIdx));
                    map.put("date", cursor.getString(dateIdx));
                    map.put("complaint", cursor.getString(complaintIdx));
                    map.put("payment", cursor.getString(paymentIdx));
                    map.put("status", cursor.getString(statusIdx));

                    // Add profile properties
                    map.put("parent_name", parentIdx >= 0 && cursor.getString(parentIdx) != null ? cursor.getString(parentIdx) : "");
                    map.put("phone", phoneIdx >= 0 && cursor.getString(phoneIdx) != null ? cursor.getString(phoneIdx) : "");
                    map.put("child_name", childIdx >= 0 && cursor.getString(childIdx) != null ? cursor.getString(childIdx) : "");
                    map.put("child_dob", dobIdx >= 0 && cursor.getString(dobIdx) != null ? cursor.getString(dobIdx) : "");
                    map.put("no_antrean", antreanIdx >= 0 && cursor.getString(antreanIdx) != null ? cursor.getString(antreanIdx) : "");

                    targetList.add(map);
                }
            }
            cursor.close();
        }
    }

    private void setupRecycler(RecyclerView rv, TextView tvEmpty, ArrayList<HashMap<String, String>> dataList) {
        if (dataList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rv.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rv.setVisibility(View.VISIBLE);
            AdminQueueAdapter adapter = new AdminQueueAdapter(dataList);
            rv.setAdapter(adapter);
        }
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

    // RecyclerView Adapter for Admin Queue management
    private class AdminQueueAdapter extends RecyclerView.Adapter<AdminQueueAdapter.ViewHolder> {

        private final ArrayList<HashMap<String, String>> list;

        public AdminQueueAdapter(ArrayList<HashMap<String, String>> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_queue, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HashMap<String, String> item = list.get(position);
            int id = Integer.parseInt(item.get("id"));
            String status = item.get("status");
            String username = item.get("username");
            String date = item.get("date");
            String complaint = item.get("complaint");
            String payment = item.get("payment");

            // Load properties directly from database map
            String parentName = item.get("parent_name");
            if (parentName == null || parentName.trim().isEmpty()) {
                parentName = username;
            }
            String phone = item.get("phone");
            String childName = item.get("child_name");
            String noAntrean = item.get("no_antrean");

            holder.tvPatientName.setText(parentName + " (Child: " + (childName.isEmpty() ? "-" : childName) + ")");
            holder.tvComplaint.setText("Complaint: " + complaint);
            holder.tvPayment.setText("Method: " + translatePaymentMethod(payment));

            // Display queue number inside date field if available
            if (noAntrean != null && !noAntrean.trim().isEmpty()) {
                holder.tvDate.setText("Visit Date: " + date + " | Queue No: " + noAntrean);
            } else {
                holder.tvDate.setText("Visit Date: " + date);
            }

            // Set Avatar initials
            holder.tvAvatar.setText(getInitials(parentName));

            // Set formatted status texts matching Tailwind mockup
            if (status.equalsIgnoreCase(DatabaseHelper.STATUS_WAITING)) {
                holder.tvStatus.setText("Pending Verification");
            } else if (status.equalsIgnoreCase(DatabaseHelper.STATUS_EXAM_DONE)) {
                holder.tvStatus.setText("Pending Payment");
            } else if (status.equalsIgnoreCase(DatabaseHelper.STATUS_PAID_PENDING)) {
                holder.tvStatus.setText("Paid (Pending Confirmation)");
            } else if (status.equalsIgnoreCase(DatabaseHelper.STATUS_VERIFIED)) {
                holder.tvStatus.setText("Verified / In Queue");
            } else {
                holder.tvStatus.setText(status);
            }

            // Style badge
            styleStatusBadge(holder.tvStatus, status);

            // Manage actions and details based on status
            if (status.equalsIgnoreCase(DatabaseHelper.STATUS_WAITING)) {
                holder.layoutAdminMedDetails.setVisibility(View.GONE);
                holder.btnAction.setVisibility(View.VISIBLE);
                holder.btnAction.setText("Verify");
                holder.btnAction.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                holder.btnAction.setOnClickListener(v -> {
                    // Generate queue ticket e.g., A-001, A-002...
                    String ticket = "A-" + String.format("%03d", id);
                    dbHelper.verifyPendaftaran(id, ticket);
                    Toast.makeText(AdminActivity.this, "Registration successfully verified! Queue Number: " + ticket, Toast.LENGTH_SHORT).show();
                    loadStats();
                    loadQueueData();
                });
            } else if (status.equalsIgnoreCase(DatabaseHelper.STATUS_PAID_PENDING)) {
                holder.layoutAdminMedDetails.setVisibility(View.VISIBLE);
                
                // Fetch medical record
                Cursor rmCursor = dbHelper.getRekamMedisByPendaftaran(id);
                if (rmCursor != null && rmCursor.moveToFirst()) {
                    int diagIdx = rmCursor.getColumnIndex(DatabaseHelper.KEY_RM_DIAGNOSA);
                    int rxIdx = rmCursor.getColumnIndex(DatabaseHelper.KEY_RM_RESEP_OBAT);
                    if (diagIdx >= 0 && rxIdx >= 0) {
                        holder.tvAdminDiagnosis.setText("Diagnosis: " + rmCursor.getString(diagIdx));
                        holder.tvAdminPrescription.setText("Prescription: " + rmCursor.getString(rxIdx));
                    }
                    rmCursor.close();
                } else {
                    holder.tvAdminDiagnosis.setText("Diagnosis: -");
                    holder.tvAdminPrescription.setText("Prescription: -");
                }

                holder.btnAction.setVisibility(View.VISIBLE);
                holder.btnAction.setText("Confirm Payment");
                holder.btnAction.setBackgroundColor(getResources().getColor(R.color.status_completed_text));
                holder.btnAction.setOnClickListener(v -> {
                    dbHelper.updatePendaftaranStatus(id, DatabaseHelper.STATUS_COMPLETED);
                    Toast.makeText(AdminActivity.this, "Payment confirmed. Done!", Toast.LENGTH_SHORT).show();
                    loadStats();
                    loadQueueData();
                });
            } else {
                holder.layoutAdminMedDetails.setVisibility(View.GONE);
                holder.btnAction.setVisibility(View.GONE);
            }

            // WhatsApp intent trigger for contact
            if (phone != null && !phone.trim().isEmpty()) {
                holder.btnContact.setVisibility(View.VISIBLE);
                final String finalParentName = parentName;
                final String finalChildName = childName;
                final String finalNoAntrean = noAntrean;
                holder.btnContact.setOnClickListener(v -> {
                    try {
                        String cleanPhone = phone.replaceAll("[^0-9+]", "");
                        if (!cleanPhone.startsWith("+")) {
                            if (cleanPhone.startsWith("0")) {
                                cleanPhone = "+62" + cleanPhone.substring(1);
                            } else if (!cleanPhone.startsWith("62")) {
                                cleanPhone = "+62" + cleanPhone;
                            } else {
                                cleanPhone = "+" + cleanPhone;
                            }
                        }

                        String childDisp = finalChildName.isEmpty() ? "your child" : "Child " + finalChildName;
                        String antreanDisp = (finalNoAntrean != null && !finalNoAntrean.trim().isEmpty()) ? finalNoAntrean : ("A-" + String.format("%03d", id));
                        String message = "Hello Mr./Ms. " + finalParentName + ", the registration for " + childDisp + " for the doctor's visit at LittleCure has been verified. Your Queue Number is: " + antreanDisp + ". Please arrive at the clinic. Thank you!";

                        String url = "https://api.whatsapp.com/send?phone=" + cleanPhone + "&text=" + Uri.encode(message);
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(AdminActivity.this, "Failed to open WhatsApp: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                holder.btnContact.setVisibility(View.GONE);
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

        @Override
        public int getItemCount() {
            return list.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvPatientName, tvStatus, tvDate, tvComplaint, tvPayment, tvAvatar;
            TextView tvAdminDiagnosis, tvAdminPrescription;
            LinearLayout layoutAdminMedDetails;
            Button btnAction;
            ImageButton btnContact;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvPatientName = itemView.findViewById(R.id.tvAdminQueuePatientName);
                tvStatus = itemView.findViewById(R.id.tvAdminQueueStatus);
                tvDate = itemView.findViewById(R.id.tvAdminQueueDate);
                tvComplaint = itemView.findViewById(R.id.tvAdminQueueComplaint);
                tvPayment = itemView.findViewById(R.id.tvAdminQueuePayment);
                tvAvatar = itemView.findViewById(R.id.tvAdminQueueAvatar);
                
                layoutAdminMedDetails = itemView.findViewById(R.id.layoutAdminMedDetails);
                tvAdminDiagnosis = itemView.findViewById(R.id.tvAdminDiagnosis);
                tvAdminPrescription = itemView.findViewById(R.id.tvAdminPrescription);
                
                btnAction = itemView.findViewById(R.id.btnAdminAction);
                btnContact = itemView.findViewById(R.id.btnAdminContact);
            }
        }
    }
}
