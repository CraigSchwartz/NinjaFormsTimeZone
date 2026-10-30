package com.astrotools.ninjaforms.generate;

import com.astrotools.ninjaforms.model.NinjaFormsElement;
import com.astrotools.ninjaforms.model.NinjaFormsPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Turns a {@link NinjaFormsPlugin} description into the files of a WordPress
 * plugin that registers custom Ninja Forms elements.
 *
 * <p>Output layout (mirrors Ninja Forms' own extension plugins):</p>
 * <pre>
 * &lt;outputDir&gt;/&lt;pluginSlug&gt;/
 *   &lt;pluginSlug&gt;.php                    main plugin file
 *   includes/Fields/&lt;FieldClass&gt;.php   one field class per element
 *   templates/fields-&lt;type&gt;.html       one front-end template per element
 * </pre>
 */
public final class PhpPluginGenerator
{
    /** Generates the plugin files and returns the paths written, in order. */
    public List<Path> generate( NinjaFormsPlugin plugin, Path outputDir ) throws IOException
    {
        plugin.validate();
        Path pluginDir = outputDir.resolve( plugin.getPluginSlug() );
        List<Path> written = new ArrayList<>();

        written.add( writeMainFile( plugin, pluginDir ) );
        for ( NinjaFormsElement element : plugin.getElements() ) {
            written.add( writeFieldClass( plugin, element, pluginDir ) );
            written.add( writeFieldTemplate( element, pluginDir ) );
        }
        return written;
    }

    private Path writeMainFile( NinjaFormsPlugin plugin, Path pluginDir ) throws IOException
    {
        String registrations = plugin.getElements().stream()
            .map( element -> String.format(
                "\trequire_once plugin_dir_path( __FILE__ ) . 'includes/Fields/%1$s.php';\n" +
                "\t$fields[ '%2$s' ] = new %1$s();",
                element.getFieldClass(), element.getType() ) )
            .collect( Collectors.joining( "\n\n" ) );

        Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put( "plugin_name", plugin.getPluginName() );
        tokens.put( "plugin_slug", plugin.getPluginSlug() );
        tokens.put( "description", plugin.getDescription() );
        tokens.put( "version", plugin.getVersion() );
        tokens.put( "author", plugin.getAuthor() );
        tokens.put( "text_domain", plugin.getTextDomain() );
        tokens.put( "function_prefix", plugin.getFunctionPrefix() );
        tokens.put( "element_registrations", registrations );

        return writeFile(
            pluginDir.resolve( plugin.getPluginSlug() + ".php" ),
            TemplateEngine.render( TemplateEngine.load( "plugin-main.php.tpl" ), tokens ) );
    }

    private Path writeFieldClass( NinjaFormsPlugin plugin, NinjaFormsElement element, Path pluginDir )
        throws IOException
    {
        String templateName = ( element.getKind() == NinjaFormsElement.Kind.SELECT )
            ? "field-select.php.tpl" : "field-text.php.tpl";

        Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put( "field_class", element.getFieldClass() );
        tokens.put( "type", element.getType() );
        tokens.put( "nicename", element.getNiceName() );
        tokens.put( "section", element.getSection() );
        tokens.put( "icon", element.getIcon() );
        tokens.put( "test_value", element.getTestValue() );
        tokens.put( "text_domain", plugin.getTextDomain() );
        tokens.put( "placeholder_label", element.getPlaceholderLabel() );

        // Render the options snippet with the tokens known so far (not including
        // options_php itself), so snippets may reference %text_domain% etc.
        String optionsPhp = TemplateEngine.render( element.getOptionsPhp(), tokens );
        tokens.put( "options_php", optionsPhp );

        return writeFile(
            pluginDir.resolve( "includes/Fields/" + element.getFieldClass() + ".php" ),
            TemplateEngine.render( TemplateEngine.load( templateName ), tokens ) );
    }

    private Path writeFieldTemplate( NinjaFormsElement element, Path pluginDir ) throws IOException
    {
        String templateName = ( element.getKind() == NinjaFormsElement.Kind.SELECT )
            ? "template-select.html.tpl" : "template-text.html.tpl";

        Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put( "type", element.getType() );

        return writeFile(
            pluginDir.resolve( "templates/fields-" + element.getType() + ".html" ),
            TemplateEngine.render( TemplateEngine.load( templateName ), tokens ) );
    }

    private static Path writeFile( Path target, String content ) throws IOException
    {
        Files.createDirectories( target.getParent() );
        Files.writeString( target, content, StandardCharsets.UTF_8 );
        return target;
    }
}
