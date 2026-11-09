package me.aap.fermata.addon.web.player;

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
 * @see PlayerPlAddon
 */
@Keep
@SuppressWarnings("unused")
public class PlayerPlFragment extends WebBrowserFragment {
	private static final String DEFAULT_URL = "https://player.pl/";

	@Override
	public int getFragmentId() {
		return me.aap.fermata.R.id.player_pl_fragment;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
													 @Nullable Bundle savedInstanceState) {
		dynCtx(requireContext());
		return inflater.inflate(R.layout.player_pl, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
		WebBrowserAddon addon = getAddon();
		if (addon == null) return;

		PlayerPlWebView webView = view.findViewById(R.id.playerPlWebView);
		ViewGroup fullScreenView = view.findViewById(R.id.playerPlFullScreenView);
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
		PlayerPlWebView v = getWebView();
		if (v == null) return false;
		FermataChromeClient chrome = v.getWebChromeClient();
		return (chrome != null) && (chrome.isFullScreen() || (v.getScrollY() > 0));
	}

	@Nullable
	@Override
	protected WebBrowserAddon getAddon() {
		return AddonManager.get().getAddon(PlayerPlAddon.class);
	}

	@Nullable
	@Override
	protected PlayerPlWebView getWebView() {
		View v = getView();
		return (v != null) ? v.findViewById(R.id.playerPlWebView) : null;
	}

	@Override
	protected boolean isDesktopVersionSupported() {
		// Always the desktop version, the mobile site has no player
		return false;
	}

	@Override
	protected String getSearchUrl() {
		// The site searches in an overlay, without an URL
		return "https://www.google.com/search?q=site%3Aplayer.pl+";
	}
}
