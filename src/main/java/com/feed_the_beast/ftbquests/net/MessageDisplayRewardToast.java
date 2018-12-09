package com.feed_the_beast.ftbquests.net;

import com.feed_the_beast.ftblib.lib.icon.Icon;
import com.feed_the_beast.ftblib.lib.io.DataIn;
import com.feed_the_beast.ftblib.lib.io.DataOut;
import com.feed_the_beast.ftblib.lib.net.MessageToClient;
import com.feed_the_beast.ftblib.lib.net.NetworkWrapper;
import com.feed_the_beast.ftbquests.gui.GuiRewardNotifications;
import com.feed_the_beast.ftbquests.gui.tree.ToastReward;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author LatvianModder
 */
public class MessageDisplayRewardToast extends MessageToClient
{
	private ITextComponent text;
	private Icon icon;

	public MessageDisplayRewardToast()
	{
	}

	public MessageDisplayRewardToast(ITextComponent t, Icon i)
	{
		text = t;
		icon = i;
	}

	@Override
	public NetworkWrapper getWrapper()
	{
		return FTBQuestsNetHandler.GENERAL;
	}

	@Override
	public void writeData(DataOut data)
	{
		data.writeTextComponent(text);
		data.writeIcon(icon);
	}

	@Override
	public void readData(DataIn data)
	{
		text = data.readTextComponent();
		icon = data.readIcon();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void onMessage()
	{
		GuiRewardNotifications.RewardKey key = new GuiRewardNotifications.RewardKey(text.getUnformattedText(), icon);

		if (!GuiRewardNotifications.add(key, 1))
		{
			Minecraft.getMinecraft().getToastGui().add(new ToastReward(text.getFormattedText(), icon));
		}
	}
}