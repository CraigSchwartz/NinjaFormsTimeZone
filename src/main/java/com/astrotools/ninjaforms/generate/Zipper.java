package com.astrotools.ninjaforms.generate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Zips a generated plugin directory for upload via the WordPress plugin
 * installer. The archive contains the plugin folder itself, so extracting it
 * inside wp-content/plugins/ produces the expected directory layout.
 */
public final class Zipper
{
    private Zipper() {}

    public static void zipDirectory( Path pluginDir, Path zipFile ) throws IOException
    {
        Path root = pluginDir.toAbsolutePath().normalize();
        String prefix = root.getFileName().toString();
        try ( ZipOutputStream zip = new ZipOutputStream(
                Files.newOutputStream( zipFile.toAbsolutePath().normalize() ) );
              Stream<Path> files = Files.walk( root ) ) {

            files.filter( Files::isRegularFile ).forEach( file -> {
                try {
                    String entryName = prefix + "/" + root.relativize( file );
                    zip.putNextEntry( new ZipEntry( entryName ) );
                    Files.copy( file, zip );
                    zip.closeEntry();
                } catch ( IOException e ) {
                    throw new UncheckedIOException( e );
                }
            } );
        }
    }
}
