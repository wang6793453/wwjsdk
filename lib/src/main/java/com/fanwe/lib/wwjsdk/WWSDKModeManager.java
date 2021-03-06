package com.fanwe.lib.wwjsdk;

import android.text.TextUtils;

import com.fanwe.lib.http.callback.ModelRequestCallback;
import com.fanwe.lib.http.impl.httprequest.GetRequest;
import com.fanwe.lib.looper.FLooper;
import com.fanwe.lib.looper.impl.FSimpleLooper;
import com.fanwe.lib.wwjsdk.log.WWLogger;
import com.fanwe.lib.wwjsdk.model.InitActModel;
import com.fanwe.lib.wwjsdk.model.WWServerConfig;
import com.fanwe.lib.wwjsdk.socketio.WWSocket;
import com.fanwe.lib.wwjsdk.utils.WWJsonUtil;
import com.fanwe.lib.wwjsdk.utils.WWUtils;

import java.util.logging.Level;

/**
 * 娃娃机sdk模式管理类
 */
public class WWSDKModeManager
{
    private static WWSDKModeManager sInstance;

    private FLooper mLooper = new FSimpleLooper();
    private WWSocket mSocket = new WWSocket();
    private int mMode = Mode.OTHER;

    private WWSDKModeManager()
    {
    }

    public static WWSDKModeManager getInstance()
    {
        if (sInstance == null)
        {
            synchronized (WWSDKModeManager.class)
            {
                if (sInstance == null)
                {
                    sInstance = new WWSDKModeManager();
                }
            }
        }
        return sInstance;
    }

    /**
     * 返回当前的sdk模式
     *
     * @return {@link Mode}
     */
    public int getMode()
    {
        return mMode;
    }

    private void setMode(int mode)
    {
        if (mMode != mode)
        {
            mMode = mode;
            WWLogger.get().log(Level.WARNING, "sdk mode change to " + mode);
        }
    }

    /**
     * 开始监听
     */
    public void startMonitor()
    {
        mLooper.setInterval(30 * 1000);
        mLooper.start(new Runnable()
        {
            @Override
            public void run()
            {
                WWLogger.get().log(Level.INFO, "current sdk mode " + getMode() + " start check ----------");
                onMonitor();
            }
        });
    }

    private void onMonitor()
    {
        WWServerConfig config = WWServerConfig.get();
        if (config == null)
        {
            return;
        }
        final String url = config.initUrl;
        if (TextUtils.isEmpty(url))
        {
            return;
        }

        GetRequest request = new GetRequest();
        request.setUrl(url);
        request.getParams().put("mac", WWUtils.getMacAddress(0));
        request.execute(new ModelRequestCallback<InitActModel>()
        {
            @Override
            public void onSuccess()
            {
                WWLogger.get().log(Level.INFO, "request init result:" + getResult());
                if (getActModel() == null)
                {
                    WWLogger.get().log(Level.WARNING, "request init error: actModel is null");
                    return;
                }

                if (getActModel().getStatus() == 1)
                {
                    dealRequestResult(getActModel());
                } else
                {
                    WWLogger.get().log(Level.WARNING, "request init fail: error status");
                }
            }

            @Override
            public void onError(Exception e)
            {
                super.onError(e);
                WWLogger.get().log(Level.SEVERE, "request init error:" + e, e);
            }

            @Override
            protected InitActModel parseToModel(String content, Class<InitActModel> clazz)
            {
                return WWJsonUtil.jsonToObject(content, clazz);
            }
        });
    }

    private void dealRequestResult(InitActModel model)
    {
        final int requestMode = model.getType();
        WWLogger.get().log(Level.INFO, "request init success current mode " + getMode() + " request mode " + requestMode);
        setMode(requestMode);

        if (getMode() == Mode.FANWE)
        {
            String url = model.getSocket_address();
            if (!TextUtils.isEmpty(url))
            {
                mSocket.connect(url);
            } else
            {
                WWLogger.get().log(Level.SEVERE, "init param error:empty socket_address");
            }
        } else
        {
            mSocket.disconnect();
        }
    }

    public static final class Mode
    {
        public static final int OTHER = 0;
        public static final int FANWE = 1;
    }
}
