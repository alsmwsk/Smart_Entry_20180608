package kr.co.bastion.smartentry;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import db.DBHelper;

public class SettingActivity extends AppCompatActivity {
    ImageButton imgBack;

    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        dbHelper =  new DBHelper(getApplicationContext(), "SMART_ENTRY_USER_TABLE", null, 1);
        //dbHelper.testDB();


        imgBack = findViewById(R.id.imgBack);
        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    //스마트 엔트리 등록
    public void btnSmartEntryRegisteration(View v){ // QR code
        startQRCode();
    }

    //스마트 엔트리 관리?
    public void btnSmartEntryUserManagement(View v){ //OTP

    }

    //로그아웃을 하면 로그인 액티비티로 이동
    public void btnLogout(View v){
        Intent intent = new Intent(SettingActivity.this,LoginActivity.class);
        startActivity(intent);
        ActivityCompat.finishAffinity(this);
    }


    public void startQRCode() {
        new IntentIntegrator(this).initiateScan();
    }


    //QR코드 결과
    //onActivityResult는 언제 실행되는 함수?
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        //  com.google.zxing.integration.android.IntentIntegrator.REQUEST_CODE
        //  = 0x0000c0de; // Only use bottom 16 bits
        if (requestCode == IntentIntegrator.REQUEST_CODE) {
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (result.getContents() == null) {
                // 취소됨
                Toast.makeText(this, "취소되었습니다.", Toast.LENGTH_LONG).show();
            } else {
                // 스캔된 QRCode --> result.getContents()

                String getUUID = result.getContents();

                if (dbHelper.isExist(getUUID)) {
                    Toast.makeText(this, "이미 등록되어 있는 차량입니다.", Toast.LENGTH_LONG).show();
                } else {
                    if (dbHelper.insert(getUUID, toSHA256(getUUID))) {
                        Toast.makeText(this, "새로운 차량이 등록되었습니다.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private String toSHA256(String str) {
        String SHA;
        try {
            MessageDigest sh = MessageDigest.getInstance("SHA-256");
            sh.update(str.getBytes());
            byte byteData[] = sh.digest();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < byteData.length; i++) {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).toUpperCase().substring(1));
            }
            SHA = sb.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            SHA = null;
        }
        return SHA;
    }
}
