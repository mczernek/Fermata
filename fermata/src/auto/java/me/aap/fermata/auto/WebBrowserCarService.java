package me.aap.fermata.auto;

/**
 * A separate car app, opening the Fermata UI with the web browser.
 */
public class WebBrowserCarService extends FullScreenCarService {

	@Override
	protected String getInitialAddon() {
		return "me.aap.fermata.addon.web.WebBrowserAddon";
	}
}
