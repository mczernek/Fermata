package me.aap.fermata.addon.tv;

import static java.util.Objects.requireNonNull;
import static me.aap.utils.async.Completed.completed;
import static me.aap.utils.function.ResultConsumer.Cancel.isCancellation;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.concurrent.CancellationException;

import me.aap.fermata.addon.AddonManager;
import me.aap.fermata.addon.tv.m3u.TvM3uFile;
import me.aap.fermata.addon.tv.m3u.TvM3uFileSystem;
import me.aap.fermata.addon.tv.m3u.TvM3uFileSystemProvider;
import me.aap.fermata.addon.tv.m3u.TvM3uItem;
import me.aap.fermata.media.lib.DefaultMediaLib;
import me.aap.fermata.media.lib.MediaLib;
import me.aap.fermata.media.lib.MediaLib.BrowsableItem;
import me.aap.fermata.media.lib.MediaLib.Item;
import me.aap.fermata.media.service.FermataServiceUiBinder;
import me.aap.fermata.ui.activity.MainActivityDelegate;
import me.aap.fermata.ui.fragment.MediaLibFragment;
import me.aap.fermata.ui.view.DcViews;
import me.aap.fermata.ui.view.MediaItemMenuHandler;
import me.aap.fermata.ui.view.MediaItemView;
import me.aap.utils.app.App;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.log.Log;
import me.aap.utils.ui.UiUtils;
import me.aap.utils.ui.menu.OverlayMenu;
import me.aap.utils.ui.menu.OverlayMenuItem;
import me.aap.utils.ui.view.FloatingButton;
import me.aap.utils.ui.view.ToolBarView;

/**
 * @author Andrey Pavlenko
 */
public class TvFragment extends MediaLibFragment {
	// The last selected playlist, shown when TV is opened
	private static String selectedPlaylistId;
	private FrameLayout headerHolder;
	private HorizontalScrollView chipsScroll;
	private LinearLayout chips;
	private TextView emptyView;

	@Override
	public ToolBarView.Mediator getToolBarMediator() {
		// The screen has its own header: the title, the playlists and Add playlist
		return ToolBarView.Mediator.Invisible.instance;
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
													 @Nullable Bundle savedInstanceState) {
		View list = super.onCreateView(inflater, container, savedInstanceState);
		Context ctx = inflater.getContext();
		LinearLayout root = new LinearLayout(ctx);
		root.setOrientation(LinearLayout.VERTICAL);
		headerHolder = new FrameLayout(ctx);
		root.addView(headerHolder, DcViews.matchWidth());

		chips = new LinearLayout(ctx);
		chips.setOrientation(LinearLayout.HORIZONTAL);
		int pad = UiUtils.toIntPx(ctx, 16);
		chips.setPadding(pad, UiUtils.toIntPx(ctx, 8), pad, UiUtils.toIntPx(ctx, 8));
		chipsScroll = new HorizontalScrollView(ctx);
		chipsScroll.setHorizontalScrollBarEnabled(false);
		chipsScroll.setFillViewport(false);
		chipsScroll.addView(chips);

		emptyView = new TextView(ctx);
		emptyView.setText(R.string.tv_no_playlists);
		emptyView.setTextSize(16);
		emptyView.setGravity(Gravity.CENTER);
		emptyView.setTextColor(DcViews.color(ctx, me.aap.fermata.R.color.dc_muted));
		emptyView.setPadding(pad * 2, pad * 2, pad * 2, pad * 2);
		emptyView.setVisibility(View.GONE);

		if (!isCar(ctx)) root.addView(chipsScroll, DcViews.matchWidth());
		root.addView(emptyView, DcViews.matchWidth());
		root.addView(list, new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1));
		return root;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		getListView().setForcedGrid(isCar(view.getContext()));
		super.onViewCreated(view, savedInstanceState);
		updateHeader();
	}

	private static boolean isCar(Context ctx) {
		return MainActivityDelegate.get(ctx).isCarActivity();
	}

	/**
	 * The playlist, that contains the item, or null for the root.
	 */
	@Nullable
	private static BrowsableItem getPlaylist(@Nullable BrowsableItem i) {
		while (i != null) {
			BrowsableItem p = i.getParent();
			if (p instanceof TvRootItem) return i;
			i = p;
		}
		return null;
	}

	private void updateHeader() {
		if ((headerHolder == null) || (getAdapter() == null)) return;
		Context ctx = headerHolder.getContext();
		BrowsableItem parent = getAdapter().getParent();
		BrowsableItem playlist = getPlaylist(parent);
		boolean inGroup = (playlist != null) && (parent != playlist);
		LinearLayout h = DcViews.header(ctx, inGroup ? parent.getName() : getFragmentTitle(),
				inGroup ? () -> onBackPressed() : null);

		if (!inGroup) {
			if (isCar(ctx)) {
				// In the car the playlists are next to the title, to leave the space for the channels
				View title = h.findViewById(me.aap.fermata.R.id.dc_header_title);
				title.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
				ViewGroup p = (ViewGroup) chipsScroll.getParent();
				if (p != null) p.removeView(chipsScroll);
				chips.setPadding(UiUtils.toIntPx(ctx, 12), 0, 0, 0);
				h.addView(chipsScroll, new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1));
			}
			DcViews.addAction(h, DcViews.pillButton(ctx, me.aap.fermata.R.drawable.dc_plus,
					me.aap.fermata.R.color.dc_tv, getString(R.string.tv_add_playlist), v -> addSource()));
		}

		headerHolder.removeAllViews();
		headerHolder.addView(h);
		chipsScroll.setVisibility(inGroup ? View.GONE : View.VISIBLE);
		if (!inGroup) updateChips(playlist);
	}

	private void updateChips(@Nullable BrowsableItem selected) {
		TvRootItem root = getRootItem();
		root.getUnsortedChildren().main().onSuccess(list -> {
			if (chips == null) return;
			Context ctx = chips.getContext();
			chips.removeAllViews();
			emptyView.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
			int tv = DcViews.color(ctx, me.aap.fermata.R.color.dc_tv);
			View sel = null;

			for (Item i : list) {
				if (!(i instanceof BrowsableItem b)) continue;
				boolean isSel = b.equals(selected);
				TextView c = DcViews.chip(ctx, b.getName(), isSel, tv, v -> selectPlaylist(b));
				c.setOnLongClickListener(v -> {
					showPlaylistMenu(b);
					return true;
				});
				LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
				lp.setMarginEnd(UiUtils.toIntPx(ctx, 8));
				chips.addView(c, lp);
				if (isSel) sel = c;
			}

			if (sel != null) {
				View s = sel;
				chipsScroll.post(() -> chipsScroll.requestChildRectangleOnScreen(s,
						new android.graphics.Rect(0, 0, s.getWidth(), s.getHeight()), false));
			}
		});
	}

	private void selectPlaylist(BrowsableItem playlist) {
		selectedPlaylistId = playlist.getId();
		getAdapter().setParent(playlist);
	}

	/**
	 * Long press on a playlist chip: edit or delete the playlist.
	 */
	private void showPlaylistMenu(BrowsableItem playlist) {
		getMainActivity().getContextMenu().show(b -> {
			b.setTitle(playlist.getName());
			b.addItem(me.aap.fermata.R.id.edit, me.aap.fermata.R.drawable.edit,
					me.aap.fermata.R.string.edit).setData(playlist).setHandler(this::contextMenuItemSelected);
			b.addItem(me.aap.fermata.R.id.delete, me.aap.fermata.R.drawable.delete,
					me.aap.fermata.R.string.delete).setData(playlist).setHandler(this::contextMenuItemSelected);
		});
	}

	@Override
	protected ListAdapter createAdapter(FermataServiceUiBinder b) {
		return new TvAdapter(getMainActivity(), getRootItem());
	}

	@Override
	public CharSequence getFragmentTitle() {
		return getResources().getString(me.aap.fermata.R.string.addon_name_tv);
	}

	@Override
	public int getFragmentId() {
		return me.aap.fermata.R.id.tv_fragment;
	}

	@Override
	public FloatingButton.Mediator getFloatingButtonMediator() {
		return TvFloatingButtonMediator.instance;
	}

	public void navBarItemReselected(int itemId) {
		getAdapter().setParent(getRootItem());
	}

	@Override
	public boolean isRootPage() {
		return isRootItem();
	}

	@Override
	public boolean onBackPressed() {
		// The playlists are switched with the chips, back from a playlist goes Home
		if ((getAdapter() != null) && isRootItem() && !getMainActivity().isVideoMode())
			return false;
		return super.onBackPressed();
	}


	public void addSource() {
		TvM3uFileSystemProvider prov = new TvM3uFileSystemProvider();
		prov.select(getMainActivity(), Collections.singletonList(TvM3uFileSystem.getInstance())).main()
				.onFailure(this::failedToAddSource).onSuccess(this::addM3uSource);
	}

	public TvRootItem getRootItem() {
		return requireNonNull(AddonManager.get().getAddon(TvAddon.class)).getRootItem(
				(DefaultMediaLib) getMainActivity().getLib());
	}

	@Override
	public void contributeToContextMenu(OverlayMenu.Builder b, MediaItemMenuHandler h) {
		if (!(h.getItem() instanceof TvM3uItem)) return;
		b.addItem(me.aap.fermata.R.id.edit, me.aap.fermata.R.drawable.edit,
						me.aap.fermata.R.string.edit).setData(h.getItem())
				.setHandler(this::contextMenuItemSelected);
		b.addItem(me.aap.fermata.R.id.delete, me.aap.fermata.R.drawable.delete,
						me.aap.fermata.R.string.delete).setData(h.getItem())
				.setHandler(this::contextMenuItemSelected);
		super.contributeToContextMenu(b, h);
	}

	private boolean contextMenuItemSelected(OverlayMenuItem item) {
		int id = item.getItemId();
		if (id == me.aap.fermata.R.id.edit) {
			TvM3uItem i = item.getData();
			new TvM3uFileSystemProvider().edit(getMainActivity(), i.getResource())
					.onCompletion((ok, err) -> {
						if ((err != null) && !(err instanceof CancellationException)) {
							Log.e(err, "Failed to edit TV source ", i);
							UiUtils.showAlert(getContext(), err.getLocalizedMessage());
						}
						getMainActivity().showFragment(getFragmentId());
						if ((ok != null) && ok) i.refresh().thenRun(this::refresh);
					});
		} else if (id == me.aap.fermata.R.id.delete) {
			TvRootItem root = getRootItem();
			TvM3uItem i = item.getData();
			if (i.getId().equals(selectedPlaylistId)) selectedPlaylistId = null;
			root.removeItem(i).onSuccess(v -> getAdapter().setParent(root));
		}
		return true;
	}

	@Override
	public void contributeToNavBarMenu(OverlayMenu.Builder builder) {
		super.contributeToNavBarMenu(builder);

		// Add playlist is in the header
		if (getAdapter().getParent() instanceof TvRootItem) return;

		TvAdapter a = getAdapter();

		if (a.getListView().isSelectionActive() && a.hasSelectable() && a.hasSelected()) {
			OverlayMenu.Builder b = builder.withSelectionHandler(this::navBarMenuItemSelected);
			b.addItem(me.aap.fermata.R.id.favorites_add, me.aap.fermata.R.drawable.favorite,
					me.aap.fermata.R.string.favorites_add);
			getMainActivity().addPlaylistMenu(b, completed(a.getSelectedItems()));
		}
	}

	@Override
	protected boolean isSupportedItem(Item i) {
		return getRootItem().isChildItemId(i.getId());
	}

	@Override
	protected boolean isRefreshSupported() {
		return true;
	}

	private void addM3uSource(TvM3uFile m3u) {
		MainActivityDelegate a = getMainActivity();
		if (m3u != null) getRootItem().addSource(m3u);
		getAdapter().setParent(getRootItem());
		a.showFragment(getFragmentId());
	}

	private void failedToAddSource(Throwable ex) {
		getMainActivity().showFragment(me.aap.fermata.R.id.tv_fragment);
		if (isCancellation(ex)) return;

		App.get().getHandler().post(() -> {
			String msg = ex.getLocalizedMessage();
			UiUtils.showAlert(getContext(),
					getString(R.string.err_failed_to_add_tv_source, (msg != null) ? msg : ex.toString()));
		});
	}

	/**
	 * The root or a playlist (the playlists are selected with the chips).
	 */
	private boolean isRootItem() {
		BrowsableItem p = getAdapter().getParent();
		return (p == null) || (p instanceof TvRootItem) || (p.getParent() instanceof TvRootItem);
	}

	private class TvAdapter extends ListAdapter {

		TvAdapter(MainActivityDelegate activity, BrowsableItem parent) {
			super(activity, parent);
			openPlaylist(parent);
		}

		@Override
		public FutureSupplier<?> setParent(BrowsableItem parent, boolean userAction) {
			FutureSupplier<?> f = super.setParent(parent, userAction);
			updateHeader();
			openPlaylist(parent);
			return f;
		}

		@Override
		protected void onItemViewCreated(MediaItemView v) {
			v.setCardStyle(true, getListView().isGrid());
		}

		/**
		 * The list of the playlists is not shown, the selected (or the first) playlist is opened.
		 */
		private void openPlaylist(BrowsableItem parent) {
			if (!(parent instanceof TvRootItem)) return;
			parent.getUnsortedChildren().main().onSuccess(c -> {
				if ((getParent() != parent) || c.isEmpty()) return;
				Item open = c.get(0);
				for (Item i : c) {
					if (i.getId().equals(selectedPlaylistId)) {
						open = i;
						break;
					}
				}
				if (open instanceof BrowsableItem b) setParent(b);
			});
		}

		public boolean isLongPressDragEnabled() {
			return false;
		}

		@Override
		protected void onItemDismiss(int position) {
			BrowsableItem i = getAdapter().getParent();
			if (i instanceof TvRootItem) ((TvRootItem) i).removeItem(position);
			super.onItemDismiss(position);
		}

		@Override
		protected boolean onItemMove(int fromPosition, int toPosition) {
			BrowsableItem i = getAdapter().getParent();
			if (i instanceof MediaLib.Folders) ((MediaLib.Folders) i).moveItem(fromPosition, toPosition);
			return super.onItemMove(fromPosition, toPosition);
		}
	}
}
