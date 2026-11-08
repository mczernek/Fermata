package me.aap.fermata.addon.web.tvp;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;

import java.util.Set;

import me.aap.fermata.addon.web.desktop.DesktopChromeWebView;

/**
 * TVP VOD as desktop Chrome.
 */
public class TvpVodWebView extends DesktopChromeWebView {

	public TvpVodWebView(Context context) {
		super(context);
	}

	public TvpVodWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public TvpVodWebView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@NonNull
	@Override
	protected Set<String> getSiteOrigins() {
		return Set.of("https://tvp.pl", "https://*.tvp.pl");
	}
}
