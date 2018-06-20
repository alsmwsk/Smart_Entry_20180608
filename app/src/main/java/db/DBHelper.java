package db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

public class DBHelper extends SQLiteOpenHelper {
    private Context context;
    static String DBname;
    public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);

        this.DBname = name;

        this.context = context;

    }
    /*
     Database가 존재하지 않을 때, 딱 한번 실행된다.
     DB를 만드는 역할을 한다.

    @param db
    */


    @Override
    public void onCreate(SQLiteDatabase db) {
        // String 보다 StringBuffer가 Query 만들기 편하다.
        StringBuffer sb = new StringBuffer();
        sb.append(" CREATE TABLE ");
        sb.append(DBname + " ( ");
        sb.append(" UUID TEXT PRIMARY KEY, ");
        sb.append(" HASHCODE TEXT ) ");
        // SQLite Database로 쿼리 실행

        db.execSQL(sb.toString());
        Toast.makeText(context, "Table 생성완료", Toast.LENGTH_LONG).show();
        db.close();
    }
    /*
     * Application의 버전이 올라가서
     * Table 구조가 변경되었을 때 실행된다.
     * @param db * @param oldVersion
     * @param newVersion
     */
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
    public void testDB() {
        SQLiteDatabase db = getWritableDatabase();
    }

    public Boolean insert(String uuid, String hashcode) {
        // 읽고 쓰기가 가능하게 DB 열기
        try {
            SQLiteDatabase db = getWritableDatabase();
            // DB에 입력한 값으로 행 추가

            StringBuffer sb = new StringBuffer();
            sb.append(" INSERT INTO ");
            sb.append(DBname + " VALUES(");
            sb.append("\""+uuid + "\", \"" + hashcode +"\")");

            db.execSQL(sb.toString());
            db.close();

            return true;
        }catch (Exception e){
            return false;
        }
    }

    public Boolean isExist(String str) {
        // 읽기가 가능하게 DB 열기
        try{
            SQLiteDatabase db = getReadableDatabase();

            Cursor cursor = db.query(DBname, null, null, null, null, null, null);
            while (cursor.moveToNext()) {
                if (cursor.getString(0).equals(str))
                    return true;
            }

            return false;
        }catch (Exception e){
            return false;
        }
    }

    public void DeleteTable(){
        try {
            SQLiteDatabase db = getReadableDatabase();
            db.execSQL("DROP TABLE " + DBname);
            db.close();

            Toast.makeText(context, "Table 삭제 완료", Toast.LENGTH_LONG).show();
        } catch (Exception e){
            Toast.makeText(context, "Table 삭제 실패", Toast.LENGTH_LONG).show();
        }
    }

}
