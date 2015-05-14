package com.brainblendr.azteccollect;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.apache.http.HttpException;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {
    public static final int REQUEST_CODE = 0x0000c0de;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void scanBarcode(View view) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Loading Scanner...");
        pd.show();
        startScannerTask sct = new startScannerTask(this, pd);
        sct.execute();
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        postDataTask pdt = new postDataTask(this);
        pdt.execute(result);
    }

    public String performPostCall(String requestURL,
                                  HashMap<String, String> postDataParams) {

        URL url;
        String response = "";
        try {
            url = new URL(requestURL);

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setChunkedStreamingMode(0);
            conn.setDoInput(true);
            conn.setDoOutput(true);


            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(getPostDataString(postDataParams));

            writer.flush();
            writer.close();
            os.close();
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpsURLConnection.HTTP_INTERNAL_ERROR) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                while ((line = br.readLine()) != null) {
                    response += line;
                }
            } else if (responseCode == HttpsURLConnection.HTTP_BAD_METHOD) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                while ((line = br.readLine()) != null) {
                    response += line;
                }
            } else if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    response += line;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }


    private class postDataTask extends AsyncTask<IntentResult, Void, Void> {
        private Activity activity;

        public postDataTask(Activity activity) {
            this.activity = activity;
        }

        @Override
        protected Void doInBackground(IntentResult... result) {
            if (result[0] != null) {
                try {
                    String fName = result[0].getFormatName();
                    String output = "";
                    if (fName.equals("AZTEC")) {
                        if (result[0].getRawBytes() != null) {
                            output = bytesToHex(result[0].getRawBytes());
                            ConnectivityManager connMgr = (ConnectivityManager)
                                    getSystemService(Context.CONNECTIVITY_SERVICE);
                            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                            if (networkInfo != null && networkInfo.isConnected()) {
                                HashMap<String, String> hm = new HashMap<String, String>();
                                hm.put("data", output);
                                String result_s = performPostCall("https://api1.nl/aztec/code/store.json", hm);
                                if (result_s != null) {
                                    JSONObject json = new JSONObject(result_s);
                                    JSONObject error = new JSONObject(json.getString("error"));
                                    if (json.getBoolean("success")) {
                                        Log.i("AZTEC-RESP", "Success");
                                    } else {
                                        Log.e("AZTEC-RESP", error.getString("message"));
                                    }
                                } else {
                                    Log.e("AZTEC-RESP", "Error!");
                                }
                            } else {
                                // No Network
                                //Toast.makeText(this.activity, "No Network, Exiting...", Toast.LENGTH_LONG).show();
                                MainActivity.this.finish();
                                System.exit(0);
                            }
                        }
                    } else {
                        //Toast.makeText(this.activity, fName + " is not the right type", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception exc) {
                    Log.e("AZTEC-PDT", exc.getMessage());
                }
            } else {
                // Continue as normal
            }
            return null;
        }
    }

    private class startScannerTask extends AsyncTask<Void, Void, Void> {
        private Activity activity;
        private ProgressDialog pd;

        public startScannerTask(Activity activity, ProgressDialog pd) {
            this.activity = activity;
            this.pd = pd;
        }

        @Override
        protected void onPostExecute(Void v) {
            if (this.pd.isShowing()) {
                this.pd.dismiss();
            }
            super.onPostExecute(v);
        }

        @Override
        protected Void doInBackground(Void... params) {
            Intent intentScan = new Intent(this.activity, getCaptureActivity());
            intentScan.setAction("com.google.zxing.client.android.SCAN");
            intentScan.putExtra("SCAN_FORMATS", "AZTEC");
            intentScan.putExtra("PROMPT_MESSAGE", "Scan KeyCard");
            intentScan.putExtra("RESULT_DISPLAY_DURATION_MS", Long.valueOf(0));
            intentScan.putExtra("SCAN_ORIENTATION", ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            this.activity.startActivityForResult(intentScan, REQUEST_CODE);
            return null;
        }
    }

    protected Class<?> getCaptureActivity() {

        try {
            return Class.forName("com.google.zxing.client.android.CaptureActivity");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not find CaptureActivity. Make sure one of the zxing-android libraries are loaded.", e);
        }

    }
}
