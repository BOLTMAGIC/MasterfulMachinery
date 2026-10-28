package io.ticticboom.mods.mmtest;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Registers the {@link GameTest} methods of the holder classes. Since 1.21.5 a game test is a test function (in the
 * test_function registry) plus a test instance naming it, its structure and its environment.
 */
public final class MMGameTests {
    public static final String NAMESPACE = "mmtest";

    /** The classes whose @GameTest methods run; a class is listed once its tests are ported to 26.1. */
    static final List<Class<?>> HOLDERS = List.of(
            ItemPortInsertionGameTests.class,
            RegistrySmokeGameTests.class
    );

    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, NAMESPACE);
    private static final List<Registered> TESTS = new ArrayList<>();

    private record Registered(Identifier id, GameTest settings) {
    }

    private MMGameTests() {
    }

    public static void register(IEventBus modBus) {
        for (Class<?> holder : HOLDERS) {
            for (Method method : holder.getDeclaredMethods()) {
                GameTest settings = method.getAnnotation(GameTest.class);
                if (settings == null || !Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                method.setAccessible(true);
                var name = (holder.getSimpleName() + "." + method.getName()).toLowerCase(Locale.ROOT);
                var id = Identifier.fromNamespaceAndPath(NAMESPACE, name);
                FUNCTIONS.register(name, () -> helper -> invoke(method, helper));
                TESTS.add(new Registered(id, settings));
            }
        }
        FUNCTIONS.register(modBus);
        modBus.addListener(MMGameTests::onRegisterTests);
    }

    private static void invoke(Method method, GameTestHelper helper) {
        try {
            method.invoke(null, helper);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (e.getCause() instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(e.getCause());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void onRegisterTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(Identifier.fromNamespaceAndPath(NAMESPACE, "default"));
        for (Registered test : TESTS) {
            var function = ResourceKey.create(Registries.TEST_FUNCTION, test.id());
            var structure = Identifier.fromNamespaceAndPath(NAMESPACE, test.settings().template());
            var data = new TestData<>(environment, structure, test.settings().timeoutTicks(), test.settings().setupTicks(), test.settings().required());
            event.registerTest(test.id(), new FunctionGameTestInstance(function, data));
        }
    }
}
