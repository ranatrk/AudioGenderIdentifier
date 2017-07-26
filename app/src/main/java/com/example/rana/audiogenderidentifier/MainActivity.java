package com.example.rana.audiogenderidentifier;

/**
 * Created by Rana on 08.06.17.
 */

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;

    //dataArrayList --> recorded data
    //finalDataArrayList --> divided data with overlaping bytes
    private ArrayList<short[]> dataArrayList = new ArrayList();
    private ArrayList<int[]> finalDataArrayList = new ArrayList();

    // Requesting permission to RECORD_AUDIO
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};

    //Tensorflow
    static {
        System.loadLibrary("tensorflow_inference");
    }
    private static final String MODEL_FILE = "file:///android_asset/optimized_tfdroid.pb";
    private static final String INPUT_NODE = "I";
    private static final String OUTPUT_NODE = "O";
    private TensorFlowInferenceInterface inferenceInterface;


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bufferSize = AudioRecord.getMinBufferSize(16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        setButtonHandlers();
        enableButtons(false);

        inferenceInterface = new TensorFlowInferenceInterface();
        inferenceInterface.initializeTensorFlow(getAssets(), MODEL_FILE);
//        tensorFlowSample();

    }


    //Add listeners to start,stop recording buttons
    private void setButtonHandlers() {
        ((Button)findViewById(R.id.btnStart)).setOnClickListener(btnClick);
        ((Button)findViewById(R.id.btnStop)).setOnClickListener(btnClick);
    }


    //Enable/Disable single button based on boolean parameter input
    private void enableButton(int id,boolean isEnable){
        ((Button)findViewById(id)).setEnabled(isEnable);
    }


    //toggle enabling start,stop buttons
    private void enableButtons(boolean isRecording) {
        enableButton(R.id.btnStart,!isRecording);
        enableButton(R.id.btnStop,isRecording);
    }

    //start recording audio in a new Thread
    //call methods required to obtain the bytes and save them in required array
    private void startRecording(){
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING, bufferSize);

        int i = recorder.getState();
        if(i==1)
            recorder.startRecording();

        isRecording = true;

        recordingThread = new Thread(new Runnable() {

            @Override
            public void run() {
                writeAudioDataToArrayList();
                reorganiseDataArray(flattenArray());
            }
        },"AudioRecorder Thread");

        recordingThread.start();
    }

    //Save data(type:short) from mic recorder to array buffers and add them to temporary dataArrayList
    private void writeAudioDataToArrayList(){

        short data[] = new short[bufferSize];
        int read = 0;

            while(isRecording){

                read = recorder.read(data, 0, bufferSize);

                if(AudioRecord.ERROR_INVALID_OPERATION != read){
                        dataArrayList.add(data);

                }
            }
    }

    //flatten  and return dataArrayList(arrayList of arrays) to one arrayList of int values of the data
    private ArrayList<Integer> flattenArray(){
        ArrayList<Integer> flattenedDataArrayList = new ArrayList();
        short [] tempArray;
        for (int i = 0; i < dataArrayList.size() ; i++){
            tempArray = dataArrayList.remove(i);
            for (int j = 0; j<tempArray.length;j++){
                flattenedDataArrayList.add((int)tempArray[j]);
            }
        }
        return flattenedDataArrayList;
    }


    //Regroup int values into arrays of certain size,
    // taking into consideration Overlap of audio samples(1 sec=80 samples)
    private void reorganiseDataArray(ArrayList<Integer> flattenedDataArrayList){
        //finalDataArrayList
        int i = 0 ;
        int singleArrayCounter = 1;
        int [] tempArray;
        int sampleArraySize = RECORDER_SAMPLERATE * 5;
        while(i<flattenedDataArrayList.size()){
            //if the counter is less than the number of samples to be saved in each single array
            switch (singleArrayCounter){
                //First position in new Array
                case 1:tempArray = new int[sampleArraySize];
                    tempArray[singleArrayCounter - 1] = flattenedDataArrayList.get(i).intValue();
                    finalDataArrayList.add(tempArray);
                    singleArrayCounter++;
                    break;
                //Last position in single new Array
                case RECORDER_SAMPLERATE:tempArray = finalDataArrayList.get(finalDataArrayList.size()-1);
                    tempArray[singleArrayCounter - 1] = flattenedDataArrayList.get(i).intValue();
                    singleArrayCounter = 1;
                    //TODO should the overlap be 80 samples as sample rate is 8Khz
                    i -= 80-1;
                    break;
                //Intermediate positions in new Array
                default:tempArray = finalDataArrayList.get(finalDataArrayList.size()-1);
                    tempArray[singleArrayCounter - 1] = flattenedDataArrayList.get(i).intValue();
                    singleArrayCounter++;
                    break;

            }
            i++;
        }

    }


    //Stop recording data in Thread and close Thread
    private void stopRecording(){
        if(null != recorder){
            isRecording = false;

            int i = recorder.getState();
            if(i==1)
                recorder.stop();
            recorder.release();

            recorder = null;
            recordingThread = null;
        }

    }



    private View.OnClickListener btnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch(v.getId()){
                case R.id.btnStart:{
                    Log.v("Status","Start Recording");

                    enableButtons(true);
                    startRecording();

                    break;
                }
                case R.id.btnStop:{
                    Log.v("Status","Stop Recording");

                    enableButtons(false);
                    stopRecording();

                    break;
                }
            }
        }
    };


    //Use TensorFlow model on the finalDataArrayList to return ArrayList of results
    //reference : https://omid.al/posts/2017-02-20-Tutorial-Build-Your-First-Tensorflow-Android-App.html
    private ArrayList<float[]> useTensorFlow(int resultArrayLength){
        ArrayList<float[]> tensorflowResultArray = new ArrayList<float[]>();

        for (int i=0; i<finalDataArrayList.size() ; i++){
            int arraySize = finalDataArrayList.get(i).length;
            int [] audioSampleArray = {1,arraySize};

            inferenceInterface.fillNodeInt(INPUT_NODE,audioSampleArray,finalDataArrayList.get(i));
            inferenceInterface.runInference(new String[] {OUTPUT_NODE});

            float[] tempResult = new float[resultArrayLength];
            inferenceInterface.readNodeFloat(OUTPUT_NODE, tempResult);

            tensorflowResultArray.add(tempResult);
        }
        return tensorflowResultArray;

//        int arraySize = finalDataArrayList.get(0).length;
//        int [] array = {1,arraySize};
////        inferenceInterface.fillNodeFloat(INPUT_NODE,array,finalDataArrayList.get(0));
////        inferenceInterface.runInference(new String[] {OUTPUT_NODE});
//
//        inferenceInterface.fillNodeInt(INPUT_NODE,array,finalDataArrayList.get(0));
//        inferenceInterface.runInference(new String[] {OUTPUT_NODE});
//        //TODO declare and initialize the result array with what size
//        float[] result = {0, 0};
//        inferenceInterface.readNodeFloat(OUTPUT_NODE, result);
    }


    public void tensorFlowSample(){
        float num1 = 0.0f;
        float num2 = 3.33f;
        float num3 = 1.2f;

        String MODEL_FILE = "file:///android_asset/optimized_tfdroid2.pb";
        String INPUT_NODE = "I";
        String OUTPUT_NODE = "O";
        int[] INPUT_SIZE = {1,3};


        inferenceInterface = new TensorFlowInferenceInterface();
        inferenceInterface.initializeTensorFlow(getAssets(), MODEL_FILE);
        float[] inputFloats = {num1, num2, num3};

        inferenceInterface.fillNodeFloat(INPUT_NODE, INPUT_SIZE, inputFloats);
        inferenceInterface.runInference(new String[] {OUTPUT_NODE});


        float[] resu = new float[2];
        inferenceInterface.readNodeFloat(OUTPUT_NODE, resu);

        Log.v("output_mode" , Arrays.toString(resu));
    }


}
