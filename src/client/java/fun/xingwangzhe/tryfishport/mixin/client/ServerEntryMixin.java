package fun.xingwangzhe.tryfishport.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

import fun.xingwangzhe.tryfishport.client.PingUI;

@Mixin(MultiplayerServerListWidget.ServerEntry.class)
public class ServerEntryMixin {
    @Unique
    private static final Logger LOGGER = LogManager.getLogger("TryFishport");
    
    // 存储每个服务器条目的按钮
    @Unique
    private static final Map<MultiplayerServerListWidget.ServerEntry, ButtonWidget> entryButtons = new HashMap<>();

    @Inject(at = @At("TAIL"), method = "render(Lnet/minecraft/client/gui/DrawContext;IIIIIIIZF)V")
    private void onRender(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo info) {
        MultiplayerServerListWidget.ServerEntry entry = (MultiplayerServerListWidget.ServerEntry) (Object) this;

        // 只有在hovered为true时才显示按钮
        if (hovered) {
            // 获取或创建按钮
            ButtonWidget button = entryButtons.get(entry);
            if (button == null) {
                button = ButtonWidget.builder(Text.of("Ping"), btn -> {
                    System.out.println("Ping button clicked!"); // 添加日志
                    // 获取当前屏幕并确保它是MultiplayerScreen
                    Screen currentScreen = MinecraftClient.getInstance().currentScreen;
                    if (currentScreen instanceof MultiplayerScreen) {
                        MinecraftClient.getInstance().setScreen(new PingUI((MultiplayerScreen) currentScreen, entry.getServer()));
                    } else {
                        // 如果由于某种原因当前屏幕不是MultiplayerScreen，则创建一个新的
                        MinecraftClient.getInstance().setScreen(new PingUI(new MultiplayerScreen(null), entry.getServer()));
                    }
                }).dimensions(0, 0, 60, 20).build();
                entryButtons.put(entry, button);
            }

            // 设置按钮位置（右下角）
            int buttonX = x + entryWidth - 62;  // 右边距2px + 按钮宽度60px
            int buttonY = y + entryHeight - 22; // 底边距2px + 按钮高度20px
            button.setX(buttonX);
            button.setY(buttonY);

            // 渲染按钮
            button.render(context, mouseX, mouseY, tickDelta);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        MultiplayerServerListWidget.ServerEntry entry = (MultiplayerServerListWidget.ServerEntry) (Object) this;
        
        // 获取该条目对应的按钮
        ButtonWidget btn = entryButtons.get(entry);
        if (btn != null && btn.mouseClicked(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }
    
    @Inject(method = "close", at = @At("HEAD"))
    private void onClose(CallbackInfo ci) {
        MultiplayerServerListWidget.ServerEntry entry = (MultiplayerServerListWidget.ServerEntry) (Object) this;
        entryButtons.remove(entry);
    }
    
    // 替换 printStackTrace 为更可靠的日志记录
    @Unique
    private void logException(Exception e) {
        LOGGER.error("Exception occurred while rendering TryFishport button", e);
    }
}