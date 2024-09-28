package net.mehvahdjukaar.amendments.client.gui;

import net.mehvahdjukaar.amendments.common.LecternEditMenu;
import net.mehvahdjukaar.amendments.common.network.ModNetwork;
import net.mehvahdjukaar.amendments.common.network.ServerBoundSyncLecternBookMessage;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;

import java.util.Objects;
import java.util.Optional;


public class LecternBookEditScreen extends BookEditScreen implements MenuAccess<LecternEditMenu> {
    private final LecternEditMenu menu;

    private final ContainerListener listener = new ContainerListener() {
        @Override
        public void slotChanged(AbstractContainerMenu containerToSend, int dataSlotIndex, ItemStack stack) {
            book = stack;
            pages.clear();
            WritableBookContent writableBookContent = book.get(DataComponents.WRITABLE_BOOK_CONTENT);
            if (writableBookContent != null) {
                writableBookContent.getPages(Minecraft.getInstance().isTextFilteringEnabled()).forEach(pages::add);
                clearDisplayCache();
            }
        }

        @Override
        public void dataChanged(AbstractContainerMenu containerMenu, int dataSlotIndex, int value) {
            if (dataSlotIndex == 0) {
                setPage(value);
            }
        }
    };


    private int lastPage;
    private Button takeBookButton;
    private QuillButton quill;
    private InkButton ink;
    private StyledTextFieldHelper page;

    public LecternBookEditScreen(LecternEditMenu lecternMenu, Inventory inventory, Component component) {
        super(inventory.player, lecternMenu.getBook(), InteractionHand.MAIN_HAND);
        this.menu = lecternMenu;
        this.lastPage = this.menu.getPage();
    }

    @Override
    public void saveChanges(boolean publish) {
        this.saveChanges(publish, false);
    }

    public void saveChanges(boolean publish, boolean takeBook) {
        if (this.isModified) {
            this.eraseEmptyTrailingPages();
            this.updateLocalCopy();

            NetworkHelper.sendToServer(new ServerBoundSyncLecternBookMessage(menu.getPos(),
                    this.pages, publish ? Optional.of(this.title.trim()) : Optional.empty(),
                    takeBook));
        }
    }

    //lectern menu stuff

    @Override
    public LecternEditMenu getMenu() {
        return this.menu;
    }

    @Override
    protected void init() {
        this.page = new StyledTextFieldHelper(this::getCurrentPageText,
                this::setCurrentPageText, this::getClipboard, this::setClipboard,
                (string) -> string.length() < 1024 && this.font.wordWrapHeight(string, 114) <= 128);
        this.pageEdit = page;
        this.menu.addSlotListener(this.listener);
        this.quill = this.addRenderableWidget(new QuillButton(this));
        this.ink = this.addRenderableWidget(new InkButton(this));

        int width = 76;
        this.takeBookButton = this.addRenderableWidget(Button.builder(Component.translatable("lectern.take_book"), (button) -> {
            this.saveChanges(false, true);
            if (!this.isModified) this.sendButtonClick(3);
        }).bounds(this.width / 2 - width / 2 - 6, 196, width, 20).build());

        super.init();

        //we need to replace all set screens with onClose to close the container
        int separation = 80;
        this.signButton.setX((this.width - width) / 2 - separation - 6);
        this.signButton.setWidth(width);

        this.removeWidget(this.doneButton);
        this.doneButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (button) -> {
            this.saveChanges(false);
            this.onClose();
        }).bounds((this.width - width) / 2 + separation - 6,
                196, width, 20).build());

        this.removeWidget(finalizeButton);
        this.finalizeButton = this.addRenderableWidget(Button.builder(Component.translatable("book.finalizeButton"), (button) -> {
            if (this.isSigning) {
                this.saveChanges(true);
                this.onClose();
            }

        }).bounds(this.width / 2 - 100, 196, 98, 20).build());


        this.updateButtonVisibility();
    }

    @Override
    protected void updateButtonVisibility() {
        super.updateButtonVisibility();
        this.takeBookButton.visible = !this.isSigning;
        this.ink.visible = !this.isSigning;
        this.quill.visible = !this.isSigning;
    }

    @Override
    public void onClose() {
        this.minecraft.player.closeContainer();
        super.onClose();
    }

    //Sends stuff to server which then notifies clients. Multiple players might be viewing this
    @Override
    public void pageBack() {
        this.sendButtonClick(1);
    }

    @Override
    public void pageForward() {
        this.sendButtonClick(2);
    }

    //hack
    private void setPage(int value) {
        while (lastPage != value) {
            if (value > lastPage) {
                lastPage++;
                super.pageForward();
                if (lastPage > this.pages.size()) {
                    for(int i = 0; i < value - this.pages.size(); i++){
                        this.pages.add("");
                    }
                    this.isModified = true;
                }
            } else {
                lastPage--;
                super.pageBack();
            }
        }
    }

    private void sendButtonClick(int pageData) {
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, pageData);
    }

    @Override
    public boolean titleKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 335) {
            if (!this.title.isEmpty()) {
                this.saveChanges(true);
                this.onClose();
            }

            return true;
        }
        return super.titleKeyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        super.removed();
        this.menu.removeSlotListener(this.listener);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!isSigning && StringUtil.isAllowedChatCharacter(codePoint)) {
            this.page.insertStyledText(Character.toString(codePoint), ink.getChatFormatting(), quill.getChatFormatting());
            this.clearDisplayCache();
            return true;
        } else {
            return super.charTyped(codePoint, modifiers);
        }
    }


    public void onInkClicked() {
        if (this.page.isSelecting()) {
            this.page.formatSelected(ink.getChatFormatting(), null);
        }
    }

    public void onQuillClicked() {
        if (this.page.isSelecting()) {
            this.page.formatSelected(null, quill.getChatFormatting());
        }
    }
}
