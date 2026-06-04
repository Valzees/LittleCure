package com.example.littlecure;

import androidx.appcompat.app.AppCompatDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;

public class DoctorActivity extends AppCompatActivity {

    private TextView tvWelcomeDoctor;
    private ImageButton btnDoctorLogout;

    // Recycler
    private RecyclerView rvDoctorQueue;
    private TextView tvDoctorEmptyQueue;
    private DoctorQueueAdapter queueAdapter;

    private DatabaseHelper dbHelper;
    private String currentUsername;

    private static final String PREFS_NAME = "LittleCurePrefs";
    private static final String KEY_USERNAME = "username";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Enforce Light Mode only, ignoring system dark mode settings
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor);

        dbHelper = new DatabaseHelper(this);

        // Get session
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        currentUsername = prefs.getString(KEY_USERNAME, "Dokter");

        // UI Header
        tvWelcomeDoctor = findViewById(R.id.tvWelcomeDoctor);
        tvWelcomeDoctor.setText("dr. " + currentUsername);
        btnDoctorLogout = findViewById(R.id.btnDoctorLogout);
        btnDoctorLogout.setOnClickListener(v -> handleLogout());

        // Queue
        rvDoctorQueue = findViewById(R.id.rvDoctorQueue);
        tvDoctorEmptyQueue = findViewById(R.id.tvDoctorEmptyQueue);
        rvDoctorQueue.setLayoutManager(new LinearLayoutManager(this));

        // Load data
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

        Toast.makeText(this, "Doctor logged out.", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void loadQueueData() {
        ArrayList<HashMap<String, String>> queueList = new ArrayList<>();
        
        // Doctor only sees patients who are "Diverifikasi / Antre" or "Sedang Diperiksa"
        Cursor cursor = dbHelper.getPendaftaranByStatusOr(DatabaseHelper.STATUS_VERIFIED, DatabaseHelper.STATUS_CHECKING);

        if (cursor != null) {
            int idIdx = cursor.getColumnIndex(DatabaseHelper.KEY_ID);
            int userIdx = cursor.getColumnIndex(DatabaseHelper.KEY_USER_USERNAME);
            int dateIdx = cursor.getColumnIndex(DatabaseHelper.KEY_PEND_TANGGAL);
            int complaintIdx = cursor.getColumnIndex(DatabaseHelper.KEY_PEND_KELUHAN);
            int paymentIdx = cursor.getColumnIndex(DatabaseHelper.KEY_PEND_JENIS_BAYAR);
            int statusIdx = cursor.getColumnIndex(DatabaseHelper.KEY_PEND_STATUS);
            int parentIdx = cursor.getColumnIndex(DatabaseHelper.KEY_USER_PARENT_NAME);

            if (idIdx >= 0 && userIdx >= 0 && dateIdx >= 0 && complaintIdx >= 0 && paymentIdx >= 0 && statusIdx >= 0) {
                while (cursor.moveToNext()) {
                    HashMap<String, String> map = new HashMap<>();
                    map.put("id", String.valueOf(cursor.getInt(idIdx)));
                    map.put("username", cursor.getString(userIdx));
                    map.put("date", cursor.getString(dateIdx));
                    map.put("complaint", cursor.getString(complaintIdx));
                    map.put("payment", cursor.getString(paymentIdx));
                    map.put("status", cursor.getString(statusIdx));
                    map.put("parent_name", parentIdx >= 0 && cursor.getString(parentIdx) != null ? cursor.getString(parentIdx) : "");
                    queueList.add(map);
                }
            }
            cursor.close();
        }

        if (queueList.isEmpty()) {
            tvDoctorEmptyQueue.setVisibility(View.VISIBLE);
            rvDoctorQueue.setVisibility(View.GONE);
        } else {
            tvDoctorEmptyQueue.setVisibility(View.GONE);
            rvDoctorQueue.setVisibility(View.VISIBLE);
            queueAdapter = new DoctorQueueAdapter(queueList);
            rvDoctorQueue.setAdapter(queueAdapter);
        }
    }

    private void showExamineDialog(int id, String patientName, String complaint) {
        final AppCompatDialog dialog = new AppCompatDialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_doctor_examine);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);

        // Styling dialog to occupy full width
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setAttributes(lp);

        TextView tvDialogPatientInfo = dialog.findViewById(R.id.tvDialogPatientInfo);
        TextView tvDialogComplaintInfo = dialog.findViewById(R.id.tvDialogComplaintInfo);
        EditText etDialogDiagnosis = dialog.findViewById(R.id.etDialogDiagnosis);

        HorizontalScrollView hsvSelectedMedicines = dialog.findViewById(R.id.hsvSelectedMedicines);
        LinearLayout layoutSelectedMedicines = dialog.findViewById(R.id.layoutSelectedMedicines);
        Button btnAddMedicineChip = dialog.findViewById(R.id.btnAddMedicineChip);
        LinearLayout layoutAutocompleteSearch = dialog.findViewById(R.id.layoutAutocompleteSearch);
        AutoCompleteTextView actvMedicineSearch = dialog.findViewById(R.id.actvMedicineSearch);

        Button btnDialogCancel = dialog.findViewById(R.id.btnDialogCancel);
        Button btnDialogSave = dialog.findViewById(R.id.btnDialogSave);

        tvDialogPatientInfo.setText("Patient: " + patientName);
        tvDialogComplaintInfo.setText("Complaint: " + complaint);

        // Load suggestions for autocomplete search
        dbHelper.createObatTableIfNeeded();
        ArrayList<String> medicineNames = new ArrayList<>();
        Cursor obatCursor = dbHelper.getAllObat();
        if (obatCursor != null) {
            int nameIdx = obatCursor.getColumnIndex("nama");
            if (nameIdx >= 0) {
                while (obatCursor.moveToNext()) {
                    medicineNames.add(obatCursor.getString(nameIdx));
                }
            }
            obatCursor.close();
        }
        
        android.widget.ArrayAdapter<String> searchAdapter = new android.widget.ArrayAdapter<>(
            this, android.R.layout.simple_dropdown_item_1line, medicineNames
        );
        actvMedicineSearch.setAdapter(searchAdapter);

        final ArrayList<String> selectedMedicines = new ArrayList<>();

        // Helper inner class to render chips dynamically
        class ChipRenderer {
            void render() {
                layoutSelectedMedicines.removeAllViews();
                for (final String med : selectedMedicines) {
                    View chipView = LayoutInflater.from(DoctorActivity.this).inflate(
                        R.layout.view_medicine_chip, layoutSelectedMedicines, false
                    );
                    TextView tvChipText = chipView.findViewById(R.id.tvChipText);
                    TextView btnRemoveChip = chipView.findViewById(R.id.btnRemoveChip);

                    tvChipText.setText(med);
                    btnRemoveChip.setOnClickListener(v -> {
                        selectedMedicines.remove(med);
                        render();
                    });
                    layoutSelectedMedicines.addView(chipView);
                }
            }
        }
        final ChipRenderer renderer = new ChipRenderer();

        // Prepopulate if records already exist
        Cursor rmCursor = dbHelper.getRekamMedisByPendaftaran(id);
        if (rmCursor != null && rmCursor.moveToFirst()) {
            int diagIdx = rmCursor.getColumnIndex(DatabaseHelper.KEY_RM_DIAGNOSA);
            int rxIdx = rmCursor.getColumnIndex(DatabaseHelper.KEY_RM_RESEP_OBAT);
            if (diagIdx >= 0 && rxIdx >= 0) {
                etDialogDiagnosis.setText(rmCursor.getString(diagIdx));
                String rawPrescription = rmCursor.getString(rxIdx);
                if (rawPrescription != null && !rawPrescription.trim().isEmpty()) {
                    String[] items = rawPrescription.split(",\\s*");
                    for (String item : items) {
                        if (!item.trim().isEmpty()) {
                            selectedMedicines.add(item.trim());
                        }
                    }
                }
            }
            rmCursor.close();
        }
        renderer.render();

        btnAddMedicineChip.setOnClickListener(v -> {
            if (layoutAutocompleteSearch.getVisibility() == View.GONE) {
                layoutAutocompleteSearch.setVisibility(View.VISIBLE);
                actvMedicineSearch.requestFocus();
            } else {
                layoutAutocompleteSearch.setVisibility(View.GONE);
            }
        });

        actvMedicineSearch.setOnItemClickListener((parent, view, position, rowId) -> {
            String selectedMed = (String) parent.getItemAtPosition(position);
            actvMedicineSearch.setText("");
            layoutAutocompleteSearch.setVisibility(View.GONE);

            // Open quantity prompt dialog
            showQuantityPrompt(selectedMed, qty -> {
                String chipString = selectedMed + " (" + qty + "x)";
                selectedMedicines.add(chipString);
                renderer.render();
            });
        });

        btnDialogCancel.setOnClickListener(v -> dialog.dismiss());

        btnDialogSave.setOnClickListener(v -> {
            String diagnosis = etDialogDiagnosis.getText().toString().trim();
            if (diagnosis.isEmpty()) {
                Toast.makeText(DoctorActivity.this, "Diagnosis cannot be empty!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedMedicines.isEmpty()) {
                Toast.makeText(DoctorActivity.this, "Please add at least one medicine!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Concatenate selected medicines to a comma-separated string
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < selectedMedicines.size(); i++) {
                sb.append(selectedMedicines.get(i));
                if (i < selectedMedicines.size() - 1) {
                    sb.append(", ");
                }
            }
            String prescriptionString = sb.toString();

            boolean saveRm = dbHelper.addRekamMedis(id, diagnosis, prescriptionString);
            boolean saveStatus = dbHelper.updatePendaftaranStatus(id, DatabaseHelper.STATUS_EXAM_DONE);

            if (saveRm && saveStatus) {
                Toast.makeText(DoctorActivity.this, "Examination saved successfully!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadQueueData();
            } else {
                Toast.makeText(DoctorActivity.this, "Failed to save examination.", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    interface QuantityCallback {
        void onQuantityEntered(int qty);
    }

    private void showQuantityPrompt(String medicineName, QuantityCallback callback) {
        final AppCompatDialog dialog = new AppCompatDialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_quantity_prompt);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);

        // Fit width
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setAttributes(lp);

        TextView tvTitle = dialog.findViewById(R.id.tvQtyTitle);
        EditText etQty = dialog.findViewById(R.id.etQtyInput);
        Button btnCancel = dialog.findViewById(R.id.btnQtyCancel);
        Button btnConfirm = dialog.findViewById(R.id.btnQtyConfirm);

        tvTitle.setText("Quantity for " + medicineName);
        etQty.setText("1");
        etQty.setSelection(1);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String input = etQty.getText().toString().trim();
            int qty = 1;
            if (!input.isEmpty()) {
                try {
                    qty = Integer.parseInt(input);
                } catch (NumberFormatException e) {
                    qty = 1;
                }
            }
            if (qty <= 0) qty = 1;
            callback.onQuantityEntered(qty);
            dialog.dismiss();
        });

        dialog.show();
    }

    // RecyclerView Adapter for Doctor
    private class DoctorQueueAdapter extends RecyclerView.Adapter<DoctorQueueAdapter.ViewHolder> {

        private final ArrayList<HashMap<String, String>> list;

        public DoctorQueueAdapter(ArrayList<HashMap<String, String>> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_doctor_queue, parent, false);
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

            String tempParentName = item.get("parent_name");
            if (tempParentName == null || tempParentName.trim().isEmpty()) {
                tempParentName = username;
            }
            final String parentName = tempParentName;

            holder.tvPatientName.setText(parentName);
            holder.tvDate.setText("Visit Date: " + date);
            holder.tvComplaint.setText("Complaint: " + complaint);

            // Style status badge and action button
            if (status.equalsIgnoreCase(DatabaseHelper.STATUS_VERIFIED)) {
                holder.tvStatus.setBackgroundResource(R.color.status_verified_bg);
                holder.tvStatus.setTextColor(getResources().getColor(R.color.status_verified_text));
                holder.tvStatus.setText("Verified / In Queue");
                holder.btnAction.setText("Start Examination");
            } else {
                holder.tvStatus.setBackgroundResource(R.color.status_checking_bg);
                holder.tvStatus.setTextColor(getResources().getColor(R.color.status_checking_text));
                holder.tvStatus.setText("Examining");
                holder.btnAction.setText("Continue Examination");
            }

            holder.btnAction.setOnClickListener(v -> {
                // If it is in queue, update status to "Sedang Diperiksa"
                if (status.equalsIgnoreCase(DatabaseHelper.STATUS_VERIFIED)) {
                    dbHelper.updatePendaftaranStatus(id, DatabaseHelper.STATUS_CHECKING);
                }
                
                // Show input dialog
                showExamineDialog(id, parentName, complaint);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvPatientName, tvStatus, tvDate, tvComplaint;
            Button btnAction;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvPatientName = itemView.findViewById(R.id.tvDoctorQueuePatientName);
                tvStatus = itemView.findViewById(R.id.tvDoctorQueueStatus);
                tvDate = itemView.findViewById(R.id.tvDoctorQueueDate);
                tvComplaint = itemView.findViewById(R.id.tvDoctorQueueComplaint);
                btnAction = itemView.findViewById(R.id.btnDoctorAction);
            }
        }
    }
}
