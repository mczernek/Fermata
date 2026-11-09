package me.aap.fermata.addon.web.prime;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;

import java.util.Set;

import me.aap.fermata.addon.web.desktop.DesktopChromeWebView;

/**
 * Prime Video as desktop Chrome on Windows. The sign-in pages are on the Amazon domains.
 */
public class PrimeVideoWebView extends DesktopChromeWebView {

	public PrimeVideoWebView(Context context) {
		super(context);
	}

	public PrimeVideoWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PrimeVideoWebView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@NonNull
	@Override
	protected Set<String> getSiteOrigins() {
		return Set.of("https://primevideo.com", "https://*.primevideo.com", "https://amazon.com",
				"https://*.amazon.com", "https://*.amazon.pl", "https://*.amazon.de",
				"https://*.amazon.co.uk");
	}

	/**
	 * Windows, like player.pl: the best supported desktop for the protected (Widevine) playback.
	 */
	@Override
	protected boolean isWindowsIdentity() {
		return true;
	}
}
