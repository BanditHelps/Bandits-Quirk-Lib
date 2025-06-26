package com.github.b4ndithelps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BanditsQuirkLib {
    public static final String MOD_ID = "bandits_quirk_lib";
    public static final String LOGGING_ID = "Bandit's Quirk Lib";

    public static final Logger LOGGER = LoggerFactory.getLogger(LOGGING_ID);

    public static void init() {
        // Write common init code here.
        LOGGER.info("Initializing Bandit's Quirk Lib");
    }
}
