package org.quantumbadger.redreader.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class GroupedRecyclerViewAdapter extends RecyclerView.Adapter {

	private static final AtomicLong ITEM_UNIQUE_ID_GENERATOR = new AtomicLong(100000);

	public static abstract class Item {

		private final long mUniqueId = ITEM_UNIQUE_ID_GENERATOR.incrementAndGet();
		private boolean mCurrentlyHidden = false;

		public abstract Class getViewType();

		public abstract RecyclerView.ViewHolder onCreateViewHolder(final ViewGroup viewGroup);

		public abstract void onBindViewHolder(final RecyclerView.ViewHolder viewHolder);

		public abstract boolean isHidden();
	}

	private final ArrayList<Item>[] mItems;
	private final HashMap<Class, Integer> mItemViewTypeMap = new HashMap<>();
	private final HashMap<Integer, Item> mViewTypeItemMap = new HashMap<>();

	public GroupedRecyclerViewAdapter(final int groups) {
		//noinspection unchecked
		mItems = (ArrayList<Item>[])new ArrayList[groups];

		for(int i = 0; i < groups; i++) {
			mItems[i] = new ArrayList<>();
		}

		setHasStableIds(true);
	}

	private int getItemPositionInternal(final int groupId, final Item item) {

		final ArrayList<Item> group = mItems[groupId];

		for(int i = 0; i < group.size(); i++) {
			if(group.get(i) == item) {
				return getItemPositionInternal(groupId, i);
			}
		}

		throw new RuntimeException("Item not found");
	}

	// "positionInGroup" should include both hidden and visible items
	private int getItemPositionInternal(final int group, final int positionInGroup) {

		int result = 0;

		for(int i = 0; i < group; i++) {
			result += getGroupUnhiddenCount(i);
		}

		for(int i = 0; i < positionInGroup; i++) {
			if(!mItems[group].get(i).mCurrentlyHidden) {
				result++;
			}
		}

		return result;
	}

	private Item getItemInternal(final int desiredPosition) {

		if(desiredPosition < 0) {
			throw new RuntimeException("Item desiredPosition " + desiredPosition + " is too low");
		}

		int currentPosition = 0;

		for(int groupId = 0; groupId < mItems.length; groupId++) {

			final ArrayList<Item> group = mItems[groupId];

			for(int positionInGroup = 0; positionInGroup < group.size(); positionInGroup++) {

				final Item item = group.get(positionInGroup);

				if(!item.mCurrentlyHidden) {

					if(currentPosition == desiredPosition) {
						return item;
					}

					currentPosition++;
				}
			}
		}

		throw new RuntimeException("Item desiredPosition " + desiredPosition + " is too high");
	}

	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(final ViewGroup viewGroup, final int viewType) {
		return mViewTypeItemMap.get(viewType).onCreateViewHolder(viewGroup);
	}

	@Override
	public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, final int position) {
		getItemInternal(position).onBindViewHolder(viewHolder);
	}

	@Override
	public int getItemViewType(final int position) {

		final Item item = getItemInternal(position);
		final Class viewTypeClass = item.getViewType();

		Integer typeId = mItemViewTypeMap.get(viewTypeClass);

		if(typeId == null) {
			typeId = mItemViewTypeMap.size();
			mItemViewTypeMap.put(viewTypeClass, typeId);
			mViewTypeItemMap.put(typeId, item);
		}

		return typeId;
	}

	private int getGroupUnhiddenCount(final int groupId) {

		final ArrayList<Item> group = mItems[groupId];

		int result = 0;

		for(int i = 0; i < group.size(); i++) {
			if(!group.get(i).mCurrentlyHidden) {
				result++;
			}
		}

		return result;
	}

	@Override
	public long getItemId(final int position) {
		return getItemInternal(position).mUniqueId;
	}

	@Override
	public int getItemCount() {

		int count = 0;

		//noinspection ForLoopReplaceableByForEach
		for(int i = 0; i < mItems.length; i++) {
			count += getGroupUnhiddenCount(i);
		}

		return count;
	}

	public void appendToGroup(final int group, final Item item) {

		final int position = getItemPositionInternal(group + 1, 0);

		mItems[group].add(item);

		item.mCurrentlyHidden = false;
		notifyItemInserted(position);
	}

	public void appendToGroup(final int group, final Collection<Item> items) {

		final int position = getItemPositionInternal(group + 1, 0);

		mItems[group].addAll(items);

		for(final Item item : items) {
			item.mCurrentlyHidden = false;
		}

		notifyItemRangeInserted(position, items.size());
	}

	public void removeFromGroup(final int groupId, final Item item) {

		final ArrayList<Item> group = mItems[groupId];

		for(int i = 0; i < group.size(); i++) {
			if(group.get(i) == item) {

				final int position = getItemPositionInternal(groupId, i);

				group.remove(i);

				if(!item.mCurrentlyHidden) {
					notifyItemRemoved(position);
				}

				return;
			}
		}

		throw new RuntimeException("Item not found");
	}

	public void updateHiddenStatus() {

		int position = 0;

		for(int groupId = 0; groupId < mItems.length; groupId++) {

			final ArrayList<Item> group = mItems[groupId];

			for(int positionInGroup = 0; positionInGroup < group.size(); positionInGroup++) {

				final Item item = group.get(positionInGroup);

				final boolean wasHidden = item.mCurrentlyHidden;
				final boolean isHidden = item.isHidden();
				item.mCurrentlyHidden = isHidden;

				if(isHidden && !wasHidden) {
					notifyItemRemoved(position);

				} else if(!isHidden && wasHidden) {
					notifyItemInserted(position);
				}

				if(!isHidden) {
					position++;
				}
			}
		}
	}

	public void notifyItemChanged(final int groupId, final Item item) {
		final int position = getItemPositionInternal(groupId, item);
		notifyItemChanged(position);
	}
}
