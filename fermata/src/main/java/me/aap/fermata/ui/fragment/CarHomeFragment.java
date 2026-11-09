package me.aap.fermata.ui.fragment;

import static android.util.TypedValue.COMPLEX_UNIT_PX;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import me.aap.fermata.BuildConfig;
import me.aap.fermata.R;
import me.aap.fermata.addon.AddonInfo;
import me.aap.fermata.addon.AddonManager;
import me.aap.fermata.addon.FermataAddon;
import me.aap.fermata.addon.FermataFragmentAddon;
import me.aap.fermata.ui.activity.MainActivityDelegate;
import me.aap.fermata.ui.view.DcViews;
import me.aap.fermata.ui.activity.MainActivityPrefs;
import me.aap.utils.pref.PreferenceStore;
import me.aap.utils.ui.UiUtils;
import me.aap.utils.ui.fragment.ActivityFragment;
import me.aap.utils.ui.view.FloatingButton;
import me.aap.utils.ui.view.ToolBarView;

/**
 * The home screen - a hub of the streaming services, TV and the local media, on the phone and in
 * the car. Every streaming service has its own tile, TV and the local media (folders, favorites,
 * playlists) are grouped. Other addons and the voice assistant are in the "More" section.
 * <p>
 * Settings, About and Exit are in the menu of the navigation bar / panel.
 */
public class CarHomeFragment extends MainActivityFragment
		implements AddonManager.Listener, PreferenceStore.Listener {
	private static final String TV_ADDON = "me.aap.fermata.addon.tv.TvAddon";
	private static final String STREAMING_PKG = "me.aap.fermata.addon.web.";
	private static final String BROWSER_ADDON = "me.aap.fermata.addon.web.WebBrowserAddon";
	private static final int WIDE_DP = 600;
	private static final int GAP_DP = 12;
	// Grid spans per column, so that 2, 3 or 4 groups share a row evenly
	private static final int SPANS = 12;
	// The grid spans, when everything fits on one screen: rows of 1 to 5 tiles share a row evenly
	private static final int FIT_SPANS = 60;
	private final HomeAdapter adapter = new HomeAdapter();

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
		return new HomeGrid(inflater.getContext(), adapter);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		AddonManager.get().addBroadcastListener(this);
		MainActivityPrefs.get().addBroadcastListener(this);
		reload();
	}

	@Override
	public void onDestroyView() {
		AddonManager.get().removeBroadcastListener(this);
		MainActivityPrefs.get().removeBroadcastListener(this);
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

	@Override
	public void onPreferenceChanged(PreferenceStore store, List<PreferenceStore.Pref<?>> prefs) {
		// The tiles, chosen in the settings on the phone
		if ((getView() != null) && getActivityDelegate().isCarActivity()) reload();
	}

	private void reload() {
		if (getView() instanceof HomeGrid g) {
			MainActivityDelegate a = getActivityDelegate();
			adapter.setItems(createItems(a), a.isCarActivity());
			g.requestLayout();
		}
	}

	private static List<Item> createItems(MainActivityDelegate a) {
		List<Item> streaming = new ArrayList<>();
		List<Item> groups = new ArrayList<>();
		List<Item> more = new ArrayList<>();
		Item browser = null;
		AddonManager amgr = AddonManager.get();
		boolean car = a.isCarActivity();
		// In the car, only the tiles chosen in the settings
		Predicate<String> shown = k -> !car || a.getPrefs().getCarHomeItemPref(k);

		for (AddonInfo ai : BuildConfig.ADDONS) {
			if (!(amgr.getAddon(ai.className) instanceof FermataFragmentAddon fa)) continue;
			if (!shown.test(ai.className)) continue;
			int id = fa.getFragmentId();
			Runnable open = () -> a.showFragment(id);

			if (ai.className.equals(TV_ADDON)) {
				groups.add(0, new Item(Item.GROUP, ai.icon, ai.addonName, R.string.home_tv_sub,
						R.color.dc_tv, R.color.dc_tv_dim, R.color.dc_tv_text, open));
			} else if (ai.className.equals(BROWSER_ADDON)) {
				// Not a streaming service, it's in the second row with TV and My media
				browser = new Item(Item.GROUP, ai.icon, ai.addonName, R.string.home_browser_sub,
						R.color.dc_web, R.color.dc_web_dim, R.color.dc_web_text, open);
			} else if (ai.className.startsWith(STREAMING_PKG)) {
				streaming.add(new Item(Item.SERVICE, ai.icon, ai.addonName, 0,
						serviceColor(ai.className), 0, 0, open));
			} else {
				more.add(new Item(Item.SMALL, ai.icon, ai.addonName, 0, R.color.dc_other,
						R.color.dc_other_dim, 0, open));
			}
		}

		if (shown.test(MainActivityPrefs.CAR_HOME_ITEM_MY_MEDIA)) groups.add(new Item(Item.GROUP, me.aap.utils.R.drawable.folder, R.string.home_my_media,
				R.string.home_my_media_sub, R.color.dc_media, R.color.dc_media_dim, R.color.dc_media_text,
				() -> {
					a.selectNavItem(R.id.folders_fragment);
					a.showFragment(R.id.folders_fragment);
				}));
		if (browser != null) groups.add(browser);

		if (a.getPrefs().getVoiceControlEnabledPref() &&
				shown.test(MainActivityPrefs.CAR_HOME_ITEM_VOICE)) {
			more.add(new Item(Item.SMALL, R.drawable.record_voice, R.string.voice_assistant, 0,
					R.color.dc_other, R.color.dc_other_dim, 0, a::startVoiceAssistant));
		}

		List<Item> items = new ArrayList<>();
		items.add(new Item(Item.HEADER, 0, 0, 0, 0, 0, 0, null));
		if (!streaming.isEmpty()) {
			if (!car) items.add(section(R.string.home_streaming));
			items.addAll(streaming);
		}
		if (!car) items.add(section(R.string.home_tv_and_media));
		items.addAll(groups);
		if (!more.isEmpty()) {
			items.add(section(R.string.home_more));
			items.addAll(more);
		}
		return items;
	}

	private static Item section(@StringRes int title) {
		return new Item(Item.SECTION, 0, title, 0, 0, 0, 0, null);
	}

	@ColorRes
	private static int serviceColor(String className) {
		if (className.endsWith(".YoutubeAddon")) return R.color.dc_youtube;
		if (className.endsWith(".DisneyAddon")) return R.color.dc_disney;
		if (className.endsWith(".CanalPlusAddon")) return R.color.dc_canal;
		if (className.endsWith(".TvpVodAddon")) return R.color.dc_tvp;
		if (className.endsWith(".PlayerPlAddon")) return R.color.dc_player_pl;
		if (className.endsWith(".PrimeVideoAddon")) return R.color.dc_prime_video;
		if (className.endsWith(".WebBrowserAddon")) return R.color.dc_web;
		return R.color.dc_other;
	}

	/**
	 * Everything the floating button offers is available on this screen or in the menu.
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

	private record Item(int type, @DrawableRes int icon, @StringRes int title, @StringRes int subtitle,
											@ColorRes int color, @ColorRes int dimColor, @ColorRes int subtitleColor,
											Runnable action) {
		static final int HEADER = 0;
		static final int SECTION = 1;
		static final int SERVICE = 2;
		static final int GROUP = 3;
		static final int SMALL = 4;
	}

	/**
	 * Two columns on the phone, four on wide screens. In the car the tiles are sized to fill the
	 * screen, if possible.
	 */
	private static final class HomeGrid extends RecyclerView {
		private final HomeAdapter adapter;
		private final GridLayoutManager lm;
		private final int gap;
		private int cols = 2;

		HomeGrid(Context ctx, HomeAdapter adapter) {
			super(ctx);
			this.adapter = adapter;
			gap = UiUtils.toIntPx(ctx, GAP_DP);
			lm = new GridLayoutManager(ctx, 2 * SPANS);
			lm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
				@Override
				public int getSpanSize(int position) {
					int spans = lm.getSpanCount();
					return switch (adapter.getItemViewType(position)) {
						case Item.SERVICE -> spans / adapter.serviceRowSize(position, cols);
						case Item.SMALL -> spans / cols;
						// On wide screens all the groups are in one row
						case Item.GROUP -> (cols <= 2) ? spans : spans / Math.max(1, adapter.count(Item.GROUP));
						default -> spans;
					};
				}
			});
			setLayoutManager(lm);
			setAdapter(adapter);
			setPadding(gap, gap / 2, gap, gap * 2);
			setClipToPadding(false);
			addItemDecoration(new ItemDecoration() {
				@Override
				public void getItemOffsets(@NonNull android.graphics.Rect out, @NonNull View view,
																	 @NonNull RecyclerView parent, @NonNull State state) {
					int pos = parent.getChildAdapterPosition(view);
					if (adapter.fit && (pos != NO_POSITION) &&
							(adapter.getItemViewType(pos) == Item.SECTION)) out.set(0, 0, 0, 0);
					else out.set(gap / 2, gap / 2, gap / 2, gap / 2);
				}
			});
		}

		@Override
		protected void onMeasure(int widthSpec, int heightSpec) {
			Context ctx = getContext();
			int w = MeasureSpec.getSize(widthSpec);
			int h = MeasureSpec.getSize(heightSpec);
			cols = (w >= UiUtils.toIntPx(ctx, WIDE_DP)) ? 4 : 2;
			int service = UiUtils.toIntPx(ctx, 128);
			int group = UiUtils.toIntPx(ctx, 80);

			// In the car and on wide, short screens (landscape) everything fits on one screen
			boolean fit = adapter.car || ((h > 0) && (w > h));
			int spans = fit ? FIT_SPANS : cols * SPANS;
			if (lm.getSpanCount() != spans) lm.setSpanCount(spans);
			// Very wide car screens have room for 5 services in a row
			int maxPerRow = (fit && (cols == 4) && (h > 0) && (w >= h * 12 / 5)) ? 5 : cols;

			if (fit && (h > 0)) {
				// Fill the screen: the header, the service rows and one row of the groups
				int rows = HomeAdapter.rows(adapter.count(Item.SERVICE), maxPerRow);
				int avail = h - getPaddingTop() - getPaddingBottom() - UiUtils.toIntPx(ctx, 64) -
						gap * (rows + 1);
				if (rows > 0) {
					service = Math.max(UiUtils.toIntPx(ctx, 96), avail * 3 / 5 / rows);
					group = Math.max(UiUtils.toIntPx(ctx, 80), avail * 2 / 5);
				} else {
					group = Math.max(UiUtils.toIntPx(ctx, 80), avail);
				}
			}

			adapter.setSizes(service, group, fit, maxPerRow);
			super.onMeasure(widthSpec, heightSpec);
		}
	}

	private static final class HomeAdapter extends RecyclerView.Adapter<HomeHolder> {
		private List<Item> items = new ArrayList<>();
		boolean car;
		boolean fit;
		private int serviceHeight;
		private int groupHeight;
		private int maxPerRow;

		@SuppressWarnings("NotifyDataSetChanged")
		void setItems(List<Item> items, boolean car) {
			this.items = items;
			this.car = car;
			notifyDataSetChanged();
		}

		@SuppressWarnings("NotifyDataSetChanged")
		void setSizes(int serviceHeight, int groupHeight, boolean fit, int maxPerRow) {
			if ((this.serviceHeight == serviceHeight) && (this.groupHeight == groupHeight) &&
					(this.fit == fit) && (this.maxPerRow == maxPerRow)) return;
			this.fit = fit;
			this.maxPerRow = maxPerRow;
			this.serviceHeight = serviceHeight;
			this.groupHeight = groupHeight;
			notifyDataSetChanged();
		}

		int first(int type) {
			for (int i = 0, n = items.size(); i < n; i++) if (items.get(i).type() == type) return i;
			return 0;
		}

		static int rows(int count, int perRow) {
			return (count + perRow - 1) / perRow;
		}

		/**
		 * The number of the services in the row of the service at the position. On the phone the rows
		 * are of the column count. When everything fits on one screen, the services are balanced
		 * between the rows (e.g. 3 + 2 instead of 4 + 1), so that all the rows are full.
		 */
		int serviceRowSize(int position, int cols) {
			if (!fit) return cols;
			int n = count(Item.SERVICE);
			int rows = rows(n, Math.max(1, maxPerRow));
			if (rows == 0) return 1;
			int base = n / rows;
			int extra = n % rows; // The first rows have one more
			int idx = position - first(Item.SERVICE);
			int longRows = extra * (base + 1);
			return (idx < longRows) ? base + 1 : base;
		}

		int count(int type) {
			int n = 0;
			for (Item i : items) if (i.type() == type) n++;
			return n;
		}

		@Override
		public int getItemViewType(int position) {
			return items.get(position).type();
		}

		@NonNull
		@Override
		public HomeHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			Context ctx = parent.getContext();
			return switch (viewType) {
				case Item.HEADER -> new HomeHolder(createHeader(ctx));
				case Item.SECTION -> new HomeHolder(createSection(ctx));
				case Item.SERVICE -> new HomeHolder(createService(ctx));
				default -> new HomeHolder(createGroup(ctx));
			};
		}

		@Override
		public void onBindViewHolder(@NonNull HomeHolder h, int position) {
			h.bind(items.get(position), this);
		}

		@Override
		public int getItemCount() {
			return items.size();
		}
	}

	private static final class HomeHolder extends RecyclerView.ViewHolder {
		private Item item;

		HomeHolder(View v) {
			super(v);
			if (v.isClickable()) v.setOnClickListener(t -> {
				if ((item != null) && (item.action() != null)) item.action().run();
			});
		}

		void bind(Item item, HomeAdapter adapter) {
			this.item = item;
			Context ctx = itemView.getContext();

			switch (item.type()) {
				case Item.HEADER -> {
					TextView t = itemView.findViewById(R.id.dc_header_title);
					t.setTextSize(COMPLEX_UNIT_PX, UiUtils.toPx(ctx, adapter.car ? 34 : 30));
				}
				case Item.SECTION -> {
					// The section titles take too much space, when everything must fit on one screen
					((TextView) itemView).setText(ctx.getString(item.title()).toUpperCase());
					itemView.setVisibility(adapter.fit ? View.GONE : View.VISIBLE);
					setHeight(adapter.fit ? 0 : WRAP_CONTENT);
				}
				case Item.SERVICE -> bindService(ctx, item, adapter);
				default -> bindGroup(ctx, item, adapter);
			}
		}

		private void bindService(Context ctx, Item item, HomeAdapter adapter) {
			int height = adapter.serviceHeight;
			setHeight(height);
			int color = ContextCompat.getColor(ctx, item.color());
			itemView.setBackground(tileBackground(ctx, color, adapter.car ? 24 : 22));
			ImageView icon = itemView.findViewById(R.id.home_tile_icon);
			TextView title = itemView.findViewById(R.id.home_tile_title);
			icon.setImageResource(item.icon());
			title.setText(item.title());
			int iconSize = Math.max(UiUtils.toIntPx(ctx, 36), height / 4);
			setSize(icon, iconSize, iconSize);
			// Large in the car, but not so large, that long names are cut
			title.setTextSize(COMPLEX_UNIT_PX,
					Math.min(UiUtils.toPx(ctx, adapter.car ? 30 : 24),
							Math.max(UiUtils.toPx(ctx, 17), height / 7f)));
		}

		private void bindGroup(Context ctx, Item item, HomeAdapter adapter) {
			boolean small = item.type() == Item.SMALL;
			int height = small ? UiUtils.toIntPx(ctx, 64) : adapter.groupHeight;
			setHeight(height);
			itemView.setBackground(
					tileBackground(ctx, ContextCompat.getColor(ctx, item.dimColor()), adapter.car ? 24 : 22));
			FrameLayout box = itemView.findViewById(R.id.home_tile_icon_box);
			ImageView icon = itemView.findViewById(R.id.home_tile_icon);
			TextView title = itemView.findViewById(R.id.home_tile_title);
			TextView sub = itemView.findViewById(R.id.home_tile_subtitle);
			ImageView chevron = itemView.findViewById(R.id.home_tile_chevron);
			int color = ContextCompat.getColor(ctx, item.color());

			int boxSize = Math.min(height - UiUtils.toIntPx(ctx, 24), UiUtils.toIntPx(ctx, 84));
			setSize(box, boxSize, boxSize);
			GradientDrawable boxBg = new GradientDrawable();
			boxBg.setColor(color);
			boxBg.setCornerRadius(boxSize * 0.27f);
			box.setBackground(boxBg);
			int iconSize = boxSize * 11 / 20;
			setSize(icon, iconSize, iconSize);
			icon.setImageResource(item.icon());

			title.setText(item.title());
			title.setTextSize(COMPLEX_UNIT_PX, UiUtils.toPx(ctx, small ? 16 : (adapter.car ? 26 : 18)));

			if (item.subtitle() != 0) {
				sub.setVisibility(View.VISIBLE);
				sub.setText(item.subtitle());
				sub.setTextColor(ContextCompat.getColor(ctx, item.subtitleColor()));
				sub.setTextSize(COMPLEX_UNIT_PX, UiUtils.toPx(ctx, adapter.car ? 17 : 13));
			} else {
				sub.setVisibility(View.GONE);
			}

			if (!small && !adapter.car) {
				chevron.setVisibility(View.VISIBLE);
				chevron.setImageTintList(ColorStateList.valueOf(color));
			} else {
				chevron.setVisibility(View.GONE);
			}
		}

		private void setHeight(int height) {
			ViewGroup.LayoutParams lp = itemView.getLayoutParams();
			if ((lp != null) && (lp.height != height)) {
				lp.height = height;
				itemView.setLayoutParams(lp);
			}
		}

		private static void setSize(View v, int w, int h) {
			ViewGroup.LayoutParams lp = v.getLayoutParams();
			if ((lp.width != w) || (lp.height != h)) {
				lp.width = w;
				lp.height = h;
				v.setLayoutParams(lp);
			}
		}
	}

	/**
	 * The wordmark with the Settings and Menu buttons on the right (there is no navigation bar).
	 */
	private static View createHeader(Context ctx) {
		LinearLayout l = new LinearLayout(ctx);
		l.setId(R.id.dc_header);
		l.setLayoutParams(new RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
		l.setOrientation(LinearLayout.HORIZONTAL);
		l.setGravity(Gravity.CENTER_VERTICAL);
		l.setPaddingRelative(0, 0, DcViews.topEndInset(ctx), 0);
		TextView t = createWordmark(ctx);
		l.addView(t, new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1));
		DcViews.addAction(l, DcViews.iconButton(ctx, R.drawable.dc_settings, R.color.dc_cream,
				ctx.getString(R.string.settings),
				v -> MainActivityDelegate.get(v.getContext()).showFragment(R.id.settings_fragment)));
		DcViews.addAction(l, DcViews.iconButton(ctx, R.drawable.dc_menu, R.color.dc_cream,
				ctx.getString(R.string.menu),
				v -> MainActivityDelegate.get(v.getContext()).showNavMenu()));
		return l;
	}

	private static TextView createWordmark(Context ctx) {
		TextView t = new TextView(ctx);
		t.setId(R.id.dc_header_title);
		int pad = UiUtils.toIntPx(ctx, 4);
		t.setPadding(pad, pad * 2, pad, pad);
		t.setTypeface(ResourcesCompat.getFont(ctx, R.font.barlow_condensed_bold));
		t.setTextColor(ContextCompat.getColor(ctx, R.color.dc_cream));
		SpannableString s = new SpannableString("DashCinema");
		s.setSpan(new ForegroundColorSpan(ContextCompat.getColor(ctx, R.color.dc_orange)), 4,
				s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		t.setText(s);
		t.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
		return t;
	}

	private static TextView createSection(Context ctx) {
		TextView t = new TextView(ctx);
		t.setLayoutParams(new RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
		int pad = UiUtils.toIntPx(ctx, 4);
		t.setPadding(pad, pad * 3, pad, 0);
		t.setTextColor(ContextCompat.getColor(ctx, R.color.dc_muted));
		t.setTextSize(13);
		t.setLetterSpacing(0.08f);
		t.setTypeface(ResourcesCompat.getFont(ctx, R.font.plus_jakarta_sans_extrabold));
		return t;
	}

	private static View createService(Context ctx) {
		LinearLayout l = new LinearLayout(ctx);
		l.setLayoutParams(new RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
		l.setOrientation(LinearLayout.VERTICAL);
		int pad = UiUtils.toIntPx(ctx, 16);
		l.setPadding(pad, pad, pad, pad);
		makeClickable(ctx, l);
		int onTile = ContextCompat.getColor(ctx, R.color.dc_on_tile);

		ImageView icon = new ImageView(ctx);
		icon.setId(R.id.home_tile_icon);
		icon.setImageTintList(ColorStateList.valueOf(onTile));
		icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
		icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
		l.addView(icon, new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));

		View spacer = new View(ctx);
		l.addView(spacer, new LinearLayout.LayoutParams(0, 0, 1));

		TextView title = new TextView(ctx);
		title.setId(R.id.home_tile_title);
		title.setTextColor(onTile);
		title.setTypeface(ResourcesCompat.getFont(ctx, R.font.plus_jakarta_sans_extrabold));
		title.setMaxLines(2);
		title.setEllipsize(TextUtils.TruncateAt.END);
		l.addView(title, new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
		return l;
	}

	private static View createGroup(Context ctx) {
		LinearLayout l = new LinearLayout(ctx);
		l.setLayoutParams(new RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
		l.setOrientation(LinearLayout.HORIZONTAL);
		l.setGravity(Gravity.CENTER_VERTICAL);
		int pad = UiUtils.toIntPx(ctx, 14);
		l.setPadding(pad, 0, pad, 0);
		makeClickable(ctx, l);

		FrameLayout box = new FrameLayout(ctx);
		box.setId(R.id.home_tile_icon_box);
		ImageView icon = new ImageView(ctx);
		icon.setId(R.id.home_tile_icon);
		icon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.dc_on_tile)));
		icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
		box.addView(icon, new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER));
		l.addView(box, new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));

		LinearLayout texts = new LinearLayout(ctx);
		texts.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1);
		tlp.setMarginStart(pad);
		tlp.setMarginEnd(pad / 2);
		l.addView(texts, tlp);

		TextView title = new TextView(ctx);
		title.setId(R.id.home_tile_title);
		title.setTextColor(ContextCompat.getColor(ctx, R.color.dc_cream));
		title.setTypeface(ResourcesCompat.getFont(ctx, R.font.plus_jakarta_sans_extrabold));
		title.setSingleLine();
		title.setEllipsize(TextUtils.TruncateAt.END);
		texts.addView(title);

		TextView sub = new TextView(ctx);
		sub.setId(R.id.home_tile_subtitle);
		sub.setSingleLine();
		sub.setEllipsize(TextUtils.TruncateAt.END);
		texts.addView(sub);

		ImageView chevron = new ImageView(ctx);
		chevron.setId(R.id.home_tile_chevron);
		chevron.setImageResource(R.drawable.home_chevron);
		chevron.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
		int c = UiUtils.toIntPx(ctx, 22);
		l.addView(chevron, new LinearLayout.LayoutParams(c, c));
		return l;
	}

	private static void makeClickable(Context ctx, View v) {
		v.setClickable(true);
		v.setFocusable(true);
		// A cream outline, when focused with the D-pad or a rotary controller
		StateListDrawable fg = new StateListDrawable();
		GradientDrawable focus = new GradientDrawable();
		focus.setCornerRadius(UiUtils.toPx(ctx, 22));
		focus.setStroke(UiUtils.toIntPx(ctx, 3), ContextCompat.getColor(ctx, R.color.dc_cream));
		fg.addState(new int[]{android.R.attr.state_focused}, focus);
		fg.addState(new int[]{}, new ColorDrawable(0));
		v.setForeground(fg);
	}

	private static RippleDrawable tileBackground(Context ctx, int color, int radiusDp) {
		GradientDrawable shape = new GradientDrawable();
		shape.setColor(color);
		shape.setCornerRadius(UiUtils.toPx(ctx, radiusDp));
		ColorStateList ripple =
				ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.dc_orange_ripple));
		return new RippleDrawable(ripple, shape, null);
	}
}
