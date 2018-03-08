package com.hbwy.fan.speex;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.nfc.Tag;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.trunkbow.speextest.Speex;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    String TAG = "MainActivity";

    AudioManager audioManager;
    AudioRecord audioRecord;
    AudioTrack audioTrack;
    static final int frequency = 8000;//44100;
    static final int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    int recBufSize, playBufSize;

    String FILE_DIR = "Speex";
    String MIC_RECORD = "mic_record.pcm";
    String SPEEX_ENCODE = "speex_encode.spx";
    String SPEEX_DECODE = "speex_decode.pcm";
    File micRecordFile;
    File speexEncodeFile;//mic音频编码后
    File speexDecodeFile;//mic音频编码后再解码
    Speex speex;
    int speexLen = 1024;//一帧音频编码后的byte长度（20ms）与编码质量有关:Speex.open(int compression);
    boolean isRecord;
    int oneFrameSize = 1024;//窄带160,宽带320,超宽带640


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        recBufSize = AudioRecord.getMinBufferSize(frequency,
                channelConfiguration, audioEncoding);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency,
                channelConfiguration, audioEncoding, recBufSize * 10);

        playBufSize = AudioTrack.getMinBufferSize(frequency,
                AudioFormat.CHANNEL_OUT_MONO, audioEncoding);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, frequency,
                AudioFormat.CHANNEL_OUT_MONO, audioEncoding,
                playBufSize, AudioTrack.MODE_STREAM);


        findViewById(R.id.btn_startrecord).setOnClickListener(onClickListener);
        findViewById(R.id.btn_stoprecord).setOnClickListener(onClickListener);
        findViewById(R.id.btn_playrecord).setOnClickListener(onClickListener);
        findViewById(R.id.btn_speex_encode).setOnClickListener(onClickListener);
        findViewById(R.id.btn_speex_decode).setOnClickListener(onClickListener);
        findViewById(R.id.btn_play_decoded).setOnClickListener(onClickListener);

        createFile();
        speex = new Speex();
        speex.init();

        oneFrameSize = speex.getFrameSize();
        Log.e(TAG,"= "+oneFrameSize);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        speex.close();
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_startrecord:
                    isRecord = true;
                    new Thread(new RecordThread()).start();
                    break;
                case R.id.btn_stoprecord:
                    isRecord = false;
                    break;
                case R.id.btn_playrecord:
                    new Thread(new Player(micRecordFile)).start();
                    break;
                case R.id.btn_speex_encode:
                    micEncode();
                    break;
                case R.id.btn_speex_decode:
                    new Thread(new Decode()).start();
                    break;
                case R.id.btn_play_decoded:
                    new Thread(new Player(speexDecodeFile)).start();
                    break;
            }
        }
    };

    private void micEncode() {
        byte[] pcmDataByte = new byte[(int) micRecordFile.length()];
        byte[] speexData = new byte[oneFrameSize];
        DataInputStream dis = null;
        FileOutputStream fos = null;
        try {
            FileInputStream fis = new FileInputStream(micRecordFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            dis = new DataInputStream(bis);
            fos = new FileOutputStream(speexEncodeFile);
//                        int position = 0;
//                        while (dis.available() > 0) {
//                            pcmData[position] = dis.readShort();
//                            position++;
//                        }
            dis.read(pcmDataByte);
            short[] pcmData = toShortArray(pcmDataByte);
            for (int i = 0; i < pcmData.length; i += oneFrameSize) {
                short[] oneFramePCM = new short[oneFrameSize];
                System.arraycopy(pcmData, i, oneFramePCM, 0, oneFrameSize);
                speexLen = speex.encode(oneFramePCM, 0, speexData, oneFramePCM.length);
                Log.e(TAG, "speexLen = " + speexLen);
                byte[] oneFrameSpeex = new byte[speexLen];
                System.arraycopy(speexData, 0, oneFrameSpeex, 0, speexLen);
                fos.write(oneFrameSpeex);
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        } finally {
            try {
                if (dis != null) dis.close();
                if (fos != null) fos.close();
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    public static byte[] toByteArray(short[] src) {

        int count = src.length;
        byte[] dest = new byte[count << 1];
        for (int i = 0; i < count; i++) {//fffc
            dest[i * 2] = (byte) (src[i] >> 0);
            dest[i * 2 + 1] = (byte) (src[i] >> 8);//fcff
        }

        return dest;
    }

    public static short[] toShortArray(byte[] src) {

        int count = src.length >> 1;
        short[] dest = new short[count];
        for (int i = 0; i < count; i++) {//
            dest[i] = (short) (src[i * 2] & 0xff | (src[2 * i + 1] << 8));
        }
        return dest;
    }

    private void writeToFile(String fileName, byte[] data) {
        try {
            File file = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + FILE_DIR + File.separator + fileName);
            if (!file.exists())
                file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            DataOutputStream dos = new DataOutputStream(bos);
            dos.write(data);
            dos.flush();
            dos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class Player implements Runnable {
        File playFile;

        public Player(File f) {
            playFile = f;
        }

        @Override
        public void run() {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(playFile);
                audioTrack.play();
                int offset = 0;
                int fileSize = (int) playFile.length();
                byte[] buffer = new byte[fileSize];
                try {
                    fis.skip(offset);
                    fis.read(buffer);
                } catch (IOException e) {
                    e.printStackTrace();

                }
                audioTrack.write(buffer, 0, fileSize);
                Log.e(TAG, "play over");
                audioTrack.stop();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fis != null) fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class Decode implements Runnable {
        @Override
        public void run() {
            FileInputStream fis = null;
            FileOutputStream fos = null;
            try {
                fis = new FileInputStream(speexEncodeFile);
                fos = new FileOutputStream(speexDecodeFile);
                byte[] sdata = new byte[speexLen];
                int off = 0;
                while (true) {
                    short[] linn = new short[160];
                    if (fis.read(sdata, off, sdata.length) < 0) {
                        break;
                    }
                    speex.decode(sdata, linn, sdata.length);
                    fos.write(toByteArray(linn));
                }
                fos.flush();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fis != null) fis.close();
                    if (fos != null) fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    class RecordThread implements Runnable {

        @Override
        public void run() {
            DataOutputStream dos = null;
            try {
                dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(micRecordFile)));
                audioRecord.startRecording();
                Log.e(TAG, "start record");
                short[] buffer = new short[oneFrameSize];
                while (isRecord) {
                    int readResult = audioRecord.read(buffer, 0, oneFrameSize);
                    short[] data = new short[readResult];
                    System.arraycopy(buffer, 0, data, 0, readResult);
                    Log.e(TAG, "readResult = " + readResult);

                    dos.write(toByteArray(data));

                }
                dos.flush();
                Log.e(TAG, "end record");
                audioRecord.stop();
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            } finally {
                try {
                    if (dos != null)
                        dos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    void createFile() {
        micRecordFile = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + MIC_RECORD);
        speexEncodeFile = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + SPEEX_ENCODE);
        speexDecodeFile = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + SPEEX_DECODE);
        try {
            if (!micRecordFile.exists()) {
                micRecordFile.createNewFile();
            }
            if (!speexEncodeFile.exists()) {
                speexEncodeFile.createNewFile();
            }
            if (!speexDecodeFile.exists()) {
                speexDecodeFile.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
