package com.debug.tools.alipay;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.alipay.sdk.app.AlipayApi;
import com.alipay.sdk.app.AuthTask;
import com.alipay.sdk.app.PayTask;
import com.debug.tools.AndroidHelper;

import java.util.Map;

public class AliPay {
    @SuppressLint("StaticFieldLeak")
    private static Activity mActivity;
    private static final int SDK_PAY_FLAG = 1;
    private static final int SDK_AUTH_FLAG = 2;
    public static String APP_ID;
    @SuppressLint({"HandlerLeak"})
    private static Handler mHandler = new Handler(){
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SDK_PAY_FLAG: {
                    @SuppressWarnings("unchecked")
                    PayResult payResult = new PayResult((Map<String, String>) msg.obj);
                    /*
                     * 对于支付结果，请商户依赖服务端的异步通知结果。同步通知结果，仅作为支付结束的通知。
                     */
                    String resultInfo = payResult.getResult();// 同步返回需要验证的信息
                    String resultStatus = payResult.getResultStatus();
                    // 判断resultStatus 为9000则代表支付成功
                    if (TextUtils.equals(resultStatus, "9000")) {
                        // 该笔订单是否真实支付成功，需要依赖服务端的异步通知。
                        AndroidHelper.sendUnityMsg("{\"Name\":\"AlipayPayResult\",\"Data\":\"支付成功\"}");
                    } else {
                        // 该笔订单真实的支付结果，需要依赖服务端的异步通知。
                        AndroidHelper.sendUnityMsg("{\"Name\":\"AlipayPayResult\",\"Data\":\"支付失败\"}");
                    }
                    break;
                }
                case SDK_AUTH_FLAG: {
                    @SuppressWarnings("unchecked")
                    AuthResult authResult = new AuthResult((Map<String, String>) msg.obj, true);
                    String resultStatus = authResult.getResultStatus();
                    String resultCode = authResult.getAuthCode();
                    // 判断resultStatus 为“9000”且result_code
                    // 为“200”则代表授权成功，具体状态码代表含义可参考授权接口文档
                    if (TextUtils.equals(resultStatus, "9000") && TextUtils.equals(authResult.getResultCode(), "200")) {
                        // 获取alipay_open_id，调支付时作为参数extern_token 的value
                        // 传入，则支付账户为该授权账户
                        AndroidHelper.sendUnityMsg("{\"Name\":\"AlipayAuthResult\",\"Data\":\"鉴权成功\"}");
                    } else {
                        // 其他状态值则为授权失败
                        AndroidHelper.sendUnityMsg("{\"Name\":\"AlipayAuthResult\",\"Data\":\"鉴权成功\"}");
                    }
                    break;
                }
                default:
                    break;
            }
        };
    };

    public static void init(Activity activity, String appId){
        APP_ID = appId;
        mActivity = activity;
        AlipayApi.registerApp(mActivity, appId);
    }
    public static void Pay(final String orderInfo) {
        try{
            Runnable payRunnable = new Runnable() {
                @Override
                public void run() {
                    PayTask pay = new PayTask(mActivity);
                    Map<String,String> result = pay.payV2(orderInfo,true);
                    Message msg = new Message();
                    msg.what = SDK_PAY_FLAG;
                    msg.obj = result;
                    mHandler.sendMessage(msg);
                }
            };
            // 必须异步调用
            Thread payThread = new Thread(payRunnable);
            payThread.start();
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static void Auth(String authInfo) {
        Runnable authRunnable = new Runnable() {
            @Override
            public void run() {
                // 构造AuthTask 对象
                AuthTask auth = new AuthTask(mActivity);
                // 调用授权接口，获取授权结果
                Map<String, String> result = auth.authV2(authInfo, true);
                Message msg = new Message();
                msg.what = SDK_AUTH_FLAG;
                msg.obj = result;
                mHandler.sendMessage(msg);
            }
        };

        // 必须异步调用
        Thread authThread = new Thread(authRunnable);
        authThread.start();
    }
}
