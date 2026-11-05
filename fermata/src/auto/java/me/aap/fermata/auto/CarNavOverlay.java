package me.aap.fermata.auto;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static me.aap.utils.ui.UiUtils.toPx;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;

import me.aap.fermata.R;
import me.aap.fermata.ui.activity.MainActivityDelegate;

/**
 * Home, Back and Menu buttons, shown over the content of the projection based car apps, like the
 * action strip of the full screen car apps. It replaces the floating button, that is hidden in
 * the car, and stays visible in the video players.
 */
@SuppressLint("ViewConstructor")
final class CarNavOverlay extends LinearLayout {

	CarNavOverlay(MainActivityDelegate a) {
		super(a.getContext());
		Context ctx = getContext();
		setId(R.id.car_nav_overlay);
		setOrientation(HORIZONTAL);
		setElevation(toPx(ctx, 20));

		int pad = (int) toPx(ctx, 4);
		setPadding(pad, pad, pad, pad);
		GradientDrawable bg = new GradientDrawable();
		bg.setColor(0x99000000);
		bg.setCornerRadius(toPx(ctx, 32));
		setBackground(bg);

		int size = (int) (toPx(ctx, 48) * a.getTextIconSize());
		addButton(ctx, size, R.drawable.home, a::goHome);
		addButton(ctx, size, R.drawable.back, a::onBackPressed);
		addButton(ctx, size, R.drawable.car_menu, a::showNavMenu);

		var lp = new ConstraintLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
		int margin = (int) toPx(ctx, 12);
		// Where the floating button was, so the tool bar buttons are not covered
		lp.bottomToTop = R.id.control_panel;
		lp.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
		lp.bottomMargin = margin;
		lp.setMarginEnd(margin);
		setLayoutParams(lp);
	}

	private void addButton(Context ctx, int size, @DrawableRes int icon, Runnable action) {
		var v = new AppCompatImageView(ctx);
		int pad = size / 4;
		v.setImageResource(icon);
		v.setPadding(pad, pad, pad, pad);
		v.setScaleType(ImageView.ScaleType.FIT_CENTER);
		v.setBackgroundResource(me.aap.utils.R.drawable.focusable_shape_transparent);
		v.setFocusable(true);
		v.setClickable(true);
		v.setOnClickListener(b -> action.run());
		addView(v, new LayoutParams(size, size));
	}
}
