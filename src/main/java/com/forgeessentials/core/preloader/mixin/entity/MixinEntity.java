package com.forgeessentials.core.preloader.mixin.entity;
//
//import net.minecraft.entity.Entity;
//import net.minecraftforge.common.MinecraftForge;
//import net.minecraftforge.fe.event.world.PressurePlateEvent;
//
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Overwrite;
//
//@Mixin(Entity.class)
//public abstract class MixinEntity
//{
//    /**
//     * Send pressure-plate event on plate depress 
//
//     * @author Maximuslotro
//     * @reason stuff
//     */
//    @Overwrite
//    public boolean isIgnoringBlockTriggers()
//    {
//        return MinecraftForge.EVENT_BUS.post(new PressurePlateEvent((Entity) (Object) this));
//    }
//
//}