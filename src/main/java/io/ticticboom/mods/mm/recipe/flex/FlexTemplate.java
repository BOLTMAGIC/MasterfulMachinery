package io.ticticboom.mods.mm.recipe.flex;

import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Turns an input item id into an output item id for flex recipes.
 * "match" is a regex tested against the whole path of the input id; "item" is the output id, where
 * {ns} is the input namespace, {path} the input path and $1..$n the match groups.
 * E.g. match "(.+)_seeds", item "{ns}:$1_essence": mysticalagriculture:iron_seeds -> mysticalagriculture:iron_essence.
 */
public record FlexTemplate(Pattern match, String item) {

    public static FlexTemplate parse(@Nullable String match, String item) {
        if (item == null || item.isBlank()) {
            throw new RuntimeException("Flex output needs an 'item' template, e.g. \"{ns}:$1_essence\"");
        }
        try {
            return new FlexTemplate(Pattern.compile(match == null || match.isEmpty() ? "(.*)" : match), item);
        } catch (PatternSyntaxException e) {
            throw new RuntimeException("Flex output 'match' is not a valid regex: " + match, e);
        }
    }

    /**
     * @return the output id text, or null if the input path doesn't match
     */
    @Nullable
    public String apply(String namespace, String path) {
        Matcher m = match.matcher(path);
        if (!m.matches()) {
            return null;
        }
        String out = item.replace("{ns}", namespace).replace("{path}", path);
        // highest group first, so $1 doesn't eat the start of $10
        for (int g = m.groupCount(); g >= 1; g--) {
            String group = m.group(g);
            out = out.replace("$" + g, group == null ? "" : group);
        }
        return out;
    }
}
