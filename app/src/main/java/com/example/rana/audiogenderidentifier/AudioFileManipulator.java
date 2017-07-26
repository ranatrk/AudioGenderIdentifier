package com.example.rana.audiogenderidentifier;

import android.media.AudioFormat;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Rana on 26.07.17.
 */

public class AudioFileManipulator {
    private static final int RECORDER_BPP = 16; // bits per sample
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";

    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private ArrayList<int[]> dataArrayList = new ArrayList();

    public void deleteFolderContents(String folderName){
        //Delete All files in Audio Recorder folder of the emulator
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,folderName);
        String[] myFiles;

        myFiles = file.list();
        for (int i=0; i<myFiles.length; i++) {
            File myFile = new File(file, myFiles[i]);
            myFile.delete();
        }

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

    private String getTempFilename(int fileNumber){
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

    private void writeAudioDataToTempFiles(){
        //TODO data was short [] and wasnt tested with making it int []
        int data[];
        String filename;

//        Log.v("finalArray","final Data Array size :" + finalDataArrayList.size());
//        Log.v("finalArray", "first element size in final data Array :" + finalDataArrayList.get(0).length);
        for (int fileNumber = 0; fileNumber < dataArrayList.size() ; fileNumber++){

            ObjectOutputStream os = null;
            filename = getTempFilename(fileNumber);
//            Log.v("recorder",filename+":"+fileNumber);
            data = dataArrayList.get(fileNumber);
            if(fileNumber==0){
                Log.v("arrays","writeAudioDataToTempFiles:(finalDataArrayList[0]) : "+ Arrays.toString(dataArrayList.get(0)));
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

    private void copyTempFilesToWavFiles() {
        if(dataArrayList ==  null){
            return;
        }
        for (int fileNumber = 0; fileNumber < dataArrayList.size(); fileNumber++) {
            copyWaveFile(getTempFilename(fileNumber), getFilename());
            deleteTempFile(fileNumber);
        }

    }

    private void deleteTempFile() {
        File file = new File(getTempFilename());
        file.delete();
    }

    private void deleteTempFile(int fileNumber) {
        File file = new File(getTempFilename(fileNumber));

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
            //TODO data should be int
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
}
