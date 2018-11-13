package net.covers1624.classloader.resolvers;

import net.covers1624.classloader.IResourceResolver;
import net.covers1624.classloader.IResourceResolverFactory;
import net.covers1624.classloader.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;

/**
 * Created by covers1624 on 10/11/18.
 */
public class SimpleResolver implements IResourceResolverFactory {

    @Override
    public IResourceResolver create() throws IOException {
        for (URL url : Utils.toIterable(getClass().getClassLoader().getResources("META-INF/MANIFEST.MF"))) {
            try (InputStream is = url.openStream()) {
                Manifest manifest = new Manifest(is);
                String rel = manifest.getMainAttributes().getValue("Resolver-Path");
                if (rel != null) {
                    List<URL> urls = new ArrayList<>();
                    if (rel.contains(";")) {
                        for (String seg : rel.split(";")) {
                            urls.addAll(Utils.dirToURLs(new File(seg), (dir, name) -> name.endsWith(".jar")));
                        }
                    } else {
                        urls.addAll(Utils.dirToURLs(new File(rel), (dir, name) -> name.endsWith(".jar")));
                    }
                    return IResourceResolver.fromURLs(urls);
                }
            }
        }
        return null;
    }
}
