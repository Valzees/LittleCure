package com.example.littlecure;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "LittleCure.db";
    private static final int DATABASE_VERSION = 6; // Upgraded version to support user profiles & automatic extensive dummy data seeding, incremented to reset DB
    private static final String TAG = "DatabaseHelper";

    // Table names
    public static final String TABLE_USERS = "users";
    public static final String TABLE_PENDAFTARAN = "pendaftaran";
    public static final String TABLE_REKAM_MEDIS = "rekam_medis";

    // Common columns
    public static final String KEY_ID = "id";

    // USERS Table columns
    public static final String KEY_USER_USERNAME = "username";
    public static final String KEY_USER_PASSWORD = "password";
    public static final String KEY_USER_ROLE = "role";
    public static final String KEY_USER_PARENT_NAME = "parent_name";
    public static final String KEY_USER_PHONE = "phone";
    public static final String KEY_USER_CHILD_NAME = "child_name";
    public static final String KEY_USER_CHILD_DOB = "child_dob";

    // PENDAFTARAN Table columns
    public static final String KEY_PEND_USER_ID = "user_id";
    public static final String KEY_PEND_TANGGAL = "tanggal";
    public static final String KEY_PEND_KELUHAN = "keluhan";
    public static final String KEY_PEND_JENIS_BAYAR = "jenis_bayar";
    public static final String KEY_PEND_STATUS = "status";
    public static final String KEY_PEND_NO_ANTREAN = "no_antrean";

    // REKAM_MEDIS Table columns
    public static final String KEY_RM_PENDAFTARAN_ID = "pendaftaran_id";
    public static final String KEY_RM_DIAGNOSA = "diagnosa";
    public static final String KEY_RM_RESEP_OBAT = "resep_obat";

    // Status Constants
    public static final String STATUS_WAITING = "Menunggu Verifikasi";
    public static final String STATUS_VERIFIED = "Diverifikasi / Antre";
    public static final String STATUS_CHECKING = "Sedang Diperiksa";
    public static final String STATUS_EXAM_DONE = "Selesai Pemeriksaan";
    public static final String STATUS_COMPLETED = "Selesai";
    public static final String STATUS_PAID_PENDING = "Paid (Pending Confirmation)";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Users Table
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_USER_USERNAME + " TEXT UNIQUE,"
                + KEY_USER_PASSWORD + " TEXT,"
                + KEY_USER_ROLE + " TEXT,"
                + KEY_USER_PARENT_NAME + " TEXT,"
                + KEY_USER_PHONE + " TEXT,"
                + KEY_USER_CHILD_NAME + " TEXT,"
                + KEY_USER_CHILD_DOB + " TEXT" + ")";
        db.execSQL(CREATE_USERS_TABLE);

        // Create Pendaftaran Table
        String CREATE_PENDAFTARAN_TABLE = "CREATE TABLE " + TABLE_PENDAFTARAN + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_PEND_USER_ID + " INTEGER,"
                + KEY_PEND_TANGGAL + " TEXT,"
                + KEY_PEND_KELUHAN + " TEXT,"
                + KEY_PEND_JENIS_BAYAR + " TEXT,"
                + KEY_PEND_STATUS + " TEXT,"
                + KEY_PEND_NO_ANTREAN + " TEXT,"
                + "FOREIGN KEY(" + KEY_PEND_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + KEY_ID + ")" + ")";
        db.execSQL(CREATE_PENDAFTARAN_TABLE);

        // Create Rekam Medis Table
        String CREATE_REKAM_MEDIS_TABLE = "CREATE TABLE " + TABLE_REKAM_MEDIS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_RM_PENDAFTARAN_ID + " INTEGER UNIQUE,"
                + KEY_RM_DIAGNOSA + " TEXT,"
                + KEY_RM_RESEP_OBAT + " TEXT,"
                + "FOREIGN KEY(" + KEY_RM_PENDAFTARAN_ID + ") REFERENCES " + TABLE_PENDAFTARAN + "(" + KEY_ID + ")" + ")";
        db.execSQL(CREATE_REKAM_MEDIS_TABLE);

        // Create Obat Table
        db.execSQL("CREATE TABLE IF NOT EXISTS obat(id INTEGER PRIMARY KEY AUTOINCREMENT, nama TEXT UNIQUE, harga INTEGER)");
        db.execSQL("INSERT OR IGNORE INTO obat(nama, harga) VALUES('Paracetamol Syrup', 35000)");
        db.execSQL("INSERT OR IGNORE INTO obat(nama, harga) VALUES('Amoxicillin Syrup', 50000)");
        db.execSQL("INSERT OR IGNORE INTO obat(nama, harga) VALUES('Cazetin Nystatin Drop', 45000)");
        db.execSQL("INSERT OR IGNORE INTO obat(nama, harga) VALUES('Lactobe (Obat Diare)', 30000)");
        db.execSQL("INSERT OR IGNORE INTO obat(nama, harga) VALUES('Apialys Drop (Vitamin)', 25000)");
        db.execSQL("INSERT OR IGNORE INTO obat(nama, harga) VALUES('Tempra Drop', 40000)");
        db.execSQL("INSERT OR IGNORE INTO obat(nama, harga) VALUES('OBH Anak Syrup', 20000)");
        db.execSQL("INSERT OR IGNORE INTO obat(nama, harga) VALUES('Bisolvon Kids Syrup', 30000)");
        db.execSQL("INSERT OR IGNORE INTO obat(nama, harga) VALUES('Sanmol Drop', 35000)");
        db.execSQL("INSERT OR IGNORE INTO obat(nama, harga) VALUES('Zamel Syrup (Vitamin)', 30000)");

        // Seed Default Users
        seedDefaultUsers(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS obat");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REKAM_MEDIS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PENDAFTARAN);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    private void seedDefaultUsers(SQLiteDatabase db) {
        try {
            // Admin user
            ContentValues adminValues = new ContentValues();
            adminValues.put(KEY_USER_USERNAME, "admin");
            adminValues.put(KEY_USER_PASSWORD, "admin");
            adminValues.put(KEY_USER_ROLE, "admin");
            db.insert(TABLE_USERS, null, adminValues);

            // Doctor user
            ContentValues doctorValues = new ContentValues();
            doctorValues.put(KEY_USER_USERNAME, "dokter");
            doctorValues.put(KEY_USER_PASSWORD, "dokter");
            doctorValues.put(KEY_USER_ROLE, "dokter");
            db.insert(TABLE_USERS, null, doctorValues);

            // Patient user 1 (Default)
            ContentValues patientValues = new ContentValues();
            patientValues.put(KEY_ID, 3);
            patientValues.put(KEY_USER_USERNAME, "pasien");
            patientValues.put(KEY_USER_PASSWORD, "pasien");
            patientValues.put(KEY_USER_ROLE, "pasien");
            patientValues.put(KEY_USER_PARENT_NAME, "Budi Santoso");
            patientValues.put(KEY_USER_PHONE, "+6281234567890");
            patientValues.put(KEY_USER_CHILD_NAME, "Andi");
            patientValues.put(KEY_USER_CHILD_DOB, "2022-10-15");
            db.insert(TABLE_USERS, null, patientValues);

            // Patient user 2 (Lisa)
            ContentValues lisaValues = new ContentValues();
            lisaValues.put(KEY_ID, 5);
            lisaValues.put(KEY_USER_USERNAME, "lisa@gmail.com");
            lisaValues.put(KEY_USER_PASSWORD, "123456");
            lisaValues.put(KEY_USER_ROLE, "pasien");
            lisaValues.put(KEY_USER_PARENT_NAME, "Lisa Permata");
            lisaValues.put(KEY_USER_PHONE, "081298765432");
            lisaValues.put(KEY_USER_CHILD_NAME, "Roni");
            lisaValues.put(KEY_USER_CHILD_DOB, "2021-04-10");
            db.insert(TABLE_USERS, null, lisaValues);

            // Patient user 3 (Hendra)
            ContentValues hendraValues = new ContentValues();
            hendraValues.put(KEY_ID, 6);
            hendraValues.put(KEY_USER_USERNAME, "hendra@gmail.com");
            hendraValues.put(KEY_USER_PASSWORD, "123456");
            hendraValues.put(KEY_USER_ROLE, "pasien");
            hendraValues.put(KEY_USER_PARENT_NAME, "Hendra Wijaya");
            hendraValues.put(KEY_USER_PHONE, "085612345678");
            hendraValues.put(KEY_USER_CHILD_NAME, "Siti");
            hendraValues.put(KEY_USER_CHILD_DOB, "2023-01-20");
            db.insert(TABLE_USERS, null, hendraValues);

            // Patient user 4 (Dewi)
            ContentValues dewiValues = new ContentValues();
            dewiValues.put(KEY_ID, 7);
            dewiValues.put(KEY_USER_USERNAME, "dewi@gmail.com");
            dewiValues.put(KEY_USER_PASSWORD, "123456");
            dewiValues.put(KEY_USER_ROLE, "pasien");
            dewiValues.put(KEY_USER_PARENT_NAME, "Dewi Lestari");
            dewiValues.put(KEY_USER_PHONE, "087811223344");
            dewiValues.put(KEY_USER_CHILD_NAME, "Boni");
            dewiValues.put(KEY_USER_CHILD_DOB, "2021-08-05");
            db.insert(TABLE_USERS, null, dewiValues);
            // Seed Pendaftaran
            int[][] pendaftarans = {
                {1, 3}, {2, 3}, {3, 5}, {4, 5}, {5, 6}, {6, 6}, {7, 7}, {8, 7}, {10, 6}, {11, 7}, {12, 7}
            };
            String[] dates = {"2026-06-09", "2026-06-08", "2026-06-09", "2026-06-06", "2026-06-09", "2026-06-07", "2026-06-09", "2026-06-05", "2026-05-30", "2026-06-01", "2026-05-28"};
            String[] complaints = {
                "Andi (3 tahun) - Demam tinggi sejak tadi malam",
                "Andi (3 tahun) - Batuk berdahak",
                "Roni (5 tahun) - Gatal-gatal di seluruh tubuh setelah makan seafood",
                "Roni (5 tahun) - Diare ringan",
                "Siti (3 tahun) - Nafsu makan menurun dan lemas",
                "Siti (3 tahun) - Batuk pilek",
                "Boni (5 tahun) - Sakit telinga kanan",
                "Boni (5 tahun) - Luka lecet di lutut",
                "Siti (3 tahun) - Imunisasi rutin",
                "Boni (5 tahun) - Demam sumeng",
                "Boni (5 tahun) - Kontrol tumbuh kembang"
            };
            String[] payments = {"Cash", "Cash", "Insurance", "Cash", "Cash", "Cash", "Cash", "Cash", "Insurance", "Cash", "Insurance"};
            String[] statuses = {STATUS_WAITING, STATUS_COMPLETED, STATUS_VERIFIED, STATUS_COMPLETED, STATUS_CHECKING, STATUS_EXAM_DONE, STATUS_PAID_PENDING, STATUS_COMPLETED, STATUS_COMPLETED, STATUS_COMPLETED, STATUS_COMPLETED};
            String[] tickets = {"", "A-001", "A-002", "A-004", "A-003", "A-002", "A-004", "A-003", "A-001", "A-005", "A-002"};

            for (int i = 0; i < pendaftarans.length; i++) {
                ContentValues pendValues = new ContentValues();
                pendValues.put(KEY_ID, pendaftarans[i][0]);
                pendValues.put(KEY_PEND_USER_ID, pendaftarans[i][1]);
                pendValues.put(KEY_PEND_TANGGAL, dates[i]);
                pendValues.put(KEY_PEND_KELUHAN, complaints[i]);
                pendValues.put(KEY_PEND_JENIS_BAYAR, payments[i]);
                pendValues.put(KEY_PEND_STATUS, statuses[i]);
                pendValues.put(KEY_PEND_NO_ANTREAN, tickets[i]);
                db.insert(TABLE_PENDAFTARAN, null, pendValues);
            }

            // Seed Rekam Medis
            int[] rmPids = {2, 4, 6, 7, 8, 10, 11, 12};
            String[] diagnoses = {
                "ISPA (Infeksi Saluran Pernapasan Akut)",
                "Gastroenteritis Ringan",
                "Rinitis Alergi",
                "Otitis Media Akut",
                "Luka Ekskoriasi Lutut Kanan",
                "Imunisasi Booster",
                "Febris Observasi",
                "Tumbuh Kembang Normal"
            };
            String[] prescriptions = {
                "OBH Anak Syrup (1x), Amoxicillin Syrup (1x)",
                "Lactobe (Obat Diare) (2x), Apialys Drop (Vitamin) (1x)",
                "Bisolvon Kids Syrup (1x)",
                "Amoxicillin Syrup (1x), Apialys Drop (Vitamin) (1x)",
                "-",
                "-",
                "Paracetamol Syrup (1x)",
                "-"
            };

            for (int i = 0; i < rmPids.length; i++) {
                ContentValues rmValues = new ContentValues();
                rmValues.put(KEY_RM_PENDAFTARAN_ID, rmPids[i]);
                rmValues.put(KEY_RM_DIAGNOSA, diagnoses[i]);
                rmValues.put(KEY_RM_RESEP_OBAT, prescriptions[i]);
                db.insert(TABLE_REKAM_MEDIS, null, rmValues);
            }

            Log.d(TAG, "Default users and extensive dummy records seeded successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Error seeding default data: " + e.getMessage());
        }
    }

    // --- Users CRUD ---

    public boolean registerUser(String username, String password, String role, String parentName, String phone, String childName, String childDob) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_USER_USERNAME, username);
        values.put(KEY_USER_PASSWORD, password);
        values.put(KEY_USER_ROLE, role);
        values.put(KEY_USER_PARENT_NAME, parentName);
        values.put(KEY_USER_PHONE, phone);
        values.put(KEY_USER_CHILD_NAME, childName);
        values.put(KEY_USER_CHILD_DOB, childDob);

        if (username != null) {
            String cleanEmail = username.trim().toLowerCase();
            if (cleanEmail.equals("dummy@gmail.com")) {
                values.put(KEY_ID, 4);
            } else if (cleanEmail.equals("lisa@gmail.com")) {
                values.put(KEY_ID, 5);
            } else if (cleanEmail.equals("hendra@gmail.com")) {
                values.put(KEY_ID, 6);
            } else if (cleanEmail.equals("dewi@gmail.com")) {
                values.put(KEY_ID, 7);
            }
        }

        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    public Cursor checkUserLogin(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_USERS + " WHERE " 
                + KEY_USER_USERNAME + " = ? COLLATE NOCASE AND " + KEY_USER_PASSWORD + " = ?";
        return db.rawQuery(query, new String[]{username, password});
    }

    public Cursor getUserByUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_USERS + " WHERE " + KEY_USER_USERNAME + " = ? COLLATE NOCASE";
        return db.rawQuery(query, new String[]{username});
    }

    public boolean checkPhoneExists(String phone) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT 1 FROM " + TABLE_USERS + " WHERE " + KEY_USER_PHONE + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{phone});
        boolean exists = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) cursor.close();
        return exists;
    }

    public boolean updateUserPassword(String username, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_USER_PASSWORD, newPassword);
        int result = db.update(TABLE_USERS, values, KEY_USER_USERNAME + " = ?", new String[]{username});
        return result > 0;
    }

    public boolean updateUserChildDetails(String username, String childName, String childDob) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_USER_CHILD_NAME, childName);
        values.put(KEY_USER_CHILD_DOB, childDob);
        int result = db.update(TABLE_USERS, values, KEY_USER_USERNAME + " = ?", new String[]{username});
        return result > 0;
    }

    // --- Pendaftaran CRUD ---

    public boolean addPendaftaran(int userId, String tanggal, String keluhan, String jenisBayar) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_PEND_USER_ID, userId);
        values.put(KEY_PEND_TANGGAL, tanggal);
        values.put(KEY_PEND_KELUHAN, keluhan);
        values.put(KEY_PEND_JENIS_BAYAR, jenisBayar);
        values.put(KEY_PEND_STATUS, STATUS_WAITING);
        values.put(KEY_PEND_NO_ANTREAN, ""); // Initialize empty

        long result = db.insert(TABLE_PENDAFTARAN, null, values);
        return result != -1;
    }

    public Cursor getActivePendaftaran(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_PENDAFTARAN + " WHERE " + KEY_PEND_USER_ID + " = ? ORDER BY " + KEY_ID + " DESC LIMIT 1";
        return db.rawQuery(query, new String[]{String.valueOf(userId)});
    }

    public Cursor getAllPendaftaran() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT p.*, u." + KEY_USER_USERNAME + ", u." + KEY_USER_PARENT_NAME + ", u." + KEY_USER_PHONE 
                + ", u." + KEY_USER_CHILD_NAME + ", u." + KEY_USER_CHILD_DOB + " FROM " + TABLE_PENDAFTARAN + " p JOIN " 
                + TABLE_USERS + " u ON p." + KEY_PEND_USER_ID + " = u." + KEY_ID + " ORDER BY p." + KEY_ID + " DESC";
        return db.rawQuery(query, null);
    }

    public Cursor getPendaftaranByStatus(String status) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT p.*, u." + KEY_USER_USERNAME + ", u." + KEY_USER_PARENT_NAME + ", u." + KEY_USER_PHONE 
                + ", u." + KEY_USER_CHILD_NAME + ", u." + KEY_USER_CHILD_DOB + " FROM " + TABLE_PENDAFTARAN + " p JOIN " 
                + TABLE_USERS + " u ON p." + KEY_PEND_USER_ID + " = u." + KEY_ID + " WHERE p." + KEY_PEND_STATUS + " = ? ORDER BY p." + KEY_ID + " ASC";
        return db.rawQuery(query, new String[]{status});
    }

    public Cursor getPendaftaranByStatusOr(String status1, String status2) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT p.*, u." + KEY_USER_USERNAME + ", u." + KEY_USER_PARENT_NAME + ", u." + KEY_USER_PHONE 
                + ", u." + KEY_USER_CHILD_NAME + ", u." + KEY_USER_CHILD_DOB + " FROM " + TABLE_PENDAFTARAN + " p JOIN " 
                + TABLE_USERS + " u ON p." + KEY_PEND_USER_ID + " = u." + KEY_ID 
                + " WHERE p." + KEY_PEND_STATUS + " = ? OR p." + KEY_PEND_STATUS + " = ? ORDER BY p." + KEY_ID + " ASC";
        return db.rawQuery(query, new String[]{status1, status2});
    }

    public boolean updatePendaftaranStatus(int pendaftaranId, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_PEND_STATUS, status);

        int result = db.update(TABLE_PENDAFTARAN, values, KEY_ID + " = ?", new String[]{String.valueOf(pendaftaranId)});
        return result > 0;
    }

    public boolean verifyPendaftaran(int pendaftaranId, String noAntrean) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_PEND_STATUS, STATUS_VERIFIED);
        values.put(KEY_PEND_NO_ANTREAN, noAntrean);

        int result = db.update(TABLE_PENDAFTARAN, values, KEY_ID + " = ?", new String[]{String.valueOf(pendaftaranId)});
        return result > 0;
    }

    // --- Rekam Medis CRUD ---

    public boolean addRekamMedis(int pendaftaranId, String diagnosa, String resepObat) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_RM_PENDAFTARAN_ID, pendaftaranId);
        values.put(KEY_RM_DIAGNOSA, diagnosa);
        values.put(KEY_RM_RESEP_OBAT, resepObat);

        long result;
        Cursor cursor = getRekamMedisByPendaftaran(pendaftaranId);
        if (cursor != null && cursor.moveToFirst()) {
            result = db.update(TABLE_REKAM_MEDIS, values, KEY_RM_PENDAFTARAN_ID + " = ?", new String[]{String.valueOf(pendaftaranId)});
            cursor.close();
        } else {
            result = db.insert(TABLE_REKAM_MEDIS, null, values);
            if (cursor != null) cursor.close();
        }
        return result != -1;
    }

    public Cursor getRekamMedisByPendaftaran(int pendaftaranId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_REKAM_MEDIS + " WHERE " + KEY_RM_PENDAFTARAN_ID + " = ?";
        return db.rawQuery(query, new String[]{String.valueOf(pendaftaranId)});
    }

    public Cursor getMedicalHistory(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT p.*, r." + KEY_RM_DIAGNOSA + ", r." + KEY_RM_RESEP_OBAT + " FROM " + TABLE_PENDAFTARAN + " p JOIN "
                + TABLE_REKAM_MEDIS + " r ON p." + KEY_ID + " = r." + KEY_RM_PENDAFTARAN_ID
                + " WHERE p." + KEY_PEND_USER_ID + " = ? AND (p." + KEY_PEND_STATUS + " = ? OR p." + KEY_PEND_STATUS + " = ?)"
                + " ORDER BY p." + KEY_ID + " DESC";
        return db.rawQuery(query, new String[]{String.valueOf(userId), STATUS_EXAM_DONE, STATUS_COMPLETED});
    }

    public void createObatTableIfNeeded() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("CREATE TABLE IF NOT EXISTS obat(id INTEGER PRIMARY KEY AUTOINCREMENT, nama TEXT UNIQUE, harga INTEGER)");
        
        // Seed if empty
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM obat", null);
        if (cursor != null) {
            if (cursor.moveToFirst() && cursor.getInt(0) == 0) {
                db.execSQL("INSERT OR IGNORE INTO obat(nama, harga) VALUES('Paracetamol Syrup', 35000)");
                db.execSQL("INSERT OR IGNORE INTO obat(nama, harga) VALUES('Amoxicillin Syrup', 50000)");
                db.execSQL("INSERT OR IGNORE INTO obat(nama, harga) VALUES('Cazetin Nystatin Drop', 45000)");
                db.execSQL("INSERT OR IGNORE INTO obat(nama, harga) VALUES('Lactobe (Obat Diare)', 30000)");
                db.execSQL("INSERT OR IGNORE INTO obat(nama, harga) VALUES('Apialys Drop (Vitamin)', 25000)");
                db.execSQL("INSERT OR IGNORE INTO obat(nama, harga) VALUES('Tempra Drop', 40000)");
                db.execSQL("INSERT OR IGNORE INTO obat(nama, harga) VALUES('OBH Anak Syrup', 20000)");
                db.execSQL("INSERT OR IGNORE INTO obat(nama, harga) VALUES('Bisolvon Kids Syrup', 30000)");
                db.execSQL("INSERT OR IGNORE INTO obat(nama, harga) VALUES('Sanmol Drop', 35000)");
                db.execSQL("INSERT OR IGNORE INTO obat(nama, harga) VALUES('Zamel Syrup (Vitamin)', 30000)");
            }
            cursor.close();
        }
    }

    public Cursor getAllObat() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM obat ORDER BY nama ASC", null);
    }

    public int getObatPrice(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT harga FROM obat WHERE nama = ?", new String[]{name});
        int price = 40000; // Default flat rate fallback if not found in lookup
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                price = cursor.getInt(0);
            }
            cursor.close();
        }
        return price;
    }

    public String getWeeklyGrowthString() {
        SQLiteDatabase db = this.getReadableDatabase();
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
        java.util.Calendar cal = java.util.Calendar.getInstance();
        
        String refDateStr = null;
        Cursor maxCur = db.rawQuery("SELECT MAX(" + KEY_PEND_TANGGAL + ") FROM " + TABLE_PENDAFTARAN + " WHERE " + KEY_PEND_TANGGAL + " IS NOT NULL AND " + KEY_PEND_TANGGAL + " != ''", null);
        if (maxCur != null) {
            if (maxCur.moveToFirst()) {
                refDateStr = maxCur.getString(0);
            }
            maxCur.close();
        }

        if (refDateStr != null && refDateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
            try {
                java.util.Date parsedDate = sdf.parse(refDateStr);
                if (parsedDate != null) {
                    cal.setTime(parsedDate);
                }
            } catch (Exception e) {
                Log.e("DatabaseHelper", "Error parsing max date: " + refDateStr, e);
            }
        }

        String dateToday = sdf.format(cal.getTime());
        
        cal.add(java.util.Calendar.DAY_OF_YEAR, -6);
        String dateSixDaysAgo = sdf.format(cal.getTime());
        
        cal.add(java.util.Calendar.DAY_OF_YEAR, -1);
        String dateSevenDaysAgo = sdf.format(cal.getTime());
        
        cal.add(java.util.Calendar.DAY_OF_YEAR, -6);
        String dateThirteenDaysAgo = sdf.format(cal.getTime());
        
        // Count for current week (last 7 days, including reference date)
        int currentWeekCount = 0;
        String queryCurrent = "SELECT COUNT(*) FROM " + TABLE_PENDAFTARAN 
                + " WHERE " + KEY_PEND_TANGGAL + " BETWEEN ? AND ?";
        Cursor cursorCurrent = db.rawQuery(queryCurrent, new String[]{dateSixDaysAgo, dateToday});
        if (cursorCurrent != null && cursorCurrent.moveToFirst()) {
            currentWeekCount = cursorCurrent.getInt(0);
            cursorCurrent.close();
        }
        
        // Count for previous week (7 days before that)
        int previousWeekCount = 0;
        Cursor cursorPrevious = db.rawQuery(queryCurrent, new String[]{dateThirteenDaysAgo, dateSevenDaysAgo});
        if (cursorPrevious != null && cursorPrevious.moveToFirst()) {
            previousWeekCount = cursorPrevious.getInt(0);
            cursorPrevious.close();
        }

        Log.d("DatabaseHelper", "getWeeklyGrowthString: currentWeekCount=" + currentWeekCount 
                + ", previousWeekCount=" + previousWeekCount + ", dateToday=" + dateToday 
                + ", dateSixDaysAgo=" + dateSixDaysAgo + ", dateSevenDaysAgo=" + dateSevenDaysAgo 
                + ", dateThirteenDaysAgo=" + dateThirteenDaysAgo);        
        
        if (previousWeekCount == 0) {
            if (currentWeekCount == 0) {
                return "0%";
            } else {
                // If previous week was 0, calculate growth assuming baseline of 1 to keep it clean, or return percentage
                return "+" + (currentWeekCount * 100) + "%";
            }
        }
        
        int diff = currentWeekCount - previousWeekCount;
        double pct = ((double) diff / previousWeekCount) * 100;
        int pctInt = (int) Math.round(pct);
        
        if (pctInt >= 0) {
            return "+" + pctInt + "%";
        } else {
            return pctInt + "%";
        }
    }
}
