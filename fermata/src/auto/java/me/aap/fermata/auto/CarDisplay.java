package me.aap.fermata.auto;

import static android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY;
import static android.os.SystemClock.uptimeMillis;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;

import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.Surface;

import androidx.annotation.Nullable;

import java.util.Objects;

import me.aap.fermata.FermataApplication;
import me.aap.utils.function.Cancellable;
import me.aap.utils.log.Log;

/**
 * A private virtual display, rendering the Fermata UI ({@link CarDisplayHost}) to a car surface.
 * <p>
 * Android Auto gives navigation apps a surface, covering the whole car screen. Instead of
 * mirroring the phone screen (see {@link MirrorDisplay}), the Fermata UI is shown on a virtual
 * display, backed by this surface. The car touch events are delivered to the UI directly,
 * so neither screen capture nor accessibility/root based input injection is required.
 */
public final class CarDisplay {
	private static final long SCROLL_UP_DELAY = 300;
	private static CarDisplay instance;
	private final Rect insets = new Rect();
	@Nullable
	private VirtualDisplay vd;
	@Nullable
	private Surface surface;
	private int width;
	private int height;
	@Nullable
	private CarDisplayHost host;
	@Nullable
	private InputHost inputHost;
	@Nullable
	private String dedicatedAddon;
	@Nullable
	private CarInputEditText activeInput;
	private Cancellable scrollUp = Cancellable.CANCELED;
	private long scrollDownTime;
	private long scrollMoveTime;
	private float scrollX;
	private float scrollY;

	private CarDisplay() {
	}

	public static CarDisplay get() {
		if (instance == null) instance = new CarDisplay();
		return instance;
	}

	/**
	 * Receives the input requests, i.e. shows the car keyboard.
	 */
	public interface InputHost {
		void startInput(CarInputEditText et);

		void stopInput(CarInputEditText et);
	}

	/**
	 * Binds the display to a car session, that receives the input requests.
	 * <p>
	 * Each of the full screen car apps has its own session, but the display is shared. If the
	 * display shows the UI of another app, the UI is closed and recreated, when the new session
	 * provides the surface.
	 *
	 * @param dedicatedAddon the class name of the addon, the UI is restricted to, or null
	 */
	public void attach(InputHost session, @Nullable String dedicatedAddon) {
		if ((inputHost != session) || !Objects.equals(this.dedicatedAddon, dedicatedAddon)) close();
		inputHost = session;
		this.dedicatedAddon = dedicatedAddon;
	}

	/**
	 * Closes the display, unless it has already been taken over by another session.
	 */
	public void detach(InputHost session) {
		if (inputHost != session) return;
		inputHost = null;
		close();
	}

	public void setSurface(Surface surface, int width, int height, int dpi) {
		this.surface = surface;
		this.width = width;
		this.height = height;

		if (vd == null) {
			var dm = FermataApplication.get().getSystemService(DisplayManager.class);
			vd = dm.createVirtualDisplay("Fermata Car Display", width, height, dpi, surface,
					VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY);
			if (vd == null) {
				Log.e("Failed to create virtual display");
				return;
			}
			Log.i("Virtual display created: ", vd.getDisplay());
		} else {
			vd.resize(width, height, dpi);
			vd.setSurface(surface);
		}

		if (host == null) {
			host = new CarDisplayHost(vd.getDisplay(), dedicatedAddon);
			host.getWindow().getDecorView().post(() -> {
				if (host != null) host.setContentInsets(insets);
			});
		}
	}

	public void releaseSurface(Surface surface) {
		if (this.surface != surface) return;
		this.surface = null;
		if (vd != null) vd.setSurface(null);
	}

	/**
	 * The area of the surface, that is not covered by the car UI.
	 */
	public void setContentArea(Rect area) {
		if ((width == 0) || (height == 0)) return;
		insets.set(Math.max(0, area.left), Math.max(0, area.top), Math.max(0, width - area.right),
				Math.max(0, height - area.bottom));
		if (host != null) host.setContentInsets(insets);
	}

	public void close() {
		scrollUp.cancel();
		var h = host;
		host = null;
		if (h != null) h.destroy();
		if (vd != null) {
			vd.release();
			vd = null;
		}
		surface = null;
		activeInput = null;
	}

	public void home() {
		if (host != null) host.home();
	}

	public void back() {
		if (host != null) host.back();
	}

	public void click(float x, float y) {
		finishScroll();
		long time = uptimeMillis();
		if (!dispatch(time, time, ACTION_DOWN, x, y)) return;
		// Give the views a chance to show the pressed state
		FermataApplication.get().getHandler()
				.postDelayed(() -> dispatch(time, uptimeMillis(), ACTION_UP, x, y), 50);
	}

	/**
	 * Android Auto delivers the scroll distances, but not the touch position. Emulate a drag,
	 * starting at the center of the content.
	 */
	public void scroll(float distanceX, float distanceY) {
		long time = uptimeMillis();
		scrollUp.cancel();

		if (scrollDownTime == 0) {
			scrollX = insets.left + (width - insets.left - insets.right) / 2f;
			scrollY = insets.top + (height - insets.top - insets.bottom) / 2f;
			if (!dispatch(time, time, ACTION_DOWN, scrollX, scrollY)) return;
			scrollDownTime = time;
		}

		scrollMoveTime = time;
		scrollX -= distanceX;
		scrollY -= distanceY;
		dispatch(scrollDownTime, time, ACTION_MOVE, scrollX, scrollY);
		scrollUp = FermataApplication.get().getHandler().schedule(this::finishScroll, SCROLL_UP_DELAY);
	}

	public void fling(float velocityX, float velocityY) {
		if (scrollDownTime == 0) return;
		scrollUp.cancel();
		// A short, fast move right before the UP event makes the view fling with the same velocity
		long dt = 10;
		long time = Math.max(uptimeMillis(), scrollMoveTime + dt);
		scrollX += velocityX * dt / 1000f;
		scrollY += velocityY * dt / 1000f;
		dispatch(scrollDownTime, time, ACTION_MOVE, scrollX, scrollY);
		dispatch(scrollDownTime, time, ACTION_UP, scrollX, scrollY);
		scrollDownTime = 0;
	}

	private void finishScroll() {
		scrollUp.cancel();
		scrollUp = Cancellable.CANCELED;
		if (scrollDownTime == 0) return;
		dispatch(scrollDownTime, uptimeMillis(), ACTION_UP, scrollX, scrollY);
		scrollDownTime = 0;
	}

	void startInput(CarInputEditText et) {
		if (activeInput == et) return;
		var host = inputHost;
		if (host == null) return;
		activeInput = et;
		host.startInput(et);
	}

	void stopInput(CarInputEditText et) {
		if (activeInput != et) return;
		activeInput = null;
		var host = inputHost;
		if (host != null) host.stopInput(et);
	}

	/**
	 * Called by the input host, when the car keyboard is closed.
	 */
	public void inputFinished(CarInputEditText et) {
		if (activeInput == et) activeInput = null;
	}

	boolean isInputActive(CarInputEditText et) {
		return activeInput == et;
	}

	private boolean dispatch(long downTime, long eventTime, int action, float x, float y) {
		var h = host;
		if (h == null) return false;
		var e = MotionEvent.obtain(downTime, eventTime, action, x, y, 0);
		e.setSource(InputDevice.SOURCE_TOUCHSCREEN);
		try {
			return h.dispatchTouchEvent(e);
		} catch (Exception err) {
			Log.e(err, "Failed to dispatch event ", e);
			return false;
		} finally {
			e.recycle();
		}
	}
}
