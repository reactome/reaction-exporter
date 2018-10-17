package org.reactome.server.tools.reaction.exporter.renderer.profile;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ProfileFactory {

    private static final Collection<String> PROFILE_FILENAMES = Arrays.asList("" +
            "diagram_profile_01",
            "diagram_profile_02"
            );

    private static final String DEFAULT_PROFILE_NAME = "modern";
    private static final Map<String, DiagramProfile> PROFILE_MAP = new HashMap<>();

    static {
        final ObjectMapper mapper = new ObjectMapper();
        for (String profileFilename : PROFILE_FILENAMES) {
            try {
                final String name = "/profiles/" + profileFilename + ".json";
                final InputStream resource = ProfileFactory.class.getResourceAsStream(name);
                final String json = IOUtils.toString(resource, Charset.defaultCharset());
                mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
                final DiagramProfile profile = mapper.readValue(json, DiagramProfile.class);
                PROFILE_MAP.put(profile.getName().toLowerCase(), profile);
            } catch (IOException e) {
                // this should work always
                throw new RuntimeException("Resource missing " + profileFilename, e);
            }

        }
    }

    public static DiagramProfile get(String profileName) {
        if (profileName == null) profileName = DEFAULT_PROFILE_NAME;
        return PROFILE_MAP.getOrDefault(profileName.toLowerCase(), PROFILE_MAP.get(DEFAULT_PROFILE_NAME));
    }
}
