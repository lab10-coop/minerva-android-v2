# Minerva android v2

##Build
Build is done via docker. If you want to build an app and have docker installed just type:

### Debug
- `docker-compose up`
### Release
- `docker-compose run builder ./gradlew assembleRelease`  