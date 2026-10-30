package com.astrotools.ninjaforms.generate;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Minimal token replacement for the PHP/HTML templates shipped as resources.
 *
 * <p>Tokens look like {@code %token_name%}. Only the tokens present in the
 * supplied map are replaced, so PHP {@code sprintf()} format strings such as
 * {@code '%s (UTC%s)'} pass through untouched, as does the Underscore.js
 * syntax of the field templates ({@code {{{ data.id }}}}).</p>
 */
public final class TemplateEngine
{
    private static final String RESOURCE_DIR = "/com/astrotools/ninjaforms/templates/";

    private TemplateEngine() {}

    /** Loads a bundled template resource. */
    public static String load( String templateName )
    {
        try ( InputStream in = TemplateEngine.class.getResourceAsStream( RESOURCE_DIR + templateName ) ) {
            if ( in == null ) {
                throw new IOException( "Template resource not found: " + RESOURCE_DIR + templateName );
            }
            return new String( in.readAllBytes(), StandardCharsets.UTF_8 );
        } catch ( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    /** Replaces every {@code %token%} present in {@code tokens}. */
    public static String render( String template, Map<String, String> tokens )
    {
        String result = template;
        for ( Map.Entry<String, String> entry : tokens.entrySet() ) {
            String value = entry.getValue() == null ? "" : entry.getValue();
            result = result.replace( "%" + entry.getKey() + "%", value );
        }
        return result;
    }
}
