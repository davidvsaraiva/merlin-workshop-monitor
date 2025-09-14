package io.github.davidvsaraiva.merlin.monitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class WorkshopsRepository {

    private static final Logger LOG = LoggerFactory.getLogger(WorkshopsRepository.class);


    private final Path path;
    private final ObjectMapper mapper;

    public WorkshopsRepository(Path path) {
        this.path = path;
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public WorkshopState loadOrCreate() throws IOException {
        LOG.debug("WorkshopsRepository.loadOrCreate() called");
        if (!Files.exists(path)) {
            LOG.debug("File {} does not exist. Creating empty workshop list object", path);
            return new WorkshopState(new HashMap<>(), null);
        }
        return mapper.readValue(Files.readString(path), WorkshopState.class);
    }

    public void save (WorkshopState state) throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), state);
    }

    public static class StoreData {
        private Map<String, WorkshopEntry> workshops;
        private String lastChecked;

        public StoreData() {
        }

        public StoreData(Map<String, WorkshopEntry> workshops, String lastChecked) {
            this.workshops = workshops;
            this.lastChecked = lastChecked;
        }

        public Map<String, WorkshopEntry> getWorkshops() {
            return workshops;
        }

        public String getLastChecked() {
            return lastChecked;
        }

        public void setLastChecked(String lastChecked) {
            this.lastChecked = lastChecked;
        }
    }
}
