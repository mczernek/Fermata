package me.aap.fermata.addon.web.disney;

import static androidx.webkit.WebViewFeature.DOCUMENT_START_SCRIPT;
import static androidx.webkit.WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST;
import static androidx.webkit.WebViewFeature.USER_AGENT_METADATA;
import static androidx.webkit.WebViewFeature.USER_AGENT_METADATA_FORM_FACTORS;
import static androidx.webkit.WebViewFeature.isFeatureSupported;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.CookieManager;
import android.webkit.WebSettings;

import androidx.webkit.UserAgentMetadata;
import androidx.webkit.UserAgentMetadata.BrandVersion;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewCompat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.aap.fermata.addon.web.FermataChromeClient;
import me.aap.fermata.addon.web.FermataWebClient;
import me.aap.fermata.addon.web.FermataWebView;
import me.aap.fermata.addon.web.WebBrowserAddon;
import me.aap.utils.log.Log;

/**
 * A web view, that is recognized by Disney+ as desktop Chrome, like Chrome's "Desktop site" mode:
 * <ul>
 *   <li>The reduced desktop Linux User-Agent with the Chrome version of the web view.</li>
 *   <li>The User-Agent client hints (Sec-CH-UA* headers and navigator.userAgentData): the Chrome
 *   brands instead of "Android WebView", not mobile, Linux.</li>
 *   <li>No X-Requested-With header, revealing the app package.</li>
 *   <li>navigator.platform, consistent with the User-Agent.</li>
 *   <li>Widevine without the options, the web view does not support.</li>
 *   <li>The video, stretched to the player area, that covers the whole window.</li>
 *   <li>The desktop layout viewport.</li>
 * </ul>
 * The video is protected by Widevine, the protected media permission is granted by
 * {@link FermataChromeClient}.
 */
public class DisneyWebView extends FermataWebView {
	private static final Pattern CHROME_VERSION = Pattern.compile(".* Chrome/((\\d+)[.\\d]*) .*");
	private static final String DEFAULT_CHROME_VERSION = "140.0.0.0";
	/**
	 * navigator.platform, consistent with the User-Agent, the video stretched to the player area
	 * and the Widevine fallback: the web view rejects some of the options, requested by the desktop
	 * players - the robustness levels (SW_SECURE_CRYPTO, SW_SECURE_DECODE...),
	 * distinctiveIdentifier "not-allowed" (the Android CDM always uses it) and persistent license
	 * sessions, but supports Widevine without them.
	 */
	private static final String DESKTOP_JS = """
			(function() {
			  try {
			    Object.defineProperty(Navigator.prototype, 'platform', {
			      get: function() { return 'Linux x86_64'; }, configurable: true
			    });
			  } catch (e) {}
			  // The player lays the video out at the top of the tall window, stretch it to the
			  // player area, which covers the whole window
			  var style = document.createElement('style');
			  style.textContent = '.btm-media-clients disney-web-player,' +
			    ' .btm-media-clients .btm-media-client,' +
			    ' .btm-media-clients .media-element-container { height: 100% !important; }' +
			    ' .btm-media-clients video { width: 100% !important; height: 100% !important;' +
			    ' object-fit: contain !important; }';
			  document.documentElement.appendChild(style);
			  var request = Navigator.prototype.requestMediaKeySystemAccess;
			  if (!request) return;
			  var noRobustness = function(caps) {
			    return caps && caps.map(function(c) { return Object.assign({}, c, {robustness: ''}); });
			  };
			  Navigator.prototype.requestMediaKeySystemAccess = function(keySystem, configs) {
			    var nav = this;
			    return request.call(nav, keySystem, configs).catch(function(err) {
			      if (!/widevine/.test(keySystem) || !Array.isArray(configs)) throw err;
			      return request.call(nav, keySystem, configs.map(function(c) {
			        var r = Object.assign({}, c, {
			          videoCapabilities: noRobustness(c.videoCapabilities),
			          audioCapabilities: noRobustness(c.audioCapabilities)
			        });
			        if (r.distinctiveIdentifier === 'not-allowed') r.distinctiveIdentifier = 'optional';
			        if (r.sessionTypes) r.sessionTypes = ['temporary'];
			        return r;
			      }));
			    });
			  };
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

	@Override
	public void init(WebBrowserAddon addon, FermataWebClient webClient,
									 FermataChromeClient chromeClient) {
		super.init(addon, webClient, chromeClient);
		setDesktopChrome();
	}

	@SuppressWarnings("removal") // Older web views still send the header
	private void setDesktopChrome() {
		WebSettings s = getSettings();
		Matcher m = CHROME_VERSION.matcher(s.getUserAgentString());
		String fullVersion = m.matches() ? m.group(1) : DEFAULT_CHROME_VERSION;
		String major = m.matches() ? m.group(2) : fullVersion.substring(0, fullVersion.indexOf('.'));

		// Chrome reports the reduced version in the User-Agent
		String ua = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/" +
				major + ".0.0.0 Safari/537.36";
		Log.d("Disney User-Agent: ", ua);
		s.setUserAgentString(ua);
		s.setUseWideViewPort(true);
		s.setLoadWithOverviewMode(true);

		if (isFeatureSupported(USER_AGENT_METADATA)) {
			var md = new UserAgentMetadata.Builder()
					.setBrandVersionList(Arrays.asList(
							brand("Google Chrome", major, fullVersion),
							brand("Chromium", major, fullVersion),
							brand("Not_A Brand", "99", "99.0.0.0")))
					.setFullVersion(fullVersion)
					.setPlatform("Linux")
					.setPlatformVersion("")
					.setArchitecture("x86")
					.setBitness(64)
					.setModel("")
					.setMobile(false)
					.setWow64(false);
			if (isFeatureSupported(USER_AGENT_METADATA_FORM_FACTORS)) {
				md.setFormFactors(Collections.singletonList(UserAgentMetadata.FORM_FACTOR_DESKTOP));
			}
			WebSettingsCompat.setUserAgentMetadata(s, md.build());
		} else {
			Log.w("User-Agent metadata is not supported by the web view");
		}

		if (isFeatureSupported(REQUESTED_WITH_HEADER_ALLOW_LIST)) {
			WebSettingsCompat.setRequestedWithHeaderOriginAllowList(s, Collections.emptySet());
		}

		if (isFeatureSupported(DOCUMENT_START_SCRIPT)) {
			WebViewCompat.addDocumentStartJavaScript(this, DESKTOP_JS,
					new HashSet<>(Arrays.asList("https://disneyplus.com", "https://*.disneyplus.com")));
		}
	}

	private static BrandVersion brand(String name, String major, String full) {
		return new BrandVersion.Builder().setBrand(name).setMajorVersion(major).setFullVersion(full)
				.build();
	}

	@Override
	protected void pageLoaded(String uri) {
		// Do not replace the last URL of the web browser
		addFocusHighlight();
		CookieManager.getInstance().flush();
	}
}
