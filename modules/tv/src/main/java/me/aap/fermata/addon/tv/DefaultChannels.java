package me.aap.fermata.addon.tv;

import static me.aap.utils.async.Async.forEach;
import static me.aap.utils.async.Completed.completedVoid;

import android.content.Context;

import androidx.annotation.RawRes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import me.aap.fermata.BuildConfig;
import me.aap.fermata.addon.tv.m3u.TvM3uFileSystemProvider;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.function.BooleanSupplier;
import me.aap.utils.function.IntSupplier;
import me.aap.utils.io.FileUtils;
import me.aap.utils.log.Log;
import me.aap.utils.pref.PreferenceStore.Pref;

/**
 * Free TV channels, available out of the box.
 * <p>
 * The playlists are bundled with the app and each one is added as a separate TV source once -
 * if the user removes a source, it's not added again. The local copies of the playlists are
 * refreshed on app updates, so the channel lists can be updated with the app.
 */
final class DefaultChannels {
	private static final Pref<IntSupplier> VERSION =
			Pref.i("DEFAULT_CHANNELS_VERSION", 0).withInheritance(false);
	private static final Playlist[] PLAYLISTS = {
			new Playlist("polska", "Polska", R.raw.tv_polska),
			new Playlist("world_news", "World News", R.raw.tv_world_news),
			new Playlist("sport_music", "Sport & Music", R.raw.tv_sport_music),
			new Playlist("europe", "Europe", R.raw.tv_europe),
	};

	private DefaultChannels() {
	}

	static void install(TvRootItem root) {
		if (root.getIntPref(VERSION) == BuildConfig.VERSION_CODE) return;
		root.applyIntPref(VERSION, BuildConfig.VERSION_CODE);
		Context ctx = root.getLib().getContext();
		File dir = new File(ctx.getFilesDir(), "tv");
		if (!dir.isDirectory() && !dir.mkdirs()) Log.e("Failed to create directory ", dir);
		// Sequentially - concurrent additions would overwrite each other
		forEach(p -> install(ctx, root, p, new File(dir, p.key + ".m3u")), PLAYLISTS);
	}

	private static FutureSupplier<Void> install(Context ctx, TvRootItem root, Playlist p, File f) {
		try (InputStream in = ctx.getResources().openRawResource(p.res)) {
			FileUtils.copy(in, f);
		} catch (IOException err) {
			Log.e(err, "Failed to copy playlist to ", f);
			return completedVoid();
		}

		Pref<BooleanSupplier> added =
				Pref.b("DEFAULT_CHANNELS_ADDED#" + p.key, false).withInheritance(false);
		if (root.getBooleanPref(added)) return completedVoid();
		root.applyBooleanPref(added, true);
		Log.i("Adding TV source ", p.name);
		return new TvM3uFileSystemProvider().create(p.name, f.getAbsolutePath()).main()
				.then(root::addSource).ifFail(err -> {
					Log.e(err, "Failed to add TV source ", p.name);
					return null;
				});
	}

	private record Playlist(String key, String name, @RawRes int res) {}
}
