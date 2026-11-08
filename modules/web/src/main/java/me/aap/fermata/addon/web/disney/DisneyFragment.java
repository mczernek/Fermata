package me.aap.fermata.addon.web.disney;

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
import me.aap.fermata.addon.web.desktop.DesktopChromeWebClient;
import me.aap.fermata.addon.web.R;
import me.aap.fermata.addon.web.WebBrowserAddon;
import me.aap.fermata.addon.web.WebBrowserFragment;
import me.aap.fermata.ui.activity.MainActivityDelegate;
import me.aap.utils.ui.view.ToolBarView;

/**
 * @see DisneyAddon
 */
@Keep
@SuppressWarnings("unused")
public class DisneyFragment extends WebBrowserFragment {
	private static final String DEFAULT_URL = "https://www.disneyplus.com/home";

	@Override
	public int getFragmentId() {
		return me.aap.fermata.R.id.disney_fragment;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
													 @Nullable Bundle savedInstanceState) {
		dynCtx(requireContext());
		return inflater.inflate(R.layout.disney, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
		WebBrowserAddon addon = getAddon();
		if (addon == null) return;

		DisneyWebView webView = view.findViewById(R.id.dpWebView);
		ViewGroup fullScreenView = view.findViewById(R.id.dpFullScreenView);
		webView.init(addon, new DesktopChromeWebClient(),
				new FermataChromeClient(webView, fullScreenView));
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
		DisneyWebView v = getWebView();
		if (v == null) return false;
		FermataChromeClient chrome = v.getWebChromeClient();
		return (chrome != null) && (chrome.isFullScreen() || (v.getScrollY() > 0));
	}

	@Nullable
	@Override
	protected WebBrowserAddon getAddon() {
		return AddonManager.get().getAddon(DisneyAddon.class);
	}

	@Nullable
	@Override
	protected DisneyWebView getWebView() {
		View v = getView();
		return (v != null) ? v.findViewById(R.id.dpWebView) : null;
	}

	@Override
	protected boolean isDesktopVersionSupported() {
		// Always the desktop version, the mobile site has no player
		return false;
	}

	@Override
	protected String getSearchUrl() {
		return "https://www.disneyplus.com/search?q=";
	}
}
