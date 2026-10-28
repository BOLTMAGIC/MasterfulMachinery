package io.ticticboom.mods.mmtest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a static {@code void (GameTestHelper)} method of a {@link GameTestHolder} class as a game test. Minecraft
 * 1.21.5 dropped its @GameTest annotation (tests are registry entries now); this keeps the 1.20.1 test sources
 * readable and {@link MMGameTests} registers what it marks.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GameTest {
    /** A structure under data/mmtest/structure. */
    String template() default "empty";

    int timeoutTicks() default 100;

    int setupTicks() default 0;

    boolean required() default true;
}
