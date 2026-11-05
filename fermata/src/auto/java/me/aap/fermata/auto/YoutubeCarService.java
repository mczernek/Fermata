package me.aap.fermata.auto;

/**
 * A separate car app, opening the Fermata UI with YouTube.
 */
public class YoutubeCarService extends FullScreenCarService {

	@Override
	protected String getInitialAddon() {
		return "me.aap.fermata.addon.web.yt.YoutubeAddon";
	}
}
