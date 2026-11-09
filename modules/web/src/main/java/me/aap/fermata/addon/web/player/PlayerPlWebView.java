package me.aap.fermata.addon.web.player;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;

import java.util.Set;

import me.aap.fermata.addon.web.desktop.DesktopChromeWebView;

/**
 * player.pl as desktop Chrome on Windows.
 */
public class PlayerPlWebView extends DesktopChromeWebView {

	public PlayerPlWebView(Context context) {
		super(context);
	}

	public PlayerPlWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PlayerPlWebView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@NonNull
	@Override
	protected Set<String> getSiteOrigins() {
		return Set.of("https://player.pl", "https://*.player.pl");
	}

	/**
	 * The site rejects Linux: "Change the browser".
	 */
	@Override
	protected boolean isWindowsIdentity() {
		return true;
	}
}
