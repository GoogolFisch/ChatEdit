package me.googol.chatEdit.mixin;

import me.googol.chatEdit.client.ChatEditClient;
import me.googol.chatEdit.inter.ChatCompInterface;
import me.googol.chatEdit.inter.CommandBlockScreenInterface;
import me.googol.chatEdit.inter.ScreenInterface;
import me.googol.chatEdit.screens.TextScreen;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractCommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
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

@Mixin(AbstractCommandBlockEditScreen.class)
public abstract class AbsCommandBlockScreenMixin implements CommandBlockScreenInterface {

    @Unique
    private boolean was_active = false;
    @Unique
    private String deferInsert = "";
    @Inject(method = "tick",at=@At("TAIL"))
    public void tick(CallbackInfo ci){
        if(!was_active)return;
        if(((Object)this) instanceof CommandBlockEditScreen){
            CommandBlockEditScreen cbes = (CommandBlockEditScreen) (Object)this;
            cbes.updateGui();
            if(!deferInsert.isEmpty())
                commandEdit.setValue(deferInsert);
        }
        deferInsert = "";
        was_active = false;
    }//  **/

    @Shadow
    protected EditBox commandEdit;

    @Inject(method = "keyPressed",at = @At("HEAD"), cancellable = true)
    public void keyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir){
        Minecraft mc = ((ScreenInterface)this).funnyBinds$getMinecraft();
        if(mc == null){
            ChatEditClient.LOGGER.info("minecraft not found!");
            return;
        }
        TextScreen ts;
        if(keyEvent.hasControlDown() && keyEvent.key() == GLFW.GLFW_KEY_F) {
            ts = new TextScreen(commandEdit.getValue(),mc.screen);
            ts.addRangeText(mc.gui.getChat().getRecentChat());
            ts.addText(commandEdit.getValue());
            deferInsert = commandEdit.getValue();
            was_active = true;
            //ts.init
            mc.setScreen(ts);
            cir.setReturnValue(true);

            return;
        }
        else if(keyEvent.hasControlDown() && keyEvent.key() == GLFW.GLFW_KEY_T) {
            ts = new TextScreen(commandEdit.getValue(),mc.screen);
            List<GuiMessage> vas = ((ChatCompInterface)mc.gui.getChat()).funnyBinds$getAllMessages().reversed();
            ChatEditClient.LOGGER.info(""+vas.size());
            for(GuiMessage gm : vas){
                Component cont = gm.content();
                ts.addText(cont.getString());
                ChatEditClient.LOGGER.info(cont.getString());
            } //  */
            //ts.addRangeText(mc.gui.getChat());
            ts.addText(commandEdit.getValue());
            deferInsert = commandEdit.getValue();
            was_active = true;
            //ts.init
            mc.setScreen(ts);
            cir.setReturnValue(true);
            return;
        }
    }
    public void funnyBinds$setText(String text){
        //commandEdit.setValue(text);
        deferInsert = text;
    }
}
