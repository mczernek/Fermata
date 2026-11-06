package me.aap.fermata.ui.view;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static me.aap.utils.ui.UiUtils.toPx;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;

import me.aap.fermata.R;
import me.aap.fermata.ui.activity.MainActivityDelegate;
import me.aap.utils.function.IntSupplier;
import me.aap.utils.pref.PreferenceStore;
import me.aap.utils.pref.PreferenceStore.Pref;
import me.aap.utils.pref.SharedPreferenceStore;

/**
 * Home, Back and Menu buttons in one panel, shown over the content on the phone and in all the car
 * apps. It replaces the floating button.
 * <p>
 * The panel is shown, when the screen is touched (see {@link MainLayout}), and hidden after a few
 * seconds of inactivity, like the action strip of the car apps. It is dragged to another corner of
 * the content area. The full screen car apps don't receive the touch position of the drag events,
 * so the panel has a move button there: tap it, then tap the screen near the target corner.
 */
@SuppressLint("ViewConstructor")
public final class NavPanel extends LinearLayout {
	public static final int TOP_START = 0;
	public static final int TOP_END = 1;
	public static final int BOTTOM_START = 2;
	public static final int BOTTOM_END = 3;
	private static final long HIDE_DELAY = 5000;
	private static final int MOVE_MODE_COLOR = 0xFFFFC107;
	private static final long FADE_DURATION = 150;
	private static final long SNAP_DURATION = 200;
	private static final String PREFS_NAME = "NavPanel";
	private static final Pref<IntSupplier> CORNER = Pref.i("corner", BOTTOM_END);
	private static final Pref<IntSupplier> CAR_CORNER = Pref.i("carCorner", BOTTOM_END);
	private final MainActivityDelegate activity;
	private final PreferenceStore prefs;
	private final Pref<IntSupplier> cornerPref;
	private final boolean tapToMove;
	private final Runnable hide = this::hide;
	private final int touchSlop;
	private int corner;
	private boolean dragging;
	private float downX;
	private float downY;
	private float startTx;
	private float startTy;
	private boolean moveMode;
	private boolean moveGesture;
	private AppCompatImageView moveButton;
	@Nullable
	private ObjectAnimator fade;

	/**
	 * @param tapToMove whether the panel is moved with the move button, instead of dragging
	 */
	public NavPanel(MainActivityDelegate a, boolean tapToMove) {
		super(a.getContext());
		activity = a;
		this.tapToMove = tapToMove;
		Context ctx = getContext();
		touchSlop = ViewConfiguration.get(ctx).getScaledTouchSlop();
		prefs = SharedPreferenceStore.create(ctx, PREFS_NAME);
		cornerPref = a.getAppActivity().isCarActivity() ? CAR_CORNER : CORNER;
		corner = prefs.getIntPref(cornerPref);
		if ((corner < TOP_START) || (corner > BOTTOM_END)) corner = BOTTOM_END;

		setId(R.id.nav_panel);
		setOrientation(HORIZONTAL);
		setElevation(toPx(ctx, 20));
		int pad = (int) toPx(ctx, 4);
		setPadding(pad, pad, pad, pad);
		GradientDrawable bg = new GradientDrawable();
		bg.setColor(0x99000000);
		bg.setCornerRadius(toPx(ctx, 32));
		setBackground(bg);
		createButtons();
		setLayoutParams(createLayoutParams(corner));
		setVisibility(GONE);
	}

	/**
	 * Recreates the buttons, e.g. when the icon size is changed.
	 */
	public void createButtons() {
		removeAllViews();
		Context ctx = getContext();
		int size = (int) (toPx(ctx, 48) * activity.getTextIconSize());
		addButton(ctx, size, R.drawable.home, R.string.nav_panel_home, activity::goHome);
		addButton(ctx, size, R.drawable.nav_panel_back, R.string.nav_panel_back,
				activity::onBackPressed);
		View menu = addButton(ctx, size, R.drawable.car_menu, R.string.menu, activity::showNavMenu);
		// Like the long click on the floating button
		menu.setOnLongClickListener(v -> {
			if (!activity.getPrefs().getVoiceControlFBPref()) return false;
			activity.startVoiceAssistant();
			return true;
		});
		if (tapToMove) {
			moveButton = addButton(ctx, size, R.drawable.nav_panel_move, R.string.nav_panel_move,
					() -> setMoveMode(!moveMode));
		}
	}

	/**
	 * Shows the panel and restarts the hide timer. Called on any user interaction.
	 */
	public void show() {
		removeCallbacks(hide);
		postDelayed(hide, HIDE_DELAY);
		if ((getVisibility() == VISIBLE) && (fade == null)) return;
		if (getVisibility() != VISIBLE) {
			setAlpha(0f);
			setVisibility(VISIBLE);
		}
		fade(1f);
	}

	public void hide() {
		removeCallbacks(hide);
		if (dragging || moveMode) {
			postDelayed(hide, HIDE_DELAY);
			return;
		}
		if (getVisibility() != VISIBLE) return;
		if (hasFocus()) clearFocus();
		fade(0f);
	}

	/**
	 * The fade has its own animator, so it doesn't cancel the move to another corner.
	 */
	private void fade(float alpha) {
		if (fade != null) fade.cancel();
		var a = ObjectAnimator.ofFloat(this, ALPHA, alpha).setDuration(FADE_DURATION);
		a.addListener(new AnimatorListenerAdapter() {
			private boolean canceled;

			@Override
			public void onAnimationCancel(Animator animation) {
				canceled = true;
			}

			@Override
			public void onAnimationEnd(Animator animation) {
				if (fade == animation) fade = null;
				if (!canceled && (alpha == 0f)) setVisibility(GONE);
			}
		});
		fade = a;
		a.start();
	}

	/**
	 * Called by the parent for every touch event, before it is dispatched to the children.
	 *
	 * @return true, if the event is consumed by the move mode and must not be dispatched
	 */
	boolean interceptParentTouch(MotionEvent e) {
		int action = e.getActionMasked();

		if (moveGesture) {
			if (action == MotionEvent.ACTION_UP) {
				moveGesture = false;
				setMoveMode(false);
				snapTo(nearestCorner(e.getX(), e.getY()));
			} else if (action == MotionEvent.ACTION_CANCEL) {
				moveGesture = false;
			}
			return true;
		}

		if (moveMode && (action == MotionEvent.ACTION_DOWN) && !contains(e.getX(), e.getY())) {
			moveGesture = true;
			return true;
		}

		return false;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent e) {
		return !tapToMove && handleDrag(e);
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		if (tapToMove) return super.onTouchEvent(e);
		handleDrag(e);
		// Touches on the panel background must not reach the content below
		return true;
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		removeCallbacks(hide);
		if (fade != null) fade.cancel();
	}

	private boolean handleDrag(MotionEvent e) {
		switch (e.getActionMasked()) {
			case MotionEvent.ACTION_DOWN -> {
				dragging = false;
				downX = e.getRawX();
				downY = e.getRawY();
				startTx = getTranslationX();
				startTy = getTranslationY();
				animate().cancel();
				return false;
			}
			case MotionEvent.ACTION_MOVE -> {
				float dx = e.getRawX() - downX;
				float dy = e.getRawY() - downY;
				if (!dragging && (Math.hypot(dx, dy) > touchSlop)) {
					dragging = true;
					cancelPressed();
				}
				if (dragging) {
					setTranslationX(startTx + dx);
					setTranslationY(startTy + dy);
				}
				return dragging;
			}
			case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
				if (!dragging) return false;
				dragging = false;
				snapTo(nearestCorner(getX() + getWidth() / 2f, getY() + getHeight() / 2f));
				show();
				return true;
			}
			default -> {
				return dragging;
			}
		}
	}

	private void cancelPressed() {
		for (int i = 0, n = getChildCount(); i < n; i++) getChildAt(i).setPressed(false);
	}

	private void setMoveMode(boolean moveMode) {
		this.moveMode = moveMode;
		show();
		if (moveButton == null) return;
		if (moveMode) moveButton.setColorFilter(MOVE_MODE_COLOR);
		else moveButton.clearColorFilter();
	}

	private boolean contains(float x, float y) {
		return (x >= getX()) && (x < getX() + getWidth()) && (y >= getY()) &&
				(y < getY() + getHeight());
	}

	/**
	 * The corner of the content area, nearest to the given point of the parent.
	 */
	private int nearestCorner(float x, float y) {
		View area = getArea();
		float cx = (area == null) ? ((View) getParent()).getWidth() / 2f :
				(area.getLeft() + area.getRight()) / 2f;
		float cy = (area == null) ? ((View) getParent()).getHeight() / 2f :
				(area.getTop() + area.getBottom()) / 2f;
		return corner(x >= cx, y >= cy);
	}

	static int corner(boolean end, boolean bottom) {
		if (bottom) return end ? BOTTOM_END : BOTTOM_START;
		return end ? TOP_END : TOP_START;
	}

	/**
	 * Moves the panel to the corner, animating from its current position.
	 */
	private void snapTo(int corner) {
		View area = getArea();
		if (area == null) return;
		this.corner = corner;
		prefs.applyIntPref(cornerPref, corner);

		int margin = getMargin();
		boolean end = (corner == TOP_END) || (corner == BOTTOM_END);
		boolean bottom = (corner == BOTTOM_START) || (corner == BOTTOM_END);
		float left = end ? (area.getRight() - margin - getWidth()) : (area.getLeft() + margin);
		float top = bottom ? (area.getBottom() - margin - getHeight()) : (area.getTop() + margin);
		float x = getX();
		float y = getY();
		setLayoutParams(createLayoutParams(corner));
		setTranslationX(x - left);
		setTranslationY(y - top);
		animate().translationX(0f).translationY(0f).setDuration(SNAP_DURATION).start();
	}

	private View getArea() {
		return ((View) getParent()).findViewById(R.id.body_layout);
	}

	private int getMargin() {
		return (int) toPx(getContext(), 12);
	}

	@NonNull
	private ConstraintLayout.LayoutParams createLayoutParams(int corner) {
		var lp = new ConstraintLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
		int margin = getMargin();
		// The content area, so the tool bar, the control panel and the navigation bar are not covered
		if ((corner == TOP_END) || (corner == BOTTOM_END)) {
			lp.endToEnd = R.id.body_layout;
			lp.setMarginEnd(margin);
		} else {
			lp.startToStart = R.id.body_layout;
			lp.setMarginStart(margin);
		}
		if ((corner == BOTTOM_START) || (corner == BOTTOM_END)) {
			lp.bottomToBottom = R.id.body_layout;
			lp.bottomMargin = margin;
		} else {
			lp.topToTop = R.id.body_layout;
			lp.topMargin = margin;
		}
		return lp;
	}

	private AppCompatImageView addButton(Context ctx, int size, @DrawableRes int icon, @StringRes int desc,
												 Runnable action) {
		var v = new AppCompatImageView(ctx);
		int pad = size / 4;
		v.setImageResource(icon);
		v.setContentDescription(ctx.getString(desc));
		v.setPadding(pad, pad, pad, pad);
		v.setScaleType(ImageView.ScaleType.FIT_CENTER);
		v.setBackgroundResource(me.aap.utils.R.drawable.focusable_shape_transparent);
		v.setFocusable(true);
		v.setClickable(true);
		v.setOnClickListener(b -> {
			if (moveMode && (b != moveButton)) setMoveMode(false);
			action.run();
		});
		addView(v, new LayoutParams(size, size));
		return v;
	}
}
