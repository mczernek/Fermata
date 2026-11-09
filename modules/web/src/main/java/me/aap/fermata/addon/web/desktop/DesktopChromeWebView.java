package me.aap.fermata.addon.web.desktop;

import static androidx.webkit.WebViewFeature.DOCUMENT_START_SCRIPT;
import static androidx.webkit.WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST;
import static androidx.webkit.WebViewFeature.USER_AGENT_METADATA;
import static androidx.webkit.WebViewFeature.USER_AGENT_METADATA_FORM_FACTORS;
import static androidx.webkit.WebViewFeature.isFeatureSupported;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.webkit.CookieManager;
import android.webkit.WebSettings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.webkit.UserAgentMetadata;
import androidx.webkit.UserAgentMetadata.BrandVersion;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewCompat;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.aap.fermata.addon.web.FermataChromeClient;
import me.aap.fermata.addon.web.FermataWebClient;
import me.aap.fermata.addon.web.FermataWebView;
import me.aap.fermata.addon.web.WebBrowserAddon;
import me.aap.utils.log.Log;

/**
 * A web view, that is recognized by the streaming sites as desktop Chrome, like Chrome's "Desktop
 * site" mode. The mobile sites of these services don't provide the player, only the desktop ones do:
 * <ul>
 *   <li>The reduced desktop Linux User-Agent with the Chrome version of the web view.</li>
 *   <li>The User-Agent client hints (Sec-CH-UA* headers and navigator.userAgentData): the Chrome
 *   brands instead of "Android WebView", not mobile, Linux.</li>
 *   <li>No X-Requested-With header, revealing the app package.</li>
 *   <li>navigator.platform, consistent with the User-Agent.</li>
 *   <li>Widevine without the options, the web view does not support.</li>
 *   <li>The desktop layout viewport.</li>
 * </ul>
 * Some sites block the desktop identity on a phone in certain places, e.g. the bot protection of
 * the login pages. The pages, matching {@link #isMobileIdentityPage(Uri)}, are loaded as mobile
 * Chrome instead (see {@link DesktopChromeWebClient}).
 * The video is protected by Widevine, the protected media permission is granted by
 * {@link FermataChromeClient}.
 */
public abstract class DesktopChromeWebView extends FermataWebView {
	private static final Pattern CHROME_VERSION = Pattern.compile(".* Chrome/((\\d+)[.\\d]*) .*");
	private static final String DEFAULT_CHROME_VERSION = "140.0.0.0";
	/**
	 * navigator.platform, consistent with the User-Agent, and the Widevine fallback: the web view
	 * rejects some of the options, requested by the desktop players - the robustness levels
	 * (SW_SECURE_CRYPTO, SW_SECURE_DECODE...), distinctiveIdentifier "not-allowed" (the Android CDM
	 * always uses it) and persistent license sessions, but supports Widevine without them.
	 */
	private static final String DESKTOP_JS = """
			(function() {
			  try {
			    var platform = (navigator.userAgent.indexOf('X11') > 0) ? 'Linux x86_64' :
			      (navigator.userAgent.indexOf('Windows NT') > 0) ? 'Win32' : null;
			    if (platform) {
			      Object.defineProperty(Navigator.prototype, 'platform', {
			        get: function() { return platform; }, configurable: true
			      });
			    }
			  } catch (e) {}
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

	private String desktopUserAgent;
	private String mobileUserAgent;
	private UserAgentMetadata desktopMetadata;
	private UserAgentMetadata mobileMetadata;
	private boolean mobileIdentity;

	public DesktopChromeWebView(Context context) {
		super(context);
	}

	public DesktopChromeWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public DesktopChromeWebView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	/**
	 * The origins of the site, the desktop scripts are injected into, e.g. "https://*.example.com".
	 */
	@NonNull
	protected abstract Set<String> getSiteOrigins();

	/**
	 * A site specific script, injected at the document start after the desktop one.
	 */
	@NonNull
	protected String getSiteScript() {
		return "";
	}

	/**
	 * Whether to be desktop Chrome on Windows instead of Linux. Some sites only support Windows and
	 * macOS as the desktop systems.
	 */
	protected boolean isWindowsIdentity() {
		return false;
	}

	/**
	 * Whether the page must be loaded as mobile Chrome, not as desktop Chrome.
	 */
	protected boolean isMobileIdentityPage(@NonNull Uri uri) {
		return false;
	}

	/**
	 * Switches the identity (User-Agent and client hints) for the page, if required.
	 *
	 * @return true, if the identity has been changed and the page must be (re)loaded with it
	 */
	public boolean updateIdentity(@Nullable Uri uri) {
		if ((uri == null) || (desktopUserAgent == null)) return false;
		String scheme = uri.getScheme();
		if (!"https".equals(scheme) && !"http".equals(scheme)) return false;
		boolean mobile = isMobileIdentityPage(uri);
		if (mobile == mobileIdentity) return false;
		mobileIdentity = mobile;
		Log.d(mobile ? "Mobile" : "Desktop", " identity for ", uri.getHost());
		WebSettings s = getSettings();
		s.setUserAgentString(mobile ? mobileUserAgent : desktopUserAgent);
		UserAgentMetadata md = mobile ? mobileMetadata : desktopMetadata;
		if (md != null) WebSettingsCompat.setUserAgentMetadata(s, md);
		return true;
	}

	@Override
	public void loadUrl(@NonNull String url) {
		updateIdentity(Uri.parse(url));
		super.loadUrl(url);
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
		boolean win = isWindowsIdentity();
		String ua = "Mozilla/5.0 (" + (win ? "Windows NT 10.0; Win64; x64" : "X11; Linux x86_64") +
				") AppleWebKit/537.36 (KHTML, like Gecko) Chrome/" + major + ".0.0.0 Safari/537.36";
		desktopUserAgent = ua;
		mobileUserAgent = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) " +
				"Chrome/" + major + ".0.0.0 Mobile Safari/537.36";
		Log.d("Desktop User-Agent: ", ua);
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
					.setPlatform(win ? "Windows" : "Linux")
					.setPlatformVersion(win ? "15.0.0" : "")
					.setArchitecture("x86")
					.setBitness(64)
					.setModel("")
					.setMobile(false)
					.setWow64(false);
			var mmd = new UserAgentMetadata.Builder()
					.setBrandVersionList(Arrays.asList(
							brand("Google Chrome", major, fullVersion),
							brand("Chromium", major, fullVersion),
							brand("Not_A Brand", "99", "99.0.0.0")))
					.setFullVersion(fullVersion)
					.setPlatform("Android")
					.setPlatformVersion("")
					.setArchitecture("")
					.setModel("")
					.setMobile(true)
					.setWow64(false);
			if (isFeatureSupported(USER_AGENT_METADATA_FORM_FACTORS)) {
				md.setFormFactors(Collections.singletonList(UserAgentMetadata.FORM_FACTOR_DESKTOP));
				mmd.setFormFactors(Collections.singletonList(UserAgentMetadata.FORM_FACTOR_MOBILE));
			}
			desktopMetadata = md.build();
			mobileMetadata = mmd.build();
			WebSettingsCompat.setUserAgentMetadata(s, desktopMetadata);
		} else {
			Log.w("User-Agent metadata is not supported by the web view");
		}

		if (isFeatureSupported(REQUESTED_WITH_HEADER_ALLOW_LIST)) {
			WebSettingsCompat.setRequestedWithHeaderOriginAllowList(s, Collections.emptySet());
		}

		if (isFeatureSupported(DOCUMENT_START_SCRIPT)) {
			WebViewCompat.addDocumentStartJavaScript(this, DESKTOP_JS + getSiteScript(),
					getSiteOrigins());
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
