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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

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

    private void postData(String data) {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            String result = executePost("https://www.api1.nl/aztec/code/store.json", "data=" + data);
            if (result != null) {
                Toast.makeText(this, result, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "An Error Occured, Try Again Later.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No Network, Exiting...", Toast.LENGTH_LONG).show();
            MainActivity.this.finish();
            System.exit(0);
        }

    }

    public static String executePost(String targetURL, String urlParameters) {
        URL url;
        HttpURLConnection connection = null;
        try {
            //Create connection
            url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");

            connection.setRequestProperty("Content-Length", "" +
                    Integer.toString(urlParameters.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");

            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream(
                    connection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();

            //Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();

        } catch (Exception e) {

            e.printStackTrace();
            return null;

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
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
                            postData(output);
                        }
                    } else {
                        Toast.makeText(this.activity, fName + " is not the right type", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception exc) {
                    Log.e("AZTEC-COLLECT", exc.getMessage());
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
            intentScan.putExtra("RESULT_DISPLAY_DURATION_MS", 0);
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
