package me.aap.fermata.auto;

/**
 * A separate full screen (navigation) car app, opening the Fermata UI with Prime Video.
 */
public class PrimeVideoCarService extends FullScreenCarService {

	@Override
	protected String getInitialAddon() {
		return "me.aap.fermata.addon.web.prime.PrimeVideoAddon";
	}
}
