package com.lyz.scan;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import com.google.zxing.ResultPoint;

public final class ScanView extends View {

	private static final long ANIMATION_DELAY = 100L;
	private static final int OPAQUE = 0xFF;

	private final Paint paint;
	private Bitmap resultBitmap;
	private final int maskColor;
	private final int resultColor;
	private final int frameColor;
//	private final int resultPointColor;
//	private Collection<ResultPoint> possibleResultPoints;
//	private Collection<ResultPoint> lastPossibleResultPoints;
	private Drawer drawScanLaser;
	private Drawer drawScanMask;
	
	private Rect frame;

	// This constructor is used when the class is built from an XML resource.
	public ScanView(Context context, AttributeSet attrs) {
		super(context, attrs);
		frame = createFrameRect();
		// Initialize these once for performance rather than calling them every
		// time in onDraw().
		paint = new Paint();
		Resources resources = getResources();
		maskColor = resources.getColor(R.color.scan_mask);
		resultColor = resources.getColor(R.color.result_view);
		frameColor = resources.getColor(R.color.scan_frame);
		
		drawScanLaser = new DrawScanLaser(resources, 2000);
		drawScanMask = new DrawScanMask(resources);
//		resultPointColor = resources.getColor(R.color.possible_result_points);
//		possibleResultPoints = new HashSet<ResultPoint>(5);
	}

	@Override
	public void onDraw(Canvas canvas) {
		drawScanMask.doDraw(canvas);
		if (resultBitmap != null) {
			// Draw the opaque result bitmap over the scanning rectangle
			paint.setAlpha(OPAQUE);
			canvas.drawBitmap(resultBitmap, frame.left, frame.top, paint);
		} else {
//			// Draw a two pixel solid black border inside the framing rect
			paint.setColor(frameColor);
			canvas.drawRect(frame.left, frame.top, frame.right + 1,
					frame.top + 2, paint);
			canvas.drawRect(frame.left, frame.top + 2, frame.left + 2,
					frame.bottom - 1, paint);
			canvas.drawRect(frame.right - 1, frame.top, frame.right + 1,
					frame.bottom - 1, paint);
			canvas.drawRect(frame.left, frame.bottom - 1, frame.right + 1,
					frame.bottom + 1, paint);

			// Draw a red "laser scanner" line through the middle to show
			// decoding is active
			drawScanLaser.doDraw(canvas);
			

//			Collection<ResultPoint> currentPossible = possibleResultPoints;
//			Collection<ResultPoint> currentLast = lastPossibleResultPoints;
//			if (currentPossible.isEmpty()) {
//				lastPossibleResultPoints = null;
//			} else {
//				possibleResultPoints = new HashSet<ResultPoint>(5);
//				lastPossibleResultPoints = currentPossible;
//				paint.setAlpha(OPAQUE);
//				paint.setColor(resultPointColor);
//				for (ResultPoint point : currentPossible) {
//					canvas.drawCircle(frame.left + point.getX(), frame.top
//							+ point.getY(), 6.0f, paint);
//				}
//			}
//			if (currentLast != null) {
//				paint.setAlpha(OPAQUE / 2);
//				paint.setColor(resultPointColor);
//				for (ResultPoint point : currentLast) {
//					canvas.drawCircle(frame.left + point.getX(), frame.top
//							+ point.getY(), 3.0f, paint);
//				}
//			}

			// Request another update at the animation interval, but only
			// repaint the laser line,
			// not the entire viewfinder mask.
			postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top,
					frame.right, frame.bottom);
		}
	}

	public void drawViewfinder() {
		resultBitmap = null;
		invalidate();
	}

	/**
	 * Draw a bitmap with the result points highlighted instead of the live
	 * scanning display.
	 * 
	 * @param barcode
	 *            An image of the decoded barcode.
	 */
	public void drawResultBitmap(Bitmap barcode) {
		resultBitmap = barcode;
		invalidate();
	}

	public void addPossibleResultPoint(ResultPoint point) {
//		possibleResultPoints.add(point);
	}
	
	
	 private static final int MIN_FRAME_WIDTH = 240;
	  private static final int MIN_FRAME_HEIGHT = 240;
	  private static final int MAX_FRAME_WIDTH = 480;
	  private static final int MAX_FRAME_HEIGHT = 360;
	private Rect createFrameRect() {
		DisplayMetrics display = getResources().getDisplayMetrics();
		int width = display.widthPixels * 3 / 4;
		int height = display.heightPixels * 3 / 4;
		if (width < MIN_FRAME_WIDTH) {
			width = MIN_FRAME_WIDTH;
		} else if (width > MAX_FRAME_WIDTH) {
			width = MAX_FRAME_WIDTH;
		}
		if (height < MIN_FRAME_HEIGHT) {
			height = MIN_FRAME_HEIGHT;
		} else if (height > MAX_FRAME_HEIGHT) {
			height = MAX_FRAME_HEIGHT;
		}
		int leftOffset = (display.widthPixels - width) / 2;
		int topOffset = (display.heightPixels - height) / 2;
		Rect framingRect = new Rect(leftOffset, topOffset, leftOffset + width,
				topOffset + height);
		return framingRect;
	}
	
	private abstract class Drawer{
		public abstract void doDraw(Canvas canvas);
	}
	
	/**
	 * 扫描极光线
	 */
	private final class DrawScanLaser extends Drawer{
		private final Paint paint = new Paint();
		private final int maxEachScanTime;
		private final int length;
		private long initialTime = -1;
		private DrawScanLaser(Resources resources, int maxEachScanTime) {
			paint.setColor(resources.getColor(R.color.scan_laser));
			this.length = frame.bottom - frame.top;
			this.maxEachScanTime = maxEachScanTime;
		}

		@Override
		public void doDraw(Canvas canvas) {
			long elapsedTime = adjustElapsedTime();
			float per = elapsedTime * 1.0f / maxEachScanTime;
			float top = frame.top +  length * per;
			canvas.drawRect(frame.left , top , frame.right,top + 3, paint);
		}

		private long adjustElapsedTime() {
			if(initialTime == -1){
				initialTime = SystemClock.elapsedRealtime();
			}
			long currentRealtime = SystemClock.elapsedRealtime();
			long elapsedTime = currentRealtime - initialTime;
			if(elapsedTime>maxEachScanTime){
				initialTime = -1;
			}
			if(elapsedTime==0){
				return 1;
			}
			return elapsedTime;
		}
	}
	
	/**
	 * 绘制遮罩
	 */
	private final class DrawScanMask extends Drawer {
		private final Paint paint = new Paint();
		private Path path = new Path();
		private DrawScanMask(Resources resources) {
			paint.setColor(resources.getColor(R.color.scan_mask));
			//FIXME 
			DisplayMetrics display = getResources().getDisplayMetrics();
			int width = display.widthPixels;
			int height = display.heightPixels;
			RectF rect = new RectF(0,0,width,height);
			path.addRect(new RectF(frame), Path.Direction.CCW);
			path.addRect(rect, Path.Direction.CW);
			path.close();
		}

		@Override
		public void doDraw(Canvas canvas) {
			canvas.drawPath(path, paint);
		}
		
	}
}
