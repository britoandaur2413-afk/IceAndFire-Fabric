package com.iafabric.client.model.util;

import com.iafabric.citadel.client.model.TabulaModelHandler;
import com.iafabric.citadel.client.model.container.TabulaModelContainer;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TabulaModelHandlerHelper {

    public static TabulaModelContainer loadTabulaModel(String path) throws IOException {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        // resource names are lowercase in 26.x (the .tbl files were renamed)
        path = path.toLowerCase(java.util.Locale.ROOT);
        if (!path.endsWith(".tbl")) {
            path = path + ".tbl";
        }

        InputStream stream = TabulaModelHandler.INSTANCE.getClass().getClassLoader().getResourceAsStream(path.substring(1));
        if (stream == null) {
            throw new IOException("No Tabula model at " + path);
        }
        return TabulaModelHandler.INSTANCE.loadTabulaModel(getModelJsonStream(path, stream));
    }

    private static InputStream getModelJsonStream(String name, InputStream file) throws IOException {
        ZipInputStream zip = new ZipInputStream(file);

        ZipEntry entry;
        do {
            if ((entry = zip.getNextEntry()) == null) {
                throw new RuntimeException("No model.json present in " + name);
            }
        } while (!entry.getName().equals("model.json"));

        return zip;
    }
}
