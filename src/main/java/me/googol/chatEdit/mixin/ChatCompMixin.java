package me.googol.chatEdit.mixin;

import me.googol.chatEdit.inter.ChatCompInterface;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(ChatComponent.class)
public class ChatCompMixin implements ChatCompInterface {
    @Shadow
    @Final
    private List<GuiMessage> allMessages;

    @Override
    public List<GuiMessage> funnyBinds$getAllMessages() {
        return allMessages;
    }
}
