package me.googol.chatEdit.mixin;


import me.googol.chatEdit.client.ChatEditClient;
import me.googol.chatEdit.inter.AnvilScreenInterface;
import me.googol.chatEdit.inter.ChatCompInterface;
import me.googol.chatEdit.inter.ScreenInterface;
import me.googol.chatEdit.screens.TextScreen;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin implements AnvilScreenInterface {
    @Unique
    private boolean was_active = false;
    @Unique
    private String deferInsert = "";
    @Shadow
    private EditBox name;

    @Shadow
    protected abstract void onNameChanged(String string);

    @Inject(method = "keyPressed",at = @At("HEAD"), cancellable = true)
    public void keyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir){
        if(!name.canConsumeInput())return;
        Minecraft mc = ((ScreenInterface)this).funnyBinds$getMinecraft();
        if(mc == null){
            ChatEditClient.LOGGER.info("minecraft not found!");
            return;
        }
        TextScreen ts;
        if(keyEvent.hasControlDown() && keyEvent.key() == GLFW.GLFW_KEY_F) {
            ts = new TextScreen(name.getValue(),mc.screen);
            ts.addRangeText(mc.gui.getChat().getRecentChat());
            ts.addText(name.getValue());
            deferInsert = name.getValue();
            was_active = true;
            //ts.init
            mc.setScreen(ts);
            cir.setReturnValue(true);

            return;
        }
        else if(keyEvent.hasControlDown() && keyEvent.key() == GLFW.GLFW_KEY_T) {
            ts = new TextScreen(name.getValue(),mc.screen);
            List<GuiMessage> vas = ((ChatCompInterface)mc.gui.getChat()).funnyBinds$getAllMessages().reversed();
            ChatEditClient.LOGGER.info(""+vas.size());
            for(GuiMessage gm : vas){
                Component cont = gm.content();
                ts.addText(cont.getString());
                ChatEditClient.LOGGER.info(cont.getString());
            } //  */
            //ts.addRangeText(mc.gui.getChat());
            ts.addText(name.getValue());
            deferInsert = name.getValue();
            was_active = true;
            //ts.init
            mc.setScreen(ts);
            cir.setReturnValue(true);
            return;
        }
    }

    @Override
    public void funnyBinds$setText(String text){
        //commandEdit.setValue(text);
        deferInsert = text;
    }

    @Inject(method = "containerTick",at = @At("HEAD"))
    protected void containerTick(CallbackInfo ci){
        if(was_active){
            name.setValue(deferInsert);
            was_active = false;
            onNameChanged(deferInsert);
        }
    }
}
