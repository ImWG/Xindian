package com.example.ecgfile;

import android.os.Environment;

public class CommonVar {
	//log 标签
	public static String TAG = "Bluetooth";
    //外部存储根目录
    static String rootPath = Environment.getExternalStorageDirectory().getPath()+"/ECG/";
    //心电数据文件
    public static final String ecgFilePath = rootPath +"EcgFile/";
    //心电数据待上传文件
    public static final String ecgFilePathTemp = rootPath +"EcgFileUpTemp";
    //15秒
    public static final long PERIOD_15 = 15000;
    //10ms
    public static final int PERIOD_10MS = 10;
    //文件上传目录
    public final static String URL_FileUpload = "http://weiwangzhan2014.duapp.com/index.php/ecg/uploadEcgData";
  
}
