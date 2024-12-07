package com.example.testrenderscript;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.renderscript.Allocation;
import androidx.renderscript.RenderScript;
import android.util.Log;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.renderscriptexample.ScriptC_simple;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "HelloCompute";

    private Bitmap mBitmapIn;
    private Bitmap mBitmapOutRS;
    private Bitmap mBitmapOutJava;

    private RenderScript mRS;
    private Allocation mInAllocation;
    private Allocation mOutAllocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBitmapIn = loadBitmap(R.drawable.data);
        int w = mBitmapIn.getWidth();
        int h = mBitmapIn.getHeight();
        mBitmapOutRS = Bitmap.createBitmap(w, h, mBitmapIn.getConfig());
        mBitmapOutJava = Bitmap.createBitmap(w, h, mBitmapIn.getConfig());

        ImageView in = findViewById(R.id.displayin);
        in.setImageBitmap(mBitmapIn);

        ImageView outRS = findViewById(R.id.displayoutrs);
        outRS.setImageBitmap(mBitmapOutRS);

        ImageView outJava = findViewById(R.id.displayoutjava);
        outJava.setImageBitmap(mBitmapOutJava);

        // RenderScript
        long t1 = System.nanoTime();

        createScript();

        long t2 = System.nanoTime();
        Log.d(TAG, "RenderScript: " + (t2 - t1)/1000);

        // Java
        t1 = System.nanoTime();

        int size = w * h;
        int[] pixels = new int[size];
        mBitmapIn.getPixels(pixels, 0, w, 0, 0, w, h);

        // Brighten each pixel
        final int BRIGHTNESS_FACTOR = 50; // Adjust as needed
        for (int i = 0; i < size; i++) {
            int c = pixels[i]; // 0xAARRGGBB
            int r = (c >> 16) & 0xFF; // Extract red
            int g = (c >> 8) & 0xFF;  // Extract green
            int b = c & 0xFF;         // Extract blue

            // Increase brightness (clamp to 255 to prevent overflow)
            r = Math.min(255, r + BRIGHTNESS_FACTOR);
            g = Math.min(255, g + BRIGHTNESS_FACTOR);
            b = Math.min(255, b + BRIGHTNESS_FACTOR);

            // Recombine components into ARGB format
            pixels[i] = (c & 0xFF000000) | (r << 16) | (g << 8) | b;
        }

        // Apply modified pixels back to the output bitmap
        mBitmapOutJava.setPixels(pixels, 0, w, 0, 0, w, h);
        t2 = System.nanoTime();
        Log.d(TAG, "Java: " + (t2 - t1)/1000);

    }


    private void createScript() {
        // Step 1: Initialize the RenderScript context
        mRS = RenderScript.create(this);

        // Step 2: Initialize the ScriptC_simple class
        ScriptC_simple script = new ScriptC_simple(mRS);

        // Step 3: Create input Allocation from a Bitmap
        mInAllocation = Allocation.createFromBitmap(mRS, mBitmapIn,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT);

        // Step 4: Create output Allocation with the same type as input
        mOutAllocation = Allocation.createTyped(mRS, mInAllocation.getType());

        // Step 5: Run the brighten kernel
        script.forEach_brighten(mInAllocation, mOutAllocation);

        // Step 6: Copy the processed output back to a bitmap
        mOutAllocation.copyTo(mBitmapOutRS);

        // Cleanup resources
        mInAllocation.destroy();
        mOutAllocation.destroy();
        script.destroy();
        mRS.destroy();
    }

    private Bitmap loadBitmap(int resource) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeResource(getResources(), resource, options);
    }
}