package io.ticticboom.mods.mmtest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A class holding {@link GameTest} methods; listed in {@link MMGameTests#HOLDERS}. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GameTestHolder {
    String value() default "mmtest";
}
