package com.joshdholtz.picpick.test;

import com.joshdholtz.picpick.PicPickDialog;
import com.joshdholtz.picpick.PicPickDialog.PicPickDialogListener;
import com.joshdholtz.picpick.R;

import android.os.Bundle;
import android.app.Activity;
import android.graphics.Bitmap;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends FragmentActivity {

	ImageView imageView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		imageView = (ImageView) this.findViewById(R.id.image_view);
		
//		TextView txtMakeModel = (TextView) this.findViewById(R.id.makemodel);
//		txtMakeModel.setText(android.os.Build.BRAND + "/" + android.os.Build.PRODUCT + "/"
//				+ android.os.Build.DEVICE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public void onClickOpenDialog(View view) {
		FragmentManager fragmentManager = getSupportFragmentManager();
	    PicPickDialog newFragment = new PicPickDialog();
	    newFragment.setListener(new PicPickDialogListener() {

			@Override
			public void picPickDialogFinished(PicPickDialog dialog, Bitmap bitmap) {
				imageView.setImageBitmap(bitmap);
				dialog.dismiss();
			}
	    	
	    });
	    newFragment.show(fragmentManager, "dialog");
	    
	}
	
}
