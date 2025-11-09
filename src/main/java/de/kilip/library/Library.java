package de.kilip.library;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;

public class Library implements ModInitializer {
	public static final String MOD_ID = "library";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public record PlayerPosKey(UUID playerId, int mapPosition) {}
    public static Map<PlayerPosKey, BigInteger> playerPositionMap = new HashMap<>();

    @Override
	public void onInitialize() {
        BaseSixtyKConverterCommands.registerCommands();
        BookCommands.registerCommands();
        BlockCommands.registerCommands();

		LOGGER.info("Library mod initialized with fully reversible BigInteger placement.");
	}
    /*
        System.out.println("Started");
		BigInteger x = new BigInteger("12345678901234567890");
		BigInteger y = new BigInteger("9876543210987654321");
		BigInteger z = new BigInteger("1928374655647382910");

		BigInteger seed = Morton3D.encode(x, y, z);
		BigInteger[] decoded = Morton3D.decode(seed);
		LOGGER.info(decoded[0]+" " + decoded[1]+" "+decoded[2]);
    */

}
