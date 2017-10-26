package com.android.ictteam.callrecorderrjeffm.listeners;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.android.ictteam.callrecorderrjeffm.database.CallLog;
import com.android.ictteam.callrecorderrjeffm.database.Database;
import com.android.ictteam.callrecorderrjeffm.receivers.MyCallReceiver;
import com.android.ictteam.callrecorderrjeffm.services.RecordCallService;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Pon Long Bong on 10/25/2017.
 */

public class PhoneListener extends PhoneStateListener {
    private static PhoneListener instance = null;

    /**
     * Must be called once on app startup
     *
     * @param context - application context
     * @return
     */
    public static PhoneListener getInstance(Context context) {
        if (instance == null) {
            instance = new PhoneListener(context);
        }
        return instance;
    }

    public static boolean hasInstance() {
        return null != instance;
    }

    private final Context context;
    private CallLog phoneCall;

    private PhoneListener(Context context) {
        this.context = context;
    }

    AtomicBoolean isRecording = new AtomicBoolean();
    AtomicBoolean isWhitelisted = new AtomicBoolean();


    /**
     * Set the outgoing phone number
     * <p/>
     * Called by {@link MyCallReceiver}  since that is where the phone number is available in a outgoing call
     *
     * @param phoneNumber
     */
    public void setOutgoing(String phoneNumber) {
        if (null == phoneCall)
            phoneCall = new CallLog();
        phoneCall.setPhoneNumber(phoneNumber);
        phoneCall.setOutgoing();
        // called here so as not to miss recording part of the conversation in TelephonyManager.CALL_STATE_OFFHOOK
        isWhitelisted.set(Database.isWhitelisted(context, phoneCall.getPhoneNumber()));
    }

    @Override
    public void onCallStateChanged(int state, String incomingNumber) {
        super.onCallStateChanged(state, incomingNumber);

        switch (state) {
            case TelephonyManager.CALL_STATE_IDLE: // Idle... no call
                if (isRecording.get()) {
                    RecordCallService.stopRecording(context);
                    phoneCall = null;
                    isRecording.set(false);
                }
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK: // Call answered
                if (isWhitelisted.get()) {
                    isWhitelisted.set(false);
                    return;
                }
                if (!isRecording.get()) {
                    isRecording.set(true);
                    // start: Probably not ever usefull
                    if (null == phoneCall)
                        phoneCall = new CallLog();
                    if (!incomingNumber.isEmpty()) {
                        phoneCall.setPhoneNumber(incomingNumber);
                    }
                    // end: Probably not ever usefull
                    RecordCallService.sartRecording(context, phoneCall);
                }
                break;
            case TelephonyManager.CALL_STATE_RINGING: // Phone ringing
                // DO NOT try RECORDING here! Leads to VERY poor quality recordings
                // I think something is not fully settled with the Incoming phone call when we get CALL_STATE_RINGING
                // a "SystemClock.sleep(1000);" in the code will allow the incoming call to stabilize and produce a good recording...(as proof of above)
                if (null == phoneCall)
                    phoneCall = new CallLog();
                if (!incomingNumber.isEmpty()) {
                    phoneCall.setPhoneNumber(incomingNumber);
                    // called here so as not to miss recording part of the conversation in TelephonyManager.CALL_STATE_OFFHOOK
                    isWhitelisted.set(Database.isWhitelisted(context, phoneCall.getPhoneNumber()));
                }
                break;
        }

    }
}
