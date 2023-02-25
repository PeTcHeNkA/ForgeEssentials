package com.forgeessentials.util.selections;

import net.minecraft.command.CommandSource;
import net.minecraftforge.server.permission.DefaultPermissionLevel;

import com.forgeessentials.commons.selections.Selection;
import com.forgeessentials.core.commands.ForgeEssentialsCommandBuilder;
import com.forgeessentials.core.misc.TranslatedCommandException;
import com.forgeessentials.util.output.ChatOutputHandler;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

public class CommandExpandY extends ForgeEssentialsCommandBuilder
{

    public CommandExpandY(boolean enabled)
    {
        super(enabled);
    }

    @Override
    public String getPrimaryAlias()
    {
        return "/expandY";
    }

    @Override
    public LiteralArgumentBuilder<CommandSource> setExecution()
    {
        return null;
    }

    @Override
    public int processCommandPlayer(CommandContext<CommandSource> ctx, Object... params) throws CommandSyntaxException
    {
        Selection sel = SelectionHandler.getSelection(getServerPlayer(ctx.getSource()));
        if (sel == null)
            throw new TranslatedCommandException("Invalid selection.");
        SelectionHandler.setStart(getServerPlayer(ctx.getSource()), sel.getStart().setY(0));
        SelectionHandler.setEnd(getServerPlayer(ctx.getSource()), sel.getEnd().setY(ctx.getSource().getLevel().getMaxBuildHeight()));
        ChatOutputHandler.chatConfirmation(ctx.getSource(), "Selection expanded from bottom to top.");
        return Command.SINGLE_SUCCESS;
    }

    @Override
    public String getPermissionNode()
    {
        return "fe.core.pos.expandy";
    }

    @Override
    public boolean canConsoleUseCommand()
    {
        return false;
    }

    @Override
    public DefaultPermissionLevel getPermissionLevel()
    {
        return DefaultPermissionLevel.ALL;
    }

}
