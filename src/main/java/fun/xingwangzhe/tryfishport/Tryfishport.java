package fun.xingwangzhe.tryfishport;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tryfishport implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("tryfishport");

	@Override
	public void onInitialize() {
		LOGGER.info("Tryfishport mod initialized!");
	}
}