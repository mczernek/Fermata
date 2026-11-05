package me.aap.fermata.addon.web.netflix;

import static me.aap.fermata.util.Utils.dynCtx;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import me.aap.fermata.addon.AddonManager;
import me.aap.fermata.addon.web.FermataChromeClient;
import me.aap.fermata.addon.web.FermataWebClient;
import me.aap.fermata.addon.web.R;
import me.aap.fermata.addon.web.WebBrowserAddon;
import me.aap.fermata.addon.web.WebBrowserFragment;
import me.aap.fermata.ui.activity.MainActivityDelegate;
import me.aap.utils.ui.view.ToolBarView;

/**
 * @see NetflixAddon
 */
@Keep
@SuppressWarnings("unused")
public class NetflixFragment extends WebBrowserFragment {
	private static final String DEFAULT_URL = "https://www.netflix.com/browse";

	@Override
	public int getFragmentId() {
		return me.aap.fermata.R.id.netflix_fragment;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
													 @Nullable Bundle savedInstanceState) {
		dynCtx(requireContext());
		return inflater.inflate(R.layout.netflix, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
		WebBrowserAddon addon = getAddon();
		if (addon == null) return;

		NetflixWebView webView = view.findViewById(R.id.nfWebView);
		ViewGroup fullScreenView = view.findViewById(R.id.nfFullScreenView);
		webView.init(addon, new FermataWebClient(), new FermataChromeClient(webView, fullScreenView));
		webView.loadUrl((state != null) ? state.getString("url", DEFAULT_URL) : DEFAULT_URL);
		MainActivityDelegate.getActivityDelegate(view.getContext()).onSuccess(this::registerListeners);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle state) {
		super.onSaveInstanceState(state);
		String url = getUrl();
		if (url != null) state.putString("url", url);
	}

	@Override
	public ToolBarView.Mediator getToolBarMediator() {
		// The site has its own navigation
		return ToolBarView.Mediator.Invisible.instance;
	}

	@Override
	public boolean canScrollUp() {
		NetflixWebView v = getWebView();
		if (v == null) return false;
		FermataChromeClient chrome = v.getWebChromeClient();
		return (chrome != null) && (chrome.isFullScreen() || (v.getScrollY() > 0));
	}

	@Nullable
	@Override
	protected WebBrowserAddon getAddon() {
		return AddonManager.get().getAddon(NetflixAddon.class);
	}

	@Nullable
	@Override
	protected NetflixWebView getWebView() {
		View v = getView();
		return (v != null) ? v.findViewById(R.id.nfWebView) : null;
	}

	@Override
	protected boolean isDesktopVersionSupported() {
		// Always the desktop version, the mobile site has no player
		return false;
	}

	@Override
	protected String getSearchUrl() {
		return "https://www.netflix.com/search?q=";
	}
}
