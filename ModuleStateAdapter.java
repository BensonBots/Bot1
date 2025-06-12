package newgame;

import com.google.gson.*;
import com.google.gson.stream.*;
import java.io.IOException;

public class ModuleStateAdapter extends TypeAdapter<ModuleState<?>> {
    @Override
    public void write(JsonWriter out, ModuleState<?> value) throws IOException {
        out.beginObject();
        out.name("enabled").value(value.enabled);
        
        if (value.settings != null) {
            out.name("settings");
            // Write the settings as a string representation
            out.value(value.settings.toString());
        }
        out.endObject();
    }

    @Override
    public ModuleState<?> read(JsonReader in) throws IOException {
        boolean enabled = false;
        String settings = null;
        
        in.beginObject();
        while (in.hasNext()) {
            String name = in.nextName();
            if (name.equals("enabled")) {
                enabled = in.nextBoolean();
            } else if (name.equals("settings")) {
                settings = in.nextString();
            }
        }
        in.endObject();
        
        // FIXED: Return the settings string instead of null
        // This allows Auto Gather settings to be properly preserved
        return new ModuleState<>(enabled, settings);
    }
}