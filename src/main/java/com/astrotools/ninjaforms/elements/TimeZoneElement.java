package com.astrotools.ninjaforms.elements;

import com.astrotools.ninjaforms.model.NinjaFormsElement;
import com.astrotools.ninjaforms.model.NinjaFormsPlugin;

/**
 * Built-in "Timezone" element: a select field offering every IANA timezone
 * identifier, each labelled with its current UTC offset
 * (e.g. "Europe/Berlin (UTC+02:00)"). The list is computed by PHP at render
 * time, so it always matches the timezone database of the host.
 */
public final class TimeZoneElement
{
    private TimeZoneElement() {}

    public static NinjaFormsPlugin plugin()
    {
        NinjaFormsElement timezone = NinjaFormsElement
            .select( "nftz-timezone", "NFTZ_Fields_Timezone", "Timezone" )
            .section( "common" )
            .icon( "chevron-down" )
            .testValue( "UTC" )
            .placeholderLabel( "Select Timezone" )
            .optionsPhp( OPTIONS_PHP )
            .build();

        return new NinjaFormsPlugin.Builder()
            .pluginName( "Ninja Forms Timezone Element" )
            .pluginSlug( "ninjaforms-timezone" )
            .description( "Adds a Timezone select field to Ninja Forms: every IANA timezone "
                + "identifier with its current UTC offset. Options are fixed by the plugin." )
            .author( "Craig" )
            .version( "0.1.0" )
            .functionPrefix( "nftz" )
            .element( timezone )
            .build();
    }

    /**
     * Appended inside the generated get_options() method. PHP: iterate the
     * IANA timezone database and append one option per identifier, labelled
     * with the identifier and its current UTC offset.
     */
    private static final String OPTIONS_PHP = """
        foreach ( DateTimeZone::listIdentifiers( DateTimeZone::ALL ) as $timezone ) {
        \t$order++;
        \t$options[] = array(
        \t\t'label'    => sprintf( '%s (UTC%s)', $timezone, ( new DateTime( 'now', new DateTimeZone( $timezone ) ) )->format( 'P' ) ),
        \t\t'value'    => $timezone,
        \t\t'calc'     => '',
        \t\t'selected' => 0,
        \t\t'order'    => $order,
        \t);
        }""";
}
