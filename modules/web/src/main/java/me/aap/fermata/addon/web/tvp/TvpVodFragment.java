package me.aap.fermata.addon.web.tvp;

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
 * @see TvpVodAddon
 */
@Keep
@SuppressWarnings("unused")
public class TvpVodFragment extends WebBrowserFragment {
	private static final String DEFAULT_URL = "https://vod.tvp.pl/";

	@Override
	public int getFragmentId() {
		return me.aap.fermata.R.id.tvp_vod_fragment;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
													 @Nullable Bundle savedInstanceState) {
		dynCtx(requireContext());
		return inflater.inflate(R.layout.tvp_vod, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
		WebBrowserAddon addon = getAddon();
		if (addon == null) return;

		TvpVodWebView webView = view.findViewById(R.id.tvpWebView);
		ViewGroup fullScreenView = view.findViewById(R.id.tvpFullScreenView);
		webView.init(addon, new FermataWebClient(),
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
		TvpVodWebView v = getWebView();
		if (v == null) return false;
		FermataChromeClient chrome = v.getWebChromeClient();
		return (chrome != null) && (chrome.isFullScreen() || (v.getScrollY() > 0));
	}

	@Nullable
	@Override
	protected WebBrowserAddon getAddon() {
		return AddonManager.get().getAddon(TvpVodAddon.class);
	}

	@Nullable
	@Override
	protected TvpVodWebView getWebView() {
		View v = getView();
		return (v != null) ? v.findViewById(R.id.tvpWebView) : null;
	}

	@Override
	protected String getSearchUrl() {
		return "https://vod.tvp.pl/szukaj?query=";
	}
}
