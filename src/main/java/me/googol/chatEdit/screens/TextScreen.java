package me.googol.chatEdit.screens;

import me.googol.chatEdit.inter.AnvilScreenInterface;
import me.googol.chatEdit.inter.BookEditScreenInterface;
import me.googol.chatEdit.inter.CommandBlockScreenInterface;
import me.googol.chatEdit.inter.SignScreenInterface;
import me.googol.chatEdit.screens.widget.TextBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractCommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedList;
import java.util.List;

public class TextScreen extends Screen {
    protected String initial;
    public Screen lastScreen;
    protected EditBox cmd;
    protected TextBox box;
    boolean doAcceptInput;
    private List<String> deferAppendBox;
    public AbstractWidget focusText;
    public TextScreen(String string, Screen parent) {
        super(Component.translatable("chat_screen.title"));
        this.doAcceptInput = false;
        this.initial = string;
        lastScreen = parent;
        deferAppendBox = null;
    }
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }
    public void onCloseAccept(String outp){
        onClose();
        if(lastScreen instanceof ChatScreen){
            ChatScreen cs = (ChatScreen)lastScreen;
            cs.insertText(outp,true);
        }
        if(lastScreen instanceof AbstractCommandBlockEditScreen acbs){
            ((CommandBlockScreenInterface)acbs).funnyBinds$setText(outp);
        }
        if(lastScreen instanceof AbstractSignEditScreen ases){
            ((SignScreenInterface)ases).funnyBinds$setText(outp);
        }
        if(lastScreen instanceof BookEditScreen bes){
            ((BookEditScreenInterface)bes).funnyBinds$setText(outp);
        }
        if(lastScreen instanceof AnvilScreen as){
            ((AnvilScreenInterface)as).funnyBinds$setText(outp);
        }
    }
    protected void init() {

        this.cmd = new EditBox(this.minecraft.fontFilterFishy, 4, this.height - 12, this.width - 4, 12, Component.translatable("chat.editBox")) {
            protected MutableComponent createNarrationMessage() {
                return super.createNarrationMessage();//.append(TextScreen.this.commandSuggestions.getNarrationMessage());
            }
        };
        this.cmd.setMaxLength(1024);
        this.cmd.setBordered(false);
        this.cmd.setValue(this.initial);
        this.cmd.setResponder(this::onEdited);
        this.cmd.addFormatter(this::formatChat);
        this.cmd.setCanLoseFocus(true);
        this.addRenderableWidget(this.cmd);
        this.box = new TextBox(this,minecraft.fontFilterFishy, 4,4,this.width - 8, this.height - font.lineHeight * 2,Component.empty());
        //this.box.appendText("Hello");
        this.addRenderableWidget(this.box);
        if(deferAppendBox != null){
            box.appendTexts(deferAppendBox);
        }
        setFocused(box);
        /*
        this.commandSuggestions = new CommandSuggestions(this.minecraft, this, this.input, this.font, false, false, 1, 10, true, -805306368);
        this.commandSuggestions.setAllowHiding(false);
        this.commandSuggestions.setAllowSuggestions(false);
        this.commandSuggestions.updateCommandInfo();
         */
    }
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        guiGraphics.fill(2, this.height - 14, this.width - 2, this.height - 2, this.minecraft.options.getBackgroundColor(Integer.MIN_VALUE));
        //this.minecraft.gui.getChat().render(guiGraphics, this.font, this.minecraft.gui.getGuiTicks(), i, j, true, this.insertionClickMode());
        //this.cmd.render(guiGraphics,i,j,f);
        //guiGraphics.drawString(font,"Hello World",10,20,200);
        super.render(guiGraphics, i, j, f);
        //this.commandSuggestions.render(guiGraphics, i, j);
    }
    private boolean insertionClickMode() {
        return this.minecraft.hasShiftDown();
    }

    public void focusCmd(String op){
        this.setFocused(cmd);
        cmd.setValue(op);
    }

    public boolean keyPressed(KeyEvent keyEvent){
        //GLFW.GLFW_KEY_BACKSPACE
        if(this.getFocused() != null && this.getFocused().keyPressed(keyEvent)){
            if(this.getFocused() == cmd && cmd.getValue().isEmpty()){
                this.setFocused(box);
                cmd.setFocused(false);
            }
            return true;
        }else if(this.getFocused() != null && keyEvent.key() == GLFW.GLFW_KEY_ESCAPE){
            this.setFocused(box);
            cmd.setFocused(false);
            return true;
        }else if(this.getFocused() != null && keyEvent.isConfirmation()){
            this.setFocused(box);
            cmd.setFocused(false);
            box.passIntoCommand(cmd.getValue());
            return true;
        }
        else if(keyEvent.isConfirmation()) {
            //onClose();
            onCloseAccept(cmd.getValue() + "Blaaaa");
            return true;
        }
        /*else if (super.keyPressed(keyEvent)) {
            return true;
        }/*else if(focusText != null){
            return focusText.keyPressed(keyEvent);
        }*/
        return false;
    }
    public boolean charTyped(CharacterEvent characterEvent) {
        if(box.isFocused()){
            box.charTyped(characterEvent);
            return true;
        }
        return super.charTyped(characterEvent);
    }
    private void onEdited(String string) {
        /*
        this.commandSuggestions.setAllowSuggestions(true);
        this.commandSuggestions.updateCommandInfo();
        this.isDraft = false;*/
    }
    private @Nullable FormattedCharSequence formatChat(String string, int i) {
        //return this.isDraft ? FormattedCharSequence.forward(string, Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true)) : null;
        return null;
    }

    public void addRangeText(List<String> lines){
        if(box != null)
            box.appendTexts(lines);
        else if(deferAppendBox == null){
            deferAppendBox = new LinkedList<>();
            deferAppendBox.addAll(lines);
        }else{
            deferAppendBox.addAll(lines);
        }
    }
    public void addText(String line) {
        if(box != null) {
            box.appendText(line);
            return;
        }
        else if(deferAppendBox == null)
            deferAppendBox = new LinkedList<>();
        deferAppendBox.add(line);
    }
    public void setDoAccept(boolean doAcc){
        this.doAcceptInput = doAcc;
        if(box != null)
            box.doAccept = doAcc;
    }
}
