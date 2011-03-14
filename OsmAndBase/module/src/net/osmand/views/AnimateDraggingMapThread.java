package net.osmand.views;

import net.osmand.LogUtil;
import net.osmand.osm.MapUtils;

import org.apache.commons.logging.Log;

import android.util.FloatMath;

/**
 * Thread for animated dragging.
 * Defines accelerator to stop dragging screen. 
 */
public class AnimateDraggingMapThread implements Runnable {
	
	protected static final Log log = LogUtil.getLog(AnimateDraggingMapThread.class);
	
	public interface AnimateDraggingCallback {
		
		public void dragTo(float curX, float curY, float newX, float newY, boolean notify);
		
		public void setLatLon(double latitude, double longitude, boolean notify);
		
		public void zoomTo(float zoom, boolean notify);
		
		public void rotateTo(float rotate);
		
		public float getRotate();
		
	}
	
	private boolean animateDrag = true;
	private float curX;
	private float curY;
	private float vx;
	private float vy;
	private float ax;
	private float ay;
	private byte dirX;
	private byte dirY;
	private final float  a = 0.0014f;
	
	private long time;
	private volatile boolean stopped;
	
	// 0 - zoom out, 1 - moving, 2 - zoom in
	private byte phaseOfMoving ;
	private int endZ;
	private byte dirZ;
	private int intZ;
	private byte dirIntZ;
	private float curZ;
	private int timeZEnd;
	private int timeZInt;
	private int timeMove;
	private float moveX;
	private float moveY;
	private double moveLat;
	private double moveLon;
	
	private volatile Thread currentThread = null;
	private AnimateDraggingCallback callback = null;
	private boolean notifyListener;

	private double targetLatitude = 0;
	private double targetLongitude = 0;
	private int targetZoom = 0;
	private float targetRotate = 0;
	
	@Override
	public void run() {
		currentThread = Thread.currentThread();
		try {
			boolean conditionToCountinue = true;
			while (!stopped && conditionToCountinue) {
				// calculate sleep
				long sleep = 0;
				if(animateDrag){
//					sleep = (long) (40d / (Math.max(vx, vy) + 0.45));
					sleep = 80;
				} else {
					sleep = 80;
				}
				Thread.sleep(sleep);
				long curT = System.currentTimeMillis();
				int dt = (int) (curT - time);
				float newX = animateDrag && vx > 0 ? curX + dirX * vx * dt : curX;
				float newY = animateDrag && vy > 0 ? curY + dirY * vy * dt : curY;
				
				float newZ = curZ;
				if(!animateDrag){
					if (phaseOfMoving == 0 || phaseOfMoving == 2) {
						byte dir = phaseOfMoving == 2 ? dirZ : dirIntZ;
						int time = phaseOfMoving == 2 ? timeZEnd : timeZInt;
						float end = phaseOfMoving == 2 ? endZ : intZ;
						if (time > 0) {
							newZ = newZ + dir * (float) dt / time;
						}
						if (dir > 0 == newZ > end) {
							newZ = end;
						}
					} else {
						if(timeMove > 0){
							newX = newX + moveX * (float) dt / timeMove;
							newY = newY + moveY * (float) dt / timeMove;
							
							if(moveX > 0 == newX > moveX){
								newX = moveX;
							}
							if(moveY > 0 == newY > moveY){
								newY = moveY;
							}
						}
					}
				}
				if (!stopped && callback != null) {
					if (animateDrag || phaseOfMoving == 1) {
						callback.dragTo(curX, curY, newX, newY, notifyListener);
					} else {
						callback.zoomTo(newZ, notifyListener);
					}
				}
				time = curT;
				if(animateDrag){
					vx -= ax * dt;
					vy -= ay * dt;
					curX = newX;
					curY = newY;
					conditionToCountinue = vx > 0.5 || vy > 0.5;
				} else {
					if(phaseOfMoving == 0){
						curZ = newZ;
						if(curZ == intZ){
							curX = 0;
							curY = 0;
							phaseOfMoving ++;
						}
					} else if(phaseOfMoving == 2){
						curZ = newZ;
						conditionToCountinue = curZ != endZ;
					} else  {
						curX = newX;
						curY = newY;
						if(curX == moveX && curY == moveY){
							phaseOfMoving ++;
							callback.setLatLon(moveLat, moveLon, notifyListener);
						}
					}
				}
			}
			if(curZ != ((int) Math.round(curZ))){
				if(Math.abs(curZ - endZ) > 3){
					callback.zoomTo(Math.round(curZ), notifyListener);
				} else {
					callback.zoomTo(endZ, notifyListener);
				}
			}
			//rotate after animation
			conditionToCountinue = true;
			while (conditionToCountinue && callback != null) {
				conditionToCountinue = false;
				float rotationDiff = targetRotate-callback.getRotate();
				if (Math.abs((rotationDiff+360)%360) < Math.abs((rotationDiff-360)%360)) {
					rotationDiff = (rotationDiff+360)%360;
				} else {
					rotationDiff = (rotationDiff-360)%360;
				}
				float absDiff = Math.abs(rotationDiff);
				if (absDiff > 0) {
					Thread.sleep(60);
					if (absDiff < 1) {
						callback.rotateTo(targetRotate);
					} else {
						conditionToCountinue = true;
						callback.rotateTo(((absDiff/10)*Math.signum(rotationDiff) + callback.getRotate())%360);
					}
				}
			}

		} catch (InterruptedException e) {
		}
		currentThread = null;
	}
	

	/**
	 * Stop dragging async
	 */
	public void stopAnimating(){
		stopped = true;
	}
	
	public boolean isAnimating(){
		return currentThread != null;
	}
	
	/**
	 * Stop dragging sync
	 */
	public void stopAnimatingSync(){
		// wait until current thread != null
		stopped = true;
		while(currentThread != null){
			try {
				currentThread.join();
			} catch (InterruptedException e) {
			}
		}
	}
	
	public void startZooming(int zoomStart, int zoomEnd){
		stopAnimatingSync();
		targetZoom = 0;
		this.notifyListener = false;
		if(zoomStart < zoomEnd){
			dirZ = 1;
		} else {
			dirZ = -1;
		}
		curZ = zoomStart;
		endZ = zoomEnd;
		timeZEnd = 600;
		phaseOfMoving = 2;
		animateDrag = false;
		time = System.currentTimeMillis();
		stopped = false;
		Thread thread = new Thread(this,"Animatable dragging"); //$NON-NLS-1$
		thread.start();
	}
	
	public void startMoving(double curLat, double curLon, double finalLat, double finalLon, int curZoom, int endZoom, int tileSize, float rotate, boolean notifyListener){
		stopAnimatingSync();
		targetLatitude = finalLat;
		targetLongitude = finalLon;
		targetZoom = endZoom;
		
		this.notifyListener = notifyListener;
		curZ = curZoom;
		intZ = curZoom;
		float mX = (float) ((MapUtils.getTileNumberX(intZ, curLon) - MapUtils.getTileNumberX(intZ, finalLon)) * tileSize);
		float mY = (float) ((MapUtils.getTileNumberY(intZ, curLat) - MapUtils.getTileNumberY(intZ, finalLat)) * tileSize);
		while (Math.abs(mX) + Math.abs(mY) > 1200 && intZ > 4) {
			intZ--;
			mX = (float) ((MapUtils.getTileNumberX(intZ, curLon) - MapUtils.getTileNumberX(intZ, finalLon)) * tileSize);
			mY = (float) ((MapUtils.getTileNumberY(intZ, curLat) - MapUtils.getTileNumberY(intZ, finalLat)) * tileSize);
		}
		float rad = (float) Math.toRadians(rotate);
		moveX = FloatMath.cos(rad) * mX - FloatMath.sin(rad) * mY; 
		moveY = FloatMath.sin(rad) * mX + FloatMath.cos(rad) * mY;
		moveLat = finalLat;
		moveLon = finalLon;
		if(curZoom < intZ){
			dirIntZ = 1;
		} else {
			dirIntZ = -1;
		}
		
		if(intZ < endZoom){
			dirZ = 1;
		} else {
			dirZ = -1;
		}
		
		endZ = endZoom;
		
//		timeZInt = Math.abs(curZoom - intZ) * 300;
//		if (timeZInt > 900) {
//			
//		}
		timeZInt = 600;
		timeZEnd = 500;
		timeMove = (int) (Math.abs(moveX) + Math.abs(moveY) * 3);
		if(timeMove > 2200){
			timeMove = 2200;
		}
		animateDrag = false;
		phaseOfMoving = (byte) (intZ == curZoom ? 1 : 0);
		curX = 0;
		curY = 0;

		
		time = System.currentTimeMillis();
		stopped = false;
		Thread thread = new Thread(this,"Animatable dragging"); //$NON-NLS-1$
		thread.start();
	}
	
	public void startDragging(float velocityX, float velocityY, float  startX, float  startY, float  endX, float  endY){
		stopAnimatingSync();
		targetZoom = 0;
		this.notifyListener = true;
		vx = velocityX;
		vy = velocityY;
		dirX = (byte) (endX > startX ? 1 : -1);
		dirY = (byte) (endY > startY ? 1 : -1);
		animateDrag = true;
		ax = vx * a;
		ay = vy * a;
		time = System.currentTimeMillis();
		stopped = false;
		Thread thread = new Thread(this,"Animatable dragging"); //$NON-NLS-1$
		thread.start();
	}
	
	public void startRotate(float rotate)
	{
		this.targetRotate = rotate;
		if (!isAnimating()) {
			//stopped = false;
			//do we need to kill and recreate the thread? wait would be enough as now it
			//also handles the rotation?
			Thread thread = new Thread(this,"Animatable dragging"); //$NON-NLS-1$
			thread.start();			
		}
	}
	
	public int getTargetZoom() {
		return targetZoom;
	}
	
	public double getTargetLatitude() {
		return targetLatitude;
	}
	
	public double getTargetLongitude() {
		return targetLongitude;
	}
	
	public AnimateDraggingCallback getCallback() {
		return callback;
	}
	
	public void setCallback(AnimateDraggingCallback callback) {
		this.callback = callback;
	}
}

