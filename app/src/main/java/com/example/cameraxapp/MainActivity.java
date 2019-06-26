package com.example.cameraxapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.io.File;
import java.util.List;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private Preview mPreview;
    private TextureView textureView;
    private String DIRECTORY_NAME = "CameraX";
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView = findViewById(R.id.texture);
        button = findViewById(R.id.btn_take_picture);

        setPermission();
    }

    private void setPermission() {
        TedPermission.with(this)
                .setPermissionListener(permissionlistener)
                .setPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA)
                .check();
    }

    PermissionListener permissionlistener = new PermissionListener() {
        @Override
        public void onPermissionGranted() {
            startCamera();
            Log.i(TAG, "onPermissionGranted: granted");
        }

        @Override
        public void onPermissionDenied(List<String> deniedPermissions) {
            Toast.makeText(MainActivity.this, "Permission not granted", Toast.LENGTH_SHORT).show();
        }
    };

    void startCamera() {
        CameraX.unbindAll();

        int aspRatioW = textureView.getWidth();
        int aspRatioH = textureView.getHeight();

        Rational rational = new Rational(aspRatioW, aspRatioH);
        Size screenSize = new Size(aspRatioW, aspRatioH);

        PreviewConfig previewConfig = new PreviewConfig.Builder()
                .setTargetAspectRatio(rational)
                .setTargetResolution(screenSize)
                .build();

        mPreview = new Preview(previewConfig);

        mPreview.setOnPreviewOutputUpdateListener(new Preview.OnPreviewOutputUpdateListener() {
            @Override
            public void onUpdated(Preview.PreviewOutput output) {
                ViewGroup parent = (ViewGroup) textureView.getParent();

                parent.removeView(textureView);
                parent.addView(textureView, 0);

                textureView.setSurfaceTexture(output.getSurfaceTexture());
                updateTransform(textureView);
            }
        });

        ImageCaptureConfig imgCapConfig = new ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();
        final ImageCapture imgCap = new ImageCapture(imgCapConfig);

        captureImage(imgCap);
        analyzeImage(imgCap);
    }


    public void captureImage(final ImageCapture imgCap) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    File storageFolder = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_NAME).toString());

                    if (!storageFolder.exists()) {
                        if (!storageFolder.mkdirs()) {
                            Log.e(TAG, "Failed to create directory");
                        }
                    }

                    File file = new File(storageFolder.getPath() + File.separator
                            + "IMG_" + System.currentTimeMillis() + ".jpg");
                    imgCap.takePicture(file, new ImageCapture.OnImageSavedListener() {
                        @Override
                        public void onImageSaved(@NonNull File file) {
                            String msg = "Photo capture succeeded: " + file.getAbsolutePath();
                            Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onError(@NonNull ImageCapture.UseCaseError useCaseError,
                                            @NonNull String message, @Nullable Throwable cause) {
                            String msg = "Photo capture failed: " + message;
                            Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
                            if (cause != null) {
                                cause.printStackTrace();
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "onClick: " + e.getMessage());
                }
            }
        });

    }

    void analyzeImage(final ImageCapture imgCap) {
        ImageAnalysisConfig imgAConfig = new ImageAnalysisConfig.Builder()
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE).build();
        ImageAnalysis analysis = new ImageAnalysis(imgAConfig);

        analysis.setAnalyzer(
                new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(ImageProxy image, int rotationDegrees) {
                        //y'all can add code to analyse stuff here idek go wild.
                    }
                });

        //bind to lifecycle:
        CameraX.bindToLifecycle(this, analysis, imgCap, mPreview);
    }


    private void updateTransform(TextureView txView) {
        /*
         * compensates the changes in orientation for the viewfinder, bc the rest of the layout stays in portrait mode.
         * methinks :thonk:
         * imgCap does this already, this class can be commented out or be used to optimise the preview
         */
        Matrix mx = new Matrix();
        float w = txView.getMeasuredWidth();
        float h = txView.getMeasuredHeight();

        float centreX = w / 2f; //calc centre of the viewfinder
        float centreY = h / 2f;

        int rotationDgr;
        int rotation = (int) txView.getRotation(); //cast to int bc switches don't like floats

        switch (rotation) { //correct output to account for display rotation
            case Surface.ROTATION_0:
                rotationDgr = 0;
                break;
            case Surface.ROTATION_90:
                rotationDgr = 90;
                break;
            case Surface.ROTATION_180:
                rotationDgr = 180;
                break;
            case Surface.ROTATION_270:
                rotationDgr = 270;
                break;
            default:
                return;
        }

        mx.postRotate((float) rotationDgr, centreX, centreY);
        txView.setTransform(mx); //apply transformations to textureview
    }
}
