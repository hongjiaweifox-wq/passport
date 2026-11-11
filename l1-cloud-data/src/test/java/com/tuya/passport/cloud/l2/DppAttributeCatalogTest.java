package com.tuya.passport.cloud.l2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tuya.passport.cloud.l2.service.DppAttributeCatalog;
import org.junit.jupiter.api.Test;

class DppAttributeCatalogTest {
    private final DppAttributeCatalog catalog = new DppAttributeCatalog();

    @Test void catalogIsCompleteAndHasUniqueFieldCodes() {
        assertEquals(92, catalog.all().size());
        assertEquals(92, catalog.codes().size());
        assertTrue(catalog.contains("battery_serial_number"));
        assertTrue(catalog.contains("manufacturing_date"));
        assertFalse(catalog.isReserved("manufacturing_place"));
        assertTrue(catalog.isReserved("sn"));
        assertEquals("GENERIC", catalog.find("manufacturing_place").implementationStatus());
    }
}
