package me.googol.chatEdit.mixin;

import me.googol.chatEdit.inter.CommandBlockScreenInterface;
import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandBlockEditScreen.class)
public abstract class CommandBlockScreenMixin implements CommandBlockScreenInterface {
    @Shadow
    protected abstract void enableControls(boolean bl);

    @Shadow
    public abstract void updateGui();

    @Unique
    private boolean wasActive = false;
    @Inject(method = "init",at=@At("TAIL"))
    public void init(CallbackInfo ci){
        this.enableControls(wasActive);
        if(wasActive)
            updateGui();
        wasActive = false;
    }
}
