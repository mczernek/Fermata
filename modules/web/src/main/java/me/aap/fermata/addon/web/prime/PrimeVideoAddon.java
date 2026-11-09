package me.aap.fermata.addon.web.prime;

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
 * Prime Video in the web view, as the mobile site (the desktop version can be enabled in the menu).
 */
@Keep
@SuppressWarnings("unused")
public class PrimeVideoAddon extends WebBrowserAddon {
	@NonNull
	private static final AddonInfo info = FermataAddon.findAddonInfo(PrimeVideoAddon.class.getName());
	// The site is dark already, the algorithmic darkening only distorts it
	private static final Pref<IntSupplier> PRIME_VIDEO_DARK_MODE = Pref.i("PRIME_VIDEO_DARK_MODE", DARK_MODE_DISABLED);
	private static final Pref<BooleanSupplier> PRIME_VIDEO_DESKTOP_VERSION = Pref.b("PRIME_VIDEO_DESKTOP_VERSION", false);
	private static final Pref<Supplier<String[]>> PRIME_VIDEO_BOOKMARKS = Pref.sa("PRIME_VIDEO_BOOKMARKS");

	@IdRes
	@Override
	public int getAddonId() {
		return me.aap.fermata.R.id.prime_video_fragment;
	}

	@NonNull
	@Override
	public AddonInfo getInfo() {
		return info;
	}

	@NonNull
	@Override
	public ActivityFragment createFragment() {
		return new PrimeVideoFragment();
	}

	@Override
	public Pref<IntSupplier> getForceDarkPref() {
		return PRIME_VIDEO_DARK_MODE;
	}

	@Override
	public Pref<BooleanSupplier> getDesktopVersionPref() {
		return PRIME_VIDEO_DESKTOP_VERSION;
	}

	@Override
	public Pref<Supplier<String[]>> getBookmarksPref() {
		return PRIME_VIDEO_BOOKMARKS;
	}

	/**
	 * The web browser's "open on start" handling must not be applied to this addon.
	 */
	@Override
	public void onPreferenceChanged(PreferenceStore store, List<Pref<?>> prefs) {
	}
}
