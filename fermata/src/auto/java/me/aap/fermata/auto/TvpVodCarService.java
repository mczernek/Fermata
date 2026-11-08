package me.aap.fermata.auto;

/**
 * A separate full screen (navigation) car app, opening the Fermata UI with TVP VOD.
 */
public class TvpVodCarService extends FullScreenCarService {

	@Override
	protected String getInitialAddon() {
		return "me.aap.fermata.addon.web.tvp.TvpVodAddon";
	}
}
