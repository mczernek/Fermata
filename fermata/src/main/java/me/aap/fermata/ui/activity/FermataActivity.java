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
	 * The only top level fragment, this activity is dedicated to (e.g. YouTube), or 0 if the
	 * activity is not restricted to a single fragment.
	 */
	@IdRes
	default int getDedicatedFragmentId() {
		return 0;
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
