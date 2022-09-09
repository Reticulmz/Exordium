package dev.tr7zw.fastergui.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.tr7zw.fastergui.FasterGuiModBase;
import dev.tr7zw.fastergui.access.ChatAccess;
import dev.tr7zw.fastergui.util.BufferRenderer;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

@Mixin(value= Gui.class, priority = 1500) // higher priority, so it also captures rendering happening at RETURN
public class GuiMixin {
    
    private BufferRenderer bufferRenderer = new BufferRenderer();
    
    @Shadow
    private Minecraft minecraft;
    @Shadow
    private ChatComponent chat;
    @Shadow
    private Component title;
    @Shadow
    private int titleTime;
    @Shadow
    private int titleFadeInTime;
    @Shadow
    private int titleStayTime;
    @Shadow
    private int titleFadeOutTime;
    @Shadow
    private int tickCount;
    @Shadow
    private Component overlayMessageString;
    @Shadow
    private int overlayMessageTime;
    @Shadow
    private int toolHighlightTimer;
    
    @Inject(method = "render", at = @At(value="INVOKE", target = "Lnet/minecraft/client/Minecraft;getDeltaFrameTime()F"), cancellable = true)
    public void render(PoseStack arg, float g, CallbackInfo ci) {
        if(!FasterGuiModBase.instance.config.enabledGui) {
            return;
        }
        boolean cancel = bufferRenderer.render();
        if(cancel)
            ci.cancel();
    }
    
    @Inject(method = "render", at = @At("RETURN"))
    public void renderEnd(PoseStack arg, float f, CallbackInfo ci) {
        if(!FasterGuiModBase.instance.config.enabledGui) {
            return;
        }
        int targetFps = FasterGuiModBase.instance.config.targetFPSIngameGui;
        if(FasterGuiModBase.instance.config.enabledGuiAnimationSpeedup) {
            // Item name tooltip
            if(toolHighlightTimer > 0 && toolHighlightTimer < 15) {
                targetFps = FasterGuiModBase.instance.config.targetFPSIngameGuiAnimated;
            }
            // title/subtitle
            if (this.title != null && this.titleTime > 0) {
                int m = 255;
                float j = this.titleTime - f;
                if (this.titleTime > this.titleFadeOutTime + this.titleStayTime) {
                    float p = (this.titleFadeInTime + this.titleStayTime + this.titleFadeOutTime) - j;
                    m = (int) (p * 255.0F / this.titleFadeInTime);
                }
                if (this.titleTime <= this.titleFadeOutTime)
                    m = (int) (j * 255.0F / this.titleFadeOutTime);
                m = Mth.clamp(m, 0, 255);
                if (m != 255) {
                    targetFps = FasterGuiModBase.instance.config.targetFPSIngameGuiAnimated;
                }
            }
            // Attack indicator
            if (this.minecraft.options.attackIndicator == AttackIndicatorStatus.CROSSHAIR) {
                float j = this.minecraft.player.getAttackStrengthScale(0.0F);
                if(j < 1.0F) {
                    targetFps = FasterGuiModBase.instance.config.targetFPSIngameGuiAnimated;
                }
            }
            // Chat
            ChatAccess chatAccess = (ChatAccess) chat;
            if(chatAccess.hasActiveAnimations(tickCount)) {
                targetFps = FasterGuiModBase.instance.config.targetFPSIngameGuiAnimated;
            }
            // Overlaymessage "Actionbar"
            if (this.overlayMessageString != null && this.overlayMessageTime > 0) {
                this.minecraft.getProfiler().push("overlayMessage");
                float timerj = this.overlayMessageTime - f;
                int m = (int) (timerj * 255.0F / 20.0F);
                if (m > 255)
                    m = 255;
                if (m > 8 && m != 255) {
                    targetFps = FasterGuiModBase.instance.config.targetFPSIngameGuiAnimated;
                }
                this.minecraft.getProfiler().pop();
            }
        }
        bufferRenderer.renderEnd(1000/targetFps);
    }
   
    // Fix for Bossbar
    
    @Inject(method = "render", at = @At(value="INVOKE", target = "Lnet/minecraft/client/gui/components/BossHealthOverlay;render(Lcom/mojang/blaze3d/vertex/PoseStack;)V", shift = Shift.BEFORE))
    public void renderBossbar(PoseStack arg, float g, CallbackInfo ci) {
        FasterGuiModBase.correctBlendMode();
        FasterGuiModBase.setForceBlend(true);
    }
    
    @Inject(method = "render", at = @At(value="INVOKE", target = "Lnet/minecraft/client/gui/components/BossHealthOverlay;render(Lcom/mojang/blaze3d/vertex/PoseStack;)V", shift = Shift.AFTER))
    public void renderBossbarReturn(PoseStack arg, float g, CallbackInfo ci) {
        FasterGuiModBase.setForceBlend(false);
        RenderSystem.defaultBlendFunc();
    }
    
    
    // Fix for AppleSkin
    
    @Inject(method = "renderPlayerHealth", at = @At("HEAD"))
    private void renderPlayerHealth(PoseStack poseStack, CallbackInfo ci) {
        FasterGuiModBase.correctBlendMode();
        FasterGuiModBase.setForceBlend(true);
    }
    
    @Inject(method = "renderPlayerHealth", at = @At("RETURN"))
    private void renderPlayerHealthReturn(PoseStack poseStack, CallbackInfo ci) {
        FasterGuiModBase.setForceBlend(false);
    }
    
    // Fix for chat breaking the armor bar outline
    
    @Inject(method = "render", at = @At(value="INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;render(Lcom/mojang/blaze3d/vertex/PoseStack;I)V", shift = Shift.BEFORE))
    public void renderChat(PoseStack arg, float g, CallbackInfo ci) {
        FasterGuiModBase.correctBlendMode();
        FasterGuiModBase.setForceBlend(true);
    }
    
    @Inject(method = "render", at = @At(value="INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;render(Lcom/mojang/blaze3d/vertex/PoseStack;I)V", shift = Shift.AFTER))
    public void renderChatEnd(PoseStack arg, float g, CallbackInfo ci) {
        FasterGuiModBase.setForceBlend(false);
        RenderSystem.defaultBlendFunc();
    }
    
    // Fix for tablist
    
    @Inject(method = "render", at = @At(value="INVOKE", target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;render(Lcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V", shift = Shift.BEFORE))
    public void renderTab(PoseStack arg, float g, CallbackInfo ci) {
        FasterGuiModBase.correctBlendMode();
        FasterGuiModBase.setForceBlend(true);
    }
    
    @Inject(method = "render", at = @At(value="INVOKE", target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;render(Lcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V", shift = Shift.AFTER))
    public void renderTabEnd(PoseStack arg, float g, CallbackInfo ci) {
        FasterGuiModBase.setForceBlend(false);
        RenderSystem.defaultBlendFunc();
    }
    
    // Fix Scoreboard overlapping with overlays like spyglass
    
    @Inject(method = "render", at = @At(value="INVOKE", target = "Lnet/minecraft/client/gui/Gui;displayScoreboardSidebar(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/scores/Objective;)V", shift = Shift.BEFORE))
    private void displayScoreboardSidebarBefore(PoseStack arg, float g, CallbackInfo ci) {
        FasterGuiModBase.correctBlendMode();
        FasterGuiModBase.setForceBlend(true);
    }
    
    @Inject(method = "render", at = @At(value="INVOKE", target = "Lnet/minecraft/client/gui/Gui;displayScoreboardSidebar(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/scores/Objective;)V", shift = Shift.AFTER))
    private void displayScoreboardSidebarAfter(PoseStack arg, float g, CallbackInfo ci) {
        FasterGuiModBase.setForceBlend(false);
        RenderSystem.defaultBlendFunc();
    }

}
