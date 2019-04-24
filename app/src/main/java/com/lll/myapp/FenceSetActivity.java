package com.lll.myapp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.CircleOptions;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Stroke;
import com.baidu.mapapi.model.LatLng;
import com.baidu.trace.api.fence.AlarmPoint;
import com.baidu.trace.api.fence.CreateFenceRequest;
import com.baidu.trace.api.fence.CreateFenceResponse;
import com.baidu.trace.api.fence.DeleteFenceRequest;
import com.baidu.trace.api.fence.DeleteFenceResponse;
import com.baidu.trace.api.fence.FenceAlarmPushInfo;
import com.baidu.trace.api.fence.FenceListRequest;
import com.baidu.trace.api.fence.FenceListResponse;
import com.baidu.trace.api.fence.HistoryAlarmResponse;
import com.baidu.trace.api.fence.MonitoredAction;
import com.baidu.trace.api.fence.MonitoredStatus;
import com.baidu.trace.api.fence.MonitoredStatusByLocationResponse;
import com.baidu.trace.api.fence.MonitoredStatusInfo;
import com.baidu.trace.api.fence.MonitoredStatusResponse;
import com.baidu.trace.api.fence.OnFenceListener;
import com.baidu.trace.api.fence.UpdateFenceResponse;
import com.baidu.trace.model.CoordType;
import com.baidu.trace.model.OnTraceListener;
import com.baidu.trace.model.PushMessage;
import com.baidu.trace.model.StatusCodes;
import com.lll.myapp.utils.Constant;

import java.util.List;

import static com.lll.myapp.utils.Constant.coordinate1;
import static com.lll.myapp.utils.Constant.inFenceIds;
import static com.lll.myapp.utils.Constant.mClient;
import static com.lll.myapp.utils.Constant.outFenceIds;

/*************
 *  问题能够创建围栏，fenceids 查询有问题， 导致不能查询状态
 *  4.16 创建内外栏怎么分
 *  4.17 围栏绘制的数据在退出界面时会清空,Constant中数据会清空,需要保存的是服务端的列表编号,车库点信息
 *  4.20修改查询历史轨迹方式，改为直接在服务端上查询一天内的轨迹,删除通过读取本地文件轨迹点数据查询方式，增加自定义围栏半径功能
 *
 * ***/
public class FenceSetActivity extends AppCompatActivity {
    private static final String TAG = FenceSetActivity.class.getSimpleName();
    static int x = 0;
    static int y = 0;
    private MapView mapview;
    private BaiduMap bdMap;
    private LocationClient locationClient;
    private BDAbstractLocationListener locationListener;
    private double longitude;// 精度
    private double latitude;// 维度
    private float radius;// 定位精度半径，单位是米
    private float direction;// 手机方向信息
    private Button btn_create;
    private Button btn_check;
    private Button btn_delete;
    private Button btn_setcenter;
    //目前定位的经纬度
    public LatLng coordinate;

    // marker
    public Marker marker1;
    private boolean isNoSetMark = true;
    // 初始化全局 bitmap 信息，不用时及时 recycle
    // 构建marker图标
    BitmapDescriptor bitmap = BitmapDescriptorFactory
            .fromResource(R.mipmap.icon_gcoding);
    // 记录是否第一次定位
    private boolean isFirstLoc = true;
    private boolean isFirstFence =true;
    LatLng newcenter;

//轨迹相关

    OnTraceListener traceListener = null;
    com.baidu.trace.model.LatLng fence_center ;
    OnFenceListener mFenceListener;
    java.lang.Long fenceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fence_set);

        mapview = (MapView) findViewById(R.id.bd_mapview);
        btn_create =findViewById(R.id.create_fence);
        btn_check =findViewById(R.id.check_fence);
        btn_delete =findViewById(R.id.delete_fence);
        btn_setcenter = findViewById(R.id.set_center);
        //地图属性更新
        MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(15.0f);
        bdMap = mapview.getMap();
        bdMap.setMapStatus(msu);
        //Marker拖拽监听
        bdMap.setOnMarkerDragListener(new BaiduMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker arg0) {

            }
            @Override
            public void onMarkerDragEnd(Marker arg0) {
                Toast.makeText(
                        FenceSetActivity.this,
                        "拖拽结束,车库位置坐标：" + arg0.getPosition().latitude + ", "
                                + arg0.getPosition().longitude,
                        Toast.LENGTH_LONG).show();
                //新位置的经纬度
                coordinate1 = new LatLng(arg0.getPosition().latitude, arg0.getPosition().longitude);
                //将拖动后的marker的新位置在地图上显示
                addMarkerOverlay(coordinate1);
                y++;//表示marker已经被拖动
                isNoSetMark = false;
            }
            @Override
            public void onMarkerDrag(Marker arg0) {

            }
        });
        init();

        initListener();
        //mClient.startTrace(Constant.mTrace, traceListener);//开始服务

        btn_create.setOnClickListener(new View.OnClickListener() {

            LayoutInflater inflater = getLayoutInflater();
            View view=inflater.inflate(R.layout.dialog_fence_create,null);
            @Override
            public void onClick(View v) {
                ViewGroup vg=(ViewGroup)view.getParent();
                AlertDialog.Builder builder = new AlertDialog.Builder(FenceSetActivity.this);
                builder.setTitle("Please input radius"); //这是设置标题
                        if(vg !=null)
                        {
                            vg.removeView(view);
                        }
                builder.setView(view);

                builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {//添加"Yes"按钮
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //在这你可以做点击后的一些处理
                                EditText moutEdit =view.findViewById(R.id.edtTxt_outFence);
                                EditText minEdit =view.findViewById(R.id.edtTxt_inFence);
                                Constant.outfence_radius = Double.parseDouble(moutEdit.getText().toString()) ;
                                Constant.infence_radius = Double.parseDouble(minEdit.getText().toString());

                                fence_center =new com.baidu.trace.model.LatLng(coordinate1.latitude,coordinate1.longitude);
                                // 创建服务圆外栏实例
                                CreateFenceRequest OutFenceRequest = CreateFenceRequest.buildServerCircleRequest(Constant.tag,Constant.serviceId,Constant.fenceName,
                                        Constant.entityName,fence_center
                                        ,Constant.outfence_radius,Constant.denoise,Constant.coordType);

                                // 创建服务圆形围栏
                                mClient.createFence(OutFenceRequest, mFenceListener);

                                //  创建内栏
                                CreateFenceRequest InFenceRequest = CreateFenceRequest.buildServerCircleRequest(Constant.tag,Constant.serviceId,Constant.fenceName,
                                        Constant.entityName,fence_center
                                        ,Constant.infence_radius,Constant.indenoise,Constant.coordType);
                                mClient.createFence(InFenceRequest, mFenceListener);
                                // 保存车库坐标点到xml
                                latRegisterStateWrite();

                            }
                        })
                        .create();
                builder.show();//显示出来
            }
        });
        btn_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deletefence();
                Log.d(TAG,"删除围栏。。。");

            }
        });
        btn_check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkfence();
                Log.d(TAG,"查询围栏。。。");

            }
        });
        btn_setcenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCenter();
                readRadiusRegisterState();
                darwFence();
            }
        });
    }

    private void initListener()
    {
        /*******************初始化轨迹服务监听器************/
        traceListener = new OnTraceListener() {

            @Override
            public void onBindServiceCallback(int errorNo, String message) {
                Log.d(TAG, " onBindServiceCallback: " + errorNo);

            }

            // 开启服务回调
            @Override
            public void onStartTraceCallback(int status, String message) {
                Log.d(TAG, " onStartTraceCallback: " + status);

                if (status == 0) {
                    Log.d(TAG, " onStartTraceCallback: success" + status+ message);
                    Toast.makeText(FenceSetActivity.this, "开始服务 "+ status+message, Toast.LENGTH_LONG).show();
                    mClient.startGather(traceListener);//开启采集

                } else {
                    Log.d(TAG, " onStartTraceCallback: fail" + status + message);
                    Toast.makeText(FenceSetActivity.this, "开启失败 "+ status+message, Toast.LENGTH_LONG).show();
                    mClient.startTrace(Constant.mTrace, traceListener);//开始服务
                }
            }

            // 停止服务回调
            @Override
            public void onStopTraceCallback(int status, String message) {
                Log.d(TAG, " onStopTraceCallback: " + status);
            }

            // 开启采集回调
            @Override
            public void onStartGatherCallback(int status, String message) {
                Log.d(TAG, " onStartGatherCallback: " + status);

            }

            // 停止采集回调
            @Override
            public void onStopGatherCallback(int status, String message) {
                Log.d(TAG, " onStopGatherCallback: " + status);
            }

            // 推送回调
            @Override
            public void onPushCallback(byte messageNo, PushMessage message){
                if (messageNo < 0x03 || messageNo > 0x04) {
                    return;
                }
                /**
                 * 获取报警推送消息
                 */
                FenceAlarmPushInfo alarmPushInfo = message.getFenceAlarmPushInfo();
                alarmPushInfo.getFenceId();//获取围栏id
                alarmPushInfo.getMonitoredPerson();//获取监控对象标识
                alarmPushInfo.getFenceName();//获取围栏名称
                alarmPushInfo.getPrePoint();//获取上一个点经度信息
                AlarmPoint alarmPoin = alarmPushInfo.getCurrentPoint();//获取报警点经纬度等信息
                alarmPoin.getCreateTime();//获取此位置上传到服务端时间
                alarmPoin.getLocTime();//获取定位产生的原始时间

                if(alarmPushInfo.getMonitoredAction() == MonitoredAction.enter){//动作类型
                    //进入围栏
                    Toast.makeText(FenceSetActivity.this, "you enter fence!", Toast.LENGTH_SHORT).show();


                }else if(alarmPushInfo.getMonitoredAction() == MonitoredAction.exit){
                    //离开围栏
                    Toast.makeText(FenceSetActivity.this, "you exit fence!", Toast.LENGTH_SHORT).show();
                }
            }
            //存储服务
            @Override
            public void onInitBOSCallback(int i, String s) {
                Log.d(TAG, " onPushCallback: " + i);
            }
        };


        /*******************初始化围栏监听器************/
        mFenceListener = new OnFenceListener() {
            // 创建围栏回调
            @Override
            public void onCreateFenceCallback(CreateFenceResponse response) {
                //创建围栏响应结果,能获取围栏的一些信息
                if(StatusCodes.SUCCESS != response.getStatus()){
                    Log.d(TAG,"no create success "+response.getStatus()+response.getMessage());
                    Toast.makeText(FenceSetActivity.this, "创建失败:"+response.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
                else {
                    Log.d(TAG,"create success "+response.getStatus()+response.getMessage());
                    Toast.makeText(FenceSetActivity.this, "创建成功", Toast.LENGTH_SHORT).show();
                    fenceId = response.getFenceId();
                    Log.d(TAG,"fenceid : "+fenceId);
                    setCircle();
                }
                if (isFirstFence)
                {
                    outFenceIds.add(fenceId);
                    outRegisterStateWrite();
                    Log.d(TAG,"out : "+outFenceIds);
                    isFirstFence =false;
                }else {
                    inFenceIds.add(fenceId);//构造CircleOptions对象
                    inRegisterStateWrite();
                    isFirstFence =true;
                }



            }
            // 更新围栏回调
            @Override
            public void onUpdateFenceCallback(UpdateFenceResponse response) {
            }
            // 删除围栏回调
            @Override
            public void onDeleteFenceCallback(DeleteFenceResponse response) {
                Log.d(TAG,"delete success "+response.getStatus());
                Log.d(TAG,"delete FenceIds "+response.getFenceIds());
                if(response.status ==0)
                {
                    Toast.makeText(FenceSetActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                    deleLatRegisterStateWrite();
                }

            }
            // 围栏列表回调
            @Override
            public void onFenceListCallback(FenceListResponse response) {
                Log.d(TAG,"围栏大小："+response.getSize());
                Log.d(TAG,"围栏列表："+response.getFenceInfos());
                if(response.getSize()>2)
                {
                    Toast.makeText(FenceSetActivity.this, "请不要设置两个以上围栏！", Toast.LENGTH_SHORT).show();
                }
                Toast.makeText(FenceSetActivity.this, "围栏大小:"+response.getSize(), Toast.LENGTH_SHORT).show();
                Constant.fencesize = response.getSize();
            }
            // 监控状态回调
            @Override
            public void onMonitoredStatusCallback(MonitoredStatusResponse
                                                          response) {
                //查询监控对象状态响应结果
                List<MonitoredStatusInfo> monitoredStatusInfos = response.getMonitoredStatusInfos();
                for (MonitoredStatusInfo monitoredStatusInfo : monitoredStatusInfos){
                    monitoredStatusInfo.getFenceId();
                    MonitoredStatus status = monitoredStatusInfo.getMonitoredStatus();//获取状态
                    switch (status){
                        case in:
                            //监控的设备在围栏内
                            Toast.makeText(FenceSetActivity.this, "in fence", Toast.LENGTH_SHORT).show();
                            break;
                        case out:
                            //监控的设备在围栏外
                            Toast.makeText(FenceSetActivity.this, "out fence", Toast.LENGTH_SHORT).show();
                            break;
                        case unknown:
                            //监控的设备状态未知
                            Toast.makeText(FenceSetActivity.this, "unknow", Toast.LENGTH_SHORT).show();
                            break;
                    }
                }

            }
            // 指定位置监控状态回调
            @Override
            public void onMonitoredStatusByLocationCallback(MonitoredStatusByLocationResponse response) {}
            // 历史报警回调
            @Override
            public void onHistoryAlarmCallback(HistoryAlarmResponse response) {}
        };

    }
    private void checkfence()
    {
        FenceListRequest request = FenceListRequest.buildServerRequest(Constant.tag,Constant.serviceId, Constant.entityName, Constant.FenceIds, CoordType.bd09ll);
//发起查询围栏请求
        mClient.queryFenceList(request, mFenceListener);

    }

    private void deletefence()
    {
        DeleteFenceRequest deleteRequest = DeleteFenceRequest.buildServerRequest(Constant.tag,Constant.serviceId, Constant.entityName, Constant.FenceIds);
        //发起删除围栏请求
        mClient.deleteFence(deleteRequest , mFenceListener);

    }


    /**
     * 添加标注覆盖物
     */
    //marker的当前坐标latlonx
    private void addMarkerOverlay(LatLng latlonx) {
        bdMap.clear();
        // 构建markerOption，用于在地图上添加marker
        OverlayOptions options = new MarkerOptions()//
                .position(latlonx)// 设置marker的位置
                .icon(bitmap)// 设置marker的图标
                .zIndex(9)// 設置marker的所在層級
                .draggable(true);// 设置手势拖拽
        // 在地图上添加marker，并显示
        marker1 = (Marker) bdMap.addOverlay(options);
    }


    /**
     * 定位初始化
     */
    private void init() {
        bdMap.setMyLocationEnabled(true);
        // 1. 初始化LocationClient类
        locationClient = new LocationClient(getApplicationContext());
        // 2. 声明LocationListener类
        locationListener = new MyLocationListener();
        // 3. 注册监听函数
        locationClient.registerLocationListener(locationListener);
        // 4. 设置参数
        LocationClientOption locOption = new LocationClientOption();
        locOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);// 设置定位模式
        locOption.setCoorType("bd09ll");// 设置定位结果类型
        locOption.setScanSpan(2000);// 设置发起定位请求的间隔时间,ms
        locOption.setIsNeedAddress(true);// 返回的定位结果包含地址信息
        locOption.setNeedDeviceDirect(true);// 设置返回结果包含手机的方向

        locationClient.setLocOption(locOption);
        // 5. 注册位置提醒监听事件

        // 6. 开启/关闭 定位SDK
        locationClient.start();
    }


    class MyLocationListener extends BDAbstractLocationListener {

        // 异步返回的定位结果
        @Override
        public void onReceiveLocation(BDLocation location) {
            if (location == null) {
                return;
            }
            x++;
            longitude = location.getLongitude();
            latitude = location.getLatitude();
            //坐标
            coordinate = new LatLng(latitude, longitude);

            if(isNoSetMark)
            {
                coordinate1 = coordinate;
            }
            //添加覆盖物marker
            if (x == 1) {//防止红色图标拖动到新位置后又因为第二次定位而自动又返回到小蓝点的位置
                //只在最开始定位时，让marker和小蓝点定位在一起，随着marker的拖动，marker不在和小蓝点绑在一起
                addMarkerOverlay(coordinate);
            }
            if (x > 1)//表示大于一次定位，由于addMarkerOverlay(latlon)只能被调用一次,如果此时是第二次定位且第一次定位时没有拖动红色图标会导致marker没有初始化而被闪退
            {
                if (y == 0) {
                    //如果j==0表示没有拖动红色图标并且不是第一次定位
                    addMarkerOverlay(coordinate);//如果是这种情况，再次让红色图标与小蓝点绑定在一起,已到达初始化的目的
                }
                //将上一次拖动后的marker的新位置作为第二次进入的初始化在地图上显示。
                if (y > 0) {
                    addMarkerOverlay(coordinate1);//保存marker上一次拖动的位置
                }
            }

            if (location.hasRadius()) {// 判断是否有定位精度半径
                radius = location.getRadius();
            }
            direction = location.getDirection();// 获取手机方向，【0~360°】,手机上面正面朝北为0°
            // 构造定位数据
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(radius)
                    .direction(direction)// 方向
                    .latitude(latitude)
                    .longitude(longitude)
                    .build();
            // 设置定位数据
            bdMap.setMyLocationData(locData);

            //如果是第一次定位需要把地图移动到当前点
            if (isFirstLoc) {
                isFirstLoc = false;
                LatLng lll = new LatLng(location.getLatitude(),
                        location.getLongitude());
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(lll).zoom(18.0f);//室内定位18层
                bdMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            }
            //最新位置
            newcenter = new LatLng(location.getLatitude(), location.getLongitude());
        }
    }
    private void setCenter()
    {

        MapStatus.Builder builder = new MapStatus.Builder();
        builder.target(newcenter);
        bdMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));

    }
    private void setCircle(){

        Constant.outCircle = new CircleOptions().center(coordinate1)
                .radius((int)Constant.outfence_radius)
                .fillColor(0xccffff)
                .stroke(new Stroke(5, 0xAA00ff00)); //边框宽和边框颜色
        Constant.inCircle = new CircleOptions().center(coordinate1)
                .radius((int)Constant.infence_radius)
                .fillColor(0xccffff)
                .stroke(new Stroke(5, 0xAA9933ff)); //边框宽和边框颜色

    }
    public void darwFence()
    {
        bdMap.addOverlay(Constant.inCircle);
        bdMap.addOverlay(Constant.outCircle);
    }

    /* 将状态存放到xml文件中
     */
    public void inRegisterStateWrite(){
        SharedPreferences sf;
        Context context = this;
        sf = context.getSharedPreferences("fenceData", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sf.edit();
        editor.putLong("inFenceId",fenceId);
        editor.commit();
    }
    public void outRegisterStateWrite(){
        SharedPreferences sf;
        Context context = this;
        sf = context.getSharedPreferences("fenceData", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sf.edit();
        editor.putLong("outFenceId",fenceId);
        editor.commit();
    }
    public void latRegisterStateWrite(){
        SharedPreferences sf;
        Context context = this;
        sf = context.getSharedPreferences("fenceData", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sf.edit();
        editor.putString("centerLatitude",String.valueOf(coordinate1.latitude));
        editor.putString("centerLongtitude",String.valueOf(coordinate1.longitude));
        Toast.makeText(FenceSetActivity.this, "保存坐标:"+coordinate1, Toast.LENGTH_SHORT).show();
        editor.putString("outFenceRadius",String.valueOf(Constant.outfence_radius));
        editor.putString("inFenceRadius",String.valueOf(Constant.infence_radius));
        editor.commit();
    }
    // 删除了之后要对xml文件进行改变
    public void deleLatRegisterStateWrite()
    {
        SharedPreferences sf;
        Context context = this;
        sf = context.getSharedPreferences("fenceData", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sf.edit();
        editor.putString("centerLatitude",String.valueOf(0));
        editor.putString("centerLongtitude",String.valueOf(0));
        editor.putString("outFenceRadius",String.valueOf(0));
        editor.putString("inFenceRadius",String.valueOf(0));
        editor.commit();
    }
    /******读围栏数据*****/
    private void readRadiusRegisterState()
    {

        Context context = this;
        SharedPreferences sp = context.getSharedPreferences("fenceData", Context.MODE_PRIVATE);
        //  Toast.makeText(MainActivity.this, "坐标："+coordinate1, Toast.LENGTH_SHORT).show();
        Constant.infence_radius = Double.valueOf(sp.getString("inFenceRadius","50"));
        Constant.outfence_radius = Double.valueOf(sp.getString("outFenceRadius","500"));

    }



    protected void onResume() {
        super.onResume();
        mapview.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapview.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationClient.unRegisterLocationListener(locationListener);
        //取消位置提醒
        locationClient.stop();
        // 回收bitmip资源
        bitmap.recycle();
        mapview.onDestroy();
        // mClient.stopTrace(Constant.mTrace,traceListener);
    }
}
