package me.aap.fermata.auto;

/**
 * A separate car app, showing YouTube only.
 */
public class YoutubeCarService extends FullScreenCarService {

	@Override
	protected String getDedicatedAddon() {
		return "me.aap.fermata.addon.web.yt.YoutubeAddon";
	}
}
