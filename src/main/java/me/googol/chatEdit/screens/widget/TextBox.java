package me.googol.chatEdit.screens.widget;

import me.googol.chatEdit.screens.TextScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Tuple;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class TextBox extends AbstractWidget {
    public enum VimState{
        Normal,Insert,
        Visual,VisualLines,
        Replace,ReplaceContinue,
    }
    public static class Rect {
        public int staX,staY;
        public int endX,endY;
        public int posX,posY;
        public Rect(int sX,int sY,int eX,int eY,int pX,int pY){
            staX = sX;staY = sY;endX = eX;endY = eY;
            posX = pX; posY = pY;
        }
        public Rect sort(){
            if(staY > endY || (staY == endY && staX > endX)){
                return new Rect(endX, endY, staX, staY, posX, posY);
            }
            return this;
        }
    }
    ////
    public List<String> renderText;
    public Font font;
    public boolean doAccept = false;
    long focusedTime;
    Rect selection = new Rect(0, 0, 0, 0, 0, 0);
    int cursorX,cursorY;
    int xStart,yStart = 0;
    int scroll,linePer;
    static String copied = "";
    String fileName,compound,findNext;
    Screen parent;
    VimState state;
    private final List<EditBox.TextFormatter> formatters;

    public TextBox(Screen parent,Font font,int sx, int sy, int wid, int hei, Component component) {
        super(sx, sy, wid, hei, component);
        this.state = VimState.Normal;
        this.font = font;
        compound = "";
        findNext = "";
        fileName = "";
        this.parent = parent;
        renderText = new LinkedList<String>();
        this.formatters = new ArrayList<>();
        //
        cursorX = cursorY = -1;
        xStart = sx;
        focusedTime = 0;
        yStart = sy;
        scroll = 0;
        linePer = 0;
    }

    public boolean isVisible() {
        return this.visible;
    }
    public void addFormatter(EditBox.TextFormatter textFormatter) {
        this.formatters.add(textFormatter);
    }
    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        if (!this.isVisible())return;
        int FRAME_WINDOW = 5;// dead-space before auto scroll
        int xIter = xStart;
        int yIter = yStart;
        int lnCursor = -1;
        int cursorColor = 0xffaaccff;
        int textColor = 0xffffeeaa;
        //
        if     (state == VimState.Insert         ){cursorColor = 0xffffbbbb;}
        else if(state == VimState.Visual         ){cursorColor = 0xffbbffaa;}
        else if(state == VimState.Replace        ){cursorColor = 0xffffAA00;}
        else if(state == VimState.ReplaceContinue){cursorColor = 0xffffff00;}
        // else if(state ==  ){cursorColor = 0xffaa88ff;} // reserve
        //
        //int linePer = guiGraphics.guiHeight() / font.lineHeight;
        linePer = height / font.lineHeight;
        if(scroll > cursorY - FRAME_WINDOW){
            scroll = cursorY - FRAME_WINDOW;
        }
        if(scroll + linePer < cursorY + FRAME_WINDOW){
            scroll = cursorY + FRAME_WINDOW - linePer;
        }//  */
        scroll = Math.max(0,scroll);
        //
        boolean blink = this.isFocused() && (Util.getMillis() - this.focusedTime) / 300L % 2L == 0L;
        boolean didDraw = false;

        String line;
        int idx = scroll;
        int renderCh = 0;
        Rect sorted = selection.sort();
        int lowerSelX = sorted.staX,lowerSelY = sorted.staY;
        int upperSelX = sorted.endX,upperSelY = sorted.endY;
        /*if(lowerSelY > upperSelY || (lowerSelY == upperSelY && lowerSelX > upperSelX)){
            lowerSelX = cursorX;lowerSelY = cursorY;
        }*/
        for(int counter = 0;counter < linePer && idx < renderText.size();counter++){
            line = renderText.get(idx);
            String renderStr = this.font.plainSubstrByWidth(line.substring(renderCh),width - 20);
            guiGraphics.drawString(this.font,this.applyFormat(renderStr,lnCursor - 1),xIter,yIter,textColor,true);
            if(cursorY == idx && (
                    (renderCh <= cursorX && cursorX <= renderCh + renderStr.length()) ||
                            (renderCh + renderStr.length() == line.length()))
            ){
                didDraw = true;
                int saveX = Math.min(cursorX - renderCh,renderStr.length());
                if(blink && saveX >= 0)
                    if (this.cursorX != line.length()) {
                        Objects.requireNonNull(this.font);
                        saveX = xStart + this.font.width(renderStr.substring(0,saveX));
                        guiGraphics.fill(saveX, yIter - 1, saveX + 1, yIter + 9, cursorColor);
                    } else {
                        guiGraphics.drawString(this.font, "_", xStart+ this.font.width(renderStr), yIter, cursorColor, true);
                    }
            }
            boolean doMark = lowerSelY < idx && idx < upperSelY;
            doMark |= lowerSelY == idx && lowerSelX < renderCh + renderStr.length();
            doMark |= idx == upperSelY && renderCh <= upperSelX;
            //if(lowerSelY <= idx && idx <= upperSelY && lowerSelX < renderCh + renderStr.length() && renderCh <= upperSelX){
            if(doMark){
                int stOff = Math.min(renderStr.length(),Math.max(0,lowerSelX - renderCh));
                int edOff = Math.min(renderStr.length(),Math.max(0,upperSelX - renderCh));
                int vsta,vend;
                if(lowerSelY < idx)
                    vsta = xStart;
                else
                    vsta = xStart + this.font.width(renderStr.substring(0,stOff));
                if(idx < upperSelY)
                    vend = xStart + this.font.width(renderStr);
                else
                    vend = xStart + this.font.width(renderStr.substring(0,edOff));
                //int visX = xStart + this.font.width(renderStr.substring(0,lowerSelX - renderCh));
                guiGraphics.textHighlight(vsta,yIter - 1,vend,yIter + 8,true);

            }
            //
            renderCh += renderStr.length();
            if(renderCh >= line.length()){
                idx++;
                renderCh = 0;
            }
            xIter = xStart;
            yIter += font.lineHeight;
        }
        if(!didDraw)
            scroll++;

    }
    public void setFocused(boolean foc){
        super.setFocused(foc);
        this.focusedTime = Util.getMillis();
        if(renderText.isEmpty()) {
            cursorX = 0;
            cursorY = 0;
            renderText.add("");
        }
    }
    private FormattedCharSequence applyFormat(String string, int i) {
        for(EditBox.TextFormatter textFormatter : this.formatters) {
            FormattedCharSequence formattedCharSequence = textFormatter.format(string, i);
            if (formattedCharSequence != null) {
                return formattedCharSequence;
            }
        }

        return FormattedCharSequence.forward(string, Style.EMPTY);
    }

    private void resetSelection(){
        selection = new Rect(cursorX, cursorY, cursorX, cursorY, cursorX, cursorY);
    }

    public int moveFind(int offset, int start, char stop) {
        int iter = start;
        boolean dir = offset < 0;
        int count = Math.abs(offset);
        String base = renderText.get(cursorY);
        for(int counter = 0; counter < count; ++counter) {
            if (!dir) {
                int length = base.length();
                iter = base.indexOf(stop, iter);
                iter++;
                if (iter == 0 || iter > length) {
                    iter = length;
                }
            } else {
                while(iter > 0 && base.charAt(iter - 1) != stop) {
                    --iter;
                }
                iter--;
            }
        }
        return iter;
    }
    public Tuple<Integer,Integer> getWordPosition(int offset, int stX,int stY, boolean whiteLastSkip,boolean wordSkipping) {
        int iterX = stX;
        int iterY = stY;
        boolean dir = offset < 0;
        int wskipCount = Math.abs(offset);

        String base = renderText.get(iterY);
        for(int counter = 0; counter < wskipCount; ++counter) {
            if(iterY >= renderText.size()) break;
            //
            if (!dir) {
                int length = base.length();
                if(!whiteLastSkip && iterX > length) {
                    iterY++;
                    if(iterY >= renderText.size())break;
                    iterX = 0;
                    base = renderText.get(iterY);
                    length = base.length();
                }
                iterX = base.indexOf(32, iterX);
                if (iterX == -1) {
                    if(whiteLastSkip) {
                        iterY++;
                        if(iterY >= renderText.size())break;
                        iterX = 0;
                        base = renderText.get(iterY);
                    }else{
                        iterX = length;
                    }
                } else {
                    if(counter == wskipCount - 1 && !whiteLastSkip)continue;
                    while(iterX < length && base.charAt(iterX) == ' ') {
                        ++iterX;
                    }
                }
            } else {
                if(iterX <= 0){
                    iterY--;
                    base = renderText.get(iterY);
                    iterX = base.length();
                }
                while(iterX > 0 && base.charAt(iterX - 1) == ' ') {
                    --iterX;
                }

                while(iterX > 0 && base.charAt(iterX - 1) != ' ') {
                    --iterX;
                }
            }
        }
        return new Tuple<>(iterX,iterY);
    }
    public Rect getDelta(String data){
        int length = data.length();
        if(length == 0) {
            return null;
            //return new Rect(cursorX, cursorY, cursorX, cursorY + 1, cursorX + 1, cursorY + 1);
        }
        char codep = data.charAt(0);
        int saveState;
        int repeatCount = 1;
        int stX = cursorX,stY = cursorY;
        if(stY >= renderText.size())stY = 0;
        saveState = renderText.get(Math.min(cursorY,renderText.size() - 1)).length();
        Tuple<Integer,Integer> savePos;
        switch (codep){
            case 'n':
                while(stX == -1 || repeatCount > 0) {
                    stX = renderText.get(stY).indexOf(findNext,stX + 1);
                    if(stX != -1) {
                        repeatCount--;
                        continue;
                    }
                    //stX = -1;
                    stY++;
                    if(stY >= renderText.size()){
                        stY = 0;
                    }
                    if(stY == cursorY){
                        return new Rect(cursorX, cursorY, cursorX, cursorY, cursorX, cursorY);
                    }
                }
                return new Rect(cursorX, cursorY, stX, stY, stX, stY);
            case 'N':
                while(stX == -1 || repeatCount > 0) {
                    stX = renderText.get(stY).lastIndexOf(findNext,stX - 1);
                    if(stX != -1) {
                        repeatCount--;
                        continue;
                    }
                    stY--;
                    if(stY < 0){
                        stY = renderText.size() - 1;
                    }
                    stX = renderText.get(stY).length();
                    if(stY == cursorY){
                        return new Rect(cursorX, cursorY, cursorX, cursorY, cursorX, cursorY);
                    }
                }
                return new Rect(cursorX, cursorY, stX, stY, stX, stY);
            case ' ':
            case 'l':
                saveState = Math.min(cursorX + repeatCount,saveState);
                return new Rect(cursorX, cursorY, saveState, cursorY, saveState, cursorY);
            case 'h':
                saveState = Math.max(0,cursorX - repeatCount);
                return new Rect(cursorX, cursorY, saveState, cursorY, saveState, cursorY);
            case 'k':
                saveState = Math.max(0,cursorY - repeatCount);
                return new Rect(0, saveState, 0, cursorY + 1, cursorX, saveState);
            case 'j':
                saveState = Math.min(renderText.size() - 1,cursorY + repeatCount);
                return new Rect(0, cursorY, 0, saveState + 1, cursorX, saveState);
            case '0': return new Rect(0, cursorY, cursorX, cursorY, 0, cursorY);
            case '$':
                return new Rect(cursorX, cursorY, saveState, cursorY, saveState, cursorY);
            case '^':
                String s = renderText.get(cursorY);
                if(!s.isEmpty() && s.charAt(0) == ' ')
                    savePos = getWordPosition(1,0,cursorY,true,false);
                else savePos = new Tuple<>(0,cursorY);
                return new Rect(savePos.getA(), savePos.getB(), cursorX, cursorY, savePos.getA(), savePos.getB());
            case 'w':
                savePos = getWordPosition(repeatCount,cursorX,cursorY,true,false);
                return new Rect(cursorX, cursorY, savePos.getA(), savePos.getB(), savePos.getA(), savePos.getB());
            case 'W':
                savePos = getWordPosition(repeatCount,cursorX,cursorY,true,true);
                return new Rect(cursorX, cursorY, savePos.getA(), savePos.getB(), savePos.getA(), savePos.getB());
            case 'e':
                savePos = getWordPosition(repeatCount,cursorX + 1,cursorY,false,false);
                return new Rect(cursorX, cursorY, savePos.getA(), savePos.getB(), savePos.getA(), savePos.getB());
            case 'E':
                savePos = getWordPosition(repeatCount,cursorX + 1,cursorY,false,true);
                return new Rect(cursorX, cursorY, savePos.getA(), savePos.getB(), savePos.getA(), savePos.getB());
            case 'b':
                savePos = getWordPosition(-repeatCount,cursorX,cursorY,true,false);
                return new Rect(savePos.getA(), savePos.getB(), cursorX, cursorY, savePos.getA(), savePos.getB());
            case 'B':
                savePos = getWordPosition(-repeatCount,cursorX,cursorY,true,true);
                return new Rect(savePos.getA(), savePos.getB(), cursorX, cursorY, savePos.getA(), savePos.getB());
            case 't':
                if(length < 2)return null;
                saveState = moveFind(repeatCount,cursorX,data.charAt(1)) - 1;
                return new Rect(cursorX, cursorY, saveState, cursorY, saveState, cursorY);
            case 'T':
                if(length < 2)return null;
                saveState = moveFind(-repeatCount,cursorX,data.charAt(1)) + 1;
                return new Rect(saveState, cursorY, cursorX, cursorY, saveState, cursorY);
            case 'f':
                if(length < 2)return null;
                saveState = moveFind(repeatCount,cursorX,data.charAt(1));
                return new Rect(cursorX, cursorY, saveState, cursorY, saveState, cursorY);
            case 'F':
                if(length < 2)return null;
                saveState = moveFind(-repeatCount,cursorX,data.charAt(1));
                return new Rect(saveState, cursorY, cursorX, cursorY, saveState, cursorY);
            case 'G':
                saveState = renderText.size() - 1;
                int savedX = renderText.get(saveState).length();
                return new Rect(cursorX, cursorY, savedX, saveState, savedX, saveState);
            case 'g':
                if(length < 2)return null;
                if(compound.charAt(1) == 'g'){
                    return new Rect(0, 0, cursorX, cursorY, 0, 0);
                }
                return new Rect(saveState, cursorY, cursorX, cursorY, saveState, cursorY);
            case '\\':
                if(length < 3)break;
                if(compound.charAt(1) == '2'){
                    if(compound.charAt(2) == 'd'){
                        saveState = Math.min(renderText.size() - 1,cursorY + (linePer / 2) * repeatCount);
                        return new Rect(0, cursorY, 0, saveState + 1, cursorX, saveState);
                    }
                    if(compound.charAt(2) == 'u'){
                        saveState = Math.max(0,cursorY - (linePer / 2) * repeatCount);
                        return new Rect(0, cursorY, 0, saveState + 1, cursorX, saveState);
                    }
                }
                break;
            case '%':
                return moveInBlock();
        }
        return new Rect(0, cursorY, 0, cursorY + 1, cursorX, cursorY + 1);
    }
    public Rect moveInBlock(){
        char dow = 0;
        char dUp = 0;
        int depth = 0;
        int posY = cursorY;
        int posX = cursorX;
        String here = renderText.get(posY);
        for(posX = cursorX;posX < here.length();posX++){
            char nex = here.charAt(posX);
            //if(dow != 0)continue;
            if(nex == '('){dow = nex;dUp = ')';break;}
            if(nex == '['){dow = nex;dUp = ']';break;}
            if(nex == '{'){dow = nex;dUp = '}';break;}
            if(nex == ')'){dow = nex;dUp = '(';break;}
            if(nex == ']'){dow = nex;dUp = '[';break;}
            if(nex == '}'){dow = nex;dUp = '{';break;}
            //
        }
        if(dow == 0)
            return new Rect(cursorX, cursorY, cursorX, cursorY, cursorX, cursorY);
        if(dow == '(' || dow == '[' || dow == '{'){
            do{
                if(posX >= here.length()){
                    posX = 0;
                    posY++;
                    if(posY >= renderText.size())
                        break;
                    here = renderText.get(posY);
                }
                char nex = here.charAt(posX);
                if(nex == dow)depth++;
                if(nex == dUp)depth--;
                posX++;
            }while(depth > 0);
            posX--;
        }else{
            do{
                if(posX < 0){
                    posY--;
                    if(posY >= renderText.size())
                        break;
                    here = renderText.get(posY);
                    posX = here.length() - 1;
                }
                char nex = here.charAt(posX);
                if(nex == dow)depth++;
                if(nex == dUp)depth--;
                posX--;
            }while(depth > 0);
            posX++;
        }

        if(depth != 0)
            return new Rect(cursorX, cursorY, cursorX, cursorY, cursorX, cursorY);
        return new Rect(cursorX, cursorY, posX, posY, posX, posY);
    }
    public void limitCursorX(){
        if(cursorX < 0)
            cursorX = 0;
        String basel = renderText.get(cursorY);
        if(cursorX > basel.length())
            cursorX = basel.length();
    }
    public void limitCursorY(){
        if(cursorY < 0)
            cursorY = 0;
        if(cursorY >= renderText.size())
            cursorY = renderText.size() - 1;
    }
    public boolean keyPressed(KeyEvent keyEvent) {
        if (!(this.isActive() && this.isFocused()))
            return false;
        boolean isDone = false;
        if(keyEvent.isLeft()){
            cursorX = Math.max(0,cursorX - 1);return true;
        }
        if(keyEvent.isRight()){
            cursorX = Math.min(renderText.get(cursorY).length(),cursorX + 1);return true;
        }
        if(keyEvent.isUp()){
            cursorY = Math.max(0,cursorY - 1);return true;
        }
        if(keyEvent.isDown()){
            cursorY = Math.min(renderText.size() - 1,cursorY + 1);return true;
        }
        if(keyEvent.hasControlDown() && keyEvent.hasShiftDown() && keyEvent.key() == GLFW.GLFW_KEY_V){compound += "\\2V";isDone = true;}
        else if(keyEvent.hasControlDown() && keyEvent.key() == GLFW.GLFW_KEY_U){compound += "\\2u";isDone = true;}
        else if(keyEvent.hasControlDown() && keyEvent.key() == GLFW.GLFW_KEY_D){compound += "\\2d";isDone = true;}
        else if(keyEvent.hasControlDown() && keyEvent.key() == GLFW.GLFW_KEY_C){compound += "\\2c";isDone = true;}
        else if(keyEvent.hasControlDown() && keyEvent.key() == GLFW.GLFW_KEY_V){compound += "\\2v";isDone = true;}
        //
        if(state == VimState.Normal) {
            if (keyEvent.key() == GLFW.GLFW_KEY_ESCAPE) {
                parent.onClose();
                return true;
            }
            if(keyEvent.isConfirmation()){
                if(parent instanceof TextScreen tsp && !doAccept) {
                    if(renderText.size() <= cursorY)
                        tsp.onCloseAccept("");
                    else
                        tsp.onCloseAccept(renderText.get(cursorY));
                }
                return true;
            }
            if(keyEvent.key() == GLFW.GLFW_KEY_BACKSPACE){
                cursorX = Math.max(0,cursorX - 1);
                return true;
            }
            if(isDone){
                parseNormalCompound(null);
                return true;
            }
        }
        if(state != VimState.Normal) {
            if(keyEvent.key() == GLFW.GLFW_KEY_ESCAPE){
                state = VimState.Normal;
                cursorX--;
                limitCursorX();
                resetSelection();
                return true;
            }
        }
        if(state == VimState.Insert) {
            if (keyEvent.isConfirmation()) {
                if (parent instanceof TextScreen tsp && !doAccept) {
                    if (renderText.size() <= cursorY)
                        tsp.onCloseAccept("");
                    else
                        tsp.onCloseAccept(renderText.get(cursorY));
                }
                return true;
            }
            if(keyEvent.key() == GLFW.GLFW_KEY_BACKSPACE){
                deleteText(new Rect(cursorX - 1, cursorY, cursorX, cursorY, cursorX, cursorY));
                cursorX = Math.max(0,cursorX - 1);
                return true;
            }
            if(compound.equals("\\2v")){
                String s = Minecraft.getInstance().keyboardHandler.getClipboard();
                insertText(s,false);
                cursorX += s.length();
                limitCursorX();
            }
            if(compound.equals("\\2V")){
                String s = Minecraft.getInstance().keyboardHandler.getClipboard();
                insertChar(s);
                cursorX += s.length();
                limitCursorX();
            }
            compound = "";
        }
        if(state == VimState.Visual) {
            if (keyEvent.isConfirmation()) {
                return true;
            }
            if(keyEvent.key() == GLFW.GLFW_KEY_BACKSPACE){
                deleteText(new Rect(cursorX - 1, cursorY, cursorX, cursorY, cursorX, cursorY));
                cursorX = Math.max(0,cursorX - 1);
                return true;
            }
            if(isDone){
                parseVisualCompound();
                return true;
            }
        }
        return false;
    }
    public boolean charTyped(CharacterEvent characterEvent) {
        if (!this.canConsumeInput()) {
            return false;
        }
        int codep = characterEvent.codepoint();
        if(state == VimState.Normal) {
            if(codep == '\\'){compound += "\\";}
            if((characterEvent.modifiers() & ~GLFW.GLFW_MOD_SHIFT) != 0){
                //String[] apping = {"","\\s","\\c","\\S","\\a","\\A","\\g"};
                // this is shift, alt and ctrl
                compound += '0' + (characterEvent.modifiers() & (GLFW.GLFW_MOD_SUPER - 1) & ~GLFW.GLFW_MOD_SHIFT);
            }
            compound += characterEvent.codepointAsString();
            parseNormalCompound(null);
            return true;
        }
        if(state == VimState.Visual || state == VimState.VisualLines) {
            if(codep == '\\'){compound += "\\";}
            if((characterEvent.modifiers() & ~GLFW.GLFW_MOD_SHIFT) != 0){
                //String[] apping = {"","\\s","\\c","\\S","\\a","\\A","\\g"};
                // this is shift, alt and ctrl
                compound += '0' + (characterEvent.modifiers() & (GLFW.GLFW_MOD_SUPER - 1) & ~GLFW.GLFW_MOD_SHIFT);
            }
            compound += characterEvent.codepointAsString();
            parseVisualCompound();
            return true;
        }
        if(state == VimState.Insert){
            if (characterEvent.isAllowedChatCharacter()) {
                //this.insertText(characterEvent.codepointAsString());
                insertChar(characterEvent.codepointAsString());
                return true;
            }
        }
        if(state == VimState.Replace || state == VimState.ReplaceContinue){
            if (characterEvent.isAllowedChatCharacter()) {
                int preX = cursorX;
                if(cursorX < renderText.get(cursorY).length())
                    deleteText(new Rect(cursorX,cursorY,cursorX + 1,cursorY,cursorX + 1,cursorY));
                insertChar(characterEvent.codepointAsString());
                if(state == VimState.Replace){
                    cursorX = preX;
                    state = VimState.Normal;
                }
                return true;
            }
            if(state == VimState.Replace){
                state = VimState.Normal;
            }
        }
        return false;
    }

    private void parseVisualCompound() {
        int length = compound.length();
        Rect getRect;
        if(length == 0)return;
        char codep = compound.charAt(0);
        boolean doClear = false;
        String s;
        if(compound.equals("\\2c")){
            String rawFetched = rawSelectedString(selection);
            Minecraft.getInstance().keyboardHandler.setClipboard(rawFetched);

            //
            codep = 0;
            doClear = true;
            state = VimState.Normal;
        }
        switch (codep) {
            case ' ':
            case 'l':
            case 'h':
            case 'k':
            case 'j':
            case '$':
            case '0':
            case '^':
            case 'w':
            case 'W':
            case 'e':
            case 'E':
            case 'b':
            case 'B':
            case 'n':
            case 'N':
            case 't':
            case 'T':
            case 'f':
            case 'F':
            case '\\':
            case '%':
            case 'g':
            case 'G':
                getRect = getDelta(compound);
                if (getRect == null)
                    break;
                cursorX = getRect.posX;
                cursorY = getRect.posY;
                doClear = true;
                break;
            case 'o':
                int tmp;
                if(state == VimState.Visual) {
                    tmp = selection.endX;
                    selection.endX = selection.staX;
                    selection.staX = tmp;
                    tmp = selection.endY;
                    selection.endY = selection.staY;
                    selection.staY = tmp;
                    cursorX = selection.endX;
                    cursorY = selection.endY;
                }else{
                    if(selection.endY < selection.posY) {
                        selection.posY++;
                    }
                    tmp = selection.endY;
                    selection.endY = selection.posY;
                    selection.posY = tmp;
                    selection.staY = tmp;
                    cursorY = selection.endY;
                    if(selection.endY > selection.posY)
                        cursorY--;
                    if(selection.endY < selection.posY) {
                        selection.posY--;

                    }
                }
                limitCursorY();
                doClear = true;
                break;
            case 'c':
                doClear = true;
                state = VimState.Insert;
                deleteText(selection);
                cursorX = selection.staX;
                cursorY = selection.staY;
                limitCursorY();
                resetSelection();
                break;
            case 'd':
            case 'y':
                doClear = true;
                state = VimState.Normal;
                parseNormalCompound(selection);
                selection = selection.sort();
                cursorX = selection.staX;
                cursorY = selection.staY;
                limitCursorY();
                resetSelection();
                break;
            case 'v':
            case 'V':
                state = VimState.Normal;
                resetSelection();
                doClear = true;
                break;
            default:
                doClear = true;break;
        }
        if(doClear)
            compound = "";
        selection.endX = cursorX;
        selection.endY = cursorY;
        if(state == VimState.VisualLines){
            if(selection.staY > selection.endY){
                //selection.staX = renderText.get(selection.staY).length();
                selection.endX = 0;
                selection.staX = 0;
                selection.staY = selection.posY + 1;
            }else {
                selection.endX = 0;
                selection.endY++;
                //selection.endX = renderText.get(selection.endY).length();
                selection.staX = 0;
            }
        }
        return;
    }

    public void parseNormalCompound(Rect getRect){
        int length = compound.length();
        if(length == 0)return;
        char codep = compound.charAt(0);
        boolean doClear = false;
        String s = "";
        //Rect getRect;
        if(compound.startsWith("\\")){
            // spacial stuff
            if(compound.equals("\\2v")){
                s = Minecraft.getInstance().keyboardHandler.getClipboard();
                insertText(s,false);
                doClear = true;
            }
            if(compound.equals("\\2V")) {
                s = Minecraft.getInstance().keyboardHandler.getClipboard();
                insertChar(s);
                doClear = true;
            }
            if(doClear) {
                cursorX += s.length();
                limitCursorX();
                codep = 0;
            }
        }
        switch (codep) {
            case ':':
            case '/':
                if (parent instanceof TextScreen tsp) {
                    tsp.focusCmd(Character.toString(codep));
                    doClear = true;
                    break;
                }
                break;
            case 'r':
                state = VimState.Replace;
                doClear = true;
                break;
            case 'R':
                state = VimState.ReplaceContinue;
                doClear = true;
                break;
            case 'i':
                state = VimState.Insert;
                doClear = true;
                break;
            case 'I':
                cursorX = 0;
                state = VimState.Insert;
                doClear = true;
                break;
            case 'a':
                cursorX++;
                limitCursorX();
                state = VimState.Insert;
                doClear = true;
                break;
            case 'A':
                cursorX = renderText.get(cursorY).length();
                state = VimState.Insert;
                doClear = true;
                break;
            case 'v':
                state = VimState.Visual;
                resetSelection();
                doClear = true;
                break;
            case 'V':
                state = VimState.VisualLines;
                selection = new Rect(0, cursorY, renderText.get(cursorY).length(), cursorY, cursorX, cursorY);
                doClear = true;
                break;
            // movement
            case ' ':
            case 'l': case 'h':
            case 'k': case 'j':
            case '$': case '0': case '^':
            case '%':
            case 'w': case 'W':
            case 'e': case 'E':
            case 'b': case 'B':
            case 'n': case 'N':
            case 't': case 'T':
            case 'f': case 'F':
            case '\\':
            case 'g': case 'G':
                getRect = getDelta(compound);
                if (getRect == null)
                    break;
                cursorX = getRect.posX;
                cursorY = getRect.posY;
                limitCursorY();
                doClear = true;
                break;
            case 'o':
                cursorY++;
                cursorX = 0;
                renderText.add(cursorY, "");
                state = VimState.Insert;
                doClear = true;
                break;
            case 'O':
                cursorX = 0;
                renderText.add(cursorY, "");
                state = VimState.Insert;
                doClear = true;
                break;
            case 'z':
                if (length < 2) break;
                if (compound.charAt(1) == 'z') {
                    scroll = cursorY - linePer / 2;
                }
                doClear = true;
                break;
            case 'x':
                s = renderText.get(cursorY);
                if (cursorX >= s.length()) {
                    limitCursorX();
                    if (cursorX != 0) {
                        renderText.set(cursorY, s.substring(0, cursorX - 1) + s.substring(cursorX));
                    }
                } else
                    renderText.set(cursorY, s.substring(0, cursorX) + s.substring(cursorX + 1));
                //cursorX--;
                limitCursorX();
                doClear = true;
                break;
            case 'X':
                s = renderText.get(cursorY);
                if (cursorX <= 0) {
                    limitCursorX();
                    if (cursorX != s.length()) {
                        renderText.set(cursorY, s.substring(0, cursorX) + s.substring(cursorX + 1));
                    }
                } else
                    renderText.set(cursorY, s.substring(0, cursorX - 1) + s.substring(cursorX));
                cursorX--;
                limitCursorX();
                doClear = true;
                break;
            case 'J':
                if (cursorY - 1 < renderText.size())
                {
                    s = renderText.remove(cursorY + 1);
                    renderText.set(cursorY, renderText.get(cursorY) + " " + s);
                }
                doClear = true;
                break;
            case 'd':
                if(getRect == null)getRect = getDelta(compound.substring(1));
                if(getRect == null)
                    break;
                deleteText(getRect);
                limitCursorY();
                doClear = true;
                break;
            case 'c':
                if(getRect == null)getRect = getDelta(compound.substring(1));
                if(getRect == null)
                    break;
                deleteText(getRect);
                doClear = true;
                cursorX = 0;
                renderText.add(cursorY, "");
                limitCursorY();
                state = VimState.Insert;
                break;
            case 'y':
                if(getRect == null)getRect = getDelta(compound.substring(1));
                if(getRect == null)break;
                copied = rawSelectedString(getRect);
                doClear = true;
                break;
            case 'P':
                insertText(copied,true);
                //cursorY++;
                doClear = true;
                break;
            case 'p':
                insertText(copied,false);
                doClear = true;
                break;
            //
            default:
                doClear = true;break;
        }
        if(doClear)
            compound = "";
        return;
    }
    public String rawSelectedString(Rect rect){
        rect = rect.sort();
        int minY = rect.staY;
        int maxY = rect.endY;
        int minX = rect.staX;
        int maxX = rect.endX;
        int sliceMin,sliceMax;
        StringBuilder stOut = new StringBuilder();
        String iter;
        for(int idx = minY;idx <= maxY;idx++){
            iter = renderText.get(idx);
            sliceMin = minX;
            sliceMax = maxX;
            if(idx > minY){
                sliceMin = 0;
                stOut.append("\n");
            }
            if(idx < maxY)sliceMax = iter.length();
            stOut.append(iter, sliceMin, sliceMax);
        }
        return stOut.toString();
    }
    public void deleteText(Rect rect){
        rect = rect.sort();
        int minY = rect.staY;
        int maxY = rect.endY;
        int minX = rect.staX;
        int maxX = rect.endX;
        /*
        if(minY > maxY){
            maxY = rect.staY;
            minY = rect.endY;
            highX = rect.staX;
            lowX = rect.endX;
        }*/
        if(maxY >= renderText.size()){
            maxY = renderText.size() - 1;
            maxX = renderText.get(maxY).length();
            minX--;
            if(minX < 0){
                minY--;
                if(minY < 0)minY = 0;
                minX = renderText.get(minY).length();
            }
        }

        String s = renderText.get(minY);
        String into = s.substring(0,minX);
        /*if(maxY == minY){
            into += s.substring(rect.endX);
            renderText.set(minY,into);
            return;
        }//  */
        for(int iterY = minY;iterY < maxY;iterY++) {
            s = renderText.remove(minY + 1);
        }
        into += s.substring(maxX);
        renderText.set(minY,into);
        return;

    }
    public void insertText(String text){
        insertText(text,false);
    }
    public void insertText(String text,boolean reversePaste){
        if(renderText.isEmpty())
            renderText.add("");
        int ypos = Math.max(0,Math.min(cursorY,renderText.size() - 1));
        String base;
        String bout;
        int xpos;
        if(text.endsWith("\n")){
            bout = text.substring(0,text.length() - 1);
            if(!reversePaste) {
                cursorY++;
                ypos++;
            }
        }else {
            base = renderText.remove(ypos);
            xpos = Math.max(0, Math.min(cursorX, base.length()));
            if(!reversePaste){
                xpos++;
                xpos = Math.min(cursorX,base.length());
            }
            bout = base.substring(0,xpos) + text + base.substring(xpos);
        }
        //
        int idxY = ypos;
        int idx0X = 0;
        int idx1X;
        while(idx0X != -1){
            idx1X = bout.indexOf('\n',idx0X);
            if(idx1X == -1){
                renderText.add(idxY,bout.substring(idx0X));
                break;
            }
            renderText.add(idxY,bout.substring(idx0X,idx1X));
            idxY++;
            idx0X = idx1X + 1;
        }
        //cursorX += text.length();
    }
    public void insertChar(String text) {
        if(renderText.isEmpty())
            renderText.add("");
        int ypos = Math.max(0,Math.min(cursorY,renderText.size() - 1));
        int xpos;
        String base;
        base = renderText.get(ypos);
        xpos = Math.max(0, Math.min(cursorX, base.length()));
        base = base.substring(0,xpos) + text + base.substring(xpos);
        cursorX += text.length();
        renderText.set(ypos,base);
    }
    public boolean canConsumeInput() {
        return this.isActive() && this.isFocused();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }
    public void passIntoCommand(String cmd){
        if(cmd.length() < 2){return;}
        char op = cmd.charAt(0);
        if(op == '/'){
            findNext = cmd.substring(1);

            Rect getRect = getDelta("n");
            if (getRect == null)
                return;
            cursorX = getRect.posX;
            cursorY = getRect.posY;
            return;
        }
        if(op == ':'){

        }
    }
    public void appendText(String text){
        renderText.add(text);
        cursorY = renderText.size() - 1;
        cursorX = renderText.get(cursorY).length();
    }
    public void appendTexts(List<String> texts){
        renderText.addAll(texts);
        cursorY = renderText.size() - 1;
        if(cursorY >= 0)
            cursorX = renderText.get(cursorY).length();
        else{cursorX = 0;}
    }
}
