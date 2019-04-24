package com.lll.myapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.CircleOptions;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Stroke;
import com.baidu.mapapi.model.LatLng;
import com.baidu.trace.LBSTraceClient;
import com.baidu.trace.Trace;
import com.baidu.trace.model.OnTraceListener;
import com.baidu.trace.model.PushMessage;
import com.google.android.gms.location.DetectedActivity;
import com.lll.myapp.services.ActivityDetectionService;
import com.lll.myapp.utils.Constant;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.lll.myapp.utils.Constant.coordinate1;
import static com.lll.myapp.utils.Constant.inFenceIds;
import static com.lll.myapp.utils.Constant.isFirstServer;
import static com.lll.myapp.utils.Constant.mClient;
import static com.lll.myapp.utils.Constant.outFenceIds;

/**发现一个bug，系统设置的字体会影响程序运行*/
public class MainActivity extends AppCompatActivity {
    protected static final String TAG = MainActivity.class.getSimpleName();

    private TextView mTextview;
    private Button mBtnLocationView;
    private Button mBtnTraceView;
    private Button mBtnStatusView;
    private Button mBtnFencesetView;

    //状态
    private long startTime;
    private long stopTime;
    String prelable;
    int newType;
    int preType;
    SimpleDateFormat formatter;
    private SDKReceiver mReceiver;

    // 轨迹初始化
    OnTraceListener traceListener = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "main oncreate");
        // 初始化SDK
        SDKInitializer.initialize(getApplicationContext());
        // apikey的授权需要一定的时间，在授权成功之前地图相关操作会出现异常；apikey授权成功后会发送广播通知，我们这里注册 SDK 广播监听者
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_OK);
        iFilter.addAction(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR);
        iFilter.addAction(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR);
        mReceiver = new SDKReceiver();
        registerReceiver(mReceiver, iFilter);

        setContentView(R.layout.activity_main);
        // 读设备号
        RegisterStateRead();
        if(Constant.entityName =="myTrace")
        {
            Constant.entityName = getImei(this);
        }


        AlertDialog malertDialog = new AlertDialog.Builder(this)
                .setTitle("Welcome") //这是设置标题
                .setMessage("please init！")//这是设置内容
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {//添加"Yes"按钮
                    @Override
// 下面的监听事件可以设置很多个
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(Constant.entityName == "myTrace")
                        {
                            Toast.makeText(MainActivity.this, "no find imie please exit", Toast.LENGTH_LONG).show();
                            return;
                        }
                        // 设置定位和打包周期
                        mClient.setInterval(Constant.gatherInterval,Constant.packInterval);
                        //初始化鹰眼服务端，serviceId：服务端唯一id，entityName：监控的对象，可以是手机设备唯一标识
                        Constant.mTrace = new Trace(Constant.serviceId, Constant.entityName,false);
                        initListener();
                        mClient.startTrace(Constant.mTrace, traceListener);//开始服务
                        mClient.startGather(traceListener);//开始采集
                        new Thread() {
                            @Override public void run() {
                                super.run();
                                try {
                                    Thread.sleep(10000);//休眠n/1000秒
                                    mClient.stopTrace(Constant.mTrace,traceListener);//停止服务
                                    RegisterStateWrite();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } }.start();
                        Toast.makeText(MainActivity.this, "please wait 10 seconds ", Toast.LENGTH_LONG).show();

                    }
                })
                .create();
        if(isFirstServer){
            malertDialog.show();//显示出来
        }


        mBtnLocationView = findViewById(R.id.btn_location); //定位
        mBtnTraceView = findViewById(R.id.btn_trace);    //画轨迹
        mBtnFencesetView = findViewById(R.id.btn_set);// 围栏设置
        mBtnStatusView = findViewById(R.id.btn_status);// 状态查看

        mTextview = findViewById(R.id.text_sensor);
/************初始化轨迹服务操作**********/
        initTrace();
        Log.d(TAG,"轨迹服务初始化成功");

        // 把xml文件读取出来
        readRegisterState();

        // 设置 内外圈
        setCircle();

        // 开启广播接收监听
        LocalBroadcastManager.getInstance(this).registerReceiver(mActivityBroadcastReceiver,
                new IntentFilter(Constant.BROADCAST_DETECTED_ACTIVITY));
        //开始谷歌状态服务
        startService(new Intent(this, ActivityDetectionService.class));

        startTime = System.currentTimeMillis();
        formatter = new SimpleDateFormat("yyyy-MM.dd-HH:mm:ss");

        mBtnLocationView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LocationActivity.class);
                startActivity(intent);
            }
        });
        mBtnTraceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TraceActivity.class);
                startActivity(intent);
            }
        });
        mBtnStatusView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,StatusActivity.class);
                startActivity(intent);
            }
        });
        mBtnFencesetView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent =new Intent(MainActivity.this,FenceSetActivity.class);
                startActivity(intent);
            }
        });

    }
    /**
     * 构造广播监听类，监听 SDK key 验证以及网络异常广播
     */
    public class SDKReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            String s = intent.getAction();

            if (s.equals(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR)) {
                Toast.makeText(MainActivity.this,"apikey验证失败，地图功能无法正常使用",Toast.LENGTH_SHORT).show();
            } else if (s.equals(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_OK)) {
                Toast.makeText(MainActivity.this,"apikey验证成功",Toast.LENGTH_SHORT).show();
            } else if (s.equals(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR)) {
                Toast.makeText(MainActivity.this,"网络错误",Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 状态 type :DetectedActivity.IN_VEHICLE
    // ON_BICYCLE,ON_FOOT,RUNNING,STILL,TILTING,WALKING,UNKNOWN

    BroadcastReceiver mActivityBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //收到指定广播信号
            if (intent.getAction().equals(Constant.BROADCAST_DETECTED_ACTIVITY)) {
                newType = intent.getIntExtra("type", -1);
                stopTime = System.currentTimeMillis();
                String lable;
                if(newType!=preType)
                {
                    try {
                        mWrite(prelable);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    startTime = stopTime;
                }
                switch (newType) {
                    case DetectedActivity.IN_VEHICLE:
                        lable = "in car";
                        break;
                    case DetectedActivity.STILL:
                        lable = "still";
                        break;
                    default:
                        lable = "other";
                        break;
                }
                prelable = lable;
                mTextview.setText("Status:" + lable);
                preType = newType;
            }
        }
    };

    protected void mWrite(String lable)throws IOException
    {
        File file = new File("/sdcard/" + "MyStatus.txt");
        // 写文件
        FileWriter out = new FileWriter(file,true);
        Date date = new Date(startTime);
        Date date1 = new Date(stopTime);
        out.write(formatter.format(date)+"--->"+formatter.format(date1)+"Status:"+lable);
        out.write("\r\n");
        out.close();
    }

    private void initTrace() {
        //初始化鹰眼服务端，serviceId：服务端唯一id，entityName：监控的对象，可以是手机设备唯一标识
        Constant.mTrace = new Trace(Constant.serviceId, Constant.entityName,false);
        mClient = new LBSTraceClient(getApplicationContext());
        mClient.startTrace(Constant.mTrace, traceListener);//开始服务
        // 初始化监听器
        initListener();
        Log.d(TAG,"成功初始化监听器");

    }
    /**
     * 获取设备IMEI码
     */
    public static String getImei(Context context) {
        String imei;
        try {
            imei = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
        } catch (Exception e) {
            imei = "myTrace";
        }
        return imei;
    }

    private void initListener()
    {
        /*******************初始化轨迹服务监听器************/
        traceListener = new OnTraceListener() {

            @Override
            public void onBindServiceCallback(int errorNo, String message) {
            }

            // 开启服务回调
            @Override
            public void onStartTraceCallback(int status, String message) {
                if(status==0)
                {
                    //Toast.makeText(MainActivity.this, "开始服务 "+ status+message, Toast.LENGTH_LONG).show();
                }else {
                    //initTrace();
                }

            }

            // 停止服务回调
            @Override
            public void onStopTraceCallback(int status, String message) {
                if(status==0)
                {
                   // Toast.makeText(MainActivity.this, "停止服务 "+ status+message, Toast.LENGTH_LONG).show();

                }else {

                }
            }

            // 开启采集回调
            @Override
            public void onStartGatherCallback(int status, String message) {
            }

            // 停止采集回调
            @Override
            public void onStopGatherCallback(int status, String message) {
            }

            // 推送回调
            @Override
            public void onPushCallback(byte messageNo, PushMessage message){

            }
            //存储服务
            @Override
            public void onInitBOSCallback(int i, String s) {
                Log.d(TAG, " onPushCallback: " + i);
            }
        };
    }

/******读围栏数据*****/
    private void readRegisterState()
    {
        long infenceId;
        long outfenceId;
        double latitude;
        double longtitude;
        Context context = this;
        SharedPreferences sp = context.getSharedPreferences("fenceData", Context.MODE_PRIVATE);
        infenceId = sp.getLong("inFenceId", 1);//第二个参数为默认值
        outfenceId =sp.getLong("outFenceId",2);
        if(outfenceId<3)
        {
            Toast.makeText(MainActivity.this, "未发现围栏", Toast.LENGTH_SHORT).show();
        }
        //Toast.makeText(MainActivity.this, "外圈："+outfenceId, Toast.LENGTH_SHORT).show();
        //Toast.makeText(MainActivity.this, "内圈："+infenceId, Toast.LENGTH_SHORT).show();
        inFenceIds.clear();
        outFenceIds.clear();
        inFenceIds.add(infenceId);
        outFenceIds.add(outfenceId);
        latitude =  Double.valueOf(sp.getString("centerLatitude","0")) ;
        longtitude = Double.valueOf(sp.getString("centerLongtitude","0")) ;
        coordinate1 = new LatLng(latitude,longtitude);
        //Toast.makeText(MainActivity.this, "坐标："+coordinate1, Toast.LENGTH_SHORT).show();
        Constant.infence_radius = Double.valueOf(sp.getString("inFenceRadius","50"));
        Constant.outfence_radius = Double.valueOf(sp.getString("outFenceRadius","500"));
        if(latitude>0.0001)
        {
            Toast.makeText(MainActivity.this,"读取数据成功！",Toast.LENGTH_SHORT).show();
        }else {
            Toast.makeText(MainActivity.this,"参数错误：请设置车库点！",Toast.LENGTH_SHORT).show();
        }

    }
    private void setCircle(){
        if(coordinate1==null)
        {
            coordinate1 = new LatLng(0,0);
        }
        Constant.outCircle = new CircleOptions().center(coordinate1)
                .radius((int)Constant.outfence_radius)
                .fillColor(0xccffff)
                .stroke(new Stroke(5, 0xAA00ff00)); //边框宽和边框颜色
        Constant.inCircle = new CircleOptions().center(coordinate1)
                .radius((int)Constant.infence_radius)
                .fillColor(0xccffff)
                .stroke(new Stroke(5, 0xAA9933ff)); //边框宽和边框颜色
    }

    public void RegisterStateWrite(){
        SharedPreferences sf;
        Context context = this;
        sf = context.getSharedPreferences("initData", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sf.edit();
        editor.putBoolean("isFirstStart",false);
        editor.putString("IMie",Constant.entityName);
        editor.commit();
    }
    private void RegisterStateRead()
    {
        Context context = this;
        SharedPreferences sp = context.getSharedPreferences("initData", Context.MODE_PRIVATE);
        isFirstServer = sp.getBoolean("isFirstStart", true);//第二个参数为默认值
        Constant.entityName = sp.getString("IMie","myTrace");
    }



    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
      //  mMapView.onResume();
        // 适配android M，检查权限 M=23
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isNeedRequestPermissions(permissions)) {
            // 如果没有权限的时候，请求增加权限 参数：string[],请求权限码(必须大于等于零)
            requestPermissions(permissions.toArray(new String[permissions.size()]), 0);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        //mMapView.onPause();
    }

    @Override
    protected void onDestroy() {

        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理

        super.onDestroy();

         //销毁广播
        if (mActivityBroadcastReceiver != null) {
            stopService(new Intent(this, ActivityDetectionService.class));
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mActivityBroadcastReceiver);
        }
        // 关闭鹰眼服务
        mClient.stopTrace(Constant.mTrace, traceListener);

    }

    //在这个里面增加你的需要用到的动态权限
    private boolean isNeedRequestPermissions(List<String> permissions) {
        // 定位精确位置
        addPermission(permissions, Manifest.permission.ACCESS_FINE_LOCATION);
        // 存储权限
        addPermission(permissions, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        // 读取手机状态
        addPermission(permissions, Manifest.permission.READ_PHONE_STATE);
        // sd卡权限
        // addPermission(permissions,Manifest.permission.MOUNT_FORMAT_FILESYSTEMS);
        return permissions.size() > 0;
    }

    // PackageManager.PERMISSION_GRANTED 有这项权限的返回值
    // PERMISSION_DENIED  没有这项权限的返回值
    private void addPermission(List<String> permissionsList, String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
        }

    }

}

