package com.lll.myapp.services;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.lll.myapp.utils.Constant;
import java.util.List;

// 检测设备
public class DetectedActivityIntentService extends IntentService {
    protected static final String TAG = DetectedActivityIntentService.class.getSimpleName();

    public DetectedActivityIntentService() {
        super(TAG);
        // Log.d(TAG,TAG + "DetectedActivityIntentService()");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,TAG + "onCreate()");

    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG,TAG + " onHandleIntent()");
        // 接收结果
        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

        // 会获取一个活动分析结果列表，并且用0-100表示确信度
        List<DetectedActivity> detectedActivities = result.getProbableActivities();

        for (DetectedActivity activity : detectedActivities) {
            Log.d(TAG, "Detected activity: " + activity.getType() + ", " + activity.getConfidence());

            if(activity.getConfidence()>90) {
                broadcastActivity(activity);
            }
        }

    }

    private void broadcastActivity(DetectedActivity activity) {
        Log.d(TAG,TAG+ "broadcastActivity()");

        Intent intent = new Intent(Constant.BROADCAST_DETECTED_ACTIVITY);
        intent.putExtra("type", activity.getType());
        intent.putExtra("confidence", activity.getConfidence()); // 确信度
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.d(TAG," send over!");
    }

    public void onDestroy()
    {
        super.onDestroy();
        Log.d(TAG," onDestory");
    }

}
