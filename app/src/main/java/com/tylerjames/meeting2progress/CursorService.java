package com.tylerjames.meeting2progress;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import java.util.Timer;

public class CursorService extends Service {

    private WindowManager mWindowManager;
    private View mFloatingView;

    Handler handler;

    private WindowManager.LayoutParams params;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * The onCreate() method
     * @params - none
     * Builds and starts a layout view, that overlays the entire phone (over everything)
     * Starts the cursor moving service found in moveCursor()
     */
    @Override
    public void onCreate() {
        super.onCreate();

        //Inflate the floating view layout we created
        mFloatingView = LayoutInflater.from(this).inflate(R.layout.cursor_layout, null);

        // Handler to help us process the cursor movement thread in the background
        handler = new Handler();

        // Check phone version for correct flag to get layout to display correctly
        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        //Add the view to the window.
        // add the correct layout paramters
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        //Specify the view position
        params.gravity = Gravity.TOP | Gravity.LEFT;        //Initially view will be added to top-left corner
        params.x = 0;
        params.y = 100;

        //Add the view to the window
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mFloatingView, params);

        // Every 1 second it will call the movecursor function to move the cursor
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        moveCursor();
                    }
                },
                1000
        );
    }

    // Starts a runnable handler thread so we can update the layout (cursor position)
    private void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }

    /**
     * Function to move the cursor position on the screen
     * Runs a new UI thread in the background that continuously updates the X and Y position of the cursor params
     * get the coords from the OpenCV Main Activity onCameraFrame
     * set the params to the x and y
     */
    public void moveCursor() {
        // Allows us to update the layout outside of onCreate()
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Main activity function to retrieve current X and y position
                int[] coords = MainActivity.getCoords();

                // Sets the cursor x and y to the pulled coords
                params.x = coords[0];
                params.y = coords[1];
                Log.e("x: ", String.valueOf(params.x));
                Log.e("y: ", String.valueOf(params.y));
                mWindowManager.updateViewLayout(mFloatingView, params);
            }
        });
    }

    // Default method for destroying view.
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFloatingView != null) mWindowManager.removeView(mFloatingView);
    }
}