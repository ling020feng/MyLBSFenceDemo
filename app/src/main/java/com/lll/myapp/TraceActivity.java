package com.lll.myapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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
import com.baidu.trace.api.track.HistoryTrackRequest;
import com.baidu.trace.api.track.HistoryTrackResponse;
import com.baidu.trace.api.track.OnTrackListener;
import com.baidu.trace.api.track.TrackPoint;
import com.baidu.trace.model.BaseRequest;
import com.baidu.trace.model.ProcessOption;
import com.baidu.trace.model.StatusCodes;
import com.baidu.trace.model.TransportMode;
import com.lll.myapp.utils.Constant;

import java.util.ArrayList;
import java.util.List;

import static com.lll.myapp.utils.Constant.mClient;
/***
 * 4.20 把查询轨迹改成查询服务台数据，删除读取本地轨迹数据点
 * ***/
public class TraceActivity extends AppCompatActivity {
    // 存放读取的绘画点
    public List<LatLng> mdrawTrackPoint = new ArrayList<>();
    // 轨迹点列表
    private List<TrackPoint> trackPoints = new ArrayList<>();
    // 绘画点
    public  List<LatLng> drawTrackPoint = new ArrayList<>();
    MapView mapView = null;
    BaiduMap baiduMap = null;
    // 定位客户端构建
    LocationClient mLocationClient;
    LatLng newCenter;

    OnTrackListener mTrackListener = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trace);

//找一个地图控件
        mapView = findViewById(R.id.mTraceMap);
        baiduMap = mapView.getMap();
// 设置地图属性和显示定位
        initMap();
//更新定位数据
        updateCnfiguratin();
// 初始监听
       initListener();
// 获取轨迹
        getTrace();
// 画轨迹
        drawTrace();

    }

    public void initMap() {
        // 开启定位图层
        baiduMap.setMyLocationEnabled(true);

        // 定位初始化
        mLocationClient = new LocationClient(this);
        // 声明LocationClient类实例并配置定位参数
        TraceActivity.MyLocationListener myLocationListener = new TraceActivity.MyLocationListener();
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
            newCenter = new LatLng(location.getLatitude(), location.getLongitude());
            setCenter();
        }
    }


    private void setCenter()
    {

        MapStatus.Builder builder = new MapStatus.Builder();
        builder.target(newCenter);
        builder.target(newCenter).zoom(18.0f);// 图层级别
        baiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));

    }
    public void updateCnfiguratin() {
        // 注册LocationListener监听器
        TraceActivity.MyLocationListener mListener = new TraceActivity.MyLocationListener();
        mLocationClient.registerLocationListener(mListener);
        // 开始定位位置
        mLocationClient.start();

    }
    // 请求查询轨迹
    private void getTrace(){
       Constant.startTime = System.currentTimeMillis() / 1000 - 12*60*60;
       Constant.stopTime = System.currentTimeMillis() / 1000;
        Toast.makeText(TraceActivity.this, "start checktrace", Toast.LENGTH_SHORT).show();
// 创建历史轨迹请求实例
        HistoryTrackRequest historyTrackRequest = new HistoryTrackRequest(Constant.tag, Constant.serviceId, Constant.entityName);
// 设置开始时间
        historyTrackRequest.setStartTime(Constant.startTime);
// 设置结束时间
        historyTrackRequest.setEndTime(Constant.stopTime);
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


    public void drawFence()
    {
        baiduMap.addOverlay(Constant.inCircle);
        baiduMap.addOverlay(Constant.outCircle);
    }

    //绘画轨迹
    public void drawTrace() {
        if(drawTrackPoint.isEmpty())
        {
            return;
        }else if (drawTrackPoint.size()==1)
        {
            Toast.makeText(TraceActivity.this,"the points only one",Toast.LENGTH_SHORT).show();
            return;
        }
        //画图
        OverlayOptions mOverlayOptions = new PolylineOptions()
                .width(10)
                .color(0xAAFF0000)
                .points(drawTrackPoint);

        baiduMap.addOverlay(mOverlayOptions);

        drawFence();
        Toast.makeText(TraceActivity.this,"draw trace end",Toast.LENGTH_SHORT).show();
    }

    private void initListener()
    {
/******************************初始化历史轨迹监听器**********************/

        mTrackListener = new OnTrackListener() {
            // 历史轨迹回调
            @Override
            public void onHistoryTrackCallback(HistoryTrackResponse response) {
                if (response.status == 0)
                {
                    int total = response.getTotal();// 点的总数量
                    Toast.makeText(TraceActivity.this,"点的数量为："+total,Toast.LENGTH_SHORT).show();
                    if (StatusCodes.SUCCESS != response.getStatus()) {
                        Toast.makeText(TraceActivity.this, "no find", Toast.LENGTH_SHORT).show();
                    } else if (0 == total) {
                        Toast.makeText(TraceActivity.this, "nothing", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(TraceActivity.this, "get trackpoints", Toast.LENGTH_SHORT).show();
                        trackPoints.clear();                // 每次查询都清空接收数组
                        trackPoints = response.getTrackPoints();// 返回的处理点集合
                    }
                    //把查询到的轨迹集合简单处理
                    if (null != trackPoints) {
                        double newLatitude;
                        double newLongtitude;
                        drawTrackPoint.clear();
                        Toast.makeText(TraceActivity.this, "size:"+trackPoints.size(), Toast.LENGTH_SHORT).show();
                        for (int i=0;i< trackPoints.size();i++) {// 迭代遍历，如果不是空点，加入轨迹点集合
                            newLatitude = trackPoints.get(i).getLocation().getLatitude();
                            newLongtitude = trackPoints.get(i).getLocation().getLongitude();
                            LatLng mPoint = new LatLng(newLatitude,newLongtitude);
                            drawTrackPoint.add(mPoint);
                        }
                    }
                    drawTrace();

                }else {
                    Toast.makeText(TraceActivity.this,"获取失败",Toast.LENGTH_SHORT).show();

                }

            }

        };

    }
    // 销毁
    protected void onDestroy() {
        super.onDestroy();
        mLocationClient.stop();
        baiduMap.setMyLocationEnabled(false);
        mapView.onDestroy();
        mapView = null;
    }

}


