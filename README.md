
# Jar Presenter

A tool to package a web-based presentation into a jar. It provides
it own tiny HTTP server to serve the presentation.

## Features

### Server
The server is started with the sub-command `server`. It starts a minimalistic
HTTP server and serves the presentation included in the jar.

If requested with `-b` the default web browser is started automatically and
will show the presentation.

I called with `-g` or starting without a terminal (double click in a file
explorer), a GUI dialog is opened. It shows the server address and has a button
to stop the server.

Help output:
```
$ java -jar jar-presenter-0.1.0-SNAPSHOT.jar server --help
server - starts a web server to serve the presentation
      USAGE: java -jar jar-presenter.jar server [-b] [-v] [port]
        -b       immediately start the (default) browser
        -v       increase logging output
        -g       Use gui to report that server is running. Default when no
                 terminal is attached.
        port     use given port (default is random)
```

### Extract
With the sub-command `extract` the contained presentation is extracted to the
given directory in the sub-directory `presentation`.

Help output:
```
$ java -jar jar-presenter-0.1.0-SNAPSHOT.jar extract --help
extract - extract the contained presentation to the given directory
      USAGE: java -jar jar-presenter.jar extract <target-dir>
```

### Build
With the `build` sub-command a new jar-presenter with a new presentation can be
created. This command takes the Java classes from the current jar and combines
them with a presentation on disk into a new jar.

Help output:
```
$ java -jar jar-presenter-0.1.0-SNAPSHOT.jar build --help
build - build a NEW presentation jar for given presentation
      USAGE: java -jar jar-presenter.jar build <new-jar-name> <presentation-dir>
        new-jar-name
                 name of the new jar to create
        presentation-dir
                 directory of the presentation to include in new jar
```

## Building Jar-Presenter


Normal build is easy: `mvn clean package`

The normal build doesn't carry any presentation. It is runable, but will
always answer with a HTTP/404 Not Found.

To build an example run `mvn clean package -Pexample`.

This will create a jar that contains the demo presentation for
[reveal.js](https://revealjs.com/).

