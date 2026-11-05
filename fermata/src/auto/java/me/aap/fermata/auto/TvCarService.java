package me.aap.fermata.auto;

/**
 * A separate car app, opening the Fermata UI with TV.
 */
public class TvCarService extends FullScreenCarService {

	@Override
	protected String getInitialAddon() {
		return "me.aap.fermata.addon.tv.TvAddon";
	}
}
