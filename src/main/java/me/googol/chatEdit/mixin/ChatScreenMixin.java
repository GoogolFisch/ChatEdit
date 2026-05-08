package me.googol.chatEdit.mixin;

import me.googol.chatEdit.client.ChatEditClient;
import me.googol.chatEdit.inter.ChatCompInterface;
import me.googol.chatEdit.inter.ScreenInterface;
import me.googol.chatEdit.screens.TextScreen;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

    @Shadow
    public abstract void moveInHistory(int i);

    @Shadow
    protected EditBox input;

    @Inject(method = "keyPressed",at = @At("HEAD"),cancellable = true)
    public void keyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = ((ScreenInterface)this).funnyBinds$getMinecraft();
        if(mc == null){
            ChatEditClient.LOGGER.info("minecraft not found!");
            return;
        }
        TextScreen ts;
        //Minecraft mc = minecraft;
        if(keyEvent.hasControlDown() && keyEvent.key() == GLFW.GLFW_KEY_N){
            this.moveInHistory(1);
            cir.setReturnValue(true);
            return;
        }
        else if(keyEvent.hasControlDown() && keyEvent.key() == GLFW.GLFW_KEY_P){
            this.moveInHistory(-1);
            cir.setReturnValue(true);
            return;
        }
        else if(keyEvent.hasControlDown() && keyEvent.key() == GLFW.GLFW_KEY_H){
            mc.gui.getChat().scrollChat(mc.gui.getChat().getLinesPerPage() - 1);
            cir.setReturnValue(true);
            return;
        }
        else if(keyEvent.hasControlDown() && keyEvent.key() == GLFW.GLFW_KEY_L){
            mc.gui.getChat().scrollChat(-mc.gui.getChat().getLinesPerPage() + 1);
            cir.setReturnValue(true);
            return;
        }
        else if(keyEvent.hasControlDown() && keyEvent.key() == GLFW.GLFW_KEY_F) {
            ts = new TextScreen(input.getValue(),mc.screen);
            ts.addRangeText(mc.gui.getChat().getRecentChat());
            ts.addText(input.getValue());
            //ts.init
            mc.setScreen(ts);
            cir.setReturnValue(true);

            return;
        }
        else if(keyEvent.hasControlDown() && keyEvent.key() == GLFW.GLFW_KEY_T) {
            ts = new TextScreen(input.getValue(),mc.screen);
            List<GuiMessage> vas = ((ChatCompInterface)mc.gui.getChat()).funnyBinds$getAllMessages().reversed();
            ChatEditClient.LOGGER.info(""+vas.size());
            for(GuiMessage gm : vas){
                Component cont = gm.content();
                ts.addText(cont.getString());
                ChatEditClient.LOGGER.info(cont.getString());
            } //  */
            //ts.addRangeText(mc.gui.getChat());
            ts.addText(input.getValue());
            //ts.init
            mc.setScreen(ts);
            cir.setReturnValue(true);
            return;
        }
        //cir.setReturnValue(true);
    }
    @Inject(method = "init",at = @At("TAIL"))
    public void o(CallbackInfo ci){
        this.input.setMaxLength(1024);
    }
}
