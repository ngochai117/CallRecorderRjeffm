package com.android.ictteam.callrecorderrjeffm.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.android.ictteam.callrecorderrjeffm.MainActivity;
import com.android.ictteam.callrecorderrjeffm.R;
import com.android.ictteam.callrecorderrjeffm.callrecorder.AppPreferences;
import com.android.ictteam.callrecorderrjeffm.callrecorder.LocalBroadcastActions;
import com.android.ictteam.callrecorderrjeffm.database.CallLog;

import java.io.File;
import java.util.Calendar;

/**
 * Created by Pon Long Bong on 10/25/2017.
 */

public class RecordCallService extends Service {
    public final static String ACTION_START_RECORDING = "com.jlcsoftware.ACTION_CLEAN_UP";
    public final static String ACTION_STOP_RECORDING = "com.jlcsoftware.ACTION_STOP_RECORDING";
    public final static String EXTRA_PHONE_CALL = "com.jlcsoftware.EXTRA_PHONE_CALL";

    public RecordCallService(){
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ContentValues parcelableExtra = intent.getParcelableExtra(EXTRA_PHONE_CALL);
        startRecording(new CallLog(parcelableExtra));
        return START_NOT_STICKY ;
    }

    @Override
    public void onDestroy() {
        stopRecording();
        super.onDestroy();
    }

    private CallLog phoneCall;

    boolean isRecording = false;

    private void stopRecording() {

        if (isRecording) {
            try {
                phoneCall.setEndTime(Calendar.getInstance());
                mediaRecorder.stop();
                mediaRecorder.reset();
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;

                phoneCall.save(getBaseContext());
                displayNotification(phoneCall);

                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(LocalBroadcastActions.NEW_RECORDING_BROADCAST));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        phoneCall = null;
    }


    MediaRecorder mediaRecorder;


    private void startRecording(CallLog phoneCall) {
        if (!isRecording) {
            isRecording = true;
            this.phoneCall = phoneCall;
            File file = null;
            try {
                this.phoneCall.setSartTime(Calendar.getInstance());
                File dir = AppPreferences.getInstance(getApplicationContext()).getFilesDirectory();
                mediaRecorder = new MediaRecorder();
                file = File.createTempFile("record", ".3gp", dir);
                this.phoneCall.setPathToRecording(file.getAbsolutePath());
                if(Integer.parseInt(Build.VERSION.RELEASE.substring(0,1))>=6) {
                    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
                } else {
                    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
                }
                mediaRecorder.setAudioSamplingRate(MediaRecorder.MEDIA_RECORDER_INFO_UNKNOWN);
                mediaRecorder.setAudioEncodingBitRate(64000);
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                mediaRecorder.setOutputFile(phoneCall.getPathToRecording());
                mediaRecorder.prepare();
                mediaRecorder.start();
                /*Log.d("AudioSource.VOICE_CALL",String.valueOf(MediaRecorder.AudioSource.VOICE_CALL));
        Log.d("AudioSource.VOICE_CALL",String.valueOf(MediaRecorder.AudioSource.VOICE_CALL));
        Log.d("AudioSource.VOICE_CALL",String.valueOf(MediaRecorder.AudioSource.VOICE_CALL));
        Log.d("AudioSource.VOICE_CALL",String.valueOf(MediaRecorder.AudioSource.VOICE_CALL));
        Log.d("AudioSource.VOICE_CALL",String.valueOf(MediaRecorder.AudioSource.VOICE_CALL));
        Log.d("AudioSource.VOICE_CALL",String.valueOf(MediaRecorder.AudioSource.VOICE_CALL));*/
                Log.d("RELEASE",String.valueOf(Build.VERSION.RELEASE));
                Log.d("SDK_INT",String.valueOf(Build.VERSION.SDK_INT));
                Log.d("CAMCORDER",String.valueOf(MediaRecorder.AudioSource.CAMCORDER));
                Log.d("DEFAULT",String.valueOf(MediaRecorder.AudioSource.DEFAULT));
                Log.d("MIC",String.valueOf(MediaRecorder.AudioSource.MIC));
                Log.d("REMOTE_SUBMIX",String.valueOf(MediaRecorder.AudioSource.REMOTE_SUBMIX));
                Log.d("UNPROCESSED",String.valueOf(MediaRecorder.AudioSource.UNPROCESSED));
                Log.d("VOICE_CALL",String.valueOf(MediaRecorder.AudioSource.VOICE_CALL));
                Log.d("VOICE_COMMUNICATION",String.valueOf(MediaRecorder.AudioSource.VOICE_COMMUNICATION));
                Log.d("VOICE_DOWNLINK",String.valueOf(MediaRecorder.AudioSource.VOICE_DOWNLINK));
                Log.d("VOICE_RECOGNITION",String.valueOf(MediaRecorder.AudioSource.VOICE_RECOGNITION));
                Log.d("VOICE_UPLINK",String.valueOf(MediaRecorder.AudioSource.VOICE_UPLINK));

            } catch (Exception e) {
                e.printStackTrace();
                isRecording = false;
                if (file != null) file.delete();
                this.phoneCall = null;
                isRecording = false;
            }
        }





    }

    public void displayNotification(CallLog phoneCall) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_recording_conversation_white_24);
        builder.setContentTitle(getApplicationContext().getString(R.string.notification_title));
        builder.setContentText(getApplicationContext().getString(R.string.notification_text));
        builder.setContentInfo(getApplicationContext().getString(R.string.notification_more_text));
        builder.setAutoCancel(true);

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setAction(Long.toString(System.currentTimeMillis())); // fake action to force PendingIntent.FLAG_UPDATE_CURRENT
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        intent.putExtra("RecordingId", phoneCall.getId());

        builder.setContentIntent(PendingIntent.getActivity(this, 0xFeed, intent, PendingIntent.FLAG_UPDATE_CURRENT));
        notificationManager.notify(0xfeed, builder.build());
    }


    public static void sartRecording(Context context, CallLog phoneCall) {
        Intent intent = new Intent(context, RecordCallService.class);
        intent.setAction(ACTION_START_RECORDING);
        intent.putExtra(EXTRA_PHONE_CALL, phoneCall.getContent());
        context.startService(intent);
    }


    public static void stopRecording(Context context) {
        Intent intent = new Intent(context, RecordCallService.class);
        intent.setAction(ACTION_STOP_RECORDING);
        context.stopService(intent);
    }


}
