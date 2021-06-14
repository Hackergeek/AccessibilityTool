package com.lgh.accessibilitytool

import android.graphics.Rect
import java.util.*

class WidgetButtonDescribe {
    var packageName: String
    var activityName: String
    var className: String
    var idName: String
    var describe: String
    var text: String
    var bonus: Rect
    var clickable: Boolean
    var onlyClick: Boolean

    constructor() {
        packageName = ""
        activityName = ""
        className = ""
        idName = ""
        describe = ""
        text = ""
        bonus = Rect()
        clickable = false
        onlyClick = false
    }

    constructor(widgetDescribe: WidgetButtonDescribe) {
        packageName = widgetDescribe.packageName
        activityName = widgetDescribe.activityName
        className = widgetDescribe.className
        idName = widgetDescribe.idName
        describe = widgetDescribe.describe
        text = widgetDescribe.text
        bonus = Rect(widgetDescribe.bonus)
        clickable = widgetDescribe.clickable
        onlyClick = widgetDescribe.onlyClick
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (this === other) return true
        if (!(other is WidgetButtonDescribe)) return false
        return (bonus == other.bonus)
    }

    override fun hashCode(): Int {
        return Objects.hash(bonus)
    }
}