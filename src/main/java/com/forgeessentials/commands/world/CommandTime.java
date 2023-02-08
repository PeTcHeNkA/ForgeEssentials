package com.forgeessentials.commands.world;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.IWorldInfo;
import net.minecraft.world.storage.ServerWorldInfo;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;

import com.forgeessentials.api.permissions.FEPermissions;
import com.forgeessentials.commands.ModuleCommands;
import com.forgeessentials.commands.world.CommandWeather.WeatherData;
import com.forgeessentials.core.commands.ForgeEssentialsCommandBuilder;
import com.forgeessentials.core.misc.FECommandManager.ConfigurableCommand;
import com.forgeessentials.core.misc.TranslatedCommandException;
import com.forgeessentials.data.v2.DataManager;
import com.forgeessentials.util.CommandParserArgs;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

public class CommandTime extends ForgeEssentialsCommandBuilder implements ConfigurableCommand
{

    public CommandTime(String name, int permissionLevel, boolean enabled)
    {
        super(enabled);
    }

    public static final int dayTimeStart = 1;
    public static final int dayTimeEnd = 11;
    public static final int nightTimeStart = 14;
    public static final int nightTimeEnd = 22;

    public static class TimeData
    {
        Long frozenTime;
    }

    protected static HashMap<RegistryKey<World>, TimeData> timeData = new HashMap<>();

    protected static TimeData getTimeData(RegistryKey<World> registryKey)
    {
        TimeData td = timeData.get(registryKey);
        if (td == null)
        {
            td = new TimeData();
            timeData.put(registryKey, td);
        }
        return td;
    }

    /* ------------------------------------------------------------ */

    @Override
    public String getPrimaryAlias()
    {
        return "time";
    }

    @Override
    public boolean canConsoleUseCommand()
    {
        return true;
    }

    @Override
    public DefaultPermissionLevel getPermissionLevel()
    {
        return DefaultPermissionLevel.OP;
    }

    @Override
    public String getPermissionNode()
    {
        return ModuleCommands.PERM + ".time";
    }

    @Override
    public LiteralArgumentBuilder<CommandSource> setExecution()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void parse(CommandParserArgs arguments) throws CommandException
    {
        if (arguments.isEmpty())
        {
            arguments.confirm("/time set|add <t> [dim]");
            arguments.confirm("/time freeze [dim]");
            return;
        }

        arguments.tabComplete("freeze", "set", "add");
        String subCmd = arguments.remove().toLowerCase();
        switch (subCmd)
        {
        case "freeze":
            parseFreeze(arguments);
            break;
        case "set":
            parseTime(arguments, false);
            break;
        case "add":
            parseTime(arguments, true);
            break;
        default:
            throw new TranslatedCommandException(FEPermissions.MSG_UNKNOWN_SUBCOMMAND, subCmd);
        }
    }

    public static void parseFreeze(CommandParserArgs arguments) throws CommandException
    {
        World world = arguments.isEmpty() ? null : arguments.parseWorld();
        if (arguments.isTabCompletion)
            return;

        if (world == null)
        {
            boolean freeze = getTimeData(ServerWorld.OVERWORLD).frozenTime == null;
            for (World w : DimensionManager.getWorlds())
            {
                TimeData td = getTimeData(w.dimension());
                td.frozenTime = freeze ? w.getLevelData().getDayTime() : null;
            }
            if (freeze)
                arguments.confirm("Froze time in all worlds");
            else
                arguments.confirm("Unfroze time in all worlds");
        }
        else
        {
            TimeData td = getTimeData(world.dimension());
            td.frozenTime = (td.frozenTime == null) ? world.getLevelData().getDayTime() : null;
            if (td.frozenTime != null)
                arguments.confirm("Froze time");
            else
                arguments.confirm("Unfroze time");
        }
        save();
    }

    public static void parseTime(CommandParserArgs arguments, boolean addTime) throws CommandException
    {
        if (arguments.isEmpty())
        {
            throw new TranslatedCommandException("Please Specify a time!");
        }
        long time;
        if (CommandParserArgs.timeFormatPattern.matcher(arguments.peek()).find())
        {
            time = arguments.mcParseTimeReadable();
        }
        else
        {
            if (addTime)
            {
                throw new TranslatedCommandException("Add time does not accept time values in the form of day, midday, etc");
            }
            arguments.tabComplete("day", "midday", "dusk", "night", "midnight");
            String timeStr = arguments.remove().toLowerCase();
            switch (timeStr)
            {
            case "day":
                time = 1000;
                break;
            case "midday":
                time = 6 * 1000;
                break;
            case "dusk":
                time = 12 * 1000;
                break;
            case "night":
                time = 14 * 1000;
                break;
            case "midnight":
                time = 18 * 1000;
                break;
            default:
                throw new TranslatedCommandException("Invalid Time format");
            }
        }

        World world = arguments.isEmpty() ? null : arguments.parseWorld();
        if (arguments.isTabCompletion)
            return;

        if (world == null)
        {
            for (World w : DimensionManager.getWorlds())
            {
                if (addTime)
                    w.getWorldInfo().setWorldTime(w.getWorldInfo().getWorldTime() + time);
                else
                    w.getWorldInfo().setWorldTime(time);
                TimeData td = getTimeData(w.provider.getDimension());
                if (td.frozenTime != null)
                    td.frozenTime = w.getWorldInfo().getWorldTime();
            }
            arguments.confirm("Set time to %s in all worlds", time);
        }
        else
        {
            if (addTime)
                world.getWorldInfo().setWorldTime(world.getWorldInfo().getWorldTime() + time);
            else
                world.getWorldInfo().setWorldTime(time);
            TimeData td = getTimeData(world.provider.getDimension());
            if (td.frozenTime != null)
                td.frozenTime = world.getWorldInfo().getWorldTime();
            arguments.confirm("Set time to %s", time);
        }
    }

    /* ------------------------------------------------------------ */

    @SubscribeEvent
    public void doWorldTick(TickEvent.WorldTickEvent event)
    {
        if (event.phase == Phase.START)
            return;
        World world = event.world;
        IWorldInfo wi = world.getLevelData();
        if (wi.getGameTime() % 10 == 0)
            updateWorld(world);
    }

    public static void updateWorld(ServerWorld world)
    {
        TimeData td = getTimeData(world.dimension());
        if (td.frozenTime != null)
            world.setDayTime(td.frozenTime);
    }

    public static void save()
    {
        DataManager.getInstance().deleteAll(WeatherData.class);
        for (Entry<Integer, TimeData> state : timeData.entrySet())
        {
            DataManager.getInstance().save(state.getValue(), state.getKey().toString());
        }
    }

    @Override
    public void loadData()
    {
        Map<String, TimeData> states = DataManager.getInstance().loadAll(TimeData.class);
        timeData.clear();
        for (Entry<String, TimeData> state : states.entrySet())
        {
            if (state.getValue() == null)
                continue;
            try
            {
                timeData.put(Integer.parseInt(state.getKey()), state.getValue());
            }
            catch (NumberFormatException e)
            {
                /* do nothing or log message */
            }
        }
    }

    @Override
    public void loadConfig(ForgeConfigSpec.Builder BUILDER, String category)
    {
        /* do nothing */
    }

    @Override
    public void bakeConfig(boolean reload)
    {
    	/* do nothing */
    }
}