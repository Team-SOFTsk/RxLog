# RxLog
RxLog is a simple helper library to handle your RxJava logs. Supports both RxJava1 and RxJava2

[ ![Download](https://api.bintray.com/packages/team-softsk/maven/rxlog/images/download.svg) ](https://bintray.com/team-softsk/maven/rxlog/_latestVersion)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/sk.teamsoft/rxlog/badge.svg)](https://maven-badges.herokuapp.com/maven-central/sk.teamsoft/rxlog)

### Usage

For RxJava2
```
compile 'sk.teamsoft:rxlog:1.0.2'
```


For RxJava1
```
compile 'sk.teamsoft:rxlog1:1.0.2'
```

**For full lifecycle logging**
```
Observable.just(data)
    .compose(RxLog("this will log all lifecycle events (subscribe/next/complete/error...) of this stream"))
    .subscribe(...);
```

**For custom logging**
```
Observable.just(data)
    .compose(RxLog("this will log only next and complete events", RxLog.LOG_NEXT_DATA | RxLog.LOG_COMPLETE))
    .subscribe(...);
```


Available options for bitmask:
- **LOG_NEXT_DATA**         - logs next event with data
- **LOG_NEXT_EVENT**        - logs next event but without data (useful when data is too big to serialize)
- **LOG_ERROR**             - logs error event
- **LOG_COMPLETE**          - logs complete event
- **LOG_SUBSCRIBE**         - logs subscribe event
- **LOG_TERMINATE**         - logs terminate event
- **LOG_DISPOSE**           - logs dispose event


### Author
Team-SOFT s.r.o.<br/>
dusan@teamsoft.sk
