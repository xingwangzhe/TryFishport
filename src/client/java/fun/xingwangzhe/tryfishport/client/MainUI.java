package fun.xingwangzhe.tryfishport.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;

public class MainUI extends Screen {

    public MainUI() {
        super(Text.of("我的世界首页"));
    }

    @Override
    protected void init() {
        // 添加按钮 使用新的Builder模式
        this.addDrawableChild(ButtonWidget.builder(Text.of("点击我"), button -> {
                    // 按钮点击事件
                    System.out.println("按钮被点击了！");
                })
                .dimensions(this.width / 2 - 50, this.height / 2 - 10, 100, 20) // 按钮位置和尺寸
                .build());
                
        // 添加返回按钮
        this.addDrawableChild(ButtonWidget.builder(Text.of("返回"), button -> {
                    // 关闭当前界面，返回到上一个界面（主菜单）
                    this.close();
                })
                .dimensions(this.width / 2 - 50, this.height / 2 + 20, 100, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680); // 渲染背景
        // 绘制标题文本
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public boolean shouldPause() {
        return false; // 不暂停游戏
    }
}