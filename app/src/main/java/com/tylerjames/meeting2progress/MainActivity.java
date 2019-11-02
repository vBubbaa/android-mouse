package com.tylerjames.meeting2progress;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.*;
import android.os.SystemClock;
import android.text.style.TtsSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{
    LinearLayout mFloatLayout;
    private static final String TAG = "xxinput";
    //private static final String TAG = MainActivity.class.getSimpleName();
    // Allows camera to start and to process each frame
    CameraBridgeViewBase cameraBridgeViewBase;
    BaseLoaderCallback baseLoaderCallback;

    Button Buttonclick;
    Button changeButtonText;

    // The opencv face classifier
    private CascadeClassifier cascadeClassifier;

    private Mat grayscaleImage;
    private int absoluteFaceSize;

    public static int centerX;
    public static int centerY;

    // Cursor object
    private ImageView cursor;
    // X and Y we will set to move the cursor in moveCursor()
    int x;
    int y;
    int x2;
    int y2;

    int a=1;
    int b=0;
    public void moveCursor(Integer paramX, Integer paramY) {
        x = paramX;
        y = paramY;

        cursor.setX(x);
        cursor.setY(y);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cursor = findViewById(R.id.cursor);

        // Gets the java camera view
        cameraBridgeViewBase = (JavaCameraView)findViewById(R.id.CameraView);

        // Set it to front facing
        cameraBridgeViewBase.setCameraIndex(1);

        // Add the listener to the camera
        cameraBridgeViewBase.setCvCameraViewListener(this);

        // Set it to invisible, but still running
        cameraBridgeViewBase.setAlpha(0);

        // starts the accessability service for android (allowing clicking)
        startService(new Intent(this, MouseAccessabilityService.class));

        // Mouse accessability custom class object
        final MouseAccessabilityService mMouseAccessabilityService = new MouseAccessabilityService();

        Buttonclick = (Button)findViewById(R.id.clickbutton);

        Buttonclick.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {

                Log.e("TAG", Float.toString(changeButtonText.getX()) + Float.toString(changeButtonText.getY()));
                autoClick(centerX, centerY);
            }
        });

        changeButtonText = (Button)findViewById(R.id.mybutton);

        changeButtonText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeButtonText.setText("Second Text");
                //autoClick(centerX, centerY);

            }
        });

        // Checks that it all loaded properly, then enable the view if succes
        baseLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);
                switch(status){
                    case BaseLoaderCallback.SUCCESS:
                        initializeOpenCVDependencies();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }
            }
        };

    }

    private void autoClick(Integer paramX, Integer paramY) {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        x2 = paramX;
        y2 = paramY;

        int metaState = 0;
        final MotionEvent keyDown = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_DOWN,
                x2,
                y2,
                metaState
        );
        final MotionEvent keyUp = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_UP,
                x2,
                y2,
                metaState
        );
        Log.i(TAG, x2 + ", " + y2);

        dispatchTouchEvent(keyDown);
        dispatchTouchEvent(keyUp);

        keyDown.recycle();
        keyUp.recycle();

        Log.i(TAG, "Click");
    }



    // Init all of the classifiers that we used through opencv (found in R.raw)
    // Load the classifiers
    // Catch any errors loading them
    // Start the camera view, we are now ready to process frames
    // This function is called in the baseLoaderCallback function
    private void initializeOpenCVDependencies() {
        try {
            // Copy the resource into a temp file so OpenCV can load it
            InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);


            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            // Load the cascade classifier
            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            cascadeClassifier.load(mCascadeFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e("OpenCVActivity", "Error loading cascade", e);
        }
        // Now we start the view
        cameraBridgeViewBase.enableView();
        // Set it to invisible, but still running
        cameraBridgeViewBase.setAlpha(0);
    }


    // Processes each camera frame
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        // Set it to invisible, but still running
        cameraBridgeViewBase.setAlpha(0);

        Mat frame = inputFrame.rgba();

        // Create a grayscale image
        Imgproc.cvtColor(frame, grayscaleImage, Imgproc.COLOR_RGBA2RGB);

        MatOfRect faces = new MatOfRect();

        // Use the classifier defined in initializeOpenCVDependencies() to detect faces
        if (cascadeClassifier != null) {
            cascadeClassifier.detectMultiScale(grayscaleImage, faces, 1.1, 2, 2,
                    new Size(absoluteFaceSize, absoluteFaceSize), new Size());
        }

        // If there are any faces found, draw a rectangle around it
        Rect[] facesArray = faces.toArray();

        for (int i = 0; i < facesArray.length; i++) {
            centerX = facesArray[i].x + facesArray[i].width / 2;
            centerY = facesArray[i].y + facesArray[i].height / 2;

            Imgproc.circle(frame, new Point(centerX, centerY), 20, new Scalar(0, 255, 0, 255), 3);
            Imgproc.rectangle(frame, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0, 255), 3);

//            Log.e("X", String.valueOf(centerX));
//            Log.e("Y", String.valueOf(centerY));

        }

        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void run() {
                        moveCursor(centerX, centerY);

                    }
                },
                1000
        );

        return frame;
    }

    // Camera Started
    @Override
    public void onCameraViewStarted(int width, int height) {
        grayscaleImage = new Mat(height, width, CvType.CV_8UC4);

        // The faces will be a 20% of the height of the screen
        absoluteFaceSize = (int) (height * 0.2);
        // Set it to invisible, but still running
        cameraBridgeViewBase.setAlpha(0);
    }

    // Camera Stopped
    @Override
    public void onCameraViewStopped() {

    }

    @Override
    protected void onResume() {
        super.onResume();

        // If there is a problem resuming the app
        if (!OpenCVLoader.initDebug()){
            Toast.makeText(getApplicationContext(),"There's a problem!", Toast.LENGTH_SHORT).show();
        }
        else
        {
            baseLoaderCallback.onManagerConnected(baseLoaderCallback.SUCCESS);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        // If there are no problems and it pauses, disable the view
        if(cameraBridgeViewBase!=null){
            cameraBridgeViewBase.disableView();
        }
    }

    // When the app is terminated
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // If there are no problems and the app stops, disable the view
        if (cameraBridgeViewBase!=null){
            cameraBridgeViewBase.disableView();
        }
    }
}
