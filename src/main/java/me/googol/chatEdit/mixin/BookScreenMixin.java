package me.googol.chatEdit.mixin;

import me.googol.chatEdit.client.ChatEditClient;
import me.googol.chatEdit.inter.BookEditScreenInterface;
import me.googol.chatEdit.inter.ChatCompInterface;
import me.googol.chatEdit.inter.ScreenInterface;
import me.googol.chatEdit.screens.TextScreen;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(BookEditScreen.class)
public abstract class BookScreenMixin implements BookEditScreenInterface {

    @Shadow
    @Final
    private List<String> pages;
    @Shadow
    private int currentPage;

    @Shadow
    protected abstract int getNumPages();

    @Shadow
    protected abstract void updatePageContent();

    @Shadow
    protected abstract void pageForward();

    @Unique
    private boolean was_active = false;
    @Unique
    private String deferInsert = "";


    @Inject(method = "render",at=@At("TAIL"))
    public void render(CallbackInfo ci){
        if(!was_active)return;
        BookEditScreen bes = (BookEditScreen) (Object)this;
        //
        if(!deferInsert.isEmpty()) {
            pages.set(currentPage, deferInsert);

            updatePageContent();
        }
        deferInsert = "";
        was_active = false;
    }//  **/

    @Inject(method = "keyPressed",at = @At("HEAD"), cancellable = true)
    public void keyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir){
        Minecraft mc = ((ScreenInterface)this).funnyBinds$getMinecraft();
        if(mc == null){
            ChatEditClient.LOGGER.info("minecraft not found!");
            return;
        }
        TextScreen ts;
        if(keyEvent.hasControlDown() && keyEvent.key() == GLFW.GLFW_KEY_P) {
            if(currentPage > 0) {
                currentPage--;
                updatePageContent();
            }
            cir.setReturnValue(true);
            return;
        }
        else if(keyEvent.hasControlDown() && keyEvent.hasShiftDown() && keyEvent.key() == GLFW.GLFW_KEY_N) {
            if(currentPage < getNumPages()){
                pageForward();
            }
            cir.setReturnValue(true);
            return;
        }
        else if(keyEvent.hasControlDown() && keyEvent.key() == GLFW.GLFW_KEY_N) {
            if(currentPage < getNumPages() - 1){
                currentPage++;
                updatePageContent();
            }
            cir.setReturnValue(true);
            return;
        }
        else if(keyEvent.hasControlDown() && keyEvent.key() == GLFW.GLFW_KEY_F) {
            ts = new TextScreen(pages.get(currentPage),mc.screen);
            ts.addRangeText(mc.gui.getChat().getRecentChat());
            ts.addText(pages.get(currentPage));
            was_active = true;
            //ts.init
            mc.setScreen(ts);
            cir.setReturnValue(true);

            return;
        }
        else if(keyEvent.hasControlDown() && keyEvent.key() == GLFW.GLFW_KEY_T) {
            ts = new TextScreen(pages.get(currentPage),mc.screen);
            List<GuiMessage> vas = ((ChatCompInterface)mc.gui.getChat()).funnyBinds$getAllMessages().reversed();
            ChatEditClient.LOGGER.info(""+vas.size());
            for(GuiMessage gm : vas){
                Component cont = gm.content();
                ts.addText(cont.getString());
                ChatEditClient.LOGGER.info(cont.getString());
            } //  */
            //ts.addRangeText(mc.gui.getChat());
            ts.addText(pages.get(currentPage));
            was_active = true;
            //ts.init
            mc.setScreen(ts);
            cir.setReturnValue(true);
            return;
        }
    }

    @Override
    public void funnyBinds$setText(String text) {
        deferInsert = text;
    }
}
