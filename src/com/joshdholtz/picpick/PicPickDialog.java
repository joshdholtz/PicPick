package com.joshdholtz.picpick;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import com.joshdholtz.picpick.R;

import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class PicPickDialog extends DialogFragment {

	private final static int RESULT_GALLERY = 1111;
	private final static int RESULT_CAMERA = 2222;
	private final static int RESULT_CAMERA_SAMSUNG = 3333;

	View view;

	Button btnChoose;
	Button btnCancel;
	Button btnRotate;
	Button btnDone;
	RelativeLayout viewImage;
	ImageView img;

	Bitmap bmp;

	PicPickDialogListener listener;


	/**
	 * @param listener the listener to set
	 */
	public void setListener(PicPickDialogListener listener) {
		this.listener = listener;
	}

	/**
	 * @param bmp the bmp to set
	 */
	public void setBmp(Bitmap bmp) {
		this.bmp = bmp;
	}

	/** The system calls this to get the DialogFragment's layout, regardless
	of whether it's being displayed as a dialog or an embedded fragment. */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(0));
		view = inflater.inflate(R.layout.dialog_picpick, container, false);

		btnChoose = (Button) view.findViewById(R.id.dialog_picpick_btn_chooser);
		btnCancel = (Button) view.findViewById(R.id.dialog_picpick_btn_cancel);
		btnRotate = (Button) view.findViewById(R.id.dialog_picpick_btn_rotate);
		btnDone = (Button) view.findViewById(R.id.dialog_picpick_btn_done);
		viewImage = (RelativeLayout) view.findViewById(R.id.dialog_picpick_view_image);
		img = (ImageView) view.findViewById(R.id.dialog_picpick_img);

		btnChoose.setOnClickListener(chooseOnClickListener);
		btnCancel.setOnClickListener(cancelOnClickListener);
		btnRotate.setOnClickListener(rotateOnClickListener);
		btnDone.setOnClickListener(doneOnClickListener);
		img.setOnClickListener(chooseOnClickListener);

		updateView();

		return view;
	}

	/** The system calls this only when creating the layout in a dialog. */
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		return dialog;
	}

	public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
		super.onActivityResult(requestCode, resultCode, imageReturnedIntent); 

		switch(requestCode) { 
		case RESULT_GALLERY:
			if (resultCode == Activity.RESULT_OK){  
				try {
					Uri selectedImage = imageReturnedIntent.getData();
					String[] filePathColumn = {MediaStore.Images.Media.DATA};

					Cursor cursor = getActivity().getContentResolver().query(
							selectedImage, filePathColumn, null, null, null);
					cursor.moveToFirst();

					int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
					String filePath = cursor.getString(columnIndex);
					cursor.close();

					bmp = BitmapFactory.decodeFile(filePath);

					if (filePath != null) {
						ExifInterface exif = null;
						try {
							exif = new ExifInterface(filePath);
						} catch (IOException e) {
							e.printStackTrace();
						}

						if (exif == null) {
							return;
						}

						int exifOrientation = exif.getAttributeInt(
								ExifInterface.TAG_ORIENTATION,
								ExifInterface.ORIENTATION_NORMAL);

						int rotate = 0;

						switch (exifOrientation) {
						case ExifInterface.ORIENTATION_ROTATE_90:
							rotate = 90;
							break; 

						case ExifInterface.ORIENTATION_ROTATE_180:
							rotate = 180;
							break;

						case ExifInterface.ORIENTATION_ROTATE_270:
							rotate = 270;
							break;
						}

						if (rotate != 0) {
							int w = bmp.getWidth();
							int h = bmp.getHeight();

							// Setting pre rotate
							Matrix mtx = new Matrix();
							mtx.preRotate(rotate);

							// Rotating Bitmap & convert to ARGB_8888, required by tess
							Bitmap theNewImage = Bitmap.createBitmap(bmp, 0, 0, w, h, mtx, false);
							theNewImage = theNewImage.copy(Bitmap.Config.ARGB_8888, true);

							if (theNewImage != null) {
								bmp = theNewImage;
							}
						}

					} else {
//						Log.d(Constants.LOG, "No output file");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
			break;
		case RESULT_CAMERA:
			if (resultCode == Activity.RESULT_OK){

				// Loads image from uri
				Uri u = null;
				if (hasImageCaptureBug()) {
					File fi = new File("/sdcard/tmp");
					try {
						u = Uri.parse(android.provider.MediaStore.Images.Media.insertImage(getActivity().getContentResolver(), fi.getAbsolutePath(), null, null));
						if (!fi.delete()) {
//	                         Log.i("logMarker", "Failed to delete " + fi);
						}
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
				} else {
					u = imageReturnedIntent.getData();
				}

				// Loads from data
				if (u != null) {
					try {
						bmp = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), u);
					} catch (Exception e) { e.printStackTrace(); }
				} else {
					bmp = (Bitmap) imageReturnedIntent.getExtras().get("data");
				}

				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = 4;

			} else if (resultCode == Activity.RESULT_CANCELED) {
				Toast.makeText(getActivity(), "HERE SOME ERROR", Toast.LENGTH_SHORT).show();
			}
			break;
		case RESULT_CAMERA_SAMSUNG:
			Bundle extras = imageReturnedIntent.getExtras();
            if (extras.keySet().contains("data") ){
                BitmapFactory.Options options = new BitmapFactory.Options();
                bmp = (Bitmap) extras.get("data");
                if (bmp != null) {
//                    BitmapFactory.Options opt = new BitmapFactory.Options();
//                    bmp = (Bitmap) extras.get("data");
                }
            } else {
                Uri imageURI = getActivity().getIntent().getData();
                ContentResolver cr = getActivity().getContentResolver();
                InputStream in;
				try {
					in = cr.openInputStream(imageURI);
					BitmapFactory.Options options = new BitmapFactory.Options();
	                options.inSampleSize=8;
	                bmp = BitmapFactory.decodeStream(in,null,options);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
			break;
		}

		updateView();
	}

	private void updateView() {
		if (bmp == null) {
			btnChoose.setVisibility(View.VISIBLE);
			viewImage.setVisibility(View.GONE);
		} else {
			btnChoose.setVisibility(View.GONE);
			viewImage.setVisibility(View.VISIBLE);
			img.setImageBitmap(bmp);
		}
	}

	final View.OnClickListener chooseOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			CharSequence cs[] = { "Gallery", "Camera" };

			AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
			alertBuilder.setTitle("Options");
			alertBuilder.setSingleChoiceItems(cs, -1, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					if (item == 0) {
						Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
						startActivityForResult(i, RESULT_GALLERY);
					} else if (item == 1) {
						String BX1 =  android.os.Build.MANUFACTURER;
						
						Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
						if (hasImageCaptureBug()) {
							cameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File("/sdcard/tmp")));
						} else {
							//	                    	cameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
						}
						
						startActivityForResult(cameraIntent, ( BX1.equalsIgnoreCase("samsung") ? RESULT_CAMERA_SAMSUNG : RESULT_CAMERA));
					}
					dialog.dismiss();
				}
			});
			alertBuilder.show();
		}
	};
	
	final View.OnClickListener cancelOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (listener != null) {
				listener.picPickDialogFinished(PicPickDialog.this, null);
			}
		}
	};

	final View.OnClickListener rotateOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			bmp = fixOrientation(bmp, 90);
			updateView();
		}
	};

	final View.OnClickListener doneOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (listener != null) {
				listener.picPickDialogFinished(PicPickDialog.this, bmp);
			}
		}
	};

	public boolean hasImageCaptureBug() {

		// list of known devices that have the bug
		ArrayList<String> devices = new ArrayList<String>();
		devices.add("android-devphone1/dream_devphone/dream");
		devices.add("generic/sdk/generic");
		devices.add("vodafone/vfpioneer/sapphire");
		devices.add("tmobile/kila/dream");
		devices.add("verizon/voles/sholes");
		devices.add("google_ion/google_ion/sapphire");

		return devices.contains(android.os.Build.BRAND + "/" + android.os.Build.PRODUCT + "/"
				+ android.os.Build.DEVICE);

	}

	private Bitmap fixOrientation(Bitmap bitmap, int rotate) {
		if (rotate != 0) {
			int w = bitmap.getWidth();
			int h = bitmap.getHeight();

			// Setting pre rotate
			Matrix mtx = new Matrix();
			mtx.preRotate(rotate);

			// Rotating Bitmap & convert to ARGB_8888, required by tess
			Bitmap theNewImage = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
			theNewImage = theNewImage.copy(Bitmap.Config.ARGB_8888, true);

			if (theNewImage != null) {
				bitmap = theNewImage;
			}

			return bitmap;

		}

		return bitmap;
	}

	private String getLastImageId(){
		final String[] imageColumns = { MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA };
		final String imageOrderBy = MediaStore.Images.Media._ID+" DESC";
		Cursor imageCursor = getActivity().managedQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageColumns, null, null, imageOrderBy);
		if(imageCursor.moveToFirst()){
			int id = imageCursor.getInt(imageCursor.getColumnIndex(MediaStore.Images.Media._ID));
			String fullPath = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Images.Media.DATA));
			//			Log.d(Constants.LOG, "getLastImageId::id " + id);
			//			Log.d(Constants.LOG, "getLastImageId::path " + fullPath);
			imageCursor.close();
			return fullPath;
		}else{
			return null;
		}
	}

	public boolean equalImages(Bitmap bitmap1, Bitmap bitmap2) {
		ByteBuffer buffer1 = ByteBuffer.allocate(bitmap1.getHeight() * bitmap1.getRowBytes());
		bitmap1.copyPixelsToBuffer(buffer1);

		ByteBuffer buffer2 = ByteBuffer.allocate(bitmap2.getHeight() * bitmap2.getRowBytes());
		bitmap2.copyPixelsToBuffer(buffer2);

		return Arrays.equals(buffer1.array(), buffer2.array());
	}

	public static interface PicPickDialogListener {
		public void picPickDialogFinished(PicPickDialog dialog, Bitmap bitmap);
	}

}
