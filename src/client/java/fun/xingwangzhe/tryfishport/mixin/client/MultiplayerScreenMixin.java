package fun.xingwangzhe.tryfishport.mixin.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public class MultiplayerScreenMixin extends Screen {
    protected MultiplayerScreenMixin(Text title) {
        super(title);
    }

    @Inject(at = @At("TAIL"), method = "init()V")
    private void onInit(CallbackInfo ci) {
        // 在服务器列表界面右上角附近添加自定义按钮
        this.addDrawableChild(ButtonWidget
            .builder(Text.literal("TryFishport"), button -> {
                System.out.println("TryFishport button clicked!");
            })
            .dimensions(this.width - 105, 5, 100, 20)
            .build());
    }
}