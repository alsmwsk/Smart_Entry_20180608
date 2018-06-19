package kr.co.bastion.smartentry;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class LoginActivity extends Activity {
    WebView webView;
    WebSettings webSettings;
    String code = "";
    static String DeviceID = "";
    static final String pushRegId = "APA91bHT3wTnIE1mFWT_R8u9XTyttR8Z6takb3yNiBZKouzrG9RY-HPA9_NkMSCt-JXlK9Kosx4bcj4b5ZMM-VX4Vbhb3ivI7TKkgLvYfJEZik_734UPBfwUaDA07r-I-Mi4s-wQMUtV";

    private long backPressedTime = 0;
    private final long FINISH_INTERVAL_TIME = 2000;

    static final String ACCOUNT_STATE = "smart_entry_account_01";
    static final String CLIENT_ID = "25fa8900-60b0-4f5d-802b-04c7168f64ea";
    static final String client_secret = "secret";

    static final String GET_OAUTH_URL = "http://bluelink.connected-car.io/api/v1/user/oauth2/authorize"; //auth2 인증 url
    static final String REDIRECT_URI = "http://bluelink.connected-car.io/api/v1/user/oauth2/redirect";
    static final String POST_TOKEN_URL = "http://bluelink.connected-car.io/api/v1/user/oauth2/token";
    static final String GET_USER_PROFILE_URL = "http://bluelink.connected-car.io/api/v1/user/profile";
    static final String POST_PUSH_DEVICE_REGISTRATION_URL = "http://bluelink.connected-car.io/api/v1/spa/notifications/register";
    static final String SIGN_URL = "http://bluelink.connected-car.io/signin";

    //디바이스를 등록하는 부분인데.. 시퀀스 상에서는 로그인을 한후에 디바이스 등록을 하기로 되어 있는데 앱내의 소스코드상에서는 앱을 실행시키자마자
    //무조건 push를 보내기로 되어있어 헷갈림..
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        new Thread() { // 어플 실행시 push를 등록하는 부분. 로그인에 최초로 성공했을때 한번만 실행된다. Device ID 요청
            @Override
            public void run() {
                try {
                    URL url = new URL(POST_PUSH_DEVICE_REGISTRATION_URL);

                    JSONObject jsonObject = new JSONObject();

                    jsonObject.accumulate("uuid", "9b79beef26fcc499"); //(Mandatory) The unique id of the phone 핸드폰 고유값 이 값은 누가 정해주는 것인가?
                    jsonObject.accumulate("pushRegId", pushRegId); //(Mandatory) Push registration ID push 등록번호
                    jsonObject.accumulate("pushType", "GCM"); //(Mandatory) The type of push ('GCM' or 'APNS' or 'APNS_SANDBOX')

                    String json = jsonObject.toString();

                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("ccsp-service-id", CLIENT_ID); // The service id that can distinguish the service type (ex-6e605695-6a28-45aa-953231154e14c7e2)
                    //ccsp-service를 받을 수 있는 id


                    connection.setConnectTimeout(60000);
                    connection.setReadTimeout(60000);
                    connection.setDoOutput(true); //소켓통신할려고 쓰는거 GET방식 일 때는 필요없음.
                    connection.setDoInput(true); // 소켓통신할려고 쓰는거 값을 받아올때

                    OutputStream output = connection.getOutputStream();
                    output.write(json.getBytes()); // 이거는 잘 모르겠음..
                    output.flush();
                    output.close();
                    connection.connect();
                    try {
                        int result = connection.getResponseCode();
                        Log.d("Msg", String.valueOf(result));
                        if (result == 200) { // result 값이 현재 400 으로 나오는 상태입니다.
                            connection.disconnect();

                            //json 읽어오기
                            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));

                            String line;
                            String page = "";

                            while ((line = reader.readLine()) != null) {
                                page += line;
                            }

                            Log.d("Msg", "전체내용 : " + page);
                            try {
                                jsonObject = new JSONObject(page);
                                String retCode = jsonObject.getString("retCode");
                                String resCode = jsonObject.getString("resCode");
                                String resMsg = jsonObject.getString("resMsg");
                                String msgID = jsonObject.getString("msgID");

                                if (retCode.equals("S")) {
                                    jsonObject = new JSONObject(resMsg); // 조금헷갈림
                                    DeviceID = jsonObject.getString("deviceId");
                                } else {
                                    Toast.makeText(getApplicationContext(),"푸시 등록 실패("+resMsg+")",Toast.LENGTH_SHORT).show();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (Exception e) {
                        connection.disconnect();
                    }
                } catch (MalformedURLException murle){
                    murle.printStackTrace();
                } catch (IOException ioe){
                    ioe.printStackTrace();
                } catch (Exception e) {
                    Log.d("Msg", e.toString());
                }
            }
        }.start();
        // 푸시 등록 종료

        // 로그인 페이지 시작

        webView = findViewById(R.id.loginWebView);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) { // 로그인 화면에서 로그인을 누르면 아래 내용이 실행됨.
                super.onPageFinished(view, url);
                System.out.println(url);
                if (url.contains(REDIRECT_URI)) { // 넘어온 페이지의 url이 리다이렉트 url이 맞는지 확인하고,
                    int idx = url.indexOf("state"); // 이해하기 힘든부분 state가 몇번째 인덱스에서 시작하는지 알려줌 시작인덱스는 0부터 없으면 -1 리턴함
                    String state = url.substring(idx + 6); // 이해하기 힘든부분

                    if (idx < 0 || !state.equals(ACCOUNT_STATE)) { // 앞서 입력한 state 값이 정상적인지 체크.
                        Toast.makeText(getApplicationContext(), "잘못된 접근입니다.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                    idx = url.indexOf("code");
                    code = url.substring(idx + 5, idx + 5 + 22); // 인증 코드 획득

                    new Thread(){
                        public void run() { //
                            try {
                                URL url = new URL(POST_TOKEN_URL); // 인증 코드를 통해 엑세스 토큰을 획득 시도
                                Log.d("Msg", "code:" + code);

                                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                                connection.setRequestMethod("POST");

                                String base64EncodedString = "Basic " + Base64.encodeToString((CLIENT_ID + ":" + client_secret).getBytes(), Base64.NO_WRAP); //base64 암호화방식으로 암호화
                                String tempData = "grant_type=authorization_code&code=" + code + "&redirect_uri=" + REDIRECT_URI;

                                connection.setConnectTimeout(60000);
                                connection.setRequestProperty("Authorization", base64EncodedString);
                                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                                connection.setReadTimeout(60000);
                                connection.setDoOutput(true);
                                connection.setDoInput(true);

                                OutputStream output = connection.getOutputStream();
                                output.write(tempData.getBytes());
                                output.close();
                                connection.connect();

                                int result = connection.getResponseCode();
                                if (result == 200) { // 획득 성공했다면, 돌려받은 json 내용을 분석하여 액세스 토큰을 획득
                                    connection.disconnect();

                                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));

                                    String line;
                                    StringBuffer page = new StringBuffer();

                                    while ((line = reader.readLine()) != null) {
                                        page.append(line);
                                    }

                                    Log.d("Msg", "전체내용 : " + page);
                                    try {
                                        JSONObject jsonObject = new JSONObject(page.toString());
                                        String tokenString = jsonObject.getString("access_token"); // 획득한 Access Token 값. access Token 얻기
                                        Log.d("Msg", "토큰 : " + tokenString);

                                        url = new URL(GET_USER_PROFILE_URL);
                                        connection = (HttpURLConnection) url.openConnection();
                                        connection.setConnectTimeout(60000);
                                        connection.setReadTimeout(60000);
                                        connection.setRequestMethod("GET");
                                        connection.setRequestProperty("Authorization", "Bearer " + tokenString);

                                        result = connection.getResponseCode();
                                        if (result == 200) { // Access Token을 넣어 success로 나오면 해당 유저의 정보를 불러온 후 로그인 완료 처리. 차량 목록 액티비티로 이동함.
                                            // 4xx이 나오면 에러..
                                            reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                                            page.delete(0, page.length()); // 앞서 사용한 스트링 초기화
                                            while ((line = reader.readLine()) != null)
                                                page.append(line);

                                            jsonObject = new JSONObject(page.toString());
                                            String user_ID = jsonObject.getString("id");
                                            String user_EMAIL = jsonObject.getString("email");
                                            String user_NAME = jsonObject.getString("name");
                                            String user_MOBILENUMBER = jsonObject.getString("mobileNum");

                                            Intent resultIntent = new Intent(LoginActivity.this, VehicleListActivity.class); //차량목록 화면으로 넘어간다.
                                            resultIntent.putExtra("ID", user_ID);
                                            resultIntent.putExtra("Email", user_EMAIL);
                                            resultIntent.putExtra("Name", user_NAME);
                                            resultIntent.putExtra("MobileNum", user_MOBILENUMBER);
                                            resultIntent.putExtra("AccessToken", tokenString);
                                            startActivity(resultIntent);

                                        } else {
                                            System.out.println("유저 정보 호출 실패 / error : " + result);
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    Log.d("Msg", "액세스 토큰 획득 실패 / error:" + result);
                                    connection.disconnect();
                                    finish();
                                }
                            } catch (NullPointerException npe) {
                                Log.d("Msg", "Null Pointer Exception");
                            } catch (Exception e) {
                                Log.d("Msg", e.getMessage());
                            }

                            finish();
                        }
                    }.start();
                }
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
        webView.setNetworkAvailable(true);

        webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webView.addJavascriptInterface(new MyJavascriptInterface(), "Android");

        webView.loadUrl(GET_OAUTH_URL + "?response_type=code&client_id=" + CLIENT_ID + "&redirect_uri=" + REDIRECT_URI + "&state=" + ACCOUNT_STATE); // 1차적으로 OAUTH2 인증 후 redirect 페이지로 이동
    }

    public class MyJavascriptInterface {

        @JavascriptInterface
        public void getHtml(String html) { //위 자바스크립트가 호출되면 여기로 html이 반환됨
            System.out.println(html);
        }
    }

   /* private void setRegisterPushDevice() {
        new Thread() {
            @Override
            public void run() {
                try {
                    URL url = new URL(POST_PUSH_DEVICE_REGISTRATION_URL);

                    JSONObject jsonObject = new JSONObject();
                    //jsonObject.accumulate("uuid","9b79beef26fcc499");
                    //jsonObject.accumulate("pushRegId","eZru3y9bptA:APA91bHT3wTnIE1mFWT_R8u9XTyttR8Z6takb3yNiBZKouzrG9RY-HPA9_NkMSCt-JXlK9Kosx4bcj4b5ZMM-VX4Vbhb3ivI7TKkgLvYfJEZik_734UPBfwUaDA07r-I-Mi4s-wQMUtV");

                    jsonObject.accumulate("uuid", "9b79beef26fcc499");
                    jsonObject.accumulate("pushRegId", "APA91bHT3wTnIE1mFWT_R8u9XTyttR8Z6takb3yNiBZKouzrG9RY-HPA9_NkMSCt-JXlK9Kosx4bcj4b5ZMM-VX4Vbhb3ivI7TKkgLvYfJEZik_734UPBfwUaDA07r-I-Mi4s-wQMUtV");
                    jsonObject.accumulate("pushType", "GCM");

                    String json = jsonObject.toString();

                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("ccsp-service-id", CLIENT_ID);

                    connection.setConnectTimeout(60000);
                    connection.setReadTimeout(60000);
                    connection.setDoOutput(true);
                    connection.setDoInput(true);

                    OutputStream output = connection.getOutputStream();
                    output.write(json.getBytes());
                    output.flush();
                    output.close();
                    connection.connect();
                    try {
                        int result = connection.getResponseCode();
                        Log.d("Msg", String.valueOf(result));
                        if (result == 200) {
                            connection.disconnect();

                            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));

                            String line;
                            String page = "";

                            while ((line = reader.readLine()) != null) {
                                page += line;
                            }

                            Log.d("Msg", "전체내용 : " + page);
                            try {
                                jsonObject = new JSONObject(page);
                                String retCode = jsonObject.getString("retCode");
                                String resCode = jsonObject.getString("resCode");
                                String resMsg = jsonObject.getString("resMsg");
                                String msgID = jsonObject.getString("msgID");

                                if (retCode.equals("S")) {
                                    jsonObject = new JSONObject(resMsg);
                                    DeviceID = jsonObject.getString("deviceId");
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (Exception e) {
                        connection.disconnect();
                    }
                } catch (MalformedURLException murle){
                    murle.printStackTrace();
                } catch (IOException ioe){
                    ioe.printStackTrace();
                } catch (Exception e) {
                    Log.d("Msg", e.toString());
                }
            }
        }.start();
    }
*/

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        long tempTime = System.currentTimeMillis();
        long intervalTime = tempTime - backPressedTime;

        if ( (keyCode == KeyEvent.KEYCODE_BACK) && (webView.getUrl().equals(SIGN_URL)) ){
            if (0 <= intervalTime && FINISH_INTERVAL_TIME >= intervalTime) {
                super.onBackPressed();
                return false;
            } else {
                backPressedTime = tempTime;
                Toast.makeText(this, "뒤로 버튼을 한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();
                keyCode = KeyEvent.KEYCODE_UNKNOWN;
            }
        }
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
            webView.goBack();
            if (webView.getOriginalUrl().equals(SIGN_URL)){
                webView.loadUrl(GET_OAUTH_URL + "?response_type=code&client_id=" + CLIENT_ID + "&redirect_uri=" + REDIRECT_URI + "&state=" + ACCOUNT_STATE);
            }
            return true;
        } else if ((keyCode == KeyEvent.KEYCODE_BACK) && !(webView.canGoBack())) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);

    }

}
