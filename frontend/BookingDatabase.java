package com.example.staygeniefrontend;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BookingDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME = "manusaipdd"; // database name isn't sure.
    private static final int DB_VERSION = 1;

    private static final String TABLE_BOOKINGS = "bookings";
    private static final String COL_ID = "id";
    private static final String COL_HOTEL = "hotel_name";
    private static final String COL_STATUS = "status";
    private static final String COL_BOOKING_ID = "booking_id";
    private static final String COL_AMOUNT = "amount";
    private static final String COL_DETAILS = "details";
    private static final String COL_BOOKING_DATE = "booking_date";

    public BookingDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_BOOKINGS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COL_HOTEL + " TEXT," +
                COL_STATUS + " TEXT," +
                COL_BOOKING_ID + " TEXT," +
                COL_AMOUNT + " TEXT," +
                COL_DETAILS + " TEXT," +
                COL_BOOKING_DATE + " INTEGER DEFAULT " + System.currentTimeMillis() + ")";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKINGS);
        onCreate(db);
    }

    // Add a booking to DB
    public void addBooking(String hotelName, String status, String bookingId, String details) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_HOTEL, hotelName);
        values.put(COL_STATUS, status);
        values.put(COL_BOOKING_ID, bookingId);
        values.put(COL_DETAILS, details);
        values.put(COL_BOOKING_DATE, System.currentTimeMillis());
        
        // Extract amount from details if present
        if (details != null && details.contains("Amount:")) {
            String[] parts = details.split("Amount:");
            if (parts.length > 1) {
                String amountPart = parts[1].split("\\|")[0].trim();
                values.put(COL_AMOUNT, amountPart);
            }
        }
        
        db.insert(TABLE_BOOKINGS, null, values);
        db.close();
    }

    // Optional: fetch all bookings (useful for history)
    public Cursor getAllBookings() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_BOOKINGS + " ORDER BY " + COL_ID + " DESC", null);
    }
}
