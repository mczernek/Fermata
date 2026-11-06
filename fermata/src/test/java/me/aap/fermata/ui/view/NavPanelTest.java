package me.aap.fermata.ui.view;

import static me.aap.fermata.ui.view.NavPanel.BOTTOM_END;
import static me.aap.fermata.ui.view.NavPanel.BOTTOM_START;
import static me.aap.fermata.ui.view.NavPanel.TOP_END;
import static me.aap.fermata.ui.view.NavPanel.TOP_START;

import org.junit.Assert;
import org.junit.Test;

public class NavPanelTest extends Assert {

	@Test
	public void cornerOfTheQuarter() {
		assertEquals(TOP_START, NavPanel.corner(false, false));
		assertEquals(TOP_END, NavPanel.corner(true, false));
		assertEquals(BOTTOM_START, NavPanel.corner(false, true));
		assertEquals(BOTTOM_END, NavPanel.corner(true, true));
	}
}
