package it.univaq.caffeine.catan.config;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.io.Reader;

public final class ConfigLoader {

    private static final String CONFIG_PATH =
            "game_config.json";

    private ConfigLoader(){}

    public static GameConfig load() {

        try {

            Reader reader =
                    new InputStreamReader(
                            ConfigLoader.class
                                    .getClassLoader()
                                    .getResourceAsStream(CONFIG_PATH)
                    );

            return new Gson().fromJson(
                    reader,
                    GameConfig.class);

        } catch(Exception e){

            throw new RuntimeException(
                    "Cannot load config",
                    e);
        }
    }
}