package com.lll.myapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.trace.api.fence.AlarmPoint;
import com.baidu.trace.api.fence.CreateFenceResponse;
import com.baidu.trace.api.fence.DeleteFenceResponse;
import com.baidu.trace.api.fence.FenceAlarmPushInfo;
import com.baidu.trace.api.fence.FenceListResponse;
import com.baidu.trace.api.fence.HistoryAlarmResponse;
import com.baidu.trace.api.fence.MonitoredAction;
import com.baidu.trace.api.fence.MonitoredStatus;
import com.baidu.trace.api.fence.MonitoredStatusByLocationRequest;
import com.baidu.trace.api.fence.MonitoredStatusByLocationResponse;
import com.baidu.trace.api.fence.MonitoredStatusInfo;
import com.baidu.trace.api.fence.MonitoredStatusResponse;
import com.baidu.trace.api.fence.OnFenceListener;
import com.baidu.trace.api.fence.UpdateFenceResponse;
import com.baidu.trace.api.track.HistoryTrackRequest;
import com.baidu.trace.api.track.HistoryTrackResponse;
import com.baidu.trace.api.track.OnTrackListener;
import com.baidu.trace.api.track.TrackPoint;
import com.baidu.trace.model.BaseRequest;
import com.baidu.trace.model.OnTraceListener;
import com.baidu.trace.model.ProcessOption;
import com.baidu.trace.model.PushMessage;
import com.baidu.trace.model.StatusCodes;
import com.baidu.trace.model.TransportMode;
import com.lll.myapp.utils.Constant;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static com.lll.myapp.utils.Constant.FIRST_START_STATUS;
import static com.lll.myapp.utils.Constant.INSIDE;
import static com.lll.myapp.utils.Constant.INTERMEDIATE;
import static com.lll.myapp.utils.Constant.OUTSIDE;
import static com.lll.myapp.utils.Constant.coordinate1;
import static com.lll.myapp.utils.Constant.inFenceIds;
import static com.lll.myapp.utils.Constant.isIN_INFENCE;
import static com.lll.myapp.utils.Constant.isIN_OUTFENCE;
import static com.lll.myapp.utils.Constant.mClient;
import static com.lll.myapp.utils.Constant.mFinishStatus;
import static com.lll.myapp.utils.Constant.outFenceIds;
/*********查看实时位置*
 * 4.15完成对围栏编写，状态分离出来，围栏设置编写，能够设置车库门位置坐标 问题：暂时不能准确接收设定围栏状态信息
 * 4.16 还需对服务是否开启判断，实时状态检测代码待写,内栏外栏判定
 *4.17 能够判定进围栏，出围栏。能够绘制围栏。解决了创建围栏失败问题，原因云端没有创建对象,写进出围栏的逻辑代码 目前未解决问题： 关闭应用后，每次都需要进设置界面初始化鹰眼服务，
 * 优化一些逻辑，
 * 4.18 解决昨天遗留问题，解决方式把文件写成xml文件保存，现能够进出判断和能够绘制出围栏间的轨迹，轨迹数据保存代码编写，不过还需要进一步测试.装应用第一次进入鹰眼必须要先设置围栏
 * 4.19 解决第一次安装应用创建不了对象这个问题，不过需要十秒中初始化时间，后续待优化
 * 4.20 删除写Trace点数据文件,增加自定义围栏半径功能
 * 4.22 对一些逻辑问题优化
 *
 * *******/
public class LocationActivity extends AppCompatActivity {
    private static final String TAG = LocationActivity.class.getSimpleName();
    MapView mapView = null;
    BaiduMap baiduMap = null;
    // 是否第一次定位
    boolean firstLoc = true;
    // 定位客户端构建
    LocationClient mLocationClient;
    LatLng mcenter;
    SimpleDateFormat formatter;
//围栏参数

    // private TrackApplication trackApp = null;

    Button mbtn;

    OnFenceListener minFenceListener;
    OnFenceListener moutFenceListener;
    OnTraceListener traceListener = null;
    OnTrackListener mTrackListener = null;


    int preStatus = FIRST_START_STATUS;

    // 轨迹绘制参数

    long startTime;
    long stopTime;
    boolean isStart = false;//控制采集停止
    boolean isStop = false;

    private boolean isStartGather = false;
    // 轨迹点列表
    private List<TrackPoint> trackPoints = new ArrayList<>();
    // 绘画点
    public  List<LatLng> drawTrackPoint = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 显示location界面
        setContentView(R.layout.activity_location);
        mbtn = findViewById(R.id.setcent);

        // 把xml文件读取出来
        readRegisterState();

        //Toast.makeText(LocationActivity.this,"xml 读取成功",Toast.LENGTH_SHORT).show();

        // 初始化显示
        initView();
        Log.d(TAG,"成功初始化显示");
        // 设置地图属性和显示定位
        initMap();
        Log.d(TAG,"成功初始化地图");
        //更新定位数据
        updateCnfiguratin();
        Log.d(TAG,"成功更新地图");

// 初始化监听器
        initListener();
        Log.d(TAG,"成功初始化监听器");
        //Toast.makeText(LocationActivity.this, "外圈："+outFenceIds, Toast.LENGTH_SHORT).show();
        //Toast.makeText(LocationActivity.this, "内圈："+inFenceIds, Toast.LENGTH_SHORT).show();


        mbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(LocationActivity.this, "中心点为"+coordinate1, Toast.LENGTH_SHORT).show();

                setCenter();
                darwFence();
                if(mFinishStatus==INSIDE)
                {
                    Toast.makeText(LocationActivity.this,"你在内栏",Toast.LENGTH_SHORT).show();
                }else if(mFinishStatus == OUTSIDE)
                {
                    Toast.makeText(LocationActivity.this,"你在外栏外",Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(LocationActivity.this,"你在中间",Toast.LENGTH_SHORT).show();
                }

            }
        });

    }

    public void initView() {
        //找一个地图控件
        mapView = findViewById(R.id.mLocationMap);

        baiduMap = mapView.getMap();
        baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);

    }

    public void initMap() {
        // 开启定位图层
        baiduMap.setMyLocationEnabled(true);

        // 定位初始化
        mLocationClient = new LocationClient(this);
        // 声明LocationClient类实例并配置定位参数
        MyLocationListener myLocationListener = new MyLocationListener();
        // 通过LocationClientOption设置LocationClient相关参数
        LocationClientOption option = new LocationClientOption();
        // 注册监听函数
        mLocationClient.registerLocationListener(myLocationListener);
        // 打开gps
        option.setOpenGps(true);
        // 设置坐标类型 默认gcj02
        option.setCoorType("bd09ll");
        // 发起定位请求的间隔时间 单位ms
        option.setScanSpan(1000);
        // 可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        // 可选，设置是否需要地址信息，默认不需要
        option.setIsNeedAddress(true);
        MyLocationConfiguration.LocationMode mMapMode = MyLocationConfiguration.LocationMode.FOLLOWING;
        BitmapDescriptor mMapMarker = BitmapDescriptorFactory.fromResource(R.mipmap.icon_point);
        // 参数列表 显示模式；是否显示方向；设置自定义图标；填充颜色；填充边框颜色
        MyLocationConfiguration mLocationConfiguration = new MyLocationConfiguration(mMapMode, true, mMapMarker);
        baiduMap.setMyLocationConfiguration(mLocationConfiguration);
// 设置locationClientOption
        mLocationClient.setLocOption(option);
    }

    public void updateCnfiguratin() {
        // 注册LocationListener监听器
        LocationActivity.MyLocationListener mListener = new LocationActivity.MyLocationListener();
        mLocationClient.registerLocationListener(mListener);
        // 开始定位位置
        mLocationClient.start();

    }

    public class MyLocationListener extends BDAbstractLocationListener {
        @Override // 获取纬度信息

        // 请求回调         参数：需要回调的类，里面含有各种数据信息
        public void onReceiveLocation(BDLocation location) {
            // mapView 销毁后不在处理新接收的位置
            if (location == null || mapView == null) {
                return;
            }
            //构建一个地图状态 用来设置中心点、俯视角、旋转角度、缩放级别
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(location.getDirection()).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();

            // 设置定位数据，当允许定位图层才会生效
            baiduMap.setMyLocationData(locData);
            // 设置中心点
            mcenter = new LatLng(location.getLatitude(), location.getLongitude());
//  查询该点相对内外栏的状态
            checkinFence();
            checkoutFence();
// 处理该状态
            dealStatus();
            darwFence();
        }
    }

    private void initListener()
    {
        /******************************初始化轨迹服务监听器**********************/
        traceListener = new OnTraceListener() {

            @Override
            public void onBindServiceCallback(int i, String s) {

            }

            @Override
            public void onStartTraceCallback(int i, String s) {
                if (i == 0) {
                    Toast.makeText(LocationActivity.this, "开始服务", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(LocationActivity.this, "开始服务失败"+s, Toast.LENGTH_LONG).show();
                    mClient.startTrace(Constant.mTrace, traceListener);//开始服务

                }
            }

            @Override
            public void onStopTraceCallback(int i, String s) {

            }

            @Override
            public void onStartGatherCallback(int i, String s) {
                if(i==0)
                {
                    Toast.makeText(LocationActivity.this, "开始采集轨迹", Toast.LENGTH_SHORT).show();
                    isStartGather =true;
                }
            }

            @Override
            public void onStopGatherCallback(int i, String s) {
                Toast.makeText(LocationActivity.this, "停止采集轨迹", Toast.LENGTH_SHORT).show();
                isStartGather =false;
            }

            public void onPushCallback(byte messageType, PushMessage pushMessage) {
                if (messageType < 0x03 || messageType > 0x04) {
                    return;
                }
                /**
                 * 获取报警推送消息
                 */
                FenceAlarmPushInfo alarmPushInfo = pushMessage.getFenceAlarmPushInfo();
                alarmPushInfo.getFenceId();//获取围栏id
                alarmPushInfo.getMonitoredPerson();//获取监控对象标识
                alarmPushInfo.getFenceName();//获取围栏名称
                alarmPushInfo.getPrePoint();//获取上一个点经度信息
                AlarmPoint alarmPoin = alarmPushInfo.getCurrentPoint();//获取报警点经纬度等信息
                alarmPoin.getCreateTime();//获取此位置上传到服务端时间
                alarmPoin.getLocTime();//获取定位产生的原始时间

                if(alarmPushInfo.getMonitoredAction() == MonitoredAction.enter){//动作类型
                    //进入围栏
                    Toast.makeText(LocationActivity.this, "you enter fence!", Toast.LENGTH_SHORT).show();


                }else if(alarmPushInfo.getMonitoredAction() == MonitoredAction.exit){
                    //离开围栏
                    Toast.makeText(LocationActivity.this, "you exit fence!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onInitBOSCallback(int i, String s) {

            }
        };



        // 初始化围栏监听器
        moutFenceListener = new OnFenceListener() {
            // 创建围栏回调
            @Override
            public void onCreateFenceCallback(CreateFenceResponse response) {

            }
            // 更新围栏回调
            @Override
            public void onUpdateFenceCallback(UpdateFenceResponse response) {
            }
            // 删除围栏回调
            @Override
            public void onDeleteFenceCallback(DeleteFenceResponse response) {}
            // 围栏列表回调
            @Override
            public void onFenceListCallback(FenceListResponse response) {

            }
            // 监控状态回调
            @Override
            public void onMonitoredStatusCallback(MonitoredStatusResponse
                                                          response) {
                //这里后续开发 服务端报警推送内容

            }
            // 指定位置监控状态回调
            @Override
            public void onMonitoredStatusByLocationCallback(MonitoredStatusByLocationResponse response) {
                //查询监控对象状态响应结果
                List<MonitoredStatusInfo> monitoredStatusInfos = response.getMonitoredStatusInfos();
                for (MonitoredStatusInfo monitoredStatusInfo : monitoredStatusInfos){
                    monitoredStatusInfo.getFenceId();
                    MonitoredStatus status = monitoredStatusInfo.getMonitoredStatus();//获取状态
                    switch (status){
                        case in:
                            //监控的设备在围栏内
                            // Toast.makeText(LocationActivity.this, "in outfence", Toast.LENGTH_SHORT).show();
                            isIN_OUTFENCE =true;

                            break;
                        case out:
                            //监控的设备在围栏外
                            //  Toast.makeText(LocationActivity.this, "out outfence", Toast.LENGTH_SHORT).show();
                            isIN_OUTFENCE =false;

                            break;
                        case unknown:
                            //监控的设备状态未知
                            Toast.makeText(LocationActivity.this, "unknow", Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            }
            // 历史报警回调
            @Override
            public void onHistoryAlarmCallback(HistoryAlarmResponse response) {
            }
        };
/******************************初始化内围栏监听器**********************/
        minFenceListener = new OnFenceListener() {
            // 创建围栏回调
            @Override
            public void onCreateFenceCallback(CreateFenceResponse response) {
                //创建围栏响应结果,能获取围栏的一些信息
                if(StatusCodes.SUCCESS != response.getStatus()){
                    Log.d(TAG,"no success "+response.getStatus());
                    return;
                }
            }
            // 更新围栏回调
            @Override
            public void onUpdateFenceCallback(UpdateFenceResponse response) {
            }
            // 删除围栏回调
            @Override
            public void onDeleteFenceCallback(DeleteFenceResponse response) {}
            // 围栏列表回调
            @Override
            public void onFenceListCallback(FenceListResponse response) {

            }
            // 监控状态回调
            @Override
            public void onMonitoredStatusCallback(MonitoredStatusResponse
                                                          response) {
                /*********这里后续用服务端判断状态*******/

            }
            // 指定位置监控状态回调
            @Override
            public void onMonitoredStatusByLocationCallback(MonitoredStatusByLocationResponse response) {
                //查询监控对象状态响应结果
                List<MonitoredStatusInfo> monitoredStatusInfos = response.getMonitoredStatusInfos();
                if(monitoredStatusInfos!=null)
                {
                    for (MonitoredStatusInfo monitoredStatusInfo : monitoredStatusInfos){
                        monitoredStatusInfo.getFenceId();
                        MonitoredStatus status = monitoredStatusInfo.getMonitoredStatus();//获取状态
                        switch (status){
                            case in:
                                //监控的设备在围栏内
                                //Toast.makeText(LocationActivity.this, "in infence", Toast.LENGTH_SHORT).show();
                                isIN_INFENCE = true;
                                break;
                            case out:
                                //监控的设备在围栏外
                                //Toast.makeText(LocationActivity.this, "out infence", Toast.LENGTH_SHORT).show();
                                isIN_INFENCE = false;
                                break;
                            case unknown:
                                //监控的设备状态未知
                                Toast.makeText(LocationActivity.this, "unknow", Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                }else {
                    Toast.makeText(LocationActivity.this, "服务端围栏数为空！", Toast.LENGTH_SHORT).show();
                }

            }
            // 历史报警回调
            @Override
            public void onHistoryAlarmCallback(HistoryAlarmResponse response) {
            }
        };

/******************************初始化历史轨迹监听器**********************/

        mTrackListener = new OnTrackListener() {
            // 历史轨迹回调
            @Override
            public void onHistoryTrackCallback(HistoryTrackResponse response) {
                if (response.status == 0)
                {
                    int total = response.getTotal();// 点的总数量
                    Toast.makeText(LocationActivity.this,"点的数量为："+total,Toast.LENGTH_SHORT).show();
                    if (StatusCodes.SUCCESS != response.getStatus()) {
                        Toast.makeText(LocationActivity.this, "no find", Toast.LENGTH_SHORT).show();
                    } else if (0 == total) {
                        Toast.makeText(LocationActivity.this, "nothing", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LocationActivity.this, "get trackpoints", Toast.LENGTH_SHORT).show();
                        trackPoints.clear();                // 每次查询都清空接收数组
                        trackPoints = response.getTrackPoints();// 返回的处理点集合
                    }
                    //把查询到的轨迹集合简单处理
                    if (null != trackPoints) {
                        double newLatitude;
                        double newLongtitude;
                        drawTrackPoint.clear();
                        Toast.makeText(LocationActivity.this, "size:"+trackPoints.size(), Toast.LENGTH_SHORT).show();
                        for (int i=0;i< trackPoints.size();i++) {// 迭代遍历，如果不是空点，加入轨迹点集合
                            newLatitude = trackPoints.get(i).getLocation().getLatitude();
                            newLongtitude = trackPoints.get(i).getLocation().getLongitude();
                            LatLng mPoint = new LatLng(newLatitude,newLongtitude);
                            drawTrackPoint.add(mPoint);
                        }
                    }

                    drawTrace();
                }else {
                    Toast.makeText(LocationActivity.this,"获取失败",Toast.LENGTH_SHORT).show();

                }

            }

        };

    }
    //绘画轨迹
    public void drawTrace() {
        if(trackPoints.isEmpty())
        {
            return;
        }else if (trackPoints.size()==1)
        {
            Toast.makeText(LocationActivity.this,"the points only one",Toast.LENGTH_SHORT).show();
            return;
        }
        //画图
        OverlayOptions mOverlayOptions = new PolylineOptions()
                .width(10)
                .color(0xAAFF0000)
                .points(drawTrackPoint);

        baiduMap.addOverlay(mOverlayOptions);
        Toast.makeText(LocationActivity.this,"draw trace end",Toast.LENGTH_SHORT).show();


    }

    private void setCenter()
    {

        MapStatus.Builder builder = new MapStatus.Builder();
        builder.target(mcenter);
        builder.target(mcenter).zoom(18.0f);// 图层级别
        baiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));

    }

    private void checkinFence(){

        // 位置坐标
        com.baidu.trace.model.LatLng location = new com.baidu.trace.model.LatLng(mcenter.latitude,mcenter.longitude);
        // 创建查询服务端围栏监控状态请求
        MonitoredStatusByLocationRequest request = MonitoredStatusByLocationRequest.buildServerRequest(
                Constant.tag,Constant.serviceId, Constant.entityName, inFenceIds, location, Constant.coordType);
// 查询围栏监控者状态
        mClient.queryMonitoredStatusByLocation(request, minFenceListener);
    }

    private void checkoutFence()
    {
        // 位置坐标
        com.baidu.trace.model.LatLng location = new com.baidu.trace.model.LatLng(mcenter.latitude,mcenter.longitude);

        // 创建查询服务端围栏监控状态请求
        MonitoredStatusByLocationRequest request = MonitoredStatusByLocationRequest.buildServerRequest(
                Constant.tag,Constant.serviceId, Constant.entityName, outFenceIds, location, Constant.coordType);
        Log.d(TAG,"status check  ");
// 查询围栏监控者状态
        mClient.queryMonitoredStatusByLocation(request, moutFenceListener);
    }
    private void dealStatus()
    {
        if(isIN_OUTFENCE)
        {
            if(isIN_INFENCE)
            {
                mFinishStatus = INSIDE;// 在内栏
            } else {
                mFinishStatus = INTERMEDIATE; // 在中间
            }
        }else {
            mFinishStatus = OUTSIDE; // 在外栏外
        }

        if(preStatus ==FIRST_START_STATUS){
        }else {
            if(preStatus != mFinishStatus)
            {
                if(preStatus==OUTSIDE&&mFinishStatus==INTERMEDIATE)
                {
                    Toast.makeText(LocationActivity.this,"你进入了外栏",Toast.LENGTH_LONG).show();
                    isStart = true;
                }else if(preStatus==INTERMEDIATE&&mFinishStatus==INSIDE)
                {
                    Toast.makeText(LocationActivity.this,"你进入了内栏",Toast.LENGTH_LONG).show();
                    isStop =true;
                }else if(preStatus==INSIDE&&mFinishStatus ==INTERMEDIATE)
                {
                    Toast.makeText(LocationActivity.this,"你从内栏出来了",Toast.LENGTH_LONG).show();
                    isStart =true;
                }else if(preStatus==INTERMEDIATE&&mFinishStatus==OUTSIDE)
                {
                    Toast.makeText(LocationActivity.this,"你从外栏出来了",Toast.LENGTH_LONG).show();
                    isStop = true;
                }
            }
        }
        preStatus = mFinishStatus;

        getTrace();


    }

    public void darwFence()
    {
        baiduMap.addOverlay(Constant.inCircle);
        baiduMap.addOverlay(Constant.outCircle);
    }

    private void getTrace()
    {
        if(isStart){
            startTime = System.currentTimeMillis() / 1000;
            //Date date = new Date(startTime);
           // formatter = new SimpleDateFormat("yyyy-MM.dd-HH:mm:ss");
            //Toast.makeText(LocationActivity.this, "现在时间："+formatter.format(date), Toast.LENGTH_SHORT).show();
            mClient.startGather(traceListener);
            isStart =false;
        }
        if(isStop){
            stopTime = System.currentTimeMillis() / 1000;
            //Date date1 = new Date(stopTime);
            //formatter = new SimpleDateFormat("yyyy-MM.dd-HH:mm:ss");
            //Toast.makeText(LocationActivity.this, "结束时间："+formatter.format(date1), Toast.LENGTH_SHORT).show();
            mClient.stopGather(traceListener);
            checkTrace();
            isStop =false;
        }

    }
   /****************这里对处理采集轨迹的参数进行设置************************/

    // 请求查询轨迹
    public void checkTrace() {
        Toast.makeText(LocationActivity.this, "start checktrace", Toast.LENGTH_SHORT).show();
// 创建历史轨迹请求实例
        HistoryTrackRequest historyTrackRequest = new HistoryTrackRequest(Constant.tag, Constant.serviceId, Constant.entityName);
// 设置开始时间
        historyTrackRequest.setStartTime(startTime);
// 设置结束时间
        historyTrackRequest.setEndTime(stopTime);
// 是否返回纠偏轨迹
        historyTrackRequest.setProcessed(true);//  纠偏
        ProcessOption mProcessOption = new ProcessOption();
        mProcessOption.setNeedDenoise(true);// 去噪
        mProcessOption.setNeedMapMatch(false);// 绑路
        mProcessOption.setNeedVacuate(false);// 抽稀
        //mProcessOption.setRadiusThreshold(50);// 精度
        mProcessOption.setTransportMode(TransportMode.driving);//模式 driving walking riding
// 设置纠偏的类型
        historyTrackRequest.setProcessOption(mProcessOption);

        ((BaseRequest) historyTrackRequest).setTag(Constant.tag);//设置请求标识，用于唯一标记本次请求，在响应结果中会返回该标识
        historyTrackRequest.setServiceId(Constant.serviceId);//设置轨迹服务id，Trace中的id
        historyTrackRequest.setEntityName(Constant.entityName);//Trace中的entityName
// 查询历史轨迹
        mClient.queryHistoryTrack(historyTrackRequest, mTrackListener);
    }

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

        inFenceIds.clear();
        outFenceIds.clear();
        inFenceIds.add(infenceId);
        outFenceIds.add(outfenceId);
        latitude =  Double.valueOf(sp.getString("centerLatitude","0")) ;
        longtitude = Double.valueOf(sp.getString("centerLongtitude","0")) ;
        coordinate1 = new LatLng(latitude,longtitude);
        if (latitude== 0)
        {
            Toast.makeText(LocationActivity.this,"错误坐标：0,0 ",Toast.LENGTH_SHORT).show();
        }else {
            Toast.makeText(LocationActivity.this,"读的坐标"+coordinate1,Toast.LENGTH_SHORT).show();
        }
    }

    // 暂停
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    // 重启
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    // 销毁
    protected void onDestroy() {
        super.onDestroy();
        mLocationClient.stop();
        baiduMap.setMyLocationEnabled(false);
        mapView.onDestroy();
        mapView = null;
        //mClient.stopTrace(Constant.mTrace, traceListener);
        if(isStartGather)
        {
            mClient.stopGather(traceListener);
        }

    }
}