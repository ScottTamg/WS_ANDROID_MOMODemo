package com.wushuangtech.videocore;

import android.app.Activity;
import android.content.Context;
import android.view.SurfaceView;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RemotePlayerManger {
    private static RemotePlayerManger remotePlayerManger = null;
    private ConcurrentHashMap<String, RemoteSurfaceView> mapView = new ConcurrentHashMap<>();

    private RemotePlayerManger() {
    }

    public SurfaceView createRemoteSurfaceView(Context mContext){
        return new RemoteSurfaceView(mContext);
    }

    SurfaceView getRemoteSurfaceView(Activity activity, String devID, int showMode) {
        RemoteSurfaceView view = mapView.get(devID);
        if (view == null) {
            view = new RemoteSurfaceView(activity, showMode);
            mapView.put(devID, view);
        }
        return view;
    }

    public RemoteSurfaceView getRemoteSurfaceView(String devID) {
        return mapView.get(devID);
    }

    public Map<String, RemoteSurfaceView> getAllRemoteViews(){
        return mapView;
    }

    public static RemotePlayerManger getInstance() {
        if (remotePlayerManger == null) {
            synchronized (RemotePlayerManger.class) {
                if (remotePlayerManger == null) {
                    remotePlayerManger = new RemotePlayerManger();
                }
            }
        }
        return remotePlayerManger;
    }

    int getDecFrameCount() {
        int totalCount = 0;
        for (Map.Entry<String, RemoteSurfaceView> entry : mapView.entrySet()) {
            RemoteSurfaceView view = entry.getValue();
            totalCount += view.getDecFrames();
        }
        return totalCount;
    }

    int getRenFrameCount() {
        int totalCount = 0;
        for (Map.Entry<String, RemoteSurfaceView> entry : mapView.entrySet()) {
            RemoteSurfaceView view = entry.getValue();
            totalCount += view.getRenFrames();
        }
        return totalCount;
    }

    String getDevId(RemoteSurfaceView surface) {
        for (Map.Entry<String, RemoteSurfaceView> entry : mapView.entrySet()) {
            if (entry.getValue().equals(surface)) {
                return entry.getKey();
            }
        }
        return null;
    }

    synchronized int getDisplayMode(String devID) {
        if (mapView.size() > 0) {
            Set<Map.Entry<String, RemoteSurfaceView>> entries = mapView.entrySet();
            for (Map.Entry<String, RemoteSurfaceView> next : entries) {
                if (next.getKey().equals(devID)) {
                    return next.getValue().getDisplayMode();
                }
            }
        }
        return -2;
    }

    synchronized void setDisplayMode(String devID , int showMode) {
        if (mapView.size() > 0) {
            Set<Map.Entry<String, RemoteSurfaceView>> entries = mapView.entrySet();
            for (Map.Entry<String, RemoteSurfaceView> next : entries) {
                if (next.getKey().equals(devID)) {
                    next.getValue().setDisplayMode(showMode);
                    break;
                }
            }
        }
    }

    void setViewIDMap(String devID , RemoteSurfaceView view) {
        RemoteSurfaceView tempView = mapView.get(devID);
        if (tempView != null) {
            mapView.remove(devID);
        }
        mapView.put(devID, view);
    }

    synchronized void adjustAllRemoteViewDisplay(boolean mIsDisplay){
        if (mapView.size() > 0) {
            Set<Map.Entry<String, RemoteSurfaceView>> entries = mapView.entrySet();
            for (Map.Entry<String, RemoteSurfaceView> next : entries) {
                if (mIsDisplay) {
                    next.getValue().openOrCloseVideoDevice(true);
                } else {
                    next.getValue().openOrCloseVideoDevice(false);
                }
            }
        }
    }
}
