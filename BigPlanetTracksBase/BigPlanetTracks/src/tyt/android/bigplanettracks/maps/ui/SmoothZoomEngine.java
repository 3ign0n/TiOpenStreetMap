package tyt.android.bigplanettracks.maps.ui;

import java.util.LinkedList;

import tyt.android.bigplanettracks.maps.AbstractCommand;
import tyt.android.bigplanettracks.maps.PhysicMap;
//import java.util.concurrent.Semaphore;

public class SmoothZoomEngine {

	public static SmoothZoomEngine sze;
	
	private LinkedList<Integer> scaleQueue = new LinkedList<Integer>();

	private Float scaleFactor = 1000f;

	private AbstractCommand updateScreen;

	private AbstractCommand reloadMap;
	
	public static boolean stop = false;

	public static SmoothZoomEngine getInstance() {
		if (sze == null) {
			sze = new SmoothZoomEngine();
		}
		return sze;
	}

	public void setUpdateScreenCommand(final AbstractCommand updateScreen) {
		this.updateScreen = updateScreen;
	}

	public void setReloadMapCommand(final AbstractCommand reloadMap) {
		this.reloadMap = reloadMap;
	}

	public void nullScaleFactor() {
		synchronized (scaleFactor) {
			scaleFactor = 1000f;
		}
	}

	private SmoothZoomEngine() {
//		System.out.println("create queue");
		createQueue();
	}
	
	private void createQueue() {
		new Thread("SmoothZoomEngine") {
			
			@Override
			public void run() {
				boolean isEmpty = true;
				if (updateScreen != null) {
					updateScreen.execute();
				}
				double endScaleFactor;
				while (true) {
					if (scaleQueue.size() > 0) {
//						System.out.println("scaleQueue=" + scaleQueue);
						isEmpty = false;
						int scaleDirection = scaleQueue.removeFirst();
						endScaleFactor = (scaleDirection == -1 
											? scaleFactor / 2 
											: scaleFactor * 2);
						int z = PhysicMap.getZoomLevel();
						if ((scaleDirection == -1 && z < 17) || (scaleDirection == 1 && z > -2)) {
							if (!(endScaleFactor > 8000 || endScaleFactor < 125)) {
//								System.out.println("smooth scaling");
								synchronized (sze) {
									synchronized (scaleFactor) {
										do {
											try {
												Thread.sleep(5);
												scaleFactor = scaleFactor + (scaleDirection) * 25;
												updateScreen.execute(new Float(scaleFactor / 1000));
											} catch (Exception e) {
												e.printStackTrace();
											}
										} while (!(scaleFactor == (endScaleFactor)));
									}
								}
							}
						}

						if (!isEmpty && scaleQueue.size() == 0) {
//							System.out.println("reload");
							isEmpty = true;
							try {
//								Thread.sleep(5);
								reloadMap.execute(new Float(scaleFactor / 1000));
								// semaphore.release();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}

					} // end [ if (scaleQueue.size() > 0) ]

					// avoid Thread still running while quitting the application
					if (stop) {
						break;
					}
					// avoid busy waiting
					try {
						Thread.sleep(5);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				} // end while
			} // end run()

		}.start();
	}

	public void addToScaleQ(int direction) {
		synchronized (scaleQueue) {
//			System.out.println("add to scale " + direction);
			scaleQueue.addLast(direction);
		}
	}

}
