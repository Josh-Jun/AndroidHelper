package com.debug.tools;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import androidx.annotation.RequiresPermission;
import androidx.core.content.FileProvider;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class AndroidHelper {
    @SuppressLint("StaticFieldLeak")
    private static Context mContext = null;
    @SuppressLint("StaticFieldLeak")
    private static Activity UnityActivity = null;

    private static int mSignalLevel = 0;
    private static Toast mToast;

    public static UnityMsgHolder holder = new UnityMsgHolder();

    private static final String GoName = "Master";

    // 初始化
    public static void init(Context context) {
        if (mContext == null) {
            mContext = context;
        }
        if (UnityActivity == null) {
            UnityActivity = (Activity) context;
        }
    }

    // 获取App发过来的消息
    public static String getAppData(String key) {
        return UnityActivity.getIntent().getStringExtra(key);
    }

    //打开应用设置
    public static void openAppSetting(){
        Uri uri = Uri.fromParts("package", mContext.getPackageName(), (String)null);
        Intent intent = new Intent();
        intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
        intent.setData(uri);
        mContext.startActivity(intent);
    }

    // 退出UnityActivity
    public static void quitUnityActivity() { UnityActivity.finish(); }

    // 安装apk
    public static void installApp(String appFullPath) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            File apkFile = new File(appFullPath);
            Uri uri = null;
            if (Build.VERSION.SDK_INT >= 24) {
                uri = FileProvider.getUriForFile(mContext, "com.debug.tools.fileProvider", apkFile);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                uri = Uri.fromFile(apkFile);
            }

            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            //解决安卓8.0安装界面不弹出
            //查询所有符合 intent 跳转目标应用类型的应用，注意此方法必须放置在 setDataAndType 方法之后
            List<ResolveInfo> resolveLists = mContext.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            // 然后全部授权
            for (ResolveInfo resolveInfo : resolveLists) {
                String packageName = resolveInfo.activityInfo.packageName;
                mContext.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
            mContext.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*获取wifi信号强度
      wifi-info.getRssi()；获取RSSI，RSSI就是接受信号强度指示。
      这里得到信号强度就靠wifi-info.getRssi()这个方法。
      得到的值是一个0到-100的区间值，是一个int型数据，其中0到-50表示信号最好，
      -50到-70表示信号偏差，小于-70表示最差，
      有可能连接不上或者掉线，一般Wifi已断则值为-200。*/
    public static int getWiFiSignal() {
        // Wifi的连接速度及信号强度：
        int level = 0;
        int strength = 0;
        WifiManager wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo();
        if (info.getBSSID() != null) {
            // 链接信号强度
            strength = WifiManager.calculateSignalLevel(info.getRssi(), 5);
        }
        if (strength > -100 && strength < -70) {
            level = 1;
        } else if (strength > -70 && strength < -50) {
            level = 2;
        } else if (strength > -50) {
            level = 3;
        }
        return level;
    }

    //获取移动网络信号 0-4
    public static int getMonetSignal() {
        TelephonyManager mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (mTelephonyManager != null) {
            mTelephonyManager.listen(new PhoneStateListener() {
                @Override
                public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                    super.onSignalStrengthsChanged(signalStrength);
                    mSignalLevel = signalStrength.getLevel();
                }
            }, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        }
        return mSignalLevel;
    }
    //震动效果
    //这里的pattern数组 pattern[0] 是延迟震动时间，pattern[1]是震动的频率，pattern[2]是暂停时间，pattern[3]是震动的频率 依此类推
    //index：震动的类型
    //index = -1 只震动一次
    //index = 0 一直震动
    @RequiresPermission(Manifest.permission.VIBRATE)
    public static void vibrate(long[] pattern, int index) {
        Vibrator vibrator = (Vibrator)mContext.getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, index));
        }
    }
    //Toast弹窗
    public static void showTips(final String str) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(mContext, str, Toast.LENGTH_SHORT);
        mToast.show();
    }

    //发送消息给Unity
    public static void sengUnityMsg(String methodName, String parameter) {
        try {
            Class<?> unity = UnityActivity.getClass();
            Method method = unity.getMethod("UnitySendMessage", String.class, String.class, String.class);
            method.invoke(unity, GoName, methodName, parameter);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    // 接收Unity消息
    public static void receiveUnityMsg(String msg) {
        holder.execute(msg);
    }
}