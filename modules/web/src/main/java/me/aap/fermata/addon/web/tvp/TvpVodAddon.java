package me.aap.fermata.addon.web.tvp;

import androidx.annotation.IdRes;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import java.util.List;

import me.aap.fermata.addon.AddonInfo;
import me.aap.fermata.addon.FermataAddon;
import me.aap.fermata.addon.web.WebBrowserAddon;
import me.aap.utils.function.BooleanSupplier;
import me.aap.utils.function.IntSupplier;
import me.aap.utils.function.Supplier;
import me.aap.utils.pref.PreferenceStore;
import me.aap.utils.ui.fragment.ActivityFragment;

/**
 * TVP VOD in a web view, that identifies itself as desktop Chrome. The mobile site asks to
 * install the app instead of playing.
 */
@Keep
@SuppressWarnings("unused")
public class TvpVodAddon extends WebBrowserAddon {
	@NonNull
	private static final AddonInfo info = FermataAddon.findAddonInfo(TvpVodAddon.class.getName());
	// The site is dark already, the algorithmic darkening only distorts it
	private static final Pref<IntSupplier> TVP_DARK_MODE = Pref.i("TVP_DARK_MODE", DARK_MODE_DISABLED);
	private static final Pref<BooleanSupplier> TVP_DESKTOP_VERSION = Pref.b("TVP_DESKTOP_VERSION", true);
	private static final Pref<Supplier<String[]>> TVP_BOOKMARKS = Pref.sa("TVP_BOOKMARKS");

	@IdRes
	@Override
	public int getAddonId() {
		return me.aap.fermata.R.id.tvp_vod_fragment;
	}

	@NonNull
	@Override
	public AddonInfo getInfo() {
		return info;
	}

	@NonNull
	@Override
	public ActivityFragment createFragment() {
		return new TvpVodFragment();
	}

	@Override
	public Pref<IntSupplier> getForceDarkPref() {
		return TVP_DARK_MODE;
	}

	@Override
	public Pref<BooleanSupplier> getDesktopVersionPref() {
		return TVP_DESKTOP_VERSION;
	}

	@Override
	public Pref<Supplier<String[]>> getBookmarksPref() {
		return TVP_BOOKMARKS;
	}

	/**
	 * The web browser's "open on start" handling must not be applied to this addon.
	 */
	@Override
	public void onPreferenceChanged(PreferenceStore store, List<Pref<?>> prefs) {
	}
}
