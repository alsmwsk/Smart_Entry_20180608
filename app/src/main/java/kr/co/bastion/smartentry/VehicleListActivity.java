package kr.co.bastion.smartentry;

import android.content.Intent;
import android.os.Bundle;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class VehicleListActivity extends AppCompatActivity {
    private long backPressedTime = 0;
    private final long FINISH_INTERVAL_TIME = 2000;

    static final String CLIENT_ID = "25fa8900-60b0-4f5d-802b-04c7168f64ea";

    static String GET_VEHICLE_LIST_URL = "http://bluelink.connected-car.io/api/v1/spa/vehicles";
    static String REDIRECT_URI = "http://bluelink.connected-car.io/api/v1/user/oauth2/redirect";
    static String GET_REGISTER_CAR_URL = "http://bluelink.connected-car.io/api/v1/profile/vehicles";
    static String GET_REQUEST_CAR_SHARING_URL = "http://bluelink.connected-car.io/api/v1/profile/users/";

    static String userID;
    static String userEMail;
    static String userName;
    static String userMobileNum;
    static String AccessToken;
    static String carID = "";
    static String shareID = "";

    TextView txvUsername;
    TextView txvUsermail;

    WebView webView;

    LinearLayout linearLayout_NoVehicle;
    ScrollView scrollView_VehicleList;

    LinearLayout LayoutCarShareByOwner;
    LinearLayout LayoutCarRemote;

    ImageView imgBack;
    ImageView imgOptions;
    Button btnRegisterVehicles;
    Button btnRegisterVehicles2;
    Button btnRequestVehiclesShareByUser; // 차량공유요청


    Boolean isExistVehicles;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehiclelist);

        webView = findViewById(R.id.webview1);

        setIntentParams(); // 로그인 페이지에서 받은 내용들을 설정하는 부분

        Boolean isVehicleRegistered = getVehicleList(); // 유저의 정보를 이용하여 유저에게 등록된 차량을 확인하는 부분.
        setUIInfo(isVehicleRegistered); // 등록되어 있는 차량이 있으면 차량 목록이 나타나도록 설정하고
                                        // 등록되어 있지 않으면 차량이 없는 화면이 나타나도록 설정.
                                        // 현재 차량 목록은 직접 하드코딩으로 넣어둔 상태입니다.
                                        // 차량목록 결과요청 함수


    }

    //차량목록 결과 받아오는 함수
    private void setUIInfo(Boolean isVehicleRegistered) {
        linearLayout_NoVehicle = findViewById(R.id.linearLayout_NoVehicle);
        scrollView_VehicleList = findViewById(R.id.ScrollView_VehicleList);

        //차량소유자가 차량공유버튼 눌렀을 경우
        LayoutCarShareByOwner = findViewById(R.id.LayoutCarShareByOwner);
        LayoutCarShareByOwner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        System.out.println(url);
                        if (url.contains(REDIRECT_URI)) {

                            webView.setVisibility(View.INVISIBLE);
                            int idx = url.indexOf("share_id"); // 공유id
                            String id = url.substring(idx + 9);

                            if (idx < 0) {
                                Toast.makeText(getApplicationContext(), "차량 정보를 불러올 수 없습니다.", Toast.LENGTH_LONG).show();
                                finish();
                            } else {
                                shareID = id; // 공유받은 차량 ID 획득
                            }
                        }
                    }
                });
                webView.setWebChromeClient(new WebChromeClient());
                webView.setNetworkAvailable(true);

                WebSettings webSettings = webView.getSettings();
                webSettings.setJavaScriptEnabled(true);
                webSettings.setDomStorageEnabled(true);
                webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
                webView.addJavascriptInterface(new MyJavascriptInterface(), "Android");

                Map<String, String> extraHeaders = new HashMap<String, String>();
                extraHeaders.put("Authorization","Bearer "+AccessToken); //extraHeader... 규격서 참고해야함.
                extraHeaders.put("Accept-Language","KO");
                webView.setVisibility(View.VISIBLE);
                webView.loadUrl(GET_REQUEST_CAR_SHARING_URL+userID+"/cars/"+carID+"/share",extraHeaders); // carID를 받아오지 못하여 확인할 수 없는 상황입니다.
            }
        });

        //차량원격제어 클릭했을때 실행되는것
        LayoutCarRemote = findViewById(R.id.LayoutCarRemote);
        LayoutCarRemote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(VehicleListActivity.this, HomeActivity.class);
                startActivity(intent);
            }
        });

        txvUsername = findViewById(R.id.txvUsername);
        txvUsername.setText(userName);
        txvUsermail = findViewById(R.id.txvUsermail);
        txvUsermail.setText(userEMail);

        //뒤로가기 버튼 누르면 로그인 페이지로 이동.
        imgBack = findViewById(R.id.imgBack);
        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(VehicleListActivity.this,LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
        imgOptions = findViewById(R.id.imgOptions);
        imgOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(VehicleListActivity.this,SettingActivity.class);
                startActivity(intent);
            }
        });
        if (!isVehicleRegistered){
            linearLayout_NoVehicle.setVisibility(View.VISIBLE);
            scrollView_VehicleList.setVisibility(View.INVISIBLE);
        } else {
            linearLayout_NoVehicle.setVisibility(View.INVISIBLE);
            scrollView_VehicleList.setVisibility(View.VISIBLE);
        }

        //신규 차량 등록 요청이 2가지가 있다..
        //btnRegisterVehicles 아무것도 없는 상태에서 차량등록
        //btnRegisterVehicles2 차량이 1개이상 있는 상태에서 차량등록

        btnRegisterVehicles = findViewById(R.id.btnRegisterVehicles);
        btnRegisterVehicles.setOnClickListener(new View.OnClickListener() { // 등록된 차량이 없는 화면에서의 차량 등록버튼
            @Override
            public void onClick(View v) {
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) { // 웹 페이지가 닫히면서 차량 ID가 들어와야 하는데 VIN 내용을 몰라서 테스트하지 못했습니다.
                        super.onPageFinished(view, url);
                        System.out.println(url);
                        if (url.contains(REDIRECT_URI)) {

                            webView.setVisibility(View.INVISIBLE);
                            int idx = url.indexOf("car_id");
                            String id = url.substring(idx + 7); // car id

                            if (idx < 0) {
                                Toast.makeText(getApplicationContext(), "차량 등록 실패", Toast.LENGTH_LONG).show();
                            } else {
                                carID = id;
                            }
                        }
                    }
                });
                webView.setWebChromeClient(new WebChromeClient());
                webView.setNetworkAvailable(true);

                WebSettings webSettings = webView.getSettings();
                webSettings.setJavaScriptEnabled(true);
                webSettings.setDomStorageEnabled(true);
                webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
                webView.addJavascriptInterface(new MyJavascriptInterface(), "Android");

                Map<String, String> extraHeaders = new HashMap<String, String>();
                extraHeaders.put("Authorization","Bearer "+AccessToken);
                extraHeaders.put("Accept-Language","KO");
                webView.setVisibility(View.VISIBLE);
                webView.loadUrl(GET_REGISTER_CAR_URL,extraHeaders); //차량등록화면 웹뷰
            }
        });
        btnRegisterVehicles2 = findViewById(R.id.btnRegisterVehicles2); // 등록된 차량이 있는 화면에서의 차량 등록 버튼
        btnRegisterVehicles2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        System.out.println(url);
                        if (url.contains(REDIRECT_URI)) {

                            webView.setVisibility(View.INVISIBLE);
                            int idx = url.indexOf("car_id");
                            String id = url.substring(idx + 7);

                            if (idx < 0) {
                                Toast.makeText(getApplicationContext(), "차량 등록 실패", Toast.LENGTH_LONG).show();
                                finish();
                            } else {
                                carID = id;
                            }
                        }
                    }
                });
                webView.setWebChromeClient(new WebChromeClient());
                webView.setNetworkAvailable(true);

                WebSettings webSettings = webView.getSettings();
                webSettings.setJavaScriptEnabled(true);
                webSettings.setDomStorageEnabled(true);
                webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
                webView.addJavascriptInterface(new MyJavascriptInterface(), "Android");

                Map<String, String> extraHeaders = new HashMap<String, String>();
                extraHeaders.put("Authorization","Bearer "+AccessToken);
                extraHeaders.put("Accept-Language","KO");
                webView.setVisibility(View.VISIBLE);
                webView.loadUrl(GET_REGISTER_CAR_URL,extraHeaders);
            }
        });

        btnRequestVehiclesShareByUser = findViewById(R.id.btnRequestVehiclesShareByUser); // 사용자가 계약자에게 요청하는 버튼
        btnRequestVehiclesShareByUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        System.out.println(url);
                        if (url.contains(REDIRECT_URI)) {

                            webView.setVisibility(View.INVISIBLE);
                            int idx = url.indexOf("share_id");
                            String id = url.substring(idx + 9);

                            if (idx < 0) {
                                Toast.makeText(getApplicationContext(), "차량 정보를 불러올 수 없습니다.", Toast.LENGTH_LONG).show();
                                finish();
                            } else {
                                shareID = id; // 공유받은 차량 ID 획득
                            }
                        }
                    }
                });
                webView.setWebChromeClient(new WebChromeClient());
                webView.setNetworkAvailable(true);

                WebSettings webSettings = webView.getSettings();
                webSettings.setJavaScriptEnabled(true);
                webSettings.setDomStorageEnabled(true);
                webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
                webView.addJavascriptInterface(new MyJavascriptInterface(), "Android");

                Map<String, String> extraHeaders = new HashMap<String, String>();
                extraHeaders.put("Authorization","Bearer "+AccessToken); // 규격서확인요망
                extraHeaders.put("Accept-Language","KO"); // 규격서 확인요망
                webView.setVisibility(View.VISIBLE);
                webView.loadUrl(GET_REQUEST_CAR_SHARING_URL+userID+"/shares",extraHeaders);
            }
        });
    }



    // 로그인 액티비티에서 넘겨받은 유저정보값 저장
    private void setIntentParams(){
        Intent thisIntent = getIntent();

        userID = thisIntent.getStringExtra("ID");
        userEMail = thisIntent.getStringExtra("Email");
        userName = thisIntent.getStringExtra("Name");
        userMobileNum = thisIntent.getStringExtra("MobileNum");
        AccessToken = thisIntent.getStringExtra("AccessToken"); // 액세스 토큰
    }


    //차량목록 요청함수
    private Boolean getVehicleList(){
        isExistVehicles = false;

        new Thread() {
            public void run() {
                try {
                    URL url = new URL(GET_VEHICLE_LIST_URL);

                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(60000);
                    connection.setReadTimeout(60000);
                    connection.setRequestProperty("Authorization","Bearer "+ AccessToken);
                    connection.setRequestProperty("Content-Type","application/json");
                    connection.setRequestMethod("GET");

                    int result = connection.getResponseCode();
                    try{
                        if (result == 200) {
                            connection.disconnect();

                            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));

                            String line;
                            String page = "";

                            while ((line = reader.readLine()) != null){
                                page += line;
                            }

                            Log.d("Msg","전체내용 : "+page);
                            try
                            {
                                JSONObject jsonObject = new JSONObject(page);
                                String resCode = jsonObject.getString("resCode"); //결과 코드
                                String resMsg = jsonObject.getString("resMsg"); // 결과 메세지
                                String msgID = jsonObject.getString("msgID"); // 결과 id

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Log.d("Msg", "실패 / error:" + result);
                            connection.disconnect();
                        }
                    } catch (Exception e){
                        Log.d("Msg", "실패 / error:" + result);
                        connection.disconnect();
                    }
                } catch (NullPointerException npe) {
                    Log.d("Msg", "Null Pointer");
                } catch (Exception e) {
                    Log.d("Msg", e.getMessage());
                }
            }
        }.start();


        //return isExistVehicles;
        return true;
    }

    //FINISH_INTERVAL TIME = 2000 2초
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) { // 종료부분 확인하기...
        long tempTime = System.currentTimeMillis(); // 현재시간
        long intervalTime = tempTime - backPressedTime; // 현재시간 - 뒤로가기버튼 누른시간

        if (webView.getVisibility() == View.VISIBLE){
            if (keyCode == KeyEvent.KEYCODE_BACK){
                if (0 <= intervalTime && FINISH_INTERVAL_TIME >= intervalTime) {
                    webView.loadUrl("about:blank");
                    webView.clearHistory();
                    webView.setVisibility(View.INVISIBLE);
                    return false;
                } else {
                    backPressedTime = tempTime;
                    Toast.makeText(this, "뒤로 버튼을 한 번 더 누르면 페이지가 종료됩니다.", Toast.LENGTH_SHORT).show();
                    keyCode = KeyEvent.KEYCODE_UNKNOWN;
                }
            }
        } else {
            if (keyCode == KeyEvent.KEYCODE_BACK){
                if (0 <= intervalTime && FINISH_INTERVAL_TIME >= intervalTime) {
                    super.onBackPressed();
                    return false;
                } else {
                    backPressedTime = tempTime;
                    Toast.makeText(this, "뒤로 버튼을 한 번 더 누르면 어플이 종료됩니다.", Toast.LENGTH_SHORT).show();
                    keyCode = KeyEvent.KEYCODE_UNKNOWN;
                }
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    public class MyJavascriptInterface {

        @JavascriptInterface
        public void getHtml(String html) { //위 자바스크립트가 호출되면 여기로 html이 반환됨
            System.out.println(html);
        }
    }
}
