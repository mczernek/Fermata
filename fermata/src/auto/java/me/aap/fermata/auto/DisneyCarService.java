package me.aap.fermata.auto;

/**
 * A separate full screen (navigation) car app, opening the Fermata UI with Disney+.
 */
public class DisneyCarService extends FullScreenCarService {

	@Override
	protected String getInitialAddon() {
		return "me.aap.fermata.addon.web.disney.DisneyAddon";
	}
}
