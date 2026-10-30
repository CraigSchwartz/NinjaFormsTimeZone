package com.astrotools.ninjaforms;

import com.astrotools.ninjaforms.elements.TimeZoneElement;
import com.astrotools.ninjaforms.generate.PhpPluginGenerator;
import com.astrotools.ninjaforms.model.NinjaFormsElement;
import com.astrotools.ninjaforms.model.NinjaFormsPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class PhpPluginGeneratorTest
{
    @TempDir
    Path tempDir;

    @Test
    void generatesTimezonePluginWithExpectedFiles() throws IOException
    {
        List<Path> files = new PhpPluginGenerator()
            .generate( TimeZoneElement.plugin(), tempDir );

        assertEquals( 3, files.size() );
        Path pluginDir = tempDir.resolve( "ninjaforms-timezone" );
        assertTrue( Files.isRegularFile( pluginDir.resolve( "ninjaforms-timezone.php" ) ) );
        assertTrue( Files.isRegularFile( pluginDir.resolve( "includes/Fields/NFTZ_Fields_Timezone.php" ) ) );
        assertTrue( Files.isRegularFile( pluginDir.resolve( "templates/fields-nftz-timezone.html" ) ) );
    }

    @Test
    void mainFileRegistersTheElementWithNinjaForms() throws IOException
    {
        generateTimezone();
        String main = read( "ninjaforms-timezone.php" );

        assertTrue( main.startsWith( "<?php" ) );
        assertTrue( main.contains( "Plugin Name:       Ninja Forms Timezone Element" ) );
        assertTrue( main.contains( "Text Domain:        ninjaforms-timezone" ) );
        assertTrue( main.contains( "add_filter( 'ninja_forms_register_fields', 'nftz_register_fields' )" ) );
        assertTrue( main.contains( "add_filter( 'ninja_forms_field_template_file_paths', 'nftz_template_paths' )" ) );
        assertTrue( main.contains( "$paths[] = plugin_dir_path( __FILE__ ) . 'templates/';" ) );

        // The field class must only load when Ninja Forms asks for fields.
        int filterPos = main.indexOf( "add_filter( 'ninja_forms_register_fields'" );
        int requirePos = main.indexOf( "require_once plugin_dir_path( __FILE__ )" );
        int functionClose = main.indexOf( "return $fields;" );
        assertTrue( filterPos >= 0 && requirePos > filterPos && requirePos < functionClose,
            "field class must be required inside the registration callback" );
        assertTrue( main.contains( "$fields[ 'nftz-timezone' ] = new NFTZ_Fields_Timezone();" ) );
    }

    @Test
    void timezoneFieldClassMirrorsNinjaFormsListField() throws IOException
    {
        generateTimezone();
        String php = read( "includes/Fields/NFTZ_Fields_Timezone.php" );

        assertTrue( php.startsWith( "<?php if ( ! defined( 'ABSPATH' ) ) exit;" ) );
        assertTrue( php.contains( "class NFTZ_Fields_Timezone extends NF_Abstracts_List" ) );
        assertTrue( php.contains( "protected $_type = 'nftz-timezone';" ) );
        assertTrue( php.contains( "protected $_templates = array( 'nftz-timezone', 'listselect' );" ) );
        assertTrue( php.contains( "protected $_test_value = 'UTC';" ) );
        assertTrue( php.contains( "esc_html__( 'Timezone', 'ninjaforms-timezone' )" ) );

        // Fixed options: builder option editor hidden, default-value select wired up.
        assertTrue( php.contains( "$this->_settings[ 'options' ][ 'group' ] = '';" ) );
        assertTrue( php.contains( "'options' => $this->get_default_value_options()," ) );
        assertTrue( php.contains( "add_filter( 'ninja_forms_render_options_' . $this->_type" ) );
        assertTrue( php.contains( "add_filter( 'ninja_forms_custom_columns'" ) );
        assertTrue( php.contains( "add_filter( 'ninja_forms_subs_export_field_value_' . $this->_name" ) );

        // The option source is the IANA timezone database with live offsets.
        assertTrue( php.contains( "DateTimeZone::listIdentifiers( DateTimeZone::ALL )" ) );
        assertTrue( php.contains( "->format( 'P' )" ) );
        assertTrue( php.contains( "sprintf( '%s (UTC%s)', $timezone" ) );
        assertTrue( php.contains( "esc_html__( 'Select Timezone', 'ninjaforms-timezone' )" ) );

        // Submission editing renders a select, not a text input.
        assertTrue( php.contains( "echo \"<select name='fields[$id]'>\";" ) );
        assertTrue( php.contains( "public function admin_form_element( $id, $value )" ) );
    }

    @Test
    void timezoneTemplateUsesUnderscoreSyntaxAndOptionRendering() throws IOException
    {
        generateTimezone();
        String html = read( "templates/fields-nftz-timezone.html" );

        assertTrue( html.startsWith( "<script id=\"tmpl-nf-field-nftz-timezone\" type=\"text/template\">" ) );
        assertTrue( html.contains( "{{{ data.renderOptions() }}}" ) );
        assertTrue( html.contains( "name=\"nf-field-{{{ data.id }}}\"" ) ) ;
        // The option sub-template belongs to core's listselect template; we must
        // not duplicate its DOM id.
        assertFalse( html.contains( "tmpl-nf-field-listselect-option" ) );
    }

    @Test
    void noUnresolvedTokensRemainInGeneratedFiles() throws IOException
    {
        generateTimezone();
        Pattern token = Pattern.compile( "%[a-z_]+%" );
        try ( Stream<Path> files = Files.walk( tempDir ) ) {
            files.filter( Files::isRegularFile ).forEach( file -> {
                try {
                    String content = Files.readString( file );
                    Matcher matcher = token.matcher( content );
                    if ( matcher.find() ) {
                        fail( "Unresolved token '" + matcher.group() + "' in " + file );
                    }
                } catch ( IOException e ) {
                    fail( "Could not read " + file );
                }
            } );
        }
    }

    @Test
    void generatesTextElements() throws IOException
    {
        NinjaFormsPlugin plugin = new NinjaFormsPlugin.Builder()
            .pluginName( "Test Text Elements" )
            .pluginSlug( "test-text" )
            .description( "test" )
            .author( "tester" )
            .functionPrefix( "tte" )
            .element( NinjaFormsElement.text( "tte-note", "TTE_Fields_Note", "Note" ).build() )
            .build();

        new PhpPluginGenerator().generate( plugin, tempDir );

        String php = Files.readString(
            tempDir.resolve( "test-text/includes/Fields/TTE_Fields_Note.php" ) );
        assertTrue( php.contains( "class TTE_Fields_Note extends NF_Abstracts_Input" ) );
        assertTrue( php.contains( "protected $_templates = array( 'tte-note', 'textbox' );" ) );

        String html = Files.readString(
            tempDir.resolve( "test-text/templates/fields-tte-note.html" ) );
        assertTrue( html.contains( "id=\"tmpl-nf-field-tte-note\"" ) );
        assertTrue( html.contains( "{{{ _.escape( data.value ) }}}" ) );
    }

    @Test
    void rejectsInvalidElementDefinitions()
    {
        NinjaFormsPlugin badType = pluginWithElement(
            NinjaFormsElement.select( "BadType", "X_Field", "Bad" ).build() );
        assertValidationFails( badType, "type" );

        NinjaFormsPlugin selectWithoutOptions = pluginWithElement(
            NinjaFormsElement.select( "x-field", "X_Field", "X" ).build() );
        assertValidationFails( selectWithoutOptions, "optionsPhp" );

        NinjaFormsElement textWithOptions = NinjaFormsElement
            .text( "x-field", "X_Field", "X" ).optionsPhp( "echo 'x';" ).build();
        assertValidationFails( pluginWithElement( textWithOptions ), "optionsPhp" );

        NinjaFormsPlugin quoteInNiceName = pluginWithElement(
            NinjaFormsElement.text( "x-field", "X_Field", "It's broken" ).build() );
        assertValidationFails( quoteInNiceName, "niceName" );

        NinjaFormsPlugin duplicateTypes = new NinjaFormsPlugin.Builder()
            .pluginName( "Dup" ).pluginSlug( "dup-plugin" )
            .description( "d" ).author( "a" ).functionPrefix( "dp" )
            .element( NinjaFormsElement.text( "x-field", "DP_Fields_One", "One" ).build() )
            .element( NinjaFormsElement.text( "x-field", "DP_Fields_Two", "Two" ).build() )
            .build();
        assertValidationFails( duplicateTypes, "duplicate element type" );
    }

    @Test
    void rejectsInvalidPluginDefinitions()
    {
        assertValidationFails( new NinjaFormsPlugin.Builder().build(), "pluginName is required" );

        NinjaFormsPlugin badSlug = new NinjaFormsPlugin.Builder()
            .pluginName( "Bad Slug" ).pluginSlug( "Bad Slug" )
            .description( "d" ).author( "a" )
            .element( NinjaFormsElement.text( "x-field", "X_Field", "X" ).build() )
            .build();
        assertValidationFails( badSlug, "pluginSlug" );
    }

    // ---------------------------------------------------------------------

    private void generateTimezone() throws IOException
    {
        new PhpPluginGenerator().generate( TimeZoneElement.plugin(), tempDir );
    }

    private String read( String relative ) throws IOException
    {
        return Files.readString( tempDir.resolve( "ninjaforms-timezone" ).resolve( relative ) );
    }

    private NinjaFormsPlugin pluginWithElement( NinjaFormsElement element )
    {
        return new NinjaFormsPlugin.Builder()
            .pluginName( "Test" ).pluginSlug( "test-plugin" )
            .description( "test" ).author( "tester" ).functionPrefix( "tp" )
            .element( element )
            .build();
    }

    private void assertValidationFails( NinjaFormsPlugin plugin, String expectedInMessage )
    {
        try {
            new PhpPluginGenerator().generate( plugin, tempDir );
            fail( "Expected validation failure containing '" + expectedInMessage + "'" );
        } catch ( IllegalStateException e ) {
            assertTrue( e.getMessage().contains( expectedInMessage ),
                "message '" + e.getMessage() + "' should contain '" + expectedInMessage + "'" );
        } catch ( IOException e ) {
            fail( "Unexpected IOException: " + e.getMessage() );
        }
    }
}
