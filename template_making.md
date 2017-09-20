# CYOA Studio template creation

CYOA Studio uses HTML and CSS packaged into a template to create the final document. This document describes the process of making your own template.

You'll need:

- **HTML/CSS knowledge.** This document does not teach the technical know how required, but there are more than enough tutorials on this topic on the internet already.
- **CYOA Studio.** For testing your template (and of course for using it).
- **A zip tool.** The final `.cyoatemplate` file is simply a renamed `.zip` file. Most modern operating systems ship with the ability to zip files.

## Template structure

A template consists of these files:

- `page.html.mustache` contains the document structure that the sections and options are placed into. If a template doesn't provide its own page file, a default is provided that should work for most templates.
- `style.css.mustache` contains the style sheet. This file is mandatory.
- `style_option.json` describes the customization options and their default values.
- Any number of image files used in the template.

Both the page and style sheet are mustache templates. The page will be provided with the content that should be displayed, and the style sheet with the customization options the user chooses. The values are accessed by a curly brace syntax like `{{valueName}}`. For more information see the [mustache manual](https://mustache.github.io/mustache.5.html).

You can access the default files in the [repository](https://github.com/Quantencomputer/cyoastudio/tree/master/src/main/resources/cyoastudio/templating/defaultTemplate) for reference. Feel free to use them as a basis for your template.

### `page.html.mustache`

This template provides the base structure for the document. The default should work for most use cases, but you can provide your own.
The following values are provided to the template:

- `style` contains the content of `style.css.mustache` after it's been templated.
- `customCss` contains the custom CSS provided by the user.
- `projectTitle`. Can be null if the title is empty.
- `section` contains a list of all the sections. The sections have the following values:
	- `sectionTitle`. Can be null if the title is empty.
	- `description`. Can be null if the description is empty.
	- `options` contains a list of all the options in the section. The sections have the following values:
		- `optionTitle`. Can be null if the title is empty.
		- `description`. Can be null if the description is empty.
		- `image` contains the image of the option, to be used as the `src` attribute of an `img` tag.

Note that the `title`s and `description`s contains HTML strings, and as such should be included using the non-escaping triple bracket syntax. The same goes for `style` and `customCss`.

### `style_options.json`

This JSON file describes the options available to the user. It should contain a simple JSON object. Depending on the name of the fields, different values are assumed:

- `\*Image`: An image. The default value has to be the name of an image file included in the template.
- `\*Color`: A color. The default value has to be a valid CSS color.
- `\*Font`: A font-family. The default value has to be the name of the font family.

In the template, images are provided as strings to be used in an `url()` expression. Colors and fonts are provided as strings to be used in color attributes and `font-family`, respectively.

If no such file is provided, no options are presented to the user.

### `style.css.mustache`

This file can contain arbitrary CSS to style the document. The options described in the `style_options.json` file are provided. Use the triple bracket syntax to avoid escaping of control characters.

Sadly, the document is displayed with the slightly outdated version of webkit that JavaFX uses. It supports a lot of modern CSS3 features, but not all of them. CSS grids are not supported, neither are blend modes.

This tool still supports users changing the color of the background image. If a style option ending in `Image` and one with the same name but ending in `ImageColor` exist, additionally to the two values the style sheet can also use a value ending in `ImageBlended`, containing the original image, multiplied with the color. For example, if two options called `backgroundImage` and `backgroundImageColor` exist, `backgroundImageBlended` will contain the background image with the color applied. If you use this option, provide a light, black and white image for best results.

## Testing

If you put all the files required for the template, you can load it through the `Import template->From folder...` option.
For development, it might be useful to export the file to HTML and edit that exported file.

## Packaging up

Once your template is done, you can zip it together and change the file ending to `.cyoastudio`.
You can then distribute that file if you want.