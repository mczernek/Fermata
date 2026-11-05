package me.aap.fermata.addon.web.netflix;

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
 * A web view, that is recognized by Netflix as desktop Chrome, like Chrome's "Desktop site" mode:
 * <ul>
 *   <li>The reduced desktop Linux User-Agent with the Chrome version of the web view.</li>
 *   <li>The User-Agent client hints (Sec-CH-UA* headers and navigator.userAgentData): the Chrome
 *   brands instead of "Android WebView", not mobile, Linux.</li>
 *   <li>No X-Requested-With header, revealing the app package.</li>
 *   <li>navigator.platform, consistent with the User-Agent.</li>
 *   <li>The desktop layout viewport.</li>
 * </ul>
 * The video is protected by Widevine, the protected media permission is granted by
 * {@link FermataChromeClient}.
 */
public class NetflixWebView extends FermataWebView {
	private static final Pattern CHROME_VERSION = Pattern.compile(".* Chrome/((\\d+)[.\\d]*) .*");
	private static final String DEFAULT_CHROME_VERSION = "140.0.0.0";
	private static final String PLATFORM_JS = """
			(function() {
			  try {
			    Object.defineProperty(Navigator.prototype, 'platform', {
			      get: function() { return 'Linux x86_64'; }, configurable: true
			    });
			  } catch (e) {}
			})();""";

	public NetflixWebView(Context context) {
		super(context);
	}

	public NetflixWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public NetflixWebView(Context context, AttributeSet attrs, int defStyleAttr) {
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
		Log.d("Netflix User-Agent: ", ua);
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
			WebViewCompat.addDocumentStartJavaScript(this, PLATFORM_JS,
					new HashSet<>(Arrays.asList("https://netflix.com", "https://*.netflix.com")));
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
