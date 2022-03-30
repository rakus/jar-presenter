
Prime Directives:
* No dependencies! JDK classes only.
* As less code as possible.
* "Nice to have" is no reason

* `server` command
    * Only allow clients from localhost & option to allow any client.
    * Expand content type detection. Maybe also look into the first bytes to
      transfer.
* `build` command
    * Check presentation for `index.html`. Complain if not found and `-i` not
      given.
* `extract` command
    * Don't create additional sub-dir `presentation`.


