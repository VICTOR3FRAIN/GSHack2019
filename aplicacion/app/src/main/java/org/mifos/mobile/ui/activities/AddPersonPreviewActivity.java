package org.mifos.mobile.ui.activities;

import android.os.Bundle;

import org.mifos.mobile.R;
import org.mifos.mobile.ui.activities.base.BaseActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.SurfaceView;
import android.widget.ImageButton;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.io.File;
import java.util.Date;
import java.util.List;

import ch.zhaw.facerecognitionlibrary.Helpers.CustomCameraView;
import ch.zhaw.facerecognitionlibrary.Helpers.FileHelper;
import ch.zhaw.facerecognitionlibrary.Helpers.MatName;
import ch.zhaw.facerecognitionlibrary.Helpers.MatOperation;
import ch.zhaw.facerecognitionlibrary.PreProcessor.PreProcessorFactory;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import ch.zhaw.facerecognitionlibrary.Helpers.PreferencesHelper;

import ch.zhaw.facerecognitionlibrary.Recognition.Recognition;
import ch.zhaw.facerecognitionlibrary.Recognition.RecognitionFactory;

import 	android.util.Log;

public class AddPersonPreviewActivity extends BaseActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    public static final int TIME = 0;
    public static final int MANUALLY = 1;
    private CustomCameraView mAddPersonView;
    // The timerDiff defines after how many milliseconds a picture is taken
    private long timerDiff;
    private long lastTime;
    private PreProcessorFactory ppF;
    private FileHelper fh;
    private String folder;
    private String subfolder;
    private String name;
    private int total;
    private int numberOfPictures;
    private int method;
    private ImageButton btn_Capture;
    private boolean capturePressed;
    private boolean front_camera;
    private boolean night_portrait;
    private int exposure_compensation;

    private static final String TAG = "Training";
    TextView progress;
    Thread thread;

    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //addPreferencesFromResource(R.xml.preferences);

        setContentView(R.layout.activity_add_person_preview);

        Intent intent = getIntent();
        folder = intent.getStringExtra("Folder");
        if(folder.equals("Test")){
            subfolder = intent.getStringExtra("Subfolder");
        }
        name = intent.getStringExtra("Name");
        method = intent.getIntExtra("Method", 0);
        capturePressed = false;
        /*if(method == MANUALLY){
            btn_Capture = (ImageButton)findViewById(R.id.btn_Capture);
            btn_Capture.setVisibility(View.VISIBLE);
            btn_Capture.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    capturePressed = true;
                }
            });
        }*/

        fh = new FileHelper();
        total = 0;
        lastTime = new Date().getTime();

        //SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        timerDiff = 500; //Integer.valueOf("500");

        mAddPersonView = (CustomCameraView) findViewById(R.id.AddPersonPreview);
        // Use camera which is selected in settings
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        front_camera = true; //sharedPref.getBoolean("key_front_camera", true);

        numberOfPictures = 10;//Integer.valueOf(sharedPref.getString("key_numberOfPictures", "100"));

        night_portrait = false; //sharedPref.getBoolean("key_night_portrait", false);
        exposure_compensation = 50;//Integer.valueOf(sharedPref.getString("key_exposure_compensation", "50"));

        if (front_camera){
            mAddPersonView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
        } else {
            mAddPersonView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
        }
        mAddPersonView.setVisibility(SurfaceView.VISIBLE);
        mAddPersonView.setCvCameraViewListener(this);

        int maxCameraViewWidth = 640;//Integer.parseInt(sharedPref.getString("key_maximum_camera_view_width", "640"));
        int maxCameraViewHeight = 480;//Integer.parseInt(sharedPref.getString("key_maximum_camera_view_height", "480"));
        mAddPersonView.setMaxFrameSize(maxCameraViewWidth, maxCameraViewHeight);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

        if (night_portrait) {
            mAddPersonView.setNightPortrait();
        }

        if (exposure_compensation != 50 && 0 <= exposure_compensation && exposure_compensation <= 100)
            mAddPersonView.setExposure(exposure_compensation);
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat imgRgba = inputFrame.rgba();
        Mat imgCopy = new Mat();
        imgRgba.copyTo(imgCopy);
        // Selfie / Mirror mode
        if(front_camera){
            Log.d(TAG,"Selfie / Mirror mode");
            Core.flip(imgRgba,imgRgba,1);
        }

        long time = new Date().getTime();
        if((method == TIME) && (lastTime + timerDiff < time)){
            lastTime = time;

            Log.d(TAG,"Check that only 1 face is found. Skip if any or more than 1 are found.");
            List<Mat> images = ppF.getCroppedImage(imgCopy);
            if (images != null && images.size() == 1){
                Mat img = images.get(0);
                if(img != null){
                    Rect[] faces = ppF.getFacesForRecognition();
                    Log.d(TAG,"Only proceed if 1 face has been detected, ignore if 0 or more than 1 face have been detected");
                    if((faces != null) && (faces.length == 1)){
                        faces = MatOperation.rotateFaces(imgRgba, faces, ppF.getAngleForRecognition());
                        if(((method == MANUALLY) && capturePressed) || (method == TIME)){
                            MatName m = new MatName(name + "_" + total, img);
                            if (folder.equals("Test")) {
                                String wholeFolderPath = fh.TEST_PATH + name + "/" + subfolder;
                                new File(wholeFolderPath).mkdirs();
                                fh.saveMatToImage(m, wholeFolderPath + "/");
                            } else {
                                String wholeFolderPath = fh.TRAINING_PATH + name;
                                new File(wholeFolderPath).mkdirs();
                                fh.saveMatToImage(m, wholeFolderPath + "/");
                            }

                            for(int i = 0; i<faces.length; i++){
                                MatOperation.drawRectangleAndLabelOnPreview(imgRgba, faces[i], String.valueOf(total), front_camera);
                            }

                            total++;

                            Log.d(TAG,"Stop after numberOfPictures (settings option)");
                            if(total >= numberOfPictures){
                                //selfieTraining();
                                /*Intent intent = new Intent(getApplicationContext(), RegistrationActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);*/
                                PreProcessorFactory ppF = new PreProcessorFactory(getApplicationContext());
                                PreferencesHelper preferencesHelper = new PreferencesHelper(getApplicationContext());

                                String algorithm = "Eigenfaces with NN";//preferencesHelper.getClassificationMethod();

                                FileHelper fileHelper = new FileHelper();
                                fileHelper.createDataFolderIfNotExsiting();
                                final File[] persons = fileHelper.getTrainingList();
                                if (persons.length > 0) {
                                    Recognition rec = RecognitionFactory.getRecognitionAlgorithm(getApplicationContext(), Recognition.TRAINING, algorithm);
                                    for (File person : persons) {
                                        if (person.isDirectory()){
                                            File[] files = person.listFiles();
                                            int counter = 1;
                                            for (File file : files) {
                                                if (FileHelper.isFileAnImage(file)){
                                                    Mat imgRgb = Imgcodecs.imread(file.getAbsolutePath());
                                                    Imgproc.cvtColor(imgRgb, imgRgb, Imgproc.COLOR_BGRA2RGBA);
                                                    Mat processedImage = new Mat();
                                                    imgRgb.copyTo(processedImage);
                                                    List<Mat> images2 = ppF.getProcessedImage(processedImage, PreProcessorFactory.PreprocessingMode.RECOGNITION);
                                                    if (images2 == null || images2.size() > 1) {
                                                        // More than 1 face detected --> cannot use this file for training
                                                        continue;
                                                    } else {
                                                        processedImage = images2.get(0);
                                                    }
                                                    if (processedImage.empty()) {
                                                        continue;
                                                    }
                                                    // The last token is the name --> Folder name = Person name
                                                    String[] tokens = file.getParent().split("/");
                                                    final String name = tokens[tokens.length - 1];

                                                    MatName m2 = new MatName("processedImage", processedImage);
                                                    fileHelper.saveMatToImage(m2, FileHelper.DATA_PATH);
                                                    Log.d(TAG,"NOMBRE: " +name);
                                                    rec.addImage(processedImage, name, false);

//                                      fileHelper.saveCroppedImage(imgRgb, ppF, file, name, counter);

                                                    // Update screen to show the progress
                                                    final int counterPost = counter;
                                                    final int filesLength = files.length;
                                        /*progress.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                progress.append("Image " + counterPost + " of " + filesLength + " from " + name + " imported.\n");
                                            }
                                        });*/

                                                    //counter++;
                                                }
                                            }
                                        }
                                    }
                                    final Intent intent = new Intent(getApplicationContext(), RegistrationActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    //startActivity(intent);
                                    //final Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                    //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    if (rec.train()) {
                                        intent.putExtra("training", "Training successful");
                                    } else {
                                        intent.putExtra("training", "Training failed");
                                    }

                                    startActivity(intent);

                                }
                            }
                            capturePressed = false;
                        } else {
                            for(int i = 0; i<faces.length; i++){
                                MatOperation.drawRectangleOnPreview(imgRgba, faces[i], front_camera);
                            }
                        }
                    }
                }
            }
        }

        return imgRgba;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        ppF = new PreProcessorFactory(this);
        mAddPersonView.enableView();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mAddPersonView != null)
            mAddPersonView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mAddPersonView != null)
            mAddPersonView.disableView();
    }


    public void selfieTraining(){

        PreProcessorFactory ppF = new PreProcessorFactory(getApplicationContext());
        PreferencesHelper preferencesHelper = new PreferencesHelper(getApplicationContext());

        String algorithm = "Eigenfaces with NN";//preferencesHelper.getClassificationMethod();

        FileHelper fileHelper = new FileHelper();
        fileHelper.createDataFolderIfNotExsiting();
        final File[] persons = fileHelper.getTrainingList();
        if (persons.length > 0) {
            Recognition rec = RecognitionFactory.getRecognitionAlgorithm(getApplicationContext(), Recognition.TRAINING, algorithm);
            for (File person : persons) {
                if (person.isDirectory()){
                    File[] files = person.listFiles();
                    int counter = 1;
                    for (File file : files) {
                        if (FileHelper.isFileAnImage(file)){
                            Mat imgRgb = Imgcodecs.imread(file.getAbsolutePath());
                            Imgproc.cvtColor(imgRgb, imgRgb, Imgproc.COLOR_BGRA2RGBA);
                            Mat processedImage = new Mat();
                            imgRgb.copyTo(processedImage);
                            List<Mat> images = ppF.getProcessedImage(processedImage, PreProcessorFactory.PreprocessingMode.RECOGNITION);
                            if (images == null || images.size() > 1) {
                                // More than 1 face detected --> cannot use this file for training
                                continue;
                            } else {
                                processedImage = images.get(0);
                            }
                            if (processedImage.empty()) {
                                continue;
                            }
                            // The last token is the name --> Folder name = Person name
                            String[] tokens = file.getParent().split("/");
                            final String name = tokens[tokens.length - 1];

                            MatName m = new MatName("processedImage", processedImage);
                            fileHelper.saveMatToImage(m, FileHelper.DATA_PATH);

                            rec.addImage(processedImage, name, false);

//                                      fileHelper.saveCroppedImage(imgRgb, ppF, file, name, counter);

                            // Update screen to show the progress
                            final int counterPost = counter;
                            final int filesLength = files.length;
                                        /*progress.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                progress.append("Image " + counterPost + " of " + filesLength + " from " + name + " imported.\n");
                                            }
                                        });*/

                            //counter++;
                        }
                    }
                }
            }
            final Intent intent = new Intent(getApplicationContext(), RegistrationActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            //startActivity(intent);
            //final Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            if (rec.train()) {
                intent.putExtra("training", "Training successful");
            } else {
                intent.putExtra("training", "Training failed");
            }

            startActivity(intent);

        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        thread.interrupt();
    }
}