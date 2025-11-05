package de.kilip.library;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.math.BigInteger;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class BaseSixtyKConverterCommands {


    public static void registerDecimalToBase60k(){
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        literal("decimalToBase60k").then(argument("num", string()).executes(context -> {
                            ServerCommandSource source = context.getSource();

                            BigInteger num;
                            try {
                                String numString = getString(context, "num");
                                num = new BigInteger(numString);
                            } catch (NumberFormatException e) {
                                source.sendError(Text.of("Invalid number!"));
                                return 0;
                            }

                            String encoded = Unifont.encode(num);
                            source.sendFeedback(() -> Text.of(encoded), false);

                            return 1;
                        }))));
    }
    public static void registerBase60kToDecimal(){
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        literal("base60kToDecimal")
                        .then(argument("num", string())
                        .executes(context -> {

                            ServerCommandSource source = context.getSource();

                            String numString = getString(context, "num");
                            BigInteger decoded;
                            try {
                                decoded = Unifont.decode(numString);
                            } catch (IllegalArgumentException e) {
                                source.sendError(Text.of("Invalid Unicode input!"));
                                return 0;
                            }

                            source.sendFeedback(() -> Text.of(decoded.toString()), false);

                            return 1;
                        }))
                )
        );
    }
    public static void registerCommands(){
        registerBase60kToDecimal();
        registerDecimalToBase60k();
    }
}
