package com.brainblendr.azteccollect;

import android.content.Intent;
import android.content.pm.ActivityInfo;
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
        IntentIntegrator ig = new IntentIntegrator(this);
        ig.setPrompt("Scan KeyCard");
        ig.setResultDisplayDuration(0);
        ig.setDesiredBarcodeFormats(Collections.singleton("AZTEC"));
        ig.setOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        ig.initiateScan();
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
        // TODO: add getRawBytes() functionality (https://github.com/zxing/zxing/blob/master/core/src/main/java/com/google/zxing/aztec/decoder/Decoder.java#L79 needs to be fixed)
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            try {
                String fName = result.getFormatName();
                String output;
                if (fName.equals("AZTEC")) {
                    if (result.getRawBytes() != null) {
                        output = bytesToHex(result.getRawBytes());
                    } else {
                        output = result.getContents();
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
}
