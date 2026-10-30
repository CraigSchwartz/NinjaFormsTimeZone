# NinjaForms Element Generator

A Maven project that **creates custom Ninja Forms elements**. Ninja Forms elements are WordPress
plugins written in PHP, so this project works as a generator: you describe elements in a small Java
model, and the generator stamps out a complete, installable WordPress plugin that registers those
elements with [Ninja Forms 3](https://ninjaforms.com/).

The first built-in element is a **Timezone** select field (fits this project's astrology context:
birth-data forms need a timezone). More elements can be added as plain Java.

## Layout

```
NinjaFormsTimeZone/
├── pom.xml                                  Maven build (Java 17+, no runtime dependencies)
├── src/main/java/com/astrotools/ninjaforms/
│   ├── Main.java                            CLI: generate plugins from built-in elements
│   ├── model/
│   │   ├── NinjaFormsPlugin.java            plugin description (name, slug, prefix, elements...)
│   │   └── NinjaFormsElement.java           element description (SELECT or TEXT kind)
│   ├── generate/
│   │   ├── PhpPluginGenerator.java          writes the WordPress plugin files
│   │   ├── TemplateEngine.java              %token% replacement for the bundled templates
│   │   └── Zipper.java                      creates the installable plugin .zip
│   └── elements/
│       └── TimeZoneElement.java             built-in Timezone element definition
├── src/main/resources/.../templates/        PHP/HTML templates stamped out by the generator
└── src/test/java/                           unit tests validating the generated PHP/HTML
```

## Build and generate

```bash
mvn package                                   # build + run the unit tests
mvn -q exec:java                              # generate the timezone plugin into ./dist
mvn -q exec:java -Dexec.args="--zip"          # ... and write dist/ninjaforms-timezone.zip
mvn -q exec:java -Dexec.args="--out /somewhere --element timezone"
mvn -q exec:java -Dexec.args="--list"        # list built-in elements
```

Generated output (mirrors how Ninja Forms' own extensions are structured):

```
dist/ninjaforms-timezone/
├── ninjaforms-timezone.php                     main plugin file (WP header + registration)
├── includes/Fields/NFTZ_Fields_Timezone.php   field class extending NF_Abstracts_List
└── templates/fields-nftz-timezone.html         front-end template (Underscore syntax)
```

## Installing the generated plugin

1. `mvn -q exec:java -Dexec.args="--zip"` to produce `dist/ninjaforms-timezone.zip`.
2. WordPress admin: Plugins → Add New → Upload Plugin → choose the zip → Activate.
3. Ninja Forms builder: the **Timezone** field appears in the *Common Fields* palette.
   Drag it onto a form like any other field.

Requirements: WordPress 5.9+, PHP 7.4+, Ninja Forms 3.x active.

## What the Timezone element does

- A select field listing **every IANA timezone identifier** (`DateTimeZone::listIdentifiers`),
  each labelled with its **current UTC offset**, e.g. `Europe/Berlin (UTC+02:00)`. The list is
  computed at render time, so it always matches the host's timezone database.
- The first option is an empty `- Select Timezone -` placeholder.
- The builder's Default Value setting is a dropdown of the same list; a configured default is
  preselected on the front end.
- Options are fixed by the plugin (the builder's option editor is hidden), and Ninja Forms'
  inherited list-field validation whitelists submitted values against them server-side.
- Submissions show the timezone label in the admin table and CSV exports, and edit-in-place
  renders a proper select.

## Adding a new element

1. Describe it in Java (`SELECT` and `TEXT` kinds are supported):

```java
NinjaFormsElement planet = NinjaFormsElement
    .select( "nfa-planet", "NFA_Fields_Planet", "Planet" )   // or .text(...)
    .testValue( "mars" )
    .placeholderLabel( "Select Planet" )
    .optionsPhp( """
        foreach ( array( 'mercury' => 'Mercury', 'mars' => 'Mars' ) as $value => $label ) {
        \t$order++;
        \t$options[] = array(
        \t\t'label' => $label, 'value' => $value,
        \t\t'calc' => '', 'selected' => 0, 'order' => $order,
        \t);
        }""" )
    .build();

NinjaFormsPlugin plugin = new NinjaFormsPlugin.Builder()
    .pluginName( "Ninja Forms Planet Element" )
    .pluginSlug( "ninjaforms-planets" )
    .description( "Planet select field for Ninja Forms." )
    .author( "Craig" )
    .functionPrefix( "nfa" )
    .element( planet )
    .build();
```

The `optionsPhp` snippet is inserted into the generated `get_options()` method; it must append
entries with `label`, `value`, `calc`, `selected` and `order` keys to `$options`. Tokens such as
`%text_domain%` may be used inside the snippet.

2. Add it to the registry in `Main.java`:
   `ELEMENTS.put( "planets", () -> plugin );`
3. Regenerate: `mvn -q exec:java -Dexec.args="--element planets --zip"`.

One plugin can contain several elements; call `.element(...)` once per field.

## How the generated plugin hooks into Ninja Forms

The generator was written against the Ninja Forms 3.15.7 source (patterns verified there, not
guessed):

| Concern | Mechanism |
|---|---|
| Register a field type | `add_filter( 'ninja_forms_register_fields', ... )` — the callback adds `$fields['nftz-timezone'] = new NFTZ_Fields_Timezone()` |
| Field class | `NF_Abstracts_List` (fixed-option selects) or `NF_Abstracts_Input` (text) |
| Front-end template | `templates/fields-<type>.html` loaded via `ninja_forms_field_template_file_paths`, Underscore syntax (`{{{ data.id }}}`, `{{{ data.renderOptions() }}}`) |
| Fixed options | `ninja_forms_render_options_<type>` filter overrides options at render time; the inherited `validate()` whitelists submitted values against them |
| Builder "Default Value" | custom `$_settings['default']` select populated from the same option list |
| Submission display/edit/CSV | `admin_form_element()`, `ninja_forms_custom_columns`, `ninja_forms_subs_export_field_value_<name>` |
| Template fallback | `$_templates = array( '<type>', 'listselect' )` — if the custom template is ever missing, Ninja Forms' own select markup is used |

Because the field class is required *inside* the `ninja_forms_register_fields` callback, the
plugin is inert (and causes no fatals) when Ninja Forms is deactivated.

## Verification status

- `mvn package`: 14 unit tests pass, covering the generated PHP structure (hooks, class
  inheritance, option generation, template ids) and token rendering.
- The generated PHP mirrors `NF_Fields_ListCountry` / `NF_Fields_ListSelect` from the Ninja Forms
  3.15.7 source.
- No PHP interpreter or Docker is available on this machine, so `php -l` was **not** run on the
  generated files. Before deploying to production, upload the zip to a staging WordPress site and
  check the field in the builder, on a page, and in a submission.

## Regenerating after edits

The generated files carry a "do not edit by hand" notice. Change the Java model or the
`src/main/resources` templates and regenerate; then bump `version` in the plugin model so
WordPress shows the update.
