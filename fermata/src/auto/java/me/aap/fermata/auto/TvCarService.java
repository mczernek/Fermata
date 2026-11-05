package me.aap.fermata.auto;

/**
 * A separate car app, showing TV only.
 */
public class TvCarService extends FullScreenCarService {

	@Override
	protected String getDedicatedAddon() {
		return "me.aap.fermata.addon.tv.TvAddon";
	}
}
