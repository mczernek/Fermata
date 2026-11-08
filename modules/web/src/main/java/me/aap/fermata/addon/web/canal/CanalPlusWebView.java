package me.aap.fermata.addon.web.canal;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;

import androidx.annotation.NonNull;

import java.util.Set;

import me.aap.fermata.addon.web.desktop.DesktopChromeWebView;

/**
 * CANAL+ online as desktop Chrome. The bot protection of the login pages blocks the sign-in of
 * desktop Chrome on a phone, so they are loaded as mobile Chrome.
 */
public class CanalPlusWebView extends DesktopChromeWebView {

	public CanalPlusWebView(Context context) {
		super(context);
	}

	public CanalPlusWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CanalPlusWebView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@NonNull
	@Override
	protected Set<String> getSiteOrigins() {
		return Set.of("https://canalplus.com", "https://*.canalplus.com", "https://canal-plus.com",
				"https://*.canal-plus.com");
	}

	@Override
	protected boolean isMobileIdentityPage(@NonNull Uri uri) {
		String host = uri.getHost();
		return (host != null) &&
				(host.equals("pass.canalplus.com") || host.equals("pass.canal-plus.com"));
	}
}
