package me.aap.fermata.addon.web.netflix;

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
 * Netflix in a web view, that identifies itself as desktop Chrome. The mobile site does not
 * provide the player, only the desktop one does.
 */
@Keep
@SuppressWarnings("unused")
public class NetflixAddon extends WebBrowserAddon {
	@NonNull
	private static final AddonInfo info = FermataAddon.findAddonInfo(NetflixAddon.class.getName());
	// The site is dark already, the algorithmic darkening only distorts it
	private static final Pref<IntSupplier> NF_DARK_MODE = Pref.i("NF_DARK_MODE", DARK_MODE_DISABLED);
	private static final Pref<BooleanSupplier> NF_DESKTOP_VERSION = Pref.b("NF_DESKTOP_VERSION", true);
	private static final Pref<Supplier<String[]>> NF_BOOKMARKS = Pref.sa("NF_BOOKMARKS");

	@IdRes
	@Override
	public int getAddonId() {
		return me.aap.fermata.R.id.netflix_fragment;
	}

	@NonNull
	@Override
	public AddonInfo getInfo() {
		return info;
	}

	@NonNull
	@Override
	public ActivityFragment createFragment() {
		return new NetflixFragment();
	}

	@Override
	public Pref<IntSupplier> getForceDarkPref() {
		return NF_DARK_MODE;
	}

	@Override
	public Pref<BooleanSupplier> getDesktopVersionPref() {
		return NF_DESKTOP_VERSION;
	}

	@Override
	public Pref<Supplier<String[]>> getBookmarksPref() {
		return NF_BOOKMARKS;
	}

	/**
	 * The web browser's "open on start" handling must not be applied to this addon.
	 */
	@Override
	public void onPreferenceChanged(PreferenceStore store, List<Pref<?>> prefs) {
	}
}
