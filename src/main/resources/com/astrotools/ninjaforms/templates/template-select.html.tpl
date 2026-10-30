<script id="tmpl-nf-field-%type%" type="text/template">
    <select id="nf-field-{{{ data.id }}}" name="nf-field-{{{ data.id }}}" aria-invalid="false" aria-describedby="<# if( data.desc_text ) { #>nf-description-{{{ data.id }}} <# } #>nf-error-{{{ data.id }}}" class="{{{ data.renderClasses() }}} nf-element" {{{ data.renderOtherAttributes() }}}
            aria-labelledby="nf-label-field-{{{ data.id }}}"

	    {{{ data.maybeRequired() }}}
    >
        {{{ data.renderOptions() }}}
    </select>
    <div for="nf-field-{{{ data.id }}}"></div>
</script>
