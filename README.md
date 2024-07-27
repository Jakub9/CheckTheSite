# CheckTheSite

Small tool to periodically check a website and notify you per email if something appears there. Feel free to clone/fork
and implement your own site checker implementation!

## What it does

It periodically opens a website with a configurable delay between requests. Then, the chosen site checker implementation
analyzes the response and decides whether you should be notified. If that's the case, an email will be sent to all given
recipients. To prevent spamming, requests will be paused after the first email. Typing `confirm` into the program's
console re-enables the automatic scheduling of requests.

## Usage

### Site checker implementations

The site checker implementation defines how a website is checked and when it's considered worth notifying you. They
always implement the [`SiteChecker`](src/main/kotlin/checker/SiteChecker.kt) interface and are declared to be used in
the base configuration file `config.yml`. One example implementation is already
provided: [`SimpleContainsChecker`](src/main/kotlin/checker/impl/SimpleContainsChecker.kt). This one checks whether a
website contains (or doesn't contain) a string. It's fully functional and can be used if it's enough for your needs,
otherwise you can always write your own sophisticated implementation and configure the program to use it.

### Getting started

1. Clone and build a .jar using `gradlew jar`
2. Run the program, it should close itself and generate an example configuration file
3. Configure to your own needs, see [`Configuration`](src/main/kotlin/model/Configuration.kt) for explanations of the
   various attributes
4. Run the program again, if the configured site checker implementation requires configuration, it will generate another
   config file for it, otherwise it should keep running
5. (optional) Configure the site checker config and restart the program
6. (optional) Implement your own sophisticated site checker implementation
