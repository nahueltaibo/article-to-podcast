package com.nahueltaibo.pagecast;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VersionCodeTest {
    @Test
    public void versionCodeIsAboveTheFirstManualUpload() {
        // Release 1 was uploaded by hand with versionCode 1. Play rejects anything not higher.
        assertTrue(BuildConfig.VERSION_CODE > 1);
    }
}
