package com.forgeessentials.core.preloader.mixin.command;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.rcon.RConConsoleSource;
import net.minecraft.server.MinecraftServer;

import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.forgeessentials.api.APIRegistry;
import com.forgeessentials.api.UserIdent;
import com.forgeessentials.api.permissions.GroupEntry;
import com.forgeessentials.commons.selections.WorldPoint;
import com.forgeessentials.core.misc.PermissionManager;

@Mixin(value = { ServerPlayerEntity.class, MinecraftServer.class, RConConsoleSource.class, CommandBlockBaseLogic.class },
        targets = { "net/minecraft/tileentity/TileEntitySign$1", "net/minecraft/tileentity/TileEntitySign$2" })
public abstract class MixinICommandSender implements ICommandSender
{
    private static final Logger launchLog = org.apache.logging.log4j.LogManager.getLogger("ForgeEssentials");

    @Inject(method = "canUseCommand(ILjava/lang/String;)Z", at = @At("HEAD"), cancellable = true)
    private void canUseCommand(final int permissionLevel, final String commandName, final CallbackInfoReturnable<Boolean> cir)
    {
        ICommand cmd;
        String permNode;
        if ((cmd = FMLCommonHandler.instance().getMinecraftServerInstance().commandManager.getCommands().get(commandName)) != null)
        {
            permNode = PermissionManager.getCommandPermission(cmd);
        }
        else if ("@".equals(commandName))
        {
            return;
        }
        else
        {
            permNode = commandName;
        }
        UserIdent ident = UserIdent.get(this);
        WorldPoint point = null;
        if (ident != null && ident.hasPlayer())
            point = new WorldPoint(ident.getPlayer());
        String permValue = APIRegistry.perms.getPermission(ident, point, null, GroupEntry.toList(APIRegistry.perms.getPlayerGroups(ident)), permNode, false);
        if (permValue == null)
        {
            APIRegistry.perms.registerPermission(permNode, PermissionManager.fromIntegerLevel(permissionLevel),
                    String.format("Autogenerated Command Node for '%s' with permission level of '%s'", permNode, permissionLevel));
            permValue = APIRegistry.perms.getPermission(ident, point, null, GroupEntry.toList(APIRegistry.perms.getPlayerGroups(ident)), permNode, false);
        }
        if (permValue != null)
        {
            cir.setReturnValue(APIRegistry.perms.checkBooleanPermission(permValue));
        }
        else
        {
            launchLog.error(
                    "canUseCommand({}, {}) returns a null permValue for node: {}. Missing nodes are auto-registered so this should never happen. If you see this message, please report it!",
                    commandName, permissionLevel, permNode);
        }
    }
}
