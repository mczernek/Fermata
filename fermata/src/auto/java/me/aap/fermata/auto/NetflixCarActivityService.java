package me.aap.fermata.auto;

import com.google.android.apps.auto.sdk.CarActivity;

/**
 * A separate projection based car app, opening the Fermata UI with Netflix. Unlike
 * {@link NetflixCarService}, it is not a navigation app and is shown in the split screen.
 */
public class NetflixCarActivityService extends CarService {

	@Override
	public Class<? extends CarActivity> getCarActivity() {
		return Ui.class;
	}

	public static final class Ui extends MainCarActivity {

		@Override
		protected String getInitialAddon() {
			return "me.aap.fermata.addon.web.netflix.NetflixAddon";
		}
	}
}
