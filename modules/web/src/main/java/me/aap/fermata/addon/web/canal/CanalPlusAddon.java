package me.aap.fermata.addon.web.canal;

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
 * CANAL+ online in a web view, that identifies itself as desktop Chrome. The mobile site does
 * not provide the player, it asks to install the app.
 */
@Keep
@SuppressWarnings("unused")
public class CanalPlusAddon extends WebBrowserAddon {
	@NonNull
	private static final AddonInfo info = FermataAddon.findAddonInfo(CanalPlusAddon.class.getName());
	// The site is dark already, the algorithmic darkening only distorts it
	private static final Pref<IntSupplier> CP_DARK_MODE = Pref.i("CP_DARK_MODE", DARK_MODE_DISABLED);
	private static final Pref<BooleanSupplier> CP_DESKTOP_VERSION = Pref.b("CP_DESKTOP_VERSION", true);
	private static final Pref<Supplier<String[]>> CP_BOOKMARKS = Pref.sa("CP_BOOKMARKS");

	@IdRes
	@Override
	public int getAddonId() {
		return me.aap.fermata.R.id.canal_plus_fragment;
	}

	@NonNull
	@Override
	public AddonInfo getInfo() {
		return info;
	}

	@NonNull
	@Override
	public ActivityFragment createFragment() {
		return new CanalPlusFragment();
	}

	@Override
	public Pref<IntSupplier> getForceDarkPref() {
		return CP_DARK_MODE;
	}

	@Override
	public Pref<BooleanSupplier> getDesktopVersionPref() {
		return CP_DESKTOP_VERSION;
	}

	@Override
	public Pref<Supplier<String[]>> getBookmarksPref() {
		return CP_BOOKMARKS;
	}

	/**
	 * The web browser's "open on start" handling must not be applied to this addon.
	 */
	@Override
	public void onPreferenceChanged(PreferenceStore store, List<Pref<?>> prefs) {
	}
}
