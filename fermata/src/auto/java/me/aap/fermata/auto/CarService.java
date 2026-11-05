package me.aap.fermata.auto;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarActivityService;

import me.aap.fermata.media.service.FermataMediaServiceConnection;
import me.aap.fermata.media.service.MediaSessionCallback;
import me.aap.utils.log.Log;

/**
 * The projection based car app, shown by Android Auto in a part of the split screen.
 * <p>
 * Subclasses are shown as separate car apps, opening the UI with another initial screen, e.g.
 * YouTube. They share the connection to the media service.
 *
 * @author Andrey Pavlenko
 */
public class CarService extends CarActivityService {
	private static int instances;

	@Override
	public Class<? extends CarActivity> getCarActivity() {
		return MainCarActivity.class;
	}

	@Override
	public void onCreate() {
		Log.d("Creating CarService: " + this);
		instances++;
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		Log.d("Destroying CarService: " + this);
		// The connection is shared by all the projection based car apps
		if ((--instances == 0) && (MainCarActivity.service != null)) {
			FermataMediaServiceConnection s = MainCarActivity.service;
			MainCarActivity.service = null;
			MediaSessionCallback cb = s.getMediaSessionCallback();
			if ((cb != null) && cb.isPlaying()) cb.onPause();
			s.disconnect();
		}
		super.onDestroy();
	}
}
