package com.cloudmine.api.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.cloudmine.api.CMObject;
import com.cloudmine.api.Strings;
import com.cloudmine.api.persistance.ClassNameRegistry;
import com.cloudmine.api.rest.JsonUtilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores CMObjects in a relational database
 *
 * <br>
 * Copyright CloudMine, Inc. All rights reserved<br>
 * See LICENSE file included with SDK for details.
 */
public class CMObjectDBOpenHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "cmobjects.db";
    public static final String CM_OBJECT_TABLE = "CMObjectTable";
    public static final String OBJECT_ID_COLUMN = "OBJECT_ID";
    public static final String JSON_COLUMN = "JSON";
    public static final String CLASS_NAME_COLUMN = "CLASS_NAME";
    public static final String SAVED_DATE_COLUMN = "SAVED_DATE";
    public static final String SYNCED_DATE_COLUMN = "SYNCED_DATE";
    public static final int DATABASE_VERSION = 2;

    private static final String CMOBJECT_DATABASE_CREATE = "create table " + CM_OBJECT_TABLE +
            " (" +
            OBJECT_ID_COLUMN + " text not null primary key, " +
            CLASS_NAME_COLUMN + " text not null, " +
            JSON_COLUMN + " text not null, " +
            SAVED_DATE_COLUMN + " integer not null, " +
            SYNCED_DATE_COLUMN + " integer" +
            ")";
    private static final String OBJECT_ID_WHERE = OBJECT_ID_COLUMN + "=?";
    private static final String MULTI_OBJECT_ID_WHERE = OBJECT_ID_COLUMN + " in (?)";
    private static final String UPDATE_OBJECT_WHERE = OBJECT_ID_WHERE + " AND " + SAVED_DATE_COLUMN + "<?";
    private static final String CLASS_SELECT_WHERE = CLASS_NAME_COLUMN + "=?";
    private static final String[] COLUMNS = {OBJECT_ID_COLUMN, CLASS_NAME_COLUMN, JSON_COLUMN, SAVED_DATE_COLUMN, SYNCED_DATE_COLUMN};

    private static final Object syncSingleton = new Object();
    private static CMObjectDBOpenHelper cmObjectDBOpenHelper;
    static synchronized CMObjectDBOpenHelper getCMObjectDBHelper(Context context) {
        if(cmObjectDBOpenHelper == null) {
            synchronized (syncSingleton) {
                if(cmObjectDBOpenHelper == null) cmObjectDBOpenHelper = new CMObjectDBOpenHelper(context.getApplicationContext());
            }
        }
        return cmObjectDBOpenHelper;
    }

    private final Object syncDb = new Object();

    public CMObjectDBOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CMOBJECT_DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
        //TODO upgrade gracefully
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + CM_OBJECT_TABLE);
        onCreate(sqLiteDatabase);
    }

    //TODO this just always updates right now - should we not insert older objects, how much processing power are we willing to devote to that?
    public boolean insertCMObjectIfNewer(BaseLocallySavableCMObject cmObject) {
        if(cmObject == null) return false;
        synchronized (syncDb) {
            SQLiteDatabase db = getWritableDatabase();
            try {
                boolean wasInsertedOrUpdated;
                ContentValues contentValues = cmObject.toContentValues();

                int numUpdated = db.update(CM_OBJECT_TABLE, contentValues, OBJECT_ID_WHERE,
                        new String[]{cmObject.getObjectId()});
                wasInsertedOrUpdated = numUpdated > 0;
                if(!wasInsertedOrUpdated) {
                    long insertResult = db.insert(CM_OBJECT_TABLE, null, contentValues);
                    wasInsertedOrUpdated = insertResult > 0;
                }

                return wasInsertedOrUpdated;
            } finally {
                db.close();
            }
        }
    }

    public <OBJECT_TYPE extends BaseLocallySavableCMObject> OBJECT_TYPE loadObjectById(String objectId) {
        if(Strings.isEmpty(objectId)) return null;

        synchronized (syncDb) {
            SQLiteDatabase db = getReadableDatabase();
            try {
                Cursor cursor = db.query(CM_OBJECT_TABLE, new String[]{JSON_COLUMN}, OBJECT_ID_WHERE, new String[]{objectId}, null, null, null);
                if(!cursor.moveToNext()) return null;
                return fromCursor(cursor);
            }finally {
                db.close();
            }
        }
    }

    public List<BaseLocallySavableCMObject> loadAllObjects() {
        synchronized (syncDb) {
            SQLiteDatabase db = getReadableDatabase();
            List<BaseLocallySavableCMObject> allObjects = new ArrayList<BaseLocallySavableCMObject>();
            try {
                Cursor cursor = db.query(CM_OBJECT_TABLE, new String[] {JSON_COLUMN}, null, null, null, null, null);
                while (cursor.moveToNext()) {
                    BaseLocallySavableCMObject object = fromCursor(cursor);
                    allObjects.add(object);
                }
            } finally {
                db.close();
            }
            return allObjects;
        }
    }

    public int deleteObjectById(String objectId) {
        if(Strings.isEmpty(objectId)) return 0;

        synchronized (syncDb) {
            SQLiteDatabase db = getWritableDatabase();
            try {
                return db.delete(CM_OBJECT_TABLE, OBJECT_ID_WHERE, new String[]{objectId});
            }finally {
                db.close();
            }
        }
    }

    public Map<String, String> loadObjectJsonById(Collection <String> objectIds) {
        Map<String, String> objectIdsToJson = new HashMap<String, String>();

        synchronized (syncDb) {
            SQLiteDatabase db = getReadableDatabase();
            try {
                StringBuilder queryBuilder = new StringBuilder(OBJECT_ID_COLUMN).append(" IN (").append(collectionToCsv(objectIds)).append(")");
                Cursor cursor = db.query(CM_OBJECT_TABLE, new String[] {JSON_COLUMN, OBJECT_ID_COLUMN}, queryBuilder.toString(), null, null, null, null);

                int jsonIndex = cursor.getColumnIndex(JSON_COLUMN);
                int objectIdIndex = cursor.getColumnIndex(OBJECT_ID_COLUMN);

                while (!cursor.isClosed() && cursor.moveToNext()) {
                    String json = cursor.getString(jsonIndex);
                    String objectId = cursor.getString(objectIdIndex);
                    objectIdsToJson.put(objectId, json);
                }
            }
            finally {
                db.close();
            }
        }
        return objectIdsToJson;
    }

    public <TYPE> int deleteObjectsByClass(Class<TYPE> klass) {
        String[] args = {ClassNameRegistry.forClass(klass)};
        synchronized (syncDb) {
            SQLiteDatabase writableDatabase = getWritableDatabase();
            try {
                return writableDatabase.delete(CM_OBJECT_TABLE, CLASS_SELECT_WHERE, args);
            } catch (Throwable t) {} finally {
                writableDatabase.close();
                return 0;
            }
        }
    }

    public <TYPE extends BaseLocallySavableCMObject> List<TYPE> loadObjectsByClass(Class <TYPE> klass) {
        String[] args = {ClassNameRegistry.forClass(klass)};
        synchronized (syncDb) {
            SQLiteDatabase readableDatabase = getReadableDatabase();
            try {
                Cursor results = readableDatabase.query(CM_OBJECT_TABLE, COLUMNS, CLASS_SELECT_WHERE, args, null, null, null, null);
                List<TYPE> resultList = new ArrayList<TYPE>();
                while (results.moveToNext()) {
                    resultList.add((TYPE)fromCursor(results));
                }
                return resultList;
            }finally{
                readableDatabase.close();
            }
        }
    }


    private String collectionToCsv(Collection<? extends Object> collection) {
        if(collection == null || collection.isEmpty())
            return "";
        StringBuilder csvBuilder = new StringBuilder();
        String separator = "";

        for(Object element : collection) {
            csvBuilder.append(separator).append('"').append(element).append('"');
            separator = ", ";
        }
        return csvBuilder.toString();
    }

    private static <OBJECT_TYPE extends BaseLocallySavableCMObject> OBJECT_TYPE fromCursor(Cursor cursor) {
        int jsonIndex = cursor.getColumnIndex(JSON_COLUMN);
        String json = cursor.getString(jsonIndex);
        if(Strings.isEmpty(json)) return null;
        Map<String,CMObject> stringCMObjectMap = JsonUtilities.jsonToClassMap(json);
        if(stringCMObjectMap.isEmpty()) return null;
        return (OBJECT_TYPE) stringCMObjectMap.values().iterator().next();
    }
}
