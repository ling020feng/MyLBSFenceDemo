package com.lll.myapp.utils;

import com.baidu.mapapi.map.CircleOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.trace.LBSTraceClient;
import com.baidu.trace.Trace;
import com.baidu.trace.model.CoordType;

import java.util.ArrayList;
public class Constant {
    //  设定识别动作
    public static final String BROADCAST_DETECTED_ACTIVITY = "activity_intent";
    // 设定时间越大，提高电池寿命，如果是0，检测速度将是最快
    public static final long DETECTION_INTERVAL_IN_MILLISECONDS = 10*1000; // 单位间隔： N ms
    /*****状态参数*****/
    public static final int FIRST_START_STATUS =12 ;

    public static final int INSIDE = 0;   // 里面
    public static final int INTERMEDIATE = 1;// 中间
    public static final int OUTSIDE = 2; // 外面

    public static boolean isIN_INFENCE = false;// 在内栏里
    public static boolean isIN_OUTFENCE = false;// 外栏里

    public static int mFinishStatus;

    /*****轨迹服务参数*****/
    //轨迹客户端
    public static LBSTraceClient mClient ;
    //初始化鹰眼服务端，serviceId：服务端唯一id，entityName：监控的对象，可以是手机设备唯一标识
    public static Trace mTrace;
    //第一次初始化服务
    public static boolean isFirstServer = true;
    // 定位周期(单位:秒)
    public static int gatherInterval = 2;
    // 打包回传周期(单位:秒)
    public static int packInterval = 10;

    public static long startTime;
    public static long stopTime;

    /****围栏参数*****/
    //外栏
    public static final long OUT_FENCE = 100L;
    //内栏
    public static final long IN_FENCE = 101L;

    // 请求标识
    public static int tag = 3;
    // 轨迹服务ID
    public static final long serviceId = 211658;
    // 围栏名称
    public static String fenceName = "local_circle";
    // 监控对象
    public static String entityName;

    // 围栏圆心
    //com.baidu.trace.model.LatLng center = new com.baidu.trace.model.LatLng(coordinate1.latitude,coordinate1.longitude);
    // 围栏半径（单位 : 米）
    public static double outfence_radius ;
    public static double infence_radius ;

    // 去噪精度
    public static int denoise = 50;
    public static int indenoise = 15;
    // 坐标类型
    public static CoordType coordType = CoordType.bd09ll;

    // 外栏集合
    public static java.util.List<java.lang.Long> outFenceIds = new ArrayList<>();
    // 内栏集合
    public static java.util.List<java.lang.Long> inFenceIds = new ArrayList<>();
    // 所有的
    public static java.util.List<java.lang.Long> FenceIds;
    public static int fencesize=0;

    public static CircleOptions outCircle; // 绘画物实例
    public static CircleOptions inCircle;

    //这里是保存拖动后图标位置
    static public LatLng coordinate1;

}
