package com.mikescher.xamarinforms.resourcebuilder;

public class ThreadUtils {
	@SuppressWarnings("StatementWithEmptyBody")
	public static void safeSleep(int millis) {
		long s = System.currentTimeMillis();
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			while (System.currentTimeMillis() - s < millis) { /* */ }
		}
	}
}
