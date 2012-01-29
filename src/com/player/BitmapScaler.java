package com.player;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

class BitmapScaler {
	private static class Size {
		int sample;
		float scale;
	}

	private Bitmap scaled;

	BitmapScaler(Resources resources, int resId, int newWidth)
			throws IOException {
		Size size = getRoughSize(resources, resId, newWidth);
		roughScaleImage(resources, resId, size);
		scaleImage(newWidth);
	}

	BitmapScaler(File file, int newWidth) throws IOException {
		InputStream is = null;
		try {
			is = new FileInputStream(file);
			Size size = getRoughSize(is, newWidth);
			try {
				is = new FileInputStream(file);
				roughScaleImage(is, size);
				scaleImage(newWidth);
			} finally {
				is.close();
			}
		} finally {
			is.close();
		}
	}

	BitmapScaler(AssetManager manager, String assetName, int newWidth)
			throws IOException {
		InputStream is = null;
		try {
			is = manager.open(assetName);
			Size size = getRoughSize(is, newWidth);
			try {
				is = manager.open(assetName);
				roughScaleImage(is, size);
				scaleImage(newWidth);
			} finally {
				is.close();
			}
		} finally {
			is.close();
		}
	}

	Bitmap getScaled() {
		return scaled;
	}

	private void scaleImage(int newWidth) {
		int width = scaled.getWidth();
		int height = scaled.getHeight();

		float scaleWidth = ((float) newWidth) / width;
		float ratio = ((float) scaled.getWidth()) / newWidth;
		int newHeight = (int) (height / ratio);
		float scaleHeight = ((float) newHeight) / height;

		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);

		scaled = Bitmap.createBitmap(scaled, 0, 0, width, height, matrix, true);
	}

	private void roughScaleImage(InputStream is, Size size) {
		Matrix matrix = new Matrix();
		matrix.postScale(size.scale, size.scale);

		BitmapFactory.Options scaledOpts = new BitmapFactory.Options();
		scaledOpts.inSampleSize = size.sample;
		scaled = BitmapFactory.decodeStream(is, null, scaledOpts);
	}

	private void roughScaleImage(Resources resources, int resId, Size size) {
		Matrix matrix = new Matrix();
		matrix.postScale(size.scale, size.scale);

		BitmapFactory.Options scaledOpts = new BitmapFactory.Options();
		scaledOpts.inSampleSize = size.sample;
		scaled = BitmapFactory.decodeResource(resources, resId, scaledOpts);
	}

	private Size getRoughSize(InputStream is, int newWidth) {
		BitmapFactory.Options o = new BitmapFactory.Options();
		o.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(is, null, o);

		Size size = getRoughSize(o.outWidth, o.outHeight, newWidth);
		return size;
	}

	private Size getRoughSize(Resources resources, int resId, int newWidth) {
		BitmapFactory.Options o = new BitmapFactory.Options();
		o.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(resources, resId, o);

		Size size = getRoughSize(o.outWidth, o.outHeight, newWidth);
		return size;
	}

	private Size getRoughSize(int outWidth, int outHeight, int newWidth) {
		Size size = new Size();
		size.scale = outWidth / newWidth;
		size.sample = 1;

		int width = outWidth;
		int height = outHeight;

		int newHeight = (int) (outHeight / size.scale);

		while (true) {
			if (width / 2 < newWidth || height / 2 < newHeight) {
				break;
			}
			width /= 2;
			height /= 2;
			size.sample *= 2;
		}
		return size;
	}
}
