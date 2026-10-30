package com.astrotools.ninjaforms;

import com.astrotools.ninjaforms.generate.TemplateEngine;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateEngineTest
{
    @Test
    void replacesKnownTokens()
    {
        String out = TemplateEngine.render( "hello %name% and %type%", Map.of( "name", "world", "type", "nftz-timezone" ) );
        assertEquals( "hello world and nftz-timezone", out );
    }

    @Test
    void leavesUnknownTokensAndPhpFormatStringsUntouched()
    {
        String template = "sprintf( '%s (UTC%s)', $a, $b ) %unknown% {{{ data.id }}}";
        String out = TemplateEngine.render( template, Map.of( "type", "nftz-timezone" ) );
        assertEquals( template, out );
    }

    @Test
    void nullTokenValuesRenderAsEmptyString()
    {
        Map<String, String> tokens = new HashMap<>();
        tokens.put( "value", null );
        String out = TemplateEngine.render( "[%value%]", tokens );
        assertEquals( "[]", out );
    }

    @Test
    void allBundledTemplatesLoad()
    {
        String[] templates = {
            "plugin-main.php.tpl", "field-select.php.tpl", "field-text.php.tpl",
            "template-select.html.tpl", "template-text.html.tpl" };
        for ( String template : templates ) {
            assertFalse( TemplateEngine.load( template ).isBlank(), "template " + template + " must load" );
        }
    }

    @Test
    void selectTemplateKeepsUnderscoreSyntax()
    {
        String rendered = TemplateEngine.render(
            TemplateEngine.load( "template-select.html.tpl" ), Map.of( "type", "nftz-timezone" ) );
        assertTrue( rendered.contains( "id=\"tmpl-nf-field-nftz-timezone\"" ) );
        assertTrue( rendered.contains( "{{{ data.renderOptions() }}}" ) );
        assertTrue( rendered.contains( "{{{ data.renderClasses() }}}" ) );
    }

    @Test
    void loadFailsForUnknownTemplate()
    {
        assertThrows( RuntimeException.class, () -> TemplateEngine.load( "no-such.php.tpl" ) );
    }
}
