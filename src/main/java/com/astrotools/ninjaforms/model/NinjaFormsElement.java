package com.astrotools.ninjaforms.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Declarative description of one custom Ninja Forms element (field type).
 *
 * <p>The generator turns one or more elements into a WordPress plugin that
 * registers them with Ninja Forms 3.</p>
 *
 * <p>Two kinds are supported:</p>
 * <ul>
 *   <li>{@link Kind#SELECT} - a fixed-option dropdown whose options are
 *       produced by a PHP snippet (mirrors Ninja Forms' own Country field).</li>
 *   <li>{@link Kind#TEXT} - a single-line text input (mirrors the Textbox field).</li>
 * </ul>
 */
public final class NinjaFormsElement
{
    public enum Kind { SELECT, TEXT }

    private static final Pattern TYPE_SLUG  = Pattern.compile( "^[a-z][a-z0-9-]{1,48}$" );
    private static final Pattern PHP_CLASS  = Pattern.compile( "^[A-Z][A-Za-z0-9_]{1,60}$" );

    private final Kind kind;
    private final String type;
    private final String fieldClass;
    private final String niceName;
    private final String section;
    private final String icon;
    private final String testValue;
    private final String placeholderLabel;
    private final String optionsPhp;

    private NinjaFormsElement( Builder builder )
    {
        this.kind = builder.kind;
        this.type = builder.type;
        this.fieldClass = builder.fieldClass;
        this.niceName = builder.niceName;
        this.section = builder.section;
        this.icon = builder.icon;
        this.testValue = builder.testValue;
        this.placeholderLabel = builder.placeholderLabel;
        this.optionsPhp = builder.optionsPhp;
    }

    public static Builder select( String type, String fieldClass, String niceName )
    {
        return new Builder( Kind.SELECT, type, fieldClass, niceName );
    }

    public static Builder text( String type, String fieldClass, String niceName )
    {
        return new Builder( Kind.TEXT, type, fieldClass, niceName );
    }

    public Kind getKind() { return kind; }

    public String getType() { return type; }

    public String getFieldClass() { return fieldClass; }

    public String getNiceName() { return niceName; }

    public String getSection() { return section; }

    public String getIcon() { return icon; }

    public String getTestValue() { return testValue; }

    /** Label of the empty first option of a SELECT element. */
    public String getPlaceholderLabel() { return placeholderLabel; }

    /** PHP statements appended inside {@code get_options()}; used by SELECT only. */
    public String getOptionsPhp() { return optionsPhp; }

    /** Checks the values are safe to interpolate into the generated PHP. */
    void validate()
    {
        require( kind != null, "kind is required" );
        require( type != null && TYPE_SLUG.matcher( type ).matches(),
            "type '" + type + "' must be a lowercase slug like 'my-field-type' ([a-z][a-z0-9-], 2-49 chars)" );
        require( fieldClass != null && PHP_CLASS.matcher( fieldClass ).matches(),
            "fieldClass '" + fieldClass + "' must be a valid PHP class name starting with an uppercase letter" );
        require( phpLiteral( niceName ), "niceName must not contain quotes, backslashes or newlines" );
        require( phpLiteral( section ), "section must not contain quotes, backslashes or newlines" );
        require( phpLiteral( icon ), "icon must not contain quotes, backslashes or newlines" );
        require( phpLiteral( testValue ), "testValue must not contain quotes, backslashes or newlines" );
        if ( kind == Kind.SELECT ) {
            require( phpLiteral( placeholderLabel ),
                "placeholderLabel must not contain quotes, backslashes or newlines" );
            require( optionsPhp != null && !optionsPhp.isBlank(), "SELECT elements require optionsPhp" );
            require( !optionsPhp.contains( "%options_php%" ), "optionsPhp must not reference %options_php%" );
        } else {
            require( optionsPhp == null || optionsPhp.isBlank(), "TEXT elements cannot define optionsPhp" );
        }
    }

    private static void require( boolean condition, String message )
    {
        if ( !condition ) throw new IllegalStateException( "Invalid element: " + message );
    }

    private static boolean phpLiteral( String value )
    {
        return value != null && value.indexOf( '\'' ) < 0
            && value.indexOf( '\\' ) < 0 && value.indexOf( '\n' ) < 0;
    }

    /** Builder for {@link NinjaFormsElement}. */
    public static final class Builder
    {
        private final Kind kind;
        private final String type;
        private final String fieldClass;
        private final String niceName;
        private String section = "common";
        private String icon;
        private String testValue = "";
        private String placeholderLabel;
        private String optionsPhp = "";

        private Builder( Kind kind, String type, String fieldClass, String niceName )
        {
            this.kind = kind;
            this.type = type;
            this.fieldClass = fieldClass;
            this.niceName = niceName;
        }

        public Builder section( String section ) { this.section = section; return this; }

        public Builder icon( String icon ) { this.icon = icon; return this; }

        public Builder testValue( String testValue ) { this.testValue = testValue; return this; }

        /** Label of the empty first option (SELECT only). */
        public Builder placeholderLabel( String placeholderLabel )
        {
            this.placeholderLabel = placeholderLabel; return this;
        }

        /**
         * PHP statements appended inside the generated {@code get_options()}
         * method (SELECT only). The statements must populate the {@code $options}
         * array with entries holding {@code label}, {@code value}, {@code calc},
         * {@code selected} and {@code order} keys. Tokens such as
         * {@code %text_domain%} may be used.
         */
        public Builder optionsPhp( String optionsPhp ) { this.optionsPhp = optionsPhp; return this; }

        public NinjaFormsElement build()
        {
            if ( icon == null ) icon = ( kind == Kind.SELECT ) ? "chevron-down" : "text-width";
            if ( placeholderLabel == null ) placeholderLabel = "Select " + niceName;
            return new NinjaFormsElement( this );
        }
    }
}
