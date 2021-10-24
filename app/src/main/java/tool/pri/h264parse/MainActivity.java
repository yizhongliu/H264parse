package tool.pri.h264parse;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.nio.channels.InterruptedByTimeoutException;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "MainActivity";

    private HandlePaserListener paserListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();

        StringBuilder stringBuilder = new StringBuilder(getExternalCacheDir().getAbsolutePath());
        stringBuilder.append("/");
        stringBuilder.append("sintel.h264");

        Log.e(TAG, "file name：" + stringBuilder.toString());

        H264Paser h264Paser = new H264Paser();
//        h264Paser.parseH264("/data/data/tool.pri.h264parse/cache/sintel.h264");
        paserListener = new HandlePaserListener();
        h264Paser.setPaserListener(paserListener);
        h264Paser.startPaserH264("/data/data/tool.pri.h264parse/cache/sintel.h264");
    }

    public class HandlePaserListener implements H264Paser.OnPaserCallback {

        @Override
        public void onPaserDone() {
            Log.e(TAG, "onPaserDone");
        }
    }
}