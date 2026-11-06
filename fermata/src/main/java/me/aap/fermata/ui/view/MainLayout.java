package me.aap.fermata.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import me.aap.fermata.R;

/**
 * The root layout of the main activity. Every touch and key event shows the {@link NavPanel}.
 */
public class MainLayout extends ConstraintLayout {

	public MainLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent e) {
		NavPanel p = findViewById(R.id.nav_panel);
		if (p == null) return super.dispatchTouchEvent(e);
		if (p.interceptParentTouch(e)) return true;
		// The tap, that shows the hidden panel, must not click the panel's buttons
		boolean hidden = p.getVisibility() != VISIBLE;
		boolean handled = super.dispatchTouchEvent(e);
		if (hidden && (e.getActionMasked() != MotionEvent.ACTION_DOWN)) return handled;
		p.show();
		return handled;
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent e) {
		NavPanel p = findViewById(R.id.nav_panel);
		if ((p != null) && (e.getAction() == KeyEvent.ACTION_DOWN)) p.show();
		return super.dispatchKeyEvent(e);
	}

	@Override
	public boolean dispatchGenericMotionEvent(MotionEvent e) {
		NavPanel p = findViewById(R.id.nav_panel);
		if (p != null) p.show();
		return super.dispatchGenericMotionEvent(e);
	}
}
