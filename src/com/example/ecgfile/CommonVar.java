package com.example.ecgfile;

import android.os.Environment;

public class CommonVar {
	//log ��ǩ
	public static String TAG = "Bluetooth";
    //�ⲿ�洢��Ŀ¼
    static String rootPath = Environment.getExternalStorageDirectory().getPath()+"/ECG/";
    //�ĵ������ļ�
    public static final String ecgFilePath = rootPath +"EcgFile/";
    //�ĵ����ݴ��ϴ��ļ�
    public static final String ecgFilePathTemp = rootPath +"EcgFileUpTemp";
    //15��
    public static final long PERIOD_15 = 15000;
    //10ms
    public static final int PERIOD_10MS = 10;
    //�ļ��ϴ�Ŀ¼
    public final static String URL_FileUpload = "http://weiwangzhan2014.duapp.com/index.php/ecg/uploadEcgData";
  
}
