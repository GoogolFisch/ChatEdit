package me.googol.chatEdit.mixin;

import me.googol.chatEdit.inter.ScreenInterface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Screen.class)
public class ScreenMixin implements ScreenInterface {
    @Shadow
    @Final
    protected Minecraft minecraft;

    @Override
    public Minecraft funnyBinds$getMinecraft(){
        return this.minecraft;
    }
}
