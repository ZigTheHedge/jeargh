package com.cwelth.jeargh.commands;

import com.cwelth.jeargh.JEARGH;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class FixHeights {
    public static ArgumentBuilder<CommandSourceStack, ?> register(CommandDispatcher<CommandSourceStack> dispatcher) {
        return Commands.literal("fixheights")
                .then(Commands.argument("radius", IntegerArgumentType.integer(5))
                    .executes( cs -> {
                        JEARGH.profiler.fixIncorrectHeights(cs.getSource().getPlayer(), IntegerArgumentType.getInteger(cs, "radius"));
                        return 0;
                }));
    }
}
