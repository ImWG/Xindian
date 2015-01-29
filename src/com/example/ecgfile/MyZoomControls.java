package com.example.ecgfile;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ZoomControls;

public class MyZoomControls {
	/*	
	private ZoomControls zc;
	
	public ZoomControls v(){
		return zc;
	}

	public MyZoomControls(ZoomControls zc) {
		this.zc = zc;
	}

	@SuppressLint("NewApi")
	public void convert() {
        View v0 = zc.getChildAt(0);
		v0.setBackground(zc.getResources().getDrawable(R.drawable.zoomout));
		View v1 = zc.getChildAt(1);
		v1.setBackground(zc.getResources().getDrawable(R.drawable.zoomin));
	}

	public void setOnZoomInClickListener(OnClickListener onClickListener) {
		zc.setOnZoomInClickListener(onClickListener);
	}
	public void setOnZoomOutClickListener(OnClickListener onClickListener) {
		zc.setOnZoomOutClickListener(onClickListener);
	}

	public void setIsZoomInEnabled(boolean b) {
		zc.setIsZoomInEnabled(b);
	}
	public void setIsZoomOutEnabled(boolean b) {
		zc.setIsZoomOutEnabled(b);
	}
	*/
	
	@SuppressLint("NewApi")
	static public void convert(ZoomControls zc) {
        View v0 = zc.getChildAt(0);
		v0.setBackground(zc.getResources().getDrawable(R.drawable.zoomout));
		View v1 = zc.getChildAt(1);
		v1.setBackground(zc.getResources().getDrawable(R.drawable.zoomin));
	}
}
