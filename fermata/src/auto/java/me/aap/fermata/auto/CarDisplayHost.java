package me.aap.fermata.auto;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
import static android.view.WindowManager.LayoutParams.TYPE_PRIVATE_PRESENTATION;
import static me.aap.utils.async.Completed.completed;
import static me.aap.utils.async.Completed.failed;
import static me.aap.utils.ui.UiUtils.ID_NULL;

import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextWatcher;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView.OnEditorActionListener;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.FragmentController;
import androidx.fragment.app.FragmentHostCallback;
import androidx.fragment.app.FragmentManager;

import me.aap.fermata.FermataApplication;
import me.aap.fermata.addon.AddonManager;
import me.aap.fermata.addon.FermataFragmentAddon;
import me.aap.fermata.media.service.FermataMediaServiceConnection;
import me.aap.fermata.ui.activity.FermataActivity;
import me.aap.fermata.ui.activity.MainActivity;
import me.aap.fermata.ui.activity.MainActivityDelegate;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.function.Supplier;
import me.aap.utils.log.Log;

/**
 * Hosts the Fermata UI on the {@link CarDisplay} virtual display.
 * <p>
 * Like {@link MainCarActivity}, this is not a real activity - activities can't be started on an
 * untrusted virtual display. The UI is shown in a private presentation window instead and the
 * fragments are hosted by a {@link FragmentController}.
 */
final class CarDisplayHost extends ContextThemeWrapper implements FermataActivity {
	private static FermataMediaServiceConnection service;
	private final FragmentController fragments;
	private final Dialog window;
	@NonNull
	@SuppressWarnings("unchecked")
	private FutureSupplier<MainActivityDelegate> delegate =
			(FutureSupplier<MainActivityDelegate>) NO_DELEGATE;
	@Nullable
	private CarInputEditText input;
	@IdRes
	private int initialFragmentId;
	private boolean destroyed;

	/**
	 * @param initialAddon the class name of the addon to show on start, or null
	 */
	CarDisplayHost(Display display, @Nullable String initialAddon) {
		super(MainActivityDelegate.attachBaseContext(
				FermataApplication.get().createDisplayContext(display)), 0);
		MainActivityDelegate.setTheme(this, true);
		fragments = FragmentController.createController(new Host(this));
		fragments.attachHost(null);

		window = new Dialog(this, getThemeResId()) {
			@Override
			public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
				var d = delegate.peek();
				if ((d != null) && (event.getKeyCode() == KeyEvent.KEYCODE_BACK)) {
					if (event.getAction() == KeyEvent.ACTION_UP) d.onBackPressed();
					return true;
				}
				return super.dispatchKeyEvent(event);
			}
		};
		var w = window.getWindow();
		assert w != null;
		w.requestFeature(Window.FEATURE_NO_TITLE);
		w.setType(TYPE_PRIVATE_PRESENTATION);
		w.addFlags(FLAG_LAYOUT_IN_SCREEN | FLAG_KEEP_SCREEN_ON | FLAG_SHOW_WHEN_LOCKED);
		w.setLayout(MATCH_PARENT, MATCH_PARENT);
		w.setBackgroundDrawable(null);
		window.setCancelable(false);

		fragments.dispatchCreate();
		// Completes immediately, if the service is connected and the addon is loaded
		delegate = getFragmentId(initialAddon).then(id -> {
			initialFragmentId = id;
			return connect();
		}).main().map(this::onCreate)
				.onFailure(err -> Log.e(err, "Failed to create the car display UI"));
	}

	/**
	 * The window has the exact size of the car display. With MATCH_PARENT it got the size of the
	 * phone screen, when the full screen flags were applied (e.g. playing a video), and the video
	 * was stretched.
	 */
	void setWindowSize(int width, int height) {
		var w = window.getWindow();
		if (w != null) w.setLayout(width, height);
	}

	static FutureSupplier<Integer> getFragmentId(@Nullable String addon) {
		if (addon == null) return completed(ID_NULL);
		// Re-enables the addon, if it has been disabled in the settings
		return AddonManager.get().getOrInstallAddon(addon).main()
				.map(a -> ((FermataFragmentAddon) a).getFragmentId());
	}

	private FutureSupplier<FermataMediaServiceConnection> connect() {
		FermataMediaServiceConnection s = service;
		if ((s != null) && s.isConnected()) return completed(s);
		return FermataMediaServiceConnection.connect(this).main().map(c -> service = c);
	}

	private MainActivityDelegate onCreate(FermataMediaServiceConnection c) {
		var d = new MainActivityDelegate(this, c.createBinder());
		delegate = completed(d);
		window.show();
		d.onActivityCreate(null);
		// Tapping the media notification on the phone must open the phone UI
		d.getMediaSessionCallback().getSession().setSessionActivity(PendingIntent.getActivity(this, 0,
				new Intent(this, MainActivity.class), FLAG_IMMUTABLE));
		fragments.dispatchActivityCreated();
		fragments.dispatchStart();
		fragments.dispatchResume();
		fragments.execPendingActions();
		d.onActivityResume();
		return d;
	}

	void destroy() {
		if (destroyed) return;
		destroyed = true;
		delegate.onSuccess(d -> {
			fragments.dispatchPause();
			fragments.dispatchStop();
			d.onActivityDestroy();
			fragments.dispatchDestroy();
		});
		delegate.cancel();
		window.dismiss();
		FermataMediaServiceConnection s = service;
		service = null;
		if (s != null) s.disconnect();
	}

	boolean dispatchTouchEvent(MotionEvent e) {
		return window.dispatchTouchEvent(e);
	}

	/**
	 * Keep the content out of the areas, covered by the car UI.
	 */
	void setContentInsets(Rect insets) {
		View content = window.findViewById(android.R.id.content);
		if (content != null) content.setPadding(insets.left, insets.top, insets.right, insets.bottom);
	}

	/**
	 * @param addon the class name of the addon to show, or null to keep the current screen
	 */
	void showAddon(@Nullable String addon) {
		if (addon == null) return;
		getFragmentId(addon).main().onCompletion((id, err) -> {
			if (err != null) Log.e(err, "Failed to show addon ", addon);
			else if (!destroyed) delegate.onSuccess(d -> {
				d.hideActiveMenu();
				d.showFragment(id);
			});
		});
	}

	void menu() {
		delegate.onSuccess(MainActivityDelegate::showNavMenu);
	}

	@NonNull
	@Override
	public FutureSupplier<MainActivityDelegate> getActivityDelegate() {
		return delegate;
	}

	@Override
	public boolean isCarActivity() {
		return true;
	}

	@Override
	public boolean isCarHomeRequired() {
		return true;
	}

	@Override
	public boolean isTouchDragSupported() {
		return false;
	}

	@Override
	public int getTopEndInsetDp() {
		// Android Auto draws the action strip (Menu) in the top end corner
		return 72;
	}

	@Override
	public int getInitialFragmentId() {
		return initialFragmentId;
	}

	@Override
	public void setRequestedOrientation(int requestedOrientation) {
		// The orientation is defined by the car screen
	}

	@Override
	public Window getWindow() {
		return window.getWindow();
	}

	@Override
	public View getCurrentFocus() {
		return window.getCurrentFocus();
	}

	@NonNull
	@Override
	public FragmentManager getSupportFragmentManager() {
		return fragments.getSupportFragmentManager();
	}

	@Override
	public void setContentView(int layoutResID) {
		window.setContentView(layoutResID);
	}

	@Override
	public <T extends View> T findViewById(int id) {
		return window.findViewById(id);
	}

	@Override
	public void recreate() {
	}

	@Override
	public void finish() {
		delegate.onSuccess(MainActivityDelegate::onActivityFinish);
		CarDisplay.get().close();
	}

	@Override
	public boolean isFinishing() {
		return destroyed;
	}

	@Override
	public void startActivity(Intent intent, @Nullable Bundle options) {
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		FermataApplication.get().startActivity(intent, options);
	}

	@Override
	public FutureSupplier<Intent> startActivityForResult(Supplier<Intent> intent) {
		return failed(new UnsupportedOperationException());
	}

	@Override
	public FutureSupplier<int[]> checkPermissions(String... perms) {
		// Permission requests can't be handled in the car
		return failed(new UnsupportedOperationException());
	}

	@Override
	public Intent getIntent() {
		return new Intent();
	}

	@Override
	public EditText createEditText(Context ctx) {
		return new CarInputEditText(ctx);
	}

	@Override
	public EditText startInput(TextWatcher w) {
		CarInputEditText et = new CarInputEditText(this);
		et.addTextChangedListener(w);
		if (w instanceof OnEditorActionListener l) et.setOnEditorActionListener(l);
		input = et;
		CarDisplay.get().startInput(et);
		return et;
	}

	@Override
	public void stopInput() {
		if (input == null) return;
		CarDisplay.get().stopInput(input);
		input = null;
	}

	@Override
	public boolean isInputActive() {
		return (input != null) && CarDisplay.get().isInputActive(input);
	}

	@Override
	public boolean setTextInput(String text) {
		if (!isInputActive()) return false;
		input.setText(text);
		input.submit();
		stopInput();
		return true;
	}

	private static final class Host extends FragmentHostCallback<CarDisplayHost> {
		private final CarDisplayHost host;

		Host(CarDisplayHost host) {
			super(host, new Handler(Looper.getMainLooper()), 0);
			this.host = host;
		}

		@Override
		public CarDisplayHost onGetHost() {
			return host;
		}

		@NonNull
		@Override
		public LayoutInflater onGetLayoutInflater() {
			return LayoutInflater.from(host).cloneInContext(host);
		}

		@Nullable
		@Override
		public View onFindViewById(int id) {
			return host.window.findViewById(id);
		}

		@Override
		public boolean onHasView() {
			return host.window.getWindow() != null;
		}
	}
}
