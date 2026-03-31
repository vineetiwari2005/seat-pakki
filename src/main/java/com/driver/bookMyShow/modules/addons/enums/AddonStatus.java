package com.driver.bookMyShow.modules.addons.enums;

public enum AddonStatus {
    SELECTED,    // User selected but not yet confirmed
    CONFIRMED,   // Successfully processed with payment
    CANCELLED,   // User cancelled before payment
    FAILED       // Processing failed but payment succeeded (graceful degradation)
}
