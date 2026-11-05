package me.aap.fermata.auto;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Debug only. Emulates the Android Auto surface, delivering the same events, as
 * {@link FullScreenCarService}: tap, scroll and fling, plus the Home and Back actions.
 * <p>
 * adb shell am start -n <package>/me.aap.fermata.auto.CarDisplayPreviewActivity \
 * --ei w 1920 --ei h 720 --ei dpi 200
 */
public class CarDisplayPreviewActivity extends AppCompatActivity implements SurfaceHolder.Callback {
	private int width;
	private int height;
	private int dpi;
	private SurfaceView view;

	@SuppressLint("ClickableViewAccessibility")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		width = getIntent().getIntExtra("w", 1920);
		height = getIntent().getIntExtra("h", 720);
		dpi = getIntent().getIntExtra("dpi", 200);

		var root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setBackgroundColor(Color.DKGRAY);
		var buttons = new LinearLayout(this);
		var home = new Button(this);
		home.setText("Home");
		home.setOnClickListener(v -> CarDisplay.get().home());
		var back = new Button(this);
		back.setText("Back");
		back.setOnClickListener(v -> CarDisplay.get().back());
		buttons.addView(home);
		buttons.addView(back);
		root.addView(buttons);

		var frame = new FrameLayout(this);
		view = new SurfaceView(this);
		view.getHolder().setFixedSize(width, height);
		view.getHolder().addCallback(this);
		frame.addView(view, new FrameLayout.LayoutParams(0, 0, Gravity.CENTER));
		frame.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
			// Keep the car screen aspect ratio
			float scale = Math.min((r - l) / (float) width, (b - t) / (float) height);
			var lp = view.getLayoutParams();
			int w = (int) (width * scale);
			int h = (int) (height * scale);
			if ((lp.width != w) || (lp.height != h)) {
				lp.width = w;
				lp.height = h;
				view.post(() -> view.setLayoutParams(lp));
			}
		});
		root.addView(frame, new LinearLayout.LayoutParams(-1, 0, 1));
		setContentView(root);

		var gd = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onDown(@NonNull MotionEvent e) {
				return true;
			}

			@Override
			public boolean onSingleTapUp(@NonNull MotionEvent e) {
				CarDisplay.get().click(x(e.getX()), y(e.getY()));
				return true;
			}

			@Override
			public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2, float dx, float dy) {
				CarDisplay.get().scroll(x(dx), y(dy));
				return true;
			}

			@Override
			public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float vx, float vy) {
				CarDisplay.get().fling(x(vx), y(vy));
				return true;
			}
		});
		view.setOnTouchListener((v, e) -> gd.onTouchEvent(e));
	}

	private float x(float x) {
		return x * width / view.getWidth();
	}

	private float y(float y) {
		return y * height / view.getHeight();
	}

	@Override
	protected void onDestroy() {
		CarDisplay.get().close();
		super.onDestroy();
	}

	@Override
	public void surfaceCreated(@NonNull SurfaceHolder holder) {
	}

	@Override
	public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int w, int h) {
		CarDisplay.get().setSurface(holder.getSurface(), width, height, dpi);
		CarDisplay.get().setContentArea(new Rect(0, 0, width, height));
	}

	@Override
	public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
		CarDisplay.get().releaseSurface(holder.getSurface());
	}
}
