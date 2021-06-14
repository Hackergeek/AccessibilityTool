package com.lgh.accessibilitytool

class SkipPositionDescribe {
    var packageName: String
    var activityName: String
    var x: Int
    var y: Int
    var delay: Int
    var period: Int
    var number: Int

    constructor(
        packageName: String,
        activityName: String,
        x: Int,
        y: Int,
        delay: Int,
        period: Int,
        number: Int
    ) {
        this.packageName = packageName
        this.activityName = activityName
        this.x = x
        this.y = y
        this.delay = delay
        this.period = period
        this.number = number
    }

    constructor(position: SkipPositionDescribe) {
        packageName = position.packageName
        activityName = position.activityName
        x = position.x
        y = position.y
        delay = position.delay
        period = position.period
        number = position.number
    }
}