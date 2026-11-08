package me.aap.fermata.ui.fragment;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import me.aap.fermata.R;
import me.aap.fermata.ui.activity.MainActivityDelegate;
import me.aap.utils.ui.UiUtils;

/**
 * The tabs of My media: Folders, Favorites and Playlists, on top of their lists.
 */
public final class MediaTabsView extends LinearLayout {

	private MediaTabsView(Context ctx, @IdRes int active) {
		super(ctx);
		setId(R.id.media_tabs);
		setOrientation(HORIZONTAL);
		int pad = UiUtils.toIntPx(ctx, 4);
		GradientDrawable bg = new GradientDrawable();
		bg.setColor(ContextCompat.getColor(ctx, R.color.dc_surface));
		bg.setCornerRadius(UiUtils.toPx(ctx, 16));
		setBackground(bg);
		setPadding(pad, pad, pad, pad);
		addTab(ctx, R.id.folders_fragment, R.string.folders, active);
		addTab(ctx, R.id.favorites_fragment, R.string.favorites, active);
		addTab(ctx, R.id.playlists_fragment, R.string.playlists, active);
	}

	/**
	 * Returns a vertical layout with the tabs on top of the list.
	 */
	static View wrap(View list, @IdRes int active) {
		Context ctx = list.getContext();
		LinearLayout l = new LinearLayout(ctx);
		l.setOrientation(VERTICAL);
		LayoutParams tlp = new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
		int m = UiUtils.toIntPx(ctx, 12);
		tlp.setMargins(m, m, m, m / 2);
		l.addView(new MediaTabsView(ctx, active), tlp);
		l.addView(list, new LayoutParams(MATCH_PARENT, 0, 1));
		return l;
	}

	private void addTab(Context ctx, @IdRes int id, @StringRes int title, @IdRes int active) {
		boolean selected = id == active;
		TextView t = new TextView(ctx);
		t.setText(title);
		t.setGravity(Gravity.CENTER);
		t.setSingleLine();
		t.setTypeface(ResourcesCompat.getFont(ctx,
				selected ? R.font.plus_jakarta_sans_extrabold : R.font.plus_jakarta_sans_semibold));
		t.setTextSize(15);
		t.setMinHeight(UiUtils.toIntPx(ctx, 44));
		t.setTextColor(ContextCompat.getColor(ctx, selected ? R.color.dc_on_tile : R.color.dc_cream));
		t.setSelected(selected);
		t.setFocusable(true);
		t.setClickable(true);
		if (selected) {
			GradientDrawable bg = new GradientDrawable();
			bg.setColor(ContextCompat.getColor(ctx, R.color.dc_orange));
			bg.setCornerRadius(UiUtils.toPx(ctx, 12));
			t.setBackground(bg);
		} else {
			t.setOnClickListener(v -> MainActivityDelegate.get(v.getContext()).showFragment(id));
		}
		addView(t, new LayoutParams(0, WRAP_CONTENT, 1));
	}
}
