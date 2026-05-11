package com.debug.tools.wechat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.debug.tools.AndroidHelper;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXImageObject;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXTextObject;
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject;
import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

public class WeChat {
    private static IWXAPI m_WxApi;
    @SuppressLint("StaticFieldLeak")
    private static Activity mActivity;
    private static String APP_ID;

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public static void init(Activity activity, String appId){
        APP_ID = appId;
        mActivity = activity;
        m_WxApi = GetWxAPI();
        m_WxApi.registerApp(APP_ID);
        mActivity.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                m_WxApi.registerApp(APP_ID);
            }
        }, new IntentFilter("com.tencent.mm.plugin.openapi.Intent.ACTION_REFRESH_WXAPP"));

        m_WxApi.handleIntent(mActivity.getIntent(), new IWXAPIEventHandler() {
            @Override
            public void onReq(BaseReq baseReq) {
                switch (baseReq.getType()) {
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    default:
                }
            }

            @Override
            public void onResp(BaseResp baseResp) {
                switch (baseResp.getType()) {
                    case 1:
                        this.onSendAuthResp(baseResp);
                        break;
                    case 5:
                        this.onSendPayResp(baseResp);
                        break;
                    case 6:
                        break;
                }
            }

            public void onSendAuthResp(BaseResp baseResp){
                int errorCode = baseResp.errCode;
                switch (errorCode) {
                    case -4:
                        AndroidHelper.sendUnityMsg("{\"Name\":\"WeChatAuthResult\",\"Data\":\"用户拒绝\"}");
                    case -3:
                    case -1:
                        break;
                    case -2:
                        AndroidHelper.sendUnityMsg("{\"Name\":\"WeChatAuthResult\",\"Data\":\"用户取消\"}");
                        break;
                    case 0:
                        String code = ((SendAuth.Resp)baseResp).code;
                        AndroidHelper.sendUnityMsg("{\"Name\":\"WeChatAuthResult\",\"Data\":\"鉴权成功\"}");
                    default:
                }
            }

            public void onSendPayResp(BaseResp baseResp){
                int errorCode = baseResp.errCode;
                switch (errorCode) {
                    case -2:
                        AndroidHelper.sendUnityMsg("{\"Name\":\"WeChatPayResult\",\"Data\":\"支付取消\"}");
                        break;
                    case 0:
                        AndroidHelper.sendUnityMsg("{\"Name\":\"WeChatPayResult\",\"Data\":\"支付成功\"}");
                        break;
                    default:
                        AndroidHelper.sendUnityMsg("{\"Name\":\"WeChatPayResult\",\"Data\":\"支付失败\"}");
                }
            }
        });
    }
    public static IWXAPI GetWxAPI() {
        if (m_WxApi == null) {
            m_WxApi = WXAPIFactory.createWXAPI(mActivity, APP_ID);
        }
        return m_WxApi;
    }

    public static void Pay(final String partnerID, final String prepayID, final String nonceStr, final String timeStamp, final String sign) {
        try {
            PayReq req = new PayReq();
            req.appId = APP_ID;
            req.partnerId = partnerID;
            req.prepayId = prepayID;
            req.packageValue = "Sign=WXPay";
            req.nonceStr = nonceStr;
            req.timeStamp = timeStamp;
            req.sign = sign;
            m_WxApi.sendReq(req);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean Auth() {
        try {
            SendAuth.Req req = new SendAuth.Req();
            req.scope = "snsapi_userinfo";
            req.state = "wechat_sdk";
            m_WxApi.sendReq(req);
            Log.w("unity", "Pay: 微信发起登录申请");
            return true;
        } catch (Exception var1) {
            Log.e("unity", "Pay: 微信登录申请失败");
            return false;
        }
    }

    public static void ShareText(String text, int scene) {
        WXTextObject textObj = new WXTextObject();
        textObj.text = text;
        WXMediaMessage msg = new WXMediaMessage();
        msg.mediaObject = textObj;
        msg.description = text;
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction("text");
        req.message = msg;
        req.scene = scene;
        m_WxApi.sendReq(req);
    }

    public static void ShareImage(byte[] imageDate, byte[] thumbImageDate, int scene) {
        WXImageObject imgObj = new WXImageObject(imageDate);
        WXMediaMessage msg = new WXMediaMessage();
        msg.mediaObject = imgObj;
        msg.thumbData = thumbImageDate;
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction("img");
        req.message = msg;
        req.scene = scene;
        m_WxApi.sendReq(req);
    }

    public static void ShareWeb(String url, String title, String description, byte[] thumbImageData, int scene) {
        WXWebpageObject webpage = new WXWebpageObject();
        webpage.webpageUrl = url;
        WXMediaMessage msg = new WXMediaMessage(webpage);
        msg.title = title;
        msg.description = description;
        msg.thumbData = thumbImageData;
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction("webpage");
        req.message = msg;
        req.scene = scene;
        m_WxApi.sendReq(req);
    }

    private static String buildTransaction(final String type) {
        return type == null ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
    }
}
