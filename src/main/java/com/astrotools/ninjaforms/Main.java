package com.astrotools.ninjaforms;

import com.astrotools.ninjaforms.elements.TimeZoneElement;
import com.astrotools.ninjaforms.generate.PhpPluginGenerator;
import com.astrotools.ninjaforms.generate.Zipper;
import com.astrotools.ninjaforms.model.NinjaFormsPlugin;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Command line entry point of the NinjaForms Element Generator.
 *
 * <pre>
 *   mvn -q exec:java                                # generate the timezone plugin into ./dist
 *   mvn -q exec:java -Dexec.args="--zip"           # ... and produce an installable .zip
 *   mvn -q exec:java -Dexec.args="--out /tmp/gen"  # custom output directory
 *   mvn -q exec:java -Dexec.args="--list"          # list the built-in elements
 * </pre>
 */
public final class Main
{
    private static final Map<String, Supplier<NinjaFormsPlugin>> ELEMENTS = new LinkedHashMap<>();
    static {
        ELEMENTS.put( "timezone", TimeZoneElement::plugin );
    }

    private Main() {}

    public static void main( String[] args ) throws IOException
    {
        String elementName = "timezone";
        Path outputDir = Path.of( "dist" );
        boolean zip = false;

        for ( int i = 0; i < args.length; i++ ) {
            switch ( args[ i ] ) {
                case "--element" -> elementName = valueOf( args, ++i, "--element" );
                case "--out"     -> outputDir = Path.of( valueOf( args, ++i, "--out" ) );
                case "--zip"     -> zip = true;
                case "--list"    -> { listElements(); return; }
                case "--help"    -> { printUsage(); return; }
                default          -> { unknownArgument( args[ i ] ); return; }
            }
        }

        Supplier<NinjaFormsPlugin> supplier = ELEMENTS.get( elementName );
        if ( supplier == null ) {
            System.err.println( "Unknown element '" + elementName + "'. Available: " + ELEMENTS.keySet() );
            System.exit( 1 );
        }

        NinjaFormsPlugin plugin = supplier.get();
        try {
            List<Path> files = new PhpPluginGenerator().generate( plugin, outputDir );
            files.forEach( file -> System.out.println( "wrote " + file ) );
            if ( zip ) {
                Path zipFile = outputDir.resolve( plugin.getPluginSlug() + ".zip" );
                Zipper.zipDirectory( outputDir.resolve( plugin.getPluginSlug() ), zipFile );
                System.out.println( "wrote " + zipFile );
            }
        } catch ( IOException e ) {
            System.err.println( "Generation failed: " + e.getMessage() );
            System.exit( 1 );
        }
    }

    private static String valueOf( String[] args, int index, String option )
    {
        if ( index >= args.length ) {
            System.err.println( "Missing value for " + option );
            printUsage();
            System.exit( 1 );
        }
        return args[ index ];
    }

    private static void listElements()
    {
        System.out.println( "Built-in elements:" );
        ELEMENTS.keySet().forEach( name -> System.out.println( "  " + name ) );
    }

    private static void unknownArgument( String argument )
    {
        System.err.println( "Unknown argument: " + argument );
        printUsage();
        System.exit( 1 );
    }

    private static void printUsage()
    {
        System.out.println( """
            Usage: mvn -q exec:java [-Dexec.args="<args>"]

            Arguments:
              --element <name>   Built-in element to generate (default: timezone)
              --out <dir>        Output directory (default: dist)
              --zip              Also write an installable plugin .zip
              --list             List the built-in elements
              --help             Show this help""" );
    }
}
