package ru.strange.client.module.impl.utilities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;
import ru.strange.client.Strange;
import ru.strange.client.event.EventInit;
import ru.strange.client.event.impl.EventMouseInput;
import ru.strange.client.module.api.Category;
import ru.strange.client.module.api.IModule;
import ru.strange.client.module.api.Module;
import ru.strange.client.module.api.setting.impl.BindSettings;
import ru.strange.client.utils.math.TimerUtil;
import ru.strange.client.utils.other.SoundUtil;

@IModule(
        name = "Добавить друга",
        category = Category.Utilities,
        description = "Добавляем своего друга по кнопке",
        bind = -1
)
public class MiddleClick extends Module {
    public static BindSettings friendkey = new BindSettings("Кнопка", GLFW.GLFW_MOUSE_BUTTON_MIDDLE);

    public MiddleClick() {
        addSettings(friendkey);
    }
    private final TimerUtil swapWatchK = new TimerUtil();

    @EventInit
    public void onMouseClick(EventMouseInput e) {
        if (e.button() == friendkey.get() && swapWatchK.hasTimeElapsed(200)) {

            HitResult hitResult = mc.crosshairTarget;
            if (hitResult == null || hitResult.getType() != HitResult.Type.ENTITY) return;

            Entity entity = ((EntityHitResult) hitResult).getEntity();
            if (!(entity instanceof PlayerEntity player)) return;

            String name = player.getName().getString();

            if (!Strange.get.friendManager.isFriend(name)) {

                Text msg = Text.literal("Друг - ")
                        .formatted(Formatting.WHITE)
                        .append(Text.literal(name).formatted(Formatting.GREEN))
                        .append(Text.literal(" добавлен :)").formatted(Formatting.GRAY));

                mc.player.sendMessage(msg, false);

                Strange.get.friendManager.add(name);
                SoundUtil.playSound_wav("add", 0.5f);
            } else {

                Text msg = Text.literal("Друг - ")
                        .formatted(Formatting.WHITE)
                        .append(Text.literal(name).formatted(Formatting.RED))
                        .append(Text.literal(" удален :(").formatted(Formatting.GRAY));

                mc.player.sendMessage(msg, false);

                Strange.get.friendManager.remove(name);
                SoundUtil.playSound_wav("remove", 0.5f);
            }
            swapWatchK.reset();
        }
    }
}
