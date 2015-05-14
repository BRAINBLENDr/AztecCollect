package com.brainblendr.azteccollect;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.Collections;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void scanBarcode(View view)
    {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Loading Scanner...");
        pd.show();
        startScannerTask sct = new startScannerTask(this, pd);
        sct.execute();
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            try {
                String fName = result.getFormatName();
                String output = "";
                if (fName.equals("AZTEC")) {
                    if (result.getRawBytes() != null) {
                        output = bytesToHex(result.getRawBytes());
                    }
                    Toast.makeText(this, output, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, fName + " is not the right type", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception exc)
            {
                Log.e("AZTEC-COLLECT", exc.getMessage());
            }
        } else {
            // Continue as normal
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
            IntentIntegrator ig = new IntentIntegrator(this.activity);
            ig.setPrompt("Scan KeyCard");
            ig.setResultDisplayDuration(0);
            ig.setDesiredBarcodeFormats(Collections.singleton("AZTEC"));
            ig.setOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            ig.initiateScan();
            return null;
        }
    }
}
