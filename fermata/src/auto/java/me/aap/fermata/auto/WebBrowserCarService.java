package me.aap.fermata.auto;

/**
 * A separate car app, showing the web browser only.
 */
public class WebBrowserCarService extends FullScreenCarService {

	@Override
	protected String getDedicatedAddon() {
		return "me.aap.fermata.addon.web.WebBrowserAddon";
	}
}
