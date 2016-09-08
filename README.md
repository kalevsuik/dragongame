# README #

### Tested on .. ###

* java 8
* sbt 0.13.11
* OSX 10.11  & Ubuntu 14.04

### Summary of set up ###
* git clone git@bitbucket.org:kalev_suik/dragon_game.git
* cd dragon_game
* sbt "run 20"
* where 20 is number of battles
* (if started from another, then configuration might need to be adjusted/given depending on parameters.
That can be done usual Typesafe config way - https://github.com/typesafehub/config)

### Configuration can be found as below, but should not be needed ###
* src/main/resources/application.conf
* src/main/resources/logback.xml