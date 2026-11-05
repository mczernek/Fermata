package me.aap.fermata.auto;

import android.content.Context;
import android.os.SystemClock;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.widget.AppCompatEditText;

/**
 * An EditText rendered on the car display. The phone keyboard is not visible in the car,
 * so the text is entered with the car keyboard (see {@link CarDisplay#startInput}).
 */
public class CarInputEditText extends AppCompatEditText {
	private OnKeyListener keyListener;

	public CarInputEditText(Context context) {
		super(context);
		setShowSoftInputOnFocus(false);
		setOnClickListener(v -> CarDisplay.get().startInput(this));
		setOnFocusChangeListener((v, hasFocus) -> {
			if (hasFocus) CarDisplay.get().startInput(this);
		});
	}

	@Override
	public void setOnKeyListener(OnKeyListener l) {
		super.setOnKeyListener(l);
		keyListener = l;
	}

	/**
	 * Called when the search is submitted on the car keyboard.
	 */
	void submit() {
		if (keyListener != null) {
			long time = SystemClock.uptimeMillis();
			KeyEvent e = new KeyEvent(time, time, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER, 0, 0,
					KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
					KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE |
							KeyEvent.FLAG_EDITOR_ACTION);
			if (keyListener.onKey(this, KeyEvent.KEYCODE_ENTER, e)) return;
		}
		onEditorAction(EditorInfo.IME_ACTION_DONE);
	}
}
