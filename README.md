
# JAr Presenter

A tool to package a web-based presentation into a jar. It provides
it own tiny HTTP server to serve the presentation.


## Build

Normal build is easy: `mvn clean package`

The normal build doesn't carry any presentation. It is startable, but will
always answer with a HTTP/404 Not Found.

To build an example run `mvn clean package -Pexample`.

This will create a jar that contains the demo presentation for
[reveal.js](https://revealjs.com/).
