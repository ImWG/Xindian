package com.example.ecgfile;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.http.ProtocolException;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.widget.Toast;

public class NetConnect {
    
    static Activity activity;
    // 获取手机所有连接管理对象（包括对wifi,net等连接的管理）
    public static boolean checkNet(Context context) { 
        try {
            ConnectivityManager connectivity = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivity != null) {
                // 获取网络连接管理的对象
                NetworkInfo info = connectivity.getActiveNetworkInfo();
                System.out.println("Net:" + info);
                if (info == null || !info.isAvailable()) {
                    Toast.makeText(context,"网络不可用",Toast.LENGTH_SHORT).show();
                    return false;
                } else {
//                    if(isNetAvailable()){
                        return true;
//                    }
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(context,"网络不可用",Toast.LENGTH_SHORT).show();
        return false;
    }
    
    //判断wifi是否打开,无则弹出设置框
    public static boolean checkWifi(final Activity activitiy) {
        WifiManager mWifiManager = (WifiManager) activitiy.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        int ipAddress = wifiInfo == null ? 0 : wifiInfo.getIpAddress();
        if (mWifiManager.isWifiEnabled() && ipAddress != 0) {
            return true;
        } else {
            new AlertDialog.Builder(activitiy)
            .setTitle("建议在wifi环境下使用")  
            .setIcon(android.R.drawable.ic_dialog_info)                  
            .setPositiveButton("好的", new DialogInterface.OnClickListener() {                   
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    activitiy.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                }
            })
            .setNegativeButton("不用了", null).show();           
            return false;    
        }
    }
    
    //判断是否真的能否连通网络（像CMCC这种需要填写用户名密码的网络，虽然wifi连接上了，但上不了网）
    public static boolean isNetAvailable() throws ProtocolException{
        HttpURLConnection conn = null;
        int responseCode = 0;
        try {
            URL url = new URL("HTTP://www.baidu.com/index.html");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Connection", "Close");
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(2000);
            conn.connect();
            responseCode = conn.getResponseCode();
            //返回的状态,如果有返回状态码，说明可以联网
            if (responseCode == 200 || responseCode == 206 || responseCode == 404) {
                return true;                    
            }
        }catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return false;
    }
}
