package com.lauma.modmenu;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;

public class ButtonEntry extends AbstractConfigListEntry<Void> {

    private final ButtonWidget button;

    public ButtonEntry(Text label, Runnable action) {
        super(label, false);
        this.button = ButtonWidget.builder(label, b -> action.run())
            .dimensions(0, 0, 150, 20)
            .build();
    }

    @Override
    public Void getValue() { return null; }

    @Override
    public Optional<Void> getDefaultValue() { return Optional.empty(); }

    @Override
    public void save() {}

    @Override
    public boolean isRequiresRestart() { return false; }

    @Override
    public List<? extends Element> children() { return List.of(button); }

    @Override
    public List<? extends Selectable> selectableChildren() { return List.of(button); }

    @Override
    public void render(DrawContext context, int index, int y, int x, int entryWidth,
                       int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
        button.setX(x + entryWidth / 2 - 75);
        button.setY(y);
        button.render(context, mouseX, mouseY, tickDelta);
    }
}
