package me.aap.fermata.addon.web.prime;

import android.content.Context;
import android.util.AttributeSet;

import me.aap.fermata.addon.web.FermataWebView;

/**
 * Prime Video in the plain web view, as mobile Chrome (the addon's User-Agent), like the web browser.
 * The mobile site plays the videos, no desktop identity is needed. The desktop version can be
 * enabled in the menu.
 */
public class PrimeVideoWebView extends FermataWebView {

	public PrimeVideoWebView(Context context) {
		super(context);
	}

	public PrimeVideoWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PrimeVideoWebView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected boolean isAddonUserAgent() {
		return true;
	}

	@Override
	protected void pageLoaded(String uri) {
		// Do not replace the last URL of the web browser
		addFocusHighlight();
		android.webkit.CookieManager.getInstance().flush();
	}
}
