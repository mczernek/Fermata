package me.aap.fermata.ui.fragment;

import static android.util.TypedValue.COMPLEX_UNIT_PX;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import me.aap.fermata.BuildConfig;
import me.aap.fermata.R;
import me.aap.fermata.addon.AddonInfo;
import me.aap.fermata.addon.AddonManager;
import me.aap.fermata.addon.FermataAddon;
import me.aap.fermata.addon.FermataFragmentAddon;
import me.aap.fermata.ui.activity.MainActivityDelegate;
import me.aap.utils.ui.UiUtils;
import me.aap.utils.ui.fragment.ActivityFragment;
import me.aap.utils.ui.view.FloatingButton;
import me.aap.utils.ui.view.ToolBarView;

/**
 * The car home screen - large tiles for every top level destination, replacing the nav bar.
 * The tiles are laid out to fit the screen without scrolling, if possible.
 */
public class CarHomeFragment extends MainActivityFragment implements AddonManager.Listener {
	private static final int MIN_TILE_DP = 80;
	private static final int GAP_DP = 8;
	private final TileAdapter adapter = new TileAdapter();

	@Override
	public int getFragmentId() {
		return R.id.car_home_fragment;
	}

	@Override
	public CharSequence getTitle() {
		return getString(R.string.car_home);
	}

	@Override
	public ToolBarView.Mediator getToolBarMediator() {
		return ToolBarView.Mediator.Invisible.instance;
	}

	@Override
	public FloatingButton.Mediator getFloatingButtonMediator() {
		return HiddenFloatingButton.instance;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
													 @Nullable Bundle savedInstanceState) {
		return new TileGrid(inflater.getContext(), adapter);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		AddonManager.get().addBroadcastListener(this);
		reload();
	}

	@Override
	public void onDestroyView() {
		AddonManager.get().removeBroadcastListener(this);
		super.onDestroyView();
	}

	@Override
	public void onHiddenChanged(boolean hidden) {
		super.onHiddenChanged(hidden);
		if (!hidden) reload();
	}

	@Override
	public void onAddonChanged(AddonManager mgr, AddonInfo info, boolean installed) {
		reload();
	}

	private void reload() {
		if (getView() instanceof TileGrid g) {
			adapter.setTiles(createTiles(getActivityDelegate()));
			g.requestLayout();
		}
	}

	private static List<Tile> createTiles(MainActivityDelegate a) {
		List<Tile> tiles = new ArrayList<>();
		if (a.hasCurrent()) tiles.add(new Tile(R.drawable.go_to_current, R.string.now_playing,
				() -> a.goToCurrent()));
		tiles.add(fragmentTile(a, R.id.folders_fragment, me.aap.utils.R.drawable.folder,
				R.string.folders));
		tiles.add(fragmentTile(a, R.id.favorites_fragment, R.drawable.favorite_filled,
				R.string.favorites));
		tiles.add(fragmentTile(a, R.id.playlists_fragment, R.drawable.playlist, R.string.playlists));

		AddonManager amgr = AddonManager.get();
		for (AddonInfo ai : BuildConfig.ADDONS) {
			FermataAddon addon = amgr.getAddon(ai.className);
			if (addon instanceof FermataFragmentAddon fa)
				tiles.add(fragmentTile(a, fa.getFragmentId(), ai.icon, ai.addonName));
		}

		if (a.getPrefs().getVoiceControlEnabledPref()) {
			tiles.add(new Tile(R.drawable.record_voice, R.string.voice_assistant,
					a::startVoiceAssistant));
		}

		tiles.add(fragmentTile(a, R.id.settings_fragment, R.drawable.settings, R.string.settings));
		tiles.add(new Tile(R.drawable.about, R.string.about, () -> NavBarMediator.showAbout(a)));
		tiles.add(new Tile(R.drawable.exit,
				a.isCarActivityNotMirror() ? R.string.restart : R.string.exit,
				() -> NavBarMediator.exit(a)));
		return tiles;
	}

	private static Tile fragmentTile(MainActivityDelegate a, int fragmentId, @DrawableRes int icon,
																	 @StringRes int title) {
		return new Tile(icon, title, () -> a.showFragment(fragmentId));
	}

	/**
	 * Everything the floating button offers is available as a tile.
	 */
	private static final class HiddenFloatingButton implements FloatingButton.Mediator {
		static final HiddenFloatingButton instance = new HiddenFloatingButton();

		@Override
		public void enable(FloatingButton fb, ActivityFragment f) {
			fb.setVisibility(View.GONE);
		}

		@Override
		public void disable(FloatingButton fb) {
			FloatingButton.Mediator.super.disable(fb);
			fb.setVisibility(View.VISIBLE);
		}
	}

	private record Tile(@DrawableRes int icon, @StringRes int title, Runnable action) {}

	/**
	 * Picks the number of columns that gives the largest tiles while showing all of them.
	 * Falls back to a scrollable grid of minimum size tiles when the screen is too small.
	 */
	private static final class TileGrid extends RecyclerView {
		private final TileAdapter adapter;
		private final GridLayoutManager lm;
		private final int gap;

		TileGrid(Context ctx, TileAdapter adapter) {
			super(ctx);
			this.adapter = adapter;
			gap = UiUtils.toIntPx(ctx, GAP_DP);
			lm = new GridLayoutManager(ctx, 1);
			setLayoutManager(lm);
			setAdapter(adapter);
			setPadding(gap / 2, gap / 2, gap / 2, gap / 2);
			setClipToPadding(false);
			addItemDecoration(new ItemDecoration() {
				@Override
				public void getItemOffsets(@NonNull android.graphics.Rect out, @NonNull View view,
																	 @NonNull RecyclerView parent, @NonNull State state) {
					out.set(gap / 2, gap / 2, gap / 2, gap / 2);
				}
			});
		}

		@Override
		protected void onMeasure(int widthSpec, int heightSpec) {
			int w = MeasureSpec.getSize(widthSpec) - gap;
			int h = MeasureSpec.getSize(heightSpec) - gap;
			int n = adapter.getItemCount();

			if ((w > 0) && (h > 0) && (n > 0)) {
				int min = UiUtils.toIntPx(getContext(), MIN_TILE_DP);
				int bestCols = 0;
				int bestSize = 0;
				int bestHeight = 0;

				for (int cols = 1; cols <= n; cols++) {
					int rows = (n + cols - 1) / cols;
					int cellW = w / cols - gap;
					int cellH = h / rows - gap;
					int size = Math.min(cellW, cellH);
					if ((size >= min) && (size > bestSize)) {
						bestCols = cols;
						bestSize = size;
						// Do not stretch the tiles to more than 3:4 (w:h)
						bestHeight = Math.min(cellH, cellW * 4 / 3);
					}
				}

				if (bestCols == 0) { // Does not fit - scroll
					bestCols = Math.max(1, w / (min + gap));
					bestHeight = min;
				}

				if (lm.getSpanCount() != bestCols) lm.setSpanCount(bestCols);
				adapter.setTileHeight(bestHeight);
			}

			super.onMeasure(widthSpec, heightSpec);
		}
	}

	private static final class TileAdapter extends RecyclerView.Adapter<TileHolder> {
		private List<Tile> tiles = new ArrayList<>();
		private int tileHeight;

		@SuppressWarnings("NotifyDataSetChanged")
		void setTiles(List<Tile> tiles) {
			this.tiles = tiles;
			notifyDataSetChanged();
		}

		@SuppressWarnings("NotifyDataSetChanged")
		void setTileHeight(int tileHeight) {
			if (this.tileHeight == tileHeight) return;
			this.tileHeight = tileHeight;
			notifyDataSetChanged();
		}

		@NonNull
		@Override
		public TileHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View v = LayoutInflater.from(parent.getContext())
					.inflate(R.layout.car_home_tile, parent, false);
			return new TileHolder(v);
		}

		@Override
		public void onBindViewHolder(@NonNull TileHolder h, int position) {
			h.bind(tiles.get(position), tileHeight);
		}

		@Override
		public int getItemCount() {
			return tiles.size();
		}
	}

	private static final class TileHolder extends RecyclerView.ViewHolder {
		private final ImageView icon;
		private final TextView title;
		private Tile tile;

		TileHolder(View v) {
			super(v);
			icon = v.findViewById(R.id.car_home_tile_icon);
			title = v.findViewById(R.id.car_home_tile_title);
			v.setOnClickListener(t -> {
				if (tile != null) tile.action().run();
			});
		}

		void bind(Tile tile, int height) {
			this.tile = tile;
			icon.setImageResource(tile.icon());
			title.setText(tile.title());

			if (height > 0) {
				ViewGroup.LayoutParams lp = itemView.getLayoutParams();
				if (lp.height != height) {
					lp.height = height;
					itemView.setLayoutParams(lp);
				}
				// Scale the content with the tile, so it's readable on large and small screens
				int iconSize = height * 2 / 5;
				ViewGroup.LayoutParams ilp = icon.getLayoutParams();
				if (ilp.width != iconSize) {
					ilp.width = ilp.height = iconSize;
					icon.setLayoutParams(ilp);
				}
				title.setTextSize(COMPLEX_UNIT_PX, Math.max(height / 9f,
						UiUtils.toPx(itemView.getContext(), 12)));
			}
		}
	}
}
