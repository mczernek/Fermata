package me.aap.fermata.auto;

import static androidx.core.graphics.drawable.IconCompat.createWithResource;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Rect;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.AppManager;
import androidx.car.app.CarAppService;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.ScreenManager;
import androidx.car.app.Session;
import androidx.car.app.SessionInfo;
import androidx.car.app.SurfaceCallback;
import androidx.car.app.SurfaceContainer;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.OnClickListener;
import androidx.car.app.model.SearchTemplate;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.car.app.validation.HostValidator;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import me.aap.fermata.R;
import me.aap.utils.log.Log;

/**
 * Shows the Fermata UI on the whole car screen.
 * <p>
 * Unlike the projection based {@link CarService}, that Android Auto shows in a part of the split
 * screen, navigation apps get a surface covering the whole screen. The UI is rendered to this
 * surface by {@link CarDisplay}.
 * <p>
 * Subclasses are shown as separate car apps, opening the same UI with another initial screen,
 * e.g. YouTube.
 */
public class FullScreenCarService extends CarAppService {

	/**
	 * The class name of the addon to show, when the app is opened, or null to keep the current
	 * screen.
	 */
	@Nullable
	protected String getInitialAddon() {
		return null;
	}

	@NonNull
	@Override
	public HostValidator createHostValidator() {
		return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
	}

	@NonNull
	@Override
	public Session onCreateSession(@NonNull SessionInfo sessionInfo) {
		return new FullScreenSession(new ComponentName(this, getClass()), getInitialAddon());
	}

	private static final class FullScreenSession extends Session
			implements CarDisplay.InputHost, DefaultLifecycleObserver {
		private final ComponentName service;
		@Nullable
		private final String initialAddon;

		FullScreenSession(ComponentName service, @Nullable String initialAddon) {
			this.service = service;
			this.initialAddon = initialAddon;
			getLifecycle().addObserver(this);
		}

		@NonNull
		@Override
		public Screen onCreateScreen(@NonNull Intent intent) {
			CarDisplay.get().attach(this, initialAddon);
			return new DisplayScreen(getCarContext(), service);
		}

		@Override
		public void onNewIntent(@NonNull Intent intent) {
			// The app is opened again, e.g. from the app list
			CarDisplay.get().attach(this, initialAddon);
		}

		@Override
		public void onDestroy(@NonNull LifecycleOwner owner) {
			Log.i("Full screen session destroyed: ", initialAddon);
			CarDisplay.get().detach(this);
		}

		@Override
		public void startInput(CarInputEditText et) {
			getCarContext().getCarService(ScreenManager.class)
					.push(new InputScreen(getCarContext(), et));
		}

		@Override
		public void stopInput(CarInputEditText et) {
			var sm = getCarContext().getCarService(ScreenManager.class);
			if ((sm.getTop() instanceof InputScreen s) && (s.input == et)) sm.pop();
		}
	}

	private static final class DisplayScreen extends Screen implements SurfaceCallback {
		private final CarDisplay display = CarDisplay.get();
		private final ComponentName service;
		private boolean split;

		DisplayScreen(@NonNull CarContext ctx, ComponentName service) {
			super(ctx);
			this.service = service;
			ctx.getCarService(AppManager.class).setSurfaceCallback(this);
			Log.i("Full screen created. API level: ", ctx.getCarAppApiLevel());
		}

		@Override
		public void onSurfaceAvailable(@NonNull SurfaceContainer sc) {
			if (sc.getSurface() == null) return;
			Log.i("Surface available: ", sc.getWidth(), "x", sc.getHeight(), ", dpi=", sc.getDpi());
			display.setSurface(sc.getSurface(), sc.getWidth(), sc.getHeight(), sc.getDpi());
		}

		@Override
		public void onSurfaceDestroyed(@NonNull SurfaceContainer sc) {
			if (sc.getSurface() != null) display.releaseSurface(sc.getSurface());
		}

		@Override
		public void onStableAreaChanged(@NonNull Rect stableArea) {
			Log.d("Stable area changed: ", stableArea);
			boolean split = display.setContentArea(stableArea);
			if (this.split == split) return;
			this.split = split;
			invalidate();
		}

		/**
		 * Starting the app, that is already shown in the split screen, brings it back to the full
		 * screen.
		 */
		private void fullScreen() {
			try {
				getCarContext().startCarApp(new Intent(Intent.ACTION_MAIN).setComponent(service));
			} catch (Exception err) {
				Log.e(err, "Failed to switch to the full screen");
			}
		}

		@Override
		public void onClick(float x, float y) {
			display.click(x, y);
		}

		@Override
		public void onScroll(float distanceX, float distanceY) {
			display.scroll(distanceX, distanceY);
		}

		@Override
		public void onFling(float velocityX, float velocityY) {
			display.fling(velocityX, velocityY);
		}

		@NonNull
		@Override
		public Template onGetTemplate() {
			var ctx = getCarContext();
			var actions = new ActionStrip.Builder();
			if (split) actions.addAction(action(ctx, R.drawable.car_fullscreen, this::fullScreen));
			actions.addAction(action(ctx, R.drawable.home, display::home))
					.addAction(action(ctx, R.drawable.back, display::back));
			return new NavigationTemplate.Builder().setActionStrip(actions.build())
					// Scroll events are only delivered to apps, supporting the pan mode
					.setMapActionStrip(new ActionStrip.Builder().addAction(Action.PAN).build())
					.build();
		}

		private static Action action(CarContext ctx, @DrawableRes int icon, OnClickListener l) {
			return new Action.Builder()
					.setIcon(new CarIcon.Builder(createWithResource(ctx, icon)).build())
					.setOnClickListener(l).build();
		}
	}

	/**
	 * Text input with the car keyboard.
	 */
	private static final class InputScreen extends Screen
			implements SearchTemplate.SearchCallback, DefaultLifecycleObserver {
		final CarInputEditText input;

		InputScreen(@NonNull CarContext ctx, CarInputEditText input) {
			super(ctx);
			this.input = input;
			getLifecycle().addObserver(this);
		}

		@NonNull
		@Override
		public Template onGetTemplate() {
			var text = input.getText();
			return new SearchTemplate.Builder(this).setHeaderAction(Action.BACK)
					.setInitialSearchText((text == null) ? "" : text.toString())
					.setShowKeyboardByDefault(true).build();
		}

		@Override
		public void onSearchTextChanged(@NonNull String searchText) {
			input.setText(searchText);
		}

		@Override
		public void onSearchSubmitted(@NonNull String searchText) {
			input.setText(searchText);
			input.submit();
			getScreenManager().pop();
		}

		@Override
		public void onDestroy(@NonNull LifecycleOwner owner) {
			CarDisplay.get().inputFinished(input);
		}
	}
}
