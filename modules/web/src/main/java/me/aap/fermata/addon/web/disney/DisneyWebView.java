package me.aap.fermata.addon.web.disney;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;

import java.util.Set;

import me.aap.fermata.addon.web.desktop.DesktopChromeWebView;

/**
 * Disney+ as desktop Chrome. The player lays the video out at the top of the tall window, so the
 * video is stretched to the player area, that covers the whole window.
 */
public class DisneyWebView extends DesktopChromeWebView {
	private static final String PLAYER_JS = """
			(function() {
			  var style = document.createElement('style');
			  style.textContent = '.btm-media-clients disney-web-player,' +
			    ' .btm-media-clients .btm-media-client,' +
			    ' .btm-media-clients .media-element-container { height: 100% !important; }' +
			    ' .btm-media-clients video { width: 100% !important; height: 100% !important;' +
			    ' object-fit: contain !important; }';
			  document.documentElement.appendChild(style);
			})();""";

	public DisneyWebView(Context context) {
		super(context);
	}

	public DisneyWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public DisneyWebView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@NonNull
	@Override
	protected Set<String> getSiteOrigins() {
		return Set.of("https://disneyplus.com", "https://*.disneyplus.com");
	}

	@NonNull
	@Override
	protected String getSiteScript() {
		return PLAYER_JS;
	}
}
