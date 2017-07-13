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
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;


public class MainActivity2 extends AppCompatActivity {
    private static final int RECORDER_BPP = 16; // bits per sample
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;

    private ArrayList<short[]> dataArrayList = new ArrayList();
    private ArrayList<short[]> finalDataArrayList = new ArrayList();


    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};

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
        //TODO arrays created for wav file should have overriding data

        bufferSize = AudioRecord.getMinBufferSize(16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        //Delete All files in Audio Recorder folder of the emulator
//        String filepath = Environment.getExternalStorageDirectory().getPath();
//        File file = new File(filepath,AUDIO_RECORDER_FOLDER);
//        String[] myFiles;
//
//        myFiles = file.list();
//        for (int i=0; i<myFiles.length; i++) {
//            File myFile = new File(file, myFiles[i]);
//            myFile.delete();
//        }
//        Log.v("files",Arrays.toString(myFiles));

        setButtonHandlers();
        enableButtons(false);
    }

    private void setButtonHandlers() {
        ((Button)findViewById(R.id.btnStart)).setOnClickListener(btnClick);
        ((Button)findViewById(R.id.btnStop)).setOnClickListener(btnClick);
    }

    private void enableButton(int id,boolean isEnable){
        ((Button)findViewById(id)).setEnabled(isEnable);
    }

    private void enableButtons(boolean isRecording) {
        enableButton(R.id.btnStart,!isRecording);
        enableButton(R.id.btnStop,isRecording);
    }

    private String getFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
//            Log.v("Recorder","file doesnt exist (folder) in getFilename()");
            file.mkdirs();
//            Log.v("Recorder","file exists :" + file.exists());
        }

        return (file.getAbsolutePath() + "/" + System.currentTimeMillis() + AUDIO_RECORDER_FILE_EXT_WAV);
    }

    private String getTempFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
//            Log.v("Recorder","file doesnt exist (folder) in getTempFilename");
            file.mkdirs();
        }

        File tempFile = new File(filepath,AUDIO_RECORDER_TEMP_FILE);

        if(tempFile.exists()){
//            Log.v("Recorder","file exists (file) to be deleted");
            tempFile.delete();
        }

        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }

    private String getTempFilename2(int fileNumber){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        File tempFile = new File(filepath,fileNumber+AUDIO_RECORDER_TEMP_FILE);

        if(tempFile.exists()){
            tempFile.delete();
        }
        Log.v("recorder", file.getAbsolutePath() + "/" + fileNumber);

        return (file.getAbsolutePath() + "/" + fileNumber+ AUDIO_RECORDER_TEMP_FILE);
    }

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
                writeAudioDataToTempFiles();
                copyTempFilesToWavFiles();
            }
        },"AudioRecorder Thread");

        recordingThread.start();
    }

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

    private void writeAudioDataToTempFiles(){
        short data[];
        String filename;
        reorganiseDataArray(flattenArray());

//        Log.v("finalArray","final Data Array size :" + finalDataArrayList.size());
//        Log.v("finalArray", "first element size in final data Array :" + finalDataArrayList.get(0).length);
        for (int fileNumber = 0; fileNumber < finalDataArrayList.size() ; fileNumber++){

            ObjectOutputStream os = null;
            filename = getTempFilename2(fileNumber);
//            Log.v("recorder",filename+":"+fileNumber);
            data = finalDataArrayList.get(fileNumber);
            if(fileNumber==0){
                Log.v("arrays","writeAudioDataToTempFiles:(finalDataArrayList[0]) : "+ Arrays.toString(finalDataArrayList.get(0)));
            }
//            Log.v("AudioDebug","finalDataArrayList size : " + finalDataArrayList.size());

            try {
                os = new ObjectOutputStream(new FileOutputStream(filename));

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(null != os){
                try {
//                    Log.v("arrays" , "writeAudioDataToTempFiles: "+fileNumber+"--> "+ data.length);
                    os.writeObject(data);

//                    for (int i = 0; i < data.length; i++) {
//                        os.writeShort(data[i]);
//                    }
//                    os.write(data);

                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {

                    os.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }



    }

    private ArrayList<Short> flattenArray(){
        ArrayList<Short> flattenedDataArrayList = new ArrayList();
        short [] tempArray;
        for (int i = 0; i < dataArrayList.size() ; i++){
            tempArray = dataArrayList.remove(i);
            for (int j = 0; j<tempArray.length;j++){
                flattenedDataArrayList.add(tempArray[j]);
            }
        }
        return flattenedDataArrayList;
    }

    private void reorganiseDataArray(ArrayList<Short> flattenedDataArrayList){
        //finalDataArrayList
        int i = 0 ;
        int singleArrayCounter = 1;
        short [] tempArray;
        int sampleArraySize = RECORDER_SAMPLERATE * 5;
        while(i<flattenedDataArrayList.size()){
            //if the counter is less than the number of samples to be saved in each single array
            switch (singleArrayCounter){
                case 1:tempArray = new short[sampleArraySize];
                    tempArray[singleArrayCounter - 1] = flattenedDataArrayList.get(i).shortValue();
                    finalDataArrayList.add(tempArray);
                    singleArrayCounter++;
                    break;
                case RECORDER_SAMPLERATE:tempArray = finalDataArrayList.get(finalDataArrayList.size()-1);
                    tempArray[singleArrayCounter - 1] = flattenedDataArrayList.get(i).shortValue();
                    singleArrayCounter = 1;
                    break;
                default:tempArray = finalDataArrayList.get(finalDataArrayList.size()-1);
                    tempArray[singleArrayCounter - 1] = flattenedDataArrayList.get(i).shortValue();
                    singleArrayCounter++;
                    break;

            }
            i++;
        }

    }

    private void copyTempFilesToWavFiles() {

        for (int fileNumber = 0; fileNumber < finalDataArrayList.size(); fileNumber++) {
            copyWaveFile(getTempFilename2(fileNumber), getFilename());
            deleteTempFile2(fileNumber);
        }

    }


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

    private void deleteTempFile() {
        File file = new File(getTempFilename());
        file.delete();
    }

    private void deleteTempFile2(int fileNumber) {
        File file = new File(getTempFilename2(fileNumber));

        file.delete();
    }

    private void copyWaveFile(String inFilename,String outFilename){
//        FileInputStream in = null;
//        FileOutputStream out = null;
        ObjectInputStream in = null;
        ObjectOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 1; // was originally 2
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;

        short[] data;

        try {
            in = new ObjectInputStream(new FileInputStream(inFilename));
            out = new ObjectOutputStream(new FileOutputStream(outFilename));
//            totalAudioLen = in.getChannel().size();
            totalAudioLen = 16*40000;
            totalDataLen = totalAudioLen + 36;

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate);

            data = (short[]) in.readObject();
            out.writeObject(data);

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    private void WriteWaveFileHeader(
            ObjectOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = RECORDER_BPP; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
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
}
