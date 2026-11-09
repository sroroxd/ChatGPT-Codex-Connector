package de.sprachpilot;

import android.content.res.AssetManager;
import java.io.*;
import java.nio.file.Files;

final class Assets {
    static byte[] bytes(AssetManager assets,String name) throws IOException {
        try (InputStream in = assets.open(name); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[16384]; int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer,0,count);
            return out.toByteArray();
        }
    }
    static void copyTree(AssetManager assets, String source, File target) throws IOException {
        String[] children = assets.list(source);
        if (children != null && children.length > 0) {
            if (!target.isDirectory() && !target.mkdirs()) throw new IOException("Modellverzeichnis fehlt");
            for (String name : children) copyTree(assets, source + "/" + name, new File(target, name));
        } else {
            try (InputStream in = assets.open(source); OutputStream out = Files.newOutputStream(target.toPath())) {
                byte[] buffer = new byte[16384]; int count;
                while ((count = in.read(buffer)) != -1) out.write(buffer,0,count);
            }
        }
    }
    static File model(android.content.Context ctx) throws IOException {
        File target = new File(ctx.getFilesDir(), "model-de");
        File marker = new File(target, ".complete-v1");
        if (!marker.isFile()) {
            copyTree(ctx.getAssets(), "model-de", target);
            try (OutputStream out = Files.newOutputStream(marker.toPath())) { out.write("vosk-model-small-de-0.15".getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
        }
        return target;
    }
}
