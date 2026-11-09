package me.aap.fermata.auto;

import com.google.android.apps.auto.sdk.CarActivity;

/**
 * A separate projection based car app, opening the Fermata UI with player.pl. Unlike
 * {@link PlayerPlCarService}, it is not a navigation app and is shown in the split screen.
 */
public class PlayerPlCarActivityService extends CarService {

	@Override
	public Class<? extends CarActivity> getCarActivity() {
		return Ui.class;
	}

	public static final class Ui extends MainCarActivity {

		@Override
		protected String getInitialAddon() {
			return "me.aap.fermata.addon.web.player.PlayerPlAddon";
		}
	}
}
