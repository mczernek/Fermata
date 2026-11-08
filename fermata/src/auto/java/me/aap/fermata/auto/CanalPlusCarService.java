package me.aap.fermata.auto;

/**
 * A separate full screen (navigation) car app, opening the Fermata UI with CANAL+ online.
 */
public class CanalPlusCarService extends FullScreenCarService {

	@Override
	protected String getInitialAddon() {
		return "me.aap.fermata.addon.web.canal.CanalPlusAddon";
	}
}
