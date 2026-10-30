package com.astrotools.ninjaforms.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Declarative description of the WordPress plugin that registers one or more
 * custom Ninja Forms elements. This is the top-level input of the generator.
 */
public final class NinjaFormsPlugin
{
    private static final Pattern SLUG    = Pattern.compile( "^[a-z][a-z0-9-]{1,48}$" );
    private static final Pattern FUNC    = Pattern.compile( "^[a-z][a-z0-9_]{0,30}$" );

    private final String pluginName;
    private final String pluginSlug;
    private final String description;
    private final String author;
    private final String version;
    private final String textDomain;
    private final String functionPrefix;
    private final List<NinjaFormsElement> elements;

    private NinjaFormsPlugin( Builder builder )
    {
        this.pluginName = builder.pluginName;
        this.pluginSlug = builder.pluginSlug;
        this.description = builder.description;
        this.author = builder.author;
        this.version = builder.version;
        this.textDomain = builder.textDomain != null ? builder.textDomain : builder.pluginSlug;
        this.functionPrefix = builder.functionPrefix != null
            ? builder.functionPrefix
            : builder.pluginSlug != null ? builder.pluginSlug.replace( '-', '_' ) : null;
        this.elements = List.copyOf( builder.elements );
    }

    /** Human-readable plugin name used in the WordPress plugin header. */
    public String getPluginName() { return pluginName; }

    /** Plugin slug; doubles as the plugin directory and main file base name. */
    public String getPluginSlug() { return pluginSlug; }

    public String getDescription() { return description; }

    public String getAuthor() { return author; }

    public String getVersion() { return version; }

    public String getTextDomain() { return textDomain; }

    /** Prefix for the generated PHP function names, e.g. {@code nftz}. */
    public String getFunctionPrefix() { return functionPrefix; }

    public List<NinjaFormsElement> getElements() { return elements; }

    /** Checks that this description can be turned into PHP safely. */
    public void validate()
    {
        require( pluginName != null && !pluginName.isBlank(), "pluginName is required" );
        require( pluginSlug != null && SLUG.matcher( pluginSlug ).matches(),
            "pluginSlug '" + pluginSlug + "' must be a lowercase slug like 'my-plugin' ([a-z][a-z0-9-], 2-49 chars)" );
        require( description != null && !description.isBlank(), "description is required" );
        require( author != null && !author.isBlank(), "author is required" );
        require( version != null && version.matches( "^[0-9][0-9A-Za-z.-]*$" ), "version must look like 0.1.0" );
        require( functionPrefix != null && FUNC.matcher( functionPrefix ).matches(),
            "functionPrefix '" + functionPrefix + "' must be a lowercase PHP identifier" );
        require( !elements.isEmpty(), "at least one element is required" );

        Set<String> types = new HashSet<>();
        Set<String> classes = new HashSet<>();
        for ( NinjaFormsElement element : elements ) {
            element.validate();
            require( types.add( element.getType() ), "duplicate element type: " + element.getType() );
            require( classes.add( element.getFieldClass() ),
                "duplicate element field class: " + element.getFieldClass() );
        }
    }

    private static void require( boolean condition, String message )
    {
        if ( !condition ) throw new IllegalStateException( "Invalid plugin: " + message );
    }

    /** Builder for {@link NinjaFormsPlugin}. */
    public static final class Builder
    {
        private String pluginName;
        private String pluginSlug;
        private String description;
        private String author;
        private String version = "0.1.0";
        private String textDomain;
        private String functionPrefix;
        private final List<NinjaFormsElement> elements = new ArrayList<>();

        public Builder pluginName( String pluginName ) { this.pluginName = pluginName; return this; }

        public Builder pluginSlug( String pluginSlug ) { this.pluginSlug = pluginSlug; return this; }

        public Builder description( String description ) { this.description = description; return this; }

        public Builder author( String author ) { this.author = author; return this; }

        public Builder version( String version ) { this.version = version; return this; }

        public Builder textDomain( String textDomain ) { this.textDomain = textDomain; return this; }

        public Builder functionPrefix( String functionPrefix )
        {
            this.functionPrefix = functionPrefix; return this;
        }

        public Builder element( NinjaFormsElement element )
        {
            elements.add( Objects.requireNonNull( element ) ); return this;
        }

        public NinjaFormsPlugin build()
        {
            return new NinjaFormsPlugin( this );
        }
    }
}
