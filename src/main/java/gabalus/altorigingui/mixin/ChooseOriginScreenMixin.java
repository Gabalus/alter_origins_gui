package gabalus.altorigingui.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import gabalus.altorigingui.Altorigingui;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.screen.ChooseOriginScreen;
import io.github.apace100.origins.screen.OriginDisplayScreen;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.List;

@Mixin(ChooseOriginScreen.class)
public abstract class ChooseOriginScreenMixin extends OriginDisplayScreen {

    @Shadow(remap = false) @Final private List<Holder<Origin>> originSelection;
    @Shadow(remap = false) private int currentOrigin;

    @Shadow(remap = false) @Final private List<Holder<OriginLayer>> layerList;
    @Shadow(remap = false) protected abstract Holder<Origin> getCurrentOriginInternal();
    @Final
    @Shadow(remap = false) private int currentLayerIndex;
    @Shadow(remap = false) private Origin randomOrigin;
    @Shadow(remap = false) private int maxSelection;

    private static final ResourceLocation ORIGINS_CHOICES = new ResourceLocation(Altorigingui.MODID, "textures/gui/origin_choices.png");
    private static final int CHOICES_WIDTH = 219;
    private static final int CHOICES_HEIGHT = 182;

    private static final int ORIGIN_ICON_SIZE = 26;

    private int calculatedTop;
    private int calculatedLeft;

    private int currentPage = 0;
    private static final int COUNT_PER_PAGE = 35;
    private int pages;
    private float tickTime = 0.0F;

    public ChooseOriginScreenMixin(Component title, boolean showDirtBackground) {
        super(title, showDirtBackground);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.calculatedTop = (this.height - CHOICES_HEIGHT) / 2;
        this.calculatedLeft = (this.width - (CHOICES_WIDTH + 10 + windowWidth)) / 2;

        this.guiTop = (this.height - windowHeight) / 2;
        this.guiLeft = calculatedLeft + CHOICES_WIDTH + 10;
        this.pages = (int)Math.ceil((float) maxSelection / COUNT_PER_PAGE);



        // Remove original "<" and ">" buttons
        Iterator<Renderable> iterator = this.renderables.iterator();
        while (iterator.hasNext()) {
            Renderable widget = iterator.next();
            if (widget instanceof Button) {
                Button button = (Button) widget;
                if (button.getMessage().getString().equals("<") || button.getMessage().getString().equals(">")) {
                    iterator.remove();
                }
            }
        }
        var iteratorFunc = this.children().iterator();
        while (iteratorFunc.hasNext()) {
            var widget = iteratorFunc.next();
            if (widget instanceof Button) {
                Button button = (Button) widget;
                if (button.getMessage().getString().equals("<") || button.getMessage().getString().equals(">")) {
                    iteratorFunc.remove();
                }
            }
        }
        // Re-add the buttons with the correct position
        initButtons();
    }

    @Inject(method = "render", at = @At("TAIL"))
    void addRendering(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        renderOriginChoicesBox(context, mouseX, mouseY, delta);
        tickTime += delta;
    }

    @Unique
    public void renderOriginChoicesBox(GuiGraphics context, int mouseX, int mouseY, float delta) {
        context.blit(ORIGINS_CHOICES, calculatedLeft, calculatedTop, 0, 0, CHOICES_WIDTH, CHOICES_HEIGHT);
        int x = 0;
        int y = 0;
        for (int i = (currentPage * COUNT_PER_PAGE); i < Math.min((currentPage + 1) * COUNT_PER_PAGE, maxSelection); i++) {
            if (x > 6) {
                x = 0;
                y++;
            }
            int actualX = (12 + (x * (ORIGIN_ICON_SIZE + 2))) + calculatedLeft;
            int actualY = (10 + (y * (ORIGIN_ICON_SIZE + 4))) + calculatedTop;
            if (i >= originSelection.size()) {
                // This is the random origin
                boolean selected = this.getCurrentOrigin().value().equals(Origins.identifier("random"));
                renderRandomOrigin(context, mouseX, mouseY, delta, actualX, actualY, selected);
            } else {
                Holder<Origin> origin = originSelection.get(i);
                boolean selected = origin.value().equals(this.getCurrentOrigin().value());
                renderOriginWidget(context, mouseX, mouseY, delta, actualX, actualY, selected, origin);
                context.renderItem(origin.get().getIcon(), actualX + 5, actualY + 5);
            }

            x++;
        }
        context.drawCenteredString(this.font, Component.literal((currentPage + 1) + "/" + (pages)), calculatedLeft + (CHOICES_WIDTH / 2), guiTop + windowHeight + 5 + this.font.lineHeight/2, 0xFFFFFF);
    }

    private void initButtons() {
        int x = 0;
        int y = 0;
        for (int i = 0; i < Math.min(maxSelection, 35); i++) {
            if (x > 6) {
                x = 0;
                y++;
            }
            int actualX = (12 + (x * (ORIGIN_ICON_SIZE + 2))) + calculatedLeft;
            int actualY = (10 + (y * (ORIGIN_ICON_SIZE + 4))) + calculatedTop;
            int finalI = i;
            addRenderableWidget(Button.builder(Component.literal(""), b -> {
                int index = finalI + (currentPage * COUNT_PER_PAGE);
                if (index > maxSelection - 1) {
                    return;
                }
                currentOrigin = index;
                Holder<Origin> newOrigin = getCurrentOriginInternal();
                showOrigin(newOrigin, layerList.get(currentLayerIndex), newOrigin == randomOrigin);
            }).bounds(actualX, actualY, 26, 26).build());
            x++;
        }

        if(maxSelection > COUNT_PER_PAGE) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                currentPage = (currentPage - 1);
                if(currentPage < 0) {
                    currentPage = pages - 1;
                }
            }).bounds(calculatedLeft, guiTop + windowHeight + 5, 20, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                currentPage = (currentPage + 1) % (pages);
            }).bounds(this.guiLeft +176, guiTop + windowHeight + 5, 20, 20).build());
        }
    }

    public void renderOriginWidget(GuiGraphics context, int mouseX, int mouseY, float delta, int x, int y, boolean selected, Holder<Origin> origin) {
        RenderSystem.setShaderTexture(0, ORIGINS_CHOICES);
        int u = selected ? 26 : 0;
        boolean mouseHovering = mouseX >= x && mouseY >= y && mouseX < x + 26 && mouseY < y + 26;
        boolean guiSelected = (getFocused() instanceof Button buttonWidget && buttonWidget.getX() == x && buttonWidget.getY() == y) || mouseHovering;
        if (guiSelected) {
            u += 52;
        }
        context.blit(ORIGINS_CHOICES, x, y, 230, u, 26, 26);
        var impact = origin.value().getImpact();
        context.blit(ORIGINS_CHOICES, x, y, 224 + (impact.ordinal() * 8), guiSelected ? 112 : 104, 8, 8);
        if (mouseHovering) {
            //Component text = Component.translatable(getCurrentLayer().value().title()).append(": ").append(origin.value().getName());
            context.renderTooltip(this.font, origin.value().getName(), mouseX, mouseY);
        }
    }

    public void renderRandomOrigin(GuiGraphics context, int mouseX, int mouseY, float delta, int x, int y, boolean selected) {
        int u = selected ? 26 : 0;
        boolean mouseHovering = mouseX >= x && mouseY >= y && mouseX < x + 26 && mouseY < y + 26;
        boolean guiSelected = (getFocused() instanceof Button buttonWidget && buttonWidget.getX() == x && buttonWidget.getY() == y) || mouseHovering;
        if (guiSelected) {
            u += 52;
        }
        context.blit(ORIGINS_CHOICES, x, y, 230, u, 26, 26);
        context.blit(ORIGINS_CHOICES, x + 6, y + 5, 243, 120, 13, 16);
        int impact = (int) (tickTime / 15.0) % 4;
        context.blit(ORIGINS_CHOICES, x, y, 224 + (impact * 8), guiSelected ? 112 : 104, 8, 8);
    }
}
