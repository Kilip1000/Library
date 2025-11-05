package de.kilip.library;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.MergedComponentMap;
import net.minecraft.component.type.WritableBookContentComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.WritableBookItem;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.RawFilteredPair;

import java.math.BigInteger;
import java.util.Objects;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static de.kilip.library.BookUtils.giveItemStacks;
import static de.kilip.library.BookUtils.makeWritableBooks;
import static de.kilip.library.Library.playerPositionMap;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class BookCommands {
    static String[] OPERATORS = new String[]{"+", "-", "*", "/", "%", "<", ">","=b","=a","&","|"};
    static String[] LOADSAVE = new String[]{"load","save"};

    static final SuggestionProvider<ServerCommandSource> OPERATOR_SUGGESTIONS = (context, builder) -> {
        for (String op : OPERATORS) {
            builder.suggest(op);
        }
        return builder.buildFuture();
    };

    static final SuggestionProvider<ServerCommandSource> LOADSAVE_SUGGESTIONS = (context, builder) -> {
        for (String ls : LOADSAVE) {
            builder.suggest(ls);
        }
        return builder.buildFuture();
    };

    public static void registerEditCommand(){
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    literal("edit")
                                    .then(argument("player", EntityArgumentType.player())
                                    .then(argument("index", integer())
                                            .then(argument("load or save", string())
                                                    .suggests(LOADSAVE_SUGGESTIONS)
                                                    .executes(context -> {
                                                        ServerCommandSource source = context.getSource();
                                                        ServerPlayerEntity player = source.getPlayerOrThrow();

                                                        String load_Save = getString(context,"load or save");
                                                        ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "player");
                                                        int index = getInteger(context, "index");


                                                        if(Objects.equals(load_Save, "load")) {
                                                            Library.PlayerPosKey key = new Library.PlayerPosKey(targetPlayer.getUuid(), index);
                                                            BigInteger seed = playerPositionMap.get(key);
                                                            String encoded = Unifont.encode(seed);
                                                            giveItemStacks(player,makeWritableBooks(encoded,false,50));
                                                            return 1;
                                                        }

                                                        if(Objects.equals(load_Save, "save")) {
                                                            ItemStack stack = targetPlayer.getEquippedStack(EquipmentSlot.MAINHAND);
                                                            StringBuilder str = new StringBuilder();
                                                            if (stack.getItem() instanceof WritableBookItem) {
                                                                MergedComponentMap mergedComponentMap = (MergedComponentMap) stack.getComponents();
                                                                WritableBookContentComponent bookContent = mergedComponentMap.get(DataComponentTypes.WRITABLE_BOOK_CONTENT);

                                                                if (bookContent != null) {
                                                                    for (int pageIndex = 0; pageIndex < bookContent.pages().size(); pageIndex++) {
                                                                        RawFilteredPair<String> page = bookContent.pages().get(pageIndex);

                                                                        String text = page.filtered().orElse(page.raw()); // use raw if filtered not present
                                                                        str.append(text);
                                                                    }
                                                                }
                                                            }

                                                            Library.PlayerPosKey key = new Library.PlayerPosKey(targetPlayer.getUuid(), index);
                                                            playerPositionMap.put(key, Unifont.decode(String.valueOf(str)));
                                                            return 1;
                                                        }



                                                        return 0;
                                                    })
                                            )


                    )));
        });
    }

    public static void registerOperationCommand(){
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    literal("operation")
                            .then(argument("player1", EntityArgumentType.player())
                            .then(argument("index1", integer())
                            .then(argument("operator", StringArgumentType.string()).suggests(OPERATOR_SUGGESTIONS)
                            .then(argument("player2", EntityArgumentType.player())
                            .then(argument("index2", integer())
                            .then(argument("player3", EntityArgumentType.player())
                            .then(argument("index3", integer())
                            .executes(context -> {
                                String operator = getString(context, "operator");
                                ServerPlayerEntity player1 = EntityArgumentType.getPlayer(context, "player1");
                                ServerPlayerEntity player2 = EntityArgumentType.getPlayer(context, "player2");
                                ServerPlayerEntity player3 = EntityArgumentType.getPlayer(context, "player3");
                                int index1 = getInteger(context, "index1");
                                int index2 = getInteger(context, "index2");
                                int index3 = getInteger(context, "index3");
                                Library.PlayerPosKey key1 = new Library.PlayerPosKey(player1.getUuid(), index1);
                                Library.PlayerPosKey key2 = new Library.PlayerPosKey(player2.getUuid(), index2);
                                Library.PlayerPosKey key3 = new Library.PlayerPosKey(player3.getUuid(), index3);

                                BigInteger val1 = playerPositionMap.get(key1);
                                BigInteger val2 = playerPositionMap.get(key2);

                                playerPositionMap.put(key3, parseMath(val1,val2,operator));
                                return 1;
                            })

              )))))))
            );
        });
    }

    public static void registerCommands(){
        registerEditCommand();
        registerOperationCommand();
    }


    public static BigInteger parseMath(BigInteger a, BigInteger b, String operator)
    {

        return switch (operator) {
            case "=a" -> a;
            case "=b" -> b;
            case "|" -> a.or(b);
            case "&" -> a.and(b);
            case "+" -> a.add(b);
            case "-" -> a.subtract(b);
            case "*" -> a.multiply(b);
            case "/" -> {
                if (b.equals(BigInteger.ZERO)) throw new IllegalArgumentException("Division by zero");
                yield a.divide(b);
            }
            case "%" -> {
                if (b.equals(BigInteger.ZERO)) throw new IllegalArgumentException("Division by zero");
                yield a.mod(b);
            }
            case "<" -> a.min(b);
            case ">" -> a.max(b);
            default -> throw new IllegalArgumentException("Unknown operator: " + operator);
        };

    }


}
