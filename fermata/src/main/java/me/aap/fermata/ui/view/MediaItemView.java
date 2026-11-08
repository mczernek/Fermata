package me.aap.fermata.ui.view;

import static android.content.ContentResolver.SCHEME_ANDROID_RESOURCE;
import static android.util.TypedValue.COMPLEX_UNIT_PX;
import static me.aap.fermata.media.lib.MediaLib.StreamItem.STREAM_END_TIME;
import static me.aap.fermata.media.lib.MediaLib.StreamItem.STREAM_START_TIME;
import static me.aap.utils.function.ProgressiveResultConsumer.PROGRESS_DONE;
import static me.aap.utils.function.ResultConsumer.Cancel.isCancellation;
import static me.aap.utils.misc.MiscUtils.ifNull;
import static me.aap.utils.ui.UiUtils.getTextAppearanceSize;
import static me.aap.utils.ui.UiUtils.toPx;
import static me.aap.utils.ui.activity.ActivityListener.ACTIVITY_DESTROY;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.MediaDescriptionCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;
import java.util.Objects;

import me.aap.fermata.BuildConfig;
import me.aap.fermata.R;
import me.aap.fermata.media.lib.MediaLib.ArchiveItem;
import me.aap.fermata.media.lib.MediaLib.EpgItem;
import me.aap.fermata.media.lib.MediaLib.Item;
import me.aap.fermata.media.lib.MediaLib.PlayableItem;
import me.aap.fermata.media.pref.PlayableItemPrefs;
import me.aap.fermata.ui.activity.MainActivityDelegate;
import me.aap.fermata.ui.fragment.MediaLibFragment;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.function.Cancellable;
import me.aap.utils.log.Log;
import me.aap.utils.ui.UiUtils;
import me.aap.utils.ui.activity.ActivityDelegate;
import me.aap.utils.ui.fragment.ActivityFragment;
import me.aap.utils.ui.menu.OverlayMenu;

/**
 * @author Andrey Pavlenko
 */
public class MediaItemView extends ConstraintLayout
		implements OnLongClickListener, OnCheckedChangeListener, Item.ChangeListener {
	private static final RotateAnimation rotate =
			new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
					0.5f);
	private static Drawable loadingDrawable;
	@ColorInt
	private static int iconColor;
	@ColorInt
	private static int hintColor;
	@StyleRes
	private final int titleTextAppearance;
	@StyleRes
	private final int subtitleTextAppearance;
	private ColorStateList iconTint;
	private final ColorStateList textTint;
	@Nullable
	private MediaItemViewHolder holder;
	private ProgressUpdater progressUpdater;
	private VectorDrawableCompat watchedVideoDrawable;
	private VectorDrawableCompat watchingVideoDrawable;
	private VectorDrawableCompat archiveLabelDrawable;
	private FutureSupplier<MediaDescriptionCompat> loading;
	// DashCinema card style (TV channels): rounded cards with colored icon boxes
	private boolean card;
	private boolean cardGrid;

	public MediaItemView(Context ctx, AttributeSet attrs) {
		super(ctx, attrs, R.attr.appMediaItemStyle);
		TypedArray ta =
				ctx.obtainStyledAttributes(attrs, R.styleable.MediaItemView, R.attr.appMediaItemStyle,
						R.style.AppTheme_MediaItemStyle);
		iconColor = ta.getColor(R.styleable.MediaItemView_iconColor, 0);
		hintColor = ta.getColor(R.styleable.MediaItemView_hintColor, 0);
		titleTextAppearance = ta.getResourceId(R.styleable.MediaItemView_titleTextAppearance, 0);
		subtitleTextAppearance = ta.getResourceId(R.styleable.MediaItemView_subtitleTextAppearance, 0);
		setElevation(ta.getDimension(R.styleable.MediaItemView_elevation, 0));
		textTint = ta.getColorStateList(R.styleable.MediaItemView_android_textColor);
		ta.recycle();
		MainActivityDelegate a = getMainActivity();
		applyLayout(ctx, a.isGridView(), a.getPrefs().getTextIconSizePref(a));
		iconTint = getIcon().getImageTintList();
		setLongClickable(true);
		setOnLongClickListener(this);
		getCheckBox().setOnCheckedChangeListener(this);
		setBackgroundResource(R.drawable.media_item_bg);
		setFocusable(true);
	}

	public void applyLayout(Context ctx, boolean grid, float size) {
		// The cards are always either a grid or a list, not switched with the preference
		if (card) grid = (getListView() != null) ? getListView().isGrid() : grid;
		removeAllViews();
		if (card) {
			cardGrid = grid;
			inflate(ctx, grid ? R.layout.media_item_card_grid_layout : R.layout.media_item_card_layout,
					this);
			applyCardStyle(ctx);
		} else {
			inflate(ctx, grid ? R.layout.media_item_grid_layout : R.layout.media_item_list_layout, this);
		}
		setSize(ctx, grid, size);
	}

	/**
	 * Switches to the DashCinema card style: a rounded card, the icon in a colored box, large
	 * bold titles. Used for the TV channels.
	 */
	public void setCardStyle(boolean card, boolean grid) {
		if ((this.card == card) && (cardGrid == grid)) return;
		this.card = card;
		MainActivityDelegate a = getMainActivity();
		applyLayout(getContext(), grid, a.getPrefs().getTextIconSizePref(a));
	}

	public boolean isCardStyle() {
		return card;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (card && cardGrid) {
			MediaItemListView l = getListView();
			int th = (l != null) ? l.getTileHeight() : 0;

			if (th > 0) {
				int m = 0;
				if (getLayoutParams() instanceof MarginLayoutParams lp) m = lp.topMargin + lp.bottomMargin;
				heightMeasureSpec = MeasureSpec.makeMeasureSpec(Math.max(0, th - m), MeasureSpec.EXACTLY);
			}
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	private void applyCardStyle(Context ctx) {
		int pad = UiUtils.toIntPx(ctx, 12);
		setPadding(pad, pad, pad, pad);
		iconTint = ColorStateList.valueOf(DcViews.color(ctx, R.color.dc_on_tile));
		setElevation(0);
		updateCardBackground();
		ViewGroup.LayoutParams lp = getLayoutParams();
		if (lp instanceof MarginLayoutParams m) {
			int h = UiUtils.toIntPx(ctx, cardGrid ? 6 : 16);
			int v = UiUtils.toIntPx(ctx, cardGrid ? 6 : 5);
			m.setMargins(h, v, h, v);
			setLayoutParams(lp);
		}
	}

	private void updateCardBackground() {
		Context ctx = getContext();
		int color = DcViews.color(ctx, isActivated() ? R.color.dc_tv_dim : R.color.dc_card);
		float r = UiUtils.toPx(ctx, cardGrid ? 22 : 18);
		setBackground(DcViews.card(ctx, color, r));
		setForeground(DcViews.focusOutline(ctx, r));
	}

	@Override
	public void setLayoutParams(ViewGroup.LayoutParams params) {
		if (card && (params instanceof MarginLayoutParams m)) {
			Context ctx = getContext();
			int h = UiUtils.toIntPx(ctx, cardGrid ? 6 : 16);
			int v = UiUtils.toIntPx(ctx, cardGrid ? 6 : 5);
			m.setMargins(h, v, h, v);
		}
		super.setLayoutParams(params);
	}

	/**
	 * The icon box of a card: the color of the item (by its name) for the default icon, a neutral
	 * box for a logo.
	 */
	private void setCardIconBox(@Nullable Item i, boolean logo) {
		if (!card) return;
		Context ctx = getContext();
		ImageView icon = getIcon();
		int color;
		if (logo || (i == null)) {
			color = DcViews.color(ctx, R.color.dc_surface_high);
		} else {
			int[] palette = {R.color.dc_tv, R.color.dc_web, R.color.dc_youtube, R.color.dc_pink,
					R.color.dc_orange, R.color.dc_disney, R.color.dc_other, R.color.dc_canal};
			color = DcViews.color(ctx, palette[Math.floorMod(i.getName().hashCode(), palette.length)]);
		}
		GradientDrawable bg = new GradientDrawable();
		bg.setColor(color);
		bg.setCornerRadius(UiUtils.toPx(ctx, 14));
		icon.setBackground(bg);
		int pad = UiUtils.toIntPx(ctx, logo ? 6 : 14);
		icon.setPadding(pad, pad, pad, pad);
		icon.setClipToOutline(true);
	}

	public void setSize(Context ctx, boolean grid, float size) {
		if (card) {
			TextView t = getTitle();
			t.setTypeface(ResourcesCompat.getFont(ctx, R.font.plus_jakarta_sans_extrabold));
			t.setTextSize(COMPLEX_UNIT_PX, UiUtils.toPx(ctx, 16) * size);
			t.setTextColor(DcViews.color(ctx, R.color.dc_cream));
			t = getSubtitle();
			t.setTypeface(ResourcesCompat.getFont(ctx, R.font.plus_jakarta_sans));
			t.setTextSize(COMPLEX_UNIT_PX, UiUtils.toPx(ctx, 13) * size);
			t.setTextColor(DcViews.color(ctx, R.color.dc_tv_text));
			return;
		}
		setTextAppearance(ctx, getTitle(), titleTextAppearance, size);
		setTextAppearance(ctx, getSubtitle(), subtitleTextAppearance, size);
		if (!grid) {
			int iconSize = (int) (getTitle().getTextSize() + getSubtitle().getTextSize() + toPx(ctx,
					10));
			ImageView i = getIcon();
			ViewGroup.LayoutParams lp = i.getLayoutParams();
			lp.height = iconSize;
			lp.width = iconSize;
			i.setLayoutParams(lp);
		}
	}

	private void setTextAppearance(Context ctx, TextView v, @StyleRes int res, float scale) {
		v.setTextAppearance(res);
		v.setTextSize(COMPLEX_UNIT_PX, getTextAppearanceSize(ctx, res) * scale);
		v.setTextColor(textTint);
	}

	public void setHolder(@Nullable MediaItemViewHolder holder) {
		this.holder = holder;
	}

	@Nullable
	public MediaItemViewHolder getHolder() {
		return holder;
	}

	@Nullable
	public MediaItemListView getListView() {
		MediaItemViewHolder h = getHolder();
		return (h != null) ? h.getListView() : null;
	}

	@Nullable
	public MediaItemWrapper getItemWrapper() {
		MediaItemViewHolder h = getHolder();
		return (h != null) ? h.getItemWrapper() : null;
	}

	@Nullable
	public Item getItem() {
		MediaItemWrapper w = getItemWrapper();
		return (w != null) ? w.getItem() : null;
	}

	public void rebind(@Nullable MediaItemWrapper oldItem, @Nullable MediaItemWrapper newItem) {
		if (oldItem == newItem) return;
		cancelLoading();
		if (oldItem != null) oldItem.getItem().removeChangeListener(this);
		if ((newItem == null) || (getVisibility() != VISIBLE)) return;
		boolean hasListener = newItem.getItem().addChangeListener(this);
		load(newItem, !hasListener).onCompletion((r, err) -> {
			if (getItemWrapper() != newItem) return;
			if ((err != null) && !isCancellation(err))
				Log.e(err, "Failed to load media description: ", newItem);
		});
	}

	@Override
	public void mediaItemChanged(Item i) {
		MediaItemWrapper w = getItemWrapper();
		if ((w != null) && (w.getItem() == i)) load(w, false);
	}

	private void cancelLoading() {
		if (loading == null) return;
		loading.cancel();
		loading = null;
	}

	private FutureSupplier<MediaDescriptionCompat> load(MediaItemWrapper w, boolean showLoading) {
		cancelLoading();
		Item i = w.getItem();

		if (i instanceof EpgItem) {
			EpgItem e = (EpgItem) i;
			setProgress(i, e.getStartTime(), e.getEndTime());
		} else {
			setProgress(i, 0, 0);
		}

		setCardIconBox(i, false);
		FutureSupplier<MediaDescriptionCompat> load =
				loading = i.getMediaDescription().main().addConsumer((md, fail, p, total) -> {
					if (getItemWrapper() != w) return;

					if (fail != null) {
						if (isCancellation(fail)) return;
						Log.e(fail, "Failed to load media description: ", i);
						setDefaults(i, false);
						return;
					}

					if ((p == PROGRESS_DONE) || (p == 1)) {
						getTitle().setText(ifNull(md.getTitle(), i::getName));
					}
					if ((p == PROGRESS_DONE) || (p == 2)) {
						setSubtitleText(md.getSubtitle());
					}
					if ((p == PROGRESS_DONE) || (p == 3)) {
						Bundle b = md.getExtras();
						if (b != null) setProgress(i, b.getLong(STREAM_START_TIME),
								b.getLong(STREAM_END_TIME));
					}
					if ((p == PROGRESS_DONE) || (p == 4)) {
						Uri uri = md.getIconUri();

						if ((uri != null) && !SCHEME_ANDROID_RESOURCE.equals(uri.getScheme())) {
							FutureSupplier<Bitmap> loadIcon =
									i.getLib().getBitmap(uri.toString(), true, true).main()
											.onCompletion((bm, err) -> {
												if (getItemWrapper() != w) return;

												ImageView icon = getIcon();
												icon.clearAnimation();
												cancelLoading();

												if (bm != null) {
													setCardIconBox(i, true);
													icon.setImageTintList(null);
													icon.setImageBitmap(bm);
												} else {
													icon.setImageTintList(iconTint);
													icon.setImageResource(i.getIcon());
												}
											});

							if (!loadIcon.isDone()) {
								ImageView icon = getIcon();
								icon.clearAnimation();
								icon.setImageTintList(iconTint);
								icon.setImageResource(i.getIcon());
							}
						} else {
							ImageView icon = getIcon();
							icon.clearAnimation();
							icon.setImageTintList(iconTint);
							icon.setImageResource(i.getIcon());
							cancelLoading();
						}
					}
				});

		if (!load.isDone()) setDefaults(i, showLoading);
		return load;
	}

	private void setProgress(Item i, long start, long end) {
		LinearProgressIndicator p = getProgress();

		if (progressUpdater != null) {
			progressUpdater.cancel();
			progressUpdater = null;
		}

		if ((start > 0) && (end > start)) {
			progressUpdater = new ProgressUpdater(i, p, start, end);
			progressUpdater.run();
			p.setVisibility(VISIBLE);
		} else {
			p.setVisibility(GONE);
		}
	}

	private void setDefaults(Item i, boolean loading) {
		ImageView icon = getIcon();
		getTitle().setText(i.getName());

		if (loading) {
			rotate.setDuration(1000);
			rotate.setRepeatCount(Animation.INFINITE);
			icon.setImageDrawable(getLoadingDrawable(getContext()));
			icon.startAnimation(rotate);
			setSubtitleText(R.string.loading);
		} else {
			icon.clearAnimation();
			icon.setImageTintList(iconTint);
			icon.setImageResource(i.getIcon());
			setSubtitleText("");
		}
	}

	private static Drawable getLoadingDrawable(Context ctx) {
		if (loadingDrawable == null) {
			loadingDrawable = ContextCompat.getDrawable(ctx, R.drawable.loading);
			Objects.requireNonNull(loadingDrawable).setTint(iconColor);
			MainActivityDelegate.get(ctx)
					.addBroadcastListener(MediaItemView::onActivityDestroyEvent, ACTIVITY_DESTROY);
		}

		return loadingDrawable;
	}

	private static void onActivityDestroyEvent(ActivityDelegate a, long e) {
		loadingDrawable = null;
		a.removeBroadcastListener(MediaItemView::onActivityDestroyEvent);
	}

	private void setSubtitleText(CharSequence text) {
		TextView t = getSubtitle();
		t.setText(text);
		// A card without a subtitle has the title in the middle, the tiles show only the name
		if (card) t.setVisibility(((text == null) || (text.length() == 0) || cardGrid) ? GONE : VISIBLE);
	}

	private void setSubtitleText(int text) {
		setSubtitleText(getContext().getText(text));
	}

	public ImageView getIcon() {
		return (ImageView) getChildAt(0);
	}

	public TextView getTitle() {
		return (TextView) getChildAt(1);
	}

	public TextView getSubtitle() {
		return (TextView) getChildAt(2);
	}

	public LinearProgressIndicator getProgress() {
		return (LinearProgressIndicator) getChildAt(3);
	}

	public MaterialCheckBox getCheckBox() {
		return (MaterialCheckBox) getChildAt(4);
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		MediaItemWrapper w = getItemWrapper();
		if (w != null) w.setSelected(isChecked, false);
	}

	@Override
	protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
		super.onVisibilityChanged(changedView, visibility);
		Item item = getItem();
		if ((visibility == VISIBLE) && (item != null)) refresh();
	}

	@Override
	public void onDrawForeground(Canvas canvas) {
		super.onDrawForeground(canvas);
		Item item = getItem();
		VectorDrawableCompat d;

		if ((item instanceof ArchiveItem) && !((ArchiveItem) item).isExpired()) {
			d = archiveLabelDrawable;
			if (d == null) {
				d = archiveLabelDrawable =
						VectorDrawableCompat.create(getResources(), R.drawable.archive_label, null);
				if (d == null) return;
				d.setTint(hintColor);
			}
		} else {
			if (!(item instanceof PlayableItem) || ((PlayableItem) item).isStream()) return;
			PlayableItem p = (PlayableItem) item;
			if (!p.isVideo()) return;

			PlayableItemPrefs prefs = p.getPrefs();

			if (prefs.getWatchedPref()) {
				d = watchedVideoDrawable;
				if (d == null) {
					d = watchedVideoDrawable =
							VectorDrawableCompat.create(getResources(), R.drawable.done, null);
					if (d == null) return;
					d.setTint(hintColor);
				}
			} else if (prefs.getPositionPref() > 0) {
				d = watchingVideoDrawable;
				if (d == null) {
					d = watchingVideoDrawable =
							VectorDrawableCompat.create(getResources(), R.drawable.watching, null);
					if (d == null) return;
					d.setTint(hintColor);
				}
			} else {
				return;
			}
		}

		ImageView i = getIcon();
		int l = i.getLeft();
		int t = i.getTop();
		int r = i.getRight();
		int b = i.getBottom();
		d.setBounds(l + (r - l) / 3, t + (b - t) / 3, r, b);
		d.draw(canvas);
	}

	public void refresh() {
		MediaItemWrapper w = getItemWrapper();
		if (w == null) return;
		Item item = w.getItem();
		if ((item instanceof PlayableItem) && ((PlayableItem) item).isVideo()) {
			rebind(w, null);
			rebind(null, w);
		}
		refreshState(item);
	}

	private void refreshState(Item item) {
		MainActivityDelegate a = getMainActivity();
		refreshCheckbox();

		if (item instanceof EpgItem) {
			EpgItem e = (EpgItem) item;
			EpgItem prev = e.getPrev();
			boolean arch = e instanceof ArchiveItem;
			long time = System.currentTimeMillis();
			long end = e.getEndTime();
			long updateDelay = 0;
			if ((end > time) && (e.getStartTime() <= time)) updateDelay = end - time;

			if (arch && (!(prev instanceof ArchiveItem))) {
				long delay = ((ArchiveItem) e).getExpirationTime() - time;
				if ((delay > 0) && (delay > updateDelay)) updateDelay = delay;
			}

			if (updateDelay > 0) {
				setState(Typeface.BOLD, false);
				a.postDelayed(() -> {
					if (getItem() != e) return;
					refreshState(e);
					EpgItem next = e.getNext();

					if (next != null) {
						ActivityFragment f = a.getActiveFragment();
						if (f instanceof MediaLibFragment) {
							MediaItemView focus = ((MediaLibFragment) f).getListView().focusTo(next);
							if (focus != null) focus.refresh();
						}
					}
				}, updateDelay);

				if (!arch) {
					setSelected(false);
					setState(Typeface.NORMAL, false);
					return;
				}
			}
		}

		var cur = a.getCurrentPlayable();
		if (item.equals(cur)) {
			setSelected(true);
			setState(Typeface.BOLD, true);
		} else {
			boolean last =
					(cur == null) && ((item instanceof PlayableItem) && ((PlayableItem) item).isLastPlayed());
			int type = last ? Typeface.BOLD : Typeface.NORMAL;
			setSelected(false);
			setState(type, last);
		}
	}

	private void setState(int surfaceType, boolean activated) {
		if (card) {
			getTitle().setActivated(activated);
			getSubtitle().setActivated(activated);
			setActivated(activated);
			updateCardBackground();
			return;
		}
		TextView t = getTitle();
		t.setTypeface(null, surfaceType);
		t.setActivated(activated);
		t = getSubtitle();
		t.setTypeface(null, surfaceType);
		t.setActivated(activated);
		setActivated(activated);
	}

	public void refreshCheckbox() {
		MediaItemWrapper w = getItemWrapper();
		if (w == null) return;
		MaterialCheckBox cb = getCheckBox();

		if (!w.isSelectionSupported()) {
			cb.setVisibility(GONE);
			return;
		}

		MediaItemListView l = getListView();

		if ((l != null) && l.isSelectionActive()) {
			cb.setVisibility(VISIBLE);
			cb.setChecked(w.isSelected());
		} else {
			cb.setVisibility(GONE);
		}
	}

	@Override
	public boolean onLongClick(View v) {
		MainActivityDelegate a = getMainActivity();
		OverlayMenu menu = a.getContextMenu();
		MediaItemMenuHandler handler = new MediaItemMenuHandler(menu, this);
		MediaItemListView l = getListView();
		if (l != null) getListView().discardSelection();
		handler.show();
		return true;
	}

	@Override
	protected void onFocusChanged(boolean gainFocus, int direction,
																@Nullable Rect previouslyFocusedRect) {
		super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
		if (!BuildConfig.AUTO || !gainFocus) return;
		MainActivityDelegate d = getMainActivity();
		if (d.getPrefs().useDpadCursor(d)) {
			View c = d.findViewById(R.id.cursor);
			if ((c != null) && (c.getVisibility() == VISIBLE)) return;
		}

		// The next item in the list must always be visible for rotary input scrolling
		MediaItemListView lv = getListView();
		if (lv == null) return;
		int idx = lv.indexOfChild(this);

		if (idx == 0) {
			List<MediaItemWrapper> l = lv.getAdapter().getList();
			idx = l.indexOf(getItemWrapper());
			if (idx == 0) return;
			idx--;
		} else if (idx == (lv.getChildCount() - 1)) {
			List<MediaItemWrapper> l = lv.getAdapter().getList();
			idx = l.indexOf(getItemWrapper());
			if (idx == (l.size() - 1)) return;
			idx++;
		} else {
			return;
		}

		lv.scrollToPosition(idx, false);
	}

	private MainActivityDelegate getMainActivity() {
		return MainActivityDelegate.get(getContext());
	}

	private final class ProgressUpdater implements Runnable, Cancellable {
		private final Item item;
		private final LinearProgressIndicator progress;
		private final long start;
		private final long end;
		private Cancellable scheduled;

		private ProgressUpdater(Item item, LinearProgressIndicator progress, long start, long end) {
			this.item = item;
			this.progress = progress;
			this.start = start;
			this.end = end;
		}

		@Override
		public void run() {
			if (!isValid()) return;
			long time = System.currentTimeMillis();

			if (time >= end) {
				progressUpdater = null;
				progress.setProgressCompat(100, true);
				update();
			} else if (start > time) {
				progress.setProgressCompat(0, true);
				runAt(time, start);
			} else {
				long dur = end - start;
				int prog = (int) ((time - start) * 100 / dur);
				progress.setProgressCompat(prog, true);

				if (prog >= 99) {
					runAt(time, end);
				} else {
					long upTime = start + (prog + 1) * dur / 100;
					runAt(time, Math.min(upTime, end));
				}
			}
		}

		@Override
		public boolean cancel() {
			return (scheduled != null) && scheduled.cancel();
		}

		private boolean isValid() {
			return (progressUpdater == this) && (item == getItem()) && (item != null);
		}

		private void runAt(long curTime, long atTime) {
			if (scheduled != null) scheduled.cancel();
			scheduled = getMainActivity().postDelayed(this, atTime - curTime);
		}

		private void update() {
			item.getMediaDescription().main().onSuccess(d -> {
				if (!isValid()) return;
				Bundle b = d.getExtras();
				if (b != null) {
					long start = b.getLong(STREAM_START_TIME);
					long end = b.getLong(STREAM_END_TIME);
					if ((start == this.start) && (end == this.end)) return;
				}
				refresh();
			});
		}
	}
}
