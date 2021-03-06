package com.feed_the_beast.ftbquests.block;

import com.feed_the_beast.ftblib.lib.util.BlockUtils;
import com.feed_the_beast.ftbquests.client.ClientQuestFile;
import com.feed_the_beast.ftbquests.item.FTBQuestsItems;
import com.feed_the_beast.ftbquests.quest.ITeamData;
import com.feed_the_beast.ftbquests.quest.Quest;
import com.feed_the_beast.ftbquests.quest.task.QuestTask;
import com.feed_the_beast.ftbquests.quest.task.QuestTaskData;
import com.feed_the_beast.ftbquests.tile.ITaskScreen;
import com.feed_the_beast.ftbquests.tile.TileTaskScreenCore;
import com.feed_the_beast.ftbquests.tile.TileTaskScreenPart;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * @author LatvianModder
 */
public class BlockTaskScreen extends BlockWithHorizontalFacing
{
	public static boolean BREAKING_SCREEN = false;
	public static QuestTask currentTask = null;

	public static double getClickX(EnumFacing facing, int offX, int offY, double hitX, double hitZ, int size)
	{
		return 0.5D;
	}

	public static double getClickY(int offY, double hitY, int size)
	{
		return 1D - (offY + hitY) / (size * 2D + 1D);
	}

	public BlockTaskScreen()
	{
		super(Material.IRON, MapColor.BLACK);
		setHardness(0.3F);
		setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
	}

	@Override
	public boolean hasTileEntity(IBlockState state)
	{
		return true;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state)
	{
		return currentTask == null ? new TileTaskScreenCore() : currentTask.createScreenCore(world);
	}

	@Override
	public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items)
	{
		items.add(new ItemStack(this));

		for (int i = 1; i <= 4; i++)
		{
			ItemStack stack = new ItemStack(this);
			stack.setTagInfo("Size", new NBTTagByte((byte) i));
			items.add(stack);
		}
	}

	@Override
	public Item getItemDropped(IBlockState state, Random rand, int fortune)
	{
		return FTBQuestsItems.SCREEN;
	}

	@Override
	public int quantityDropped(Random random)
	{
		return 0;
	}

	@Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player)
	{
		ItemStack stack = new ItemStack(FTBQuestsItems.SCREEN);

		TileEntity tileEntity = world.getTileEntity(pos);

		if (tileEntity instanceof ITaskScreen)
		{
			TileTaskScreenCore screen = ((ITaskScreen) tileEntity).getScreen();

			if (screen != null)
			{
				screen.writeToPickBlock(stack);
			}
		}

		return stack;
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
	{
		TileEntity tileEntity = world.getTileEntity(pos);

		if (tileEntity instanceof ITaskScreen)
		{
			ITaskScreen base = (ITaskScreen) tileEntity;
			TileTaskScreenCore screen = base.getScreen();

			if (screen != null)
			{
				if (player.isSneaking())
				{
					if (player instanceof EntityPlayerMP)
					{
						screen.onClicked((EntityPlayerMP) player, hand, 0F, 0F);
					}

					return true;
				}

				if (facing == state.getValue(FACING))
				{
					if (player instanceof EntityPlayerMP)
					{
						screen.onClicked((EntityPlayerMP) player, hand, getClickX(facing, base.getOffsetX(), base.getOffsetZ(), hitX, hitZ, screen.size), getClickY(base.getOffsetY(), hitY, screen.size));
					}

					return true;
				}
			}
		}

		return false;
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
	{
		TileEntity tileEntity = world.getTileEntity(pos);

		if (tileEntity instanceof TileTaskScreenCore)
		{
			TileTaskScreenCore screen = (TileTaskScreenCore) tileEntity;
			screen.readFromItem(stack);
			screen.setIDFromPlacer(placer);

			screen.facing = state.getValue(FACING);

			if (screen.size > 0)
			{
				IBlockState state1 = FTBQuestsBlocks.SCREEN_PART.getDefaultState().withProperty(FACING, screen.getFacing());

				boolean xaxis = state.getValue(FACING).getAxis() == EnumFacing.Axis.X;

				for (int y = 0; y < screen.size * 2 + 1; y++)
				{
					for (int x = -screen.size; x <= screen.size; x++)
					{
						if (x != 0 || y != 0)
						{
							int offX = xaxis ? 0 : x;
							int offZ = xaxis ? x : 0;
							world.setBlockToAir(new BlockPos(pos.getX() + offX, pos.getY() + y, pos.getZ() + offZ));
						}
					}
				}

				for (int y = 0; y < screen.size * 2 + 1; y++)
				{
					for (int x = -screen.size; x <= screen.size; x++)
					{
						if (x != 0 || y != 0)
						{
							int offX = xaxis ? 0 : x;
							int offZ = xaxis ? x : 0;
							BlockPos pos1 = new BlockPos(pos.getX() + offX, pos.getY() + y, pos.getZ() + offZ);
							world.setBlockState(pos1, state1);

							TileEntity tileEntity1 = world.getTileEntity(pos1);

							if (tileEntity1 instanceof TileTaskScreenPart)
							{
								((TileTaskScreenPart) tileEntity1).setOffset(offX, y, offZ);
							}
						}
					}
				}
			}
		}
	}

	@Override
	@Nullable
	public String getHarvestTool(IBlockState state)
	{
		return null;
	}

	@Override
	public int getHarvestLevel(IBlockState state)
	{
		return -1;
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state)
	{
		TileEntity tileEntity = world.getTileEntity(pos);

		if (tileEntity instanceof TileTaskScreenCore)
		{
			TileTaskScreenCore screen = (TileTaskScreenCore) tileEntity;

			if (screen.size > 0)
			{
				BREAKING_SCREEN = true;
				boolean xaxis = state.getValue(FACING).getAxis() == EnumFacing.Axis.X;

				for (int y = 0; y < screen.size * 2 + 1; y++)
				{
					for (int x = -screen.size; x <= screen.size; x++)
					{
						if (x != 0 || y != 0)
						{
							int offX = xaxis ? 0 : x;
							int offZ = xaxis ? x : 0;
							BlockPos pos1 = new BlockPos(pos.getX() + offX, pos.getY() + y, pos.getZ() + offZ);
							IBlockState state1 = world.getBlockState(pos1);

							if (state1.getBlock() == FTBQuestsBlocks.SCREEN_PART)
							{
								world.setBlockToAir(pos1);
							}
						}
					}
				}

				BREAKING_SCREEN = false;
			}
		}

		super.breakBlock(world, pos, state);
	}

	@Override
	@Deprecated
	public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World world, BlockPos pos)
	{
		TileEntity tileEntity = world.getTileEntity(pos);

		if (tileEntity instanceof ITaskScreen)
		{
			TileTaskScreenCore screen = ((ITaskScreen) tileEntity).getScreen();

			if (screen != null)
			{
				return getScreenAABB(screen.getPos(), screen.getFacing(), screen.size);
			}
		}

		return new AxisAlignedBB(0D, -1D, 0D, 0D, -1D, 0D);
	}

	public static AxisAlignedBB getScreenAABB(BlockPos pos, EnumFacing facing, int size)
	{
		if (size == 0)
		{
			return FULL_BLOCK_AABB.offset(pos);
		}

		boolean xaxis = facing.getAxis() == EnumFacing.Axis.X;

		if (xaxis)
		{
			return new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ() - size, pos.getX() + 1D, pos.getY() + size * 2D + 1D, pos.getZ() + size + 1D);
		}
		else
		{
			return new AxisAlignedBB(pos.getX() - size, pos.getY(), pos.getZ(), pos.getX() + size + 1D, pos.getY() + size * 2D + 1D, pos.getZ() + 1D);
		}
	}

	@Override
	@Deprecated
	public float getBlockHardness(IBlockState state, World world, BlockPos pos)
	{
		TileEntity tileEntity = world.getTileEntity(pos);

		if (tileEntity instanceof ITaskScreen)
		{
			TileTaskScreenCore core = ((ITaskScreen) tileEntity).getScreen();

			if (core != null && core.indestructible)
			{
				return -1F;
			}
		}

		return super.getBlockHardness(state, world, pos);
	}

	@Override
	public float getExplosionResistance(World world, BlockPos pos, @Nullable Entity exploder, Explosion explosion)
	{
		TileEntity tileEntity = world.getTileEntity(pos);

		if (tileEntity instanceof ITaskScreen)
		{
			TileTaskScreenCore core = ((ITaskScreen) tileEntity).getScreen();

			if (core != null && core.indestructible)
			{
				return Float.MAX_VALUE;
			}
		}

		return super.getExplosionResistance(world, pos, exploder, explosion);
	}

	@Override
	@Deprecated
	public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
	{
		TileEntity tileEntity = world.getTileEntity(pos);

		if (tileEntity instanceof ITaskScreen)
		{
			TileTaskScreenCore core = ((ITaskScreen) tileEntity).getScreen();

			if (core != null && core.skin != BlockUtils.AIR_STATE)
			{
				return core.skin;
			}
		}

		return state;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag)
	{
		NBTTagCompound nbt = stack.getTagCompound();
		int size = nbt == null ? 0 : nbt.getByte("Size");
		tooltip.add(I18n.format("tile.ftbquests.screen.size") + ": " + TextFormatting.GOLD + (1 + size * 2) + " x " + (1 + size * 2));

		if (!ClientQuestFile.exists())
		{
			return;
		}

		ITeamData team = nbt == null ? null : ClientQuestFile.INSTANCE.getData(nbt.getShort("Team"));

		if (team == null)
		{
			team = ClientQuestFile.INSTANCE.self;
		}

		if (team != null)
		{
			tooltip.add(I18n.format("ftbquests.team") + ": " + team.getDisplayName().getFormattedText());
		}

		Quest quest = ClientQuestFile.INSTANCE.getQuest(ClientQuestFile.INSTANCE.getID(nbt == null ? null : nbt.getTag("Quest")));

		if (quest == null || quest.tasks.isEmpty())
		{
			return;
		}

		tooltip.add(I18n.format("ftbquests.chapter") + ": " + quest.chapter.getYellowDisplayName().getFormattedText());
		tooltip.add(I18n.format("ftbquests.quest") + ": " + quest.getYellowDisplayName().getFormattedText());

		QuestTask task = quest.getTask(nbt == null ? 0 : nbt.getByte("TaskIndex") & 0xFF);

		tooltip.add(I18n.format("ftbquests.task") + ": " + task.getYellowDisplayName().getFormattedText());

		if (team != null)
		{
			QuestTaskData taskData = team.getQuestTaskData(task);
			tooltip.add(I18n.format("ftbquests.progress") + ": " + TextFormatting.BLUE + String.format("%s / %s [%d%%]", taskData.getProgressString(), task.getMaxProgressString(), taskData.getRelativeProgress()));
		}
	}
}