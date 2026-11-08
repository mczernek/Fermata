package me.aap.fermata.ui.view;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import me.aap.fermata.R;
import me.aap.utils.ui.UiUtils;

/**
 * The DashCinema building blocks, shared by the screens: the screen header (a large title, an
 * optional back button and action buttons on the right), round icon buttons, pill buttons, chips
 * and the rounded card backgrounds. See docs/design.md.
 */
public final class DcViews {
	/**
	 * The minimum size of anything that can be tapped, large enough for a car screen.
	 */
	public static final int TOUCH_DP = 48;

	private DcViews() {
	}

	/**
	 * A screen header: [back] Title ........ [actions]. The actions are added to the returned
	 * layout with {@link #addAction(LinearLayout, View)}.
	 */
	public static LinearLayout header(Context ctx, CharSequence title, @Nullable Runnable back) {
		LinearLayout l = new LinearLayout(ctx);
		// The navigation panel in a top corner is placed below the header
		l.setId(R.id.dc_header);
		l.setOrientation(LinearLayout.HORIZONTAL);
		l.setGravity(Gravity.CENTER_VERTICAL);
		int pad = UiUtils.toIntPx(ctx, 16);
		l.setPaddingRelative(pad, UiUtils.toIntPx(ctx, 12), pad + topEndInset(ctx),
				UiUtils.toIntPx(ctx, 4));
		l.setMinimumHeight(UiUtils.toIntPx(ctx, 64));

		if (back != null) {
			View b = iconButton(ctx, R.drawable.dc_chevron_left, R.color.dc_cream,
					ctx.getString(R.string.nav_panel_back), v -> back.run());
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
			lp.setMarginEnd(UiUtils.toIntPx(ctx, 10));
			l.addView(b, lp);
		}

		TextView t = new TextView(ctx);
		t.setId(R.id.dc_header_title);
		t.setText(title);
		t.setTypeface(ResourcesCompat.getFont(ctx, R.font.barlow_condensed_bold));
		t.setTextColor(color(ctx, R.color.dc_cream));
		t.setTextSize((back == null) ? 34 : 28);
		t.setSingleLine();
		t.setEllipsize(TextUtils.TruncateAt.END);
		t.setAccessibilityHeading(true);
		l.addView(t, new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1));
		return l;
	}

	/**
	 * The space to leave free in the top end corner (px), see
	 * {@link me.aap.fermata.ui.activity.FermataActivity#getTopEndInsetDp()}.
	 */
	public static int topEndInset(Context ctx) {
		return UiUtils.toIntPx(ctx,
				me.aap.fermata.ui.activity.MainActivityDelegate.get(ctx).getAppActivity()
						.getTopEndInsetDp());
	}

	public static void addAction(LinearLayout header, View action) {
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
		lp.setMarginStart(UiUtils.toIntPx(header.getContext(), 8));
		header.addView(action, lp);
	}

	/**
	 * A round 48dp button with an icon on the surface color.
	 */
	public static View iconButton(Context ctx, @DrawableRes int icon, @ColorRes int tint,
																CharSequence description, View.OnClickListener click) {
		int size = UiUtils.toIntPx(ctx, TOUCH_DP);
		FrameLayout f = new FrameLayout(ctx);
		f.setLayoutParams(new LinearLayout.LayoutParams(size, size));
		f.setMinimumWidth(size);
		f.setMinimumHeight(size);
		f.setBackground(card(ctx, color(ctx, R.color.dc_surface), size / 2f));
		f.setContentDescription(description);
		clickable(f, size / 2f, click);
		ImageView i = new ImageView(ctx);
		i.setImageResource(icon);
		i.setImageTintList(ColorStateList.valueOf(color(ctx, tint)));
		i.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
		int is = UiUtils.toIntPx(ctx, 22);
		f.addView(i, new FrameLayout.LayoutParams(is, is, Gravity.CENTER));
		return f;
	}

	/**
	 * A pill button: [icon] Text, on the surface color.
	 */
	public static TextView pillButton(Context ctx, @DrawableRes int icon, @ColorRes int iconTint,
																		CharSequence text, View.OnClickListener click) {
		TextView t = new TextView(ctx);
		int h = UiUtils.toIntPx(ctx, TOUCH_DP);
		t.setText(text);
		t.setMinHeight(h);
		t.setMinimumHeight(h);
		t.setIncludeFontPadding(false);
		t.setGravity(Gravity.CENTER_VERTICAL);
		t.setSingleLine();
		t.setTextSize(15);
		t.setTypeface(ResourcesCompat.getFont(ctx, R.font.plus_jakarta_sans_extrabold));
		t.setTextColor(color(ctx, R.color.dc_cream));
		int pad = UiUtils.toIntPx(ctx, 16);
		int v = UiUtils.toIntPx(ctx, 12);
		t.setPadding(pad, v, UiUtils.toIntPx(ctx, 18), v);
		Drawable d = ContextCompat.getDrawable(ctx, icon);

		if (d != null) {
			d = d.mutate();
			d.setTint(color(ctx, iconTint));
			int s = UiUtils.toIntPx(ctx, 20);
			d.setBounds(0, 0, s, s);
			t.setCompoundDrawablesRelative(d, null, null, null);
			t.setCompoundDrawablePadding(UiUtils.toIntPx(ctx, 8));
		}

		t.setBackground(card(ctx, color(ctx, R.color.dc_surface), h / 2f));
		clickable(t, h / 2f, click);
		return t;
	}

	/**
	 * A chip of a chip row: the selected one is filled with the color, the others are on the
	 * surface color.
	 */
	public static TextView chip(Context ctx, CharSequence text, boolean selected, @ColorInt int color,
															View.OnClickListener click) {
		TextView t = new TextView(ctx);
		int h = UiUtils.toIntPx(ctx, TOUCH_DP);
		t.setText(text);
		t.setMinHeight(h);
		t.setMinimumHeight(h);
		t.setIncludeFontPadding(false);
		t.setGravity(Gravity.CENTER);
		t.setSingleLine();
		t.setTextSize(15);
		t.setTypeface(ResourcesCompat.getFont(ctx,
				selected ? R.font.plus_jakarta_sans_extrabold : R.font.plus_jakarta_sans_semibold));
		t.setTextColor(selected ? color(ctx, R.color.dc_on_tile) : color(ctx, R.color.dc_chip_text));
		int pad = UiUtils.toIntPx(ctx, 18);
		int v = UiUtils.toIntPx(ctx, 12);
		t.setPadding(pad, v, pad, v);
		t.setSelected(selected);
		t.setBackground(card(ctx, selected ? color : color(ctx, R.color.dc_surface), h / 2f));
		clickable(t, h / 2f, click);
		return t;
	}

	/**
	 * A rounded background with the orange ripple.
	 */
	public static RippleDrawable card(Context ctx, @ColorInt int color, float radiusPx) {
		GradientDrawable shape = new GradientDrawable();
		shape.setColor(color);
		shape.setCornerRadius(radiusPx);
		GradientDrawable mask = new GradientDrawable();
		mask.setColor(0xFFFFFFFF);
		mask.setCornerRadius(radiusPx);
		return new RippleDrawable(ColorStateList.valueOf(color(ctx, R.color.dc_orange_ripple)), shape,
				mask);
	}

	/**
	 * The cream outline shown when the view is focused with the D-pad or a rotary controller.
	 */
	public static Drawable focusOutline(Context ctx, float radiusPx) {
		StateListDrawable fg = new StateListDrawable();
		GradientDrawable focus = new GradientDrawable();
		focus.setCornerRadius(radiusPx);
		focus.setStroke(UiUtils.toIntPx(ctx, 3), color(ctx, R.color.dc_cream));
		fg.addState(new int[]{android.R.attr.state_focused}, focus);
		fg.addState(new int[]{}, new ColorDrawable(0));
		return fg;
	}

	public static void clickable(View v, float radiusPx, @Nullable View.OnClickListener click) {
		v.setClickable(true);
		v.setFocusable(true);
		v.setForeground(focusOutline(v.getContext(), radiusPx));
		if (click != null) v.setOnClickListener(click);
	}

	@ColorInt
	public static int color(Context ctx, @ColorRes int c) {
		return ContextCompat.getColor(ctx, c);
	}

	public static LinearLayout.LayoutParams matchWidth() {
		return new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
	}
}
