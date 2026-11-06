package me.aap.fermata.ui.activity;

import android.text.TextWatcher;
import android.widget.EditText;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;

import me.aap.utils.ui.activity.AppActivity;

/**
 * @author Andrey Pavlenko
 */
public interface FermataActivity extends AppActivity {

	boolean isCarActivity();

	/**
	 * The fragment to show on start (e.g. YouTube), instead of the default one, or 0.
	 */
	@IdRes
	default int getInitialFragmentId() {
		return 0;
	}

	/**
	 * The home screen with tiles is always used, regardless of the preference. E.g. the full screen
	 * car apps have no navigation bar, so the home screen is the only way to all the options.
	 */
	default boolean isCarHomeRequired() {
		return false;
	}

	/**
	 * Whether the touch position of the drag events is delivered. It's not in the full screen car
	 * apps, that only receive the scroll distances.
	 */
	default boolean isTouchDragSupported() {
		return true;
	}

	void setRequestedOrientation(int requestedOrientation);

	@Nullable
	default EditText startInput(TextWatcher w) {
		return null;
	}

	default void stopInput() {
	}

	default boolean isInputActive() {
		return false;
	}

	default boolean setTextInput(String text) {
		return false;
	}
}
