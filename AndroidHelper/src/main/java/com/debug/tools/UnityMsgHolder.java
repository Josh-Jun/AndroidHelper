package com.debug.tools;

import java.util.ArrayList;
import java.util.List;

public class UnityMsgHolder {
    private final List<UnityMsgListener> listeners = new ArrayList<>();

    // 添加监听器
    public void addListener(UnityMsgListener listener) {
        listeners.add(listener);
    }

    // 移除监听器
    public void removeListener(UnityMsgListener listener) {
        listeners.remove(listener);
    }

    // 设置数据并通知监听器
    public void execute(String data) {
        for (UnityMsgListener listener : listeners) {
            listener.onReceive(data);
        }
    }
}
