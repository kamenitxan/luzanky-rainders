luzanky-rainders
================
This app generates WoW guild rooster from Blizzard API as HTML file and sends it via SSH to server. Requests to API are
parallelized and compressed, so it's fast (less than 3 seconds for 50 characters). Also only changed characters are
queried.

Example: http://kamenitxan.eu/share/raiders.html

It is possible to specify any EU guild via cmd parameters: "java -jar Luzanky Thunderhorn".
With "Luzanky Thunderhorn force" characters will be force updated regardless their last update status.

Queries and HTML generator is in Generator class. Audit class checks if character passes raid requirments. dataHolders
package contains support classes with data.
Characters are stored in SQLite DB, which is updated on every app run.