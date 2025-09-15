package io.github.davidvsaraiva.merlin.monitor;

class Config {

    private Config() {
    }

    static String getEnvOrDefault(String key, String defaultValue) {
        String val = System.getenv(key);
        return val != null ? val : defaultValue;
    }

    static String getEnv(String key) {
        String val = System.getenv(key);
        if (val == null) {
            throw new IllegalArgumentException(String.format("Missing environment variable '%s'", key));
        }
        return val;
    }
}
