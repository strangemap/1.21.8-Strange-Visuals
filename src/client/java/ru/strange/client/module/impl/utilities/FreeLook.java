package ru.strange.client.module.impl.utilities;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.Vector2f;
import org.lwjgl.glfw.GLFW;
import ru.strange.client.event.EventInit;
import ru.strange.client.event.impl.EventUpdate;
import ru.strange.client.module.api.Category;
import ru.strange.client.module.api.IModule;
import ru.strange.client.module.api.Module;
import ru.strange.client.module.api.setting.impl.BindSettings;
import ru.strange.client.module.api.setting.impl.BooleanSetting;
import ru.strange.client.utils.other.FreeLookHandler;
import ru.strange.client.utils.other.Rotation;
import ru.strange.client.utils.other.RotationHandler;

@IModule(
        name = "Фри Лук",
        category = Category.Utilities,
        description = "",
        bind = -1
)
public class FreeLook extends Module {

    private final BindSettings key = new BindSettings("Кнопка", GLFW.GLFW_KEY_LEFT_ALT);
    private final BooleanSetting autoThirdPerson = new BooleanSetting("Авто F5", true);

    private Vector2f rotation = new Vector2f(0f, 0f);
    private Perspective prevPerspective = Perspective.FIRST_PERSON;

    private boolean holding = false;

    public FreeLook() {
        addSettings(key, autoThirdPerson);
    }

    @EventInit
    public void onUpdate(EventUpdate event) {
        if (!enable || mc.currentScreen != null || key.get() == -1) return;

        if (isKeyDown(key.get())) {
            if (!holding) {
                holding = true;
                onPress();
            }
            onHold();
        } else {
            if (holding) {
                holding = false;
                onRelease();
            }
        }
    }

    private void onPress() {
        if (mc.player == null) return;

        if (autoThirdPerson.get()) {
            prevPerspective = mc.options.getPerspective();
            mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        }

        setRotation();
        FreeLookHandler.setActive(true);
    }

    private void onHold() {
        RotationHandler.update(
                new Rotation(rotation.getX(), rotation.getY()),
                360, 1, 1
        );
    }

    private void onRelease() {
        if (mc.player == null) return;

        if (autoThirdPerson.get()) {
            mc.options.setPerspective(prevPerspective);
        }

        if (FreeLookHandler.isActive()) {
            FreeLookHandler.setActive(false);
            mc.player.setYaw(rotation.x);
            mc.player.setPitch(rotation.y);
        }
    }

    private void setRotation() {
        rotation = new Vector2f(mc.player.getYaw(), mc.player.getPitch());
    }

    public static boolean isKeyDown(int keyCode) {
        return InputUtil.isKeyPressed(
                MinecraftClient.getInstance().getWindow().getHandle(),
                keyCode
        );
    }
}
