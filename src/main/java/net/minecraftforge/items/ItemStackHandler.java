/*
 * Minecraft Forge
 * Copyright (c) 2016-2021.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.items;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nonnull;

public class ItemStackHandler implements IItemHandler, IItemHandlerModifiable, INBTSerializable<CompoundNBT>
{
    public NonNullList<ItemStack> stacks; // CatServer - private -> public

    public ItemStackHandler()
    {
        this(1);
    }

    public ItemStackHandler(int size)
    {
        stacks = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    public ItemStackHandler(NonNullList<ItemStack> stacks)
    {
        this.stacks = stacks;
    }

    public void setSize(int size)
    {
        stacks = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    @Override
    public void setStackInSlot(int slot, @Nonnull ItemStack stack)
    {
        validateSlotIndex(slot);
        this.stacks.set(slot, stack);
        onContentsChanged(slot);
    }

    @Override
    public int getSlots()
    {
        return stacks.size();
    }

    @Override
    @Nonnull
    public ItemStack getStackInSlot(int slot)
    {
        validateSlotIndex(slot);
        return this.stacks.get(slot);
    }

    @Override
    @Nonnull
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate)
    {
        if (stack.isEmpty())
            return ItemStack.EMPTY;
            
        if (!isItemValid(slot, stack))
            return stack;

        validateSlotIndex(slot);

        ItemStack existing = this.stacks.get(slot);

        int limit = getStackLimit(slot, stack);

        if (!existing.isEmpty())
        {
            if (!ItemHandlerHelper.canItemStacksStack(stack, existing))
                return stack;

            limit -= existing.getCount();
        }

        if (limit <= 0)
            return stack;

        boolean reachedLimit = stack.getCount() > limit;

        if (!simulate)
        {
            if (existing.isEmpty())
            {
                this.stacks.set(slot, reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, limit) : stack);
            }
            else
            {
                existing.grow(reachedLimit ? limit : stack.getCount());
            }
            onContentsChanged(slot);
        }

        return reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, stack.getCount()- limit) : ItemStack.EMPTY;
    }

    @Override
    @Nonnull
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        if (amount == 0)
            return ItemStack.EMPTY;

        validateSlotIndex(slot);

        ItemStack existing = this.stacks.get(slot);

        if (existing.isEmpty())
            return ItemStack.EMPTY;

        int toExtract = Math.min(amount, existing.getMaxStackSize());

        if (existing.getCount() <= toExtract)
        {
            if (!simulate)
            {
                this.stacks.set(slot, ItemStack.EMPTY);
                onContentsChanged(slot);
                return existing;
            }
            else
            {
                return existing.copy();
            }
        }
        else
        {
            if (!simulate)
            {
                this.stacks.set(slot, ItemHandlerHelper.copyStackWithSize(existing, existing.getCount() - toExtract));
                onContentsChanged(slot);
            }

            return ItemHandlerHelper.copyStackWithSize(existing, toExtract);
        }
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return 64;
    }

    protected int getStackLimit(int slot, @Nonnull ItemStack stack)
    {
        return Math.min(getSlotLimit(slot), stack.getMaxStackSize());
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack)
    {
        return true;
    }

    @Override
    public CompoundNBT serializeNBT()
    {
        ListNBT nbtTagList = new ListNBT();
        for (int i = 0; i < stacks.size(); i++)
        {
            if (!stacks.get(i).isEmpty())
            {
                CompoundNBT itemTag = new CompoundNBT();
                itemTag.putInt("Slot", i);
                stacks.get(i).save(itemTag);
                nbtTagList.add(itemTag);
            }
        }
        CompoundNBT nbt = new CompoundNBT();
        nbt.put("Items", nbtTagList);
        nbt.putInt("Size", stacks.size());
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt)
    {
        setSize(nbt.contains("Size", Constants.NBT.TAG_INT) ? nbt.getInt("Size") : stacks.size());
        ListNBT tagList = nbt.getList("Items", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < tagList.size(); i++)
        {
            CompoundNBT itemTags = tagList.getCompound(i);
            int slot = itemTags.getInt("Slot");

            if (slot >= 0 && slot < stacks.size())
            {
                stacks.set(slot, ItemStack.of(itemTags));
            }
        }
        onLoad();
    }

    protected void validateSlotIndex(int slot)
    {
        if (slot < 0 || slot >= stacks.size())
            throw new RuntimeException("Slot " + slot + " not in valid range - [0," + stacks.size() + ")");
    }

    protected void onLoad()
    {

    }

    protected void onContentsChanged(int slot)
    {

    }
}
