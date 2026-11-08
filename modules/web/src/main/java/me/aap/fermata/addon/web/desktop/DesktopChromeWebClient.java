package me.aap.fermata.addon.web.desktop;

import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import me.aap.fermata.addon.web.FermataWebClient;

/**
 * Switches the identity of {@link DesktopChromeWebView} before a page is loaded: the User-Agent
 * of the request is fixed, when the navigation starts, so the navigation is restarted with the
 * new identity.
 */
public class DesktopChromeWebClient extends FermataWebClient {

	@Override
	public boolean shouldOverrideUrlLoading(@NonNull WebView view,
																					@NonNull WebResourceRequest request) {
		if (request.isForMainFrame() && "GET".equals(request.getMethod()) &&
				(view instanceof DesktopChromeWebView v) && v.updateIdentity(request.getUrl())) {
			v.loadUrl(request.getUrl().toString());
			return true;
		}
		return super.shouldOverrideUrlLoading(view, request);
	}
}
